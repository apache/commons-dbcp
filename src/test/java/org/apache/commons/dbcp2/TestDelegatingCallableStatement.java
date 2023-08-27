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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings({ "deprecation", "unchecked", "rawtypes" }) // BigDecimal methods, and casting for mocks
public class TestDelegatingCallableStatement {

    private TesterConnection conn;
    private DelegatingCallableStatement delegate;
    private CallableStatement obj;

    @BeforeEach
    public void setUp() throws Exception {
        conn = new TesterConnection("test", "test");
        obj = mock(CallableStatement.class);
        delegate = new DelegatingCallableStatement(new DelegatingConnection<>(conn), obj);
    }

    @Test
    public void testExecuteQueryReturnsNotNull() throws Exception {
        final TesterCallableStatement delegateStmt = new TesterCallableStatement(conn, "select * from foo");
        obj = new DelegatingCallableStatement(new DelegatingConnection<>(conn), delegateStmt);
        try (ResultSet rs = obj.executeQuery()) {
            assertNotNull(rs);
        }
    }

    @Test
    public void testExecuteQueryReturnsNull() throws Exception {
        final TesterCallableStatement delegateStmt = new TesterCallableStatement(conn,"null");
        obj = new DelegatingCallableStatement(new DelegatingConnection<>(conn), delegateStmt);
        try (ResultSet rs = obj.executeQuery()) {
            assertNotNull(rs);
        }
    }

    @Test
    public void testGetArrayInteger() throws Exception {
        try {
            delegate.getArray(1);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getArray(1);
    }

    @Test
    public void testGetArrayString() throws Exception {
        try {
            delegate.getArray("foo");
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getArray("foo");
    }

    @Test
    public void testGetBigDecimalInteger() throws Exception {
        try {
            delegate.getBigDecimal(1);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getBigDecimal(1);
    }

    @Test
    public void testGetBigDecimalIntegerInteger() throws Exception {
        try {
            delegate.getBigDecimal(1, 1);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getBigDecimal(1, 1);
    }

    @Test
    public void testGetBigDecimalString() throws Exception {
        try {
            delegate.getBigDecimal("foo");
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getBigDecimal("foo");
    }

    @Test
    public void testGetBlobInteger() throws Exception {
        try {
            delegate.getBlob(1);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getBlob(1);
    }

    @Test
    public void testGetBlobString() throws Exception {
        try {
            delegate.getBlob("foo");
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getBlob("foo");
    }

    @Test
    public void testGetBooleanInteger() throws Exception {
        try {
            delegate.getBoolean(1);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getBoolean(1);
    }

    @Test
    public void testGetBooleanString() throws Exception {
        try {
            delegate.getBoolean("foo");
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getBoolean("foo");
    }

    @Test
    public void testGetByteInteger() throws Exception {
        try {
            delegate.getByte(1);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getByte(1);
    }

    @Test
    public void testGetBytesInteger() throws Exception {
        try {
            delegate.getBytes(1);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getBytes(1);
    }

    @Test
    public void testGetBytesString() throws Exception {
        try {
            delegate.getBytes("foo");
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getBytes("foo");
    }

    @Test
    public void testGetByteString() throws Exception {
        try {
            delegate.getByte("foo");
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getByte("foo");
    }

    @Test
    public void testGetCharacterStreamInteger() throws Exception {
        try {
            delegate.getCharacterStream(1);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getCharacterStream(1);
    }

    @Test
    public void testGetCharacterStreamString() throws Exception {
        try {
            delegate.getCharacterStream("foo");
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getCharacterStream("foo");
    }

    @Test
    public void testGetClobInteger() throws Exception {
        try {
            delegate.getClob(1);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getClob(1);
    }

    @Test
    public void testGetClobString() throws Exception {
        try {
            delegate.getClob("foo");
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getClob("foo");
    }

    @Test
    public void testGetDateInteger() throws Exception {
        try {
            delegate.getDate(1);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getDate(1);
    }

    @Test
    public void testGetDateIntegerCalendar() throws Exception {
        try {
            delegate.getDate(1, (java.util.Calendar) null);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getDate(1, (java.util.Calendar) null);
    }

    @Test
    public void testGetDateString() throws Exception {
        try {
            delegate.getDate("foo");
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getDate("foo");
    }

    @Test
    public void testGetDateStringCalendar() throws Exception {
        try {
            delegate.getDate("foo", (java.util.Calendar) null);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getDate("foo", (java.util.Calendar) null);
    }

    @Test
    public void testGetDelegate() throws Exception {
        final TesterCallableStatement delegateStmt = new TesterCallableStatement(conn,"select * from foo");
        obj = new DelegatingCallableStatement(new DelegatingConnection<>(conn), delegateStmt);
        assertEquals(delegateStmt,((DelegatingCallableStatement)obj).getDelegate());
    }

    @Test
    public void testGetDoubleInteger() throws Exception {
        try {
            delegate.getDouble(1);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getDouble(1);
    }

    @Test
    public void testGetDoubleString() throws Exception {
        try {
            delegate.getDouble("foo");
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getDouble("foo");
    }

    @Test
    public void testGetFloatInteger() throws Exception {
        try {
            delegate.getFloat(1);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getFloat(1);
    }

    @Test
    public void testGetFloatString() throws Exception {
        try {
            delegate.getFloat("foo");
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getFloat("foo");
    }

    @Test
    public void testGetIntInteger() throws Exception {
        try {
            delegate.getInt(1);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getInt(1);
    }

    @Test
    public void testGetIntString() throws Exception {
        try {
            delegate.getInt("foo");
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getInt("foo");
    }

    @Test
    public void testGetLongInteger() throws Exception {
        try {
            delegate.getLong(1);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getLong(1);
    }

    @Test
    public void testGetLongString() throws Exception {
        try {
            delegate.getLong("foo");
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getLong("foo");
    }

    @Test
    public void testGetNCharacterStreamInteger() throws Exception {
        try {
            delegate.getNCharacterStream(1);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getNCharacterStream(1);
    }

    @Test
    public void testGetNCharacterStreamString() throws Exception {
        try {
            delegate.getNCharacterStream("foo");
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getNCharacterStream("foo");
    }

    @Test
    public void testGetNClobInteger() throws Exception {
        try {
            delegate.getNClob(1);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getNClob(1);
    }

    @Test
    public void testGetNClobString() throws Exception {
        try {
            delegate.getNClob("foo");
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getNClob("foo");
    }

    @Test
    public void testGetNStringInteger() throws Exception {
        try {
            delegate.getNString(1);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getNString(1);
    }

    @Test
    public void testGetNStringString() throws Exception {
        try {
            delegate.getNString("foo");
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getNString("foo");
    }

    @Test
    public void testGetObjectInteger() throws Exception {
        try {
            delegate.getObject(1);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getObject(1);
    }

    @Test
    public void testGetObjectIntegerClass() throws Exception {
        try {
            delegate.getObject(1, Object.class);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getObject(1, Object.class);
    }

    @Test
    public void testGetObjectIntegerMap() throws Exception {
        try {
            delegate.getObject(1, (java.util.Map) null);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getObject(1, (java.util.Map) null);
    }

    @Test
    public void testGetObjectString() throws Exception {
        try {
            delegate.getObject("foo");
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getObject("foo");
    }

    @Test
    public void testGetObjectStringClass() throws Exception {
        try {
            delegate.getObject("foo", Object.class);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getObject("foo", Object.class);
    }

    @Test
    public void testGetObjectStringMap() throws Exception {
        try {
            delegate.getObject("foo", (java.util.Map) null);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getObject("foo", (java.util.Map) null);
    }

    @Test
    public void testGetRefInteger() throws Exception {
        try {
            delegate.getRef(1);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getRef(1);
    }

    @Test
    public void testGetRefString() throws Exception {
        try {
            delegate.getRef("foo");
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getRef("foo");
    }

    @Test
    public void testGetRowIdInteger() throws Exception {
        try {
            delegate.getRowId(1);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getRowId(1);
    }

    @Test
    public void testGetRowIdString() throws Exception {
        try {
            delegate.getRowId("foo");
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getRowId("foo");
    }

    @Test
    public void testGetShortInteger() throws Exception {
        try {
            delegate.getShort(1);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getShort(1);
    }

    @Test
    public void testGetShortString() throws Exception {
        try {
            delegate.getShort("foo");
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getShort("foo");
    }

    @Test
    public void testGetSQLXMLInteger() throws Exception {
        try {
            delegate.getSQLXML(1);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getSQLXML(1);
    }

    @Test
    public void testGetSQLXMLString() throws Exception {
        try {
            delegate.getSQLXML("foo");
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getSQLXML("foo");
    }

    @Test
    public void testGetStringInteger() throws Exception {
        try {
            delegate.getString(1);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getString(1);
    }

    @Test
    public void testGetStringString() throws Exception {
        try {
            delegate.getString("foo");
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getString("foo");
    }

    @Test
    public void testGetTimeInteger() throws Exception {
        try {
            delegate.getTime(1);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getTime(1);
    }

    @Test
    public void testGetTimeIntegerCalendar() throws Exception {
        try {
            delegate.getTime(1, (java.util.Calendar) null);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getTime(1, (java.util.Calendar) null);
    }

    @Test
    public void testGetTimestampInteger() throws Exception {
        try {
            delegate.getTimestamp(1);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getTimestamp(1);
    }

    @Test
    public void testGetTimestampIntegerCalendar() throws Exception {
        try {
            delegate.getTimestamp(1, (java.util.Calendar) null);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getTimestamp(1, (java.util.Calendar) null);
    }

    @Test
    public void testGetTimestampString() throws Exception {
        try {
            delegate.getTimestamp("foo");
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getTimestamp("foo");
    }

    @Test
    public void testGetTimestampStringCalendar() throws Exception {
        try {
            delegate.getTimestamp("foo", (java.util.Calendar) null);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getTimestamp("foo", (java.util.Calendar) null);
    }

    @Test
    public void testGetTimeString() throws Exception {
        try {
            delegate.getTime("foo");
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getTime("foo");
    }

    @Test
    public void testGetTimeStringCalendar() throws Exception {
        try {
            delegate.getTime("foo", (java.util.Calendar) null);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getTime("foo", (java.util.Calendar) null);
    }

    @Test
    public void testGetURLInteger() throws Exception {
        try {
            delegate.getURL(1);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getURL(1);
    }

    @Test
    public void testGetURLString() throws Exception {
        try {
            delegate.getURL("foo");
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getURL("foo");
    }

    @Test
    public void testRegisterOutParameterIntegerInteger() throws Exception {
        try {
            delegate.registerOutParameter(1, 1);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).registerOutParameter(1, 1);
    }

    @Test
    public void testRegisterOutParameterIntegerIntegerInteger() throws Exception {
        try {
            delegate.registerOutParameter(1, 1, 1);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).registerOutParameter(1, 1, 1);
    }

    @Test
    public void testRegisterOutParameterIntegerIntegerString() throws Exception {
        try {
            delegate.registerOutParameter(1, 1, "foo");
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).registerOutParameter(1, 1, "foo");
    }

    @Test
    public void testRegisterOutParameterIntegerSQLType() throws Exception {
        try {
            delegate.registerOutParameter(1, (java.sql.SQLType) null);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).registerOutParameter(1, (java.sql.SQLType) null);
    }

    @Test
    public void testRegisterOutParameterIntegerSQLTypeInteger() throws Exception {
        try {
            delegate.registerOutParameter(1, (java.sql.SQLType) null, 1);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).registerOutParameter(1, (java.sql.SQLType) null, 1);
    }

    @Test
    public void testRegisterOutParameterIntegerSQLTypeString() throws Exception {
        try {
            delegate.registerOutParameter(1, (java.sql.SQLType) null, "foo");
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).registerOutParameter(1, (java.sql.SQLType) null, "foo");
    }

    @Test
    public void testRegisterOutParameterStringInteger() throws Exception {
        try {
            delegate.registerOutParameter("foo", 1);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).registerOutParameter("foo", 1);
    }

    @Test
    public void testRegisterOutParameterStringIntegerInteger() throws Exception {
        try {
            delegate.registerOutParameter("foo", 1, 1);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).registerOutParameter("foo", 1, 1);
    }

    @Test
    public void testRegisterOutParameterStringIntegerString() throws Exception {
        try {
            delegate.registerOutParameter("foo", 1, "foo");
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).registerOutParameter("foo", 1, "foo");
    }

    @Test
    public void testRegisterOutParameterStringSQLType() throws Exception {
        try {
            delegate.registerOutParameter("foo", (java.sql.SQLType) null);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).registerOutParameter("foo", (java.sql.SQLType) null);
    }

    @Test
    public void testRegisterOutParameterStringSQLTypeInteger() throws Exception {
        try {
            delegate.registerOutParameter("foo", (java.sql.SQLType) null, 1);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).registerOutParameter("foo", (java.sql.SQLType) null, 1);
    }

    @Test
    public void testRegisterOutParameterStringSQLTypeString() throws Exception {
        try {
            delegate.registerOutParameter("foo", (java.sql.SQLType) null, "foo");
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).registerOutParameter("foo", (java.sql.SQLType) null, "foo");
    }

    @Test
    public void testSetAsciiStreamStringInputStream() throws Exception {
        try {
            delegate.setAsciiStream("foo", (java.io.InputStream) null);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setAsciiStream("foo", (java.io.InputStream) null);
    }

    @Test
    public void testSetAsciiStreamStringInputStreamInteger() throws Exception {
        try {
            delegate.setAsciiStream("foo", (java.io.InputStream) null, 1);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setAsciiStream("foo", (java.io.InputStream) null, 1);
    }

    @Test
    public void testSetAsciiStreamStringInputStreamLong() throws Exception {
        try {
            delegate.setAsciiStream("foo", (java.io.InputStream) null, 1L);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setAsciiStream("foo", (java.io.InputStream) null, 1L);
    }

    @Test
    public void testSetBigDecimalStringBigDecimal() throws Exception {
        try {
            delegate.setBigDecimal("foo", java.math.BigDecimal.valueOf(1.0d));
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setBigDecimal("foo", java.math.BigDecimal.valueOf(1.0d));
    }

    @Test
    public void testSetBinaryStreamStringInputStream() throws Exception {
        try {
            delegate.setBinaryStream("foo", (java.io.InputStream) null);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setBinaryStream("foo", (java.io.InputStream) null);
    }

    @Test
    public void testSetBinaryStreamStringInputStreamInteger() throws Exception {
        try {
            delegate.setBinaryStream("foo", (java.io.InputStream) null, 1);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setBinaryStream("foo", (java.io.InputStream) null, 1);
    }

    @Test
    public void testSetBinaryStreamStringInputStreamLong() throws Exception {
        try {
            delegate.setBinaryStream("foo", (java.io.InputStream) null, 1L);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setBinaryStream("foo", (java.io.InputStream) null, 1L);
    }

    @Test
    public void testSetBlobStringBlob() throws Exception {
        try {
            delegate.setBlob("foo", (java.sql.Blob) null);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setBlob("foo", (java.sql.Blob) null);
    }

    @Test
    public void testSetBlobStringInputStream() throws Exception {
        try {
            delegate.setBlob("foo", (java.io.InputStream) null);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setBlob("foo", (java.io.InputStream) null);
    }

    @Test
    public void testSetBlobStringInputStreamLong() throws Exception {
        try {
            delegate.setBlob("foo", (java.io.InputStream) null, 1L);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setBlob("foo", (java.io.InputStream) null, 1L);
    }

    @Test
    public void testSetBooleanStringBoolean() throws Exception {
        try {
            delegate.setBoolean("foo", Boolean.TRUE);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setBoolean("foo", Boolean.TRUE);
    }

    @Test
    public void testSetBytesStringByteArray() throws Exception {
        try {
            delegate.setBytes("foo", new byte[] { 1 });
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setBytes("foo", new byte[] { 1 });
    }

    @Test
    public void testSetByteStringByte() throws Exception {
        try {
            delegate.setByte("foo", (byte) 1);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setByte("foo", (byte) 1);
    }

    @Test
    public void testSetCharacterStreamStringReader() throws Exception {
        try {
            delegate.setCharacterStream("foo", (java.io.StringReader) null);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setCharacterStream("foo", (java.io.StringReader) null);
    }

    @Test
    public void testSetCharacterStreamStringReaderInteger() throws Exception {
        try {
            delegate.setCharacterStream("foo", (java.io.StringReader) null, 1);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setCharacterStream("foo", (java.io.StringReader) null, 1);
    }

    @Test
    public void testSetCharacterStreamStringReaderLong() throws Exception {
        try {
            delegate.setCharacterStream("foo", (java.io.StringReader) null, 1L);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setCharacterStream("foo", (java.io.StringReader) null, 1L);
    }

    @Test
    public void testSetClobStringClob() throws Exception {
        try {
            delegate.setClob("foo", (java.sql.Clob) null);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setClob("foo", (java.sql.Clob) null);
    }

    @Test
    public void testSetClobStringReader() throws Exception {
        try {
            delegate.setClob("foo", (java.io.StringReader) null);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setClob("foo", (java.io.StringReader) null);
    }

    @Test
    public void testSetClobStringReaderLong() throws Exception {
        try {
            delegate.setClob("foo", (java.io.StringReader) null, 1L);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setClob("foo", (java.io.StringReader) null, 1L);
    }

    @Test
    public void testSetDateStringSqlDate() throws Exception {
        try {
            delegate.setDate("foo", new java.sql.Date(1529827548745L));
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setDate("foo", new java.sql.Date(1529827548745L));
    }

    @Test
    public void testSetDateStringSqlDateCalendar() throws Exception {
        try {
            delegate.setDate("foo", new java.sql.Date(1529827548745L), (java.util.Calendar) null);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setDate("foo", new java.sql.Date(1529827548745L), (java.util.Calendar) null);
    }

    @Test
    public void testSetDoubleStringDouble() throws Exception {
        try {
            delegate.setDouble("foo", 1.0d);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setDouble("foo", 1.0d);
    }

    @Test
    public void testSetFloatStringFloat() throws Exception {
        try {
            delegate.setFloat("foo", 1.0f);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setFloat("foo", 1.0f);
    }

    @Test
    public void testSetIntStringInteger() throws Exception {
        try {
            delegate.setInt("foo", 1);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setInt("foo", 1);
    }

    @Test
    public void testSetLongStringLong() throws Exception {
        try {
            delegate.setLong("foo", 1L);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setLong("foo", 1L);
    }

    @Test
    public void testSetNCharacterStreamStringReader() throws Exception {
        try {
            delegate.setNCharacterStream("foo", (java.io.StringReader) null);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setNCharacterStream("foo", (java.io.StringReader) null);
    }

    @Test
    public void testSetNCharacterStreamStringReaderLong() throws Exception {
        try {
            delegate.setNCharacterStream("foo", (java.io.StringReader) null, 1L);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setNCharacterStream("foo", (java.io.StringReader) null, 1L);
    }

    @Test
    public void testSetNClobStringNClob() throws Exception {
        try {
            delegate.setNClob("foo", (java.sql.NClob) null);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setNClob("foo", (java.sql.NClob) null);
    }

    @Test
    public void testSetNClobStringReader() throws Exception {
        try {
            delegate.setNClob("foo", (java.io.StringReader) null);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setNClob("foo", (java.io.StringReader) null);
    }

    @Test
    public void testSetNClobStringReaderLong() throws Exception {
        try {
            delegate.setNClob("foo", (java.io.StringReader) null, 1L);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setNClob("foo", (java.io.StringReader) null, 1L);
    }

    @Test
    public void testSetNStringStringString() throws Exception {
        try {
            delegate.setNString("foo", "foo");
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setNString("foo", "foo");
    }

    @Test
    public void testSetNullStringInteger() throws Exception {
        try {
            delegate.setNull("foo", 1);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setNull("foo", 1);
    }

    @Test
    public void testSetNullStringIntegerString() throws Exception {
        try {
            delegate.setNull("foo", 1, "foo");
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setNull("foo", 1, "foo");
    }

    @Test
    public void testSetObjectStringObject() throws Exception {
        try {
            delegate.setObject("foo", System.err);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setObject("foo", System.err);
    }

    @Test
    public void testSetObjectStringObjectInteger() throws Exception {
        try {
            delegate.setObject("foo", System.err, 1);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setObject("foo", System.err, 1);
    }

    @Test
    public void testSetObjectStringObjectIntegerInteger() throws Exception {
        try {
            delegate.setObject("foo", System.err, 1, 1);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setObject("foo", System.err, 1, 1);
    }

    @Test
    public void testSetObjectStringObjectSQLType() throws Exception {
        try {
            delegate.setObject("foo", System.err, (java.sql.SQLType) null);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setObject("foo", System.err, (java.sql.SQLType) null);
    }

    @Test
    public void testSetObjectStringObjectSQLTypeInteger() throws Exception {
        try {
            delegate.setObject("foo", System.err, (java.sql.SQLType) null, 1);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setObject("foo", System.err, (java.sql.SQLType) null, 1);
    }

    @Test
    public void testSetRowIdStringRowId() throws Exception {
        try {
            delegate.setRowId("foo", (java.sql.RowId) null);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setRowId("foo", (java.sql.RowId) null);
    }

    @Test
    public void testSetShortStringShort() throws Exception {
        try {
            delegate.setShort("foo", (short) 1);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setShort("foo", (short) 1);
    }

    @Test
    public void testSetSQLXMLStringSQLXML() throws Exception {
        try {
            delegate.setSQLXML("foo", (java.sql.SQLXML) null);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setSQLXML("foo", (java.sql.SQLXML) null);
    }

    @Test
    public void testSetStringStringString() throws Exception {
        try {
            delegate.setString("foo", "foo");
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setString("foo", "foo");
    }

    @Test
    public void testSetTimestampStringTimestamp() throws Exception {
        try {
            delegate.setTimestamp("foo", (java.sql.Timestamp) null);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setTimestamp("foo", (java.sql.Timestamp) null);
    }

    @Test
    public void testSetTimestampStringTimestampCalendar() throws Exception {
        try {
            delegate.setTimestamp("foo", (java.sql.Timestamp) null, (java.util.Calendar) null);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setTimestamp("foo", (java.sql.Timestamp) null, (java.util.Calendar) null);
    }

    @Test
    public void testSetTimeStringTime() throws Exception {
        try {
            delegate.setTime("foo", (java.sql.Time) null);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setTime("foo", (java.sql.Time) null);
    }

    @Test
    public void testSetTimeStringTimeCalendar() throws Exception {
        try {
            delegate.setTime("foo", (java.sql.Time) null, (java.util.Calendar) null);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setTime("foo", (java.sql.Time) null, (java.util.Calendar) null);
    }

    @Test
    public void testSetURLStringUrl() throws Exception {
        try {
            delegate.setURL("foo", (java.net.URL) null);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setURL("foo", (java.net.URL) null);
    }

    @Test
    public void testWasNull() throws Exception {
        try {
            delegate.wasNull();
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).wasNull();
    }

}