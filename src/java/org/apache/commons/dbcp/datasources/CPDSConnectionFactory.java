/*
 * Copyright 1999-2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.dbcp.datasources;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import javax.sql.ConnectionEvent;
import javax.sql.ConnectionEventListener;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.PooledConnection;

import org.apache.commons.dbcp.SQLNestedException;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.PoolableObjectFactory;

/**
 * A {@link PoolableObjectFactory} that creates
 * {@link PoolableConnection}s.
 *
 * @author John D. McNally
 * @version $Revision: 1.5 $ $Date: 2004/02/28 12:18:17 $
 */
class CPDSConnectionFactory 
        implements PoolableObjectFactory, ConnectionEventListener {

    private static final String NO_KEY_MESSAGE 
            = "close() was called on a Connection, but " 
            + "I have no record of the underlying PooledConnection.";

    protected ConnectionPoolDataSource _cpds = null;
    protected String _validationQuery = null;
    protected ObjectPool _pool = null;
    protected String _username = null;
    protected String _password = null;
    private Map validatingMap = new HashMap();
    private WeakHashMap pcMap = new WeakHashMap();

    /**
     * Create a new <tt>PoolableConnectionFactory</tt>.
     * @param cpds the ConnectionPoolDataSource from which to obtain 
     *        PooledConnection's
     * @param pool the {*link ObjectPool} in which to pool those 
     *        {*link Connection}s
     * @param validationQuery a query to use to {*link #validateObject validate}
     *        {*link Connection}s. Should return at least one row. 
     *        May be <tt>null</tt>
     * @param username 
     * @param password
     */
    public CPDSConnectionFactory(ConnectionPoolDataSource cpds, 
                                 ObjectPool pool, 
                                 String validationQuery,
                                 String username,
                                 String password) {
        _cpds = cpds;
        _pool = pool;
        _pool.setFactory(this);
        _validationQuery = validationQuery;
        _username = username;
        _password = password;
    }

    /**
     * Sets the {*link ConnectionFactory} from which to obtain base 
     * {*link Connection}s.
     * @param connFactory the {*link ConnectionFactory} from which to obtain 
     *        base {*link Connection}s
     */
    public synchronized void setCPDS(ConnectionPoolDataSource cpds) {
        _cpds = cpds;
    }

    /**
     * Sets the query I use to {*link #validateObject validate} 
     * {*link Connection}s.
     * Should return at least one row.
     * May be <tt>null</tt>
     * @param validationQuery a query to use to {*link #validateObject validate} 
     *        {*link Connection}s.
     */
    public synchronized void setValidationQuery(String validationQuery) {
        _validationQuery = validationQuery;
    }

    /**
     * Sets the {*link ObjectPool} in which to pool {*link Connection}s.
     * @param pool the {*link ObjectPool} in which to pool those 
     *        {*link Connection}s
     */
    public synchronized void setPool(ObjectPool pool) throws SQLException {
        if (null != _pool && pool != _pool) {
            try {
                _pool.close();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new SQLNestedException("Cannot set the pool on this factory", e);
            }
        }
        _pool = pool;
    }

    public ObjectPool getPool() {
        return _pool;
    }

    public synchronized Object makeObject() {
        Object obj;
        try {
            PooledConnection pc = null;
            if (_username == null) {
                pc = _cpds.getPooledConnection();
            } else {
                pc = _cpds.getPooledConnection(_username, _password);
            }
            // should we add this object as a listener or the pool.
            // consider the validateObject method in decision
            pc.addConnectionEventListener(this);
            obj = new PooledConnectionAndInfo(pc, _username, _password);
            pcMap.put(pc, obj);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage());
        }
        return obj;
    }

    public void destroyObject(Object obj) throws Exception {
        if (obj instanceof PooledConnectionAndInfo) {
            ((PooledConnectionAndInfo) obj).getPooledConnection().close();
        }
    }

    public boolean validateObject(Object obj) {
        boolean valid = false;
        if (obj instanceof PooledConnectionAndInfo) {
            PooledConnection pconn = 
                ((PooledConnectionAndInfo) obj).getPooledConnection();
            String query = _validationQuery;
            if (null != query) {
                Connection conn = null;
                Statement stmt = null;
                ResultSet rset = null;
                // logical Connection from the PooledConnection must be closed
                // before another one can be requested and closing it will 
                // generate an event. Keep track so we know not to return
                // the PooledConnection 
                validatingMap.put(pconn, null);
                try {
                    conn = pconn.getConnection();
                    stmt = conn.createStatement();
                    rset = stmt.executeQuery(query);
                    if (rset.next()) {
                        valid = true;
                    } else {
                        valid = false;
                    }
                } catch (Exception e) {
                    valid = false;
                } finally {
                    try {
                        rset.close();
                    } catch (Throwable t) {
                        // ignore
                    }
                    try {
                        stmt.close();
                    } catch (Throwable t) {
                        // ignore
                    }
                    try {
                        conn.close();
                    } catch (Throwable t) {
                        // ignore
                    }
                    validatingMap.remove(pconn);
                }
            } else {
                valid = true;
            }
        } else {
            valid = false;
        }
        return valid;
    }

    public void passivateObject(Object obj) {
    }

    public void activateObject(Object obj) {
    }

    // ***********************************************************************
    // java.sql.ConnectionEventListener implementation
    // ***********************************************************************

    /**
     * This will be called if the Connection returned by the getConnection
     * method came from a PooledConnection, and the user calls the close()
     * method of this connection object. What we need to do here is to
     * release this PooledConnection from our pool...
     */
    public void connectionClosed(ConnectionEvent event) {
        PooledConnection pc = (PooledConnection) event.getSource();
        // if this event occured becase we were validating, ignore it
        // otherwise return the connection to the pool.
        if (!validatingMap.containsKey(pc)) {
            Object info = pcMap.get(pc);
            if (info == null) {
                throw new IllegalStateException(NO_KEY_MESSAGE);
            }            

            try {
                _pool.returnObject(info);
            } catch (Exception e) {
                System.err.println("CLOSING DOWN CONNECTION AS IT COULD " 
                        + "NOT BE RETURNED TO THE POOL");
                try {
                    destroyObject(info);
                } catch (Exception e2) {
                    System.err.println("EXCEPTION WHILE DESTROYING OBJECT " 
                            + info);
                    e2.printStackTrace();
                }
            }
        }
    }

    /**
     * If a fatal error occurs, close the underlying physical connection so as
     * not to be returned in the future
     */
    public void connectionErrorOccurred(ConnectionEvent event) {
        PooledConnection pc = (PooledConnection)event.getSource();
        try {
            if (null != event.getSQLException()) {
                System.err.println(
                        "CLOSING DOWN CONNECTION DUE TO INTERNAL ERROR (" 
                        + event.getSQLException() + ")");
            }
            //remove this from the listener list because we are no more 
            //interested in errors since we are about to close this connection
            pc.removeConnectionEventListener(this);
        } catch (Exception ignore) {
            // ignore
        }

        Object info = pcMap.get(pc);
        if (info == null) {
            throw new IllegalStateException(NO_KEY_MESSAGE);
        }            
        try {
            destroyObject(info);
        } catch (Exception e) {
            System.err.println("EXCEPTION WHILE DESTROYING OBJECT " + info);
            e.printStackTrace();
        }
    }

}
