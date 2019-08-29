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
 * @since 2.0
 */
public class DelegatingPreparedStatement extends DelegatingStatement implements PreparedStatement {

    /**
     * Create a wrapper for the Statement which traces this Statement to the Connection which created it and the code
     * which created it.
     *
     * @param statement the {@link PreparedStatement} to delegate all calls to.
     * @param connection the {@link DelegatingConnection} that created this statement.
     */
    public DelegatingPreparedStatement(final DelegatingConnection<?> connection, final PreparedStatement statement) {
        super(connection, statement);
    }

    @Override
    public void addBatch() throws SQLException {
        accept(PreparedStatement::addBatch, getDelegatePreparedStatement());
    }

    @Override
    public void clearParameters() throws SQLException {
        accept(PreparedStatement::clearParameters, getDelegatePreparedStatement());
    }

    @Override
    public boolean execute() throws SQLException {
        return applyTo(PreparedStatement::execute, getDelegatePreparedStatement(), false);
    }

    /**
     * @since 2.5.0
     */
    @Override
    public long executeLargeUpdate() throws SQLException {
        return applyTo(PreparedStatement::executeLargeUpdate, getDelegatePreparedStatement(), 0L);
    }

    @Override
    public ResultSet executeQuery() throws SQLException {
        return apply(() -> DelegatingResultSet.wrapResultSet(this, getDelegatePreparedStatement().executeQuery()));
    }

    @Override
    public int executeUpdate() throws SQLException {
        return applyTo(PreparedStatement::executeUpdate, getDelegatePreparedStatement(), 0);
    }

    private PreparedStatement getDelegatePreparedStatement() {
        return (PreparedStatement) getDelegate();
    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        return apply(PreparedStatement::getMetaData, getDelegatePreparedStatement());
    }

    @Override
    public java.sql.ParameterMetaData getParameterMetaData() throws SQLException {
        return apply(PreparedStatement::getParameterMetaData, getDelegatePreparedStatement());
    }

    @Override
    public void setArray(final int inputStream, final Array value) throws SQLException {
        accept(PreparedStatement::setArray, getDelegatePreparedStatement(), inputStream, value);
    }

    @Override
    public void setAsciiStream(final int parameterIndex, final InputStream value) throws SQLException {
        accept(PreparedStatement::setAsciiStream, getDelegatePreparedStatement(), parameterIndex, value);
    }

    @Override
    public void setAsciiStream(final int parameterIndex, final InputStream value, final int length) throws SQLException {
        accept(PreparedStatement::setAsciiStream, getDelegatePreparedStatement(), parameterIndex, value, length);
    }

    @Override
    public void setAsciiStream(final int parameterIndex, final InputStream value, final long length)
            throws SQLException {
        accept(PreparedStatement::setAsciiStream, getDelegatePreparedStatement(), parameterIndex, value, length);
    }

    @Override
    public void setBigDecimal(final int parameterIndex, final BigDecimal value) throws SQLException {
        accept(PreparedStatement::setBigDecimal, getDelegatePreparedStatement(), parameterIndex, value);
    }

    @Override
    public void setBinaryStream(final int parameterIndex, final InputStream value) throws SQLException {
        accept(PreparedStatement::setBinaryStream, getDelegatePreparedStatement(), parameterIndex, value);
    }

    @Override
    public void setBinaryStream(final int parameterIndex, final InputStream value, final int length)
            throws SQLException {
        accept(PreparedStatement::setBinaryStream, getDelegatePreparedStatement(), parameterIndex, value, length);
    }

    @Override
    public void setBinaryStream(final int parameterIndex, final InputStream value, final long length)
            throws SQLException {
        accept(PreparedStatement::setBinaryStream, getDelegatePreparedStatement(), parameterIndex, value, length);
    }

    @Override
    public void setBlob(final int parameterIndex, final Blob value) throws SQLException {
        accept(PreparedStatement::setBlob, getDelegatePreparedStatement(), parameterIndex, value);
    }

    @Override
    public void setBlob(final int parameterIndex, final InputStream value) throws SQLException {
        accept(PreparedStatement::setBlob, getDelegatePreparedStatement(), parameterIndex, value);
    }

    @Override
    public void setBlob(final int parameterIndex, final InputStream value, final long length)
            throws SQLException {
        accept(PreparedStatement::setBlob, getDelegatePreparedStatement(), parameterIndex, value, length);
    }

    @Override
    public void setBoolean(final int parameterIndex, final boolean value) throws SQLException {
        accept(PreparedStatement::setBoolean, getDelegatePreparedStatement(), parameterIndex, value);
    }

    @Override
    public void setByte(final int parameterIndex, final byte value) throws SQLException {
        accept(PreparedStatement::setByte, getDelegatePreparedStatement(), parameterIndex, value);
    }

    @Override
    public void setBytes(final int parameterIndex, final byte[] value) throws SQLException {
        accept(PreparedStatement::setBytes, getDelegatePreparedStatement(), parameterIndex, value);
    }

    @Override
    public void setCharacterStream(final int parameterIndex, final Reader value) throws SQLException {
        accept(PreparedStatement::setCharacterStream, getDelegatePreparedStatement(), parameterIndex, value);
    }

    @Override
    public void setCharacterStream(final int parameterIndex, final Reader value, final int length)
            throws SQLException {
        accept(PreparedStatement::setCharacterStream, getDelegatePreparedStatement(), parameterIndex, value, length);
    }

    @Override
    public void setCharacterStream(final int parameterIndex, final Reader value, final long length)
            throws SQLException {
        accept(PreparedStatement::setCharacterStream, getDelegatePreparedStatement(), parameterIndex, value, length);
    }

    @Override
    public void setClob(final int parameterIndex, final Clob value) throws SQLException {
        accept(PreparedStatement::setClob, getDelegatePreparedStatement(), parameterIndex, value);
    }

    @Override
    public void setClob(final int parameterIndex, final Reader value) throws SQLException {
        accept(PreparedStatement::setClob, getDelegatePreparedStatement(), parameterIndex, value);
    }

    @Override
    public void setClob(final int parameterIndex, final Reader value, final long length) throws SQLException {
        accept(PreparedStatement::setClob, getDelegatePreparedStatement(), parameterIndex, value, length);
    }

    @Override
    public void setDate(final int parameterIndex, final Date value) throws SQLException {
        accept(PreparedStatement::setDate, getDelegatePreparedStatement(), parameterIndex, value);
    }

    @Override
    public void setDate(final int parameterIndex, final Date value, final Calendar calendar) throws SQLException {
        accept(PreparedStatement::setDate, getDelegatePreparedStatement(), parameterIndex, value, calendar);
    }

    @Override
    public void setDouble(final int parameterIndex, final double value) throws SQLException {
        accept(PreparedStatement::setDouble, getDelegatePreparedStatement(), parameterIndex, value);
    }

    @Override
    public void setFloat(final int parameterIndex, final float value) throws SQLException {
        accept(PreparedStatement::setFloat, getDelegatePreparedStatement(), parameterIndex, value);
    }

    @Override
    public void setInt(final int parameterIndex, final int value) throws SQLException {
        accept(PreparedStatement::setInt, getDelegatePreparedStatement(), parameterIndex, value);
    }

    @Override
    public void setLong(final int parameterIndex, final long value) throws SQLException {
        accept(PreparedStatement::setLong, getDelegatePreparedStatement(), parameterIndex, value);
    }

    @Override
    public void setNCharacterStream(final int parameterIndex, final Reader value) throws SQLException {
        accept(PreparedStatement::setNCharacterStream, getDelegatePreparedStatement(), parameterIndex, value);
    }

    @Override
    public void setNCharacterStream(final int parameterIndex, final Reader value, final long length)
            throws SQLException {
        accept(PreparedStatement::setNCharacterStream, getDelegatePreparedStatement(), parameterIndex, value, length);
    }

    @Override
    public void setNClob(final int parameterIndex, final NClob value) throws SQLException {
        accept(PreparedStatement::setNClob, getDelegatePreparedStatement(), parameterIndex, value);
    }

    @Override
    public void setNClob(final int parameterIndex, final Reader value) throws SQLException {
        accept(PreparedStatement::setNClob, getDelegatePreparedStatement(), parameterIndex, value);
    }

    @Override
    public void setNClob(final int parameterIndex, final Reader value, final long length) throws SQLException {
        accept(PreparedStatement::setNClob, getDelegatePreparedStatement(), parameterIndex, value, length);
    }

    @Override
    public void setNString(final int parameterIndex, final String value) throws SQLException {
        accept(PreparedStatement::setNString, getDelegatePreparedStatement(), parameterIndex, value);
    }

    @Override
    public void setNull(final int parameterIndex, final int sqlType) throws SQLException {
        accept(PreparedStatement::setNull, getDelegatePreparedStatement(), parameterIndex, sqlType);
    }

    @Override
    public void setNull(final int parameterIndex, final int sqlType, final String typeName) throws SQLException {
        accept(PreparedStatement::setNull, getDelegatePreparedStatement(), parameterIndex, sqlType, typeName);
    }

    @Override
    public void setObject(final int parameterIndex, final Object value) throws SQLException {
        accept(PreparedStatement::setObject, getDelegatePreparedStatement(), parameterIndex, value);
    }

    @Override
    public void setObject(final int parameterIndex, final Object value, final int targetSqlType) throws SQLException {
        accept(PreparedStatement::setObject, getDelegatePreparedStatement(), parameterIndex, value, targetSqlType);
    }

    @Override
    public void setObject(final int parameterIndex, final Object value, final int targetSqlType, final int scale)
            throws SQLException {
        accept(PreparedStatement::setObject, getDelegatePreparedStatement(), parameterIndex, value, targetSqlType,
                scale);
    }

    /**
     * @since 2.5.0
     */
    @Override
    public void setObject(final int parameterIndex, final Object value, final SQLType targetSqlType)
            throws SQLException {
        accept(PreparedStatement::setObject, getDelegatePreparedStatement(), parameterIndex, value, targetSqlType);
    }

    /**
     * @since 2.5.0
     */
    @Override
    public void setObject(final int parameterIndex, final Object value, final SQLType targetSqlType,
            final int scaleOrLength) throws SQLException {
        accept(PreparedStatement::setObject, getDelegatePreparedStatement(), parameterIndex, value, targetSqlType,
                scaleOrLength);
    }

    @Override
    public void setRef(final int parameterIndex, final Ref value) throws SQLException {
        accept(PreparedStatement::setRef, getDelegatePreparedStatement(), parameterIndex, value);
    }

    @Override
    public void setRowId(final int parameterIndex, final RowId value) throws SQLException {
        accept(PreparedStatement::setRowId, getDelegatePreparedStatement(), parameterIndex, value);
    }

    @Override
    public void setShort(final int parameterIndex, final short value) throws SQLException {
        accept(PreparedStatement::setShort, getDelegatePreparedStatement(), parameterIndex, value);
    }

    @Override
    public void setSQLXML(final int parameterIndex, final SQLXML value) throws SQLException {
        accept(PreparedStatement::setSQLXML, getDelegatePreparedStatement(), parameterIndex, value);
    }

    @Override
    public void setString(final int parameterIndex, final String value) throws SQLException {
        checkOpen();
        try {
            getDelegatePreparedStatement().setString(parameterIndex, value);
        } catch (final SQLException e) {
            handleException(e);
        }
    }

    @Override
    public void setTime(final int parameterIndex, final Time value) throws SQLException {
        accept(PreparedStatement::setTime, getDelegatePreparedStatement(), parameterIndex, value);
    }

    @Override
    public void setTime(final int parameterIndex, final Time value, final Calendar calendar) throws SQLException {
        accept(PreparedStatement::setTime, getDelegatePreparedStatement(), parameterIndex, value, calendar);
    }

    @Override
    public void setTimestamp(final int parameterIndex, final Timestamp value) throws SQLException {
        accept(PreparedStatement::setTimestamp, getDelegatePreparedStatement(), parameterIndex, value);
    }

    @Override
    public void setTimestamp(final int parameterIndex, final Timestamp value, final Calendar calendar)
            throws SQLException {
        accept(PreparedStatement::setTimestamp, getDelegatePreparedStatement(), parameterIndex, value, calendar);
    }

    /** @deprecated Use setAsciiStream(), setCharacterStream() or setNCharacterStream() */
    @Deprecated
    @Override
    public void setUnicodeStream(final int parameterIndex, final InputStream value, final int length)
            throws SQLException {
        accept(PreparedStatement::setUnicodeStream, getDelegatePreparedStatement(), parameterIndex, value, length);
    }

    @Override
    public void setURL(final int parameterIndex, final java.net.URL value) throws SQLException {
        accept(PreparedStatement::setURL, getDelegatePreparedStatement(), parameterIndex, value);
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
