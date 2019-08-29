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

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
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
 * A base delegating implementation of {@link CallableStatement}.
 * <p>
 * All of the methods from the {@link CallableStatement} interface simply call the corresponding method on the
 * "delegate" provided in my constructor.
 * </p>
 * <p>
 * Extends AbandonedTrace to implement Statement tracking and logging of code which created the Statement. Tracking the
 * Statement ensures that the Connection which created it can close any open Statement's on Connection close.
 * </p>
 *
 * @since 2.0
 */
public class DelegatingCallableStatement extends DelegatingPreparedStatement implements CallableStatement {

    /**
     * Creates a wrapper for the Statement which traces this Statement to the Connection which created it and the code
     * which created it.
     *
     * @param connection the {@link DelegatingConnection} that created this statement
     * @param statement the {@link CallableStatement} to delegate all calls to
     */
    public DelegatingCallableStatement(final DelegatingConnection<?> connection, final CallableStatement statement) {
        super(connection, statement);
    }

    @Override
    public Array getArray(final int parameterIndex) throws SQLException {
        return apply(CallableStatement::getArray, getDelegateCallableStatement(), parameterIndex);
    }

    @Override
    public Array getArray(final String parameterName) throws SQLException {
        return apply(CallableStatement::getArray, getDelegateCallableStatement(), parameterName);
    }

    @Override
    public BigDecimal getBigDecimal(final int parameterIndex) throws SQLException {
        return apply(CallableStatement::getBigDecimal, getDelegateCallableStatement(), parameterIndex);
    }

    /** @deprecated Use {@link #getBigDecimal(int)} or {@link #getBigDecimal(String)} */
    @Override
    @Deprecated
    public BigDecimal getBigDecimal(final int parameterIndex, final int scale) throws SQLException {
        return apply(CallableStatement::getBigDecimal, getDelegateCallableStatement(), parameterIndex, scale);
    }

    @Override
    public BigDecimal getBigDecimal(final String parameterName) throws SQLException {
        return apply(CallableStatement::getBigDecimal, getDelegateCallableStatement(), parameterName);
    }

    @Override
    public Blob getBlob(final int parameterIndex) throws SQLException {
        return apply(CallableStatement::getBlob, getDelegateCallableStatement(), parameterIndex);
    }

    @Override
    public Blob getBlob(final String parameterName) throws SQLException {
        return apply(CallableStatement::getBlob, getDelegateCallableStatement(), parameterName);
    }

    @Override
    public boolean getBoolean(final int parameterIndex) throws SQLException {
        return applyTo(CallableStatement::getBoolean, getDelegateCallableStatement(), parameterIndex, false);
    }

    @Override
    public boolean getBoolean(final String parameterName) throws SQLException {
        return applyTo(CallableStatement::getBoolean, getDelegateCallableStatement(), parameterName, false);
    }

    @Override
    public byte getByte(final int parameterIndex) throws SQLException {
        return applyTo(CallableStatement::getByte, getDelegateCallableStatement(), parameterIndex, (byte) 0);
    }

    @Override
    public byte getByte(final String parameterName) throws SQLException {
        return applyTo(CallableStatement::getByte, getDelegateCallableStatement(), parameterName, (byte) 0);
    }

    @Override
    public byte[] getBytes(final int parameterIndex) throws SQLException {
        return apply(CallableStatement::getBytes, getDelegateCallableStatement(), parameterIndex);
    }

    @Override
    public byte[] getBytes(final String parameterName) throws SQLException {
        return apply(CallableStatement::getBytes, getDelegateCallableStatement(), parameterName);
    }

    @Override
    public Reader getCharacterStream(final int parameterIndex) throws SQLException {
        return apply(CallableStatement::getCharacterStream, getDelegateCallableStatement(), parameterIndex);
    }

    @Override
    public Reader getCharacterStream(final String parameterName) throws SQLException {
        return apply(CallableStatement::getCharacterStream, getDelegateCallableStatement(), parameterName);
    }

    @Override
    public Clob getClob(final int parameterIndex) throws SQLException {
        return apply(CallableStatement::getClob, getDelegateCallableStatement(), parameterIndex);
    }

    @Override
    public Clob getClob(final String parameterName) throws SQLException {
        return apply(CallableStatement::getClob, getDelegateCallableStatement(), parameterName);
    }

    @Override
    public Date getDate(final int parameterIndex) throws SQLException {
        return apply(CallableStatement::getDate, getDelegateCallableStatement(), parameterIndex);
    }

    @Override
    public Date getDate(final int parameterIndex, final Calendar cal) throws SQLException {
        return apply(CallableStatement::getDate, getDelegateCallableStatement(), parameterIndex, cal);
    }

    @Override
    public Date getDate(final String parameterName) throws SQLException {
        return apply(CallableStatement::getDate, getDelegateCallableStatement(), parameterName);
    }

    @Override
    public Date getDate(final String parameterName, final Calendar cal) throws SQLException {
        return apply(CallableStatement::getDate, getDelegateCallableStatement(), parameterName, cal);
    }

    private CallableStatement getDelegateCallableStatement() {
        return (CallableStatement) getDelegate();
    }

    @Override
    public double getDouble(final int parameterIndex) throws SQLException {
        return applyTo(CallableStatement::getDouble, getDelegateCallableStatement(), parameterIndex, 0d);
    }

    @Override
    public double getDouble(final String parameterName) throws SQLException {
        return applyTo(CallableStatement::getDouble, getDelegateCallableStatement(), parameterName, 0d);
    }

    @Override
    public float getFloat(final int parameterIndex) throws SQLException {
        return applyTo(CallableStatement::getFloat, getDelegateCallableStatement(), parameterIndex, 0f);
    }

    @Override
    public float getFloat(final String parameterName) throws SQLException {
        return applyTo(CallableStatement::getFloat, getDelegateCallableStatement(), parameterName, 0f);
    }

    @Override
    public int getInt(final int parameterIndex) throws SQLException {
        return applyTo(CallableStatement::getInt, getDelegateCallableStatement(), parameterIndex, 0);
    }

    @Override
    public int getInt(final String parameterName) throws SQLException {
        return applyTo(CallableStatement::getInt, getDelegateCallableStatement(), parameterName, 0);
    }

    @Override
    public long getLong(final int parameterIndex) throws SQLException {
        return applyTo(CallableStatement::getLong, getDelegateCallableStatement(), parameterIndex, 0L);
    }

    @Override
    public long getLong(final String parameterName) throws SQLException {
        return applyTo(CallableStatement::getLong, getDelegateCallableStatement(), parameterName, 0L);
    }

    @Override
    public Reader getNCharacterStream(final int parameterIndex) throws SQLException {
        return apply(CallableStatement::getNCharacterStream, getDelegateCallableStatement(), parameterIndex);
    }

    @Override
    public Reader getNCharacterStream(final String parameterName) throws SQLException {
        return apply(CallableStatement::getNCharacterStream, getDelegateCallableStatement(), parameterName);
    }

    @Override
    public NClob getNClob(final int parameterIndex) throws SQLException {
        return apply(CallableStatement::getNClob, getDelegateCallableStatement(), parameterIndex);
    }

    @Override
    public NClob getNClob(final String parameterName) throws SQLException {
        return apply(CallableStatement::getNClob, getDelegateCallableStatement(), parameterName);
    }

    @Override
    public String getNString(final int parameterIndex) throws SQLException {
        return apply(CallableStatement::getNString, getDelegateCallableStatement(), parameterIndex);
    }

    @Override
    public String getNString(final String parameterName) throws SQLException {
        return apply(CallableStatement::getNString, getDelegateCallableStatement(), parameterName);
    }

    @Override
    public Object getObject(final int parameterIndex) throws SQLException {
        return apply(CallableStatement::getObject, getDelegateCallableStatement(), parameterIndex);
    }

    @Override
    public <T> T getObject(final int parameterIndex, final Class<T> type) throws SQLException {
        checkOpen();
        try {
            return getDelegateCallableStatement().getObject(parameterIndex, type);
        } catch (final SQLException e) {
            handleException(e);
            return null;
        }
    }

    @Override
    public Object getObject(final int parameterIndex, final Map<String, Class<?>> map) throws SQLException {
        return apply(CallableStatement::getObject, getDelegateCallableStatement(), parameterIndex, map);
    }

    @Override
    public Object getObject(final String parameterName) throws SQLException {
        return apply(CallableStatement::getObject, getDelegateCallableStatement(), parameterName);
    }

    @Override
    public <T> T getObject(final String parameterName, final Class<T> type) throws SQLException {
        checkOpen();
        try {
            return getDelegateCallableStatement().getObject(parameterName, type);
        } catch (final SQLException e) {
            handleException(e);
            return null;
        }
    }

    @Override
    public Object getObject(final String parameterName, final Map<String, Class<?>> map) throws SQLException {
        checkOpen();
        try {
            return getDelegateCallableStatement().getObject(parameterName, map);
        } catch (final SQLException e) {
            handleException(e);
            return null;
        }
    }

    @Override
    public Ref getRef(final int parameterIndex) throws SQLException {
        return apply(CallableStatement::getRef, getDelegateCallableStatement(), parameterIndex);
    }

    @Override
    public Ref getRef(final String parameterName) throws SQLException {
        return apply(CallableStatement::getRef, getDelegateCallableStatement(), parameterName);
    }

    @Override
    public RowId getRowId(final int parameterIndex) throws SQLException {
        return apply(CallableStatement::getRowId, getDelegateCallableStatement(), parameterIndex);
    }

    @Override
    public RowId getRowId(final String parameterName) throws SQLException {
        return apply(CallableStatement::getRowId, getDelegateCallableStatement(), parameterName);
    }

    @Override
    public short getShort(final int parameterIndex) throws SQLException {
        return applyTo(CallableStatement::getShort, getDelegateCallableStatement(), parameterIndex, (short) 0);
    }

    @Override
    public short getShort(final String parameterName) throws SQLException {
        return applyTo(CallableStatement::getShort, getDelegateCallableStatement(), parameterName, (short) 0);
    }

    @Override
    public SQLXML getSQLXML(final int parameterIndex) throws SQLException {
        return apply(CallableStatement::getSQLXML, getDelegateCallableStatement(), parameterIndex);
    }

    @Override
    public SQLXML getSQLXML(final String parameterName) throws SQLException {
        return apply(CallableStatement::getSQLXML, getDelegateCallableStatement(), parameterName);
    }

    @Override
    public String getString(final int parameterIndex) throws SQLException {
        return apply(CallableStatement::getString, getDelegateCallableStatement(), parameterIndex);
    }

    @Override
    public String getString(final String parameterName) throws SQLException {
        return apply(CallableStatement::getString, getDelegateCallableStatement(), parameterName);
    }

    @Override
    public Time getTime(final int parameterIndex) throws SQLException {
        return apply(CallableStatement::getTime, getDelegateCallableStatement(), parameterIndex);
    }

    @Override
    public Time getTime(final int parameterIndex, final Calendar cal) throws SQLException {
        return apply(CallableStatement::getTime, getDelegateCallableStatement(), parameterIndex, cal);
    }

    @Override
    public Time getTime(final String parameterName) throws SQLException {
        return apply(CallableStatement::getTime, getDelegateCallableStatement(), parameterName);
    }

    @Override
    public Time getTime(final String parameterName, final Calendar cal) throws SQLException {
        return apply(CallableStatement::getTime, getDelegateCallableStatement(), parameterName, cal);
    }

    @Override
    public Timestamp getTimestamp(final int parameterIndex) throws SQLException {
        return apply(CallableStatement::getTimestamp, getDelegateCallableStatement(), parameterIndex);
    }

    @Override
    public Timestamp getTimestamp(final int parameterIndex, final Calendar cal) throws SQLException {
        return apply(CallableStatement::getTimestamp, getDelegateCallableStatement(), parameterIndex, cal);
    }

    @Override
    public Timestamp getTimestamp(final String parameterName) throws SQLException {
        return apply(CallableStatement::getTimestamp, getDelegateCallableStatement(), parameterName);
    }

    @Override
    public Timestamp getTimestamp(final String parameterName, final Calendar cal) throws SQLException {
        return apply(CallableStatement::getTimestamp, getDelegateCallableStatement(), parameterName, cal);
    }

    @Override
    public URL getURL(final int parameterIndex) throws SQLException {
        return apply(CallableStatement::getURL, getDelegateCallableStatement(), parameterIndex);
    }

    @Override
    public URL getURL(final String parameterName) throws SQLException {
        return apply(CallableStatement::getURL, getDelegateCallableStatement(), parameterName);
    }

    @Override
    public void registerOutParameter(final int parameterIndex, final int sqlType) throws SQLException {
        accept(CallableStatement::registerOutParameter, getDelegateCallableStatement(), parameterIndex, sqlType);
    }

    @Override
    public void registerOutParameter(final int parameterIndex, final int sqlType, final int scale) throws SQLException {
        accept(CallableStatement::registerOutParameter, getDelegateCallableStatement(), parameterIndex, sqlType, scale);
    }

    @Override
    public void registerOutParameter(final int parameterIndex, final int sqlType, final String typeName)
            throws SQLException {
        accept(CallableStatement::registerOutParameter, getDelegateCallableStatement(), parameterIndex, sqlType,
                typeName);
    }

    /**
     * @since 2.5.0
     */
    @Override
    public void registerOutParameter(final int parameterIndex, final SQLType sqlType) throws SQLException {
        accept(CallableStatement::registerOutParameter, getDelegateCallableStatement(), parameterIndex, sqlType);
    }

    /**
     * @since 2.5.0
     */
    @Override
    public void registerOutParameter(final int parameterIndex, final SQLType sqlType, final int scale)
            throws SQLException {
        accept(CallableStatement::registerOutParameter, getDelegateCallableStatement(), parameterIndex, sqlType, scale);
    }

    /**
     * @since 2.5.0
     */
    @Override
    public void registerOutParameter(final int parameterIndex, final SQLType sqlType, final String typeName)
            throws SQLException {
        accept(CallableStatement::registerOutParameter, getDelegateCallableStatement(), parameterIndex, sqlType,
                typeName);
    }

    @Override
    public void registerOutParameter(final String parameterName, final int sqlType) throws SQLException {
        accept(CallableStatement::registerOutParameter, getDelegateCallableStatement(), parameterName, sqlType);
    }

    @Override
    public void registerOutParameter(final String parameterName, final int sqlType, final int scale)
            throws SQLException {
        accept(CallableStatement::registerOutParameter, getDelegateCallableStatement(), parameterName, sqlType, scale);
    }

    @Override
    public void registerOutParameter(final String parameterName, final int sqlType, final String typeName)
            throws SQLException {
        accept(CallableStatement::registerOutParameter, getDelegateCallableStatement(), parameterName, sqlType,
                typeName);
    }

    /**
     * @since 2.5.0
     */
    @Override
    public void registerOutParameter(final String parameterName, final SQLType sqlType) throws SQLException {
        accept(CallableStatement::registerOutParameter, getDelegateCallableStatement(), parameterName, sqlType);
    }

    /**
     * @since 2.5.0
     */
    @Override
    public void registerOutParameter(final String parameterName, final SQLType sqlType, final int scale)
            throws SQLException {
        accept(CallableStatement::registerOutParameter, getDelegateCallableStatement(), parameterName, sqlType, scale);
    }

    /**
     * @since 2.5.0
     */
    @Override
    public void registerOutParameter(final String parameterName, final SQLType sqlType, final String typeName)
            throws SQLException {
        accept(CallableStatement::registerOutParameter, getDelegateCallableStatement(), parameterName, sqlType,
                typeName);
    }

    @Override
    public void setAsciiStream(final String parameterName, final InputStream value) throws SQLException {
        accept(CallableStatement::setAsciiStream, getDelegateCallableStatement(), parameterName, value);
    }

    @Override
    public void setAsciiStream(final String parameterName, final InputStream value, final int length)
            throws SQLException {
        accept(CallableStatement::setAsciiStream, getDelegateCallableStatement(), parameterName, value, length);
    }

    @Override
    public void setAsciiStream(final String parameterName, final InputStream value, final long length)
            throws SQLException {
        accept(CallableStatement::setAsciiStream, getDelegateCallableStatement(), parameterName, value, length);
    }

    @Override
    public void setBigDecimal(final String parameterName, final BigDecimal value) throws SQLException {
        accept(CallableStatement::setBigDecimal, getDelegateCallableStatement(), parameterName, value);
    }

    @Override
    public void setBinaryStream(final String parameterName, final InputStream value) throws SQLException {
        accept(CallableStatement::setBinaryStream, getDelegateCallableStatement(), parameterName, value);
    }

    @Override
    public void setBinaryStream(final String parameterName, final InputStream value, final int length)
            throws SQLException {
        accept(CallableStatement::setBinaryStream, getDelegateCallableStatement(), parameterName, value, length);
    }

    @Override
    public void setBinaryStream(final String parameterName, final InputStream value, final long length)
            throws SQLException {
        accept(CallableStatement::setBinaryStream, getDelegateCallableStatement(), parameterName, value, length);
    }

    @Override
    public void setBlob(final String parameterName, final Blob value) throws SQLException {
        accept(CallableStatement::setBlob, getDelegateCallableStatement(), parameterName, value);
    }

    @Override
    public void setBlob(final String parameterName, final InputStream value) throws SQLException {
        accept(CallableStatement::setBlob, getDelegateCallableStatement(), parameterName, value);
    }

    @Override
    public void setBlob(final String parameterName, final InputStream value, final long length) throws SQLException {
        accept(CallableStatement::setBlob, getDelegateCallableStatement(), parameterName, value, length);
    }

    @Override
    public void setBoolean(final String parameterName, final boolean value) throws SQLException {
        accept(CallableStatement::setBoolean, getDelegateCallableStatement(), parameterName, value);
    }

    @Override
    public void setByte(final String parameterName, final byte value) throws SQLException {
        accept(CallableStatement::setByte, getDelegateCallableStatement(), parameterName, value);
    }

    @Override
    public void setBytes(final String parameterName, final byte[] value) throws SQLException {
        accept(CallableStatement::setBytes, getDelegateCallableStatement(), parameterName, value);
    }

    @Override
    public void setCharacterStream(final String parameterName, final Reader value) throws SQLException {
        accept(CallableStatement::setCharacterStream, getDelegateCallableStatement(), parameterName, value);
    }

    @Override
    public void setCharacterStream(final String parameterName, final Reader value, final int length)
            throws SQLException {
        accept(CallableStatement::setCharacterStream, getDelegateCallableStatement(), parameterName, value, length);
    }

    @Override
    public void setCharacterStream(final String parameterName, final Reader value, final long length)
            throws SQLException {
        accept(CallableStatement::setCharacterStream, getDelegateCallableStatement(), parameterName, value, length);
    }

    @Override
    public void setClob(final String parameterName, final Clob value) throws SQLException {
        accept(CallableStatement::setClob, getDelegateCallableStatement(), parameterName, value);
    }

    @Override
    public void setClob(final String parameterName, final Reader value) throws SQLException {
        accept(CallableStatement::setClob, getDelegateCallableStatement(), parameterName, value);
    }

    @Override
    public void setClob(final String parameterName, final Reader value, final long length) throws SQLException {
        accept(CallableStatement::setClob, getDelegateCallableStatement(), parameterName, value, length);
    }

    @Override
    public void setDate(final String parameterName, final Date value) throws SQLException {
        accept(CallableStatement::setDate, getDelegateCallableStatement(), parameterName, value);
    }

    @Override
    public void setDate(final String parameterName, final Date value, final Calendar cal) throws SQLException {
        accept(CallableStatement::setDate, getDelegateCallableStatement(), parameterName, value, cal);
    }

    @Override
    public void setDouble(final String parameterName, final double value) throws SQLException {
        accept(CallableStatement::setDouble, getDelegateCallableStatement(), parameterName, value);
    }

    @Override
    public void setFloat(final String parameterName, final float value) throws SQLException {
        accept(CallableStatement::setFloat, getDelegateCallableStatement(), parameterName, value);
    }

    @Override
    public void setInt(final String parameterName, final int value) throws SQLException {
        accept(CallableStatement::setInt, getDelegateCallableStatement(), parameterName, value);
    }

    @Override
    public void setLong(final String parameterName, final long value) throws SQLException {
        accept(CallableStatement::setLong, getDelegateCallableStatement(), parameterName, value);
    }

    @Override
    public void setNCharacterStream(final String parameterName, final Reader value) throws SQLException {
        accept(CallableStatement::setNCharacterStream, getDelegateCallableStatement(), parameterName, value);
    }

    @Override
    public void setNCharacterStream(final String parameterName, final Reader value, final long length)
            throws SQLException {
        accept(CallableStatement::setNCharacterStream, getDelegateCallableStatement(), parameterName, value, length);
    }

    @Override
    public void setNClob(final String parameterName, final NClob value) throws SQLException {
        accept(CallableStatement::setNClob, getDelegateCallableStatement(), parameterName, value);
    }

    @Override
    public void setNClob(final String parameterName, final Reader value) throws SQLException {
        accept(CallableStatement::setNClob, getDelegateCallableStatement(), parameterName, value);
    }

    @Override
    public void setNClob(final String parameterName, final Reader value, final long length) throws SQLException {
        accept(CallableStatement::setNClob, getDelegateCallableStatement(), parameterName, value, length);
    }

    @Override
    public void setNString(final String parameterName, final String value) throws SQLException {
        accept(CallableStatement::setNString, getDelegateCallableStatement(), parameterName, value);
    }

    @Override
    public void setNull(final String parameterName, final int sqlType) throws SQLException {
        accept(CallableStatement::setNull, getDelegateCallableStatement(), parameterName, sqlType);
    }

    @Override
    public void setNull(final String parameterName, final int sqlType, final String typeName) throws SQLException {
        accept(CallableStatement::setNull, getDelegateCallableStatement(), parameterName, sqlType, typeName);
    }

    @Override
    public void setObject(final String parameterName, final Object value) throws SQLException {
        accept(CallableStatement::setObject, getDelegateCallableStatement(), parameterName, value);
    }

    @Override
    public void setObject(final String parameterName, final Object value, final int targetSqlType) throws SQLException {
        accept(CallableStatement::setObject, getDelegateCallableStatement(), parameterName, value, targetSqlType);
    }

    @Override
    public void setObject(final String parameterName, final Object value, final int targetSqlType, final int scale)
            throws SQLException {
        accept(CallableStatement::setObject, getDelegateCallableStatement(), parameterName, value, targetSqlType,
                scale);
    }

    /**
     * @since 2.5.0
     */
    @Override
    public void setObject(final String parameterName, final Object value, final SQLType targetSqlType)
            throws SQLException {
        accept(CallableStatement::setObject, getDelegateCallableStatement(), parameterName, value, targetSqlType);
    }

    /**
     * @since 2.5.0
     */
    @Override
    public void setObject(final String parameterName, final Object value, final SQLType targetSqlType,
            final int scaleOrLength) throws SQLException {
        accept(CallableStatement::setObject, getDelegateCallableStatement(), parameterName, value, targetSqlType,
                scaleOrLength);
    }

    @Override
    public void setRowId(final String parameterName, final RowId value) throws SQLException {
        accept(CallableStatement::setRowId, getDelegateCallableStatement(), parameterName, value);
    }

    @Override
    public void setShort(final String parameterName, final short value) throws SQLException {
        accept(CallableStatement::setShort, getDelegateCallableStatement(), parameterName, value);
    }

    @Override
    public void setSQLXML(final String parameterName, final SQLXML value) throws SQLException {
        accept(CallableStatement::setSQLXML, getDelegateCallableStatement(), parameterName, value);
    }

    @Override
    public void setString(final String parameterName, final String value) throws SQLException {
        accept(CallableStatement::setString, getDelegateCallableStatement(), parameterName, value);
    }

    @Override
    public void setTime(final String parameterName, final Time value) throws SQLException {
        accept(CallableStatement::setTime, getDelegateCallableStatement(), parameterName, value);
    }

    @Override
    public void setTime(final String parameterName, final Time value, final Calendar cal) throws SQLException {
        accept(CallableStatement::setTime, getDelegateCallableStatement(), parameterName, value, cal);
    }

    @Override
    public void setTimestamp(final String parameterName, final Timestamp value) throws SQLException {
        accept(CallableStatement::setTimestamp, getDelegateCallableStatement(), parameterName, value);
    }

    @Override
    public void setTimestamp(final String parameterName, final Timestamp value, final Calendar cal)
            throws SQLException {
        accept(CallableStatement::setTimestamp, getDelegateCallableStatement(), parameterName, value, cal);
    }

    @Override
    public void setURL(final String parameterName, final URL value) throws SQLException {
        accept(CallableStatement::setURL, getDelegateCallableStatement(), parameterName, value);
    }

    @Override
    public boolean wasNull() throws SQLException {
        return apply(CallableStatement::wasNull, getDelegateCallableStatement());
    }

}
