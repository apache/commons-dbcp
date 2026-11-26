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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests termination and identity-based traversal when invalidating wrapped connections.
 */
@ParameterizedClass(name = "{0}")
@EnumSource(value = TestDatabase.class, names = {"H2", "HSQLDB"})
class TestBasicDataSourceInvalidation {

    /** Database used by this invocation. */
    @Parameter
    TestDatabase database;

    /** Distinct instances compare equal, but must remain distinct nodes in a delegate chain. */
    private static final class EqualConnection extends DelegatingConnection<Connection> {
        private EqualConnection(final Connection connection) {
            super(connection);
        }

        @Override
        public boolean equals(final Object obj) {
            return obj instanceof EqualConnection;
        }

        @Override
        public int hashCode() {
            return 1;
        }
    }

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private static Stream<Arguments> acyclicArguments() {
        return Stream.of(0, 1, 2, 16, 256).flatMap(length -> Stream.of(false, true).map(external -> Arguments.of(length, external)));
    }
    private static Stream<Arguments> cyclicArguments() {
        return Stream.of(0, 1, 8).flatMap(prefixLength -> Stream.of(1, 2, 3, 16).flatMap(cycleLength ->
            Stream.of(false, true).flatMap(external -> Stream.of(false, true).map(bridged -> Arguments.of(prefixLength, cycleLength, external, bridged)))));
    }

    private static Connection externalWrapper(final Connection delegate) {
        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[] {Connection.class},
                (proxy, method, args) -> {
                    try {
                        return method.invoke(delegate, args);
                    } catch (final InvocationTargetException e) {
                        throw e.getCause();
                    }
                });
    }

    private BasicDataSource dataSource;

    private Connection pooledConnection;

    private void assertCyclicChain(final int prefixLength, final int cycleLength, final boolean external, final boolean bridged) throws SQLException {
        final List<DelegatingConnection<Connection>> nodes = new ArrayList<>();
        for (int i = 0; i < prefixLength + cycleLength; i++) {
            nodes.add(new DelegatingConnection<>(null));
        }
        for (int i = 0; i < nodes.size() - 1; i++) {
            final Connection next = nodes.get(i + 1);
            nodes.get(i).setDelegate(bridged ? externalWrapper(next) : next);
        }
        final Connection cycleStart = nodes.get(prefixLength);
        nodes.get(nodes.size() - 1).setDelegate(bridged ? externalWrapper(cycleStart) : cycleStart);
        final Connection input = external ? externalWrapper(nodes.get(0)) : nodes.get(0);
        try {
            final IllegalStateException exception = assertTimeoutPreemptively(TIMEOUT,
                    () -> assertThrows(IllegalStateException.class, () -> dataSource.invalidateConnection(input)));
            final SQLException cause = assertInstanceOf(SQLException.class, exception.getCause());
            assertEquals("Cyclic connection delegate chain.", cause.getMessage());
            assertEquals(1, dataSource.getNumActive());
            assertFalse(pooledConnection.isClosed());
            assertTrue(pooledConnection.isValid(1));
        } finally {
            // Break all links even after a timeout, so a regressed traversal can terminate and cleanup cannot recurse around a cycle.
            nodes.forEach(node -> node.setDelegate(null));
            for (final DelegatingConnection<Connection> node : nodes) {
                node.close();
            }
        }
    }

    @BeforeEach
    void setUp() throws SQLException {
        dataSource = new BasicDataSource();
        dataSource.setDriver(database.getDriver());
        dataSource.setUrl(database.newUrl());
        pooledConnection = dataSource.getConnection();
    }

    @AfterEach
    void tearDown() throws SQLException {
        try {
            pooledConnection.close();
        } finally {
            dataSource.close();
        }
    }

    @ParameterizedTest(name = "length={0}, external={1}")
    @MethodSource("acyclicArguments")
    void testInvalidateAcyclicEqualWrappers(final int length, final boolean external) throws SQLException {
        Connection head = pooledConnection;
        for (int i = 0; i < length; i++) {
            head = new EqualConnection(head);
        }
        final Connection input = external ? externalWrapper(head) : head;
        assertTimeoutPreemptively(TIMEOUT, () -> dataSource.invalidateConnection(input));
        assertEquals(0, dataSource.getNumActive());
        assertTrue(pooledConnection.isClosed());
    }

    @ParameterizedTest(name = "prefix={0}, cycle={1}, external={2}, bridged={3}")
    @MethodSource("cyclicArguments")
    void testInvalidateCyclicChain(final int prefixLength, final int cycleLength, final boolean external, final boolean bridged) throws SQLException {
        assertCyclicChain(prefixLength, cycleLength, external, bridged);
    }

    @Test
    void testInvalidateExternalDirectPoolableWrapper() throws SQLException {
        final Connection poolable = ((DelegatingConnection<?>) pooledConnection).getDelegateInternal();
        final Connection external = (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[] {Connection.class},
                (proxy, method, args) -> {
                    if ("isWrapperFor".equals(method.getName())) {
                        assertSame(DelegatingConnection.class, args[0]);
                        return false;
                    }
                    if ("unwrap".equals(method.getName())) {
                        assertSame(PoolableConnection.class, args[0]);
                        return poolable;
                    }
                    throw new AssertionError("Unexpected call: " + method.getName());
                });
        dataSource.invalidateConnection(external);
        assertEquals(0, dataSource.getNumActive());
        assertTrue(pooledConnection.isClosed());
    }

    @ParameterizedTest
    @ValueSource(strings = {"isWrapperFor", "unwrapDelegating", "unwrapPoolable"})
    void testInvalidateExternalFailure(final String operation) throws SQLException {
        final SQLException failure = new SQLException("Wrapper failed", "HY000", 42);
        final boolean wrapsDelegating = !"unwrapPoolable".equals(operation);
        final Connection external = (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[] {Connection.class},
                (proxy, method, args) -> {
                    if ("isWrapperFor".equals(method.getName())) {
                        assertSame(DelegatingConnection.class, args[0]);
                        if ("isWrapperFor".equals(operation)) {
                            throw failure;
                        }
                        return wrapsDelegating;
                    }
                    if ("unwrap".equals(method.getName())) {
                        assertSame(wrapsDelegating ? DelegatingConnection.class : PoolableConnection.class, args[0]);
                        throw failure;
                    }
                    throw new AssertionError("Unexpected call: " + method.getName());
                });
        final IllegalStateException exception = assertThrows(IllegalStateException.class, () -> dataSource.invalidateConnection(external));
        assertEquals("Cannot invalidate connection: Unwrapping poolable connection failed.", exception.getMessage());
        assertSame(failure, exception.getCause());
        assertEquals(1, dataSource.getNumActive());
        assertTrue(pooledConnection.isValid(1));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testInvalidateExternalNullDelegate(final boolean wrapsDelegating) throws SQLException {
        final Connection external = (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[] {Connection.class},
                (proxy, method, args) -> {
                    if ("isWrapperFor".equals(method.getName())) {
                        assertSame(DelegatingConnection.class, args[0]);
                        return wrapsDelegating;
                    }
                    if ("unwrap".equals(method.getName())) {
                        assertSame(wrapsDelegating ? DelegatingConnection.class : PoolableConnection.class, args[0]);
                        return null;
                    }
                    throw new AssertionError("Unexpected call: " + method.getName());
                });
        final IllegalStateException exception = assertThrows(IllegalStateException.class, () -> dataSource.invalidateConnection(external));
        assertEquals("Cannot invalidate connection: Connection is not a poolable connection.", exception.getMessage());
        assertEquals(1, dataSource.getNumActive());
        assertTrue(pooledConnection.isValid(1));
    }

    @Test
    void testInvalidateInteriorCycle() throws SQLException {
        // A -> B -> C -> B never returns to A and has no self-loop.
        assertCyclicChain(1, 2, false, false);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 16})
    void testInvalidateInterleavedWrappers(final int length) throws SQLException {
        Connection head = pooledConnection;
        for (int i = 0; i < length; i++) {
            head = new EqualConnection(externalWrapper(head));
        }
        final Connection input = head;
        assertTimeoutPreemptively(TIMEOUT, () -> dataSource.invalidateConnection(input));
        assertEquals(0, dataSource.getNumActive());
        assertTrue(pooledConnection.isClosed());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 16})
    void testInvalidateNullTerminatedChain(final int length) {
        Connection head = null;
        for (int i = 0; i < length; i++) {
            head = new EqualConnection(head);
        }
        final Connection input = head;
        final IllegalStateException exception = assertTimeoutPreemptively(TIMEOUT,
                () -> assertThrows(IllegalStateException.class, () -> dataSource.invalidateConnection(input)));
        assertEquals("Cannot invalidate connection: Connection is not a poolable connection.", exception.getMessage());
        assertEquals(1, dataSource.getNumActive());
    }
}
