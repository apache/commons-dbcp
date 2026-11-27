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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntSupplier;
import java.util.stream.Stream;

import javax.sql.ConnectionEvent;
import javax.sql.ConnectionEventListener;
import javax.sql.PooledConnection;

import org.apache.commons.dbcp2.cpdsadapter.DriverAdapterCPDS;
import org.apache.commons.dbcp2.datasources.InstanceKeyDataSource;
import org.apache.commons.dbcp2.datasources.PerUserPoolDataSource;
import org.apache.commons.dbcp2.datasources.SharedPoolDataSource;
import org.apache.commons.dbcp2.managed.ManagedConnection;
import org.apache.commons.dbcp2.managed.TransactionRegistry;
import org.apache.commons.pool2.BaseObjectPool;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.geronimo.transaction.manager.TransactionManagerImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Tests overlapping closes, including reborrowing before the original close finishes. */
class TestConcurrentConnectionClose extends AbstractEmbeddedDatabaseTest {

    /** A traced resource that pauses closing before the delegate is returned to its pool. */
    private static final class CleanupTrace extends AbandonedTrace {

        private final AtomicInteger calls = new AtomicInteger();
        private final CloseGates gates;
        private final boolean fail;

        /** Registers a resource whose cleanup can be paused and can fail. */
        CleanupTrace(final DelegatingConnection<?> parent, final CloseGates gates, final boolean fail) {
            super(parent);
            this.gates = gates;
            this.fail = fail;
        }

        @Override
        public void close() throws SQLException {
            calls.incrementAndGet();
            gates.firstCloser.compareAndSet(null, Thread.currentThread());
            gates.cleanupEntered.countDown();
            await(gates.allowCleanup);
            if (fail) {
                throw new SQLException("Statement cleanup failed");
            }
        }
    }

    /** Coordinates cleanup, pool return, and completion without relying on a race occurring by chance. */
    private static final class CloseGates {

        private final CountDownLatch cleanupEntered = new CountDownLatch(1);
        private final CountDownLatch allowCleanup = new CountDownLatch(1);
        private final CountDownLatch returned = new CountDownLatch(1);
        private final CountDownLatch allowCompletion = new CountDownLatch(1);
        private final AtomicReference<Thread> firstCloser = new AtomicReference<>();
        private final AtomicInteger returns = new AtomicInteger();
        private final boolean failAfterReturn;

        /** Configures whether the first close fails after returning its delegate. */
        CloseGates(final boolean failAfterReturn) {
            this.failAfterReturn = failAfterReturn;
        }

        /** Pauses the original close after pool return, allowing a new borrower to acquire its delegate. */
        void afterReturn() throws SQLException {
            returns.incrementAndGet();
            if (Thread.currentThread() == firstCloser.get()) {
                returned.countDown();
                await(allowCompletion);
                if (failAfterReturn) {
                    throw new SQLException("Close failed after pool return");
                }
            }
        }

        /** Releases all paused closing operations, including on assertion failures. */
        void release() {
            allowCleanup.countDown();
            allowCompletion.countDown();
        }
    }

    /** Runs a close and makes its result and progress observable to the test thread. */
    private static final class CloseTask implements AutoCloseable {

        private final AtomicReference<Throwable> failure = new AtomicReference<>();
        private final Thread thread;

        /** Starts a daemon worker so a broken close cannot keep the test JVM alive. */
        CloseTask(final Connection connection) {
            thread = new Thread(() -> {
                try {
                    connection.close();
                } catch (final Throwable t) {
                    failure.set(t);
                }
            }, "connection-close-test");
            thread.setDaemon(true);
            thread.start();
        }

        /** Gets the close failure, or null after a successful close, within the test deadline. */
        Throwable awaitCompletion() throws InterruptedException {
            thread.join(TimeUnit.SECONDS.toMillis(10));
            assertFalse(thread.isAlive(), "Close did not finish");
            return failure.get();
        }

        /** Waits until the second caller is blocked, rejecting duplicate cleanup or an early return. */
        void awaitWaiting(final CleanupTrace trace) throws InterruptedException {
            final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            while (true) {
                assertEquals(1, trace.calls.get(), "Both close calls entered cleanup");
                assertTrue(thread.isAlive(), "A concurrent close returned before cleanup finished");
                final Thread.State state = thread.getState();
                if (state == Thread.State.BLOCKED || state == Thread.State.WAITING) {
                    return;
                }
                assertTrue(System.nanoTime() < deadline, "Second close did not wait for the first close");
                Thread.sleep(1);
            }
        }

        @Override
        public void close() throws InterruptedException {
            thread.interrupt();
            thread.join(TimeUnit.SECONDS.toMillis(10));
            assertFalse(thread.isAlive(), "Close worker did not terminate");
        }
    }

    /** Failure points in the base connection's close implementation. */
    private enum Failure {
        NONE, CLEANUP, IS_CLOSED_SQL, IS_CLOSED_RUNTIME, CLOSE_SQL, CLOSE_RUNTIME
    }

    /** Failures before or after returning a connection to a real pool. */
    private enum PoolFailure {
        NONE, CLEANUP, AFTER_RETURN
    }

    /** A single-connection pool and its original borrower. */
    private static final class PoolFixture implements AutoCloseable {

        private final Connection first;
        private final Callable<Connection> borrow;
        private final IntSupplier active;
        private final IntSupplier idle;
        private final AutoCloseable pool;
        private final CloseGates gates;

        /** Borrows the original connection and retains the pool operations used by the assertions. */
        PoolFixture(final Callable<Connection> borrow, final IntSupplier active, final IntSupplier idle, final AutoCloseable pool, final CloseGates gates)
                throws Exception {
            this.borrow = borrow;
            this.active = active;
            this.idle = idle;
            this.pool = pool;
            this.gates = gates;
            first = borrow.call();
        }

        @Override
        public void close() throws Exception {
            gates.release();
            try {
                first.close();
            } finally {
                pool.close();
            }
        }
    }

    /** Client connection implementations that must not return a new borrower's connection. */
    private enum PoolPath {
        DELEGATING, DATA_SOURCE, DRIVER, MANAGED, PER_USER, SHARED
    }

    /** Waits for an explicitly coordinated phase of closing. */
    private static void await(final CountDownLatch latch) throws SQLException {
        try {
            assertTrue(latch.await(10, TimeUnit.SECONDS), "Close coordination timed out");
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException(e);
        }
    }

    /** Configures a CPDS-backed pool with one physical connection and bounded borrowing. */
    private static void configureCpdsPool(final InstanceKeyDataSource ds, final DriverAdapterCPDS cpds) {
        ds.setConnectionPoolDataSource(cpds);
        ds.setDefaultMaxTotal(1);
        ds.setDefaultMaxWait(Duration.ofSeconds(5));
    }

    /** Exercises each client wrapper with successful and failing cleanup. */
    private static Stream<Arguments> poolArguments() {
        return Stream.of(PoolPath.values()).flatMap(path -> Stream.of(PoolFailure.values()).map(failure -> Arguments.of(path, failure)));
    }

    private static Thread startAfter(final CyclicBarrier barrier, final AtomicReference<Throwable> failure, final Executable task) {
        final Thread thread = new Thread(() -> {
            try {
                barrier.await();
                task.execute();
            } catch (final Throwable t) {
                failure.compareAndSet(null, t);
            }
        });
        thread.start();
        return thread;
    }

    private PoolingDataSource<PoolableConnection> ds;

    private GenericObjectPool<PoolableConnection> pool;

    /** Creates a CPDS-backed pool and observes notifications after its factory has processed them. */
    private PoolFixture createCpdsPool(final PoolPath path, final CloseGates gates) throws Exception {
        final AtomicReference<PooledConnection> pooled = new AtomicReference<>();
        final DriverAdapterCPDS cpds = new DriverAdapterCPDS() {

            @Override
            public PooledConnection getPooledConnection(final String user, final String password) throws SQLException {
                final PooledConnection connection = super.getPooledConnection(user, password);
                pooled.set(connection);
                return connection;
            }
        };
        cpds.setDriver(database.getDriverClassName());
        cpds.setUrl(database.newUrl());
        final InstanceKeyDataSource ds;
        final PoolFixture fixture;
        if (path == PoolPath.PER_USER) {
            final PerUserPoolDataSource perUser = new PerUserPoolDataSource();
            ds = perUser;
            configureCpdsPool(ds, cpds);
            fixture = new PoolFixture(ds::getConnection, perUser::getNumActive, perUser::getNumIdle, ds, gates);
        } else {
            final SharedPoolDataSource shared = new SharedPoolDataSource();
            shared.setMaxTotal(1);
            ds = shared;
            configureCpdsPool(ds, cpds);
            fixture = new PoolFixture(ds::getConnection, shared::getNumActive, shared::getNumIdle, ds, gates);
        }
        pooled.get().addConnectionEventListener(new ConnectionEventListener() {

            @Override
            public void connectionClosed(final ConnectionEvent event) {
                try {
                    gates.afterReturn();
                } catch (final SQLException e) {
                    throw new IllegalStateException(e);
                }
            }

            @Override
            public void connectionErrorOccurred(final ConnectionEvent event) {
                throw new AssertionError(event.getSQLException());
            }
        });
        return fixture;
    }
    /** Creates a pool whose physical connection can be reborrowed while the original close is still executing. */
    private PoolFixture createPool(final PoolPath path, final CloseGates gates) throws Exception {
        if (path == PoolPath.PER_USER || path == PoolPath.SHARED) {
            return createCpdsPool(path, gates);
        }
        final PoolableConnectionFactory factory = new PoolableConnectionFactory(() -> database.connect(), null) {

            @Override
            public PooledObject<PoolableConnection> makeObject() throws SQLException {
                return new DefaultPooledObject<>(new PoolableConnection(database.connect(), getPool(), null) {

                    @Override
                    public void close() throws SQLException {
                        super.close();
                        gates.afterReturn();
                    }
                });
            }
        };
        final GenericObjectPool<PoolableConnection> pool = new GenericObjectPool<>(factory);
        factory.setPool(pool);
        pool.setMaxTotal(1);
        pool.setMaxWait(Duration.ofSeconds(5));
        final Callable<Connection> borrow;
        AutoCloseable owner = pool;
        switch (path) {
        case DATA_SOURCE:
            final PoolingDataSource<PoolableConnection> ds = new PoolingDataSource<>(pool);
            borrow = ds::getConnection;
            break;
        case DRIVER:
            final PoolingDriver driver = new PoolingDriver();
            final String name = UUID.randomUUID().toString();
            driver.registerPool(name, pool);
            borrow = () -> driver.connect(PoolingDriver.URL_PREFIX + name, new Properties());
            owner = () -> driver.closePool(name);
            break;
        case MANAGED:
            final TransactionRegistry registry = new TransactionRegistry(new TransactionManagerImpl());
            borrow = () -> new ManagedConnection<>(pool, registry, false);
            break;
        default:
            borrow = () -> new DelegatingConnection<>(pool.borrowObject());
            break;
        }
        return new PoolFixture(borrow, pool::getNumActive, pool::getNumIdle, owner, gates);
    }

    @BeforeEach
    void setUp() {
        final Properties properties = new Properties();
        properties.setProperty(Constants.KEY_USER, "userName");
        properties.setProperty(Constants.KEY_PASSWORD, "password");
        final PoolableConnectionFactory factory = new PoolableConnectionFactory(
                new DriverConnectionFactory(new TesterDriver(), "jdbc:apache:commons:testdriver", properties), null);
        factory.setValidationQuery("SELECT DUMMY FROM DUAL");
        factory.setDefaultReadOnly(Boolean.TRUE);
        factory.setDefaultAutoCommit(Boolean.TRUE);
        pool = new GenericObjectPool<>(factory);
        factory.setPool(pool);
        ds = new PoolingDataSource<>(pool);
        ds.setAccessToUnderlyingConnectionAllowed(true);
    }

    @AfterEach
    void tearDown() throws SQLException {
        ds.close();
    }

    @Test
    void testCloseAfterActivation() throws Exception {
        try (DelegatingConnection<Connection> connection = new DelegatingConnection<>(null)) {
            for (int i = 0; i < 3; i++) {
                final TesterConnection physical = new TesterConnection("u", "p");
                connection.setDelegate(physical);
                connection.activate();
                assertFalse(connection.isClosed());
                connection.close();
                assertTrue(connection.isClosed());
                assertTrue(physical.isClosed());
                connection.close();
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void testCloseMissingOrClosedDelegate(final boolean missing) throws Exception {
        final TesterConnection physical = new TesterConnection("u", "p");
        physical.close();
        try (DelegatingConnection<Connection> connection = new DelegatingConnection<>(missing ? null : physical)) {
            connection.close();
            connection.close();
            assertTrue(connection.isClosed());
        }
    }

    @ParameterizedTest
    @EnumSource(Failure.class)
    void testConcurrentCloseFailure(final Failure failure) throws Exception {
        final AtomicInteger stateChecks = new AtomicInteger();
        final AtomicInteger delegateCloses = new AtomicInteger();
        final Exception expected = failure == Failure.IS_CLOSED_RUNTIME || failure == Failure.CLOSE_RUNTIME ? new IllegalStateException("Driver failure")
                : new SQLException("Driver failure");
        final Connection physical = (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[] { Connection.class },
                (proxy, method, args) -> {
                    if ("isClosed".equals(method.getName())) {
                        final int checks = stateChecks.incrementAndGet();
                        if (failure == Failure.IS_CLOSED_SQL || failure == Failure.IS_CLOSED_RUNTIME && checks == 1) {
                            throw expected;
                        }
                        return false;
                    }
                    if ("close".equals(method.getName())) {
                        delegateCloses.incrementAndGet();
                        if (failure == Failure.CLOSE_SQL || failure == Failure.CLOSE_RUNTIME) {
                            throw expected;
                        }
                        return null;
                    }
                    throw new AssertionError("Unexpected call: " + method.getName());
                });
        final CloseGates gates = new CloseGates(false);
        try (DelegatingConnection<Connection> connection = new DelegatingConnection<>(physical)) {
            final CleanupTrace trace = new CleanupTrace(connection, gates, failure == Failure.CLEANUP);
            try (CloseTask first = new CloseTask(connection)) {
                await(gates.cleanupEntered);
                try (CloseTask second = new CloseTask(connection)) {
                    try {
                        second.awaitWaiting(trace);
                        gates.release();
                        final Throwable result = first.awaitCompletion();
                        if (failure == Failure.NONE || failure == Failure.IS_CLOSED_SQL) {
                            assertNull(result);
                        } else if (failure == Failure.CLEANUP) {
                            assertInstanceOf(SQLExceptionList.class, result);
                        } else {
                            assertSame(expected, result);
                        }
                        assertNull(second.awaitCompletion());
                    } finally {
                        gates.release();
                    }
                }
            } finally {
                gates.release();
            }
            assertTrue(connection.isClosed());
            connection.close();
            assertEquals(1, trace.calls.get());
            assertEquals(failure == Failure.IS_CLOSED_RUNTIME ? 2 : 1, stateChecks.get());
            assertEquals(1, delegateCloses.get());
        }
    }

    @ParameterizedTest
    @MethodSource("poolArguments")
    void testConcurrentClosePreservesNewBorrower(final PoolPath path, final PoolFailure failure) throws Exception {
        final CloseGates gates = new CloseGates(failure == PoolFailure.AFTER_RETURN);
        final Connection physical;
        try (PoolFixture fixture = createPool(path, gates)) {
            final DelegatingConnection<?> original = (DelegatingConnection<?>) fixture.first;
            physical = original.getInnermostDelegateInternal();
            final CleanupTrace trace = new CleanupTrace(original, gates, failure == PoolFailure.CLEANUP);
            try (CloseTask first = new CloseTask(original)) {
                await(gates.cleanupEntered);
                try (CloseTask second = new CloseTask(original)) {
                    try {
                        second.awaitWaiting(trace);
                        gates.allowCleanup.countDown();
                        await(gates.returned);
                        try (Connection borrowed = fixture.borrow.call()) {
                            assertSame(physical, ((DelegatingConnection<?>) borrowed).getInnermostDelegateInternal());
                            assertEquals(1, fixture.active.getAsInt());
                            assertEquals(0, fixture.idle.getAsInt());
                            second.awaitWaiting(trace);
                            gates.allowCompletion.countDown();
                            final Throwable result = first.awaitCompletion();
                            if (failure == PoolFailure.NONE) {
                                assertNull(result);
                            } else if (failure == PoolFailure.AFTER_RETURN && (path == PoolPath.PER_USER || path == PoolPath.SHARED)) {
                                assertInstanceOf(IllegalStateException.class, result);
                            } else {
                                assertInstanceOf(SQLException.class, result);
                            }
                            assertNull(second.awaitCompletion());
                            original.close();
                            assertEquals(1, trace.calls.get());
                            assertEquals(1, gates.returns.get());
                            assertTrue(original.isClosed());
                            assertFalse(borrowed.isClosed());
                            assertTrue(borrowed.isValid(1));
                            assertEquals(1, fixture.active.getAsInt());
                            assertEquals(0, fixture.idle.getAsInt());
                        }
                        assertEquals(2, gates.returns.get());
                        assertEquals(0, fixture.active.getAsInt());
                        assertEquals(1, fixture.idle.getAsInt());
                        assertFalse(physical.isClosed());
                    } finally {
                        gates.release();
                    }
                }
            } finally {
                gates.release();
            }
        }
        // Physical destruction must still work after passivation marked the pooled connection closed.
        assertTrue(physical.isClosed());
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void testConcurrentGuardCloseAfterStateQueryFailure(final boolean dataSource) throws Exception {
        final AtomicInteger stateChecks = new AtomicInteger();
        final AtomicInteger delegateCloses = new AtomicInteger();
        final IllegalStateException expected = new IllegalStateException("Transient state-query failure");
        final Connection physical = (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[] { Connection.class },
                (proxy, method, args) -> {
                    if ("isClosed".equals(method.getName())) {
                        if (stateChecks.incrementAndGet() == 1) {
                            throw expected;
                        }
                        return false;
                    }
                    if ("close".equals(method.getName())) {
                        delegateCloses.incrementAndGet();
                        return null;
                    }
                    throw new AssertionError("Unexpected call: " + method.getName());
                });
        final BaseObjectPool<Connection> pool = new BaseObjectPool<Connection>() {

            @Override
            public Connection borrowObject() {
                return physical;
            }

            @Override
            public void invalidateObject(final Connection connection) {
                throw new AssertionError("The guard closes its delegate directly");
            }

            @Override
            public void returnObject(final Connection connection) {
                throw new AssertionError("The guard closes its delegate directly");
            }
        };
        final PoolingDriver driver = new PoolingDriver();
        final String name = UUID.randomUUID().toString();
        driver.registerPool(name, pool);
        final CloseGates gates = new CloseGates(false);
        try (PoolingDataSource<Connection> ds = new PoolingDataSource<>(pool);
                DelegatingConnection<?> connection = (DelegatingConnection<?>) (dataSource ? ds.getConnection()
                        : driver.connect(PoolingDriver.URL_PREFIX + name, new Properties()))) {
            final CleanupTrace trace = new CleanupTrace(connection, gates, false);
            try (CloseTask first = new CloseTask(connection)) {
                await(gates.cleanupEntered);
                try (CloseTask second = new CloseTask(connection)) {
                    try {
                        second.awaitWaiting(trace);
                        gates.release();
                        assertSame(expected, first.awaitCompletion());
                        assertNull(second.awaitCompletion());
                    } finally {
                        gates.release();
                    }
                }
            } finally {
                gates.release();
            }
            assertTrue(connection.isClosed());
            assertEquals(1, trace.calls.get());
            // Preserve the existing policies: data source guards detach even on failure; driver guards can retry.
            assertEquals(dataSource ? 1 : 2, stateChecks.get());
            assertEquals(dataSource ? 0 : 1, delegateCloses.get());
            if (dataSource) {
                assertNull(connection.getDelegateInternal());
            }
        } finally {
            driver.closePool(name);
        }
    }

    @Test
    void testCpdsCloseListenerCanCloseAndReborrow() throws Exception {
        final DriverAdapterCPDS cpds = new DriverAdapterCPDS();
        cpds.setDriver(database.getDriverClassName());
        cpds.setUrl(database.newUrl());
        final PooledConnection pooled = cpds.getPooledConnection();
        try {
            final Connection first = pooled.getConnection();
            final AtomicInteger notifications = new AtomicInteger();
            final AtomicReference<Connection> borrowed = new AtomicReference<>();
            final ConnectionEventListener listener = new ConnectionEventListener() {

                @Override
                public void connectionClosed(final ConnectionEvent event) {
                    assertEquals(1, notifications.incrementAndGet());
                    try {
                        assertTrue(first.isClosed());
                        first.close();
                        borrowed.set(pooled.getConnection());
                    } catch (final SQLException e) {
                        throw new IllegalStateException(e);
                    }
                }

                @Override
                public void connectionErrorOccurred(final ConnectionEvent event) {
                    throw new AssertionError(event.getSQLException());
                }
            };
            pooled.addConnectionEventListener(listener);
            first.close();
            first.close();
            assertEquals(1, notifications.get());
            pooled.removeConnectionEventListener(listener);
            try (Connection second = borrowed.get()) {
                assertSame(((DelegatingConnection<?>) first).getInnermostDelegateInternal(), ((DelegatingConnection<?>) second).getInnermostDelegateInternal());
                assertFalse(second.isClosed());
                assertTrue(second.isValid(1));
            }
        } finally {
            pooled.close();
        }
    }

    /**
     * Two threads close the same connection while a third thread waits for one. Only one close may return the pooled connection; otherwise it is returned again
     * while the waiting thread holds it.
     */
    @Test
    void testPr564() throws Exception {
        pool.setMaxTotal(1);
        pool.setMaxWait(Duration.ofSeconds(5));
        for (int i = 0; i < 500; i++) {
            final Connection first = ds.getConnection();
            final CyclicBarrier barrier = new CyclicBarrier(3);
            final AtomicReference<Connection> second = new AtomicReference<>();
            final AtomicReference<Throwable> failure = new AtomicReference<>();
            final Thread[] threads = { startAfter(barrier, failure, first::close), startAfter(barrier, failure, first::close),
                    startAfter(barrier, failure, () -> second.set(ds.getConnection())) };
            for (final Thread thread : threads) {
                thread.join();
            }
            assertNull(failure.get());
            try (Connection borrowed = second.get()) {
                assertNotNull(borrowed);
                assertEquals(1, pool.getNumActive(), "round " + i);
                assertEquals(0, pool.getNumIdle(), "round " + i);
                assertFalse(borrowed.isClosed(), "round " + i);
            }
        }
    }
}
