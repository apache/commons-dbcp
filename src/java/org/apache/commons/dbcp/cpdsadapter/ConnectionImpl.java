/*
 * $Source: /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons//dbcp/src/java/org/apache/commons/dbcp/cpdsadapter/ConnectionImpl.java,v $
 * $Revision: 1.6 $
 * $Date: 2003/08/22 16:08:32 $
 *
 * ====================================================================
 *
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999-2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowledgement:
 *       "This product includes software developed by the
 *        Apache Software Foundation - http://www.apache.org/"
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Commons", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * http://www.apache.org/
 *
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
 * @author <a href="mailto:jmcnally@collab.net">John D. McNally</a>
 * @version $Id: ConnectionImpl.java,v 1.6 2003/08/22 16:08:32 dirkv Exp $
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
