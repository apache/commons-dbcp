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

package org.apache.commons.dbcp.cpdsadapter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Vector;

import javax.sql.ConnectionEvent;
import javax.sql.ConnectionEventListener;
import javax.sql.PooledConnection;

import org.apache.commons.dbcp.DelegatingConnection;
import org.apache.commons.dbcp.DelegatingPreparedStatement;
import org.apache.commons.dbcp.SQLNestedException;
import org.apache.commons.pool.KeyedObjectPool;
import org.apache.commons.pool.KeyedPoolableObjectFactory;

/**
 * Implementation of PooledConnection that is returned by
 * PooledConnectionDataSource.
 *
 * @author John D. McNally
 * @version $Revision: 1.16 $ $Date: 2004/03/07 11:19:25 $
 */
class PooledConnectionImpl 
        implements PooledConnection, KeyedPoolableObjectFactory {
    private static final String CLOSED 
            = "Attempted to use PooledConnection after closed() was called.";

    /**
     * The JDBC database connection that represents the physical db connection.
     */
    private Connection connection = null;
    
    /**
     * A DelegatingConnection used to create a PoolablePreparedStatementStub
     */
    private DelegatingConnection delegatingConnection = null;

    /**
     * The JDBC database logical connection.
     */
    private Connection logicalConnection = null;

    /**
     * ConnectionEventListeners
     */
    private Vector eventListeners;

    /**
     * flag set to true, once close() is called.
     */
    boolean isClosed;

    /** My pool of {*link PreparedStatement}s. */
    protected KeyedObjectPool pstmtPool = null;


    /**
     * Wrap the real connection.
     */
    PooledConnectionImpl(Connection connection, KeyedObjectPool pool) {
        this.connection = connection;
        if (connection instanceof DelegatingConnection) {
            this.delegatingConnection = (DelegatingConnection) connection;
        } else {
            this.delegatingConnection = new DelegatingConnection(connection);   
        }
        eventListeners = new Vector();
        isClosed = false;
        if (pool != null) {
            pstmtPool = pool;
            pstmtPool.setFactory(this);            
        }
    }

    /**
     * Add an event listener.
     */
    public void addConnectionEventListener(ConnectionEventListener listener) {
        if (!eventListeners.contains(listener)) {
            eventListeners.add(listener);
        }
    }

    /**
     * Closes the physical connection and marks this 
     * <code>PooledConnection</code> so that it may not be used 
     * to generate any more logical <code>Connection</code>s.
     *
     * @exception SQLException if an error occurs
     */
    public void close() throws SQLException {        
        assertOpen();
        isClosed = true;
        try {
            if (pstmtPool != null) {
                try {
                    pstmtPool.close();
                } finally {
                    pstmtPool = null;
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new SQLNestedException("Cannot close connection (return to pool failed)", e);
        } finally {
            try {
                connection.close();
            } finally {
                connection = null;
            }
        }
    }

    /**
     * Throws an SQLException, if isClosed() is true
     */
    private void assertOpen() throws SQLException {
        if (isClosed) {
            throw new SQLException(CLOSED);
        }
    }

    /**
     * Returns a JDBC connection.
     *
     * @return The database connection.
     */
    public Connection getConnection() throws SQLException {
        assertOpen();
        // make sure the last connection is marked as closed
        if (logicalConnection != null && !logicalConnection.isClosed()) {
            // should notify pool of error so the pooled connection can
            // be removed !FIXME!
            throw new SQLException("PooledConnection was reused, without" 
                    + "its previous Connection being closed.");
        }

        // the spec requires that this return a new Connection instance.
        logicalConnection = new ConnectionImpl(this, connection);
        return logicalConnection;
    }

    /**
     * Remove an event listener.
     */
    public void removeConnectionEventListener(
            ConnectionEventListener listener) {
        eventListeners.remove(listener);
    }

    /**
     * Closes the physical connection and checks that the logical connection
     * was closed as well.
     */
    protected void finalize() throws Throwable {
        // Closing the Connection ensures that if anyone tries to use it,
        // an error will occur.
        try {
            connection.close();
        } catch (Exception ignored) {
        }

        // make sure the last connection is marked as closed
        if (logicalConnection != null && !logicalConnection.isClosed()) {
            throw new SQLException("PooledConnection was gc'ed, without" 
                    + "its last Connection being closed.");
        }        
    }

    /**
     * sends a connectionClosed event.
     */
    void notifyListeners() {
        ConnectionEvent event = new ConnectionEvent(this);
        Iterator i = eventListeners.iterator();
        while (i.hasNext()) {
            ((ConnectionEventListener) i.next()).connectionClosed(event);
        }
    }

    // -------------------------------------------------------------------
    // The following code implements a PreparedStatement pool

    /**
     * Create or obtain a {*link PreparedStatement} from my pool.
     * @return a {*link PoolablePreparedStatement}
     */
    PreparedStatement prepareStatement(String sql) throws SQLException {
        if (pstmtPool == null) {
            return connection.prepareStatement(sql);
        } else {
            try {
                return (PreparedStatement) 
                        pstmtPool.borrowObject(createKey(sql));
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new SQLNestedException("Borrow prepareStatement from pool failed", e);
            }
        }
    }

    /**
     * Create or obtain a {*link PreparedStatement} from my pool.
     * @return a {*link PoolablePreparedStatement}
     */
    PreparedStatement prepareStatement(String sql, int resultSetType, 
                                       int resultSetConcurrency) 
            throws SQLException {
        if (pstmtPool == null) {
            return connection.prepareStatement(sql, resultSetType, resultSetConcurrency);
        } else {
            try {
                return (PreparedStatement) pstmtPool.borrowObject(
                    createKey(sql,resultSetType,resultSetConcurrency));
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new SQLNestedException("Borrow prepareStatement from pool failed", e);
            }
        }
    }

    /**
     * Create a {*link PooledConnectionImpl.PStmtKey} for the given arguments.
     */
    protected Object createKey(String sql, int resultSetType, 
                               int resultSetConcurrency) {
        return new PStmtKey(normalizeSQL(sql), resultSetType,
                            resultSetConcurrency);
    }

    /**
     * Create a {*link PooledConnectionImpl.PStmtKey} for the given arguments.
     */
    protected Object createKey(String sql) {
        return new PStmtKey(normalizeSQL(sql));
    }

    /**
     * Normalize the given SQL statement, producing a
     * cannonical form that is semantically equivalent to the original.
     */
    protected String normalizeSQL(String sql) {
        return sql.trim();
    }

    /**
     * My {*link KeyedPoolableObjectFactory} method for creating
     * {*link PreparedStatement}s.
     * @param obj the key for the {*link PreparedStatement} to be created
     */
    public Object makeObject(Object obj) throws Exception {
        if (null == obj || !(obj instanceof PStmtKey)) {
            throw new IllegalArgumentException();
        } else {
            // _openPstmts++;
            PStmtKey key = (PStmtKey)obj;
            if (null == key._resultSetType 
                    && null == key._resultSetConcurrency) {
                return new PoolablePreparedStatementStub(
                        connection.prepareStatement(key._sql),
                        key, pstmtPool, delegatingConnection);
            } else {
                return new PoolablePreparedStatementStub(
                        connection.prepareStatement(key._sql,
                        key._resultSetType.intValue(),
                        key._resultSetConcurrency.intValue()),
                        key, pstmtPool, delegatingConnection);
            }
        }
    }

    /**
     * My {*link KeyedPoolableObjectFactory} method for destroying
     * {*link PreparedStatement}s.
     * @param key ignored
     * @param obj the {*link PreparedStatement} to be destroyed.
     */
    public void destroyObject(Object key, Object obj) throws Exception {
        //_openPstmts--;
        if (obj instanceof DelegatingPreparedStatement) {
            ((DelegatingPreparedStatement) obj).getInnermostDelegate().close();
        } else {
            ((PreparedStatement) obj).close();
        }
    }

    /**
     * My {*link KeyedPoolableObjectFactory} method for validating
     * {*link PreparedStatement}s.
     * @param key ignored
     * @param obj ignored
     * @return <tt>true</tt>
     */
    public boolean validateObject(Object key, Object obj) {
        return true;
    }

    /**
     * My {*link KeyedPoolableObjectFactory} method for activating
     * {*link PreparedStatement}s.
     * @param key ignored
     * @param obj ignored
     */
    public void activateObject(Object key, Object obj) throws Exception {
        ((PoolablePreparedStatementStub) obj).activate();
    }

    /**
     * My {*link KeyedPoolableObjectFactory} method for passivating
     * {*link PreparedStatement}s.  Currently invokes {*link PreparedStatement#clearParameters}.
     * @param key ignored
     * @param obj a {*link PreparedStatement}
     */
    public void passivateObject(Object key, Object obj) throws Exception {
        ((PreparedStatement) obj).clearParameters();
        ((PoolablePreparedStatementStub) obj).passivate();
    }

    /**
     * A key uniquely identifying {*link PreparedStatement}s.
     */
    class PStmtKey {
        protected String _sql = null;
        protected Integer _resultSetType = null;
        protected Integer _resultSetConcurrency = null;

        PStmtKey(String sql) {
            _sql = sql;
        }

        PStmtKey(String sql, int resultSetType, int resultSetConcurrency) {
            _sql = sql;
            _resultSetType = new Integer(resultSetType);
            _resultSetConcurrency = new Integer(resultSetConcurrency);
        }

        public boolean equals(Object that) {
            try {
                PStmtKey key = (PStmtKey) that;
                return(((null == _sql && null == key._sql) || _sql.equals(key._sql)) &&
                       ((null == _resultSetType && null == key._resultSetType) || _resultSetType.equals(key._resultSetType)) &&
                       ((null == _resultSetConcurrency && null == key._resultSetConcurrency) || _resultSetConcurrency.equals(key._resultSetConcurrency))
                      );
            } catch (ClassCastException e) {
                return false;
            } catch (NullPointerException e) {
                return false;
            }
        }

        public int hashCode() {
            return(null == _sql ? 0 : _sql.hashCode());
        }

        public String toString() {
            StringBuffer buf = new StringBuffer();
            buf.append("PStmtKey: sql=");
            buf.append(_sql);
            buf.append(", resultSetType=");
            buf.append(_resultSetType);
            buf.append(", resultSetConcurrency=");
            buf.append(_resultSetConcurrency);
            return buf.toString();
        }
    }
}
