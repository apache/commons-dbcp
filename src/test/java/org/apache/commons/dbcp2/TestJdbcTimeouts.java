/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.sql.PooledConnection;

import org.apache.commons.dbcp2.cpdsadapter.DriverAdapterCPDS;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Tests JDBC timeout conversion and statement timeout isolation. */
class TestJdbcTimeouts {

    /**
     * Creates a statement using one of the JDBC statement creation overloads.
     *
     * @param connection the connection that creates the statement.
     * @param variant the creation overload index, from 0 through 11.
     * @return the created statement.
     * @throws SQLException if statement creation fails.
     * @throws IllegalArgumentException if variant is not a supported overload index.
     */
    private static Statement createStatement(final Connection connection, final int variant) throws SQLException {
        switch (variant) {
        case 0:
            return connection.createStatement();
        case 1:
            return connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        case 2:
            return connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT);
        case 3:
            return connection.prepareStatement("SELECT 1");
        case 4:
            return connection.prepareStatement("SELECT 1", Statement.RETURN_GENERATED_KEYS);
        case 5:
            return connection.prepareStatement("SELECT 1", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        case 6:
            return connection.prepareStatement("SELECT 1", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT);
        case 7:
            return connection.prepareStatement("SELECT 1", new int[] {1});
        case 8:
            return connection.prepareStatement("SELECT 1", new String[] {"id"});
        case 9:
            return connection.prepareCall("SELECT 1");
        case 10:
            return connection.prepareCall("SELECT 1", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        case 11:
            return connection.prepareCall("SELECT 1", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT);
        default:
            throw new IllegalArgumentException("Unknown statement variant " + variant);
        }
    }

    /**
     * Creates a callable statement test double backed by a mutable query timeout.
     *
     * @param timeout the value read and updated by the timeout accessors.
     * @return the statement test double.
     * @throws SQLException if configuring the statement test double fails.
     */
    private static CallableStatement mockStatement(final AtomicInteger timeout) throws SQLException {
        final CallableStatement statement = mock(CallableStatement.class);
        when(statement.getQueryTimeout()).thenAnswer(invocation -> timeout.get());
        doAnswer(invocation -> {
            timeout.set(invocation.getArgument(0));
            return null;
        }).when(statement).setQueryTimeout(anyInt());
        return statement;
    }

    /**
     * Creates a data source with statement pooling and the supplied default query timeout.
     *
     * @param defaultTimeout the default query timeout, or {@code null} to retain the driver default.
     * @return the configured data source.
     */
    private static BasicDataSource newDataSource(final Duration defaultTimeout) {
        final BasicDataSource source = new BasicDataSource();
        source.setDriverClassName(TesterDriver.class.getName());
        source.setUrl("jdbc:apache:commons:testdriver");
        source.setUsername("u1");
        source.setPassword("p1");
        source.setMaxTotal(1);
        source.setPoolPreparedStatements(true);
        source.setDefaultQueryTimeout(defaultTimeout);
        return source;
    }

    /**
     * Supplies statement types and timeout changes for delegate replacement tests.
     *
     * @return callable-statement and timeout-change flag combinations.
     */
    static Stream<Arguments> changedDelegates() {
        return Stream.of(false, true).flatMap(callable -> Stream.of(false, true).map(changeTimeout -> Arguments.of(callable, changeTimeout)));
    }

    /**
     * Supplies statement types and failure modes for timeout setter tests.
     *
     * @return callable-statement and unchecked-failure flag combinations.
     */
    static Stream<Arguments> failedTimeoutSetters() {
        return Stream.of(false, true).flatMap(callable -> Stream.of(false, true).map(unchecked -> Arguments.of(callable, unchecked)));
    }

    /**
     * Supplies statement types and default query timeouts for pooled statement tests.
     *
     * @return statement type and default timeout combinations, including an unset timeout.
     */
    static Stream<Arguments> pooledTimeouts() {
        return Stream.of(false, true).flatMap(callable -> Stream.of(null, Duration.ZERO, Duration.ofMillis(100), Duration.ofSeconds(5))
                .map(timeout -> Arguments.of(callable, timeout)));
    }

    /**
     * Supplies timeout conversion cases for every statement creation overload.
     *
     * @return creation overloads, durations, and their expected JDBC timeout values.
     */
    static Stream<Arguments> statementTimeouts() {
        return IntStream.range(0, 12).boxed().flatMap(variant -> timeouts().map(argument ->
                Arguments.of(variant, argument.get()[0], argument.get()[1])));
    }

    /**
     * Supplies duration boundary cases and their expected JDBC timeout values.
     *
     * @return negative, zero, fractional, and oversized duration conversion cases.
     */
    static Stream<Arguments> timeouts() {
        return Stream.of(
                Arguments.of(Duration.ofSeconds(Long.MIN_VALUE), Integer.MIN_VALUE),
                Arguments.of(Duration.ofSeconds((long) Integer.MIN_VALUE - 1), Integer.MIN_VALUE),
                Arguments.of(Duration.ofSeconds(Integer.MIN_VALUE), Integer.MIN_VALUE),
                Arguments.of(Duration.ofSeconds(-1), -1),
                Arguments.of(Duration.ofNanos(-1), -1),
                Arguments.of(Duration.ZERO, 0),
                Arguments.of(Duration.ofNanos(1), 1),
                Arguments.of(Duration.ofMillis(100), 1),
                Arguments.of(Duration.ofNanos(999_999_999), 1),
                Arguments.of(Duration.ofSeconds(1), 1),
                Arguments.of(Duration.ofMillis(1_900), 1),
                Arguments.of(Duration.ofSeconds(5), 5),
                Arguments.of(Duration.ofSeconds(Integer.MAX_VALUE - 1), Integer.MAX_VALUE - 1),
                Arguments.of(Duration.ofSeconds(Integer.MAX_VALUE, 999_999_999), Integer.MAX_VALUE),
                Arguments.of(Duration.ofSeconds((long) Integer.MAX_VALUE + 1), Integer.MAX_VALUE),
                Arguments.of(Duration.ofSeconds(1L << 32), Integer.MAX_VALUE),
                Arguments.of(Duration.ofSeconds(Long.MAX_VALUE, 999_999_999), Integer.MAX_VALUE));
    }

    /**
     * Tests that pooled statement query timeouts are restored between borrowers.
     *
     * @param callable whether to exercise callable statements instead of prepared statements.
     * @param defaultTimeout the configured default query timeout, or {@code null} for the driver default.
     * @throws Exception if setting up, using, or closing the test resources fails.
     */
    @ParameterizedTest
    @MethodSource("pooledTimeouts")
    void testBasicDataSourceRestoresTimeoutBetweenBorrowers(final boolean callable, final Duration defaultTimeout) throws Exception {
        try (BasicDataSource source = newDataSource(defaultTimeout)) {
            final int expected = defaultTimeout == null ? 1000 : Utils.toJdbcSeconds(defaultTimeout);
            for (int i = 0; i < 3; i++) {
                try (Connection connection = source.getConnection();
                        PreparedStatement statement = callable ? connection.prepareCall("SELECT 1") : connection.prepareStatement("SELECT 1")) {
                    assertEquals(expected, statement.getQueryTimeout());
                    statement.setQueryTimeout(7);
                    statement.setQueryTimeout(9);
                }
            }
        }
    }

    /**
     * Tests that DriverAdapterCPDS restores query timeouts between statement borrowers.
     *
     * @param callable whether to exercise callable statements instead of prepared statements.
     * @throws Exception if setting up, using, or closing the test resources fails.
     */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testCpdsAdapterRestoresTimeout(final boolean callable) throws Exception {
        final DriverAdapterCPDS source = new DriverAdapterCPDS();
        source.setDriver(TesterDriver.class.getName());
        source.setUrl("jdbc:apache:commons:testdriver");
        source.setPoolPreparedStatements(true);
        final PooledConnection pooled = source.getPooledConnection("u1", "p1");
        try {
            for (int i = 0; i < 3; i++) {
                try (Connection connection = pooled.getConnection();
                        PreparedStatement statement = callable ? connection.prepareCall("SELECT 1") : connection.prepareStatement("SELECT 1")) {
                    assertEquals(1000, statement.getQueryTimeout());
                    statement.setQueryTimeout(7);
                }
            }
        } finally {
            pooled.close();
        }
    }

    /**
     * Tests timeout conversion, negative timeout rejection, and cleanup for each statement creation overload.
     *
     * @param variant the statement creation overload index, from 0 through 11.
     * @param timeout the duration under test.
     * @param expected the expected timeout passed to JDBC, in seconds.
     * @throws SQLException if JDBC setup, an operation, or cleanup unexpectedly fails.
     */
    @ParameterizedTest
    @MethodSource("statementTimeouts")
    void testDefaultQueryTimeout(final int variant, final Duration timeout, final int expected) throws SQLException {
        final Connection raw = mock(Connection.class);
        final CallableStatement statement = mock(CallableStatement.class);
        when(createStatement(raw, variant)).thenReturn(statement);
        final SQLException failure = new SQLException("Negative timeout");
        if (expected < 0) {
            doThrow(failure).when(statement).setQueryTimeout(expected);
        }
        try (DelegatingConnection<Connection> connection = new DelegatingConnection<>(raw)) {
            connection.setDefaultQueryTimeout(timeout);
            if (expected < 0) {
                assertSame(failure, assertThrows(SQLException.class, () -> createStatement(connection, variant)));
                verify(statement).close();
            } else {
                try (Statement ignored = createStatement(connection, variant)) {
                    assertFalse(ignored.isClosed());
                }
            }
            if (expected == 0) {
                verify(statement, never()).setQueryTimeout(anyInt());
            } else {
                verify(statement).setQueryTimeout(expected);
            }
        }
    }

    /**
     * Tests that a matching query timeout avoids a redundant setter call.
     *
     * @throws SQLException if JDBC setup, an operation, or cleanup unexpectedly fails.
     */
    @Test
    void testDefaultQueryTimeoutAvoidsRedundantSetter() throws SQLException {
        final Connection raw = mock(Connection.class);
        final Statement statement = mock(Statement.class);
        when(raw.createStatement()).thenReturn(statement);
        when(statement.getQueryTimeout()).thenReturn(1);
        try (DelegatingConnection<Connection> connection = new DelegatingConnection<>(raw)) {
            connection.setDefaultQueryTimeout(Duration.ofMillis(100));
            try (Statement ignored = connection.createStatement()) {
                verify(statement, never()).setQueryTimeout(anyInt());
            }
        }
    }

    /**
     * Tests that a timeout initialization failure retains a subsequent statement close failure.
     *
     * @throws SQLException if JDBC setup, an operation, or cleanup unexpectedly fails.
     */
    @Test
    void testDefaultQueryTimeoutPreservesInitializationAndCloseFailures() throws SQLException {
        final Connection raw = mock(Connection.class);
        final Statement statement = mock(Statement.class);
        when(raw.createStatement()).thenReturn(statement);
        final SQLException failure = new SQLException("Timeout unsupported");
        final SQLException closeFailure = new SQLException("Close failed");
        doThrow(failure).when(statement).setQueryTimeout(1);
        doThrow(closeFailure).when(statement).close();
        try (DelegatingConnection<Connection> connection = new DelegatingConnection<>(raw)) {
            connection.setDefaultQueryTimeout(Duration.ofMillis(100));
            assertSame(failure, assertThrows(SQLException.class, connection::createStatement));
            assertEquals(1, failure.getSuppressed().length);
        }
    }

    /**
     * Tests direct connection validation timeout conversion and preservation of driver exceptions.
     *
     * @param timeout the duration under test.
     * @param expected the expected timeout passed to JDBC, in seconds.
     * @throws SQLException if JDBC setup, an operation, or cleanup unexpectedly fails.
     */
    @ParameterizedTest
    @MethodSource("timeouts")
    void testIsValid(final Duration timeout, final int expected) throws SQLException {
        final Connection raw = mock(Connection.class);
        final SQLException failure = new SQLException("Negative timeout");
        if (expected < 0) {
            when(raw.isValid(expected)).thenThrow(failure);
        } else {
            when(raw.isValid(expected)).thenReturn(true);
        }
        try (DelegatingConnection<Connection> connection = new DelegatingConnection<>(raw)) {
            if (expected < 0) {
                assertSame(failure, assertThrows(SQLException.class, () -> connection.isValid(timeout)));
            } else {
                assertTrue(connection.isValid(timeout));
            }
            verify(raw).isValid(expected);
        }
    }

    /**
     * Tests that validating a closed wrapper does not call the driver.
     *
     * @throws SQLException if JDBC setup, an operation, or cleanup unexpectedly fails.
     */
    @Test
    void testIsValidClosedDoesNotCallDriver() throws SQLException {
        final Connection raw = mock(Connection.class);
        try (DelegatingConnection<Connection> connection = new DelegatingConnection<>(raw)) {
            connection.setClosedInternal(true);
            assertFalse(connection.isValid(Duration.ofMillis(100)));
            verify(raw, never()).isValid(anyInt());
        }
    }

    /**
     * Tests that a statement is discarded when its original query timeout cannot be restored.
     *
     * @param callable whether to exercise callable statements instead of prepared statements.
     * @throws Exception if setting up, using, or closing the test resources fails.
     */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testPoolDiscardsStatementWhenTimeoutResetFails(final boolean callable) throws Exception {
        final Connection raw = mock(Connection.class);
        final CallableStatement statement = mock(CallableStatement.class);
        when(raw.prepareStatement("SELECT 1")).thenReturn(statement);
        when(raw.prepareCall("SELECT 1")).thenReturn(statement);
        when(statement.getQueryTimeout()).thenReturn(5);
        try (PoolingConnection connection = new PoolingConnection(raw);
                GenericKeyedObjectPool<PStmtKey, DelegatingPreparedStatement> pool = new GenericKeyedObjectPool<>(connection)) {
            connection.setStatementPool(pool);
            final PreparedStatement borrowed = callable ? connection.prepareCall("SELECT 1") : connection.prepareStatement("SELECT 1");
            borrowed.setQueryTimeout(7);
            doThrow(new SQLException("Reset failed")).when(statement).setQueryTimeout(5);
            borrowed.close();
            assertEquals(0, pool.getNumIdle());
            verify(statement).close();
        }
    }

    /**
     * Tests duration conversion to bounded JDBC timeout seconds.
     *
     * @param timeout the duration under test.
     * @param expected the expected timeout passed to JDBC, in seconds.
     */
    @ParameterizedTest
    @MethodSource("timeouts")
    void testToJdbcSeconds(final Duration timeout, final int expected) {
        assertEquals(expected, Utils.toJdbcSeconds(timeout));
    }

    /**
     * Tests that JDBC timeout conversion rejects a null duration.
     */
    @Test
    void testToJdbcSecondsNull() {
        assertThrows(NullPointerException.class, () -> Utils.toJdbcSeconds(null));
    }

    /**
     * Tests that closing a connection restores the timeout of a statement left open by its borrower.
     *
     * @param callable whether to exercise callable statements instead of prepared statements.
     * @throws Exception if setting up, using, or closing the test resources fails.
     */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testConnectionCloseRestoresUnclosedStatementTimeout(final boolean callable) throws Exception {
        try (BasicDataSource source = newDataSource(null)) {
            try (Connection connection = source.getConnection()) {
                final PreparedStatement statement = callable ? connection.prepareCall("SELECT 1") : connection.prepareStatement("SELECT 1");
                statement.setQueryTimeout(7);
            }
            try (Connection connection = source.getConnection();
                    PreparedStatement statement = callable ? connection.prepareCall("SELECT 1") : connection.prepareStatement("SELECT 1")) {
                assertEquals(1000, statement.getQueryTimeout());
            }
        }
    }

    /**
     * Tests that an unchanged pooled statement does not access the driver query timeout.
     *
     * @param callable whether to exercise callable statements instead of prepared statements.
     * @throws Exception if setting up, using, or closing the test resources fails.
     */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testUntouchedPooledStatementDoesNotReadOrWriteTimeout(final boolean callable) throws Exception {
        final Connection raw = mock(Connection.class);
        final CallableStatement statement = mock(CallableStatement.class);
        when(raw.prepareStatement("SELECT 1")).thenReturn(statement);
        when(raw.prepareCall("SELECT 1")).thenReturn(statement);
        try (PoolingConnection connection = new PoolingConnection(raw);
                GenericKeyedObjectPool<PStmtKey, DelegatingPreparedStatement> pool = new GenericKeyedObjectPool<>(connection)) {
            connection.setStatementPool(pool);
            for (int i = 0; i < 2; i++) {
                try (PreparedStatement borrowed = callable ? connection.prepareCall("SELECT 1") : connection.prepareStatement("SELECT 1")) {
                    assertFalse(borrowed.isClosed());
                }
            }
            verify(statement, never()).getQueryTimeout();
            verify(statement, never()).setQueryTimeout(anyInt());
        }
    }

    /**
     * Tests that setting a nonpooled statement timeout does not require driver support for reading it.
     *
     * @param callable whether to exercise callable statements instead of prepared statements.
     * @throws SQLException if JDBC setup, an operation, or cleanup unexpectedly fails.
     */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testNonPooledQueryTimeoutDoesNotRequireGetter(final boolean callable) throws SQLException {
        final CallableStatement raw = mock(CallableStatement.class);
        when(raw.getQueryTimeout()).thenThrow(new SQLFeatureNotSupportedException("Getter unavailable"));
        try (PreparedStatement statement = callable ? new DelegatingCallableStatement(null, raw) : new DelegatingPreparedStatement(null, raw)) {
            statement.setQueryTimeout(7);
            verify(raw).setQueryTimeout(7);
            verify(raw, never()).getQueryTimeout();
        }
    }

    /**
     * Tests restoration after a driver changes the timeout and then throws from the setter.
     *
     * @param callable whether to exercise callable statements instead of prepared statements.
     * @param unchecked whether the timeout setter throws an unchecked exception.
     * @throws Exception if setting up, using, or closing the test resources fails.
     */
    @ParameterizedTest
    @MethodSource("failedTimeoutSetters")
    void testPooledTimeoutRestoredAfterSetterMutatesAndThrows(final boolean callable, final boolean unchecked) throws Exception {
        final Connection raw = mock(Connection.class);
        final CallableStatement statement = mock(CallableStatement.class);
        final AtomicInteger timeout = new AtomicInteger(5);
        final Exception failure = unchecked ? new IllegalStateException("Timeout changed before failure")
                : new SQLException("Timeout changed before failure");
        when(raw.prepareStatement("SELECT 1")).thenReturn(statement);
        when(raw.prepareCall("SELECT 1")).thenReturn(statement);
        when(statement.getQueryTimeout()).thenAnswer(invocation -> timeout.get());
        doAnswer(invocation -> {
            final int seconds = invocation.getArgument(0);
            timeout.set(seconds);
            if (seconds == 7) {
                throw failure;
            }
            return null;
        }).when(statement).setQueryTimeout(anyInt());
        try (PoolingConnection connection = new PoolingConnection(raw);
                GenericKeyedObjectPool<PStmtKey, DelegatingPreparedStatement> pool = new GenericKeyedObjectPool<>(connection)) {
            connection.setStatementPool(pool);
            try (PreparedStatement borrowed = callable ? connection.prepareCall("SELECT 1") : connection.prepareStatement("SELECT 1")) {
                assertSame(failure, assertThrows(failure.getClass(), () -> borrowed.setQueryTimeout(7)));
                assertEquals(7, timeout.get());
            }
            assertEquals(5, timeout.get());
            assertEquals(1, pool.getNumIdle());
            try (PreparedStatement borrowed = callable ? connection.prepareCall("SELECT 1") : connection.prepareStatement("SELECT 1")) {
                assertEquals(5, borrowed.getQueryTimeout());
            }
        }
    }

    /**
     * Tests that timeout restoration uses the replacement delegate's original value.
     *
     * @param callable whether to exercise callable statements instead of prepared statements.
     * @param changeTimeout whether to change the replacement delegate's timeout before returning the statement.
     * @throws Exception if setting up, using, or closing the test resources fails.
     */
    @ParameterizedTest
    @MethodSource("changedDelegates")
    void testPooledTimeoutFollowsReplacementDelegate(final boolean callable, final boolean changeTimeout) throws Exception {
        final Connection raw = mock(Connection.class);
        final AtomicInteger originalTimeout = new AtomicInteger(5);
        final AtomicInteger replacementTimeout = new AtomicInteger(9);
        final CallableStatement original = mockStatement(originalTimeout);
        final CallableStatement replacement = mockStatement(replacementTimeout);
        when(raw.prepareStatement("SELECT 1")).thenReturn(original);
        when(raw.prepareCall("SELECT 1")).thenReturn(original);
        try (PoolingConnection connection = new PoolingConnection(raw);
                GenericKeyedObjectPool<PStmtKey, DelegatingPreparedStatement> pool = new GenericKeyedObjectPool<>(connection)) {
            connection.setStatementPool(pool);
            try (DelegatingPreparedStatement borrowed = (DelegatingPreparedStatement)
                    (callable ? connection.prepareCall("SELECT 1") : connection.prepareStatement("SELECT 1"))) {
                borrowed.setQueryTimeout(7);
                borrowed.setDelegate(replacement);
                if (changeTimeout) {
                    borrowed.setQueryTimeout(11);
                }
            }
            assertEquals(9, replacementTimeout.get());
            try (PreparedStatement borrowed = callable ? connection.prepareCall("SELECT 1") : connection.prepareStatement("SELECT 1")) {
                assertEquals(9, borrowed.getQueryTimeout());
            }
        } finally {
            original.close();
        }
    }

    /**
     * Tests that reassigning the same delegate retains the saved original timeout.
     *
     * @param callable whether to exercise callable statements instead of prepared statements.
     * @throws Exception if setting up, using, or closing the test resources fails.
     */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testPooledTimeoutPreservedWhenReassigningSameDelegate(final boolean callable) throws Exception {
        final Connection raw = mock(Connection.class);
        final AtomicInteger timeout = new AtomicInteger(5);
        final CallableStatement statement = mockStatement(timeout);
        when(raw.prepareStatement("SELECT 1")).thenReturn(statement);
        when(raw.prepareCall("SELECT 1")).thenReturn(statement);
        try (PoolingConnection connection = new PoolingConnection(raw);
                GenericKeyedObjectPool<PStmtKey, DelegatingPreparedStatement> pool = new GenericKeyedObjectPool<>(connection)) {
            connection.setStatementPool(pool);
            try (DelegatingPreparedStatement borrowed = (DelegatingPreparedStatement)
                    (callable ? connection.prepareCall("SELECT 1") : connection.prepareStatement("SELECT 1"))) {
                borrowed.setQueryTimeout(7);
                borrowed.setDelegate(statement);
            }
            assertEquals(5, timeout.get());
        }
    }

}
