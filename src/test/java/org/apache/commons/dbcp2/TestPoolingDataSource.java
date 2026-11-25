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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.pool2.BaseObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * TestSuite for PoolingDataSource
 */
public class TestPoolingDataSource extends TestConnectionPool {

    private static void closeWithStatementFailure(final DelegatingConnection<?> connection) throws SQLException {
        try (TesterStatement physicalStatement = new TesterStatement(connection);
                DelegatingStatement statement = new DelegatingStatement(connection, physicalStatement)) {
            physicalStatement.setSqlExceptionOnClose(true);
            try {
                assertThrows(SQLException.class, connection::close);
            } finally {
                physicalStatement.setSqlExceptionOnClose(false);
            }
        }
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

    protected PoolingDataSource<PoolableConnection> ds;

    private GenericObjectPool<PoolableConnection> pool;

    @Override
    protected Connection getConnection() throws Exception {
        return ds.getConnection();
    }

    @BeforeEach
    public void setUp() throws Exception {
        final Properties properties = new Properties();
        properties.setProperty(Constants.KEY_USER, "userName");
        properties.setProperty(Constants.KEY_PASSWORD, "password");
        final PoolableConnectionFactory factory =
            new PoolableConnectionFactory(
                    new DriverConnectionFactory(new TesterDriver(),
                            "jdbc:apache:commons:testdriver", properties),
                    null);
        factory.setValidationQuery("SELECT DUMMY FROM DUAL");
        factory.setDefaultReadOnly(Boolean.TRUE);
        factory.setDefaultAutoCommit(Boolean.TRUE);
        pool = new GenericObjectPool<>(factory);
        factory.setPool(pool);
        pool.setMaxTotal(getMaxTotal());
        pool.setMaxWait(getMaxWaitDuration());
        ds = new PoolingDataSource<>(pool);
        ds.setAccessToUnderlyingConnectionAllowed(true);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        ds.close();
        super.tearDown();
    }

    @ParameterizedTest
    @CsvSource({"false, false", "false, true", "true, false", "true, true"})
    void testAbortClosedGuardDoesNotAffectNextBorrower(final boolean allowed, final boolean failedClose) throws Exception {
        ds.setAccessToUnderlyingConnectionAllowed(allowed);
        try (DelegatingConnection<?> first = (DelegatingConnection<?>) ds.getConnection()) {
            final Connection pooled = first.getDelegateInternal();
            final TesterConnection physical = (TesterConnection) first.getInnermostDelegateInternal();
            if (failedClose) {
                closeWithStatementFailure(first);
            } else {
                first.close();
            }
            assertTrue(first.isClosed());
            try (DelegatingConnection<?> second = (DelegatingConnection<?>) ds.getConnection()) {
                assertSame(pooled, second.getDelegateInternal());
                first.abort(Runnable::run);
                assertFalse(physical.isAborted());
                assertFalse(second.isClosed());
                assertTrue(second.isValid(1));
                assertEquals(1, pool.getNumActive());
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testAbortOpenGuardReleasesPoolCapacity(final boolean allowed) throws Exception {
        ds.setAccessToUnderlyingConnectionAllowed(allowed);
        pool.setMaxTotal(1);
        try (DelegatingConnection<?> first = (DelegatingConnection<?>) ds.getConnection()) {
            final TesterConnection physical = (TesterConnection) first.getInnermostDelegateInternal();
            first.abort(Runnable::run);
            assertTrue(physical.isAborted());
            first.close();
            assertTrue(first.isClosed());
            assertNull(first.getDelegateInternal());
            assertEquals(0, pool.getNumActive());
            // Invalidation may already have created an idle replacement.
            try (DelegatingConnection<?> second = (DelegatingConnection<?>) ds.getConnection()) {
                final TesterConnection replacement = (TesterConnection) second.getInnermostDelegateInternal();
                assertNotSame(physical, replacement);
                assertTrue(second.isValid(1));
                first.abort(Runnable::run);
                assertFalse(replacement.isAborted());
                assertEquals(1, pool.getNumActive());
            }
        }
    }

    @Test
    void testClose() throws Exception {
        final Properties properties = new Properties();
        properties.setProperty(Constants.KEY_USER, "userName");
        properties.setProperty(Constants.KEY_PASSWORD, "password");
        final PoolableConnectionFactory f = new PoolableConnectionFactory(
                new DriverConnectionFactory(new TesterDriver(), "jdbc:apache:commons:testdriver", properties), null);
        f.setValidationQuery("SELECT DUMMY FROM DUAL");
        f.setDefaultReadOnly(Boolean.TRUE);
        f.setDefaultAutoCommit(Boolean.TRUE);
        final GenericObjectPool<PoolableConnection> p = new GenericObjectPool<>(f);
        p.setMaxTotal(getMaxTotal());
        p.setMaxWait(getMaxWaitDuration());

        try (PoolingDataSource<PoolableConnection> dataSource = new PoolingDataSource<>(p)) {
            final Connection connection = dataSource.getConnection();
            assertNotNull(connection);
            connection.close();
        }

        assertTrue(p.isClosed());
        assertEquals(0, p.getNumIdle());
        assertEquals(0, p.getNumActive());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testCloseClearsDelegateAfterStatementFailure(final boolean allowed) throws Exception {
        ds.setAccessToUnderlyingConnectionAllowed(allowed);
        try (DelegatingConnection<?> connection = (DelegatingConnection<?>) ds.getConnection()) {
            closeWithStatementFailure(connection);
            assertTrue(connection.isClosed());
            assertNull(connection.getDelegateInternal());
            assertNull(connection.getDelegate());
            assertNull(connection.getInnermostDelegateInternal());
            assertThrows(SQLException.class, connection::createStatement);
            assertEquals(0, pool.getNumActive());
            assertEquals(1, pool.getNumIdle());
            connection.close();
            assertEquals(1, pool.getNumIdle());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"isClosed", "close"})
    void testCloseFailureLeavesGuardClosedAndDetached(final String failedMethod) throws Exception {
        final Exception failure = "isClosed".equals(failedMethod) ? new IllegalStateException("State check failed") : new SQLException("Close failed");
        final Connection physical = (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[] {Connection.class},
                (proxy, method, args) -> {
                    if (failedMethod.equals(method.getName())) {
                        throw failure;
                    }
                    if ("isClosed".equals(method.getName())) {
                        return false;
                    }
                    throw new AssertionError("Unexpected call: " + method.getName());
                });
        try (PoolingDataSource<Connection> dataSource = new PoolingDataSource<>(new BaseObjectPool<Connection>() {
            @Override
            public Connection borrowObject() {
                return physical;
            }

            /**
             * Always throws {@link AssertionError}.
             *
             * @throws AssertionError Thrown always.
             */
            @Override
            public void invalidateObject(final Connection connection) {
                throw new AssertionError("The guard must close its delegate directly");
            }

            /**
             * Always throws {@link AssertionError}.
             *
             * @throws AssertionError Thrown always.
             */
            @Override
            public void returnObject(final Connection connection) {
                throw new AssertionError("The guard must close its delegate directly");
            }
        }); DelegatingConnection<?> guard = (DelegatingConnection<?>) dataSource.getConnection()) {
            assertSame(failure, assertThrows(failure.getClass(), guard::close));
            assertTrue(guard.isClosed());
            assertNull(guard.getDelegateInternal());
            assertThrows(SQLException.class, guard::createStatement);
            guard.close();
        }
    }

    /**
     * Two threads close the same connection while a third thread waits for one. Only one close may return the
     * pooled connection; otherwise it is returned again while the waiting thread holds it.
     */
    @Test
    void testConcurrentCloseDoesNotReturnBorrowedConnection() throws Exception {
        pool.setMaxTotal(1);
        pool.setMaxWait(Duration.ofSeconds(5));
        for (int i = 0; i < 500; i++) {
            final Connection first = ds.getConnection();
            final CyclicBarrier barrier = new CyclicBarrier(3);
            final AtomicReference<Connection> second = new AtomicReference<>();
            final AtomicReference<Throwable> failure = new AtomicReference<>();
            final Thread[] threads = {
                startAfter(barrier, failure, first::close),
                startAfter(barrier, failure, first::close),
                startAfter(barrier, failure, () -> second.set(ds.getConnection()))};
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

    /**
     * DBCP-412
     * Verify that omitting factory.setPool(pool) when setting up PDS does not
     * result in NPE.
     */
    @Test
    void testFixFactoryConfig() throws Exception {
        final Properties properties = new Properties();
        properties.setProperty(Constants.KEY_USER, "userName");
        properties.setProperty(Constants.KEY_PASSWORD, "password");
        final PoolableConnectionFactory f =
            new PoolableConnectionFactory(
                    new DriverConnectionFactory(new TesterDriver(),
                            "jdbc:apache:commons:testdriver", properties),
                    null);
        f.setValidationQuery("SELECT DUMMY FROM DUAL");
        f.setDefaultReadOnly(Boolean.TRUE);
        f.setDefaultAutoCommit(Boolean.TRUE);
        final GenericObjectPool<PoolableConnection> p = new GenericObjectPool<>(f);
        p.setMaxTotal(getMaxTotal());
        p.setMaxWait(getMaxWaitDuration());
        ds = new PoolingDataSource<>(p);
        assertEquals(f.getPool(), p);
        ds.getConnection();
    }

    @Test
    void testIsWrapperFor() throws Exception {
        assertTrue(ds.isWrapperFor(PoolingDataSource.class));
        assertTrue(ds.isWrapperFor(AutoCloseable.class));
        assertFalse(ds.isWrapperFor(String.class));
        assertFalse(ds.isWrapperFor(null));
    }

    @Test
    void testPoolGuardConnectionWrapperEqualInnermost() throws Exception {
        ds.setAccessToUnderlyingConnectionAllowed(true);
        final DelegatingConnection<?> con = (DelegatingConnection<?>) ds.getConnection();
        final Connection inner = con.getInnermostDelegate();
        ds.setAccessToUnderlyingConnectionAllowed(false);
        final DelegatingConnection<Connection> con2 = new DelegatingConnection<>(inner);
        assertNotEquals(con2, con);
        assertTrue(con.innermostDelegateEquals(con2.getInnermostDelegate()));
        assertTrue(con2.innermostDelegateEquals(inner));
        assertNotEquals(con, con2);
    }

    @Test
    void testPoolGuardConnectionWrapperEqualsFail() throws Exception {
        final Connection con1 = ds.getConnection();
        final Connection con2 = ds.getConnection();
        assertNotEquals(con1, con2);
        con1.close();
        con2.close();
    }

    @Test
    void testPoolGuardConnectionWrapperEqualsNull() throws Exception {
        final Connection con1 = ds.getConnection();
        final Connection con2 = null;
        assertNotEquals(con2, con1);
        con1.close();
    }

    /*
     * JIRA: DBCP-198
     */
    @Test
    void testPoolGuardConnectionWrapperEqualsReflexive()
        throws Exception {
        final Connection con = ds.getConnection();
        final Connection con2 = con;
        assertEquals(con2, con);
        assertEquals(con, con2);
        con.close();
    }

    @Test
    void testPoolGuardConnectionWrapperEqualsSameDelegate() throws Exception {
        // Get a maximal set of connections from the pool
        final Connection[] c = new Connection[getMaxTotal()];
        for (int i = 0; i < c.length; i++) {
            c[i] = newConnection();
        }
        // Close the delegate of one wrapper in the pool
        ((DelegatingConnection<?>) c[0]).getDelegate().close();

        // Grab a new connection - should get c[0]'s closed connection
        // so should be delegate-equivalent
        final Connection con = newConnection();
        assertNotEquals(c[0], con);
        assertEquals(
                ((DelegatingConnection<?>) c[0]).getInnermostDelegateInternal(),
                ((DelegatingConnection<?>) con).getInnermostDelegateInternal());
        for (final Connection element : c) {
            element.close();
        }
    }

    @Test
    void testPoolGuardConnectionWrapperEqualsType() throws Exception {
        final Connection con1 = ds.getConnection();
        final Integer con2 = 0;
        assertNotEquals(con2, con1);
        con1.close();
    }

    @Test
    void testUnwrap() throws Exception {
        assertSame(ds.unwrap(PoolingDataSource.class), ds);
        assertSame(ds.unwrap(AutoCloseable.class), ds);
        assertThrows(SQLException.class, () -> ds.unwrap(String.class));
        assertThrows(SQLException.class, () -> ds.unwrap(null));
    }
}
