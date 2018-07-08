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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.Before;
import org.junit.Test;

public class TestDelegatingStatement {

    private DelegatingConnection<Connection> conn = null;
    private Connection delegateConn = null;
    private Statement obj = null;
    private DelegatingStatement delegate = null;

    @Before
    public void setUp() throws Exception {
        delegateConn = new TesterConnection("test", "test");
        conn = new DelegatingConnection<>(delegateConn);
        obj = mock(Statement.class);
        delegate = new DelegatingStatement(conn, obj);
    }

    @Test
    public void testExecuteQueryReturnsNull() throws Exception {
        assertNull(delegate.executeQuery("null"));
    }

    @Test
    public void testGetDelegate() throws Exception {
        assertEquals(obj,delegate.getDelegate());
    }

    @Test
    public void testCheckOpen() throws Exception {
        delegate.checkOpen();
        delegate.close();
        try {
            delegate.checkOpen();
            fail("Expecting SQLException");
        } catch (final SQLException ex) {
            // expected
        }
    }

    @Test
    public void testIsWrapperFor() throws Exception {
        final TesterConnection tstConn = new TesterConnection("test", "test");
        final TesterStatement tstStmt = new TesterStatementNonWrapping(tstConn);
        final DelegatingConnection<TesterConnection> dconn = new DelegatingConnection<>(tstConn);
        final DelegatingStatement stamt = new DelegatingStatement(dconn, tstStmt);

        final Class<?> stmtProxyClass = Proxy.getProxyClass(
                this.getClass().getClassLoader(),
                Statement.class);

        assertTrue(stamt.isWrapperFor(DelegatingStatement.class));
        assertTrue(stamt.isWrapperFor(TesterStatement.class));
        assertFalse(stamt.isWrapperFor(stmtProxyClass));

        stamt.close();
    }

    private static class TesterStatementNonWrapping extends TesterStatement {

        public TesterStatementNonWrapping(final Connection conn) {
            super(conn);
        }

        @Override
        public boolean isWrapperFor(final Class<?> iface) throws SQLException {
            return false;
        }
    }

    @Test
    public void testAddBatchString() throws Exception {
        try {
            delegate.addBatch("foo");
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).addBatch("foo");
    }

    @Test
    public void testCancel() throws Exception {
        try {
            delegate.cancel();
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).cancel();
    }

    @Test
    public void testClearBatch() throws Exception {
        try {
            delegate.clearBatch();
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).clearBatch();
    }

    @Test
    public void testClearWarnings() throws Exception {
        try {
            delegate.clearWarnings();
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).clearWarnings();
    }

    @Test
    public void testClose() throws Exception {
        try {
            delegate.close();
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).close();
    }

    @Test
    public void testCloseOnCompletion() throws Exception {
        try {
            delegate.closeOnCompletion();
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).closeOnCompletion();
    }

    @Test
    public void testExecuteStringIntegerArray() throws Exception {
        try {
            delegate.execute("foo", (int[]) null);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).execute("foo", (int[]) null);
    }

    @Test
    public void testExecuteString() throws Exception {
        try {
            delegate.execute("foo");
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).execute("foo");
    }

    @Test
    public void testExecuteStringStringArray() throws Exception {
        try {
            delegate.execute("foo", (String[]) null);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).execute("foo", (String[]) null);
    }

    @Test
    public void testExecuteStringInteger() throws Exception {
        try {
            delegate.execute("foo", 1);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).execute("foo", 1);
    }

    @Test
    public void testExecuteBatch() throws Exception {
        try {
            delegate.executeBatch();
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).executeBatch();
    }

    @Test
    public void testExecuteLargeBatch() throws Exception {
        try {
            delegate.executeLargeBatch();
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).executeLargeBatch();
    }

    @Test
    public void testExecuteLargeUpdateStringInteger() throws Exception {
        try {
            delegate.executeLargeUpdate("foo", 1);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).executeLargeUpdate("foo", 1);
    }

    @Test
    public void testExecuteLargeUpdateStringIntegerArray() throws Exception {
        try {
            delegate.executeLargeUpdate("foo", (int[]) null);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).executeLargeUpdate("foo", (int[]) null);
    }

    @Test
    public void testExecuteLargeUpdateString() throws Exception {
        try {
            delegate.executeLargeUpdate("foo");
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).executeLargeUpdate("foo");
    }

    @Test
    public void testExecuteLargeUpdateStringStringArray() throws Exception {
        try {
            delegate.executeLargeUpdate("foo", (String[]) null);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).executeLargeUpdate("foo", (String[]) null);
    }

    @Test
    public void testExecuteQueryString() throws Exception {
        try {
            delegate.executeQuery("foo");
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).executeQuery("foo");
    }

    @Test
    public void testExecuteUpdateStringIntegerArray() throws Exception {
        try {
            delegate.executeUpdate("foo", (int[]) null);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).executeUpdate("foo", (int[]) null);
    }

    @Test
    public void testExecuteUpdateStringStringArray() throws Exception {
        try {
            delegate.executeUpdate("foo", (String[]) null);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).executeUpdate("foo", (String[]) null);
    }

    @Test
    public void testExecuteUpdateString() throws Exception {
        try {
            delegate.executeUpdate("foo");
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).executeUpdate("foo");
    }

    @Test
    public void testExecuteUpdateStringInteger() throws Exception {
        try {
            delegate.executeUpdate("foo", 1);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).executeUpdate("foo", 1);
    }

    /**
     * This method is a bit special, and return the delegate connection, not the
     * wrapped statement's connection.
     *
     * @throws Exception
     */
    @Test
    public void testGetConnection() throws Exception {
        try {
            delegate.getConnection();
        } catch (final SQLException e) {
        }
        verify(obj, times(0)).getConnection();
    }

    @Test
    public void testGetFetchDirection() throws Exception {
        try {
            delegate.getFetchDirection();
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getFetchDirection();
    }

    @Test
    public void testGetFetchSize() throws Exception {
        try {
            delegate.getFetchSize();
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getFetchSize();
    }

    @Test
    public void testGetGeneratedKeys() throws Exception {
        try {
            delegate.getGeneratedKeys();
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getGeneratedKeys();
    }

    @Test
    public void testGetLargeMaxRows() throws Exception {
        try {
            delegate.getLargeMaxRows();
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getLargeMaxRows();
    }

    @Test
    public void testGetLargeUpdateCount() throws Exception {
        try {
            delegate.getLargeUpdateCount();
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getLargeUpdateCount();
    }

    @Test
    public void testGetMaxFieldSize() throws Exception {
        try {
            delegate.getMaxFieldSize();
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getMaxFieldSize();
    }

    @Test
    public void testGetMaxRows() throws Exception {
        try {
            delegate.getMaxRows();
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getMaxRows();
    }

    @Test
    public void testGetMoreResultsInteger() throws Exception {
        try {
            delegate.getMoreResults(1);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getMoreResults(1);
    }

    @Test
    public void testGetMoreResults() throws Exception {
        try {
            delegate.getMoreResults();
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getMoreResults();
    }

    @Test
    public void testGetQueryTimeout() throws Exception {
        try {
            delegate.getQueryTimeout();
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getQueryTimeout();
    }

    @Test
    public void testGetResultSet() throws Exception {
        try {
            delegate.getResultSet();
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getResultSet();
    }

    @Test
    public void testGetResultSetConcurrency() throws Exception {
        try {
            delegate.getResultSetConcurrency();
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getResultSetConcurrency();
    }

    @Test
    public void testGetResultSetHoldability() throws Exception {
        try {
            delegate.getResultSetHoldability();
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getResultSetHoldability();
    }

    @Test
    public void testGetResultSetType() throws Exception {
        try {
            delegate.getResultSetType();
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getResultSetType();
    }

    @Test
    public void testGetUpdateCount() throws Exception {
        try {
            delegate.getUpdateCount();
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getUpdateCount();
    }

    @Test
    public void testGetWarnings() throws Exception {
        try {
            delegate.getWarnings();
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).getWarnings();
    }

    @Test
    public void testIsCloseOnCompletion() throws Exception {
        try {
            delegate.isCloseOnCompletion();
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).isCloseOnCompletion();
    }

    /**
     * This method is a bit special, and call isClosed in the delegate object
     * itself, not in the wrapped statement.
     *
     * @throws Exception
     */
    @Test
    public void testIsClosed() throws Exception {
        try {
            delegate.isClosed();
        } catch (final SQLException e) {
        }
        verify(obj, times(0)).isClosed();
    }

    @Test
    public void testIsPoolable() throws Exception {
        try {
            delegate.isPoolable();
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).isPoolable();
    }

    @Test
    public void testSetCursorNameString() throws Exception {
        try {
            delegate.setCursorName("foo");
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setCursorName("foo");
    }

    @Test
    public void testSetEscapeProcessingBoolean() throws Exception {
        try {
            delegate.setEscapeProcessing(Boolean.TRUE);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setEscapeProcessing(Boolean.TRUE);
    }

    @Test
    public void testSetFetchDirectionInteger() throws Exception {
        try {
            delegate.setFetchDirection(1);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setFetchDirection(1);
    }

    @Test
    public void testSetFetchSizeInteger() throws Exception {
        try {
            delegate.setFetchSize(1);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setFetchSize(1);
    }

    @Test
    public void testSetLargeMaxRowsLong() throws Exception {
        try {
            delegate.setLargeMaxRows(1l);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setLargeMaxRows(1l);
    }

    @Test
    public void testSetMaxFieldSizeInteger() throws Exception {
        try {
            delegate.setMaxFieldSize(1);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setMaxFieldSize(1);
    }

    @Test
    public void testSetMaxRowsInteger() throws Exception {
        try {
            delegate.setMaxRows(1);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setMaxRows(1);
    }

    @Test
    public void testSetPoolableBoolean() throws Exception {
        try {
            delegate.setPoolable(Boolean.TRUE);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setPoolable(Boolean.TRUE);
    }

    @Test
    public void testSetQueryTimeoutInteger() throws Exception {
        try {
            delegate.setQueryTimeout(1);
        } catch (final SQLException e) {
        }
        verify(obj, times(1)).setQueryTimeout(1);
    }

    @Test
    public void testWrap() throws SQLException {
        assertEquals(delegate, delegate.unwrap(Statement.class));
        assertEquals(delegate, delegate.unwrap(DelegatingStatement.class));
        assertEquals(obj, delegate.unwrap(obj.getClass()));
        assertNull(delegate.unwrap(String.class));
        assertTrue(delegate.isWrapperFor(Statement.class));
        assertTrue(delegate.isWrapperFor(DelegatingStatement.class));
        assertTrue(delegate.isWrapperFor(obj.getClass()));
        assertFalse(delegate.isWrapperFor(String.class));
    }
}
