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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Tests for DelegatingResultSet.
 */
@SuppressWarnings({ "deprecation", "unchecked", "rawtypes" }) // BigDecimal methods, and casting for mocks
public class TestDelegatingResultSet {

    private TesterConnection testConn;
    private DelegatingConnection<Connection> conn;
    private ResultSet rs;
    private DelegatingResultSet delegate;

    @BeforeEach
    public void setUp() {
        testConn = new TesterConnection("foo", "bar");
        conn = new DelegatingConnection<>(testConn);
        rs = mock(ResultSet.class);
        delegate = (DelegatingResultSet) DelegatingResultSet.wrapResultSet(conn, rs);
    }

    @Test
    void testAbsoluteInteger() throws Exception {
        try {
            delegate.absolute(1);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).absolute(1);
    }

    @Test
    void testAbsolutes() throws Exception {
        try {
            delegate.absolute(1);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).absolute(1);
    }

    @Test
    void testAfterLast() throws Exception {
        try {
            delegate.afterLast();
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).afterLast();
    }

    @Test
    void testBeforeFirst() throws Exception {
        try {
            delegate.beforeFirst();
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).beforeFirst();
    }

    @Test
    void testCancelRowUpdates() throws Exception {
        try {
            delegate.cancelRowUpdates();
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).cancelRowUpdates();
    }

    @Test
    void testClearWarnings() throws Exception {
        try {
            delegate.clearWarnings();
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).clearWarnings();
    }

    @Test
    void testClose() throws Exception {
        try {
            delegate.close();
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).close();
    }

    @Test
    void testDeleteRow() throws Exception {
        try {
            delegate.deleteRow();
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).deleteRow();
    }

    @Test
    void testFindColumnString() throws Exception {
        try {
            delegate.findColumn("foo");
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).findColumn("foo");
    }

    @Test
    void testFirst() throws Exception {
        try {
            delegate.first();
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).first();
    }

    @Test
    void testGetArrayInteger() throws Exception {
        try {
            delegate.getArray(1);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getArray(1);
    }

    @Test
    void testGetArrayString() throws Exception {
        try {
            delegate.getArray("foo");
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getArray("foo");
    }

    @Test
    void testGetAsciiStreamInteger() throws Exception {
        try {
            delegate.getAsciiStream(1);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getAsciiStream(1);
    }

    @Test
    void testGetAsciiStreamString() throws Exception {
        try {
            delegate.getAsciiStream("foo");
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getAsciiStream("foo");
    }

    @Test
    void testGetBigDecimalInteger() throws Exception {
        try {
            delegate.getBigDecimal(1);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getBigDecimal(1);
    }

    // FIXME: this appears to be a bug
    @Disabled
    @Test
    void testGetBigDecimalIntegerInteger() throws Exception {
        try {
            delegate.getBigDecimal(1, 1);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getBigDecimal(1, 1);
    }

    @Test
    void testGetBigDecimalString() throws Exception {
        try {
            delegate.getBigDecimal("foo");
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getBigDecimal("foo");
    }

    // FIXME: this appears to be a bug
    @Disabled
    @Test
    void testGetBigDecimalStringInteger() throws Exception {
        try {
            delegate.getBigDecimal("foo", 1);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getBigDecimal("foo", 1);
    }

    @Test
    void testGetBinaryStreamInteger() throws Exception {
        try {
            delegate.getBinaryStream(1);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getBinaryStream(1);
    }

    @Test
    void testGetBinaryStreamString() throws Exception {
        try {
            delegate.getBinaryStream("foo");
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getBinaryStream("foo");
    }

    @Test
    void testGetBlobInteger() throws Exception {
        try {
            delegate.getBlob(1);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getBlob(1);
    }

    @Test
    void testGetBlobString() throws Exception {
        try {
            delegate.getBlob("foo");
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getBlob("foo");
    }

    @Test
    void testGetBooleanInteger() throws Exception {
        try {
            delegate.getBoolean(1);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getBoolean(1);
    }

    @Test
    void testGetBooleanString() throws Exception {
        try {
            delegate.getBoolean("foo");
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getBoolean("foo");
    }

    @Test
    void testGetByteInteger() throws Exception {
        try {
            delegate.getByte(1);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getByte(1);
    }

    @Test
    void testGetBytesInteger() throws Exception {
        try {
            delegate.getBytes(1);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getBytes(1);
    }

    @Test
    void testGetBytesString() throws Exception {
        try {
            delegate.getBytes("foo");
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getBytes("foo");
    }

    @Test
    void testGetByteString() throws Exception {
        try {
            delegate.getByte("foo");
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getByte("foo");
    }

    @Test
    void testGetCharacterStreamInteger() throws Exception {
        try {
            delegate.getCharacterStream(1);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getCharacterStream(1);
    }

    @Test
    void testGetCharacterStreamString() throws Exception {
        try {
            delegate.getCharacterStream("foo");
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getCharacterStream("foo");
    }

    @Test
    void testGetClobInteger() throws Exception {
        try {
            delegate.getClob(1);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getClob(1);
    }

    @Test
    void testGetClobString() throws Exception {
        try {
            delegate.getClob("foo");
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getClob("foo");
    }

    @Test
    void testGetConcurrency() throws Exception {
        try {
            delegate.getConcurrency();
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getConcurrency();
    }

    @Test
    void testGetCursorName() throws Exception {
        try {
            delegate.getCursorName();
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getCursorName();
    }

    @Test
    void testGetDateInteger() throws Exception {
        try {
            delegate.getDate(1);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getDate(1);
    }

    @Test
    void testGetDateIntegerCalendar() throws Exception {
        try {
            delegate.getDate(1, (java.util.Calendar) null);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getDate(1, (java.util.Calendar) null);
    }

    @Test
    void testGetDateString() throws Exception {
        try {
            delegate.getDate("foo");
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getDate("foo");
    }

    @Test
    void testGetDateStringCalendar() throws Exception {
        try {
            delegate.getDate("foo", (java.util.Calendar) null);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getDate("foo", (java.util.Calendar) null);
    }

    @Test
    void testGetDoubleInteger() throws Exception {
        try {
            delegate.getDouble(1);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getDouble(1);
    }

    @Test
    void testGetDoubleString() throws Exception {
        try {
            delegate.getDouble("foo");
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getDouble("foo");
    }

    @Test
    void testGetFetchDirection() throws Exception {
        try {
            delegate.getFetchDirection();
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getFetchDirection();
    }

    @Test
    void testGetFetchSize() throws Exception {
        try {
            delegate.getFetchSize();
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getFetchSize();
    }

    @Test
    void testGetFloatInteger() throws Exception {
        try {
            delegate.getFloat(1);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getFloat(1);
    }

    @Test
    void testGetFloatString() throws Exception {
        try {
            delegate.getFloat("foo");
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getFloat("foo");
    }

    @Test
    void testGetHoldability() throws Exception {
        try {
            delegate.getHoldability();
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getHoldability();
    }

    @Test
    void testGetIntInteger() throws Exception {
        try {
            delegate.getInt(1);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getInt(1);
    }

    @Test
    void testGetIntString() throws Exception {
        try {
            delegate.getInt("foo");
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getInt("foo");
    }

    @Test
    void testGetLongInteger() throws Exception {
        try {
            delegate.getLong(1);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getLong(1);
    }

    @Test
    void testGetLongString() throws Exception {
        try {
            delegate.getLong("foo");
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getLong("foo");
    }

    @Test
    void testGetMetaData() throws Exception {
        try {
            delegate.getMetaData();
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getMetaData();
    }

    @Test
    void testGetNCharacterStreamInteger() throws Exception {
        try {
            delegate.getNCharacterStream(1);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getNCharacterStream(1);
    }

    @Test
    void testGetNCharacterStreamString() throws Exception {
        try {
            delegate.getNCharacterStream("foo");
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getNCharacterStream("foo");
    }

    @Test
    void testGetNClobInteger() throws Exception {
        try {
            delegate.getNClob(1);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getNClob(1);
    }

    @Test
    void testGetNClobString() throws Exception {
        try {
            delegate.getNClob("foo");
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getNClob("foo");
    }

    @Test
    void testGetNStringInteger() throws Exception {
        try {
            delegate.getNString(1);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getNString(1);
    }

    @Test
    void testGetNStringString() throws Exception {
        try {
            delegate.getNString("foo");
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getNString("foo");
    }

    @Test
    void testGetObjectInteger() throws Exception {
        try {
            delegate.getObject(1);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getObject(1);
    }

    @Test
    void testGetObjectIntegerClass() throws Exception {
        try {
            delegate.getObject(1, Object.class);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getObject(1, Object.class);
    }

    @Test
    void testGetObjectIntegerMap() throws Exception {
        try {
            delegate.getObject(1, (java.util.Map) null);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getObject(1, (java.util.Map) null);
    }

    @Test
    void testGetObjectString() throws Exception {
        try {
            delegate.getObject("foo");
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getObject("foo");
    }

    @Test
    void testGetObjectStringClass() throws Exception {
        try {
            delegate.getObject("foo", Object.class);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getObject("foo", Object.class);
    }

    @Test
    void testGetObjectStringMap() throws Exception {
        try {
            delegate.getObject("foo", (java.util.Map) null);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getObject("foo", (java.util.Map) null);
    }

    @Test
    void testGetRefInteger() throws Exception {
        try {
            delegate.getRef(1);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getRef(1);
    }

    @Test
    void testGetRefString() throws Exception {
        try {
            delegate.getRef("foo");
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getRef("foo");
    }

    @Test
    void testGetRow() throws Exception {
        try {
            delegate.getRow();
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getRow();
    }

    @Test
    void testGetRowIdInteger() throws Exception {
        try {
            delegate.getRowId(1);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getRowId(1);
    }

    @Test
    void testGetRowIdString() throws Exception {
        try {
            delegate.getRowId("foo");
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getRowId("foo");
    }

    @Test
    void testGetShortInteger() throws Exception {
        try {
            delegate.getShort(1);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getShort(1);
    }

    @Test
    void testGetShortString() throws Exception {
        try {
            delegate.getShort("foo");
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getShort("foo");
    }

    @Test
    void testGetSQLXMLInteger() throws Exception {
        try {
            delegate.getSQLXML(1);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getSQLXML(1);
    }

    @Test
    void testGetSQLXMLString() throws Exception {
        try {
            delegate.getSQLXML("foo");
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getSQLXML("foo");
    }

    /**
     * This method is a bit special. It actually calls statement in the
     * {@link DelegatingResultSet} object itself, instead of calling in the
     * underlying {@link ResultSet}.
     *
     * @throws Exception
     */
    @Test
    void testGetStatement() throws Exception {
        try {
            delegate.getStatement();
        } catch (final SQLException e) {
        }
        verify(rs, times(0)).getStatement();
    }

    @Test
    void testGetStringInteger() throws Exception {
        try {
            delegate.getString(1);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getString(1);
    }

    @Test
    void testGetStringString() throws Exception {
        try {
            delegate.getString("foo");
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getString("foo");
    }

    @Test
    void testGetTimeInteger() throws Exception {
        try {
            delegate.getTime(1);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getTime(1);
    }

    @Test
    void testGetTimeIntegerCalendar() throws Exception {
        try {
            delegate.getTime(1, (java.util.Calendar) null);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getTime(1, (java.util.Calendar) null);
    }

    @Test
    void testGetTimestampInteger() throws Exception {
        try {
            delegate.getTimestamp(1);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getTimestamp(1);
    }

    @Test
    void testGetTimestampIntegerCalendar() throws Exception {
        try {
            delegate.getTimestamp(1, (java.util.Calendar) null);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getTimestamp(1, (java.util.Calendar) null);
    }

    @Test
    void testGetTimestampString() throws Exception {
        try {
            delegate.getTimestamp("foo");
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getTimestamp("foo");
    }

    @Test
    void testGetTimestampStringCalendar() throws Exception {
        try {
            delegate.getTimestamp("foo", (java.util.Calendar) null);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getTimestamp("foo", (java.util.Calendar) null);
    }

    @Test
    void testGetTimeString() throws Exception {
        try {
            delegate.getTime("foo");
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getTime("foo");
    }

    @Test
    void testGetTimeStringCalendar() throws Exception {
        try {
            delegate.getTime("foo", (java.util.Calendar) null);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getTime("foo", (java.util.Calendar) null);
    }

    @Test
    void testGetType() throws Exception {
        try {
            delegate.getType();
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getType();
    }

    @Test
    void testGetUnicodeStreamInteger() throws Exception {
        try {
            delegate.getUnicodeStream(1);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getUnicodeStream(1);
    }

    @Test
    void testGetUnicodeStreamString() throws Exception {
        try {
            delegate.getUnicodeStream("foo");
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getUnicodeStream("foo");
    }

    @Test
    void testGetURLInteger() throws Exception {
        try {
            delegate.getURL(1);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getURL(1);
    }

    @Test
    void testGetURLString() throws Exception {
        try {
            delegate.getURL("foo");
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getURL("foo");
    }

    @Test
    void testGetWarnings() throws Exception {
        try {
            delegate.getWarnings();
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).getWarnings();
    }

    @Test
    void testInsertRow() throws Exception {
        try {
            delegate.insertRow();
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).insertRow();
    }

    @Test
    void testIsAfterLast() throws Exception {
        try {
            delegate.isAfterLast();
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).isAfterLast();
    }

    @Test
    void testIsBeforeFirst() throws Exception {
        try {
            delegate.isBeforeFirst();
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).isBeforeFirst();
    }

    @Test
    void testIsClosed() throws Exception {
        try {
            delegate.isClosed();
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).isClosed();
    }

    @Test
    void testIsFirst() throws Exception {
        try {
            delegate.isFirst();
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).isFirst();
    }

    @Test
    void testIsLast() throws Exception {
        try {
            delegate.isLast();
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).isLast();
    }

    @Test
    void testLast() throws Exception {
        try {
            delegate.last();
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).last();
    }

    @Test
    void testMoveToCurrentRow() throws Exception {
        try {
            delegate.moveToCurrentRow();
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).moveToCurrentRow();
    }

    @Test
    void testMoveToInsertRow() throws Exception {
        try {
            delegate.moveToInsertRow();
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).moveToInsertRow();
    }

    @Test
    void testNext() throws Exception {
        try {
            delegate.next();
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).next();
    }

    @Test
    void testPrevious() throws Exception {
        try {
            delegate.previous();
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).previous();
    }

    @Test
    void testRefreshRow() throws Exception {
        try {
            delegate.refreshRow();
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).refreshRow();
    }

    @Test
    void testRelativeInteger() throws Exception {
        try {
            delegate.relative(1);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).relative(1);
    }

    @Test
    void testRowDeleted() throws Exception {
        try {
            delegate.rowDeleted();
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).rowDeleted();
    }

    @Test
    void testRowInserted() throws Exception {
        try {
            delegate.rowInserted();
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).rowInserted();
    }

    @Test
    void testRowUpdated() throws Exception {
        try {
            delegate.rowUpdated();
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).rowUpdated();
    }

    @Test
    void testSetFetchDirectionInteger() throws Exception {
        try {
            delegate.setFetchDirection(1);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).setFetchDirection(1);
    }

    @Test
    void testSetFetchSizeInteger() throws Exception {
        try {
            delegate.setFetchSize(1);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).setFetchSize(1);
    }

    @Test
    void testToString() {
        final String toString = delegate.toString();
        assertTrue(toString.contains("DelegatingResultSet"));
        assertTrue(toString.contains("Mock for ResultSet"));
    }

    @Test
    void testUpdateArrayIntegerArray() throws Exception {
        try {
            delegate.updateArray(1, (java.sql.Array) null);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateArray(1, (java.sql.Array) null);
    }

    @Test
    void testUpdateArrayStringArray() throws Exception {
        try {
            delegate.updateArray("foo", (java.sql.Array) null);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateArray("foo", (java.sql.Array) null);
    }

    @Test
    void testUpdateAsciiStreamIntegerInputStream() throws Exception {
        try {
            delegate.updateAsciiStream(1, (java.io.InputStream) null);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateAsciiStream(1, (java.io.InputStream) null);
    }

    @Test
    void testUpdateAsciiStreamIntegerInputStreamInteger() throws Exception {
        try {
            delegate.updateAsciiStream(1, (java.io.InputStream) null, 1);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateAsciiStream(1, (java.io.InputStream) null, 1);
    }

    @Test
    void testUpdateAsciiStreamIntegerInputStreamLong() throws Exception {
        try {
            delegate.updateAsciiStream(1, (java.io.InputStream) null, 1L);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateAsciiStream(1, (java.io.InputStream) null, 1L);
    }

    @Test
    void testUpdateAsciiStreamStringInputStream() throws Exception {
        try {
            delegate.updateAsciiStream("foo", (java.io.InputStream) null);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateAsciiStream("foo", (java.io.InputStream) null);
    }

    @Test
    void testUpdateAsciiStreamStringInputStreamInteger() throws Exception {
        try {
            delegate.updateAsciiStream("foo", (java.io.InputStream) null, 1);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateAsciiStream("foo", (java.io.InputStream) null, 1);
    }

    @Test
    void testUpdateAsciiStreamStringInputStreamLong() throws Exception {
        try {
            delegate.updateAsciiStream("foo", (java.io.InputStream) null, 1L);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateAsciiStream("foo", (java.io.InputStream) null, 1L);
    }

    @Test
    void testUpdateBigDecimalIntegerBigDecimal() throws Exception {
        try {
            delegate.updateBigDecimal(1, java.math.BigDecimal.valueOf(1.0d));
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateBigDecimal(1, java.math.BigDecimal.valueOf(1.0d));
    }

    @Test
    void testUpdateBigDecimalStringBigDecimal() throws Exception {
        try {
            delegate.updateBigDecimal("foo", java.math.BigDecimal.valueOf(1.0d));
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateBigDecimal("foo", java.math.BigDecimal.valueOf(1.0d));
    }

    @Test
    void testUpdateBinaryStreamIntegerInputStream() throws Exception {
        try {
            delegate.updateBinaryStream(1, (java.io.InputStream) null);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateBinaryStream(1, (java.io.InputStream) null);
    }

    @Test
    void testUpdateBinaryStreamIntegerInputStreamInteger() throws Exception {
        try {
            delegate.updateBinaryStream(1, (java.io.InputStream) null, 1);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateBinaryStream(1, (java.io.InputStream) null, 1);
    }

    @Test
    void testUpdateBinaryStreamIntegerInputStreamLong() throws Exception {
        try {
            delegate.updateBinaryStream(1, (java.io.InputStream) null, 1L);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateBinaryStream(1, (java.io.InputStream) null, 1L);
    }

    @Test
    void testUpdateBinaryStreamStringInputStream() throws Exception {
        try {
            delegate.updateBinaryStream("foo", (java.io.InputStream) null);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateBinaryStream("foo", (java.io.InputStream) null);
    }

    @Test
    void testUpdateBinaryStreamStringInputStreamInteger() throws Exception {
        try {
            delegate.updateBinaryStream("foo", (java.io.InputStream) null, 1);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateBinaryStream("foo", (java.io.InputStream) null, 1);
    }

    @Test
    void testUpdateBinaryStreamStringInputStreamLong() throws Exception {
        try {
            delegate.updateBinaryStream("foo", (java.io.InputStream) null, 1L);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateBinaryStream("foo", (java.io.InputStream) null, 1L);
    }

    @Test
    void testUpdateBlobIntegerBlob() throws Exception {
        try {
            delegate.updateBlob(1, (java.sql.Blob) null);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateBlob(1, (java.sql.Blob) null);
    }

    @Test
    void testUpdateBlobIntegerInputStream() throws Exception {
        try {
            delegate.updateBlob(1, (java.io.InputStream) null);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateBlob(1, (java.io.InputStream) null);
    }

    @Test
    void testUpdateBlobIntegerInputStreamLong() throws Exception {
        try {
            delegate.updateBlob(1, (java.io.InputStream) null, 1L);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateBlob(1, (java.io.InputStream) null, 1L);
    }

    @Test
    void testUpdateBlobStringBlob() throws Exception {
        try {
            delegate.updateBlob("foo", (java.sql.Blob) null);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateBlob("foo", (java.sql.Blob) null);
    }

    @Test
    void testUpdateBlobStringInputStream() throws Exception {
        try {
            delegate.updateBlob("foo", (java.io.InputStream) null);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateBlob("foo", (java.io.InputStream) null);
    }

    @Test
    void testUpdateBlobStringInputStreamLong() throws Exception {
        try {
            delegate.updateBlob("foo", (java.io.InputStream) null, 1L);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateBlob("foo", (java.io.InputStream) null, 1L);
    }

    @Test
    void testUpdateBooleanIntegerBoolean() throws Exception {
        try {
            delegate.updateBoolean(1, Boolean.TRUE);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateBoolean(1, Boolean.TRUE);
    }

    @Test
    void testUpdateBooleanStringBoolean() throws Exception {
        try {
            delegate.updateBoolean("foo", Boolean.TRUE);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateBoolean("foo", Boolean.TRUE);
    }

    @Test
    void testUpdateByteIntegerByte() throws Exception {
        try {
            delegate.updateByte(1, (byte) 1);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateByte(1, (byte) 1);
    }

    @Test
    void testUpdateBytesIntegerByteArray() throws Exception {
        try {
            delegate.updateBytes(1, new byte[] { 1 });
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateBytes(1, new byte[] { 1 });
    }

    @Test
    void testUpdateBytesStringByteArray() throws Exception {
        try {
            delegate.updateBytes("foo", new byte[] { 1 });
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateBytes("foo", new byte[] { 1 });
    }

    @Test
    void testUpdateByteStringByte() throws Exception {
        try {
            delegate.updateByte("foo", (byte) 1);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateByte("foo", (byte) 1);
    }

    @Test
    void testUpdateCharacterStreamIntegerReader() throws Exception {
        try {
            delegate.updateCharacterStream(1, (java.io.StringReader) null);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateCharacterStream(1, (java.io.StringReader) null);
    }

    @Test
    void testUpdateCharacterStreamIntegerReaderInteger() throws Exception {
        try {
            delegate.updateCharacterStream(1, (java.io.StringReader) null, 1);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateCharacterStream(1, (java.io.StringReader) null, 1);
    }

    @Test
    void testUpdateCharacterStreamIntegerReaderLong() throws Exception {
        try {
            delegate.updateCharacterStream(1, (java.io.StringReader) null, 1L);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateCharacterStream(1, (java.io.StringReader) null, 1L);
    }

    @Test
    void testUpdateCharacterStreamStringReader() throws Exception {
        try {
            delegate.updateCharacterStream("foo", (java.io.StringReader) null);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateCharacterStream("foo", (java.io.StringReader) null);
    }

    @Test
    void testUpdateCharacterStreamStringReaderInteger() throws Exception {
        try {
            delegate.updateCharacterStream("foo", (java.io.StringReader) null, 1);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateCharacterStream("foo", (java.io.StringReader) null, 1);
    }

    @Test
    void testUpdateCharacterStreamStringReaderLong() throws Exception {
        try {
            delegate.updateCharacterStream("foo", (java.io.StringReader) null, 1L);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateCharacterStream("foo", (java.io.StringReader) null, 1L);
    }

    @Test
    void testUpdateClobIntegerClob() throws Exception {
        try {
            delegate.updateClob(1, (java.sql.Clob) null);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateClob(1, (java.sql.Clob) null);
    }

    @Test
    void testUpdateClobIntegerReader() throws Exception {
        try {
            delegate.updateClob(1, (java.io.StringReader) null);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateClob(1, (java.io.StringReader) null);
    }

    @Test
    void testUpdateClobIntegerReaderLong() throws Exception {
        try {
            delegate.updateClob(1, (java.io.StringReader) null, 1L);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateClob(1, (java.io.StringReader) null, 1L);
    }

    @Test
    void testUpdateClobStringClob() throws Exception {
        try {
            delegate.updateClob("foo", (java.sql.Clob) null);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateClob("foo", (java.sql.Clob) null);
    }

    @Test
    void testUpdateClobStringReader() throws Exception {
        try {
            delegate.updateClob("foo", (java.io.StringReader) null);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateClob("foo", (java.io.StringReader) null);
    }

    @Test
    void testUpdateClobStringReaderLong() throws Exception {
        try {
            delegate.updateClob("foo", (java.io.StringReader) null, 1L);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateClob("foo", (java.io.StringReader) null, 1L);
    }

    @Test
    void testUpdateDateIntegerSqlDate() throws Exception {
        try {
            delegate.updateDate(1, new java.sql.Date(1529827548745L));
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateDate(1, new java.sql.Date(1529827548745L));
    }

    @Test
    void testUpdateDateStringSqlDate() throws Exception {
        try {
            delegate.updateDate("foo", new java.sql.Date(1529827548745L));
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateDate("foo", new java.sql.Date(1529827548745L));
    }

    @Test
    void testUpdateDoubleIntegerDouble() throws Exception {
        try {
            delegate.updateDouble(1, 1.0d);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateDouble(1, 1.0d);
    }

    @Test
    void testUpdateDoubleStringDouble() throws Exception {
        try {
            delegate.updateDouble("foo", 1.0d);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateDouble("foo", 1.0d);
    }

    @Test
    void testUpdateFloatIntegerFloat() throws Exception {
        try {
            delegate.updateFloat(1, 1.0f);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateFloat(1, 1.0f);
    }

    @Test
    void testUpdateFloatStringFloat() throws Exception {
        try {
            delegate.updateFloat("foo", 1.0f);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateFloat("foo", 1.0f);
    }

    @Test
    void testUpdateIntIntegerInteger() throws Exception {
        try {
            delegate.updateInt(1, 1);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateInt(1, 1);
    }

    @Test
    void testUpdateIntStringInteger() throws Exception {
        try {
            delegate.updateInt("foo", 1);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateInt("foo", 1);
    }

    @Test
    void testUpdateLongIntegerLong() throws Exception {
        try {
            delegate.updateLong(1, 1L);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateLong(1, 1L);
    }

    @Test
    void testUpdateLongStringLong() throws Exception {
        try {
            delegate.updateLong("foo", 1L);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateLong("foo", 1L);
    }

    @Test
    void testUpdateNCharacterStreamIntegerReader() throws Exception {
        try {
            delegate.updateNCharacterStream(1, (java.io.StringReader) null);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateNCharacterStream(1, (java.io.StringReader) null);
    }

    @Test
    void testUpdateNCharacterStreamIntegerReaderLong() throws Exception {
        try {
            delegate.updateNCharacterStream(1, (java.io.StringReader) null, 1L);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateNCharacterStream(1, (java.io.StringReader) null, 1L);
    }

    @Test
    void testUpdateNCharacterStreamStringReader() throws Exception {
        try {
            delegate.updateNCharacterStream("foo", (java.io.StringReader) null);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateNCharacterStream("foo", (java.io.StringReader) null);
    }

    @Test
    void testUpdateNCharacterStreamStringReaderLong() throws Exception {
        try {
            delegate.updateNCharacterStream("foo", (java.io.StringReader) null, 1L);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateNCharacterStream("foo", (java.io.StringReader) null, 1L);
    }

    @Test
    void testUpdateNClobIntegerNClob() throws Exception {
        try {
            delegate.updateNClob(1, (java.sql.NClob) null);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateNClob(1, (java.sql.NClob) null);
    }

    @Test
    void testUpdateNClobIntegerReader() throws Exception {
        try {
            delegate.updateNClob(1, (java.io.StringReader) null);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateNClob(1, (java.io.StringReader) null);
    }

    @Test
    void testUpdateNClobIntegerReaderLong() throws Exception {
        try {
            delegate.updateNClob(1, (java.io.StringReader) null, 1L);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateNClob(1, (java.io.StringReader) null, 1L);
    }

    @Test
    void testUpdateNClobStringNClob() throws Exception {
        try {
            delegate.updateNClob("foo", (java.sql.NClob) null);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateNClob("foo", (java.sql.NClob) null);
    }

    @Test
    void testUpdateNClobStringReader() throws Exception {
        try {
            delegate.updateNClob("foo", (java.io.StringReader) null);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateNClob("foo", (java.io.StringReader) null);
    }

    @Test
    void testUpdateNClobStringReaderLong() throws Exception {
        try {
            delegate.updateNClob("foo", (java.io.StringReader) null, 1L);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateNClob("foo", (java.io.StringReader) null, 1L);
    }

    @Test
    void testUpdateNStringIntegerString() throws Exception {
        try {
            delegate.updateNString(1, "foo");
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateNString(1, "foo");
    }

    @Test
    void testUpdateNStringStringString() throws Exception {
        try {
            delegate.updateNString("foo", "foo");
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateNString("foo", "foo");
    }

    @Test
    void testUpdateNullInteger() throws Exception {
        try {
            delegate.updateNull(1);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateNull(1);
    }

    @Test
    void testUpdateNullString() throws Exception {
        try {
            delegate.updateNull("foo");
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateNull("foo");
    }

    @Test
    void testUpdateObjectIntegerObject() throws Exception {
        try {
            delegate.updateObject(1, System.err);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateObject(1, System.err);
    }

    // FIXME: this appears to be a bug
    @Disabled
    @Test
    void testUpdateObjectIntegerObjectInteger() throws Exception {
        try {
            delegate.updateObject(1, System.err, 1);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateObject(1, System.err, 1);
    }

    @Test
    void testUpdateObjectIntegerObjectSQLType() throws Exception {
        try {
            delegate.updateObject(1, System.err, (java.sql.SQLType) null);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateObject(1, System.err, (java.sql.SQLType) null);
    }

    @Test
    void testUpdateObjectIntegerObjectSQLTypeInteger() throws Exception {
        try {
            delegate.updateObject(1, System.err, (java.sql.SQLType) null, 1);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateObject(1, System.err, (java.sql.SQLType) null, 1);
    }

    @Test
    void testUpdateObjectStringObject() throws Exception {
        try {
            delegate.updateObject("foo", System.err);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateObject("foo", System.err);
    }

    // FIXME this appears to be a bug
    @Disabled
    @Test
    void testUpdateObjectStringObjectInteger() throws Exception {
        try {
            delegate.updateObject("foo", System.err, 1);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateObject("foo", System.err, 1);
    }

    @Test
    void testUpdateObjectStringObjectSQLType() throws Exception {
        try {
            delegate.updateObject("foo", System.err, (java.sql.SQLType) null);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateObject("foo", System.err, (java.sql.SQLType) null);
    }

    @Test
    void testUpdateObjectStringObjectSQLTypeInteger() throws Exception {
        try {
            delegate.updateObject("foo", System.err, (java.sql.SQLType) null, 1);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateObject("foo", System.err, (java.sql.SQLType) null, 1);
    }

    @Test
    void testUpdateRefIntegerRef() throws Exception {
        try {
            delegate.updateRef(1, (java.sql.Ref) null);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateRef(1, (java.sql.Ref) null);
    }

    @Test
    void testUpdateRefStringRef() throws Exception {
        try {
            delegate.updateRef("foo", (java.sql.Ref) null);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateRef("foo", (java.sql.Ref) null);
    }

    @Test
    void testUpdateRow() throws Exception {
        try {
            delegate.updateRow();
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateRow();
    }

    @Test
    void testUpdateRowIdIntegerRowId() throws Exception {
        try {
            delegate.updateRowId(1, (java.sql.RowId) null);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateRowId(1, (java.sql.RowId) null);
    }

    @Test
    void testUpdateRowIdStringRowId() throws Exception {
        try {
            delegate.updateRowId("foo", (java.sql.RowId) null);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateRowId("foo", (java.sql.RowId) null);
    }

    @Test
    void testUpdateShortIntegerShort() throws Exception {
        try {
            delegate.updateShort(1, (short) 1);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateShort(1, (short) 1);
    }

    @Test
    void testUpdateShortStringShort() throws Exception {
        try {
            delegate.updateShort("foo", (short) 1);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateShort("foo", (short) 1);
    }

    @Test
    void testUpdateSQLXMLIntegerSQLXML() throws Exception {
        try {
            delegate.updateSQLXML(1, (java.sql.SQLXML) null);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateSQLXML(1, (java.sql.SQLXML) null);
    }

    @Test
    void testUpdateSQLXMLStringSQLXML() throws Exception {
        try {
            delegate.updateSQLXML("foo", (java.sql.SQLXML) null);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateSQLXML("foo", (java.sql.SQLXML) null);
    }

    @Test
    void testUpdateStringIntegerString() throws Exception {
        try {
            delegate.updateString(1, "foo");
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateString(1, "foo");
    }

    @Test
    void testUpdateStringStringString() throws Exception {
        try {
            delegate.updateString("foo", "foo");
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateString("foo", "foo");
    }

    @Test
    void testUpdateTimeIntegerTime() throws Exception {
        try {
            delegate.updateTime(1, (java.sql.Time) null);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateTime(1, (java.sql.Time) null);
    }

    @Test
    void testUpdateTimestampIntegerTimestamp() throws Exception {
        try {
            delegate.updateTimestamp(1, (java.sql.Timestamp) null);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateTimestamp(1, (java.sql.Timestamp) null);
    }

    @Test
    void testUpdateTimestampStringTimestamp() throws Exception {
        try {
            delegate.updateTimestamp("foo", (java.sql.Timestamp) null);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateTimestamp("foo", (java.sql.Timestamp) null);
    }

    @Test
    void testUpdateTimeStringTime() throws Exception {
        try {
            delegate.updateTime("foo", (java.sql.Time) null);
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).updateTime("foo", (java.sql.Time) null);
    }

    @Test
    void testWasNull() throws Exception {
        try {
            delegate.wasNull();
        } catch (final SQLException e) {
        }
        verify(rs, times(1)).wasNull();
    }

    @Test
    void testWrap() throws SQLException {
        final DelegatingResultSet delegate = (DelegatingResultSet) DelegatingResultSet.wrapResultSet(conn, rs);
        assertEquals(delegate, delegate.unwrap(ResultSet.class));
        assertEquals(delegate, delegate.unwrap(DelegatingResultSet.class));
        assertEquals(rs, delegate.unwrap(rs.getClass()));
        assertNull(delegate.unwrap(String.class));
        assertTrue(delegate.isWrapperFor(ResultSet.class));
        assertTrue(delegate.isWrapperFor(DelegatingResultSet.class));
        assertTrue(delegate.isWrapperFor(rs.getClass()));
        assertFalse(delegate.isWrapperFor(String.class));
    }
}
