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

package org.apache.commons.dbcp;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.Map;

/**
 * A dummy {@link Connection}, for testing purposes.
 * 
 * @author Rodney Waldhoff
 * @author Dirk Verbeeck
 * @version $Revision: 1.11 $ $Date: 2004/02/28 11:47:52 $
 */
public class TesterConnection implements Connection {
    protected boolean _open = true;
    protected boolean _autoCommit = true;
    protected int _transactionIsolation = 1;
    protected DatabaseMetaData _metaData = null;
    protected String _catalog = null;
    protected Map _typeMap = null;
    protected boolean _readOnly = false;
    protected SQLWarning warnings = null;
    protected String username = null;
    protected String password = null;

    public TesterConnection(String username, String password) {
        this.username = username;
        this.password = password;
    }
    
    public String getUsername() {
        return this.username;
    }

    public void setWarnings(SQLWarning warning) {
        this.warnings = warning;
    }

    public void clearWarnings() throws SQLException {
        checkOpen();
        warnings = null;
    }

    public void close() throws SQLException {
        checkOpen();
        _open = false;
    }

    public void commit() throws SQLException {
        checkOpen();
    }

    public Statement createStatement() throws SQLException {
        checkOpen();
        return new TesterStatement(this);
    }

    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        checkOpen();
        return new TesterStatement(this);
    }

    public boolean getAutoCommit() throws SQLException {
        checkOpen();
        return _autoCommit;
    }

    public String getCatalog() throws SQLException {
        checkOpen();
        return _catalog;
    }

    public DatabaseMetaData getMetaData() throws SQLException {
        checkOpen();
        return _metaData;
    }

    public int getTransactionIsolation() throws SQLException {
        checkOpen();
        return _transactionIsolation;
    }

    public Map getTypeMap() throws SQLException {
        checkOpen();
        return _typeMap;
    }

    public SQLWarning getWarnings() throws SQLException {
        checkOpen();
        return warnings;
    }

    public boolean isClosed() throws SQLException {
        return !_open;
    }

    public boolean isReadOnly() throws SQLException {
        checkOpen();
        return _readOnly;
    }

    public String nativeSQL(String sql) throws SQLException {
        checkOpen();
        return sql;
    }

    public CallableStatement prepareCall(String sql) throws SQLException {
        checkOpen();
        if ("warning".equals(sql)) {
            setWarnings(new SQLWarning("warning in prepareCall"));
        }
        return null;
    }

    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        checkOpen();
        return null;
    }

    public PreparedStatement prepareStatement(String sql) throws SQLException {
        checkOpen();
        return new TesterPreparedStatement(this, sql);
    }

    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        checkOpen();
        return new TesterPreparedStatement(this, sql, resultSetType, resultSetConcurrency);
    }

    public void rollback() throws SQLException {
        checkOpen();
    }

    public void setAutoCommit(boolean autoCommit) throws SQLException {
        checkOpen();
        _autoCommit = autoCommit;
    }

    public void setCatalog(String catalog) throws SQLException {
        checkOpen();
        _catalog = catalog;
    }

    public void setReadOnly(boolean readOnly) throws SQLException {
        checkOpen();
        _readOnly = readOnly;
    }

    public void setTransactionIsolation(int level) throws SQLException {
        checkOpen();
        _transactionIsolation = level;
    }

    public void setTypeMap(Map map) throws SQLException {
        checkOpen();
        _typeMap = map;
    }

    protected void checkOpen() throws SQLException {
        if(!_open) {
            throw new SQLException("Connection is closed.");
        }
    }
    // ------------------- JDBC 3.0 -----------------------------------------
    // Will be commented by the build process on a JDBC 2.0 system

/* JDBC_3_ANT_KEY_BEGIN */

    public int getHoldability() throws SQLException {
        throw new SQLException("Not implemented.");
    }

    public void setHoldability(int holdability) throws SQLException {
        throw new SQLException("Not implemented.");
    }

    public java.sql.Savepoint setSavepoint() throws SQLException {
        throw new SQLException("Not implemented.");
    }

    public java.sql.Savepoint setSavepoint(String name) throws SQLException {
        throw new SQLException("Not implemented.");
    }

    public void rollback(java.sql.Savepoint savepoint) throws SQLException {
        throw new SQLException("Not implemented.");
    }

    public void releaseSavepoint(java.sql.Savepoint savepoint) throws SQLException {
        throw new SQLException("Not implemented.");
    }

    public Statement createStatement(int resultSetType,
                                     int resultSetConcurrency,
                                     int resultSetHoldability)
        throws SQLException {
        throw new SQLException("Not implemented.");
    }

    public PreparedStatement prepareStatement(String sql, int resultSetType,
                                              int resultSetConcurrency,
                                              int resultSetHoldability)
        throws SQLException {
        throw new SQLException("Not implemented.");
    }

    public CallableStatement prepareCall(String sql, int resultSetType,
                                         int resultSetConcurrency,
                                         int resultSetHoldability)
        throws SQLException {
        throw new SQLException("Not implemented.");
    }

    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys)
        throws SQLException {
        throw new SQLException("Not implemented.");
    }

    public PreparedStatement prepareStatement(String sql, int columnIndexes[])
        throws SQLException {
        throw new SQLException("Not implemented.");
    }

    public PreparedStatement prepareStatement(String sql, String columnNames[])
        throws SQLException {
        throw new SQLException("Not implemented.");
    }

/* JDBC_3_ANT_KEY_END */
}
