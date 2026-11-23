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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Stream;

import javax.management.OperationsException;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.InOrder;

/**
 */
public class TestPoolableConnection {

    private static Stream<Arguments> fastFailValidationArguments() {
        return Stream.of(false, true).flatMap(fastFail -> Stream.of(null, "", "SELECT 1").map(sql -> Arguments.of(fastFail, sql)));
    }

    private static PreparedStatement mockValidationStatement(final Connection nativeConnection) throws SQLException {
        final PreparedStatement statement = mock(PreparedStatement.class);
        when(nativeConnection.prepareStatement("SELECT 1")).thenReturn(statement);
        when(statement.executeQuery()).thenAnswer(invocation -> {
            final ResultSet resultSet = mock(ResultSet.class);
            when(resultSet.next()).thenReturn(true);
            return resultSet;
        });
        return statement;
    }

    private static Stream<Duration> nonPositiveValidationTimeouts() {
        return Stream.of(Duration.ofSeconds(Long.MIN_VALUE), Duration.ofSeconds(-1), Duration.ofNanos(-1), Duration.ZERO);
    }

    private static Stream<Arguments> positiveValidationTimeouts() {
        return Stream.of(
                Arguments.of(Duration.ofNanos(1), 1),
                Arguments.of(Duration.ofMillis(100), 1),
                Arguments.of(Duration.ofMillis(999), 1),
                Arguments.of(Duration.ofNanos(999_999_999), 1),
                Arguments.of(Duration.ofSeconds(1), 1),
                Arguments.of(Duration.ofSeconds(1, 1), 1),
                Arguments.of(Duration.ofMillis(1900), 1),
                Arguments.of(Duration.ofSeconds(5), 5),
                Arguments.of(Duration.ofSeconds(Integer.MAX_VALUE - 1L), Integer.MAX_VALUE - 1),
                Arguments.of(Duration.ofSeconds(Integer.MAX_VALUE), Integer.MAX_VALUE),
                Arguments.of(Duration.ofSeconds(Integer.MAX_VALUE, 999_999_999), Integer.MAX_VALUE),
                Arguments.of(Duration.ofSeconds(Integer.MAX_VALUE + 1L), Integer.MAX_VALUE),
                Arguments.of(Duration.ofSeconds(1L << 32), Integer.MAX_VALUE),
                Arguments.of(Duration.ofSeconds(Long.MAX_VALUE, 999_999_999), Integer.MAX_VALUE));
    }

    private static Stream<Arguments> validationTimeoutsWithoutQuery() {
        return Stream.concat(positiveValidationTimeouts(), nonPositiveValidationTimeouts().map(timeout -> Arguments.of(timeout, 0)))
                .flatMap(timeout -> Stream.of(null, "").map(sql -> Arguments.of(sql, timeout.get()[0], timeout.get()[1])));
    }

    private GenericObjectPool<PoolableConnection> pool;

    @BeforeEach
    public void setUp() throws Exception {
        final PoolableConnectionFactory factory = new PoolableConnectionFactory(
                new DriverConnectionFactory(new TesterDriver(), "jdbc:apache:commons:testdriver", null), null);
        factory.setDefaultAutoCommit(Boolean.TRUE);
        factory.setDefaultReadOnly(Boolean.TRUE);

        pool = new GenericObjectPool<>(factory);
        factory.setPool(pool);
    }

    @AfterEach
    public void tearDown() {
        pool.close();
    }

    @Test
    void testClosingWrappedInDelegate() throws Exception {
        assertEquals(0, pool.getNumActive());

        final Connection conn = pool.borrowObject();
        final DelegatingConnection<Connection> outer = new DelegatingConnection<>(conn);

        assertFalse(outer.isClosed());
        assertFalse(conn.isClosed());
        assertEquals(1, pool.getNumActive());

        outer.close();

        assertTrue(outer.isClosed());
        assertTrue(conn.isClosed());
        assertEquals(0, pool.getNumActive());
        assertEquals(1, pool.getNumIdle());
    }

    @Test
    void testConnectionPool() throws Exception {
        // Grab a new connection from the pool
        final Connection c = pool.borrowObject();

        assertNotNull(c, "Connection should be created and should not be null");
        assertEquals(1, pool.getNumActive(), "There should be exactly one active object in the pool");

        // Now return the connection by closing it
        c.close(); // Can't be null

        assertEquals(0, pool.getNumActive(), "There should now be zero active objects in the pool");
    }

    @Test
    void testDisconnectionIgnoreSqlCodes() throws Exception {
        pool.setTestOnReturn(true);
        final PoolableConnectionFactory factory = (PoolableConnectionFactory) pool.getFactory();
        factory.setFastFailValidation(true);
        factory.setDisconnectionIgnoreSqlCodes(Arrays.asList("08S02", "08007"));

        final PoolableConnection conn = pool.borrowObject();
        final TesterConnection nativeConnection = (TesterConnection) conn.getInnermostDelegate();

        // set up non-fatal exception
        nativeConnection.setFailure(new SQLException("Non-fatal connection error.", "08S02"));
        assertThrows(SQLException.class, conn::createStatement);
        nativeConnection.setFailure(null);

        // verify that non-fatal connection is returned to the pool
        conn.close();
        assertEquals(0, pool.getNumActive(), "The pool should have no active connections");
        assertEquals(1, pool.getNumIdle(), "The pool should have one idle connection");
    }

    @Test
    void testFastFailValidation() throws Exception {
        pool.setTestOnReturn(true);
        final PoolableConnectionFactory factory = (PoolableConnectionFactory) pool.getFactory();
        factory.setFastFailValidation(true);
        final PoolableConnection conn = pool.borrowObject();
        final TesterConnection nativeConnection = (TesterConnection) conn.getInnermostDelegate();

        // Set up non-fatal exception
        nativeConnection.setFailure(new SQLException("Not fatal error.", "Invalid syntax."));
        assertThrows(SQLException.class, conn::createStatement);
        // cleanup failure
        nativeConnection.setFailure(null);

        // validate should not fail - error was not fatal and condition was cleaned up
        conn.validate("SELECT 1", Duration.ofSeconds(1000));

        // now set up fatal failure
        nativeConnection.setFailure(new SQLException("Fatal connection error.", "01002"));
        assertThrows(SQLException.class, conn::createStatement);
        // cleanup failure
        nativeConnection.setFailure(null);

        // validate should now fail because of previous fatal error, despite cleanup
        assertThrows(SQLException.class, () -> conn.validate("SELECT 1", Duration.ofSeconds(1000)), "Should throw SQL exception on validation.");

        // verify that bad connection does not get returned to the pool
        conn.close(); // testOnReturn triggers validate, which should fail
        assertEquals(0, pool.getNumActive(), "The pool should have no active connections");
        assertEquals(0, pool.getNumIdle(), "The pool should have no idle connections");
    }

    @Test
    void testFastFailValidationCustomCodes() throws Exception {
        pool.setTestOnReturn(true);
        final PoolableConnectionFactory factory = (PoolableConnectionFactory) pool.getFactory();
        factory.setFastFailValidation(true);
        final ArrayList<String> disconnectionSqlCodes = new ArrayList<>();
        disconnectionSqlCodes.add("XXX");
        factory.setDisconnectionSqlCodes(disconnectionSqlCodes);
        final PoolableConnection conn = pool.borrowObject();
        final TesterConnection nativeConnection = (TesterConnection) conn.getInnermostDelegate();

        // Set up fatal exception
        nativeConnection.setFailure(new SQLException("Fatal connection error.", "XXX"));
        assertThrows(SQLException.class, conn::createStatement);
        // cleanup failure
        nativeConnection.setFailure(null);

        // verify that bad connection does not get returned to the pool
        conn.close(); // testOnReturn triggers validate, which should fail
        assertEquals(0, pool.getNumActive(), "The pool should have no active connections");
        assertEquals(0, pool.getNumIdle(), "The pool should have no idle connections");
    }

    @Test
    void testIsDisconnectionSqlExceptionStackOverflow() throws Exception {
        final int maxDeep = 100_000;
        final SQLException rootException = new SQLException("Data truncated", "22001");
        SQLException parentException = rootException;
        for (int i = 0; i <= maxDeep; i++) {
            final SQLException childException = new SQLException("Data truncated: " + i, "22001");
            parentException.setNextException(childException);
            parentException = childException;
        }
        final Connection conn = pool.borrowObject();
        assertFalse(((PoolableConnection) conn).isDisconnectionSqlException(rootException));
        assertFalse(((PoolableConnection) conn).isFatalException(rootException));
    }

    /**
     * Tests if the {@link PoolableConnectionMXBean} interface is a valid MXBean interface.
     */
    @Test
    void testMXBeanCompliance() throws OperationsException {
        TestBasicDataSourceMXBean.testMXBeanCompliance(PoolableConnectionMXBean.class);
    }

    // Bugzilla Bug 33591: PoolableConnection leaks connections if the
    // delegated connection closes itself.
    @Test
    void testPoolableConnectionLeak() throws Exception {
        // 'Borrow' a connection from the pool
        final Connection conn = pool.borrowObject();

        // Now close our innermost delegate, simulating the case where the
        // underlying connection closes itself
        ((PoolableConnection) conn).getInnermostDelegate().close();

        // At this point, we can close the pooled connection. The
        // PoolableConnection *should* realize that its underlying
        // connection is gone and invalidate itself. The pool should have no
        // active connections.

        try {
            conn.close();
        } catch (final SQLException e) {
            // Here we expect 'connection already closed', but the connection
            // should *NOT* be returned to the pool
        }

        assertEquals(0, pool.getNumActive(), "The pool should have no active connections");
    }

    @ParameterizedTest(name = "fastFail={0}, sql={1}")
    @MethodSource("fastFailValidationArguments")
    void testValidateAfterFatalException(final boolean fastFail, final String sql) throws Exception {
        final Connection nativeConnection = mock(Connection.class);
        final PreparedStatement statement = mockValidationStatement(nativeConnection);
        final SQLException fatalException = new SQLException("Connection lost", "08006");
        when(nativeConnection.createStatement()).thenThrow(fatalException);
        when(nativeConnection.isValid(1)).thenReturn(true);
        final PoolableConnection conn = new PoolableConnection(nativeConnection, null, null, null, fastFail);
        try {
            assertSame(fatalException, assertThrows(SQLException.class, conn::createStatement));
            if (fastFail) {
                assertThrows(SQLException.class, () -> conn.validate(sql, Duration.ofMillis(100)));
                verify(nativeConnection, never()).isValid(anyInt());
                verify(nativeConnection, never()).prepareStatement(anyString());
            } else {
                conn.validate(sql, Duration.ofMillis(100));
                if (sql == null || sql.isEmpty()) {
                    verify(nativeConnection).isValid(1);
                } else {
                    verify(statement).setQueryTimeout(1);
                    verify(statement).executeQuery();
                }
            }
        } finally {
            conn.reallyClose();
        }
    }

    @ParameterizedTest
    @MethodSource("nonPositiveValidationTimeouts")
    void testValidateWithDifferentQueryNonPositiveTimeout(final Duration timeout) throws Exception {
        final Connection nativeConnection = mock(Connection.class);
        final TesterPreparedStatement firstStatement = new TesterPreparedStatement(nativeConnection, "SELECT 1");
        final TesterPreparedStatement secondStatement = new TesterPreparedStatement(nativeConnection, "SELECT 2");
        final int defaultTimeout = secondStatement.getQueryTimeout();
        when(nativeConnection.prepareStatement("SELECT 1")).thenReturn(firstStatement);
        when(nativeConnection.prepareStatement("SELECT 2")).thenReturn(secondStatement);
        final PoolableConnection conn = new PoolableConnection(nativeConnection, null, null);
        try {
            conn.validate("SELECT 1", Duration.ofSeconds(5));
            conn.validate("SELECT 2", timeout);
            assertEquals(5, firstStatement.getQueryTimeout());
            assertEquals(defaultTimeout, secondStatement.getQueryTimeout());
            verify(nativeConnection).prepareStatement("SELECT 1");
            verify(nativeConnection).prepareStatement("SELECT 2");
        } finally {
            firstStatement.close();
            conn.reallyClose();
        }
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testValidateWithoutQueryInvalidConnection(final String sql) throws Exception {
        final Connection nativeConnection = mock(Connection.class);
        final PoolableConnection conn = new PoolableConnection(nativeConnection, null, null);
        try {
            final SQLException exception = assertThrows(SQLException.class, () -> conn.validate(sql, Duration.ofMillis(100)));
            assertEquals("isValid() returned false", exception.getMessage());
            verify(nativeConnection).isValid(1);
        } finally {
            conn.reallyClose();
        }
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testValidateWithoutQuerySQLException(final String sql) throws Exception {
        final Connection nativeConnection = mock(Connection.class);
        final SQLException expected = new SQLException("Validation failed", "08006");
        when(nativeConnection.isValid(1)).thenThrow(expected);
        final PoolableConnection conn = new PoolableConnection(nativeConnection, null, null);
        try {
            assertSame(expected, assertThrows(SQLException.class, () -> conn.validate(sql, Duration.ofMillis(100))));
            verify(nativeConnection).isValid(1);
        } finally {
            conn.reallyClose();
        }
    }

    @ParameterizedTest(name = "sql={0}, timeout={1}, seconds={2}")
    @MethodSource("validationTimeoutsWithoutQuery")
    void testValidateWithoutQueryTimeout(final String sql, final Duration timeout, final int timeoutSeconds) throws Exception {
        final Connection nativeConnection = mock(Connection.class);
        when(nativeConnection.isValid(timeoutSeconds)).thenReturn(true);
        final PoolableConnection conn = new PoolableConnection(nativeConnection, null, null);
        try {
            conn.validate(sql, timeout);
            verify(nativeConnection).isValid(timeoutSeconds);
            verify(nativeConnection, never()).prepareStatement(anyString());
        } finally {
            conn.reallyClose();
        }
    }

    @ParameterizedTest
    @MethodSource("nonPositiveValidationTimeouts")
    void testValidateWithQueryNonPositiveTimeout(final Duration timeout) throws Exception {
        final Connection nativeConnection = mock(Connection.class);
        final PreparedStatement statement = mockValidationStatement(nativeConnection);
        final PoolableConnection conn = new PoolableConnection(nativeConnection, null, null);
        try {
            conn.validate("SELECT 1", timeout);
            verify(statement, never()).setQueryTimeout(anyInt());
            verify(statement).executeQuery();
        } finally {
            conn.reallyClose();
        }
    }

    @Test
    void testValidateWithQueryNoRows() throws Exception {
        final Connection nativeConnection = mock(Connection.class);
        final PreparedStatement statement = mockValidationStatement(nativeConnection);
        final ResultSet resultSet = mock(ResultSet.class);
        when(statement.executeQuery()).thenReturn(resultSet);
        final PoolableConnection conn = new PoolableConnection(nativeConnection, null, null);
        try {
            final SQLException exception = assertThrows(SQLException.class, () -> conn.validate("SELECT 1", Duration.ofMillis(100)));
            assertEquals("validationQuery didn't return a row", exception.getMessage());
            verify(statement).setQueryTimeout(1);
            verify(resultSet).close();
        } finally {
            conn.reallyClose();
        }
    }

    @ParameterizedTest
    @MethodSource("positiveValidationTimeouts")
    void testValidateWithQueryPositiveTimeout(final Duration timeout, final int timeoutSeconds) throws Exception {
        final Connection nativeConnection = mock(Connection.class);
        final PreparedStatement statement = mockValidationStatement(nativeConnection);
        final PoolableConnection conn = new PoolableConnection(nativeConnection, null, null);
        try {
            conn.validate("SELECT 1", timeout);
            final InOrder order = inOrder(statement);
            order.verify(statement).setQueryTimeout(timeoutSeconds);
            order.verify(statement).executeQuery();
            order.verifyNoMoreInteractions();
            verify(nativeConnection, never()).isValid(anyInt());
        } finally {
            conn.reallyClose();
        }
    }

    @ParameterizedTest
    @MethodSource("nonPositiveValidationTimeouts")
    void testValidateWithQueryRetainsCachedTimeout(final Duration timeout) throws Exception {
        final Connection nativeConnection = mock(Connection.class);
        final TesterPreparedStatement statement = new TesterPreparedStatement(nativeConnection, "SELECT 1");
        when(nativeConnection.prepareStatement("SELECT 1")).thenReturn(statement);
        final PoolableConnection conn = new PoolableConnection(nativeConnection, null, null);
        try {
            conn.validate("SELECT 1", Duration.ofSeconds(5));
            conn.validate("SELECT 1", timeout);
            assertEquals(5, statement.getQueryTimeout());
            verify(nativeConnection).prepareStatement("SELECT 1");
        } finally {
            conn.reallyClose();
        }
    }

    @Test
    void testValidateWithQuerySetTimeoutSQLException() throws Exception {
        final Connection nativeConnection = mock(Connection.class);
        final PreparedStatement statement = mockValidationStatement(nativeConnection);
        final SQLException expected = new SQLException("Cannot set query timeout");
        doThrow(expected).when(statement).setQueryTimeout(1);
        final PoolableConnection conn = new PoolableConnection(nativeConnection, null, null);
        try {
            assertSame(expected, assertThrows(SQLException.class, () -> conn.validate("SELECT 1", Duration.ofMillis(100))));
            verify(statement, never()).executeQuery();
        } finally {
            conn.reallyClose();
        }
    }

    @Test
    void testValidateWithQueryTimeoutException() throws Exception {
        final Connection nativeConnection = mock(Connection.class);
        final PreparedStatement statement = mockValidationStatement(nativeConnection);
        final SQLTimeoutException expected = new SQLTimeoutException("Validation query timed out");
        when(statement.executeQuery()).thenThrow(expected);
        final PoolableConnection conn = new PoolableConnection(nativeConnection, null, null);
        try {
            assertSame(expected, assertThrows(SQLTimeoutException.class, () -> conn.validate("SELECT 1", Duration.ofMillis(100))));
            verify(statement).setQueryTimeout(1);
        } finally {
            conn.reallyClose();
        }
    }

    @ParameterizedTest
    @MethodSource("positiveValidationTimeouts")
    void testValidateWithQueryUpdatesCachedTimeout(final Duration timeout, final int timeoutSeconds) throws Exception {
        final Connection nativeConnection = mock(Connection.class);
        final PreparedStatement statement = mockValidationStatement(nativeConnection);
        final PoolableConnection conn = new PoolableConnection(nativeConnection, null, null);
        try {
            conn.validate("SELECT 1", Duration.ofSeconds(5));
            conn.validate("SELECT 1", timeout);
            verify(nativeConnection).prepareStatement("SELECT 1");
            final InOrder order = inOrder(statement);
            order.verify(statement).setQueryTimeout(5);
            order.verify(statement).executeQuery();
            order.verify(statement).setQueryTimeout(timeoutSeconds);
            order.verify(statement).executeQuery();
            order.verifyNoMoreInteractions();
        } finally {
            conn.reallyClose();
        }
    }
}
