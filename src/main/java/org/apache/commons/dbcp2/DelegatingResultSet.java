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
import java.sql.Connection;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLType;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;

/**
 * A base delegating implementation of {@link ResultSet}.
 * <p>
 * All of the methods from the {@link ResultSet} interface simply call the corresponding method on the "delegate"
 * provided in my constructor.
 * </p>
 * <p>
 * Extends AbandonedTrace to implement result set tracking and logging of code which created the ResultSet. Tracking the
 * ResultSet ensures that the Statement which created it can close any open ResultSet's on Statement close.
 * </p>
 *
 * @since 2.0
 */
public final class DelegatingResultSet extends AbandonedTrace implements ResultSet {

    /**
     * Wraps the given result set in a delegate.
     *
     * @param connection The Connection which created the ResultSet.
     * @param resultSet  The ResultSet to wrap.
     * @return a new delegate.
     */
    public static ResultSet wrapResultSet(final Connection connection, final ResultSet resultSet) {
        if (null == resultSet) {
            return null;
        }
        return new DelegatingResultSet(connection, resultSet);
    }

    /**
     * Wraps the given result set in a delegate.
     *
     * @param statement The Statement which created the ResultSet.
     * @param resultSet The ResultSet to wrap.
     * @return a new delegate.
     */
    public static ResultSet wrapResultSet(final Statement statement, final ResultSet resultSet) {
        if (null == resultSet) {
            return null;
        }
        return new DelegatingResultSet(statement, resultSet);
    }

    /** My delegate. **/
    private final ResultSet resultSet;

    /** The Statement that created me, if any. **/
    private Statement statement;

    /** The Connection that created me, if any. **/
    private Connection connection;

    /**
     * Creates a wrapper for the ResultSet which traces this ResultSet to the Connection which created it (via, for
     * example DatabaseMetadata, and the code which created it.
     * <p>
     * Private to ensure all construction is {@link #wrapResultSet(Connection, ResultSet)}
     * </p>
     *
     * @param conn Connection which created this ResultSet
     * @param res  ResultSet to wrap
     */
    private DelegatingResultSet(final Connection conn, final ResultSet res) {
        super((AbandonedTrace) conn);
        this.connection = conn;
        this.resultSet = res;
    }

    /**
     * Creates a wrapper for the ResultSet which traces this ResultSet to the Statement which created it and the code
     * which created it.
     * <p>
     * Private to ensure all construction is {@link #wrapResultSet(Statement, ResultSet)}
     * </p>
     *
     * @param statement The Statement which created the ResultSet.
     * @param resultSet The ResultSet to wrap.
     */
    private DelegatingResultSet(final Statement statement, final ResultSet resultSet) {
        super((AbandonedTrace) statement);
        this.statement = statement;
        this.resultSet = resultSet;
    }

    @Override
    public boolean absolute(final int row) throws SQLException {
        return apply(resultSet::absolute, row);
    }

    @Override
    public void afterLast() throws SQLException {
        accept(resultSet::afterLast);
    }

    @Override
    public void beforeFirst() throws SQLException {
        accept(resultSet::beforeFirst);
    }

    @Override
    public void cancelRowUpdates() throws SQLException {
        accept(resultSet::cancelRowUpdates);
    }

    @Override
    public void clearWarnings() throws SQLException {
        accept(resultSet::clearWarnings);
    }

    /**
     * Wrapper for close of ResultSet which removes this result set from being traced then calls close on the original
     * ResultSet.
     */
    @Override
    public void close() throws SQLException {
        try {
            if (statement != null) {
                removeThisTrace(statement);
                statement = null;
            }
            if (connection != null) {
                removeThisTrace(connection);
                connection = null;
            }
            resultSet.close();
        } catch (final SQLException e) {
            handleException(e);
        }
    }

    @Override
    public void deleteRow() throws SQLException {
        accept(resultSet::deleteRow);
    }

    @Override
    public int findColumn(final String columnLabel) throws SQLException {
        return apply(resultSet::findColumn, columnLabel);
    }

    @Override
    public boolean first() throws SQLException {
        return getAsBoolean(resultSet::first);
    }

    @Override
    public Array getArray(final int columnIndex) throws SQLException {
        return apply(resultSet::getArray, columnIndex);
    }

    @Override
    public Array getArray(final String columnLabel) throws SQLException {
        return apply(resultSet::getArray, columnLabel);
    }

    @Override
    public InputStream getAsciiStream(final int columnIndex) throws SQLException {
        return apply(resultSet::getAsciiStream, columnIndex);
    }

    @Override
    public InputStream getAsciiStream(final String columnLabel) throws SQLException {
        return apply(resultSet::getAsciiStream, columnLabel);
    }

    @Override
    public BigDecimal getBigDecimal(final int columnIndex) throws SQLException {
        return apply(resultSet::getBigDecimal, columnIndex);
    }

    /** @deprecated Use {@link #getBigDecimal(int)} */
    @Deprecated
    @Override
    public BigDecimal getBigDecimal(final int columnIndex, final int scale) throws SQLException {
        return apply(resultSet::getBigDecimal, columnIndex, scale);
    }

    @Override
    public BigDecimal getBigDecimal(final String columnLabel) throws SQLException {
        return apply(resultSet::getBigDecimal, columnLabel);
    }

    /** @deprecated Use {@link #getBigDecimal(String)} */
    @Deprecated
    @Override
    public BigDecimal getBigDecimal(final String columnLabel, final int scale) throws SQLException {
        return apply(resultSet::getBigDecimal, columnLabel, scale);
    }

    @Override
    public InputStream getBinaryStream(final int columnIndex) throws SQLException {
        return apply(resultSet::getBinaryStream, columnIndex);
    }

    @Override
    public InputStream getBinaryStream(final String columnLabel) throws SQLException {
        return apply(resultSet::getBinaryStream, columnLabel);
    }

    @Override
    public Blob getBlob(final int columnIndex) throws SQLException {
        return apply(resultSet::getBlob, columnIndex);
    }

    @Override
    public Blob getBlob(final String columnLabel) throws SQLException {
        return apply(resultSet::getBlob, columnLabel);
    }

    @Override
    public boolean getBoolean(final int columnIndex) throws SQLException {
        return applyTo(resultSet::getBoolean, columnIndex, false);
    }

    @Override
    public boolean getBoolean(final String columnLabel) throws SQLException {
        return applyTo(resultSet::getBoolean, columnLabel, false);
    }

    @Override
    public byte getByte(final int columnIndex) throws SQLException {
        return applyTo(resultSet::getByte, columnIndex, (byte) 0);
    }

    @Override
    public byte getByte(final String columnLabel) throws SQLException {
        return applyTo(resultSet::getByte, columnLabel, (byte) 0);
    }

    @Override
    public byte[] getBytes(final int columnIndex) throws SQLException {
        return apply(resultSet::getBytes, columnIndex);
    }

    @Override
    public byte[] getBytes(final String columnLabel) throws SQLException {
        return apply(resultSet::getBytes, columnLabel);
    }

    @Override
    public Reader getCharacterStream(final int columnIndex) throws SQLException {
        return apply(resultSet::getCharacterStream, columnIndex);
    }

    @Override
    public Reader getCharacterStream(final String columnLabel) throws SQLException {
        return apply(resultSet::getCharacterStream, columnLabel);
    }

    @Override
    public Clob getClob(final int columnIndex) throws SQLException {
        return apply(resultSet::getClob, columnIndex);
    }

    @Override
    public Clob getClob(final String columnLabel) throws SQLException {
        return apply(resultSet::getClob, columnLabel);
    }

    @Override
    public int getConcurrency() throws SQLException {
        return applyTo0(resultSet::getConcurrency);
    }

    @Override
    public String getCursorName() throws SQLException {
        return apply(resultSet::getCursorName);
    }

    @Override
    public Date getDate(final int columnIndex) throws SQLException {
        return apply(resultSet::getDate, columnIndex);
    }

    @Override
    public Date getDate(final int columnIndex, final Calendar cal) throws SQLException {
        return apply(resultSet::getDate, columnIndex, cal);
    }

    @Override
    public Date getDate(final String columnLabel) throws SQLException {
        return apply(resultSet::getDate, columnLabel);
    }

    @Override
    public Date getDate(final String columnLabel, final Calendar cal) throws SQLException {
        return apply(resultSet::getDate, columnLabel, cal);
    }

    /**
     * Gets my delegate.
     *
     * @return my delegate.
     */
    public ResultSet getDelegate() {
        return resultSet;
    }

    @Override
    public double getDouble(final int columnIndex) throws SQLException {
        return applyTo(resultSet::getDouble, columnIndex, 0d);
    }

    @Override
    public double getDouble(final String columnLabel) throws SQLException {
        return applyTo(resultSet::getDouble, columnLabel, 0d);
    }

    @Override
    public int getFetchDirection() throws SQLException {
        return getAsInt(resultSet::getFetchDirection);

    }

    @Override
    public int getFetchSize() throws SQLException {
        return getAsInt(resultSet::getFetchSize);
    }

    @Override
    public float getFloat(final int columnIndex) throws SQLException {
        return applyTo(resultSet::getFloat, columnIndex, 0f);
    }

    @Override
    public float getFloat(final String columnLabel) throws SQLException {
        return applyTo(resultSet::getFloat, columnLabel, 0f);
    }

    @Override
    public int getHoldability() throws SQLException {
        return getAsInt(resultSet::getHoldability);
    }

    /**
     * If my underlying {@link ResultSet} is not a {@code DelegatingResultSet}, returns it, otherwise recursively
     * invokes this method on my delegate.
     * <p>
     * Hence this method will return the first delegate that is not a {@code DelegatingResultSet}, or {@code null} when
     * no non-{@code DelegatingResultSet} delegate can be found by traversing this chain.
     * </p>
     * <p>
     * This method is useful when you may have nested {@code DelegatingResultSet}s, and you want to make sure to obtain
     * a "genuine" {@link ResultSet}.
     * </p>
     *
     * @return the innermost delegate.
     */
    @SuppressWarnings("resource")
    public ResultSet getInnermostDelegate() {
        ResultSet r = resultSet;
        while (r != null && r instanceof DelegatingResultSet) {
            r = ((DelegatingResultSet) r).getDelegate();
            if (this == r) {
                return null;
            }
        }
        return r;
    }

    @Override
    public int getInt(final int columnIndex) throws SQLException {
        return applyIntTo(resultSet::getInt, columnIndex, 0);
    }

    @Override
    public int getInt(final String columnLabel) throws SQLException {
        return applyTo(resultSet::getInt, columnLabel, 0);
    }

    @Override
    public long getLong(final int columnIndex) throws SQLException {
        return applyIntTo(resultSet::getLong, columnIndex, 0L);
    }

    @Override
    public long getLong(final String columnLabel) throws SQLException {
        return applyTo(resultSet::getLong, columnLabel, 0L);
    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        return apply(resultSet::getMetaData);
    }

    @Override
    public Reader getNCharacterStream(final int columnIndex) throws SQLException {
        return apply(resultSet::getNCharacterStream, columnIndex);
    }

    @Override
    public Reader getNCharacterStream(final String columnLabel) throws SQLException {
        return apply(resultSet::getNCharacterStream, columnLabel);
    }

    @Override
    public NClob getNClob(final int columnIndex) throws SQLException {
        return apply(resultSet::getNClob, columnIndex);
    }

    @Override
    public NClob getNClob(final String columnLabel) throws SQLException {
        return apply(resultSet::getNClob, columnLabel);
    }

    @Override
    public String getNString(final int columnIndex) throws SQLException {
        return apply(resultSet::getNString, columnIndex);
    }

    @Override
    public String getNString(final String columnLabel) throws SQLException {
        return apply(resultSet::getNString, columnLabel);
    }

    @Override
    public Object getObject(final int columnIndex) throws SQLException {
        return apply(resultSet::getObject, columnIndex);
    }

    @Override
    public <T> T getObject(final int columnIndex, final Class<T> type) throws SQLException {
        return apply(Jdbc41Bridge::getObject, resultSet, columnIndex, type);
    }

    @Override
    public Object getObject(final int i, final Map<String, Class<?>> map) throws SQLException {
        return apply(resultSet::getObject, i, map);
    }

    @Override
    public Object getObject(final String columnLabel) throws SQLException {
        return apply(resultSet::getObject, columnLabel);
    }

    @Override
    public <T> T getObject(final String columnLabel, final Class<T> type) throws SQLException {
        return apply(Jdbc41Bridge::getObject, resultSet, columnLabel, type);
    }

    @Override
    public Object getObject(final String columnLabel, final Map<String, Class<?>> map) throws SQLException {
        return apply(resultSet::getObject, columnLabel, map);
    }

    @Override
    public Ref getRef(final int i) throws SQLException {
        return apply(resultSet::getRef, i);
    }

    @Override
    public Ref getRef(final String columnLabel) throws SQLException {
        return apply(resultSet::getRef, columnLabel);
    }

    @Override
    public int getRow() throws SQLException {
        return getAsInt(resultSet::getRow);
    }

    @Override
    public RowId getRowId(final int columnIndex) throws SQLException {
        return apply(resultSet::getRowId, columnIndex);
    }

    @Override
    public RowId getRowId(final String columnLabel) throws SQLException {
        return apply(resultSet::getRowId, columnLabel);
    }

    @Override
    public short getShort(final int columnIndex) throws SQLException {
        return applyTo(resultSet::getShort, columnIndex, (short) 0);
    }

    @Override
    public short getShort(final String columnLabel) throws SQLException {
        return applyTo(resultSet::getShort, columnLabel, (short) 0);
    }

    @Override
    public SQLXML getSQLXML(final int columnIndex) throws SQLException {
        return apply(resultSet::getSQLXML, columnIndex);
    }

    @Override
    public SQLXML getSQLXML(final String columnLabel) throws SQLException {
        return apply(resultSet::getSQLXML, columnLabel);
    }

    @Override
    public Statement getStatement() throws SQLException {
        return statement;
    }

    @Override
    public String getString(final int columnIndex) throws SQLException {
        return apply(resultSet::getString, columnIndex);
    }

    @Override
    public String getString(final String columnLabel) throws SQLException {
        return apply(resultSet::getString, columnLabel);
    }

    @Override
    public Time getTime(final int columnIndex) throws SQLException {
        return apply(resultSet::getTime, columnIndex);
    }

    @Override
    public Time getTime(final int columnIndex, final Calendar cal) throws SQLException {
        return apply(resultSet::getTime, columnIndex, cal);
    }

    @Override
    public Time getTime(final String columnLabel) throws SQLException {
        return apply(resultSet::getTime, columnLabel);
    }

    @Override
    public Time getTime(final String columnLabel, final Calendar cal) throws SQLException {
        return apply(resultSet::getTime, columnLabel, cal);
    }

    @Override
    public Timestamp getTimestamp(final int columnIndex) throws SQLException {
        return apply(resultSet::getTimestamp, columnIndex);
    }

    @Override
    public Timestamp getTimestamp(final int columnIndex, final Calendar cal) throws SQLException {
        return apply(resultSet::getTimestamp, columnIndex, cal);
    }

    @Override
    public Timestamp getTimestamp(final String columnLabel) throws SQLException {
        return apply(resultSet::getTimestamp, columnLabel);
    }

    @Override
    public Timestamp getTimestamp(final String columnLabel, final Calendar cal) throws SQLException {
        return apply(resultSet::getTimestamp, columnLabel, cal);
    }

    @Override
    public int getType() throws SQLException {
        return getAsInt(resultSet::getType);
    }

    /** @deprecated Use {@link #getCharacterStream(int)} */
    @Deprecated
    @Override
    public InputStream getUnicodeStream(final int columnIndex) throws SQLException {
        return apply(resultSet::getUnicodeStream, columnIndex);
    }

    /** @deprecated Use {@link #getCharacterStream(String)} */
    @Deprecated
    @Override
    public InputStream getUnicodeStream(final String columnLabel) throws SQLException {
        return apply(resultSet::getUnicodeStream, columnLabel);
    }

    @Override
    public java.net.URL getURL(final int columnIndex) throws SQLException {
        return apply(resultSet::getURL, columnIndex);
    }

    @Override
    public java.net.URL getURL(final String columnLabel) throws SQLException {
        return apply(resultSet::getURL, columnLabel);
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return apply(resultSet::getWarnings);
    }

    @Override
    protected void handleException(final SQLException e) throws SQLException {
        if (statement != null && statement instanceof DelegatingStatement) {
            ((DelegatingStatement<?>) statement).handleException(e);
        } else if (connection != null && connection instanceof DelegatingConnection) {
            ((DelegatingConnection<?>) connection).handleException(e);
        } else {
            throw e;
        }
    }

    @Override
    public void insertRow() throws SQLException {
        accept(resultSet::insertRow);
    }

    @Override
    public boolean isAfterLast() throws SQLException {
        return getAsBoolean(resultSet::isAfterLast);
    }

    @Override
    public boolean isBeforeFirst() throws SQLException {
        return getAsBoolean(resultSet::isBeforeFirst);
    }

    @Override
    public boolean isClosed() throws SQLException {
        return getAsBoolean(resultSet::isClosed);
    }

    @Override
    public boolean isFirst() throws SQLException {
        return getAsBoolean(resultSet::isFirst);
    }

    @Override
    public boolean isLast() throws SQLException {
        return getAsBoolean(resultSet::isLast);
    }

    @Override
    public boolean isWrapperFor(final Class<?> iface) throws SQLException {
        if (iface.isAssignableFrom(getClass())) {
            return true;
        } else if (iface.isAssignableFrom(resultSet.getClass())) {
            return true;
        } else {
            return resultSet.isWrapperFor(iface);
        }
    }

    @Override
    public boolean last() throws SQLException {
        return getAsBoolean(resultSet::last);
    }

    @Override
    public void moveToCurrentRow() throws SQLException {
        accept(resultSet::moveToCurrentRow);
    }

    @Override
    public void moveToInsertRow() throws SQLException {
        accept(resultSet::moveToInsertRow);
    }

    @Override
    public boolean next() throws SQLException {
        return getAsBoolean(resultSet::next);
    }

    @Override
    public boolean previous() throws SQLException {
        return getAsBoolean(resultSet::previous);
    }

    @Override
    public void refreshRow() throws SQLException {
        accept(resultSet::refreshRow);
    }

    @Override
    public boolean relative(final int rows) throws SQLException {
        return applyIntTo(resultSet::relative, rows, false);
    }

    @Override
    public boolean rowDeleted() throws SQLException {
        return getAsBoolean(resultSet::rowDeleted);
    }

    @Override
    public boolean rowInserted() throws SQLException {
        return getAsBoolean(resultSet::rowInserted);
    }

    @Override
    public boolean rowUpdated() throws SQLException {
        return getAsBoolean(resultSet::rowUpdated);
    }

    @Override
    public void setFetchDirection(final int direction) throws SQLException {
        acceptInt(resultSet::setFetchDirection, direction);
    }

    @Override
    public void setFetchSize(final int rows) throws SQLException {
        acceptInt(resultSet::setFetchSize, rows);
    }

    @Override
    public synchronized String toString() {
        return super.toString() + "[resultSet=" + resultSet + ", statement=" + statement + ", connection=" + connection
            + "]";
    }

    @Override
    public <T> T unwrap(final Class<T> iface) throws SQLException {
        if (iface.isAssignableFrom(getClass())) {
            return iface.cast(this);
        } else if (iface.isAssignableFrom(resultSet.getClass())) {
            return iface.cast(resultSet);
        } else {
            return resultSet.unwrap(iface);
        }
    }

    @Override
    public void updateArray(final int columnIndex, final Array x) throws SQLException {
        accept(resultSet::updateArray, columnIndex, x);
    }

    @Override
    public void updateArray(final String columnName, final Array x) throws SQLException {
        accept(resultSet::updateArray, columnName, x);
    }

    @Override
    public void updateAsciiStream(final int columnIndex, final InputStream inputStream) throws SQLException {
        accept(resultSet::updateAsciiStream, columnIndex, inputStream);
    }

    @Override
    public void updateAsciiStream(final int columnIndex, final InputStream x, final int length) throws SQLException {
        accept(resultSet::updateAsciiStream, columnIndex, x, length);
    }

    @Override
    public void updateAsciiStream(final int columnIndex, final InputStream inputStream, final long length)
        throws SQLException {
        accept(resultSet::updateAsciiStream, columnIndex, inputStream, length);
    }

    @Override
    public void updateAsciiStream(final String columnLabel, final InputStream inputStream) throws SQLException {
        accept(resultSet::updateAsciiStream, columnLabel, inputStream);
    }

    @Override
    public void updateAsciiStream(final String columnName, final InputStream x, final int length) throws SQLException {
        accept(resultSet::updateAsciiStream, columnName, x, length);
    }

    @Override
    public void updateAsciiStream(final String columnLabel, final InputStream inputStream, final long length)
        throws SQLException {
        accept(resultSet::updateAsciiStream, columnLabel, inputStream, length);
    }

    @Override
    public void updateBigDecimal(final int columnIndex, final BigDecimal x) throws SQLException {
        accept(resultSet::updateBigDecimal, columnIndex, x);
    }

    @Override
    public void updateBigDecimal(final String columnName, final BigDecimal x) throws SQLException {
        accept(resultSet::updateBigDecimal, columnName, x);
    }

    @Override
    public void updateBinaryStream(final int columnIndex, final InputStream inputStream) throws SQLException {
        accept(resultSet::updateBinaryStream, columnIndex, inputStream);
    }

    @Override
    public void updateBinaryStream(final int columnIndex, final InputStream x, final int length) throws SQLException {
        accept(resultSet::updateBinaryStream, columnIndex, x, length);
    }

    @Override
    public void updateBinaryStream(final int columnIndex, final InputStream inputStream, final long length)
        throws SQLException {
        accept(resultSet::updateBinaryStream, columnIndex, inputStream, length);
    }

    @Override
    public void updateBinaryStream(final String columnLabel, final InputStream inputStream) throws SQLException {
        accept(resultSet::updateBinaryStream, columnLabel, inputStream);
    }

    @Override
    public void updateBinaryStream(final String columnName, final InputStream x, final int length) throws SQLException {
        accept(resultSet::updateBinaryStream, columnName, x, length);
    }

    @Override
    public void updateBinaryStream(final String columnLabel, final InputStream inputStream, final long length)
        throws SQLException {
        accept(resultSet::updateBinaryStream, columnLabel, inputStream, length);
    }

    @Override
    public void updateBlob(final int columnIndex, final Blob x) throws SQLException {
        accept(resultSet::updateBlob, columnIndex, x);
    }

    @Override
    public void updateBlob(final int columnIndex, final InputStream inputStream) throws SQLException {
        accept(resultSet::updateBlob, columnIndex, inputStream);
    }

    @Override
    public void updateBlob(final int columnIndex, final InputStream inputStream, final long length)
        throws SQLException {
        accept(resultSet::updateBlob, columnIndex, inputStream, length);
    }

    @Override
    public void updateBlob(final String columnName, final Blob x) throws SQLException {
        accept(resultSet::updateBlob, columnName, x);
    }

    @Override
    public void updateBlob(final String columnLabel, final InputStream inputStream) throws SQLException {
        accept(resultSet::updateBlob, columnLabel, inputStream);
    }

    @Override
    public void updateBlob(final String columnLabel, final InputStream inputStream, final long length)
        throws SQLException {
        accept(resultSet::updateBlob, columnLabel, inputStream, length);
    }

    @Override
    public void updateBoolean(final int columnIndex, final boolean x) throws SQLException {
        accept(resultSet::updateBoolean, columnIndex, x);
    }

    @Override
    public void updateBoolean(final String columnName, final boolean x) throws SQLException {
        accept(resultSet::updateBoolean, columnName, x);
    }

    @Override
    public void updateByte(final int columnIndex, final byte x) throws SQLException {
        accept(resultSet::updateByte, columnIndex, x);
    }

    @Override
    public void updateByte(final String columnName, final byte x) throws SQLException {
        accept(resultSet::updateByte, columnName, x);
    }

    @Override
    public void updateBytes(final int columnIndex, final byte[] x) throws SQLException {
        accept(resultSet::updateBytes, columnIndex, x);
    }

    @Override
    public void updateBytes(final String columnName, final byte[] x) throws SQLException {
        accept(resultSet::updateBytes, columnName, x);
    }

    @Override
    public void updateCharacterStream(final int columnIndex, final Reader reader) throws SQLException {
        accept(resultSet::updateCharacterStream, columnIndex, reader);
    }

    @Override
    public void updateCharacterStream(final int columnIndex, final Reader x, final int length) throws SQLException {
        accept(resultSet::updateCharacterStream, columnIndex, x, length);
    }

    @Override
    public void updateCharacterStream(final int columnIndex, final Reader reader, final long length)
        throws SQLException {
        accept(resultSet::updateCharacterStream, columnIndex, reader, length);
    }

    @Override
    public void updateCharacterStream(final String columnLabel, final Reader reader) throws SQLException {
        accept(resultSet::updateCharacterStream, columnLabel, reader);
    }

    @Override
    public void updateCharacterStream(final String columnName, final Reader reader, final int length)
        throws SQLException {
        accept(resultSet::updateCharacterStream, columnName, reader, length);
    }

    @Override
    public void updateCharacterStream(final String columnLabel, final Reader reader, final long length)
        throws SQLException {
        accept(resultSet::updateCharacterStream, columnLabel, reader, length);
    }

    @Override
    public void updateClob(final int columnIndex, final Clob x) throws SQLException {
        accept(resultSet::updateClob, columnIndex, x);
    }

    @Override
    public void updateClob(final int columnIndex, final Reader reader) throws SQLException {
        accept(resultSet::updateClob, columnIndex, reader);
    }

    @Override
    public void updateClob(final int columnIndex, final Reader reader, final long length) throws SQLException {
        accept(resultSet::updateClob, columnIndex, reader, length);
    }

    @Override
    public void updateClob(final String columnName, final Clob x) throws SQLException {
        accept(resultSet::updateClob, columnName, x);
    }

    @Override
    public void updateClob(final String columnLabel, final Reader reader) throws SQLException {
        accept(resultSet::updateClob, columnLabel, reader);
    }

    @Override
    public void updateClob(final String columnLabel, final Reader reader, final long length) throws SQLException {
        accept(resultSet::updateClob, columnLabel, reader, length);
    }

    @Override
    public void updateDate(final int columnIndex, final Date x) throws SQLException {
        accept(resultSet::updateDate, columnIndex, x);
    }

    @Override
    public void updateDate(final String columnName, final Date x) throws SQLException {
        accept(resultSet::updateDate, columnName, x);
    }

    @Override
    public void updateDouble(final int columnIndex, final double x) throws SQLException {
        accept(resultSet::updateDouble, columnIndex, x);
    }

    @Override
    public void updateDouble(final String columnName, final double x) throws SQLException {
        accept(resultSet::updateDouble, columnName, x);
    }

    @Override
    public void updateFloat(final int columnIndex, final float x) throws SQLException {
        accept(resultSet::updateFloat, columnIndex, x);
    }

    @Override
    public void updateFloat(final String columnName, final float x) throws SQLException {
        accept(resultSet::updateFloat, columnName, x);
    }

    @Override
    public void updateInt(final int columnIndex, final int x) throws SQLException {
        accept(resultSet::updateInt, columnIndex, x);
    }

    @Override
    public void updateInt(final String columnName, final int x) throws SQLException {
        accept(resultSet::updateInt, columnName, x);
    }

    @Override
    public void updateLong(final int columnIndex, final long x) throws SQLException {
        accept(resultSet::updateLong, columnIndex, x);
    }

    @Override
    public void updateLong(final String columnName, final long x) throws SQLException {
        accept(resultSet::updateLong, columnName, x);
    }

    @Override
    public void updateNCharacterStream(final int columnIndex, final Reader reader) throws SQLException {
        accept(resultSet::updateNCharacterStream, columnIndex, reader);
    }

    @Override
    public void updateNCharacterStream(final int columnIndex, final Reader reader, final long length)
        throws SQLException {
        accept(resultSet::updateNCharacterStream, columnIndex, reader, length);
    }

    @Override
    public void updateNCharacterStream(final String columnLabel, final Reader reader) throws SQLException {
        accept(resultSet::updateNCharacterStream, columnLabel, reader);
    }

    @Override
    public void updateNCharacterStream(final String columnLabel, final Reader reader, final long length)
        throws SQLException {
        accept(resultSet::updateNCharacterStream, columnLabel, reader, length);
    }

    @Override
    public void updateNClob(final int columnIndex, final NClob value) throws SQLException {
        accept(resultSet::updateNClob, columnIndex, value);
    }

    @Override
    public void updateNClob(final int columnIndex, final Reader reader) throws SQLException {
        accept(resultSet::updateNClob, columnIndex, reader);
    }

    @Override
    public void updateNClob(final int columnIndex, final Reader reader, final long length) throws SQLException {
        accept(resultSet::updateNClob, columnIndex, reader, length);
    }

    @Override
    public void updateNClob(final String columnLabel, final NClob value) throws SQLException {
        accept(resultSet::updateNClob, columnLabel, value);
    }

    @Override
    public void updateNClob(final String columnLabel, final Reader reader) throws SQLException {
        accept(resultSet::updateNClob, columnLabel, reader);
    }

    @Override
    public void updateNClob(final String columnLabel, final Reader reader, final long length) throws SQLException {
        accept(resultSet::updateNClob, columnLabel, reader, length);
    }

    @Override
    public void updateNString(final int columnIndex, final String value) throws SQLException {
        accept(resultSet::updateNString, columnIndex, value);
    }

    @Override
    public void updateNString(final String columnLabel, final String value) throws SQLException {
        accept(resultSet::updateNString, columnLabel, value);
    }

    @Override
    public void updateNull(final int columnIndex) throws SQLException {
        acceptInt(resultSet::updateNull, columnIndex);
    }

    @Override
    public void updateNull(final String columnName) throws SQLException {
        accept(resultSet::updateNull, columnName);
    }

    @Override
    public void updateObject(final int columnIndex, final Object x) throws SQLException {
        accept(resultSet::updateObject, columnIndex, x);
    }

    @Override
    public void updateObject(final int columnIndex, final Object x, final int scale) throws SQLException {
        accept(resultSet::updateObject, columnIndex, x);
    }

    /**
     * @since 2.5.0
     */
    @Override
    public void updateObject(final int columnIndex, final Object x, final SQLType targetSqlType) throws SQLException {
        accept(resultSet::updateObject, columnIndex, x, targetSqlType);
    }

    /**
     * @since 2.5.0
     */
    @Override
    public void updateObject(final int columnIndex, final Object x, final SQLType targetSqlType,
        final int scaleOrLength) throws SQLException {
        accept(resultSet::updateObject, columnIndex, x, targetSqlType, scaleOrLength);
    }

    @Override
    public void updateObject(final String columnName, final Object x) throws SQLException {
        accept(resultSet::updateObject, columnName, x);
    }

    @Override
    public void updateObject(final String columnName, final Object x, final int scale) throws SQLException {
        accept(resultSet::updateObject, columnName, x);
    }

    /**
     * @since 2.5.0
     */
    @Override
    public void updateObject(final String columnLabel, final Object x, final SQLType targetSqlType)
        throws SQLException {
        accept(resultSet::updateObject, columnLabel, x, targetSqlType);
    }

    /**
     * @since 2.5.0
     */
    @Override
    public void updateObject(final String columnLabel, final Object x, final SQLType targetSqlType,
        final int scaleOrLength) throws SQLException {
        accept(resultSet::updateObject, columnLabel, x, targetSqlType, scaleOrLength);
    }

    @Override
    public void updateRef(final int columnIndex, final Ref x) throws SQLException {
        accept(resultSet::updateRef, columnIndex, x);
    }

    @Override
    public void updateRef(final String columnName, final Ref x) throws SQLException {
        accept(resultSet::updateRef, columnName, x);
    }

    @Override
    public void updateRow() throws SQLException {
        accept(resultSet::updateRow);
    }

    @Override
    public void updateRowId(final int columnIndex, final RowId value) throws SQLException {
        accept(resultSet::updateRowId, columnIndex, value);
    }

    @Override
    public void updateRowId(final String columnLabel, final RowId value) throws SQLException {
        accept(resultSet::updateRowId, columnLabel, value);
    }

    @Override
    public void updateShort(final int columnIndex, final short x) throws SQLException {
        accept(resultSet::updateShort, columnIndex, x);
    }

    @Override
    public void updateShort(final String columnName, final short x) throws SQLException {
        accept(resultSet::updateShort, columnName, x);
    }

    @Override
    public void updateSQLXML(final int columnIndex, final SQLXML value) throws SQLException {
        accept(resultSet::updateSQLXML, columnIndex, value);
    }

    @Override
    public void updateSQLXML(final String columnLabel, final SQLXML value) throws SQLException {
        accept(resultSet::updateSQLXML, columnLabel, value);
    }

    @Override
    public void updateString(final int columnIndex, final String x) throws SQLException {
        accept(resultSet::updateString, columnIndex, x);
    }

    @Override
    public void updateString(final String columnName, final String x) throws SQLException {
        accept(resultSet::updateString, columnName, x);
    }

    @Override
    public void updateTime(final int columnIndex, final Time x) throws SQLException {
        accept(resultSet::updateTime, columnIndex, x);
    }

    @Override
    public void updateTime(final String columnName, final Time x) throws SQLException {
        accept(resultSet::updateTime, columnName, x);
    }

    @Override
    public void updateTimestamp(final int columnIndex, final Timestamp x) throws SQLException {
        accept(resultSet::updateTimestamp, columnIndex, x);
    }

    @Override
    public void updateTimestamp(final String columnName, final Timestamp x) throws SQLException {
        accept(resultSet::updateTimestamp, columnName, x);
    }

    @Override
    public boolean wasNull() throws SQLException {
        return applyToFalse(resultSet::wasNull);
    }
}
