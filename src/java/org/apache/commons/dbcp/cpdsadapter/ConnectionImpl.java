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

import java.util.Map;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.CallableStatement;
import java.sql.Statement;
import java.sql.SQLWarning;
import java.sql.SQLException;

/**
 * This class is the <code>Connection</code> that will be returned
 * from <code>PooledConnectionImpl.getConnection()</code>.  
 * Most methods are wrappers around the jdbc 1.x <code>Connection</code>.  
 * A few exceptions include preparedStatement, close and isClosed.
 * In accordance with the jdbc specification this Connection cannot
 * be used after closed() is called.  Any further usage will result in an
 * SQLException.
 *
 * @author John D. McNally
 * @version $Revision: 1.9 $ $Date: 2004/02/28 12:18:17 $
 */
class ConnectionImpl implements Connection {
    private static final String CLOSED 
            = "Attempted to use Connection after closed() was called.";

    /** The JDBC database connection. */
    private Connection connection;

    /** The object that instantiated this object */
     private PooledConnectionImpl pooledConnection;

    /** Marks whether is Connection is still usable. */
    boolean isClosed;

    /**
     * Creates a <code>ConnectionImpl</code>. 
     *
     * @param pooledConnection The PooledConnection that is calling the ctor.
     * @param connection The JDBC 1.x Connection to wrap.
     */
    ConnectionImpl(PooledConnectionImpl pooledConnection, 
            Connection connection) {
        this.pooledConnection = pooledConnection;
        this.connection = connection;
        isClosed = false;
    }

    /**
     * The finalizer helps prevent <code>ConnectionPool</code> leakage.
     */
    protected void finalize() throws Throwable {
        if (!isClosed) {
            // If this DBConnection object is finalized while linked
            // to a ConnectionPool, it means that it was taken from a pool
            // and not returned.  We log this fact, close the underlying
            // Connection, and return it to the ConnectionPool.
            throw new SQLException("A ConnectionImpl was finalized "
                      + "without being closed which will cause leakage of "
                      + " PooledConnections from the ConnectionPool.");
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

    // ***********************************************************************
    // java.sql.Connection implementation using wrapped Connection
    // ***********************************************************************

    /**
     * Pass thru method to the wrapped jdbc 1.x {@link java.sql.Connection}.
     *
     * @exception SQLException if this connection is closed or an error occurs
     * in the wrapped connection.
     */
    public void clearWarnings() throws SQLException {
        assertOpen();
        connection.clearWarnings();
    }

    /**
     * Marks the Connection as closed, and notifies the pool that the
     * pooled connection is available.
     * In accordance with the jdbc specification this Connection cannot
     * be used after closed() is called.  Any further usage will result in an
     * SQLException.
     *
     * @exception SQLException The database connection couldn't be closed.
     */
    public void close() throws SQLException {
        assertOpen();
        isClosed = true;
        pooledConnection.notifyListeners();
    }

    /**
     * Pass thru method to the wrapped jdbc 1.x {@link java.sql.Connection}.
     *
     * @exception SQLException if this connection is closed or an error occurs
     * in the wrapped connection.
     */
    public void commit() throws SQLException {
        assertOpen();
        connection.commit();
    }

    /**
     * Pass thru method to the wrapped jdbc 1.x {@link java.sql.Connection}.
     *
     * @exception SQLException if this connection is closed or an error occurs
     * in the wrapped connection.
     */
    public Statement createStatement() throws SQLException {
        assertOpen();
        return connection.createStatement();
    }

    /**
     * Pass thru method to the wrapped jdbc 1.x {@link java.sql.Connection}.
     *
     * @exception SQLException if this connection is closed or an error occurs
     * in the wrapped connection.
     */
    public Statement createStatement(int resultSetType, 
                                     int resultSetConcurrency) 
            throws SQLException {
        assertOpen();
        return connection
                .createStatement(resultSetType, resultSetConcurrency);
    }

    /**
     * Pass thru method to the wrapped jdbc 1.x {@link java.sql.Connection}.
     *
     * @exception SQLException if this connection is closed or an error occurs
     * in the wrapped connection.
     */
    public boolean getAutoCommit() throws SQLException {
        assertOpen();
        return connection.getAutoCommit();
    }

    /**
     * Pass thru method to the wrapped jdbc 1.x {@link java.sql.Connection}.
     *
     * @exception SQLException if this connection is closed or an error occurs
     * in the wrapped connection.
     */
    public String getCatalog() throws SQLException {
        assertOpen();
        return connection.getCatalog();
    }

    /**
     * Pass thru method to the wrapped jdbc 1.x {@link java.sql.Connection}.
     *
     * @exception SQLException if this connection is closed or an error occurs
     * in the wrapped connection.
     */
    public DatabaseMetaData getMetaData() throws SQLException {
        assertOpen();
        return connection.getMetaData();
    }

    /**
     * Pass thru method to the wrapped jdbc 1.x {@link java.sql.Connection}.
     *
     * @exception SQLException if this connection is closed or an error occurs
     * in the wrapped connection.
     */
    public int getTransactionIsolation() throws SQLException {
        assertOpen();
        return connection.getTransactionIsolation();
    }

    /**
     * Pass thru method to the wrapped jdbc 1.x {@link java.sql.Connection}.
     *
     * @exception SQLException if this connection is closed or an error occurs
     * in the wrapped connection.
     */
    public Map getTypeMap() throws SQLException {
        assertOpen();
        return connection.getTypeMap();
    }

    /**
     * Pass thru method to the wrapped jdbc 1.x {@link java.sql.Connection}.
     *
     * @exception SQLException if this connection is closed or an error occurs
     * in the wrapped connection.
     */
    public SQLWarning getWarnings() throws SQLException {
        assertOpen();
        return connection.getWarnings();
    }

    /**
     * Returns true after close() is called, and false prior to that.
     *
     * @return a <code>boolean</code> value
     */
    public boolean isClosed() {
        return isClosed;
    }

    /**
     * Pass thru method to the wrapped jdbc 1.x {@link java.sql.Connection}.
     *
     * @exception SQLException if this connection is closed or an error occurs
     * in the wrapped connection.
     */
    public boolean isReadOnly() throws SQLException {
        assertOpen();
        return connection.isReadOnly();
    }

    /**
     * Pass thru method to the wrapped jdbc 1.x {@link java.sql.Connection}.
     *
     * @exception SQLException if this connection is closed or an error occurs
     * in the wrapped connection.
     */
    public String nativeSQL(String sql) throws SQLException {
        assertOpen();
        return connection.nativeSQL(sql);
    }    

    /**
     * Pass thru method to the wrapped jdbc 1.x {@link java.sql.Connection}.
     *
     * @exception SQLException if this connection is closed or an error occurs
     * in the wrapped connection.
     */
    public CallableStatement prepareCall(String sql) throws SQLException {
        assertOpen();
        return connection.prepareCall(sql);
    }

    /**
     * Pass thru method to the wrapped jdbc 1.x {@link java.sql.Connection}.
     *
     * @exception SQLException if this connection is closed or an error occurs
     * in the wrapped connection.
     */
    public CallableStatement prepareCall(String sql, int resultSetType, 
                                         int resultSetConcurrency) 
            throws SQLException {
        assertOpen();
        return connection.prepareCall(sql, resultSetType, resultSetConcurrency);
    }

    /**
     * If pooling of <code>PreparedStatement</code>s is turned on in the
     * {@link DriverAdapterCPDS}, a pooled object may be returned, otherwise
     * delegate to the wrapped jdbc 1.x {@link java.sql.Connection}.
     *
     * @exception SQLException if this connection is closed or an error occurs
     * in the wrapped connection.
     */
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        assertOpen();
        return pooledConnection.prepareStatement(sql);
    }

    /**
     * If pooling of <code>PreparedStatement</code>s is turned on in the
     * {@link DriverAdapterCPDS}, a pooled object may be returned, otherwise
     * delegate to the wrapped jdbc 1.x {@link java.sql.Connection}.
     *
     * @exception SQLException if this connection is closed or an error occurs
     * in the wrapped connection.
     */
    public PreparedStatement prepareStatement(String sql, int resultSetType, 
                                              int resultSetConcurrency) 
            throws SQLException {
        assertOpen();
        return pooledConnection
            .prepareStatement(sql, resultSetType, resultSetConcurrency);
    }

    /**
     * Pass thru method to the wrapped jdbc 1.x {@link java.sql.Connection}.
     *
     * @exception SQLException if this connection is closed or an error occurs
     * in the wrapped connection.
     */
    public void rollback() throws SQLException {
        assertOpen();
        connection.rollback();
    }

    /**
     * Pass thru method to the wrapped jdbc 1.x {@link java.sql.Connection}.
     *
     * @exception SQLException if this connection is closed or an error occurs
     * in the wrapped connection.
     */
    public void setAutoCommit(boolean b) throws SQLException {
        assertOpen();
        connection.setAutoCommit(b);
    }

    /**
     * Pass thru method to the wrapped jdbc 1.x {@link java.sql.Connection}.
     *
     * @exception SQLException if this connection is closed or an error occurs
     * in the wrapped connection.
     */
    public void setCatalog(String catalog) throws SQLException {
        assertOpen();
        connection.setCatalog(catalog);
    }

    /**
     * Pass thru method to the wrapped jdbc 1.x {@link java.sql.Connection}.
     *
     * @exception SQLException if this connection is closed or an error occurs
     * in the wrapped connection.
     */
    public void setReadOnly(boolean readOnly) throws SQLException {
        assertOpen();
        connection.setReadOnly(readOnly);
    }

    /**
     * Pass thru method to the wrapped jdbc 1.x {@link java.sql.Connection}.
     *
     * @exception SQLException if this connection is closed or an error occurs
     * in the wrapped connection.
     */
    public void setTransactionIsolation(int level) throws SQLException {
        assertOpen();
        connection.setTransactionIsolation(level);
    }

    /**
     * Pass thru method to the wrapped jdbc 1.x {@link java.sql.Connection}.
     *
     * @exception SQLException if this connection is closed or an error occurs
     * in the wrapped connection.
     */
    public void setTypeMap(Map map) throws SQLException {
        assertOpen();
        connection.setTypeMap(map);
    }

    // ------------------- JDBC 3.0 -----------------------------------------
    // Will be commented by the build process on a JDBC 2.0 system

/* JDBC_3_ANT_KEY_BEGIN */

    public int getHoldability() throws SQLException {
        assertOpen();
        return connection.getHoldability();
    }

    public void setHoldability(int holdability) throws SQLException {
        assertOpen();
        connection.setHoldability(holdability);
    }

    public java.sql.Savepoint setSavepoint() throws SQLException {
        assertOpen();
        return connection.setSavepoint();
    }

    public java.sql.Savepoint setSavepoint(String name) throws SQLException {
        assertOpen();
        return connection.setSavepoint(name);
    }

    public void rollback(java.sql.Savepoint savepoint) throws SQLException {
        assertOpen();
        connection.rollback(savepoint);
    }

    public void releaseSavepoint(java.sql.Savepoint savepoint) 
            throws SQLException {
        assertOpen();
        connection.releaseSavepoint(savepoint);
    }

    public Statement createStatement(int resultSetType,
                                     int resultSetConcurrency,
                                     int resultSetHoldability)
            throws SQLException {
        assertOpen();
        return connection.createStatement(resultSetType, resultSetConcurrency,
                                     resultSetHoldability);
    }

    public PreparedStatement prepareStatement(String sql, int resultSetType,
                                              int resultSetConcurrency,
                                              int resultSetHoldability)
            throws SQLException {
        assertOpen();
        return connection.prepareStatement(sql, resultSetType,
                                      resultSetConcurrency,
                                      resultSetHoldability);
    }

    public CallableStatement prepareCall(String sql, int resultSetType,
                                         int resultSetConcurrency,
                                         int resultSetHoldability)
            throws SQLException {
        assertOpen();
        return connection.prepareCall(sql, resultSetType,
                                 resultSetConcurrency,
                                 resultSetHoldability);
    }

    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys)
            throws SQLException {
        assertOpen();
        return connection.prepareStatement(sql, autoGeneratedKeys);
    }

    public PreparedStatement prepareStatement(String sql, int columnIndexes[])
            throws SQLException {
        assertOpen();
        return connection.prepareStatement(sql, columnIndexes);
    }

    public PreparedStatement prepareStatement(String sql, String columnNames[])
            throws SQLException {
        assertOpen();
        return connection.prepareStatement(sql, columnNames);
    }

/* JDBC_3_ANT_KEY_END */
}
