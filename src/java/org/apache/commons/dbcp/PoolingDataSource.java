/*
 * $Source: /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons//dbcp/src/java/org/apache/commons/dbcp/PoolingDataSource.java,v $
 * $Revision: 1.11 $
 * $Date: 2003/10/09 21:04:44 $
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
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowledgements normally appear.
 *
 * 4. The names "The Jakarta Project", "Commons", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Software Foundation.
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

package org.apache.commons.dbcp;

import java.io.PrintWriter;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.sql.DataSource;

import org.apache.commons.pool.ObjectPool;

/**
 * A simple {@link DataSource} implementation that obtains
 * {@link Connection}s from the specified {@link ObjectPool}.
 *
 * @author Rodney Waldhoff
 * @author Glenn L. Nielsen
 * @author James House (<a href="mailto:james@interobjective.com">james@interobjective.com</a>)
 * @author Dirk Verbeeck
 * @version $Id: PoolingDataSource.java,v 1.11 2003/10/09 21:04:44 rdonkin Exp $
 */
public class PoolingDataSource implements DataSource {

    /** Controls access to the underlying connection */
    private boolean accessToUnderlyingConnectionAllowed = false; 

    public PoolingDataSource() {
        this(null);
    }

    public PoolingDataSource(ObjectPool pool) {
        _pool = pool;
    }

    public void setPool(ObjectPool pool) throws IllegalStateException, NullPointerException {
        if(null != _pool) {
            throw new IllegalStateException("Pool already set");
        } else if(null == pool) {
            throw new NullPointerException("Pool must not be null.");
        } else {
            _pool = pool;
        }
    }

    /**
     * Returns the value of the accessToUnderlyingConnectionAllowed property.
     * 
     * @return true if access to the underlying is allowed, false otherwise.
     */
    public boolean isAccessToUnderlyingConnectionAllowed() {
        return this.accessToUnderlyingConnectionAllowed;
    }

    /**
     * Sets the value of the accessToUnderlyingConnectionAllowed property.
     * It controls if the PoolGuard allows access to the underlying connection.
     * (Default: false)
     * 
     * @param allow Access to the underlying connection is granted when true.
     */
    public void setAccessToUnderlyingConnectionAllowed(boolean allow) {
        this.accessToUnderlyingConnectionAllowed = allow;
    }
    
    //--- DataSource methods -----------------------------------------

    /**
     * Return a {@link java.sql.Connection} from my pool,
     * according to the contract specified by {@link ObjectPool#borrowObject}.
     */
    public Connection getConnection() throws SQLException {
        try {
            Connection conn = (Connection)(_pool.borrowObject());
            if (conn != null) {
                conn = new PoolGuardConnectionWrapper(conn);
            } 
            return conn;
        } catch(SQLException e) {
            throw e;
        } catch(NoSuchElementException e) {
            throw new SQLNestedException("Cannot get a connection, pool exhausted", e);
        } catch(RuntimeException e) {
            throw e;
        } catch(Exception e) {
            throw new SQLNestedException("Cannot get a connection, general error", e);
        }
    }

    /**
     * Throws {@link UnsupportedOperationException}
     * @throws UnsupportedOperationException
     */
    public Connection getConnection(String uname, String passwd) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns my log writer.
     * @return my log writer
     * @see DataSource#getLogWriter
     */
    public PrintWriter getLogWriter() {
        return _logWriter;
    }

    /**
     * Throws {@link UnsupportedOperationException}.
     * Do this configuration within my {@link ObjectPool}.
     * @throws UnsupportedOperationException
     */
    public int getLoginTimeout() {
        throw new UnsupportedOperationException();
    }

    /**
     * Throws {@link UnsupportedOperationException}.
     * Do this configuration within my {@link ObjectPool}.
     * @throws UnsupportedOperationException
     */
    public void setLoginTimeout(int seconds) {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets my log writer.
     * @see DataSource#setLogWriter
     */
    public void setLogWriter(PrintWriter out) {
        _logWriter = out;
    }

    /** My log writer. */
    protected PrintWriter _logWriter = null;

    protected ObjectPool _pool = null;

    /**
     * PoolGuardConnectionWrapper is a Connection wrapper that makes sure a 
     * closed connection cannot be used anymore.
     */
    private class PoolGuardConnectionWrapper extends DelegatingConnection {

        private Connection delegate;
    
        PoolGuardConnectionWrapper(Connection delegate) {
            super(delegate);
            this.delegate = delegate;
        }

        protected void checkOpen() throws SQLException {
            if(delegate == null) {
                throw new SQLException("Connection is closed.");
            }
        }
    
        public void close() throws SQLException {
            checkOpen();
            this.delegate.close();
            this.delegate = null;
            super.setDelegate(null);
        }

        public boolean isClosed() throws SQLException {
            if (delegate == null) {
                return true;
            }
            return delegate.isClosed();
        }

        public void clearWarnings() throws SQLException {
            checkOpen();
            delegate.clearWarnings();
        }

        public void commit() throws SQLException {
            checkOpen();
            delegate.commit();
        }

        public Statement createStatement() throws SQLException {
            checkOpen();
            return delegate.createStatement();
        }

        public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
            checkOpen();
            return delegate.createStatement(resultSetType, resultSetConcurrency);
        }

        public boolean equals(Object obj) {
            if (delegate == null){
                return false;
            }
            return delegate.equals(obj);
        }

        public boolean getAutoCommit() throws SQLException {
            checkOpen();
            return delegate.getAutoCommit();
        }

        public String getCatalog() throws SQLException {
            checkOpen();
            return delegate.getCatalog();
        }

        public DatabaseMetaData getMetaData() throws SQLException {
            checkOpen();
            return delegate.getMetaData();
        }

        public int getTransactionIsolation() throws SQLException {
            checkOpen();
            return delegate.getTransactionIsolation();
        }

        public Map getTypeMap() throws SQLException {
            checkOpen();
            return delegate.getTypeMap();
        }

        public SQLWarning getWarnings() throws SQLException {
            checkOpen();
            return delegate.getWarnings();
        }

        public int hashCode() {
            if (delegate == null){
                return 0;
            }
            return delegate.hashCode();
        }

        public boolean isReadOnly() throws SQLException {
            checkOpen();
            return delegate.isReadOnly();
        }

        public String nativeSQL(String sql) throws SQLException {
            checkOpen();
            return delegate.nativeSQL(sql);
        }

        public CallableStatement prepareCall(String sql) throws SQLException {
            checkOpen();
            return delegate.prepareCall(sql);
        }

        public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
            checkOpen();
            return delegate.prepareCall(sql, resultSetType, resultSetConcurrency);
        }

        public PreparedStatement prepareStatement(String sql) throws SQLException {
            checkOpen();
            return delegate.prepareStatement(sql);
        }

        public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
            checkOpen();
            return delegate.prepareStatement(sql, resultSetType, resultSetConcurrency);
        }

        public void rollback() throws SQLException {
            checkOpen();
            delegate.rollback();
        }

        public void setAutoCommit(boolean autoCommit) throws SQLException {
            checkOpen();
            delegate.setAutoCommit(autoCommit);
        }

        public void setCatalog(String catalog) throws SQLException {
            checkOpen();
            delegate.setCatalog(catalog);
        }

        public void setReadOnly(boolean readOnly) throws SQLException {
            checkOpen();
            delegate.setReadOnly(readOnly);
        }

        public void setTransactionIsolation(int level) throws SQLException {
            checkOpen();
            delegate.setTransactionIsolation(level);
        }

        public void setTypeMap(Map map) throws SQLException {
            checkOpen();
            delegate.setTypeMap(map);
        }

        public String toString() {
            if (delegate == null){
                return null;
            }
            return delegate.toString();
        }

        // ------------------- JDBC 3.0 -----------------------------------------
        // Will be commented by the build process on a JDBC 2.0 system

/* JDBC_3_ANT_KEY_BEGIN */

        public int getHoldability() throws SQLException {
            checkOpen();
            return delegate.getHoldability();
        }
    
        public void setHoldability(int holdability) throws SQLException {
            checkOpen();
            delegate.setHoldability(holdability);
        }

        public java.sql.Savepoint setSavepoint() throws SQLException {
            checkOpen();
            return delegate.setSavepoint();
        }

        public java.sql.Savepoint setSavepoint(String name) throws SQLException {
            checkOpen();
            return delegate.setSavepoint(name);
        }

        public void releaseSavepoint(java.sql.Savepoint savepoint) throws SQLException {
            checkOpen();
            delegate.releaseSavepoint(savepoint);
        }

        public void rollback(java.sql.Savepoint savepoint) throws SQLException {
            checkOpen();
            delegate.rollback(savepoint);
        }

        public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
            checkOpen();
            return delegate.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
        }

        public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
            checkOpen();
            return delegate.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
        }

        public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
            checkOpen();
            return delegate.prepareStatement(sql, autoGeneratedKeys);
        }

        public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
            checkOpen();
            return delegate.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
        }

        public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
            checkOpen();
            return delegate.prepareStatement(sql, columnIndexes);
        }

        public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
            checkOpen();
            return delegate.prepareStatement(sql, columnNames);
        }

/* JDBC_3_ANT_KEY_END */

        /**
         * @see org.apache.commons.dbcp.DelegatingConnection#getDelegate()
         */
        public Connection getDelegate() {
            if (isAccessToUnderlyingConnectionAllowed()) {
                return super.getDelegate();
            } else {
                return null;
            }
        }

        /**
         * @see org.apache.commons.dbcp.DelegatingConnection#getInnermostDelegate()
         */
        public Connection getInnermostDelegate() {
            if (isAccessToUnderlyingConnectionAllowed()) {
                return super.getInnermostDelegate();
            } else {
                return null;
            }
        }
    }
}
