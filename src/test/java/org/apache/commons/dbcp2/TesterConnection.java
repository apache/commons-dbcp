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

package org.apache.commons.dbcp2;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

/**
 * A dummy {@link Connection}, for testing purposes.
 */
public class TesterConnection extends AbandonedTrace implements Connection {

    protected boolean open = true;
    protected boolean aborted;
    protected boolean autoCommit = true;
    protected int transactionIsolation = 1;
    protected final DatabaseMetaData metaData = new TesterDatabaseMetaData();
    protected String catalog;
    protected String schema;
    protected Map<String,Class<?>> typeMap;
    protected boolean readOnly;
    protected SQLWarning warnings;
    protected final String userName;
    protected Exception failure;
    protected boolean sqlExceptionOnClose;

    TesterConnection(final String userName,
            @SuppressWarnings("unused") final String password) {
        this.userName = userName;
    }

    @Override
    public void abort(final Executor executor) throws SQLException {
        checkFailure();
        aborted = true;
        open = false;
    }

    protected void checkFailure() throws SQLException {
        if (failure != null) {
            if (failure instanceof SQLException) {
                throw (SQLException) failure;
            }
            throw new SQLException("TesterConnection failure", failure);
        }
    }

    protected void checkOpen() throws SQLException {
        if (!open) {
            throw new SQLException("Connection is closed.");
        }
        checkFailure();
    }

    @Override
    public void clearWarnings() throws SQLException {
        checkOpen();
        warnings = null;
    }

    @Override
    public void close() throws SQLException {
        checkFailure();
        open = false;
    }

    @Override
    public void commit() throws SQLException {
        checkOpen();
        if (isReadOnly()) {
            throw new SQLException("Cannot commit a readonly connection");
        }
    }

    @Override
    public Array createArrayOf(final String typeName, final Object[] elements) throws SQLException {
        throw new SQLException("Not implemented.");
    }

    @Override
    public Blob createBlob() throws SQLException {
        throw new SQLException("Not implemented.");
    }

    @Override
    public Clob createClob() throws SQLException {
        throw new SQLException("Not implemented.");
    }

    @Override
    public NClob createNClob() throws SQLException {
        throw new SQLException("Not implemented.");
    }

    @Override
    public SQLXML createSQLXML() throws SQLException {
        throw new SQLException("Not implemented.");
    }

    @Override
    public Statement createStatement() throws SQLException {
        checkOpen();
        return new TesterStatement(this);
    }

    @Override
    public Statement createStatement(final int resultSetType, final int resultSetConcurrency) throws SQLException {
        checkOpen();
        return new TesterStatement(this);
    }

    @Override
    public Statement createStatement(final int resultSetType,
                                     final int resultSetConcurrency,
                                     final int resultSetHoldability)
        throws SQLException {
        return createStatement();
    }

    @Override
    public Struct createStruct(final String typeName, final Object[] attributes) throws SQLException {
        throw new SQLException("Not implemented.");
    }

    @Override
    public boolean getAutoCommit() throws SQLException {
        checkOpen();
        return autoCommit;
    }

    @Override
    public String getCatalog() throws SQLException {
        checkOpen();
        return catalog;
    }

    @Override
    public Properties getClientInfo() throws SQLException {
        throw new SQLException("Not implemented.");
    }

    @Override
    public String getClientInfo(final String name) throws SQLException {
        throw new SQLException("Not implemented.");
    }

    @Override
    public int getHoldability() throws SQLException {
        throw new SQLException("Not implemented.");
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        checkOpen();
        return metaData;
    }

    @Override
    public int getNetworkTimeout() throws SQLException {
        throw new SQLException("Not implemented.");
    }

    @Override
    public String getSchema() throws SQLException {
        checkOpen();
        return schema;
    }

    @Override
    public int getTransactionIsolation() throws SQLException {
        checkOpen();
        return transactionIsolation;
    }

    @Override
    public Map<String,Class<?>> getTypeMap() throws SQLException {
        checkOpen();
        return typeMap;
    }

    public String getUserName() {
        return this.userName;
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        checkOpen();
        return warnings;
    }

    public boolean isAborted() throws SQLException {
        checkFailure();
        return aborted;
    }

    @Override
    public boolean isClosed() throws SQLException {
        checkFailure();
        return !open;
    }

    @Override
    public boolean isReadOnly() throws SQLException {
        checkOpen();
        return readOnly;
    }

    public boolean isSqlExceptionOnClose() {
        return sqlExceptionOnClose;
    }

    @Override
    public boolean isValid(final int timeout) throws SQLException {
        return open;
    }

    @Override
    public boolean isWrapperFor(final Class<?> iface) throws SQLException {
        throw new SQLException("Not implemented.");
    }

    @Override
    public String nativeSQL(final String sql) throws SQLException {
        checkOpen();
        return sql;
    }

    @Override
    public CallableStatement prepareCall(final String sql) throws SQLException {
        checkOpen();
        if ("warning".equals(sql)) {
            setWarnings(new SQLWarning("warning in prepareCall"));
        }
        return new TesterCallableStatement(this, sql);
    }

    @Override
    public CallableStatement prepareCall(final String sql, final int resultSetType, final int resultSetConcurrency) throws SQLException {
        checkOpen();
        return new TesterCallableStatement(this, sql, resultSetType, resultSetConcurrency);
    }

    @Override
    public CallableStatement prepareCall(final String sql, final int resultSetType,
                                         final int resultSetConcurrency,
                                         final int resultSetHoldability)
        throws SQLException {
        checkOpen();
        return new TesterCallableStatement(this, sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public PreparedStatement prepareStatement(final String sql) throws SQLException {
        checkOpen();
        if ("null".equals(sql)) {
            return null;
        }
        if ("invalid".equals(sql)) {
            throw new SQLException("invalid query");
        }
        if ("broken".equals(sql)) {
            throw new SQLException("broken connection");
        }
        return new TesterPreparedStatement(this, sql);
    }

    @Override
    public PreparedStatement prepareStatement(final String sql, final int autoGeneratedKeys)
        throws SQLException {
        checkOpen();
        return new TesterPreparedStatement(this, sql, autoGeneratedKeys);
    }

    @Override
    public PreparedStatement prepareStatement(final String sql, final int resultSetType, final int resultSetConcurrency) throws SQLException {
        checkOpen();
        return new TesterPreparedStatement(this, sql, resultSetType, resultSetConcurrency);
    }

    @Override
    public PreparedStatement prepareStatement(final String sql, final int resultSetType,
                                              final int resultSetConcurrency,
                                              final int resultSetHoldability)
        throws SQLException {
        checkOpen();
        return new TesterPreparedStatement(this, sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public PreparedStatement prepareStatement(final String sql, final int[] columnIndexes)
        throws SQLException {
        return new TesterPreparedStatement(this, sql, columnIndexes);
    }

    @Override
    public PreparedStatement prepareStatement(final String sql, final String[] columnNames)
        throws SQLException {
        return new TesterPreparedStatement(this, sql, columnNames);
    }

    @Override
    public void releaseSavepoint(final java.sql.Savepoint savepoint) throws SQLException {
        throw new SQLException("Not implemented.");
    }

    @Override
    public void rollback() throws SQLException {
        checkOpen();
        if (isReadOnly()) {
            throw new SQLException("Cannot rollback a readonly connection");
        }
        if (getAutoCommit()) {
            throw new SQLException("Cannot rollback a connection in auto-commit");
        }
    }

    @Override
    public void rollback(final java.sql.Savepoint savepoint) throws SQLException {
        throw new SQLException("Not implemented.");
    }

    @Override
    public void setAutoCommit(final boolean autoCommit) throws SQLException {
        checkOpen();
        this.autoCommit = autoCommit;
    }

    @Override
    public void setCatalog(final String catalog) throws SQLException {
        checkOpen();
        this.catalog = catalog;
    }

    @Override
    public void setClientInfo(final Properties properties) throws SQLClientInfoException {
        throw new SQLClientInfoException();
    }

    @Override
    public void setClientInfo(final String name, final String value) throws SQLClientInfoException {
        throw new SQLClientInfoException();
    }

    public void setFailure(final Exception failure) {
        this.failure = failure;
    }

    @Override
    public void setHoldability(final int holdability) throws SQLException {
        throw new SQLException("Not implemented.");
    }

    @Override
    public void setNetworkTimeout(final Executor executor, final int milliseconds)
            throws SQLException {
        throw new SQLException("Not implemented.");
    }

    @Override
    public void setReadOnly(final boolean readOnly) throws SQLException {
        checkOpen();
        this.readOnly = readOnly;
    }

    @Override
    public java.sql.Savepoint setSavepoint() throws SQLException {
        throw new SQLException("Not implemented.");
    }

    @Override
    public java.sql.Savepoint setSavepoint(final String name) throws SQLException {
        throw new SQLException("Not implemented.");
    }

    @Override
    public void setSchema(final String schema) throws SQLException {
        checkOpen();
        this.schema= schema;
    }

    public void setSqlExceptionOnClose(final boolean sqlExceptionOnClose) {
        this.sqlExceptionOnClose = sqlExceptionOnClose;
    }

    @Override
    public void setTransactionIsolation(final int level) throws SQLException {
        checkOpen();
        this.transactionIsolation = level;
    }

    @Override
    public void setTypeMap(final Map<String,Class<?>> map) throws SQLException {
        checkOpen();
        this.typeMap = map;
    }

    public void setWarnings(final SQLWarning warning) {
        this.warnings = warning;
    }

    @Override
    public <T> T unwrap(final Class<T> iface) throws SQLException {
        throw new SQLException("Not implemented.");
    }
}
