/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.dbcp2;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLType;
import java.sql.SQLXML;
import java.util.Calendar;

/**
 * A dummy {@link PreparedStatement}, for testing purposes.
 */
public class TesterPreparedStatement extends TesterStatement implements PreparedStatement {

    private final ResultSetMetaData resultSetMetaData = null;
    private String sql;
    private String catalog;
    private int autoGeneratedKeys = 1;
    private int[] columnIndexes;
    private String[] columnNames;

    public TesterPreparedStatement(final Connection conn) {
        super(conn);
        try {
            this.catalog = conn.getCatalog();
        } catch (final SQLException e) {
            // Ignored
        }
    }

    public TesterPreparedStatement(final Connection conn, final String sql) {
        super(conn);
        this.sql = sql;
        try {
            this.catalog = conn.getCatalog();
        } catch (final SQLException e) {
            // Ignored
        }
    }

    public TesterPreparedStatement(final Connection conn, final String sql, final int autoGeneratedKeys) {
        super(conn);
        this.sql = sql;
        this.autoGeneratedKeys = autoGeneratedKeys;
        try {
            this.catalog = conn.getCatalog();
        } catch (final SQLException e) {
            // Ignored
        }
    }

    public TesterPreparedStatement(final Connection conn, final String sql, final int resultSetType, final int resultSetConcurrency) {
        super(conn, resultSetType, resultSetConcurrency);
        this.sql = sql;
        try {
            this.catalog = conn.getCatalog();
        } catch (final SQLException e) {
            // Ignored
        }
    }

    public TesterPreparedStatement(final Connection conn, final String sql, final int resultSetType, final int resultSetConcurrency,
            final int resultSetHoldability) {
        super(conn, resultSetType, resultSetConcurrency, resultSetHoldability);
        this.sql = sql;
        try {
            this.catalog = conn.getCatalog();
        } catch (final SQLException e) {
            // Ignored
        }
    }

    public TesterPreparedStatement(final Connection conn, final String sql, final int[] columnIndexes) {
        super(conn);
        this.sql = sql;
        this.columnIndexes = columnIndexes;
        try {
            this.catalog = conn.getCatalog();
        } catch (final SQLException e) {
            // Ignored
        }
    }

    public TesterPreparedStatement(final Connection conn, final String sql, final String[] columnNames) {
        super(conn);
        this.sql = sql;
        this.columnNames = columnNames;
        try {
            this.catalog = conn.getCatalog();
        } catch (final SQLException e) {
            // Ignored
        }
    }

    @Override
    public void addBatch() throws SQLException {
        checkOpen();
    }

    @Override
    public void clearParameters() throws SQLException {
        checkOpen();
    }

    @Override
    public boolean execute() throws SQLException {
        checkOpen(); return true;
    }

    @Override
    public boolean execute(final String sql, final int autoGeneratedKeys) throws SQLException {
        checkOpen();
        return true;
    }

    @Override
    public boolean execute(final String sl, final int[] columnIndexes) throws SQLException {
        checkOpen();
        return true;
    }

    @Override
    public boolean execute(final String sql, final String[] columnNames) throws SQLException {
        checkOpen();
        return true;
    }

    @Override
    public long executeLargeUpdate() throws SQLException {
        checkOpen();
        return rowsUpdated;
    }

    @Override
    public long executeLargeUpdate(final String sql) throws SQLException {
        checkOpen();
        return rowsUpdated;
    }

    @Override
    public long executeLargeUpdate(final String sql, final int autoGeneratedKeys) throws SQLException {
        checkOpen();
        return 0;
    }

    @Override
    public long executeLargeUpdate(final String sql, final int[] columnIndexes) throws SQLException {
        checkOpen();
        return 0;
    }

    @Override
    public long executeLargeUpdate(final String sql, final String[] columnNames) throws SQLException {
        checkOpen();
        return 0;
    }

    @Override
    public ResultSet executeQuery() throws SQLException {
        checkOpen();
        if ("null".equals(sql)) {
            return null;
        }
        checkQueryTimeout();
        return new TesterResultSet(this, resultSetType, resultSetConcurrency);
    }

    @Override
    public ResultSet executeQuery(final String sql) throws SQLException {
        checkOpen();
        if ("null".equals(sql)) {
            return null;
        }
        checkQueryTimeout();
        return new TesterResultSet(this, resultSetType, resultSetConcurrency);
    }

    @Override
    public int executeUpdate() throws SQLException {
        checkOpen();
        return (int) rowsUpdated;
    }

    @Override
    public int executeUpdate(final String sql) throws SQLException {
        checkOpen();
        return (int) rowsUpdated;
    }

    @Override
    public int executeUpdate(final String sql, final int autoGeneratedKeys) throws SQLException {
        checkOpen();
        return 0;
    }

    @Override
    public int executeUpdate(final String sql, final int[] columnIndexes) throws SQLException {
        checkOpen();
        return 0;
    }

    @Override
    public int executeUpdate(final String sql, final String[] columnNames) throws SQLException {
        checkOpen();
        return 0;
    }

    public int getAutoGeneratedKeys() {
        return autoGeneratedKeys;
    }

    public String getCatalog() {
        return catalog;
    }

    public int[] getColumnIndexes() {
        return columnIndexes;
    }

    public String[] getColumnNames() {
        return columnNames;
    }

    @Override
    public ResultSet getGeneratedKeys() throws SQLException {
        return new TesterResultSet(this, resultSetType, resultSetConcurrency);
    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        checkOpen();
        return resultSetMetaData;
    }

    @Override
    public boolean getMoreResults(final int current) throws SQLException {
        throw new SQLException("Not implemented.");
    }

    @Override
    public java.sql.ParameterMetaData getParameterMetaData() throws SQLException {
        throw new SQLException("Not implemented.");
    }

    /** For junit test only */
    public String getSql() {
        return sql;
    }

    @Override
    public void setArray (final int i, final Array x) throws SQLException {
        checkOpen();
    }

    @Override
    public void setAsciiStream(final int parameterIndex, final InputStream inputStream) throws SQLException {
        throw new SQLException("Not implemented.");
    }

    @Override
    public void setAsciiStream(final int parameterIndex, final InputStream inputStream, final long length) throws SQLException {
        throw new SQLException("Not implemented.");
    }

    @Override
    public void setAsciiStream(final int parameterIndex, final java.io.InputStream x, final int length) throws SQLException {
        checkOpen();
    }

    @Override
    public void setBigDecimal(final int parameterIndex, final BigDecimal x) throws SQLException {
        checkOpen();
    }

    @Override
    public void setBinaryStream(final int parameterIndex, final InputStream inputStream) throws SQLException {
        throw new SQLException("Not implemented.");
    }

    @Override
    public void setBinaryStream(final int parameterIndex, final InputStream inputStream, final long length) throws SQLException {
        throw new SQLException("Not implemented.");
    }

    @Override
    public void setBinaryStream(final int parameterIndex, final java.io.InputStream x, final int length) throws SQLException {
        checkOpen();
    }

    @Override
    public void setBlob (final int i, final Blob x) throws SQLException {
        checkOpen();
    }

    @Override
    public void setBlob(final int parameterIndex, final InputStream inputStream) throws SQLException {
        throw new SQLException("Not implemented.");
    }

    @Override
    public void setBlob(final int parameterIndex, final InputStream inputStream, final long length) throws SQLException {
        throw new SQLException("Not implemented.");
    }

    @Override
    public void setBoolean(final int parameterIndex, final boolean x) throws SQLException {
        checkOpen();
    }

    @Override
    public void setByte(final int parameterIndex, final byte x) throws SQLException {
        checkOpen();
    }

    @Override
    public void setBytes(final int parameterIndex, final byte[] x) throws SQLException {
        checkOpen();
    }

    @Override
    public void setCharacterStream(final int parameterIndex, final java.io.Reader reader, final int length) throws SQLException {
        checkOpen();
    }

    @Override
    public void setCharacterStream(final int parameterIndex, final Reader reader) throws SQLException {
        throw new SQLException("Not implemented.");
    }

    @Override
    public void setCharacterStream(final int parameterIndex, final Reader reader, final long length) throws SQLException {
        throw new SQLException("Not implemented.");
    }

    @Override
    public void setClob (final int i, final Clob x) throws SQLException {
        checkOpen();
    }

    @Override
    public void setClob(final int parameterIndex, final Reader reader) throws SQLException {
        throw new SQLException("Not implemented.");
    }

    @Override
    public void setClob(final int parameterIndex, final Reader reader, final long length) throws SQLException {
        throw new SQLException("Not implemented.");
    }

    @Override
    public void setDate(final int parameterIndex, final java.sql.Date x) throws SQLException {
        checkOpen();
    }

    @Override
    public void setDate(final int parameterIndex, final java.sql.Date x, final Calendar cal) throws SQLException {
        checkOpen();
    }

    @Override
    public void setDouble(final int parameterIndex, final double x) throws SQLException {
        checkOpen();
    }

    @Override
    public void setFloat(final int parameterIndex, final float x) throws SQLException {
        checkOpen();
    }

    @Override
    public void setInt(final int parameterIndex, final int x) throws SQLException {
        checkOpen();
    }

    @Override
    public void setLong(final int parameterIndex, final long x) throws SQLException {
        checkOpen();
    }

    @Override
    public void setNCharacterStream(final int parameterIndex, final Reader reader) throws SQLException {
        throw new SQLException("Not implemented.");
    }

    @Override
    public void setNCharacterStream(final int parameterIndex, final Reader value, final long length) throws SQLException {
        throw new SQLException("Not implemented.");
    }

    @Override
    public void setNClob(final int parameterIndex, final NClob value) throws SQLException {
        throw new SQLException("Not implemented.");
    }

    @Override
    public void setNClob(final int parameterIndex, final Reader reader) throws SQLException {
        throw new SQLException("Not implemented.");
    }

    @Override
    public void setNClob(final int parameterIndex, final Reader reader, final long length) throws SQLException {
        throw new SQLException("Not implemented.");
    }

    @Override
    public void setNString(final int parameterIndex, final String value) throws SQLException {
        throw new SQLException("Not implemented.");
    }

    @Override
    public void setNull(final int parameterIndex, final int sqlType) throws SQLException {
        checkOpen();
    }

    @Override
    public void setNull (final int paramIndex, final int sqlType, final String typeName) throws SQLException {
        checkOpen();
    }

    @Override
    public void setObject(final int parameterIndex, final Object x) throws SQLException {
        checkOpen();
    }

    @Override
    public void setObject(final int parameterIndex, final Object x, final int targetSqlType) throws SQLException {
        checkOpen();
    }

    @Override
    public void setObject(final int parameterIndex, final Object x, final int targetSqlType, final int scale) throws SQLException {
        checkOpen();
    }

    @Override
    public void setObject(final int parameterIndex, final Object x, final SQLType targetSqlType) throws SQLException {
        checkOpen();
    }

    @Override
    public void setObject(final int parameterIndex, final Object x, final SQLType targetSqlType, final int scaleOrLength) throws SQLException {
        checkOpen();
    }

    @Override
    public void setRef (final int i, final Ref x) throws SQLException {
        checkOpen();
    }

    @Override
    public void setRowId(final int parameterIndex, final RowId value) throws SQLException {
        throw new SQLException("Not implemented.");
    }

    @Override
    public void setShort(final int parameterIndex, final short x) throws SQLException {
        checkOpen();
    }

    @Override
    public void setSQLXML(final int parameterIndex, final SQLXML value) throws SQLException {
        throw new SQLException("Not implemented.");
    }

    @Override
    public void setString(final int parameterIndex, final String x) throws SQLException {
        checkOpen();
    }

    @Override
    public void setTime(final int parameterIndex, final java.sql.Time x) throws SQLException {
        checkOpen();
    }

    @Override
    public void setTime(final int parameterIndex, final java.sql.Time x, final Calendar cal) throws SQLException {
        checkOpen();
    }

    @Override
    public void setTimestamp(final int parameterIndex, final java.sql.Timestamp x) throws SQLException {
        checkOpen();
    }

    @Override
    public void setTimestamp(final int parameterIndex, final java.sql.Timestamp x, final Calendar cal) throws SQLException {
        checkOpen();
    }

    /** @deprecated */
    @Deprecated
    @Override
    public void setUnicodeStream(final int parameterIndex, final java.io.InputStream x, final int length) throws SQLException {
        checkOpen();
    }

    @Override
    public void setURL(final int parameterIndex, final java.net.URL x) throws SQLException {
        throw new SQLException("Not implemented.");
    }

    @Override
    public String toString() {
        return sql;
    }
}
