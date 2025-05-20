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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings({ "deprecation", "rawtypes" }) // BigDecimal methods, and casting for mocks
public class TestDelegatingPreparedStatement {

    private TesterConnection testerConn;
    private DelegatingConnection connection;
    private PreparedStatement obj;
    private DelegatingPreparedStatement delegate;

    @BeforeEach
    public void setUp() throws Exception {
        testerConn = new TesterConnection("test", "test");
        connection = new DelegatingConnection<>(testerConn);
        obj = mock(PreparedStatement.class);
        delegate = new DelegatingPreparedStatement(connection, obj);
    }

    @Test
    public void testAddBatch() throws Exception {
        try {
            delegate.addBatch();
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).addBatch();
    }

    @Test
    public void testClearParameters() throws Exception {
        try {
            delegate.clearParameters();
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).clearParameters();
    }

    @Test
    public void testExecute() throws Exception {
        try {
            delegate.execute();
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).execute();
    }

    @Test
    public void testExecuteLargeUpdate() throws Exception {
        try {
            delegate.executeLargeUpdate();
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).executeLargeUpdate();
    }

    @Test
    public void testExecuteQuery() throws Exception {
        try {
            delegate.executeQuery();
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).executeQuery();
    }

    @Test
    public void testExecuteQueryReturnsNotNull() throws Exception {
        obj = new TesterPreparedStatement(testerConn, "select * from foo");
        delegate = new DelegatingPreparedStatement(connection, obj);
        assertNotNull(delegate.executeQuery());
    }

    @Test
    public void testExecuteQueryReturnsNull() throws Exception {
        obj = new TesterPreparedStatement(testerConn, "null");
        delegate = new DelegatingPreparedStatement(connection, obj);
        assertNull(delegate.executeQuery());
    }

    @Test
    public void testExecuteUpdate() throws Exception {
        try {
            delegate.executeUpdate();
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).executeUpdate();
    }

    @Test
    public void testGetDelegate() throws Exception {
        obj = new TesterPreparedStatement(testerConn, "select * from foo");
        delegate = new DelegatingPreparedStatement(connection, obj);
        assertEquals(obj, delegate.getDelegate());
    }

    @Test
    public void testGetMetaData() throws Exception {
        try {
            delegate.getMetaData();
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getMetaData();
    }

    @Test
    public void testGetParameterMetaData() throws Exception {
        try {
            delegate.getParameterMetaData();
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getParameterMetaData();
    }

    @Test
    public void testSetArrayIntegerArray() throws Exception {
        try {
            delegate.setArray(1, (java.sql.Array) null);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setArray(1, (java.sql.Array) null);
    }

    @Test
    public void testSetAsciiStreamIntegerInputStream() throws Exception {
        try {
            delegate.setAsciiStream(1, (java.io.InputStream) null);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setAsciiStream(1, (java.io.InputStream) null);
    }

    @Test
    public void testSetAsciiStreamIntegerInputStreamInteger() throws Exception {
        try {
            delegate.setAsciiStream(1, (java.io.InputStream) null, 1);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setAsciiStream(1, (java.io.InputStream) null, 1);
    }

    @Test
    public void testSetAsciiStreamIntegerInputStreamLong() throws Exception {
        try {
            delegate.setAsciiStream(1, (java.io.InputStream) null, 1L);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setAsciiStream(1, (java.io.InputStream) null, 1L);
    }

    @Test
    public void testSetBigDecimalIntegerBigDecimal() throws Exception {
        try {
            delegate.setBigDecimal(1, java.math.BigDecimal.valueOf(1.0d));
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setBigDecimal(1, java.math.BigDecimal.valueOf(1.0d));
    }

    @Test
    public void testSetBinaryStreamIntegerInputStream() throws Exception {
        try {
            delegate.setBinaryStream(1, (java.io.InputStream) null);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setBinaryStream(1, (java.io.InputStream) null);
    }

    @Test
    public void testSetBinaryStreamIntegerInputStreamInteger() throws Exception {
        try {
            delegate.setBinaryStream(1, (java.io.InputStream) null, 1);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setBinaryStream(1, (java.io.InputStream) null, 1);
    }

    @Test
    public void testSetBinaryStreamIntegerInputStreamLong() throws Exception {
        try {
            delegate.setBinaryStream(1, (java.io.InputStream) null, 1L);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setBinaryStream(1, (java.io.InputStream) null, 1L);
    }

    @Test
    public void testSetBlobIntegerBlob() throws Exception {
        try {
            delegate.setBlob(1, (java.sql.Blob) null);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setBlob(1, (java.sql.Blob) null);
    }

    @Test
    public void testSetBlobIntegerInputStream() throws Exception {
        try {
            delegate.setBlob(1, (java.io.InputStream) null);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setBlob(1, (java.io.InputStream) null);
    }

    @Test
    public void testSetBlobIntegerInputStreamLong() throws Exception {
        try {
            delegate.setBlob(1, (java.io.InputStream) null, 1L);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setBlob(1, (java.io.InputStream) null, 1L);
    }

    @Test
    public void testSetBooleanIntegerBoolean() throws Exception {
        try {
            delegate.setBoolean(1, Boolean.TRUE);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setBoolean(1, Boolean.TRUE);
    }

    @Test
    public void testSetByteIntegerByte() throws Exception {
        try {
            delegate.setByte(1, (byte) 1);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setByte(1, (byte) 1);
    }

    @Test
    public void testSetBytesIntegerByteArray() throws Exception {
        try {
            delegate.setBytes(1, new byte[] { 1 });
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setBytes(1, new byte[] { 1 });
    }

    @Test
    public void testSetCharacterStreamIntegerReader() throws Exception {
        try {
            delegate.setCharacterStream(1, (java.io.StringReader) null);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setCharacterStream(1, (java.io.StringReader) null);
    }

    @Test
    public void testSetCharacterStreamIntegerReaderInteger() throws Exception {
        try {
            delegate.setCharacterStream(1, (java.io.StringReader) null, 1);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setCharacterStream(1, (java.io.StringReader) null, 1);
    }

    @Test
    public void testSetCharacterStreamIntegerReaderLong() throws Exception {
        try {
            delegate.setCharacterStream(1, (java.io.StringReader) null, 1L);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setCharacterStream(1, (java.io.StringReader) null, 1L);
    }

    @Test
    public void testSetClobIntegerClob() throws Exception {
        try {
            delegate.setClob(1, (java.sql.Clob) null);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setClob(1, (java.sql.Clob) null);
    }

    @Test
    public void testSetClobIntegerReader() throws Exception {
        try {
            delegate.setClob(1, (java.io.StringReader) null);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setClob(1, (java.io.StringReader) null);
    }

    @Test
    public void testSetClobIntegerReaderLong() throws Exception {
        try {
            delegate.setClob(1, (java.io.StringReader) null, 1L);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setClob(1, (java.io.StringReader) null, 1L);
    }

    @Test
    public void testSetDateIntegerSqlDate() throws Exception {
        try {
            delegate.setDate(1, new java.sql.Date(1529827548745L));
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setDate(1, new java.sql.Date(1529827548745L));
    }

    @Test
    public void testSetDateIntegerSqlDateCalendar() throws Exception {
        try {
            delegate.setDate(1, new java.sql.Date(1529827548745L), (java.util.Calendar) null);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setDate(1, new java.sql.Date(1529827548745L), (java.util.Calendar) null);
    }

    @Test
    public void testSetDoubleIntegerDouble() throws Exception {
        try {
            delegate.setDouble(1, 1.0d);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setDouble(1, 1.0d);
    }

    @Test
    public void testSetFloatIntegerFloat() throws Exception {
        try {
            delegate.setFloat(1, 1.0f);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setFloat(1, 1.0f);
    }

    @Test
    public void testSetIntIntegerInteger() throws Exception {
        try {
            delegate.setInt(1, 1);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setInt(1, 1);
    }

    @Test
    public void testSetLongIntegerLong() throws Exception {
        try {
            delegate.setLong(1, 1L);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setLong(1, 1L);
    }

    @Test
    public void testSetNCharacterStreamIntegerReader() throws Exception {
        try {
            delegate.setNCharacterStream(1, (java.io.StringReader) null);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setNCharacterStream(1, (java.io.StringReader) null);
    }

    @Test
    public void testSetNCharacterStreamIntegerReaderLong() throws Exception {
        try {
            delegate.setNCharacterStream(1, (java.io.StringReader) null, 1L);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setNCharacterStream(1, (java.io.StringReader) null, 1L);
    }

    @Test
    public void testSetNClobIntegerNClob() throws Exception {
        try {
            delegate.setNClob(1, (java.sql.NClob) null);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setNClob(1, (java.sql.NClob) null);
    }

    @Test
    public void testSetNClobIntegerReader() throws Exception {
        try {
            delegate.setNClob(1, (java.io.StringReader) null);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setNClob(1, (java.io.StringReader) null);
    }

    @Test
    public void testSetNClobIntegerReaderLong() throws Exception {
        try {
            delegate.setNClob(1, (java.io.StringReader) null, 1L);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setNClob(1, (java.io.StringReader) null, 1L);
    }

    @Test
    public void testSetNStringIntegerString() throws Exception {
        try {
            delegate.setNString(1, "foo");
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setNString(1, "foo");
    }

    @Test
    public void testSetNullIntegerInteger() throws Exception {
        try {
            delegate.setNull(1, 1);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setNull(1, 1);
    }

    @Test
    public void testSetNullIntegerIntegerString() throws Exception {
        try {
            delegate.setNull(1, 1, "foo");
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setNull(1, 1, "foo");
    }

    @Test
    public void testSetObjectIntegerObject() throws Exception {
        try {
            delegate.setObject(1, System.err);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setObject(1, System.err);
    }

    @Test
    public void testSetObjectIntegerObjectInteger() throws Exception {
        try {
            delegate.setObject(1, System.err, 1);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setObject(1, System.err, 1);
    }

    @Test
    public void testSetObjectIntegerObjectIntegerInteger() throws Exception {
        try {
            delegate.setObject(1, System.err, 1, 1);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setObject(1, System.err, 1, 1);
    }

    @Test
    public void testSetObjectIntegerObjectSQLType() throws Exception {
        try {
            delegate.setObject(1, System.err, (java.sql.SQLType) null);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setObject(1, System.err, (java.sql.SQLType) null);
    }

    @Test
    public void testSetObjectIntegerObjectSQLTypeInteger() throws Exception {
        try {
            delegate.setObject(1, System.err, (java.sql.SQLType) null, 1);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setObject(1, System.err, (java.sql.SQLType) null, 1);
    }

    @Test
    public void testSetRefIntegerRef() throws Exception {
        try {
            delegate.setRef(1, (java.sql.Ref) null);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setRef(1, (java.sql.Ref) null);
    }

    @Test
    public void testSetRowIdIntegerRowId() throws Exception {
        try {
            delegate.setRowId(1, (java.sql.RowId) null);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setRowId(1, (java.sql.RowId) null);
    }

    @Test
    public void testSetShortIntegerShort() throws Exception {
        try {
            delegate.setShort(1, (short) 1);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setShort(1, (short) 1);
    }

    @Test
    public void testSetSQLXMLIntegerSQLXML() throws Exception {
        try {
            delegate.setSQLXML(1, (java.sql.SQLXML) null);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setSQLXML(1, (java.sql.SQLXML) null);
    }

    @Test
    public void testSetStringIntegerString() throws Exception {
        try {
            delegate.setString(1, "foo");
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setString(1, "foo");
    }

    @Test
    public void testSetTimeIntegerTime() throws Exception {
        try {
            delegate.setTime(1, (java.sql.Time) null);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setTime(1, (java.sql.Time) null);
    }

    @Test
    public void testSetTimeIntegerTimeCalendar() throws Exception {
        try {
            delegate.setTime(1, (java.sql.Time) null, (java.util.Calendar) null);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setTime(1, (java.sql.Time) null, (java.util.Calendar) null);
    }

    @Test
    public void testSetTimestampIntegerTimestamp() throws Exception {
        try {
            delegate.setTimestamp(1, (java.sql.Timestamp) null);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setTimestamp(1, (java.sql.Timestamp) null);
    }

    @Test
    public void testSetTimestampIntegerTimestampCalendar() throws Exception {
        try {
            delegate.setTimestamp(1, (java.sql.Timestamp) null, (java.util.Calendar) null);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setTimestamp(1, (java.sql.Timestamp) null, (java.util.Calendar) null);
    }

    @Test
    public void testSetUnicodeStreamIntegerInputStreamInteger() throws Exception {
        try {
            delegate.setUnicodeStream(1, (java.io.InputStream) null, 1);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setUnicodeStream(1, (java.io.InputStream) null, 1);
    }

    @Test
    public void testSetURLIntegerUrl() throws Exception {
        try {
            delegate.setURL(1, (java.net.URL) null);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setURL(1, (java.net.URL) null);
    }

}
