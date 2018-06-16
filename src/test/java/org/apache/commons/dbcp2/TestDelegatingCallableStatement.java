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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.NClob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Calendar;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

/**
 */
public class TestDelegatingCallableStatement {

    private DelegatingConnection<Connection> conn = null;
    private Connection delegateConn = null;
    private DelegatingCallableStatement stmt = null;
    private CallableStatement delegateStmt = null;

    @Before
    public void setUp() throws Exception {
        delegateConn = new TesterConnection("test", "test");
        conn = new DelegatingConnection<>(delegateConn);
    }

    @Test
    public void testSetters() throws Exception {
        delegateStmt = new TesterCallableStatement(delegateConn,"select * from foo");
        stmt = new DelegatingCallableStatement(conn,delegateStmt);
        stmt.setTime(1, null);
        stmt.setTime("", null);
        try {
            stmt.setURL(1, null);
            fail("Should have thrown SQLException");
        } catch (SQLException e) {}
        stmt.setURL("", null);
        stmt.setNull(1, Types.ARRAY);
        stmt.setNull("", Types.ARRAY);
        stmt.setNull(1, Types.ARRAY, "test");
        stmt.setNull("", Types.ARRAY, "test");
        stmt.setBigDecimal(1, null);
        stmt.setBigDecimal("", null);
        stmt.setString(1, null);
        stmt.setString("", null);
        stmt.setBytes(1, null);
        stmt.setBytes("", null);
        stmt.setDate(1, null);
        stmt.setDate("", null);
        stmt.setDate("", null, Calendar.getInstance());
        try {
            stmt.setAsciiStream(1, null);
            fail("Should have thrown SQLException");
        } catch (SQLException e) {}
        stmt.setAsciiStream("", null);
        try {
            stmt.setBinaryStream(1, null);
            fail("Should have thrown SQLException");
        } catch (SQLException e) {}
        stmt.setBinaryStream("", null);
        stmt.setObject(1, null);
        stmt.setObject("", null);
        stmt.setObject("", null, 1);
        try {
            stmt.setCharacterStream(1, null);
            fail("Should have thrown SQLException");
        } catch (SQLException e) {}
        stmt.setCharacterStream("", null);
        try {
            stmt.setRowId(1, null);
            fail("Should have thrown SQLException");
        } catch (SQLException e) {}
        stmt.setRowId("", null);
        try {
            stmt.setNString(1, null);
            fail("Should have thrown SQLException");
        } catch (SQLException e) {}
        stmt.setNString("", null);
        try {
            stmt.setNCharacterStream(1, null);
            fail("Should have thrown SQLException");
        } catch (SQLException e) {}
        stmt.setNCharacterStream("", null);
        try {
            stmt.setNClob(1, (NClob) null);
            fail("Should have thrown SQLException");
        } catch (SQLException e) {}
        stmt.setNClob("", (NClob) null);
        stmt.setClob(1, (Clob) null);
        stmt.setClob("", (Clob) null);
        stmt.setBlob(1, (Blob) null);
        stmt.setBlob("", (Blob) null);
        try {
            stmt.setSQLXML(1, null);
            fail("Should have thrown SQLException");
        } catch (SQLException e) {}
        stmt.setSQLXML("", null);
        stmt.setBoolean(1, false);
        stmt.setBoolean("", false);
        stmt.setByte(1, (byte) 1);
        stmt.setByte("", (byte) 1);
        stmt.setShort(1, (short) 1);
        stmt.setShort("", (short) 1);
        stmt.setInt(1, 1);
        stmt.setInt("", 1);
        stmt.setLong(1, 1l);
        stmt.setLong("", 1l);
        stmt.setFloat(1, 1.0f);
        stmt.setFloat("", 1.0f);
        stmt.setDouble(1, 1.0d);
        stmt.setDouble("", 1.0d);
        stmt.setTimestamp(1, null);
        stmt.setTimestamp("", null);
        stmt.setRef(1, null);
        stmt.setArray(1, null);
        stmt.setMaxFieldSize(40);
        assertEquals(40, stmt.getMaxFieldSize());
        stmt.setMaxRows(30);
        assertEquals(30, stmt.getMaxRows());
        stmt.setEscapeProcessing(false);
        stmt.setQueryTimeout(20);
        assertEquals(20, stmt.getQueryTimeout());
        stmt.setCursorName("mycursor");
        stmt.setFetchDirection(ResultSet.FETCH_FORWARD);
        assertEquals(ResultSet.FETCH_FORWARD, stmt.getFetchDirection());
        stmt.setFetchSize(10);
        assertEquals(10, stmt.getFetchSize());
        try {
            stmt.setPoolable(false);
            fail("Should have thrown SQLException");
        } catch (SQLException e) {}
    }

    @Test
    public void testExecuteQueryReturnsNull() throws Exception {
        delegateStmt = new TesterCallableStatement(delegateConn,"null");
        stmt = new DelegatingCallableStatement(conn,delegateStmt);
        assertNull(stmt.executeQuery());
    }

    @Test
    public void testExecuteQueryReturnsNotNull() throws Exception {
        delegateStmt = new TesterCallableStatement(delegateConn,"select * from foo");
        stmt = new DelegatingCallableStatement(conn,delegateStmt);
        assertTrue(null != stmt.executeQuery());
    }

    @Test
    public void testGetDelegate() throws Exception {
        delegateStmt = new TesterCallableStatement(delegateConn,"select * from foo");
        stmt = new DelegatingCallableStatement(conn,delegateStmt);
        assertEquals(delegateStmt,stmt.getDelegate());
    }

    @Test
    public void testWasNul() throws Exception {
        delegateStmt = new TesterCallableStatement(delegateConn,"select * from foo");
        stmt = new DelegatingCallableStatement(conn,delegateStmt);
        assertFalse(stmt.wasNull());
    }

    @Test
    public void testGetString() throws Exception {
        delegateStmt = new TesterCallableStatement(delegateConn,"select * from foo");
        stmt = new DelegatingCallableStatement(conn,delegateStmt);
        assertEquals(delegateStmt.getString(1), stmt.getString(1));
        assertEquals(delegateStmt.getString(""), stmt.getString(""));
    }

    @Test
    public void testGetTime() throws Exception {
        delegateStmt = new TesterCallableStatement(delegateConn,"select * from foo");
        stmt = new DelegatingCallableStatement(conn,delegateStmt);
        assertEquals(delegateStmt.getTime(1), stmt.getTime(1));
        assertEquals(delegateStmt.getTime(""), stmt.getTime(""));
        assertEquals(delegateStmt.getTime(1, Calendar.getInstance()), stmt.getTime(1, Calendar.getInstance()));
        assertEquals(delegateStmt.getTime("", Calendar.getInstance()), stmt.getTime("", Calendar.getInstance()));
    }

    @Test
    public void testGetDate() throws Exception {
        delegateStmt = new TesterCallableStatement(delegateConn,"select * from foo");
        stmt = new DelegatingCallableStatement(conn,delegateStmt);
        assertEquals(delegateStmt.getDate(1), stmt.getDate(1));
        assertEquals(delegateStmt.getDate(""), stmt.getDate(""));
        assertEquals(delegateStmt.getDate(1, Calendar.getInstance()), stmt.getDate(1, Calendar.getInstance()));
        assertEquals(delegateStmt.getDate("", Calendar.getInstance()), stmt.getDate("", Calendar.getInstance()));
    }

    @Test
    public void testGetBigDecimal() throws Exception {
        delegateStmt = new TesterCallableStatement(delegateConn,"select * from foo");
        stmt = new DelegatingCallableStatement(conn,delegateStmt);
        assertEquals(delegateStmt.getBigDecimal(1), stmt.getBigDecimal(1));
        assertEquals(delegateStmt.getBigDecimal(""), stmt.getBigDecimal(""));
        assertEquals(delegateStmt.getBigDecimal(1, 10), stmt.getBigDecimal(1, 10));
    }

    @Test
    public void testGetTimestamp() throws Exception {
        delegateStmt = new TesterCallableStatement(delegateConn,"select * from foo");
        stmt = new DelegatingCallableStatement(conn,delegateStmt);
        assertEquals(delegateStmt.getTimestamp(1), stmt.getTimestamp(1));
        assertEquals(delegateStmt.getTimestamp(""), stmt.getTimestamp(""));
        assertEquals(delegateStmt.getTimestamp(1, Calendar.getInstance()), stmt.getTimestamp(1, Calendar.getInstance()));
        assertEquals(delegateStmt.getTimestamp("", Calendar.getInstance()), stmt.getTimestamp("", Calendar.getInstance()));
    }

    @Test
    public void testGetBlob() throws Exception {
        delegateStmt = new TesterCallableStatement(delegateConn,"select * from foo");
        stmt = new DelegatingCallableStatement(conn,delegateStmt);
        assertEquals(delegateStmt.getBlob(1), stmt.getBlob(1));
        assertEquals(delegateStmt.getBlob(""), stmt.getBlob(""));
    }

    @Test
    public void testGetClob() throws Exception {
        delegateStmt = new TesterCallableStatement(delegateConn,"select * from foo");
        stmt = new DelegatingCallableStatement(conn,delegateStmt);
        assertEquals(delegateStmt.getClob(1), stmt.getClob(1));
        assertEquals(delegateStmt.getClob(""), stmt.getClob(""));
    }

    @Test
    public void testGetRowId() throws Exception {
        delegateStmt = new TesterCallableStatement(delegateConn,"select * from foo");
        stmt = new DelegatingCallableStatement(conn,delegateStmt);
        assertEquals(delegateStmt.getRowId(1), stmt.getRowId(1));
        assertEquals(delegateStmt.getRowId(""), stmt.getRowId(""));
    }

    @Test
    public void testGetNClob() throws Exception {
        delegateStmt = new TesterCallableStatement(delegateConn,"select * from foo");
        stmt = new DelegatingCallableStatement(conn,delegateStmt);
        assertEquals(delegateStmt.getNClob(1), stmt.getNClob(1));
        assertEquals(delegateStmt.getNClob(""), stmt.getNClob(""));
    }

    @Test
    public void testGetSQLXML() throws Exception {
        delegateStmt = new TesterCallableStatement(delegateConn,"select * from foo");
        stmt = new DelegatingCallableStatement(conn,delegateStmt);
        assertEquals(delegateStmt.getSQLXML(1), stmt.getSQLXML(1));
        assertEquals(delegateStmt.getSQLXML(""), stmt.getSQLXML(""));
    }

    @Test
    public void testGetNString() throws Exception {
        delegateStmt = new TesterCallableStatement(delegateConn,"select * from foo");
        stmt = new DelegatingCallableStatement(conn,delegateStmt);
        assertEquals(delegateStmt.getNString(1), stmt.getNString(1));
        assertEquals(delegateStmt.getNString(""), stmt.getNString(""));
    }

    @Test
    public void testGetNCharacterStream() throws Exception {
        delegateStmt = new TesterCallableStatement(delegateConn,"select * from foo");
        stmt = new DelegatingCallableStatement(conn,delegateStmt);
        assertEquals(delegateStmt.getNCharacterStream(1), stmt.getNCharacterStream(1));
        assertEquals(delegateStmt.getNCharacterStream(""), stmt.getNCharacterStream(""));
    }

    @Test
    public void testGetCharacterStream() throws Exception {
        delegateStmt = new TesterCallableStatement(delegateConn,"select * from foo");
        stmt = new DelegatingCallableStatement(conn,delegateStmt);
        assertEquals(delegateStmt.getCharacterStream(1), stmt.getCharacterStream(1));
        assertEquals(delegateStmt.getCharacterStream(""), stmt.getCharacterStream(""));
    }

    @SuppressWarnings({ "unchecked", "unused" })
    @Test
    public void testGetObject() throws Exception {
        delegateStmt = new TesterCallableStatement(delegateConn,"select * from foo");
        stmt = new DelegatingCallableStatement(conn,delegateStmt);
        assertEquals(delegateStmt.getObject(1), stmt.getObject(1));
        assertEquals(delegateStmt.getObject(""), stmt.getObject(""));
        assertEquals(delegateStmt.getObject(1, String.class), stmt.getObject(1, String.class));
        assertEquals(delegateStmt.getObject("", String.class), stmt.getObject("", String.class));
        assertEquals(delegateStmt.getObject(1, Collections.<String, Class<?>>emptyMap()), stmt.getObject(1, Collections.<String, Class<?>>emptyMap()));
        assertEquals(delegateStmt.getObject("", Collections.<String, Class<?>>emptyMap()), stmt.getObject("", Collections.<String, Class<?>>emptyMap()));
    }

    @Test
    public void testGetBoolean() throws Exception {
        delegateStmt = new TesterCallableStatement(delegateConn,"select * from foo");
        stmt = new DelegatingCallableStatement(conn,delegateStmt);
        assertEquals(delegateStmt.getBoolean(1), stmt.getBoolean(1));
        assertEquals(delegateStmt.getBoolean(""), stmt.getBoolean(""));
    }

    @Test
    public void testGetByte() throws Exception {
        delegateStmt = new TesterCallableStatement(delegateConn,"select * from foo");
        stmt = new DelegatingCallableStatement(conn,delegateStmt);
        assertEquals(delegateStmt.getByte(1), stmt.getByte(1));
        assertEquals(delegateStmt.getByte(""), stmt.getByte(""));
    }

    @Test
    public void testGetShort() throws Exception {
        delegateStmt = new TesterCallableStatement(delegateConn,"select * from foo");
        stmt = new DelegatingCallableStatement(conn,delegateStmt);
        assertEquals(delegateStmt.getShort(1), stmt.getShort(1));
        assertEquals(delegateStmt.getShort(""), stmt.getShort(""));
    }

    @Test
    public void testGetInt() throws Exception {
        delegateStmt = new TesterCallableStatement(delegateConn,"select * from foo");
        stmt = new DelegatingCallableStatement(conn,delegateStmt);
        assertEquals(delegateStmt.getInt(1), stmt.getInt(1));
        assertEquals(delegateStmt.getInt(""), stmt.getInt(""));
    }

    @Test
    public void testGetLong() throws Exception {
        delegateStmt = new TesterCallableStatement(delegateConn,"select * from foo");
        stmt = new DelegatingCallableStatement(conn,delegateStmt);
        assertEquals(delegateStmt.getLong(1), stmt.getLong(1));
        assertEquals(delegateStmt.getLong(""), stmt.getLong(""));
    }

    @Test
    public void testGetFloat() throws Exception {
        delegateStmt = new TesterCallableStatement(delegateConn,"select * from foo");
        stmt = new DelegatingCallableStatement(conn,delegateStmt);
        assertEquals(delegateStmt.getFloat(1), stmt.getFloat(1), 0.00001f);
        assertEquals(delegateStmt.getFloat(""), stmt.getFloat(""), 0.00001f);
    }

    @Test
    public void testGetDouble() throws Exception {
        delegateStmt = new TesterCallableStatement(delegateConn,"select * from foo");
        stmt = new DelegatingCallableStatement(conn,delegateStmt);
        assertEquals(delegateStmt.getDouble(1), stmt.getDouble(1), 0.00001d);
        assertEquals(delegateStmt.getDouble(""), stmt.getDouble(""), 0.00001d);
    }

    @Test
    public void testGetBytes() throws Exception {
        delegateStmt = new TesterCallableStatement(delegateConn,"select * from foo");
        stmt = new DelegatingCallableStatement(conn,delegateStmt);
        assertArrayEquals(delegateStmt.getBytes(1), stmt.getBytes(1));
        assertArrayEquals(delegateStmt.getBytes(""), stmt.getBytes(""));
    }

    @Test
    public void testGetArray() throws Exception {
        delegateStmt = new TesterCallableStatement(delegateConn,"select * from foo");
        stmt = new DelegatingCallableStatement(conn,delegateStmt);
        assertEquals(delegateStmt.getArray(1), stmt.getArray(1));
        assertEquals(delegateStmt.getArray(""), stmt.getArray(""));
    }

    @Test
    public void testGetURL() throws Exception {
        delegateStmt = new TesterCallableStatement(delegateConn,"select * from foo");
        stmt = new DelegatingCallableStatement(conn,delegateStmt);
        assertEquals(delegateStmt.getURL(1), stmt.getURL(1));
        assertEquals(delegateStmt.getURL(""), stmt.getURL(""));
    }

    @Test
    public void testGetRef() throws Exception {
        delegateStmt = new TesterCallableStatement(delegateConn,"select * from foo");
        stmt = new DelegatingCallableStatement(conn,delegateStmt);
        assertEquals(delegateStmt.getRef(1), stmt.getRef(1));
        assertEquals(delegateStmt.getRef(""), stmt.getRef(""));
    }

    @Test(expected=SQLException.class)
    public void testGetMoreResults() throws Exception {
        delegateStmt = new TesterCallableStatement(delegateConn,"select * from foo");
        stmt = new DelegatingCallableStatement(conn,delegateStmt);
        assertEquals(delegateStmt.getMoreResults(1), stmt.getMoreResults(1));
    }

}