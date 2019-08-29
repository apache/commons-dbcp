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
 * @param <S> CallableStatement or a sub-type.
 * @since 2.0
 */
public class DelegatingCallableStatement<S extends CallableStatement> extends DelegatingPreparedStatement<S>
    implements CallableStatement {

    /**
     * Creates a wrapper for the Statement which traces this Statement to the Connection which created it and the code
     * which created it.
     *
     * @param connection the {@link DelegatingConnection} that created this statement
     * @param statement  the {@link CallableStatement} to delegate all calls to
     */
    public DelegatingCallableStatement(final DelegatingConnection<?> connection, final S statement) {
        super(connection, statement);
    }

    @Override
    public Array getArray(final int parameterIndex) throws SQLException {
        return apply(S::getArray, getDelegate(), parameterIndex);
    }

    @Override
    public Array getArray(final String parameterName) throws SQLException {
        return apply(S::getArray, getDelegate(), parameterName);
    }

    @Override
    public BigDecimal getBigDecimal(final int parameterIndex) throws SQLException {
        return apply(S::getBigDecimal, getDelegate(), parameterIndex);
    }

    /** @deprecated Use {@link #getBigDecimal(int)} or {@link #getBigDecimal(String)} */
    @Override
    @Deprecated
    public BigDecimal getBigDecimal(final int parameterIndex, final int scale) throws SQLException {
        return apply(S::getBigDecimal, getDelegate(), parameterIndex, scale);
    }

    @Override
    public BigDecimal getBigDecimal(final String parameterName) throws SQLException {
        return apply(S::getBigDecimal, getDelegate(), parameterName);
    }

    @Override
    public Blob getBlob(final int parameterIndex) throws SQLException {
        return apply(S::getBlob, getDelegate(), parameterIndex);
    }

    @Override
    public Blob getBlob(final String parameterName) throws SQLException {
        return apply(S::getBlob, getDelegate(), parameterName);
    }

    @Override
    public boolean getBoolean(final int parameterIndex) throws SQLException {
        return applyTo(S::getBoolean, getDelegate(), parameterIndex, false);
    }

    @Override
    public boolean getBoolean(final String parameterName) throws SQLException {
        return applyTo(S::getBoolean, getDelegate(), parameterName, false);
    }

    @Override
    public byte getByte(final int parameterIndex) throws SQLException {
        return applyTo(S::getByte, getDelegate(), parameterIndex, (byte) 0);
    }

    @Override
    public byte getByte(final String parameterName) throws SQLException {
        return applyTo(S::getByte, getDelegate(), parameterName, (byte) 0);
    }

    @Override
    public byte[] getBytes(final int parameterIndex) throws SQLException {
        return apply(S::getBytes, getDelegate(), parameterIndex);
    }

    @Override
    public byte[] getBytes(final String parameterName) throws SQLException {
        return apply(S::getBytes, getDelegate(), parameterName);
    }

    @Override
    public Reader getCharacterStream(final int parameterIndex) throws SQLException {
        return apply(S::getCharacterStream, getDelegate(), parameterIndex);
    }

    @Override
    public Reader getCharacterStream(final String parameterName) throws SQLException {
        return apply(S::getCharacterStream, getDelegate(), parameterName);
    }

    @Override
    public Clob getClob(final int parameterIndex) throws SQLException {
        return apply(S::getClob, getDelegate(), parameterIndex);
    }

    @Override
    public Clob getClob(final String parameterName) throws SQLException {
        return apply(S::getClob, getDelegate(), parameterName);
    }

    @Override
    public Date getDate(final int parameterIndex) throws SQLException {
        return apply(S::getDate, getDelegate(), parameterIndex);
    }

    @Override
    public Date getDate(final int parameterIndex, final Calendar cal) throws SQLException {
        return apply(S::getDate, getDelegate(), parameterIndex, cal);
    }

    @Override
    public Date getDate(final String parameterName) throws SQLException {
        return apply(S::getDate, getDelegate(), parameterName);
    }

    @Override
    public Date getDate(final String parameterName, final Calendar cal) throws SQLException {
        return apply(S::getDate, getDelegate(), parameterName, cal);
    }

    @Override
    public double getDouble(final int parameterIndex) throws SQLException {
        return applyTo(S::getDouble, getDelegate(), parameterIndex, 0d);
    }

    @Override
    public double getDouble(final String parameterName) throws SQLException {
        return applyTo(S::getDouble, getDelegate(), parameterName, 0d);
    }

    @Override
    public float getFloat(final int parameterIndex) throws SQLException {
        return applyTo(S::getFloat, getDelegate(), parameterIndex, 0f);
    }

    @Override
    public float getFloat(final String parameterName) throws SQLException {
        return applyTo(S::getFloat, getDelegate(), parameterName, 0f);
    }

    @Override
    public int getInt(final int parameterIndex) throws SQLException {
        return applyTo(S::getInt, getDelegate(), parameterIndex, 0);
    }

    @Override
    public int getInt(final String parameterName) throws SQLException {
        return applyTo(S::getInt, getDelegate(), parameterName, 0);
    }

    @Override
    public long getLong(final int parameterIndex) throws SQLException {
        return applyTo(S::getLong, getDelegate(), parameterIndex, 0L);
    }

    @Override
    public long getLong(final String parameterName) throws SQLException {
        return applyTo(S::getLong, getDelegate(), parameterName, 0L);
    }

    @Override
    public Reader getNCharacterStream(final int parameterIndex) throws SQLException {
        return apply(S::getNCharacterStream, getDelegate(), parameterIndex);
    }

    @Override
    public Reader getNCharacterStream(final String parameterName) throws SQLException {
        return apply(S::getNCharacterStream, getDelegate(), parameterName);
    }

    @Override
    public NClob getNClob(final int parameterIndex) throws SQLException {
        return apply(S::getNClob, getDelegate(), parameterIndex);
    }

    @Override
    public NClob getNClob(final String parameterName) throws SQLException {
        return apply(S::getNClob, getDelegate(), parameterName);
    }

    @Override
    public String getNString(final int parameterIndex) throws SQLException {
        return apply(S::getNString, getDelegate(), parameterIndex);
    }

    @Override
    public String getNString(final String parameterName) throws SQLException {
        return apply(S::getNString, getDelegate(), parameterName);
    }

    @Override
    public Object getObject(final int parameterIndex) throws SQLException {
        return apply(S::getObject, getDelegate(), parameterIndex);
    }

    @Override
    public <T> T getObject(final int parameterIndex, final Class<T> type) throws SQLException {
        checkOpen();
        try {
            return getDelegate().getObject(parameterIndex, type);
        } catch (final SQLException e) {
            handleException(e);
            return null;
        }
    }

    @Override
    public Object getObject(final int parameterIndex, final Map<String, Class<?>> map) throws SQLException {
        return apply(S::getObject, getDelegate(), parameterIndex, map);
    }

    @Override
    public Object getObject(final String parameterName) throws SQLException {
        return apply(S::getObject, getDelegate(), parameterName);
    }

    @Override
    public <T> T getObject(final String parameterName, final Class<T> type) throws SQLException {
        checkOpen();
        try {
            return getDelegate().getObject(parameterName, type);
        } catch (final SQLException e) {
            handleException(e);
            return null;
        }
    }

    @Override
    public Object getObject(final String parameterName, final Map<String, Class<?>> map) throws SQLException {
        checkOpen();
        try {
            return getDelegate().getObject(parameterName, map);
        } catch (final SQLException e) {
            handleException(e);
            return null;
        }
    }

    @Override
    public Ref getRef(final int parameterIndex) throws SQLException {
        return apply(S::getRef, getDelegate(), parameterIndex);
    }

    @Override
    public Ref getRef(final String parameterName) throws SQLException {
        return apply(S::getRef, getDelegate(), parameterName);
    }

    @Override
    public RowId getRowId(final int parameterIndex) throws SQLException {
        return apply(S::getRowId, getDelegate(), parameterIndex);
    }

    @Override
    public RowId getRowId(final String parameterName) throws SQLException {
        return apply(S::getRowId, getDelegate(), parameterName);
    }

    @Override
    public short getShort(final int parameterIndex) throws SQLException {
        return applyTo(S::getShort, getDelegate(), parameterIndex, (short) 0);
    }

    @Override
    public short getShort(final String parameterName) throws SQLException {
        return applyTo(S::getShort, getDelegate(), parameterName, (short) 0);
    }

    @Override
    public SQLXML getSQLXML(final int parameterIndex) throws SQLException {
        return apply(S::getSQLXML, getDelegate(), parameterIndex);
    }

    @Override
    public SQLXML getSQLXML(final String parameterName) throws SQLException {
        return apply(S::getSQLXML, getDelegate(), parameterName);
    }

    @Override
    public String getString(final int parameterIndex) throws SQLException {
        return apply(S::getString, getDelegate(), parameterIndex);
    }

    @Override
    public String getString(final String parameterName) throws SQLException {
        return apply(S::getString, getDelegate(), parameterName);
    }

    @Override
    public Time getTime(final int parameterIndex) throws SQLException {
        return apply(S::getTime, getDelegate(), parameterIndex);
    }

    @Override
    public Time getTime(final int parameterIndex, final Calendar cal) throws SQLException {
        return apply(S::getTime, getDelegate(), parameterIndex, cal);
    }

    @Override
    public Time getTime(final String parameterName) throws SQLException {
        return apply(S::getTime, getDelegate(), parameterName);
    }

    @Override
    public Time getTime(final String parameterName, final Calendar cal) throws SQLException {
        return apply(S::getTime, getDelegate(), parameterName, cal);
    }

    @Override
    public Timestamp getTimestamp(final int parameterIndex) throws SQLException {
        return apply(S::getTimestamp, getDelegate(), parameterIndex);
    }

    @Override
    public Timestamp getTimestamp(final int parameterIndex, final Calendar cal) throws SQLException {
        return apply(S::getTimestamp, getDelegate(), parameterIndex, cal);
    }

    @Override
    public Timestamp getTimestamp(final String parameterName) throws SQLException {
        return apply(S::getTimestamp, getDelegate(), parameterName);
    }

    @Override
    public Timestamp getTimestamp(final String parameterName, final Calendar cal) throws SQLException {
        return apply(S::getTimestamp, getDelegate(), parameterName, cal);
    }

    @Override
    public URL getURL(final int parameterIndex) throws SQLException {
        return apply(S::getURL, getDelegate(), parameterIndex);
    }

    @Override
    public URL getURL(final String parameterName) throws SQLException {
        return apply(S::getURL, getDelegate(), parameterName);
    }

    @Override
    public void registerOutParameter(final int parameterIndex, final int sqlType) throws SQLException {
        accept(S::registerOutParameter, getDelegate(), parameterIndex, sqlType);
    }

    @Override
    public void registerOutParameter(final int parameterIndex, final int sqlType, final int scale) throws SQLException {
        accept(S::registerOutParameter, getDelegate(), parameterIndex, sqlType, scale);
    }

    @Override
    public void registerOutParameter(final int parameterIndex, final int sqlType, final String typeName)
        throws SQLException {
        accept(S::registerOutParameter, getDelegate(), parameterIndex, sqlType, typeName);
    }

    /**
     * @since 2.5.0
     */
    @Override
    public void registerOutParameter(final int parameterIndex, final SQLType sqlType) throws SQLException {
        accept(S::registerOutParameter, getDelegate(), parameterIndex, sqlType);
    }

    /**
     * @since 2.5.0
     */
    @Override
    public void registerOutParameter(final int parameterIndex, final SQLType sqlType, final int scale)
        throws SQLException {
        accept(S::registerOutParameter, getDelegate(), parameterIndex, sqlType, scale);
    }

    /**
     * @since 2.5.0
     */
    @Override
    public void registerOutParameter(final int parameterIndex, final SQLType sqlType, final String typeName)
        throws SQLException {
        accept(S::registerOutParameter, getDelegate(), parameterIndex, sqlType, typeName);
    }

    @Override
    public void registerOutParameter(final String parameterName, final int sqlType) throws SQLException {
        accept(S::registerOutParameter, getDelegate(), parameterName, sqlType);
    }

    @Override
    public void registerOutParameter(final String parameterName, final int sqlType, final int scale)
        throws SQLException {
        accept(S::registerOutParameter, getDelegate(), parameterName, sqlType, scale);
    }

    @Override
    public void registerOutParameter(final String parameterName, final int sqlType, final String typeName)
        throws SQLException {
        accept(S::registerOutParameter, getDelegate(), parameterName, sqlType, typeName);
    }

    /**
     * @since 2.5.0
     */
    @Override
    public void registerOutParameter(final String parameterName, final SQLType sqlType) throws SQLException {
        accept(S::registerOutParameter, getDelegate(), parameterName, sqlType);
    }

    /**
     * @since 2.5.0
     */
    @Override
    public void registerOutParameter(final String parameterName, final SQLType sqlType, final int scale)
        throws SQLException {
        accept(S::registerOutParameter, getDelegate(), parameterName, sqlType, scale);
    }

    /**
     * @since 2.5.0
     */
    @Override
    public void registerOutParameter(final String parameterName, final SQLType sqlType, final String typeName)
        throws SQLException {
        accept(S::registerOutParameter, getDelegate(), parameterName, sqlType, typeName);
    }

    @Override
    public void setAsciiStream(final String parameterName, final InputStream value) throws SQLException {
        accept(S::setAsciiStream, getDelegate(), parameterName, value);
    }

    @Override
    public void setAsciiStream(final String parameterName, final InputStream value, final int length)
        throws SQLException {
        accept(S::setAsciiStream, getDelegate(), parameterName, value, length);
    }

    @Override
    public void setAsciiStream(final String parameterName, final InputStream value, final long length)
        throws SQLException {
        accept(S::setAsciiStream, getDelegate(), parameterName, value, length);
    }

    @Override
    public void setBigDecimal(final String parameterName, final BigDecimal value) throws SQLException {
        accept(S::setBigDecimal, getDelegate(), parameterName, value);
    }

    @Override
    public void setBinaryStream(final String parameterName, final InputStream value) throws SQLException {
        accept(S::setBinaryStream, getDelegate(), parameterName, value);
    }

    @Override
    public void setBinaryStream(final String parameterName, final InputStream value, final int length)
        throws SQLException {
        accept(S::setBinaryStream, getDelegate(), parameterName, value, length);
    }

    @Override
    public void setBinaryStream(final String parameterName, final InputStream value, final long length)
        throws SQLException {
        accept(S::setBinaryStream, getDelegate(), parameterName, value, length);
    }

    @Override
    public void setBlob(final String parameterName, final Blob value) throws SQLException {
        accept(S::setBlob, getDelegate(), parameterName, value);
    }

    @Override
    public void setBlob(final String parameterName, final InputStream value) throws SQLException {
        accept(S::setBlob, getDelegate(), parameterName, value);
    }

    @Override
    public void setBlob(final String parameterName, final InputStream value, final long length) throws SQLException {
        accept(S::setBlob, getDelegate(), parameterName, value, length);
    }

    @Override
    public void setBoolean(final String parameterName, final boolean value) throws SQLException {
        accept(S::setBoolean, getDelegate(), parameterName, value);
    }

    @Override
    public void setByte(final String parameterName, final byte value) throws SQLException {
        accept(S::setByte, getDelegate(), parameterName, value);
    }

    @Override
    public void setBytes(final String parameterName, final byte[] value) throws SQLException {
        accept(S::setBytes, getDelegate(), parameterName, value);
    }

    @Override
    public void setCharacterStream(final String parameterName, final Reader value) throws SQLException {
        accept(S::setCharacterStream, getDelegate(), parameterName, value);
    }

    @Override
    public void setCharacterStream(final String parameterName, final Reader value, final int length)
        throws SQLException {
        accept(S::setCharacterStream, getDelegate(), parameterName, value, length);
    }

    @Override
    public void setCharacterStream(final String parameterName, final Reader value, final long length)
        throws SQLException {
        accept(S::setCharacterStream, getDelegate(), parameterName, value, length);
    }

    @Override
    public void setClob(final String parameterName, final Clob value) throws SQLException {
        accept(S::setClob, getDelegate(), parameterName, value);
    }

    @Override
    public void setClob(final String parameterName, final Reader value) throws SQLException {
        accept(S::setClob, getDelegate(), parameterName, value);
    }

    @Override
    public void setClob(final String parameterName, final Reader value, final long length) throws SQLException {
        accept(S::setClob, getDelegate(), parameterName, value, length);
    }

    @Override
    public void setDate(final String parameterName, final Date value) throws SQLException {
        accept(S::setDate, getDelegate(), parameterName, value);
    }

    @Override
    public void setDate(final String parameterName, final Date value, final Calendar cal) throws SQLException {
        accept(S::setDate, getDelegate(), parameterName, value, cal);
    }

    @Override
    public void setDouble(final String parameterName, final double value) throws SQLException {
        accept(S::setDouble, getDelegate(), parameterName, value);
    }

    @Override
    public void setFloat(final String parameterName, final float value) throws SQLException {
        accept(S::setFloat, getDelegate(), parameterName, value);
    }

    @Override
    public void setInt(final String parameterName, final int value) throws SQLException {
        accept(S::setInt, getDelegate(), parameterName, value);
    }

    @Override
    public void setLong(final String parameterName, final long value) throws SQLException {
        accept(S::setLong, getDelegate(), parameterName, value);
    }

    @Override
    public void setNCharacterStream(final String parameterName, final Reader value) throws SQLException {
        accept(S::setNCharacterStream, getDelegate(), parameterName, value);
    }

    @Override
    public void setNCharacterStream(final String parameterName, final Reader value, final long length)
        throws SQLException {
        accept(S::setNCharacterStream, getDelegate(), parameterName, value, length);
    }

    @Override
    public void setNClob(final String parameterName, final NClob value) throws SQLException {
        accept(S::setNClob, getDelegate(), parameterName, value);
    }

    @Override
    public void setNClob(final String parameterName, final Reader value) throws SQLException {
        accept(S::setNClob, getDelegate(), parameterName, value);
    }

    @Override
    public void setNClob(final String parameterName, final Reader value, final long length) throws SQLException {
        accept(S::setNClob, getDelegate(), parameterName, value, length);
    }

    @Override
    public void setNString(final String parameterName, final String value) throws SQLException {
        accept(S::setNString, getDelegate(), parameterName, value);
    }

    @Override
    public void setNull(final String parameterName, final int sqlType) throws SQLException {
        accept(S::setNull, getDelegate(), parameterName, sqlType);
    }

    @Override
    public void setNull(final String parameterName, final int sqlType, final String typeName) throws SQLException {
        accept(S::setNull, getDelegate(), parameterName, sqlType, typeName);
    }

    @Override
    public void setObject(final String parameterName, final Object value) throws SQLException {
        accept(S::setObject, getDelegate(), parameterName, value);
    }

    @Override
    public void setObject(final String parameterName, final Object value, final int targetSqlType) throws SQLException {
        accept(S::setObject, getDelegate(), parameterName, value, targetSqlType);
    }

    @Override
    public void setObject(final String parameterName, final Object value, final int targetSqlType, final int scale)
        throws SQLException {
        accept(S::setObject, getDelegate(), parameterName, value, targetSqlType, scale);
    }

    /**
     * @since 2.5.0
     */
    @Override
    public void setObject(final String parameterName, final Object value, final SQLType targetSqlType)
        throws SQLException {
        accept(S::setObject, getDelegate(), parameterName, value, targetSqlType);
    }

    /**
     * @since 2.5.0
     */
    @Override
    public void setObject(final String parameterName, final Object value, final SQLType targetSqlType,
        final int scaleOrLength) throws SQLException {
        accept(S::setObject, getDelegate(), parameterName, value, targetSqlType, scaleOrLength);
    }

    @Override
    public void setRowId(final String parameterName, final RowId value) throws SQLException {
        accept(S::setRowId, getDelegate(), parameterName, value);
    }

    @Override
    public void setShort(final String parameterName, final short value) throws SQLException {
        accept(S::setShort, getDelegate(), parameterName, value);
    }

    @Override
    public void setSQLXML(final String parameterName, final SQLXML value) throws SQLException {
        accept(S::setSQLXML, getDelegate(), parameterName, value);
    }

    @Override
    public void setString(final String parameterName, final String value) throws SQLException {
        accept(S::setString, getDelegate(), parameterName, value);
    }

    @Override
    public void setTime(final String parameterName, final Time value) throws SQLException {
        accept(S::setTime, getDelegate(), parameterName, value);
    }

    @Override
    public void setTime(final String parameterName, final Time value, final Calendar cal) throws SQLException {
        accept(S::setTime, getDelegate(), parameterName, value, cal);
    }

    @Override
    public void setTimestamp(final String parameterName, final Timestamp value) throws SQLException {
        accept(S::setTimestamp, getDelegate(), parameterName, value);
    }

    @Override
    public void setTimestamp(final String parameterName, final Timestamp value, final Calendar cal)
        throws SQLException {
        accept(S::setTimestamp, getDelegate(), parameterName, value, cal);
    }

    @Override
    public void setURL(final String parameterName, final URL value) throws SQLException {
        accept(S::setURL, getDelegate(), parameterName, value);
    }

    @Override
    public boolean wasNull() throws SQLException {
        return apply(S::wasNull, getDelegate());
    }

}
