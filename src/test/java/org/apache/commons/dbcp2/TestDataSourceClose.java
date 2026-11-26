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

import static org.apache.commons.dbcp2.ConcurrentCloseTestSupport.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import javax.naming.spi.ObjectFactory;
import javax.sql.PooledConnection;

import org.apache.commons.dbcp2.ConcurrentCloseTestSupport.Task;
import org.apache.commons.dbcp2.cpdsadapter.DriverAdapterCPDS;
import org.apache.commons.dbcp2.datasources.InstanceKeyDataSource;
import org.apache.commons.dbcp2.datasources.PerUserPoolDataSource;
import org.apache.commons.dbcp2.datasources.PerUserPoolDataSourceFactory;
import org.apache.commons.dbcp2.datasources.SharedPoolDataSource;
import org.apache.commons.dbcp2.datasources.SharedPoolDataSourceFactory;
import org.apache.commons.lang3.SerializationUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests terminal data-source shutdown, including initialization and password-recovery races.
 */
class TestDataSourceClose {

    /**
     * Data-source lifecycle states in which shutdown must prevent new requests.
     */
    private enum State {
        UNCONFIGURED, CONFIGURED, INITIALIZED
    }

    /**
     * Records physical connections and optionally pauses or fails an individual creation attempt.
     */
    private static final class TrackingCPDS extends DriverAdapterCPDS {
        private final AtomicInteger attempts = new AtomicInteger();
        private final List<PooledConnection> connections = new CopyOnWriteArrayList<>();
        private final CountDownLatch entered = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);
        private volatile int pauseAt;
        private volatile int failAt;

        /**
         * Uses independent in-memory databases so tests do not share credentials or state.
         */
        TrackingCPDS() throws ClassNotFoundException {
            setDriver("org.h2.Driver");
            setUrl("jdbc:h2:mem:");
        }

        /**
         * Asserts physical shutdown, including the connection used to validate the provider.
         */
        void assertClosed() {
            assertFalse(connections.isEmpty());
            connections.forEach(connection -> assertThrows(SQLException.class, connection::getConnection));
        }

        @Override
        public PooledConnection getPooledConnection(final String user, final String password) throws SQLException {
            final int attempt = attempts.incrementAndGet();
            if (attempt == pauseAt) {
                entered.countDown();
                await(release);
            }
            if (attempt == failAt) {
                throw new SQLException("Simulated creation failure during password change");
            }
            final PooledConnection connection = super.getPooledConnection(user, password);
            connections.add(connection);
            return connection;
        }
    }

    /**
     * Checks that shutdown cannot be undone through either borrowing overload.
     */
    private static void assertRejectsBorrowing(final InstanceKeyDataSource dataSource, final TrackingCPDS cpds) {
        final int attempts = cpds.attempts.get();
        assertThrows(SQLException.class, dataSource::getConnection);
        assertThrows(SQLException.class, () -> dataSource.getConnection("sa", ""));
        assertEquals(attempts, cpds.attempts.get(), "A closed data source contacted its provider");
    }

    /**
     * Combines each data-source implementation, lifecycle state, and getConnection overload.
     */
    static Stream<Arguments> closeCases() {
        return Stream.of(false, true).flatMap(shared -> Stream.of(State.values())
                .flatMap(state -> Stream.of(false, true).map(credentials -> Arguments.of(shared, state, credentials))));
    }

    /**
     * Configures a small pool without changing the close behavior under test.
     */
    private static void configure(final InstanceKeyDataSource dataSource, final TrackingCPDS cpds) {
        dataSource.setConnectionPoolDataSource(cpds);
        dataSource.setDefaultMaxTotal(1);
        dataSource.setDefaultMaxWait(Duration.ofSeconds(5));
    }

    /**
     * Creates an unconfigured data source.
     */
    private static InstanceKeyDataSource create(final boolean shared) {
        return shared ? new SharedPoolDataSource() : new PerUserPoolDataSource();
    }

    /**
     * Combines both implementations with initialization and post-registration physical creation.
     */
    static Stream<Arguments> creationCases() {
        return Stream.of(false, true).flatMap(shared -> Stream.of(1, 2).map(attempt -> Arguments.of(shared, attempt)));
    }

    /**
     * Accepts either outcome of an in-flight borrow, but returns any acquired connection before checking shutdown.
     */
    private static void finishBorrow(final Task borrower, final AtomicReference<Connection> connection) throws Exception {
        final Throwable failure = borrower.awaitFailure();
        if (failure != null) {
            assertInstanceOf(SQLException.class, failure);
        } else {
            assertNotNull(connection.get());
            connection.get().close();
        }
    }

    /**
     * Combines shared-pool shutdown through either instance, before or after initial pool creation.
     */
    static Stream<Arguments> serializationCases() {
        return Stream.of(false, true).flatMap(shared -> Stream.of(false, true)
                .flatMap(initialized -> Stream.of(false, true).map(closeCopy -> Arguments.of(shared, initialized, closeCopy))));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testBorrowWaitingToInitializeRechecksClosedState(final boolean shared) throws Exception {
        final TrackingCPDS cpds = new TrackingCPDS();
        try (InstanceKeyDataSource dataSource = create(shared)) {
            configure(dataSource, cpds);
            Task borrower = null;
            try {
                synchronized (dataSource) {
                    borrower = new Task(() -> dataSource.getConnection().close());
                    borrower.awaitBlocked();
                    dataSource.close();
                }
                assertInstanceOf(SQLException.class, borrower.awaitFailure());
            } finally {
                if (borrower != null) {
                    borrower.close();
                }
            }
            assertEquals(0, cpds.attempts.get());
        }
    }

    @Test
    void testClearDoesNotCloseDataSource() throws Exception {
        final TrackingCPDS cpds = new TrackingCPDS();
        try (PerUserPoolDataSource dataSource = new PerUserPoolDataSource()) {
            configure(dataSource, cpds);
            dataSource.getConnection().close();
            dataSource.clear();
            cpds.assertClosed();
            try (Connection connection = dataSource.getConnection()) {
                assertTrue(connection.isValid(1));
            }
        }
        cpds.assertClosed();
    }

    @Test
    void testCloseAllUserPools() throws Exception {
        final TrackingCPDS cpds = new TrackingCPDS();
        try (PerUserPoolDataSource dataSource = new PerUserPoolDataSource()) {
            configure(dataSource, cpds);
            dataSource.getConnection("first", "").close();
            dataSource.getConnection("second", "").close();
            try (Connection active = dataSource.getConnection("third", "")) {
                dataSource.close();
                assertTrue(active.isValid(1));
                assertThrows(SQLException.class, () -> dataSource.getConnection("first", ""));
                assertThrows(SQLException.class, () -> dataSource.getConnection("fourth", ""));
            }
            cpds.assertClosed();
        }
    }

    @ParameterizedTest
    @MethodSource("serializationCases")
    void testClosedPoolIsNotRecreatedThroughDeserializedDataSource(final boolean shared, final boolean initialized, final boolean closeCopy) throws Exception {
        final DriverAdapterCPDS cpds = new DriverAdapterCPDS();
        cpds.setDriver("org.h2.Driver");
        cpds.setUrl("jdbc:h2:mem:");
        try (InstanceKeyDataSource dataSource = create(shared)) {
            dataSource.setConnectionPoolDataSource(cpds);
            if (initialized) {
                dataSource.getConnection().close();
            }
            try (InstanceKeyDataSource copy = SerializationUtils.clone(dataSource)) {
                copy.getConnection().close();
                assertEquals(1, shared ? ((SharedPoolDataSource) dataSource).getNumIdle() : ((PerUserPoolDataSource) dataSource).getNumIdle());
                if (closeCopy) {
                    copy.close();
                } else {
                    dataSource.close();
                }
                assertEquals(0, shared ? ((SharedPoolDataSource) dataSource).getNumIdle() : ((PerUserPoolDataSource) dataSource).getNumIdle());
                final ObjectFactory factory = shared ? new SharedPoolDataSourceFactory() : new PerUserPoolDataSourceFactory();
                assertNull(factory.getObjectInstance(dataSource.getReference(), null, null, null));
                assertThrows(SQLException.class, dataSource::getConnection);
                assertThrows(SQLException.class, copy::getConnection);
                assertThrows(SQLException.class, () -> dataSource.getConnection("new-user", ""));
                assertThrows(SQLException.class, () -> copy.getConnection("new-user", ""));
            }
        }
    }

    @ParameterizedTest
    @MethodSource("creationCases")
    void testCloseDuringConnectionCreation(final boolean shared, final int pauseAt) throws Exception {
        final TrackingCPDS cpds = new TrackingCPDS();
        cpds.pauseAt = pauseAt;
        try (InstanceKeyDataSource dataSource = create(shared)) {
            configure(dataSource, cpds);
            final AtomicReference<Connection> connection = new AtomicReference<>();
            try (Task borrower = new Task(() -> connection.set(dataSource.getConnection()))) {
                await(cpds.entered);
                try (Task closer = new Task(dataSource::close)) {
                    try {
                        if (pauseAt == 1) {
                            // Initialization owns the data-source monitor until the new pool is published.
                            closer.awaitBlocked();
                        } else {
                            // Ordinary physical connection creation must not prevent shutdown of a published pool.
                            closer.awaitSuccess();
                        }
                    } finally {
                        cpds.release.countDown();
                    }
                    finishBorrow(borrower, connection);
                    closer.awaitSuccess();
                }
                cpds.assertClosed();
                assertRejectsBorrowing(dataSource, cpds);
            } finally {
                cpds.release.countDown();
            }
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {4, 5})
    void testCloseDuringPasswordRecovery(final int pauseAt) throws Exception {
        final TrackingCPDS cpds = new TrackingCPDS();
        try (PerUserPoolDataSource dataSource = new PerUserPoolDataSource()) {
            configure(dataSource, cpds);
            dataSource.setDefaultMaxIdle(0);
            dataSource.getConnection().close();
            assertEquals(2, cpds.attempts.get());
            cpds.failAt = 3;
            cpds.pauseAt = pauseAt;
            final AtomicReference<Connection> connection = new AtomicReference<>();
            try (Task borrower = new Task(() -> connection.set(dataSource.getConnection()))) {
                await(cpds.entered);
                try (Task closer = new Task(dataSource::close)) {
                    try {
                        if (pauseAt == 4) {
                            // The recovery probe runs outside the initialization monitor.
                            closer.awaitSuccess();
                        } else {
                            // Replacement pool registration must be coordinated with shutdown too.
                            closer.awaitBlocked();
                        }
                    } finally {
                        cpds.release.countDown();
                    }
                    finishBorrow(borrower, connection);
                    closer.awaitSuccess();
                }
                cpds.assertClosed();
                assertRejectsBorrowing(dataSource, cpds);
                if (pauseAt == 4) {
                    assertEquals(4, cpds.attempts.get(), "Shutdown allowed replacement pool registration");
                }
            } finally {
                cpds.release.countDown();
            }
        }
    }

    @ParameterizedTest
    @MethodSource("closeCases")
    void testCloseIsTerminal(final boolean shared, final State state, final boolean credentials) throws Exception {
        final TrackingCPDS cpds = new TrackingCPDS();
        try (InstanceKeyDataSource dataSource = create(shared)) {
            if (state != State.UNCONFIGURED) {
                configure(dataSource, cpds);
            }
            if (state == State.INITIALIZED) {
                try (Connection connection = credentials ? dataSource.getConnection("sa", "") : dataSource.getConnection()) {
                    assertFalse(connection.isClosed());
                }
            }
            dataSource.close();
            dataSource.close();
            assertRejectsBorrowing(dataSource, cpds);
            if (state == State.INITIALIZED) {
                cpds.assertClosed();
            } else {
                assertEquals(0, cpds.attempts.get());
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testCloseWakesWaitersAndPreservesBorrowedConnection(final boolean shared) throws Exception {
        final TrackingCPDS cpds = new TrackingCPDS();
        try (InstanceKeyDataSource dataSource = create(shared)) {
            configure(dataSource, cpds);
            dataSource.setDefaultMaxWait(Duration.ofMillis(-1));
            try (Connection connection = dataSource.getConnection(); Task waiting = new Task(() -> {
                try (Connection unexpected = dataSource.getConnection()) {
                    throw new AssertionError("Unexpected second connection: " + unexpected);
                }
            })) {
                waiting.awaitBlocked();
                dataSource.close();
                assertInstanceOf(SQLException.class, waiting.awaitFailure());
                assertFalse(connection.isClosed());
                assertTrue(connection.isValid(1));
                assertRejectsBorrowing(dataSource, cpds);
            }
            cpds.assertClosed();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testDeserializedDataSourceSharesInitializationMonitor(final boolean shared) throws Exception {
        final DriverAdapterCPDS cpds = new DriverAdapterCPDS();
        cpds.setDriver("org.h2.Driver");
        cpds.setUrl("jdbc:h2:mem:");
        try (InstanceKeyDataSource dataSource = create(shared)) {
            dataSource.setConnectionPoolDataSource(cpds);
            try (InstanceKeyDataSource copy = SerializationUtils.clone(dataSource)) {
                Task borrower = null;
                try {
                    synchronized (dataSource) {
                        borrower = new Task(() -> copy.getConnection().close());
                        borrower.awaitBlocked();
                        copy.close();
                    }
                    assertInstanceOf(SQLException.class, borrower.awaitFailure());
                    assertThrows(SQLException.class, dataSource::getConnection);
                } finally {
                    if (borrower != null) {
                        borrower.close();
                    }
                }
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testPasswordChangeThroughDeserializedDataSource(final boolean shared) throws Exception {
        final String url = "jdbc:h2:mem:" + UUID.randomUUID();
        final DriverAdapterCPDS cpds = new DriverAdapterCPDS();
        cpds.setDriver("org.h2.Driver");
        cpds.setUrl(url);
        // Keep the database alive while password recovery replaces its pooled connections.
        try (Connection anchor = DriverManager.getConnection(url, "sa", "before"); InstanceKeyDataSource dataSource = create(shared)) {
            dataSource.setConnectionPoolDataSource(cpds);
            dataSource.setDefaultMaxTotal(1);
            dataSource.setDefaultBlockWhenExhausted(false);
            dataSource.getConnection("sa", "before").close();
            try (InstanceKeyDataSource copy = SerializationUtils.clone(dataSource)) {
                try (Statement statement = anchor.createStatement()) {
                    statement.execute("ALTER USER SA SET PASSWORD 'after'");
                }
                try (Connection connection = copy.getConnection("sa", "after")) {
                    assertTrue(connection.isValid(1));
                    assertEquals(1, shared ? ((SharedPoolDataSource) dataSource).getNumActive() : ((PerUserPoolDataSource) dataSource).getNumActive("sa"));
                    assertEquals(1, shared ? ((SharedPoolDataSource) copy).getNumActive() : ((PerUserPoolDataSource) copy).getNumActive("sa"));
                }
                try (Connection connection = dataSource.getConnection("sa", "after")) {
                    assertTrue(connection.isValid(1));
                }
                assertThrows(SQLException.class, () -> copy.getConnection("sa", "before"));
                copy.close();
                assertThrows(SQLException.class, () -> dataSource.getConnection("sa", "after"));
            }
        }
    }

    @Test
    void testPasswordRecoveryWhileOpen() throws Exception {
        final TrackingCPDS cpds = new TrackingCPDS();
        try (PerUserPoolDataSource dataSource = new PerUserPoolDataSource()) {
            configure(dataSource, cpds);
            dataSource.setDefaultMaxIdle(0);
            dataSource.getConnection().close();
            cpds.failAt = 3;
            try (Connection connection = dataSource.getConnection()) {
                assertTrue(connection.isValid(1));
                assertEquals(6, cpds.attempts.get());
            }
        }
        cpds.assertClosed();
    }

    @ParameterizedTest
    @MethodSource("serializationCases")
    void testRepeatedClosePreservesNewRegistration(final boolean shared, final boolean initialized, final boolean closeCopy) throws Exception {
        final DriverAdapterCPDS cpds = new DriverAdapterCPDS();
        cpds.setDriver("org.h2.Driver");
        cpds.setUrl("jdbc:h2:mem:");
        final ObjectFactory factory = shared ? new SharedPoolDataSourceFactory() : new PerUserPoolDataSourceFactory();
        try (InstanceKeyDataSource first = create(shared)) {
            first.setConnectionPoolDataSource(cpds);
            if (initialized) {
                first.getConnection().close();
            }
            try (InstanceKeyDataSource copy = SerializationUtils.clone(first)) {
                first.close();
                try (InstanceKeyDataSource next = create(shared)) {
                    next.setConnectionPoolDataSource(cpds);
                    assertEquals(first.getReference().get("instanceKey").getContent(), next.getReference().get("instanceKey").getContent());
                    assertSame(next, factory.getObjectInstance(next.getReference(), null, null, null));
                    if (closeCopy) {
                        copy.close();
                    } else {
                        first.close();
                    }
                    assertSame(next, factory.getObjectInstance(next.getReference(), null, null, null));
                    try (Connection connection = next.getConnection()) {
                        assertTrue(connection.isValid(1));
                    }
                }
            }
        }
    }
}
