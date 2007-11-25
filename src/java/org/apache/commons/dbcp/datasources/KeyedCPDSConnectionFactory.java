/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
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
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

import javax.sql.ConnectionEvent;
import javax.sql.ConnectionEventListener;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.PooledConnection;

import org.apache.commons.dbcp.SQLNestedException;
import org.apache.commons.pool.KeyedObjectPool;
import org.apache.commons.pool.KeyedPoolableObjectFactory;

/**
 * A {*link PoolableObjectFactory} that creates
 * {*link PoolableConnection}s.
 *
 * @author John D. McNally
 * @version $Revision$ $Date$
 */
class KeyedCPDSConnectionFactory
    implements KeyedPoolableObjectFactory, ConnectionEventListener {

    private static final String NO_KEY_MESSAGE
            = "close() was called on a Connection, but "
            + "I have no record of the underlying PooledConnection.";

    protected ConnectionPoolDataSource _cpds = null;
    protected String _validationQuery = null;
    protected boolean _rollbackAfterValidation = false;
    protected KeyedObjectPool _pool = null;
    
    /** 
     * Map of "muted" PooledConnections for which close events
     * are ignored. Connections are muted when they are being validated
     * or when they have been marked for removal.
     */
    private Map muteMap = new HashMap();
    
    /**
     * Map of PooledConnections marked for removal. Connections are
     * marked for removal from pcMap and muteMap in destroyObject.
     * Cleanup happens in makeObject.
     */
    private Map cleanupMap = new HashMap();
    
    /**
     * Map of PooledConnectionAndInfo instances
     */
    private WeakHashMap pcMap = new WeakHashMap();

    /**
     * Create a new <tt>KeyedPoolableConnectionFactory</tt>.
     * @param cpds the ConnectionPoolDataSource from which to obtain PooledConnection's
     * @param pool the {*link ObjectPool} in which to pool those {*link Connection}s
     * @param validationQuery a query to use to {*link #validateObject validate} {*link Connection}s.  Should return at least one row. May be <tt>null</tt>
     */
    public KeyedCPDSConnectionFactory(ConnectionPoolDataSource cpds,
                                      KeyedObjectPool pool,
                                      String validationQuery) {
        _cpds = cpds;
        _pool = pool;
        _pool.setFactory(this);
        _validationQuery = validationQuery;
    }

    /**
     * Create a new <tt>KeyedPoolableConnectionFactory</tt>.
     * @param cpds the ConnectionPoolDataSource from which to obtain
     * PooledConnections
     * @param pool the {@link ObjectPool} in which to pool those
     * {@link Connection}s
     * @param validationQuery a query to use to {@link #validateObject validate}
     * {@link Connection}s.  Should return at least one row. May be <tt>null</tt>
     * @param rollbackAfterValidation whether a rollback should be issued after
     * {@link #validateObject validating} {@link Connection}s.
     */
    public KeyedCPDSConnectionFactory(ConnectionPoolDataSource cpds, 
                                      KeyedObjectPool pool, 
                                      String validationQuery,
                                      boolean rollbackAfterValidation) {
        this(cpds , pool, validationQuery);
        _rollbackAfterValidation = rollbackAfterValidation;
    }

    /**
     * Sets the {@link ConnectionFactory} from which to obtain base {@link Connection}s.
     * @param connFactory the {*link ConnectionFactory} from which to obtain base {@link Connection}s
     */
    synchronized public void setCPDS(ConnectionPoolDataSource cpds) {
        _cpds = cpds;
    }

    /**
     * Sets the query I use to {*link #validateObject validate} {*link Connection}s.
     * Should return at least one row.
     * May be <tt>null</tt>
     * @param validationQuery a query to use to {*link #validateObject validate} {*link Connection}s.
     */
    synchronized public void setValidationQuery(String validationQuery) {
        _validationQuery = validationQuery;
    }

    /**
     * Sets whether a rollback should be issued after 
     * {*link #validateObject validating} 
     * {*link Connection}s.
     * @param rollbackAfterValidation whether a rollback should be issued after
     *        {*link #validateObject validating} 
     *        {*link Connection}s.
     */
    public synchronized void setRollbackAfterValidation(
            boolean rollbackAfterValidation) {
        _rollbackAfterValidation = rollbackAfterValidation;
    }

    /**
     * Sets the {*link ObjectPool} in which to pool {*link Connection}s.
     * @param pool the {*link ObjectPool} in which to pool those {*link Connection}s
     */
    synchronized public void setPool(KeyedObjectPool pool)
        throws SQLException {
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

    public KeyedObjectPool getPool() {
        return _pool;
    }

    /**
     * @param key
     * @throws SQLException if the connection could not be created.
     * @see org.apache.commons.pool.KeyedPoolableObjectFactory#makeObject(java.lang.Object)
     */
    public synchronized Object makeObject(Object key) throws Exception {
        Object obj = null;
        UserPassKey upkey = (UserPassKey)key;

        PooledConnection pc = null;
        String username = upkey.getUsername();
        String password = upkey.getPassword();
        if (username == null) {
            pc = _cpds.getPooledConnection();
        } else {
            pc = _cpds.getPooledConnection(username, password);
        }

        if (pc == null) {
            throw new IllegalStateException("Connection pool data source returned null from getPooledConnection");
        }

        // should we add this object as a listener or the pool.
        // consider the validateObject method in decision
        pc.addConnectionEventListener(this);
        obj = new PooledConnectionAndInfo(pc, username, password);
        pcMap.put(pc, obj);
        cleanupListeners();

        return obj;
    }

    /**
     * Closes the PooledConnection and marks it for removal from the
     * pcMap and for listener cleanup. Adds it to muteMap so connectionClosed
     * events generated by this or other actions are ignored.
     */
    public void destroyObject(Object key, Object obj) throws Exception {
        if (obj instanceof PooledConnectionAndInfo) {
            PooledConnection pc = ((PooledConnectionAndInfo)obj).getPooledConnection();
            cleanupMap.put(pc, null); // mark for cleanup
            muteMap.put(pc, null);    // ignore events until listener is removed
            pc.close(); 
        }
    }

    public boolean validateObject(Object key, Object obj) {
        boolean valid = false;
        if (obj instanceof PooledConnectionAndInfo) {
            PooledConnection pconn =
                ((PooledConnectionAndInfo)obj).getPooledConnection();
            String query = _validationQuery;
            if (null != query) {
                Connection conn = null;
                Statement stmt = null;
                ResultSet rset = null;
                // logical Connection from the PooledConnection must be closed
                // before another one can be requested and closing it will
                // generate an event. Keep track so we know not to return
                // the PooledConnection
                muteMap.put(pconn, null);
                try {
                    conn = pconn.getConnection();
                    stmt = conn.createStatement();
                    rset = stmt.executeQuery(query);
                    if (rset.next()) {
                        valid = true;
                    } else {
                        valid = false;
                    }
                    if (_rollbackAfterValidation) {
                        conn.rollback();
                    }
                } catch(Exception e) {
                    valid = false;
                } finally {
                    if (rset != null) {
                        try {
                            rset.close();
                        } catch (Throwable t) {
                            // ignore
                        }
                    }
                    if (stmt != null) {
                        try {
                            stmt.close();
                        } catch (Throwable t) {
                            // ignore
                        }
                    }
                    if (conn != null) {
                        try {
                            conn.close();
                        } catch (Throwable t) {
                            // ignore
                        }
                    }
                    muteMap.remove(pconn);
                }
            } else {
                valid = true;
            }
        } else {
            valid = false;
        }
        return valid;
    }

    public void passivateObject(Object key, Object obj) {
    }

    public void activateObject(Object key, Object obj) {
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
        PooledConnection pc = (PooledConnection)event.getSource();
        // if this event occurred because we were validating, or if this
        // connection has been marked for removal, ignore it
        // otherwise return the connection to the pool.
        if (!muteMap.containsKey(pc)) {
            PooledConnectionAndInfo info =
                (PooledConnectionAndInfo) pcMap.get(pc);
            if (info == null) {
                throw new IllegalStateException(NO_KEY_MESSAGE);
            }
            try {
                _pool.returnObject(info.getUserPassKey(), info);
            } catch (Exception e) {
                System.err.println("CLOSING DOWN CONNECTION AS IT COULD " +
                "NOT BE RETURNED TO THE POOL");
                cleanupMap.put(pc, null); // mark for cleanup
                muteMap.put(pc, null);    // ignore events until listener is removed
                try {
                    _pool.invalidateObject(info.getUserPassKey(), info);
                } catch (Exception e3) {
                    System.err.println("EXCEPTION WHILE DESTROYING OBJECT " +
                            info);
                    e3.printStackTrace();
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
        if (cleanupMap.containsKey(pc)) {
            return; // waiting for listener cleanup - ignore event
        }
        try {
            if (null != event.getSQLException()) {
                System.err
                    .println("CLOSING DOWN CONNECTION DUE TO INTERNAL ERROR (" +
                             event.getSQLException() + ")");
            }
            cleanupMap.put(pc, null); // mark for cleanup
            muteMap.put(pc, null);    // ignore events until listener is removed
        } catch (Exception ignore) {
            // ignore
        }

        PooledConnectionAndInfo info = (PooledConnectionAndInfo) pcMap.get(pc);
        if (info == null) {
            throw new IllegalStateException(NO_KEY_MESSAGE);
        }
        try {
            _pool.invalidateObject(info.getUserPassKey(), info);
        } catch (Exception e) {
            System.err.println("EXCEPTION WHILE DESTROYING OBJECT " + info);
            e.printStackTrace();
        }
    }
    
    /**
     * Cleans up pooled connections from cleanupMap.
     * 1) remove PoolableConnectionAndInfo wrapper from pcMap
     * 2) remove from MuteMap
     */
    private void cleanupListeners() { 
        try {
            Iterator iter = cleanupMap.keySet().iterator();
            while (iter.hasNext()) {
                PooledConnection pc = (PooledConnection) iter.next();
                pcMap.remove(pc);
                muteMap.remove(pc);
                try {
                    pc.removeConnectionEventListener(this);
                } catch (Exception ex) {
                    System.err.println("EXCEPTION REMOVING CONNECTION LISTENER ");
                    ex.printStackTrace(); 
                }
            }
        } catch (Exception e) {
            System.err.println("EXCEPTION CLEANING UP CONNECTION LISTENERS ");
            e.printStackTrace();  
        } finally {
            cleanupMap.clear();
        }
    }
}
