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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestDelegatingStatement {

    private static final class TesterStatementNonWrapping extends TesterStatement {

        public TesterStatementNonWrapping(final Connection conn) {
            super(conn);
        }

        @Override
        public boolean isWrapperFor(final Class<?> iface) throws SQLException {
            return false;
        }
    }

    private DelegatingConnection<Connection> delegatingConnection;
    private TesterConnection testerConnection;
    private Statement mockedStatement;
    private DelegatingStatement delegatingStatement;
    private DelegatingStatement delegatingTesterStatement;
    private TesterResultSet testerResultSet;
    private TesterStatement testerStatement;

    @AfterEach
    public void afterEach() {
        testerResultSet.setSqlExceptionOnClose(false);
        testerStatement.setSqlExceptionOnClose(false);
    }

    @BeforeEach
    public void setUp() throws Exception {
        testerConnection = new TesterConnection("test", "test");
        delegatingConnection = new DelegatingConnection<>(testerConnection);
        mockedStatement = mock(Statement.class);
        testerStatement = new TesterStatement(testerConnection);
        delegatingStatement = new DelegatingStatement(delegatingConnection, mockedStatement);
        delegatingTesterStatement = new DelegatingStatement(delegatingConnection, testerStatement);
        testerResultSet = new TesterResultSet(mockedStatement);
    }

    @Test
    void testAddBatchString() throws Exception {
        try {
            delegatingStatement.addBatch("foo");
        } catch (final SQLException e) {
        }
        verify(mockedStatement, times(1)).addBatch("foo");
    }

    @Test
    void testCancel() throws Exception {
        try {
            delegatingStatement.cancel();
        } catch (final SQLException e) {
        }
        verify(mockedStatement, times(1)).cancel();
    }

    @Test
    void testCheckOpen() throws Exception {
        delegatingStatement.checkOpen();
        delegatingStatement.close();
        assertThrows(SQLException.class, delegatingStatement::checkOpen);
    }

    @Test
    void testClearBatch() throws Exception {
        try {
            delegatingStatement.clearBatch();
        } catch (final SQLException e) {
        }
        verify(mockedStatement, times(1)).clearBatch();
    }

    @Test
    void testClearWarnings() throws Exception {
        try {
            delegatingStatement.clearWarnings();
        } catch (final SQLException e) {
        }
        verify(mockedStatement, times(1)).clearWarnings();
    }

    @Test
    void testClose() throws Exception {
        try {
            delegatingStatement.close();
        } catch (final SQLException e) {
        }
        verify(mockedStatement, times(1)).close();
    }

    @Test
    void testCloseOnCompletion() throws Exception {
        try {
            delegatingStatement.closeOnCompletion();
        } catch (final SQLException e) {
        }
        verify(mockedStatement, times(1)).closeOnCompletion();
    }

    @Test
    void testCloseWithResultSetCloseException() throws Exception {
        testerResultSet.setSqlExceptionOnClose(true);
        delegatingStatement.addTrace(testerResultSet);
        final SQLException e = assertThrows(SQLException.class, delegatingStatement::close);
        assertInstanceOf(SQLExceptionList.class, e);
        verify(mockedStatement, times(1)).close();
    }

    @Test
    void testCloseWithStatementCloseException() throws Exception {
        testerStatement.setSqlExceptionOnClose(true);
        final SQLException e = assertThrows(SQLException.class, delegatingTesterStatement::close);
        assertInstanceOf(SQLExceptionList.class, e);
    }

    @Test
    void testExecuteBatch() throws Exception {
        try {
            delegatingStatement.executeBatch();
        } catch (final SQLException e) {
        }
        verify(mockedStatement, times(1)).executeBatch();
    }

    @Test
    void testExecuteLargeBatch() throws Exception {
        try {
            delegatingStatement.executeLargeBatch();
        } catch (final SQLException e) {
        }
        verify(mockedStatement, times(1)).executeLargeBatch();
    }

    @Test
    void testExecuteLargeUpdateString() throws Exception {
        try {
            delegatingStatement.executeLargeUpdate("foo");
        } catch (final SQLException e) {
        }
        verify(mockedStatement, times(1)).executeLargeUpdate("foo");
    }

    @Test
    void testExecuteLargeUpdateStringInteger() throws Exception {
        try {
            delegatingStatement.executeLargeUpdate("foo", 1);
        } catch (final SQLException e) {
        }
        verify(mockedStatement, times(1)).executeLargeUpdate("foo", 1);
    }

    @Test
    void testExecuteLargeUpdateStringIntegerArray() throws Exception {
        try {
            delegatingStatement.executeLargeUpdate("foo", (int[]) null);
        } catch (final SQLException e) {
        }
        verify(mockedStatement, times(1)).executeLargeUpdate("foo", (int[]) null);
    }

    @Test
    void testExecuteLargeUpdateStringStringArray() throws Exception {
        try {
            delegatingStatement.executeLargeUpdate("foo", (String[]) null);
        } catch (final SQLException e) {
        }
        verify(mockedStatement, times(1)).executeLargeUpdate("foo", (String[]) null);
    }

    @Test
    void testExecuteQueryReturnsNull() throws Exception {
        assertNull(delegatingStatement.executeQuery("null"));
    }

    @Test
    void testExecuteQueryString() throws Exception {
        try {
            delegatingStatement.executeQuery("foo");
        } catch (final SQLException e) {
        }
        verify(mockedStatement, times(1)).executeQuery("foo");
    }

    @Test
    void testExecuteString() throws Exception {
        try {
            delegatingStatement.execute("foo");
        } catch (final SQLException e) {
        }
        verify(mockedStatement, times(1)).execute("foo");
    }

    @Test
    void testExecuteStringInteger() throws Exception {
        try {
            delegatingStatement.execute("foo", 1);
        } catch (final SQLException e) {
        }
        verify(mockedStatement, times(1)).execute("foo", 1);
    }

    @Test
    void testExecuteStringIntegerArray() throws Exception {
        try {
            delegatingStatement.execute("foo", (int[]) null);
        } catch (final SQLException e) {
        }
        verify(mockedStatement, times(1)).execute("foo", (int[]) null);
    }

    @Test
    void testExecuteStringStringArray() throws Exception {
        try {
            delegatingStatement.execute("foo", (String[]) null);
        } catch (final SQLException e) {
        }
        verify(mockedStatement, times(1)).execute("foo", (String[]) null);
    }

    @Test
    void testExecuteUpdateString() throws Exception {
        try {
            delegatingStatement.executeUpdate("foo");
        } catch (final SQLException e) {
        }
        verify(mockedStatement, times(1)).executeUpdate("foo");
    }

    @Test
    void testExecuteUpdateStringInteger() throws Exception {
        try {
            delegatingStatement.executeUpdate("foo", 1);
        } catch (final SQLException e) {
        }
        verify(mockedStatement, times(1)).executeUpdate("foo", 1);
    }

    @Test
    void testExecuteUpdateStringIntegerArray() throws Exception {
        try {
            delegatingStatement.executeUpdate("foo", (int[]) null);
        } catch (final SQLException e) {
        }
        verify(mockedStatement, times(1)).executeUpdate("foo", (int[]) null);
    }

    @Test
    void testExecuteUpdateStringStringArray() throws Exception {
        try {
            delegatingStatement.executeUpdate("foo", (String[]) null);
        } catch (final SQLException e) {
        }
        verify(mockedStatement, times(1)).executeUpdate("foo", (String[]) null);
    }

    /**
     * This method is a bit special, and return the delegate connection, not the wrapped statement's connection.
     *
     * @throws Exception
     */
    @Test
    void testGetConnection() throws Exception {
        try {
            delegatingStatement.getConnection();
        } catch (final SQLException e) {
        }
        verify(mockedStatement, times(0)).getConnection();
    }

    @Test
    void testGetDelegate() throws Exception {
        assertEquals(mockedStatement, delegatingStatement.getDelegate());
    }

    @Test
    void testGetFetchDirection() throws Exception {
        try {
            delegatingStatement.getFetchDirection();
        } catch (final SQLException e) {
        }
        verify(mockedStatement, times(1)).getFetchDirection();
    }

    @Test
    void testGetFetchSize() throws Exception {
        try {
            delegatingStatement.getFetchSize();
        } catch (final SQLException e) {
        }
        verify(mockedStatement, times(1)).getFetchSize();
    }

    @Test
    void testGetGeneratedKeys() throws Exception {
        try {
            delegatingStatement.getGeneratedKeys();
        } catch (final SQLException e) {
        }
        verify(mockedStatement, times(1)).getGeneratedKeys();
    }

    @Test
    void testGetLargeMaxRows() throws Exception {
        try {
            delegatingStatement.getLargeMaxRows();
        } catch (final SQLException e) {
        }
        verify(mockedStatement, times(1)).getLargeMaxRows();
    }

    @Test
    void testGetLargeUpdateCount() throws Exception {
        try {
            delegatingStatement.getLargeUpdateCount();
        } catch (final SQLException e) {
        }
        verify(mockedStatement, times(1)).getLargeUpdateCount();
    }

    @Test
    void testGetMaxFieldSize() throws Exception {
        try {
            delegatingStatement.getMaxFieldSize();
        } catch (final SQLException e) {
        }
        verify(mockedStatement, times(1)).getMaxFieldSize();
    }

    @Test
    void testGetMaxRows() throws Exception {
        try {
            delegatingStatement.getMaxRows();
        } catch (final SQLException e) {
        }
        verify(mockedStatement, times(1)).getMaxRows();
    }

    @Test
    void testGetMoreResults() throws Exception {
        try {
            delegatingStatement.getMoreResults();
        } catch (final SQLException e) {
        }
        verify(mockedStatement, times(1)).getMoreResults();
    }

    @Test
    void testGetMoreResultsInteger() throws Exception {
        try {
            delegatingStatement.getMoreResults(1);
        } catch (final SQLException e) {
        }
        verify(mockedStatement, times(1)).getMoreResults(1);
    }

    @Test
    void testGetQueryTimeout() throws Exception {
        try {
            delegatingStatement.getQueryTimeout();
        } catch (final SQLException e) {
        }
        verify(mockedStatement, times(1)).getQueryTimeout();
    }

    @Test
    void testGetResultSet() throws Exception {
        try {
            delegatingStatement.getResultSet();
        } catch (final SQLException e) {
        }
        verify(mockedStatement, times(1)).getResultSet();
    }

    @Test
    void testGetResultSetConcurrency() throws Exception {
        try {
            delegatingStatement.getResultSetConcurrency();
        } catch (final SQLException e) {
        }
        verify(mockedStatement, times(1)).getResultSetConcurrency();
    }

    @Test
    void testGetResultSetHoldability() throws Exception {
        try {
            delegatingStatement.getResultSetHoldability();
        } catch (final SQLException e) {
        }
        verify(mockedStatement, times(1)).getResultSetHoldability();
    }

    @Test
    void testGetResultSetType() throws Exception {
        try {
            delegatingStatement.getResultSetType();
        } catch (final SQLException e) {
        }
        verify(mockedStatement, times(1)).getResultSetType();
    }

    @Test
    void testGetUpdateCount() throws Exception {
        try {
            delegatingStatement.getUpdateCount();
        } catch (final SQLException e) {
        }
        verify(mockedStatement, times(1)).getUpdateCount();
    }

    @Test
    void testGetWarnings() throws Exception {
        try {
            delegatingStatement.getWarnings();
        } catch (final SQLException e) {
        }
        verify(mockedStatement, times(1)).getWarnings();
    }

    /**
     * This method is a bit special, and call isClosed in the delegate object itself, not in the wrapped statement.
     *
     * @throws Exception
     */
    @Test
    void testIsClosed() throws Exception {
        try {
            delegatingStatement.isClosed();
        } catch (final SQLException e) {
        }
        verify(mockedStatement, times(0)).isClosed();
    }

    @Test
    void testIsCloseOnCompletion() throws Exception {
        try {
            delegatingStatement.isCloseOnCompletion();
        } catch (final SQLException e) {
        }
        verify(mockedStatement, times(1)).isCloseOnCompletion();
    }

    @Test
    void testIsPoolable() throws Exception {
        try {
            delegatingStatement.isPoolable();
        } catch (final SQLException e) {
        }
        verify(mockedStatement, times(1)).isPoolable();
    }

    @Test
    void testIsWrapperFor() throws Exception {
        final TesterConnection tstConn = new TesterConnection("test", "test");
        final TesterStatement tstStmt = new TesterStatementNonWrapping(tstConn);
        final DelegatingConnection<TesterConnection> dconn = new DelegatingConnection<>(tstConn);
        final DelegatingStatement stamt = new DelegatingStatement(dconn, tstStmt);

        final Class<?> stmtProxyClass = Proxy.getProxyClass(this.getClass().getClassLoader(), Statement.class);

        assertTrue(stamt.isWrapperFor(DelegatingStatement.class));
        assertTrue(stamt.isWrapperFor(TesterStatement.class));
        assertFalse(stamt.isWrapperFor(stmtProxyClass));

        stamt.close();
    }

    @Test
    void testSetCursorNameString() throws Exception {
        try {
            delegatingStatement.setCursorName("foo");
        } catch (final SQLException e) {
        }
        verify(mockedStatement, times(1)).setCursorName("foo");
    }

    @Test
    void testSetEscapeProcessingBoolean() throws Exception {
        try {
            delegatingStatement.setEscapeProcessing(Boolean.TRUE);
        } catch (final SQLException e) {
        }
        verify(mockedStatement, times(1)).setEscapeProcessing(Boolean.TRUE);
    }

    @Test
    void testSetFetchDirectionInteger() throws Exception {
        try {
            delegatingStatement.setFetchDirection(1);
        } catch (final SQLException e) {
        }
        verify(mockedStatement, times(1)).setFetchDirection(1);
    }

    @Test
    void testSetFetchSizeInteger() throws Exception {
        try {
            delegatingStatement.setFetchSize(1);
        } catch (final SQLException e) {
        }
        verify(mockedStatement, times(1)).setFetchSize(1);
    }

    @Test
    void testSetLargeMaxRowsLong() throws Exception {
        try {
            delegatingStatement.setLargeMaxRows(1L);
        } catch (final SQLException e) {
        }
        verify(mockedStatement, times(1)).setLargeMaxRows(1L);
    }

    @Test
    void testSetMaxFieldSizeInteger() throws Exception {
        try {
            delegatingStatement.setMaxFieldSize(1);
        } catch (final SQLException e) {
        }
        verify(mockedStatement, times(1)).setMaxFieldSize(1);
    }

    @Test
    void testSetMaxRowsInteger() throws Exception {
        try {
            delegatingStatement.setMaxRows(1);
        } catch (final SQLException e) {
        }
        verify(mockedStatement, times(1)).setMaxRows(1);
    }

    @Test
    void testSetPoolableBoolean() throws Exception {
        try {
            delegatingStatement.setPoolable(Boolean.TRUE);
        } catch (final SQLException e) {
        }
        verify(mockedStatement, times(1)).setPoolable(Boolean.TRUE);
    }

    @Test
    void testSetQueryTimeoutInteger() throws Exception {
        try {
            delegatingStatement.setQueryTimeout(1);
        } catch (final SQLException e) {
        }
        verify(mockedStatement, times(1)).setQueryTimeout(1);
    }

    @Test
    void testWrap() throws SQLException {
        assertEquals(delegatingStatement, delegatingStatement.unwrap(Statement.class));
        assertEquals(delegatingStatement, delegatingStatement.unwrap(DelegatingStatement.class));
        assertEquals(mockedStatement, delegatingStatement.unwrap(mockedStatement.getClass()));
        assertNull(delegatingStatement.unwrap(String.class));
        assertTrue(delegatingStatement.isWrapperFor(Statement.class));
        assertTrue(delegatingStatement.isWrapperFor(DelegatingStatement.class));
        assertTrue(delegatingStatement.isWrapperFor(mockedStatement.getClass()));
        assertFalse(delegatingStatement.isWrapperFor(String.class));
    }
}
