/*

  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at

      https://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.apache.commons.dbcp2;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLType;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;

/**
 * Trivial implementation of a CallableStatement to avoid null pointer exceptions in tests.
 */
public class TesterCallableStatement extends TesterPreparedStatement implements CallableStatement {

    public TesterCallableStatement(final Connection conn) {
        super(conn);
    }

    public TesterCallableStatement(final Connection conn, final String sql) {
        super(conn, sql);
    }

    public TesterCallableStatement(final Connection conn, final String sql, final int resultSetType, final int resultSetConcurrency) {
        super(conn, sql, resultSetType, resultSetConcurrency);
    }

    public TesterCallableStatement(final Connection conn, final String sql, final int resultSetType, final int resultSetConcurrency,
            final int resultSetHoldability) {
        super(conn, sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public Array getArray(final int i) throws SQLException {
        return null;
    }

    @Override
    public Array getArray(final String parameterName) throws SQLException {
        return null;
    }

    @Override
    public BigDecimal getBigDecimal(final int parameterIndex) throws SQLException {
        return null;
    }

    /**
     * @deprecated See {@link CallableStatement#getBigDecimal(int,int)}.
     */
    @Deprecated
    @Override
    public BigDecimal getBigDecimal(final int parameterIndex, final int scale) throws SQLException {
        return null;
    }

    @Override
    public BigDecimal getBigDecimal(final String parameterName) throws SQLException {
        return null;
    }

    @Override
    public Blob getBlob(final int i) throws SQLException {
        return null;
    }

    @Override
    public Blob getBlob(final String parameterName) throws SQLException {
        return null;
    }

    @Override
    public boolean getBoolean(final int parameterIndex) throws SQLException {
        return false;
    }

    @Override
    public boolean getBoolean(final String parameterName) throws SQLException {
        return false;
    }

    @Override
    public byte getByte(final int parameterIndex) throws SQLException {
        return 0;
    }

    @Override
    public byte getByte(final String parameterName) throws SQLException {
        return 0;
    }

    @Override
    public byte[] getBytes(final int parameterIndex) throws SQLException {
        return new byte[0];
    }

    @Override
    public byte[] getBytes(final String parameterName) throws SQLException {
        return new byte[0];
    }

    @Override
    public Reader getCharacterStream(final int parameterIndex) throws SQLException {
        return null;
    }

    @Override
    public Reader getCharacterStream(final String parameterName) throws SQLException {
        return null;
    }

    @Override
    public Clob getClob(final int i) throws SQLException {
        return null;
    }

    @Override
    public Clob getClob(final String parameterName) throws SQLException {
        return null;
    }

    @Override
    public Date getDate(final int parameterIndex) throws SQLException {
        return null;
    }

    @Override
    public Date getDate(final int parameterIndex, final Calendar cal) throws SQLException {
        return null;
    }

    @Override
    public Date getDate(final String parameterName) throws SQLException {
        return null;
    }

    @Override
    public Date getDate(final String parameterName, final Calendar cal) throws SQLException {
        return null;
    }

    @Override
    public double getDouble(final int parameterIndex) throws SQLException {
        return 0;
    }

    @Override
    public double getDouble(final String parameterName) throws SQLException {
        return 0;
    }

    @Override
    public float getFloat(final int parameterIndex) throws SQLException {
        return 0;
    }

    @Override
    public float getFloat(final String parameterName) throws SQLException {
        return 0;
    }

    @Override
    public int getInt(final int parameterIndex) throws SQLException {
        return 0;
    }

    @Override
    public int getInt(final String parameterName) throws SQLException {
        return 0;
    }

    @Override
    public long getLong(final int parameterIndex) throws SQLException {
        return 0;
    }

    @Override
    public long getLong(final String parameterName) throws SQLException {
        return 0;
    }

    @Override
    public Reader getNCharacterStream(final int parameterIndex) throws SQLException {
        return null;
    }

    @Override
    public Reader getNCharacterStream(final String parameterName) throws SQLException {
        return null;
    }

    @Override
    public NClob getNClob(final int parameterIndex) throws SQLException {
        return null;
    }

    @Override
    public NClob getNClob(final String parameterName) throws SQLException {
        return null;
    }

    @Override
    public String getNString(final int parameterIndex) throws SQLException {
        return null;
    }

    @Override
    public String getNString(final String parameterName) throws SQLException {
        return null;
    }

    @Override
    public Object getObject(final int parameterIndex) throws SQLException {
        return null;
    }

    @Override
    public <T> T getObject(final int parameterIndex, final Class<T> type) throws SQLException {
        return null;
    }

    @Override
    public Object getObject(final int i, final Map<String, Class<?>> map) throws SQLException {
        return null;
    }

    @Override
    public Object getObject(final String parameterName) throws SQLException {
        return null;
    }

    @Override
    public <T> T getObject(final String parameterName, final Class<T> type) throws SQLException {
        return null;
    }

    @Override
    public Object getObject(final String parameterName, final Map<String, Class<?>> map) throws SQLException {
        return null;
    }

    @Override
    public Ref getRef(final int i) throws SQLException {
        return null;
    }

    @Override
    public Ref getRef(final String parameterName) throws SQLException {
        return null;
    }

    @Override
    public RowId getRowId(final int parameterIndex) throws SQLException {
        return null;
    }

    @Override
    public RowId getRowId(final String parameterName) throws SQLException {
        return null;
    }

    @Override
    public short getShort(final int parameterIndex) throws SQLException {
        return 0;
    }

    @Override
    public short getShort(final String parameterName) throws SQLException {
        return 0;
    }

    @Override
    public SQLXML getSQLXML(final int parameterIndex) throws SQLException {
        return null;
    }

    @Override
    public SQLXML getSQLXML(final String parameterName) throws SQLException {
        return null;
    }

    @Override
    public String getString(final int parameterIndex) throws SQLException {
        return null;
    }

    @Override
    public String getString(final String parameterName) throws SQLException {
        return null;
    }

    @Override
    public Time getTime(final int parameterIndex) throws SQLException {
        return null;
    }

    @Override
    public Time getTime(final int parameterIndex, final Calendar cal) throws SQLException {
        return null;
    }

    @Override
    public Time getTime(final String parameterName) throws SQLException {
        return null;
    }

    @Override
    public Time getTime(final String parameterName, final Calendar cal) throws SQLException {
        return null;
    }

    @Override
    public Timestamp getTimestamp(final int parameterIndex) throws SQLException {
        return null;
    }

    @Override
    public Timestamp getTimestamp(final int parameterIndex, final Calendar cal) throws SQLException {
        return null;
    }

    @Override
    public Timestamp getTimestamp(final String parameterName) throws SQLException {
        return null;
    }

    @Override
    public Timestamp getTimestamp(final String parameterName, final Calendar cal) throws SQLException {
        return null;
    }

    @Override
    public URL getURL(final int parameterIndex) throws SQLException {
        return null;
    }

    @Override
    public URL getURL(final String parameterName) throws SQLException {
        return null;
    }

    @Override
    public void registerOutParameter(final int parameterIndex, final int sqlType) throws SQLException {
    }

    @Override
    public void registerOutParameter(final int parameterIndex, final int sqlType, final int scale) throws SQLException {
    }

    @Override
    public void registerOutParameter(final int paramIndex, final int sqlType, final String typeName) throws SQLException {
    }

    @Override
    public void registerOutParameter(final int parameterIndex, final SQLType sqlType) throws SQLException {
        // Do nothing
    }

    @Override
    public void registerOutParameter(final int parameterIndex, final SQLType sqlType, final int scale) throws SQLException {
        // Do nothing
    }

    @Override
    public void registerOutParameter(final int parameterIndex, final SQLType sqlType, final String typeName) throws SQLException {
        // Do nothing
    }

    @Override
    public void registerOutParameter(final String parameterName, final int sqlType) throws SQLException {
    }

    @Override
    public void registerOutParameter(final String parameterName, final int sqlType, final int scale) throws SQLException {
    }

    @Override
    public void registerOutParameter(final String parameterName, final int sqlType, final String typeName) throws SQLException {
    }

    @Override
    public void registerOutParameter(final String parameterName, final SQLType sqlType) throws SQLException {
        // Do nothing
    }

    @Override
    public void registerOutParameter(final String parameterName, final SQLType sqlType, final int scale) throws SQLException {
        // Do nothing
    }

    @Override
    public void registerOutParameter(final String parameterName, final SQLType sqlType, final String typeName) throws SQLException {
        // Do nothing
    }

    @Override
    public void setAsciiStream(final String parameterName, final InputStream inputStream) throws SQLException {
    }

    @Override
    public void setAsciiStream(final String parameterName, final InputStream x, final int length) throws SQLException {
    }

    @Override
    public void setAsciiStream(final String parameterName, final InputStream inputStream, final long length) throws SQLException {
    }

    @Override
    public void setBigDecimal(final String parameterName, final BigDecimal x) throws SQLException {
    }

    @Override
    public void setBinaryStream(final String parameterName, final InputStream inputStream) throws SQLException {
    }

    @Override
    public void setBinaryStream(final String parameterName, final InputStream x, final int length) throws SQLException {
    }

    @Override
    public void setBinaryStream(final String parameterName, final InputStream inputStream, final long length) throws SQLException {
    }

    @Override
    public void setBlob(final String parameterName, final Blob blob) throws SQLException {
    }

    @Override
    public void setBlob(final String parameterName, final InputStream inputStream) throws SQLException {
    }

    @Override
    public void setBlob(final String parameterName, final InputStream inputStream, final long length) throws SQLException {
    }

    @Override
    public void setBoolean(final String parameterName, final boolean x) throws SQLException {
    }

    @Override
    public void setByte(final String parameterName, final byte x) throws SQLException {
    }

    @Override
    public void setBytes(final String parameterName, final byte[] x) throws SQLException {
    }

    @Override
    public void setCharacterStream(final String parameterName, final Reader reader) throws SQLException {
    }

    @Override
    public void setCharacterStream(final String parameterName, final Reader reader, final int length) throws SQLException {
    }

    @Override
    public void setCharacterStream(final String parameterName, final Reader reader, final long length) throws SQLException {
    }

    @Override
    public void setClob(final String parameterName, final Clob clob) throws SQLException {
    }

    @Override
    public void setClob(final String parameterName, final Reader reader) throws SQLException {
    }

    @Override
    public void setClob(final String parameterName, final Reader reader, final long length) throws SQLException {
    }

    @Override
    public void setDate(final String parameterName, final Date x) throws SQLException {
    }

    @Override
    public void setDate(final String parameterName, final Date x, final Calendar cal) throws SQLException {
    }

    @Override
    public void setDouble(final String parameterName, final double x) throws SQLException {
    }

    @Override
    public void setFloat(final String parameterName, final float x) throws SQLException {
    }

    @Override
    public void setInt(final String parameterName, final int x) throws SQLException {
    }

    @Override
    public void setLong(final String parameterName, final long x) throws SQLException {
    }

    @Override
    public void setNCharacterStream(final String parameterName, final Reader reader) throws SQLException {
    }

    @Override
    public void setNCharacterStream(final String parameterName, final Reader reader, final long length) throws SQLException {
    }

    @Override
    public void setNClob(final String parameterName, final NClob value) throws SQLException {
    }

    @Override
    public void setNClob(final String parameterName, final Reader reader) throws SQLException {
    }

    @Override
    public void setNClob(final String parameterName, final Reader reader, final long length) throws SQLException {
    }

    @Override
    public void setNString(final String parameterName, final String value) throws SQLException {
    }

    @Override
    public void setNull(final String parameterName, final int sqlType) throws SQLException {
    }

    @Override
    public void setNull(final String parameterName, final int sqlType, final String typeName) throws SQLException {
    }

    @Override
    public void setObject(final String parameterName, final Object x) throws SQLException {
    }

    @Override
    public void setObject(final String parameterName, final Object x, final int targetSqlType) throws SQLException {
    }

    @Override
    public void setObject(final String parameterName, final Object x, final int targetSqlType, final int scale) throws SQLException {
    }

    @Override
    public void setObject(final String parameterName, final Object x, final SQLType targetSqlType) throws SQLException {
        // Do nothing
    }

    @Override
    public void setObject(final String parameterName, final Object x, final SQLType targetSqlType, final int scaleOrLength) throws SQLException {
        // Do nothing
    }

    @Override
    public void setRowId(final String parameterName, final RowId value) throws SQLException {
    }

    @Override
    public void setShort(final String parameterName, final short x) throws SQLException {
    }

    @Override
    public void setSQLXML(final String parameterName, final SQLXML value) throws SQLException {
    }

    @Override
    public void setString(final String parameterName, final String x) throws SQLException {
    }

    @Override
    public void setTime(final String parameterName, final Time x) throws SQLException {
    }

    @Override
    public void setTime(final String parameterName, final Time x, final Calendar cal) throws SQLException {
    }

    @Override
    public void setTimestamp(final String parameterName, final Timestamp x) throws SQLException {
    }

    @Override
    public void setTimestamp(final String parameterName, final Timestamp x, final Calendar cal) throws SQLException {
    }

    @Override
    public void setURL(final String parameterName, final URL val) throws SQLException {
    }

    @Override
    public boolean wasNull() throws SQLException {
        return false;
    }
}
