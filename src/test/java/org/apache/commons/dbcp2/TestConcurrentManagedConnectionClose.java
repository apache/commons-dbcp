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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.transaction.Transaction;

import org.apache.commons.dbcp2.ConcurrentCloseTestSupport.Task;
import org.apache.commons.dbcp2.managed.LocalXAConnectionFactory;
import org.apache.commons.dbcp2.managed.ManagedConnection;
import org.apache.commons.dbcp2.managed.PoolableManagedConnectionFactory;
import org.apache.commons.dbcp2.managed.TransactionRegistry;
import org.apache.commons.pool2.BaseObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.geronimo.transaction.manager.TransactionManagerImpl;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests ownership of managed delegates when close overlaps a transaction callback.
 */
class TestConcurrentManagedConnectionClose extends AbstractDatabaseTest {

    /**
     * Failures during delegate inspection or close must not leave the delegate attached.
     */
    private enum Failure {
        NONE, IS_CLOSED_SQL, IS_CLOSED_RUNTIME, CLOSE_SQL, CLOSE_RUNTIME
    }

    /**
     * Uses a real transaction manager, managed connection factory, and single-entry database pool.
     */
    private static final class Fixture implements AutoCloseable {
        private final TransactionManagerImpl manager = new TransactionManagerImpl();
        private final LocalXAConnectionFactory factory;
        private final TestDatabase database;
        private final GenericObjectPool<PoolableConnection> pool;

        /**
         * Creates a pool that rejects a second borrower instead of waiting indefinitely.
         */
        Fixture(final TestDatabase database) throws Exception {
            this.database = database;
            factory = new LocalXAConnectionFactory(manager, database::connect);
            final PoolableManagedConnectionFactory connectionFactory = new PoolableManagedConnectionFactory(factory, null);
            pool = new GenericObjectPool<>(connectionFactory);
            connectionFactory.setPool(pool);
            pool.setMaxTotal(1);
            pool.setBlockWhenExhausted(false);
            pool.setMaxWait(Duration.ofSeconds(1));
        }

        /**
         * Borrows an ordinary managed handle.
         */
        ManagedConnection<PoolableConnection> borrow() throws SQLException {
            return new ManagedConnection<>(pool, factory.getTransactionRegistry(), true);
        }

        @Override
        public void close() throws Exception {
            try {
                if (manager.getTransaction() != null) {
                    manager.rollback();
                }
            } finally {
                pool.close();
            }
        }

        /**
         * Resumes the suspended transaction on a worker and runs the real completion listeners.
         */
        Task complete(final Transaction transaction, final boolean commit) {
            return new Task(() -> {
                manager.resume(transaction);
                if (commit) {
                    manager.commit();
                } else {
                    manager.rollback();
                }
            });
        }
    }

    /**
     * Coordinates a single callback with the application closing its logical connection.
     */
    private static final class Gate {
        private final Phase phase;
        private final CountDownLatch entered = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);
        private final AtomicBoolean paused = new AtomicBoolean();
        private volatile Thread callback;

        /**
         * Selects the callback checkpoint.
         */
        Gate(final Phase phase) {
            this.phase = phase;
        }

        /**
         * Pauses only the callback thread and only once.
         */
        void pause(final Phase current) {
            if (phase == current && Thread.currentThread() == callback && paused.compareAndSet(false, true)) {
                entered.countDown();
                await(release);
            }
        }
    }

    /**
     * Adds scheduling checkpoints without changing the delegate or closed-state values.
     */
    private static final class GatedConnection extends ManagedConnection<PoolableConnection> {
        private final Gate gate;

        /**
         * Borrows and enlists a connection in the current transaction.
         */
        GatedConnection(final GenericObjectPool<PoolableConnection> pool, final TransactionRegistry registry, final Gate gate) throws SQLException {
            super(pool, registry, true);
            this.gate = gate;
        }

        @Override
        protected boolean isClosedInternal() {
            if (gate != null) {
                gate.pause(Phase.CHECK_CLOSED);
            }
            return super.isClosedInternal();
        }

        @Override
        protected void transactionComplete() {
            gate.callback = Thread.currentThread();
            try {
                gate.pause(Phase.BEFORE_CALLBACK);
                super.transactionComplete();
                gate.pause(Phase.AFTER_CALLBACK);
            } finally {
                gate.callback = null;
            }
        }
    }

    /**
     * Points at which a transaction completion callback may be paused.
     */
    private enum Phase {
        BEFORE_CALLBACK, CHECK_CLOSED, AFTER_CALLBACK
    }

    /**
     * Checks pool ownership as well as usability of the next logical connection.
     */
    private static void assertBorrowed(final Fixture fixture, final Connection connection) throws Exception {
        assertEquals(1, fixture.pool.getNumActive());
        assertEquals(0, fixture.pool.getNumIdle());
        assertFalse(connection.isClosed());
        assertThrows(SQLException.class, fixture::borrow);
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(fixture.database.select(42))) {
            assertTrue(result.next());
            assertEquals(42, result.getInt(1));
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testClosedDuringTransactionReturnsOnlyOnCompletion(final boolean commit) throws Exception {
        final Gate gate = new Gate(Phase.BEFORE_CALLBACK);
        try (Fixture fixture = new Fixture(database)) {
            fixture.manager.begin();
            final GatedConnection owner = new GatedConnection(fixture.pool, fixture.factory.getTransactionRegistry(), gate);
            final ManagedConnection<PoolableConnection> shared = fixture.borrow();
            shared.close();
            owner.close();
            assertEquals(1, fixture.pool.getNumActive());
            assertEquals(0, fixture.pool.getNumIdle());
            gate.release.countDown();
            try (Task completion = fixture.complete(fixture.manager.suspend(), commit)) {
                completion.awaitSuccess();
            }
            assertNull(owner.getDelegate());
            assertNull(shared.getDelegate());
            assertEquals(0, fixture.pool.getNumActive());
            assertEquals(1, fixture.pool.getNumIdle());
            try (ManagedConnection<PoolableConnection> next = fixture.borrow()) {
                owner.close();
                shared.close();
                assertBorrowed(fixture, next);
            }
        }
    }

    @ParameterizedTest
    @EnumSource(Failure.class)
    void testCloseDetachesDelegateOnFailure(final Failure failure) throws Exception {
        final CountDownLatch entered = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final AtomicInteger closes = new AtomicInteger();
        final Connection delegate = (Connection) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {Connection.class},
                (proxy, method, args) -> {
                    if ("isClosed".equals(method.getName())) {
                        entered.countDown();
                        await(release);
                        if (failure == Failure.IS_CLOSED_SQL) {
                            throw new SQLException("Inspection failed");
                        }
                        if (failure == Failure.IS_CLOSED_RUNTIME) {
                            throw new IllegalStateException("Inspection failed");
                        }
                        return false;
                    }
                    if ("close".equals(method.getName())) {
                        closes.incrementAndGet();
                        if (failure == Failure.CLOSE_SQL) {
                            throw new SQLException("Close failed");
                        }
                        if (failure == Failure.CLOSE_RUNTIME) {
                            throw new IllegalStateException("Close failed");
                        }
                        return null;
                    }
                    throw new AssertionError("Unexpected call: " + method.getName());
                });
        final BaseObjectPool<Connection> pool = new BaseObjectPool<Connection>() {
            @Override
            public Connection borrowObject() {
                return delegate;
            }

            @Override
            public void invalidateObject(final Connection connection) {
                throw new AssertionError("Unexpected invalidation");
            }

            @Override
            public void returnObject(final Connection connection) {
                throw new AssertionError("Unexpected pool return");
            }
        };
        final ManagedConnection<Connection> connection = new ManagedConnection<>(pool, new TransactionRegistry(new TransactionManagerImpl()), true);
        try (Task first = new Task(connection::close)) {
            await(entered);
            try (Task second = new Task(connection::close)) {
                try {
                    second.awaitBlocked();
                } finally {
                    release.countDown();
                }
                final Throwable error = first.awaitFailure();
                if (failure == Failure.IS_CLOSED_RUNTIME || failure == Failure.CLOSE_RUNTIME) {
                    assertInstanceOf(IllegalStateException.class, error);
                } else if (failure == Failure.CLOSE_SQL) {
                    assertInstanceOf(SQLException.class, error);
                } else {
                    assertNull(error);
                }
                second.awaitSuccess();
            }
            assertNull(connection.getDelegate());
            assertTrue(connection.isClosed());
            connection.close();
            assertEquals(failure == Failure.IS_CLOSED_RUNTIME ? 0 : 1, closes.get());
        } finally {
            release.countDown();
            pool.close();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testCloseWaitsForCompletionCleanup(final boolean commit) throws Exception {
        final Gate gate = new Gate(Phase.CHECK_CLOSED);
        try (Fixture fixture = new Fixture(database)) {
            fixture.manager.begin();
            final GatedConnection original = new GatedConnection(fixture.pool, fixture.factory.getTransactionRegistry(), gate);
            final Connection physical = original.getInnermostDelegateInternal();
            try (Task completion = fixture.complete(fixture.manager.suspend(), commit)) {
                await(gate.entered);
                try (Task close = new Task(original::close)) {
                    try {
                        close.awaitBlocked();
                        assertEquals(1, fixture.pool.getNumActive());
                    } finally {
                        gate.release.countDown();
                    }
                    completion.awaitSuccess();
                    close.awaitSuccess();
                }
                assertNull(original.getDelegate());
                assertEquals(1, fixture.pool.getNumIdle());
                try (ManagedConnection<PoolableConnection> next = fixture.borrow()) {
                    assertSame(physical, next.getInnermostDelegateInternal());
                    original.close();
                    assertBorrowed(fixture, next);
                }
            } finally {
                gate.release.countDown();
                original.close();
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testCompletionAfterCloseDoesNotReturnBorrowedConnection(final boolean commit) throws Exception {
        final Gate gate = new Gate(Phase.BEFORE_CALLBACK);
        try (Fixture fixture = new Fixture(database)) {
            fixture.manager.begin();
            final GatedConnection original = new GatedConnection(fixture.pool, fixture.factory.getTransactionRegistry(), gate);
            try (Task completion = fixture.complete(fixture.manager.suspend(), commit)) {
                await(gate.entered);
                // Another handle's completion listener can mark the shared context complete before this listener runs.
                original.getTransactionContext().completeTransaction();
                original.close();
                try (ManagedConnection<PoolableConnection> next = fixture.borrow()) {
                    gate.release.countDown();
                    completion.awaitSuccess();
                    assertNull(original.getDelegate());
                    assertBorrowed(fixture, next);
                    original.close();
                    assertBorrowed(fixture, next);
                }
            } finally {
                gate.release.countDown();
                original.close();
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testSharedCloseDoesNotReturnOwnersConnection(final boolean commit) throws Exception {
        final Gate gate = new Gate(Phase.AFTER_CALLBACK);
        try (Fixture fixture = new Fixture(database)) {
            fixture.manager.begin();
            final GatedConnection owner = new GatedConnection(fixture.pool, fixture.factory.getTransactionRegistry(), gate);
            final ManagedConnection<PoolableConnection> shared = fixture.borrow();
            assertSame(owner.getDelegate(), shared.getDelegate());
            try (Task completion = fixture.complete(fixture.manager.suspend(), commit)) {
                await(gate.entered);
                shared.close();
                assertNull(shared.getDelegate());
                assertBorrowed(fixture, owner);
                gate.release.countDown();
                completion.awaitSuccess();
                assertBorrowed(fixture, owner);
                owner.close();
                assertEquals(0, fixture.pool.getNumActive());
                assertEquals(1, fixture.pool.getNumIdle());
            } finally {
                gate.release.countDown();
                shared.close();
                owner.close();
            }
        }
    }
}
