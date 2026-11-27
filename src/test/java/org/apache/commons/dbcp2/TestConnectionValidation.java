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
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import javax.management.ObjectName;

import org.apache.commons.dbcp2.managed.BasicManagedDataSource;
import org.apache.commons.dbcp2.managed.LocalXAConnectionFactory;
import org.apache.commons.dbcp2.managed.PoolableManagedConnectionFactory;
import org.apache.commons.dbcp2.managed.TransactionRegistry;
import org.apache.commons.pool2.DestroyMode;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.geronimo.transaction.manager.TransactionManagerImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.function.ThrowingConsumer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Exercises validation ownership with scripted failures and the same recovery assertions used by database integration tests. */
@Timeout(30)
class TestConnectionValidation {

    /** Failure to inject after the connection has entered its intended lifecycle phase. */
    private enum Fault {
        NONE, CLOSED, STATE_EXCEPTION, INVALID, VALID_EXCEPTION, PREPARE_EXCEPTION, EXECUTE_EXCEPTION, NO_ROWS, DIES_IN_VALIDATION
    }

    /** Records destruction and invalidation independently of the pool's own counters. */
    private static final class Fixture implements AutoCloseable {
        /** Physical connections created by this fixture. */
        private final List<ScriptedConnection> physical = new ArrayList<>();
        /** Destruction calls per pooled connection. */
        private final Map<PoolableConnection, Integer> destroys = new IdentityHashMap<>();
        /** Factory under test. */
        private final PoolableConnectionFactory factory;
        /** Pool with independently observed invalidations. */
        private final GenericObjectPool<PoolableConnection> pool;
        /** Number of explicit pool invalidations. */
        private int invalidations;
        /** Whether destruction throws after releasing resources. */
        private boolean failDestroy;
        /** Failure armed immediately after the next object is created. */
        private Fault makeFault = Fault.NONE;

        /** Creates a single-connection pool with the requested validation and statement-pooling settings. */
        private Fixture(final String query, final boolean statements) {
            factory = new PoolableConnectionFactory(() -> {
                final ScriptedConnection connection = new ScriptedConnection();
                physical.add(connection);
                return connection;
            }, null) {
                @Override
                public void destroyObject(final PooledObject<PoolableConnection> p, final DestroyMode mode) throws SQLException {
                    destroys.merge(p.getObject(), 1, Integer::sum);
                    super.destroyObject(p, mode);
                    if (failDestroy) {
                        throw new SQLException("Destruction failed");
                    }
                }

                @Override
                public PooledObject<PoolableConnection> makeObject() throws SQLException {
                    final PooledObject<PoolableConnection> result = super.makeObject();
                    latest().arm(makeFault, 0);
                    makeFault = Fault.NONE;
                    return result;
                }
            };
            factory.setValidationQuery(query);
            factory.setPoolStatements(statements);
            pool = new GenericObjectPool<PoolableConnection>(factory) {
                @Override
                public void invalidateObject(final PoolableConnection connection, final DestroyMode mode) throws Exception {
                    invalidations++;
                    super.invalidateObject(connection, mode);
                }
            };
            factory.setPool(pool);
            pool.setMaxTotal(1);
            pool.setBlockWhenExhausted(false);
        }

        /** Checks recovery without relying solely on private capacity accounting. */
        private void assertReplacement(final PoolableConnection dead) throws Exception {
            try (PoolableConnection replacement = pool.borrowObject()) {
                assertNotSame(dead, replacement);
                assertTrue(replacement.isValid(1));
                assertThrows(NoSuchElementException.class, pool::borrowObject);
                assertEquals(Integer.valueOf(1), destroys.get(dead));
                assertEquals(0, invalidations, "Validation must not invalidate objects itself");
                assertAccounting(pool);
            }
            assertAccounting(pool);
        }

        @Override
        public void close() {
            pool.close();
        }

        /** Gets the most recently created physical connection. */
        private ScriptedConnection latest() {
            return physical.get(physical.size() - 1);
        }
    }

    /** A JDBC connection with deterministic state transitions and validation failures. */
    private static final class ScriptedConnection extends TesterConnection {
        /** Current scripted failure. */
        private Fault fault = Fault.NONE;
        /** Successful closure checks remaining before the failure. */
        private int openChecks;
        /** Number of JDBC validation calls. */
        private int validations;
        /** Validation statements retained for cleanup assertions. */
        private final List<PreparedStatement> statements = new ArrayList<>();
        /** Stable exception instance for propagation assertions. */
        private final SQLException exception = new SQLException("Validation failed", "08006");
        /** Optional signal that JDBC validation has started. */
        private CountDownLatch entered;
        /** Optional gate controlling JDBC validation completion. */
        private CountDownLatch release;

        /** Creates an initially healthy connection. */
        private ScriptedConnection() {
            super("validation", "validation");
        }

        /** Arms a failure after the requested number of successful closure checks. */
        private void arm(final Fault next, final int checks) {
            fault = next;
            openChecks = checks;
        }

        @Override
        public boolean isClosed() throws SQLException {
            if (openChecks > 0) {
                openChecks--;
                return super.isClosed();
            }
            if (fault == Fault.CLOSED) {
                open = false;
            } else if (fault == Fault.STATE_EXCEPTION) {
                fault = Fault.NONE;
                throw exception;
            }
            return super.isClosed();
        }

        @Override
        public boolean isValid(final int timeout) throws SQLException {
            validations++;
            if (entered != null) {
                entered.countDown();
                try {
                    assertTrue(release.await(10, TimeUnit.SECONDS), "Validation was not released");
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new SQLException(e);
                }
            }
            if (fault == Fault.VALID_EXCEPTION) {
                throw exception;
            }
            if (fault == Fault.DIES_IN_VALIDATION) {
                open = false;
            }
            return fault != Fault.INVALID && super.isValid(timeout);
        }

        @Override
        public PreparedStatement prepareStatement(final String sql) throws SQLException {
            if (fault == Fault.PREPARE_EXCEPTION) {
                throw exception;
            }
            checkOpen();
            final PreparedStatement statement = new TesterPreparedStatement(this, sql) {
                @Override
                public ResultSet executeQuery() throws SQLException {
                    if (fault == Fault.EXECUTE_EXCEPTION) {
                        throw exception;
                    }
                    if (fault == Fault.DIES_IN_VALIDATION) {
                        ScriptedConnection.this.open = false;
                    }
                    ScriptedConnection.this.checkOpen();
                    return new TesterResultSet(this, fault == Fault.NO_ROWS ? new Object[0][0] : new Object[][] {{1}});
                }
            };
            statements.add(statement);
            return statement;
        }
    }

    /** Checks public pool counters at a quiescent point. */
    private static void assertAccounting(final GenericObjectPool<?> pool) {
        final int total = pool.getNumActive() + pool.getNumIdle();
        assertEquals(total, pool.getCreatedCount() - pool.getDestroyedCount());
        assertTrue(pool.getMaxTotal() < 0 || total <= pool.getMaxTotal());
    }

    /**
     * Exercises repeated dead-idle replacement for either scripted or real JDBC connections.
     * The caller supplies pool configuration and the physical termination operation.
     */
    static void assertDeadIdleConnections(final BasicDataSource source, final boolean evict,
            final ThrowingConsumer<Connection> terminate) throws Throwable {
        source.setTestWhileIdle(evict);
        source.setMinEvictableIdle(Duration.ofDays(1));
        source.setNumTestsPerEvictionRun(-1);
        final List<Connection> held = new ArrayList<>();
        final List<Connection> victims = new ArrayList<>();
        try {
            for (int round = 0; round < 3; round++) {
                for (int i = 0; i < source.getMaxTotal(); i++) {
                    final Connection connection = source.getConnection();
                    held.add(connection);
                    victims.add(((DelegatingConnection<?>) connection).getInnermostDelegateInternal());
                }
                closeConnections(held);
                final GenericObjectPool<PoolableConnection> pool = source.getConnectionPool();
                pool.setBlockWhenExhausted(false);
                final long destroyed = pool.getDestroyedCount();
                for (final Connection victim : victims) {
                    terminate.accept(victim);
                }
                if (evict) {
                    source.evict();
                    assertEquals(victims.size(), pool.getDestroyedCount() - destroyed, "Idle validation must destroy before borrowing");
                }
                for (int i = 0; i < source.getMaxTotal(); i++) {
                    final Connection connection = source.getConnection();
                    held.add(connection);
                    assertTrue(connection.isValid(5));
                    final Connection physical = ((DelegatingConnection<?>) connection).getInnermostDelegateInternal();
                    for (final Connection victim : victims) {
                        assertNotSame(victim, physical);
                    }
                }
                assertEquals(victims.size(), pool.getDestroyedCount() - destroyed, "Each dead object must be destroyed once");
                assertThrows(SQLException.class, () -> held.add(source.getConnection()));
                assertEquals(source.getMaxTotal(), source.getNumActive());
                assertEquals(0, source.getNumIdle());
                assertAccounting(pool);
                closeConnections(held);
                victims.clear();
                assertAccounting(pool);
            }
        } finally {
            closeConnections(held);
        }
    }

    /** Closes every held handle even if one close fails, then releases the fixture's references. */
    private static void closeConnections(final List<Connection> connections) throws SQLException {
        final List<Throwable> failures = new ArrayList<>();
        for (final Connection connection : connections) {
            Utils.close(connection, failures::add);
        }
        connections.clear();
        if (!failures.isEmpty()) {
            throw new SQLExceptionList(failures);
        }
    }

    /** Configurations shared by embedded and server database tests. */
    static Stream<Arguments> databaseConfigurations() {
        return Stream.of(false, true).flatMap(query -> Stream.of(false, true)
                .flatMap(statements -> Stream.of(false, true).map(evict -> Arguments.of(query, statements, evict))));
    }

    /** Failures shared by borrow, return, and idle validation. */
    private static Stream<Arguments> lifecycleFailures() {
        return Stream.of("borrow", "return", "idle").flatMap(phase -> Stream.of(false, true).flatMap(statements -> Stream.of(Fault.values())
                .filter(fault -> fault != Fault.NONE)
                .flatMap(fault -> Stream.of(false, true).filter(query -> query
                        ? fault != Fault.INVALID && fault != Fault.VALID_EXCEPTION
                        : fault != Fault.PREPARE_EXCEPTION && fault != Fault.EXECUTE_EXCEPTION && fault != Fault.NO_ROWS)
                        .map(query -> Arguments.of(phase, statements, fault, query)))));
    }

    /** Covers both default query spellings and multiple lost idle connections. */
    private static Stream<Arguments> recoveryConfigurations() {
        return Stream.of(null, "", "SELECT 1").flatMap(query -> Stream.of(false, true)
                .flatMap(statements -> Stream.of(false, true).flatMap(evict -> Stream.of(1, 3)
                        .map(capacity -> Arguments.of(query, statements, evict, capacity)))));
    }

    @ParameterizedTest
    @CsvSource({"true,1", "false,1", "true,-1", "false,-1"})
    void testCapacityConfiguration(final boolean blocking, final int capacity) throws Exception {
        try (Fixture fixture = new Fixture(null, false)) {
            fixture.pool.setMaxTotal(capacity);
            fixture.pool.setBlockWhenExhausted(blocking);
            fixture.pool.setMaxWait(Duration.ofMillis(20));
            fixture.pool.setLifo(blocking);
            final PoolableConnection dead = fixture.pool.borrowObject();
            dead.close();
            fixture.latest().close();
            fixture.pool.setTestOnBorrow(true);
            try (PoolableConnection replacement = fixture.pool.borrowObject()) {
                assertTrue(replacement.isValid(1));
                if (capacity > 0) {
                    assertThrows(NoSuchElementException.class, fixture.pool::borrowObject);
                } else {
                    try (PoolableConnection extra = fixture.pool.borrowObject()) {
                        assertNotSame(replacement, extra);
                        assertAccounting(fixture.pool);
                    }
                }
                assertEquals(Integer.valueOf(1), fixture.destroys.get(dead));
                assertAccounting(fixture.pool);
            }
        }
    }

    @ParameterizedTest
    @MethodSource("recoveryConfigurations")
    void testDeadIdleRecovery(final String query, final boolean statements, final boolean evict, final int capacity) throws Throwable {
        try (BasicDataSource source = new BasicDataSource() {
            @Override
            protected ConnectionFactory createConnectionFactory() {
                return ScriptedConnection::new;
            }
        }) {
            source.setMaxTotal(capacity);
            source.setPoolPreparedStatements(statements);
            source.setValidationQuery(query);
            assertDeadIdleConnections(source, evict, Connection::close);
        }
    }

    @Test
    void testDestructionFailureDoesNotCorruptCapacity() throws Exception {
        try (Fixture fixture = new Fixture(null, true)) {
            final PoolableConnection connection = fixture.pool.borrowObject();
            connection.close();
            fixture.latest().close();
            fixture.failDestroy = true;
            fixture.pool.setTestOnBorrow(true);
            fixture.assertReplacement(connection);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testDiesBetweenValidationChecks(final boolean statements) throws Exception {
        try (Fixture fixture = new Fixture(null, statements)) {
            final PoolableConnection connection = fixture.pool.borrowObject();
            final ScriptedConnection physical = fixture.latest();
            connection.close();
            physical.arm(Fault.CLOSED, 1);
            fixture.pool.setTestOnBorrow(true);
            fixture.assertReplacement(connection);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testDriverRecovery(final boolean underlyingAccess) throws Exception {
        try (Fixture fixture = new Fixture(null, false)) {
            final PoolingDriver driver = PoolingDriver.builder().setAccessToUnderlyingConnectionAllowed(underlyingAccess).get();
            final String name = UUID.randomUUID().toString();
            driver.registerPool(name, fixture.pool);
            try {
                final String url = PoolingDriver.URL_PREFIX + name;
                try (Connection connection = driver.connect(url, new Properties())) {
                    assertTrue(connection.isValid(1));
                }
                fixture.latest().close();
                fixture.pool.setTestOnBorrow(true);
                try (Connection replacement = driver.connect(url, new Properties())) {
                    assertTrue(replacement.isValid(1));
                    assertThrows(SQLException.class, () -> driver.connect(url, new Properties()));
                    assertEquals(1, fixture.pool.getDestroyedCount());
                    assertAccounting(fixture.pool);
                }
            } finally {
                driver.closePool(name);
            }
        }
    }

    @Test
    void testExceptionHandlerOverrideIsPreserved() throws Exception {
        final ScriptedConnection physical = new ScriptedConnection();
        final List<SQLException> handled = new ArrayList<>();
        final PoolableConnection connection = new PoolableConnection(physical, null, null) {
            @Override
            protected void handleException(final SQLException exception) {
                handled.add(exception);
            }
        };
        try {
            physical.arm(Fault.VALID_EXCEPTION, 0);
            assertEquals("isValid() returned false", assertThrows(SQLException.class, () -> connection.validate(null, Duration.ZERO)).getMessage());
            assertEquals(Arrays.asList(physical.exception), handled);
        } finally {
            connection.reallyClose();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testFactoryPreservesClosedStateOverride(final boolean query) throws SQLException {
        final ScriptedConnection physical = new ScriptedConnection();
        final PoolableConnection connection = new PoolableConnection(physical, null, null) {
            @Override
            protected boolean isClosedInternal() {
                return true;
            }
        };
        try {
            final PoolableConnectionFactory factory = new PoolableConnectionFactory(() -> physical, null);
            factory.setValidationQuery(query ? "SELECT 1" : null);
            assertTrue(connection.isClosed());
            assertThrows(SQLException.class, () -> factory.validateConnection(connection));
            assertThrows(SQLException.class, () -> connection.validate(null, Duration.ZERO));
            assertEquals(0, physical.validations);
            assertTrue(physical.statements.isEmpty());
        } finally {
            connection.reallyClose();
        }
    }

    @Test
    void testFactoryPreservesValidationOverride() throws SQLException {
        final List<String> queries = new ArrayList<>();
        final PoolableConnection connection = new PoolableConnection(new ScriptedConnection(), null, null) {
            @Override
            public void validate(final String sql, final Duration timeout) {
                queries.add(sql);
            }
        };
        try {
            final PoolableConnectionFactory factory = new PoolableConnectionFactory(ScriptedConnection::new, null);
            factory.setValidationQuery("SELECT 1");
            factory.validateConnection(connection);
            assertEquals(Arrays.asList("SELECT 1"), queries);
        } finally {
            connection.reallyClose();
        }
    }

    @ParameterizedTest
    @MethodSource("lifecycleFailures")
    void testFailedValidationDestroysOnce(final String phase, final boolean statements, final Fault fault, final boolean query) throws Exception {
        try (Fixture fixture = new Fixture(query ? "SELECT 1" : null, statements)) {
            final PoolableConnection connection = fixture.pool.borrowObject();
            final ScriptedConnection physical = fixture.latest();
            if (!"return".equals(phase)) {
                connection.close();
            }
            // Returning checks physical closure before the pool's validation callback.
            physical.arm(fault, "return".equals(phase) ? 1 : 0);
            fixture.pool.setTestOnBorrow(true);
            if ("return".equals(phase)) {
                fixture.pool.setTestOnReturn(true);
                connection.close();
            } else if ("idle".equals(phase)) {
                fixture.pool.setTestWhileIdle(true);
                fixture.pool.setMinEvictableIdleDuration(Duration.ofDays(1));
                fixture.pool.evict();
            }
            if (!"borrow".equals(phase)) {
                assertEquals(Integer.valueOf(1), fixture.destroys.get(connection));
            }
            fixture.assertReplacement(connection);
        }
    }

    @ParameterizedTest
    @CsvSource({"false,false", "true,false", "true,true"})
    void testJdbcExceptionPreservesFatalState(final boolean fastFail, final boolean ignored) throws Exception {
        try (Fixture fixture = new Fixture(null, false)) {
            fixture.factory.setFastFailValidation(fastFail);
            if (ignored) {
                fixture.factory.setDisconnectionIgnoreSqlCodes(Arrays.asList("08006"));
            }
            final PoolableConnection connection = fixture.pool.borrowObject();
            final ScriptedConnection physical = fixture.latest();
            physical.arm(Fault.VALID_EXCEPTION, 0);
            assertSame(physical.exception, assertThrows(SQLException.class, () -> connection.validate(null, Duration.ZERO)));
            physical.arm(Fault.NONE, 0);
            if (fastFail && !ignored) {
                assertThrows(SQLException.class, () -> connection.validate(null, Duration.ZERO));
                assertEquals(1, physical.validations);
            } else {
                connection.validate(null, Duration.ZERO);
                assertEquals(2, physical.validations);
            }
            connection.close();
        }
    }

    @Test
    void testManagedDestructionUnregistersConnection() throws Exception {
        final LocalXAConnectionFactory connections = new LocalXAConnectionFactory(new TransactionManagerImpl(), ScriptedConnection::new);
        final PoolableManagedConnectionFactory factory = new PoolableManagedConnectionFactory(connections, null);
        try (GenericObjectPool<PoolableConnection> pool = new GenericObjectPool<>(factory)) {
            factory.setPool(pool);
            pool.setMaxTotal(1);
            pool.setTestOnBorrow(true);
            final PoolableConnection connection = pool.borrowObject();
            final TransactionRegistry registry = factory.getTransactionRegistry();
            assertTrue(registry.getXAResource(connection) != null);
            connection.close();
            connection.getInnermostDelegateInternal().close();
            try (PoolableConnection replacement = pool.borrowObject()) {
                assertTrue(replacement.isValid(1));
                assertThrows(SQLException.class, () -> registry.getXAResource(connection));
                assertEquals(1, pool.getDestroyedCount());
                assertAccounting(pool);
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testManagedRecovery(final boolean statements) throws Throwable {
        try (BasicManagedDataSource source = new BasicManagedDataSource()) {
            source.setDriver(new TesterDriver());
            source.setUrl("jdbc:apache:commons:testdriver");
            source.setUsername("userName");
            source.setPassword("password");
            source.setTransactionManager(new TransactionManagerImpl());
            source.setMaxTotal(1);
            source.setPoolPreparedStatements(statements);
            assertDeadIdleConnections(source, false, Connection::close);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"isClosed", "isValid", "close", "guardClose"})
    void testPublicCleanupStillInvalidates(final String operation) throws Exception {
        try (Fixture fixture = new Fixture(null, false)) {
            final PoolableConnection connection = fixture.pool.borrowObject();
            fixture.latest().close();
            switch (operation) {
            case "isClosed":
                assertTrue(connection.isClosed());
                break;
            case "isValid":
                assertFalse(connection.isValid(1));
                break;
            case "guardClose":
                new DelegatingConnection<>(connection).close();
                break;
            default:
                connection.close();
                break;
            }
            assertEquals(1, fixture.invalidations, "The public operation must clean up before an explicit close");
            connection.close();
            assertEquals(1, fixture.invalidations);
            assertEquals(Integer.valueOf(1), fixture.destroys.get(connection));
            assertAccounting(fixture.pool);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testShutdownDuringValidation(final boolean shutdown) throws Exception {
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        try (Fixture fixture = new Fixture(null, false)) {
            final PoolableConnection connection = fixture.pool.borrowObject();
            final ScriptedConnection physical = fixture.latest();
            connection.close();
            physical.arm(Fault.DIES_IN_VALIDATION, 0);
            physical.entered = new CountDownLatch(1);
            physical.release = new CountDownLatch(1);
            fixture.pool.setTestOnBorrow(true);
            fixture.pool.setBlockWhenExhausted(true);
            fixture.pool.setMaxWait(Duration.ofSeconds(5));
            final Future<?> borrow = executor.submit(() -> {
                try (PoolableConnection replacement = fixture.pool.borrowObject()) {
                    assertTrue(replacement.isValid(1));
                } catch (final IllegalStateException expected) {
                    assertTrue(shutdown);
                }
                return null;
            });
            try {
                assertTrue(physical.entered.await(10, TimeUnit.SECONDS));
                if (shutdown) {
                    fixture.pool.close();
                }
            } finally {
                physical.release.countDown();
            }
            borrow.get(10, TimeUnit.SECONDS);
            assertEquals(Integer.valueOf(1), fixture.destroys.get(connection));
            assertEquals(0, fixture.invalidations);
            assertAccounting(fixture.pool);
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testUnregisteredDirectValidation(final boolean query) throws Exception {
        final ScriptedConnection physical = new ScriptedConnection();
        try (PoolableConnection connection = new PoolableConnection(physical, null, null)) {
            final PoolableConnectionFactory factory = new PoolableConnectionFactory(() -> physical, null);
            factory.setValidationQuery(query ? "SELECT 1" : null);
            factory.validateConnection(connection);
            physical.close();
            assertThrows(SQLException.class, () -> factory.validateConnection(connection));
            // The owner, rather than validation, performs physical destruction.
            connection.reallyClose();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"create", "newBorrow", "startup"})
    void testValidationBeforeRegistration(final String phase) throws Exception {
        try (Fixture fixture = new Fixture(null, false)) {
            fixture.makeFault = Fault.CLOSED;
            if ("startup".equals(phase)) {
                assertThrows(SQLException.class, () -> BasicDataSource.validateConnectionFactory(fixture.factory));
            } else {
                fixture.pool.setTestOnCreate("create".equals(phase));
                fixture.pool.setTestOnBorrow(true);
                assertThrows(NoSuchElementException.class, fixture.pool::borrowObject);
            }
            assertEquals(0, fixture.invalidations);
            assertTrue(fixture.physical.get(0).isClosed());
            assertAccounting(fixture.pool);
            try (PoolableConnection recovered = fixture.pool.borrowObject()) {
                assertTrue(recovered.isValid(1));
                assertThrows(NoSuchElementException.class, fixture.pool::borrowObject);
                assertAccounting(fixture.pool);
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"logical", "physical", "missing", "statementWrapper"})
    void testValidationClosureStates(final String state) throws Exception {
        try (Fixture fixture = new Fixture(null, false)) {
            final PoolableConnection connection = fixture.pool.borrowObject();
            final Connection physical = fixture.latest();
            switch (state) {
            case "logical":
                connection.setClosedInternal(true);
                break;
            case "physical":
                physical.close();
                break;
            case "missing":
                connection.setDelegate(null);
                break;
            default:
                final PoolingConnection wrapper = new PoolingConnection(physical);
                wrapper.setClosedInternal(true);
                connection.setDelegate(wrapper);
                break;
            }
            try {
                assertFalse(fixture.factory.validateObject(new DefaultPooledObject<>(connection)));
                assertThrows(SQLException.class, () -> connection.validate(null, Duration.ZERO));
                assertEquals(0, fixture.invalidations);
                assertEquals(0, fixture.destroys.size());
            } finally {
                connection.setDelegate(physical);
                connection.setClosedInternal(false);
                fixture.pool.invalidateObject(connection);
            }
        }
    }

    @Test
    void testValidationStatementAndJmxCleanup() throws Exception {
        final ObjectName name = new ObjectName("org.apache.commons.dbcp2:type=ValidationTest,name=" + UUID.randomUUID());
        final PoolableConnectionFactory factory = new PoolableConnectionFactory(ScriptedConnection::new, name);
        factory.setValidationQuery("SELECT 1");
        factory.setPoolStatements(true);
        try (GenericObjectPool<PoolableConnection> pool = new GenericObjectPool<>(factory)) {
            factory.setPool(pool);
            pool.setMaxTotal(1);
            pool.setTestOnBorrow(true);
            final PoolableConnection connection = pool.borrowObject();
            final ObjectName connectionName = new ObjectName(name + ",connectionpool=connections,connection=0");
            assertTrue(ManagementFactory.getPlatformMBeanServer().isRegistered(connectionName));
            final ScriptedConnection physical = (ScriptedConnection) connection.getInnermostDelegateInternal();
            assertEquals(1, physical.statements.size());
            connection.close();
            physical.close();
            try (PoolableConnection replacement = pool.borrowObject()) {
                assertTrue(replacement.isValid(1));
                assertTrue(physical.statements.get(0).isClosed());
                assertFalse(ManagementFactory.getPlatformMBeanServer().isRegistered(connectionName));
                assertEquals(1, pool.getDestroyedCount());
                assertAccounting(pool);
            }
        }
    }

    @Test
    void testWaitingBorrowerGetsReplacementAfterFailedReturnValidation() throws Exception {
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        try (Fixture fixture = new Fixture(null, false)) {
            fixture.pool.setBlockWhenExhausted(true);
            fixture.pool.setMaxWait(Duration.ofSeconds(10));
            fixture.pool.setTestOnReturn(true);
            final PoolableConnection connection = fixture.pool.borrowObject();
            final ScriptedConnection physical = fixture.latest();
            final Future<?> borrow = executor.submit(() -> {
                try (PoolableConnection replacement = fixture.pool.borrowObject()) {
                    assertNotSame(connection, replacement);
                    assertTrue(replacement.isValid(1));
                }
                return null;
            });
            final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (fixture.pool.getNumWaiters() == 0 && System.nanoTime() < deadline) {
                Thread.yield();
            }
            assertEquals(1, fixture.pool.getNumWaiters());
            physical.arm(Fault.CLOSED, 1);
            connection.close();
            borrow.get(10, TimeUnit.SECONDS);
            assertEquals(Integer.valueOf(1), fixture.destroys.get(connection));
            assertEquals(0, fixture.invalidations);
            assertAccounting(fixture.pool);
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
    }
}
