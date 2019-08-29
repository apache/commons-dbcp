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
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLType;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;

/**
 * A base delegating implementation of {@link PreparedStatement}.
 * <p>
 * All of the methods from the {@link PreparedStatement} interface simply check to see that the
 * {@link PreparedStatement} is active, and call the corresponding method on the "delegate" provided in my constructor.
 * </p>
 * <p>
 * Extends AbandonedTrace to implement Statement tracking and logging of code which created the Statement. Tracking the
 * Statement ensures that the Connection which created it can close any open Statement's on Connection close.
 * </p>
 *
 * @param <S> PreparedStatement or a sub-type.
 * @since 2.0
 */
public class DelegatingPreparedStatement<S extends PreparedStatement> extends DelegatingStatement<S>
    implements PreparedStatement {

    /**
     * Create a wrapper for the Statement which traces this Statement to the Connection which created it and the code
     * which created it.
     *
     * @param statement  the {@link PreparedStatement} to delegate all calls to.
     * @param connection the {@link DelegatingConnection} that created this statement.
     */
    public DelegatingPreparedStatement(final DelegatingConnection<?> connection, final S statement) {
        super(connection, statement);
    }

    @Override
    public void addBatch() throws SQLException {
        accept(S::addBatch, getDelegate());
    }

    @Override
    public void clearParameters() throws SQLException {
        accept(S::clearParameters, getDelegate());
    }

    @Override
    public boolean execute() throws SQLException {
        return applyTo(S::execute, getDelegate(), false);
    }

    /**
     * @since 2.5.0
     */
    @Override
    public long executeLargeUpdate() throws SQLException {
        return applyTo(S::executeLargeUpdate, getDelegate(), 0L);
    }

    @Override
    public ResultSet executeQuery() throws SQLException {
        return apply(() -> DelegatingResultSet.wrapResultSet(this, getDelegate().executeQuery()));
    }

    @Override
    public int executeUpdate() throws SQLException {
        return applyTo(S::executeUpdate, getDelegate(), 0);
    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        return apply(S::getMetaData, getDelegate());
    }

    @Override
    public java.sql.ParameterMetaData getParameterMetaData() throws SQLException {
        return apply(S::getParameterMetaData, getDelegate());
    }

    @Override
    public void setArray(final int inputStream, final Array value) throws SQLException {
        accept(S::setArray, getDelegate(), inputStream, value);
    }

    @Override
    public void setAsciiStream(final int parameterIndex, final InputStream value) throws SQLException {
        accept(S::setAsciiStream, getDelegate(), parameterIndex, value);
    }

    @Override
    public void setAsciiStream(final int parameterIndex, final InputStream value, final int length)
        throws SQLException {
        accept(S::setAsciiStream, getDelegate(), parameterIndex, value, length);
    }

    @Override
    public void setAsciiStream(final int parameterIndex, final InputStream value, final long length)
        throws SQLException {
        accept(S::setAsciiStream, getDelegate(), parameterIndex, value, length);
    }

    @Override
    public void setBigDecimal(final int parameterIndex, final BigDecimal value) throws SQLException {
        accept(S::setBigDecimal, getDelegate(), parameterIndex, value);
    }

    @Override
    public void setBinaryStream(final int parameterIndex, final InputStream value) throws SQLException {
        accept(S::setBinaryStream, getDelegate(), parameterIndex, value);
    }

    @Override
    public void setBinaryStream(final int parameterIndex, final InputStream value, final int length)
        throws SQLException {
        accept(S::setBinaryStream, getDelegate(), parameterIndex, value, length);
    }

    @Override
    public void setBinaryStream(final int parameterIndex, final InputStream value, final long length)
        throws SQLException {
        accept(S::setBinaryStream, getDelegate(), parameterIndex, value, length);
    }

    @Override
    public void setBlob(final int parameterIndex, final Blob value) throws SQLException {
        accept(S::setBlob, getDelegate(), parameterIndex, value);
    }

    @Override
    public void setBlob(final int parameterIndex, final InputStream value) throws SQLException {
        accept(S::setBlob, getDelegate(), parameterIndex, value);
    }

    @Override
    public void setBlob(final int parameterIndex, final InputStream value, final long length) throws SQLException {
        accept(S::setBlob, getDelegate(), parameterIndex, value, length);
    }

    @Override
    public void setBoolean(final int parameterIndex, final boolean value) throws SQLException {
        accept(S::setBoolean, getDelegate(), parameterIndex, value);
    }

    @Override
    public void setByte(final int parameterIndex, final byte value) throws SQLException {
        accept(S::setByte, getDelegate(), parameterIndex, value);
    }

    @Override
    public void setBytes(final int parameterIndex, final byte[] value) throws SQLException {
        accept(S::setBytes, getDelegate(), parameterIndex, value);
    }

    @Override
    public void setCharacterStream(final int parameterIndex, final Reader value) throws SQLException {
        accept(S::setCharacterStream, getDelegate(), parameterIndex, value);
    }

    @Override
    public void setCharacterStream(final int parameterIndex, final Reader value, final int length) throws SQLException {
        accept(S::setCharacterStream, getDelegate(), parameterIndex, value, length);
    }

    @Override
    public void setCharacterStream(final int parameterIndex, final Reader value, final long length)
        throws SQLException {
        accept(S::setCharacterStream, getDelegate(), parameterIndex, value, length);
    }

    @Override
    public void setClob(final int parameterIndex, final Clob value) throws SQLException {
        accept(S::setClob, getDelegate(), parameterIndex, value);
    }

    @Override
    public void setClob(final int parameterIndex, final Reader value) throws SQLException {
        accept(S::setClob, getDelegate(), parameterIndex, value);
    }

    @Override
    public void setClob(final int parameterIndex, final Reader value, final long length) throws SQLException {
        accept(S::setClob, getDelegate(), parameterIndex, value, length);
    }

    @Override
    public void setDate(final int parameterIndex, final Date value) throws SQLException {
        accept(S::setDate, getDelegate(), parameterIndex, value);
    }

    @Override
    public void setDate(final int parameterIndex, final Date value, final Calendar calendar) throws SQLException {
        accept(S::setDate, getDelegate(), parameterIndex, value, calendar);
    }

    @Override
    public void setDouble(final int parameterIndex, final double value) throws SQLException {
        accept(S::setDouble, getDelegate(), parameterIndex, value);
    }

    @Override
    public void setFloat(final int parameterIndex, final float value) throws SQLException {
        accept(S::setFloat, getDelegate(), parameterIndex, value);
    }

    @Override
    public void setInt(final int parameterIndex, final int value) throws SQLException {
        accept(S::setInt, getDelegate(), parameterIndex, value);
    }

    @Override
    public void setLong(final int parameterIndex, final long value) throws SQLException {
        accept(S::setLong, getDelegate(), parameterIndex, value);
    }

    @Override
    public void setNCharacterStream(final int parameterIndex, final Reader value) throws SQLException {
        accept(S::setNCharacterStream, getDelegate(), parameterIndex, value);
    }

    @Override
    public void setNCharacterStream(final int parameterIndex, final Reader value, final long length)
        throws SQLException {
        accept(S::setNCharacterStream, getDelegate(), parameterIndex, value, length);
    }

    @Override
    public void setNClob(final int parameterIndex, final NClob value) throws SQLException {
        accept(S::setNClob, getDelegate(), parameterIndex, value);
    }

    @Override
    public void setNClob(final int parameterIndex, final Reader value) throws SQLException {
        accept(S::setNClob, getDelegate(), parameterIndex, value);
    }

    @Override
    public void setNClob(final int parameterIndex, final Reader value, final long length) throws SQLException {
        accept(S::setNClob, getDelegate(), parameterIndex, value, length);
    }

    @Override
    public void setNString(final int parameterIndex, final String value) throws SQLException {
        accept(S::setNString, getDelegate(), parameterIndex, value);
    }

    @Override
    public void setNull(final int parameterIndex, final int sqlType) throws SQLException {
        accept(S::setNull, getDelegate(), parameterIndex, sqlType);
    }

    @Override
    public void setNull(final int parameterIndex, final int sqlType, final String typeName) throws SQLException {
        accept(S::setNull, getDelegate(), parameterIndex, sqlType, typeName);
    }

    @Override
    public void setObject(final int parameterIndex, final Object value) throws SQLException {
        accept(S::setObject, getDelegate(), parameterIndex, value);
    }

    @Override
    public void setObject(final int parameterIndex, final Object value, final int targetSqlType) throws SQLException {
        accept(S::setObject, getDelegate(), parameterIndex, value, targetSqlType);
    }

    @Override
    public void setObject(final int parameterIndex, final Object value, final int targetSqlType, final int scale)
        throws SQLException {
        accept(S::setObject, getDelegate(), parameterIndex, value, targetSqlType, scale);
    }

    /**
     * @since 2.5.0
     */
    @Override
    public void setObject(final int parameterIndex, final Object value, final SQLType targetSqlType)
        throws SQLException {
        accept(S::setObject, getDelegate(), parameterIndex, value, targetSqlType);
    }

    /**
     * @since 2.5.0
     */
    @Override
    public void setObject(final int parameterIndex, final Object value, final SQLType targetSqlType,
        final int scaleOrLength) throws SQLException {
        accept(S::setObject, getDelegate(), parameterIndex, value, targetSqlType, scaleOrLength);
    }

    @Override
    public void setRef(final int parameterIndex, final Ref value) throws SQLException {
        accept(S::setRef, getDelegate(), parameterIndex, value);
    }

    @Override
    public void setRowId(final int parameterIndex, final RowId value) throws SQLException {
        accept(S::setRowId, getDelegate(), parameterIndex, value);
    }

    @Override
    public void setShort(final int parameterIndex, final short value) throws SQLException {
        accept(S::setShort, getDelegate(), parameterIndex, value);
    }

    @Override
    public void setSQLXML(final int parameterIndex, final SQLXML value) throws SQLException {
        accept(S::setSQLXML, getDelegate(), parameterIndex, value);
    }

    @Override
    public void setString(final int parameterIndex, final String value) throws SQLException {
        accept(S::setString, getDelegate(), parameterIndex, value);
    }

    @Override
    public void setTime(final int parameterIndex, final Time value) throws SQLException {
        accept(S::setTime, getDelegate(), parameterIndex, value);
    }

    @Override
    public void setTime(final int parameterIndex, final Time value, final Calendar calendar) throws SQLException {
        accept(S::setTime, getDelegate(), parameterIndex, value, calendar);
    }

    @Override
    public void setTimestamp(final int parameterIndex, final Timestamp value) throws SQLException {
        accept(S::setTimestamp, getDelegate(), parameterIndex, value);
    }

    @Override
    public void setTimestamp(final int parameterIndex, final Timestamp value, final Calendar calendar)
        throws SQLException {
        accept(S::setTimestamp, getDelegate(), parameterIndex, value, calendar);
    }

    /** @deprecated Use setAsciiStream(), setCharacterStream() or setNCharacterStream() */
    @Deprecated
    @Override
    public void setUnicodeStream(final int parameterIndex, final InputStream value, final int length)
        throws SQLException {
        accept(S::setUnicodeStream, getDelegate(), parameterIndex, value, length);
    }

    @Override
    public void setURL(final int parameterIndex, final java.net.URL value) throws SQLException {
        accept(S::setURL, getDelegate(), parameterIndex, value);
    }

    /**
     * Returns a String representation of this object.
     *
     * @return String
     */
    @SuppressWarnings("resource")
    @Override
    public synchronized String toString() {
        final Statement statement = getDelegate();
        return statement == null ? "NULL" : statement.toString();
    }
}
