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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.apache.commons.dbcp2.ConcurrentCloseTestSupport.Task;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests concurrent explicit and connection-driven statement cleanup.
 */
@ParameterizedClass(name = "{0}")
@EnumSource(value = TestDatabase.class, names = {"H2", "HSQLDB"})
class TestConcurrentStatementClose {

    /** Database used by this invocation. */
    @Parameter
    TestDatabase database;

    /**
     * Cleanup failures must still leave the wrapper closed and its delegate detached.
     */
    private enum Failure {
        NONE, RESULT_SET_SQL, RESULT_SET_RUNTIME, DELEGATE_SQL, DELEGATE_RUNTIME
    }

    /**
     * JDBC statement wrapper types that inherit the common close implementation.
     */
    private enum Kind {
        STATEMENT, PREPARED, CALLABLE
    }

    /**
     * Combines each inherited implementation with every cleanup failure.
     */
    static Stream<Arguments> cleanupCases() {
        return Stream.of(Kind.values()).flatMap(kind -> Stream.of(Failure.values()).map(failure -> Arguments.of(kind, failure)));
    }

    /**
     * Combines both pooled statement types with both closing entry points and return outcomes.
     */
    static Stream<Arguments> poolingCases() {
        return Stream.of(false, true).flatMap(callable -> Stream.of(false, true)
                .flatMap(connectionClose -> Stream.of(false, true).map(fail -> Arguments.of(callable, connectionClose, fail))));
    }

    /**
     * Prepares a statement that uses a single reusable pool entry.
     */
    private PreparedStatement prepare(final Connection connection, final boolean callable) throws SQLException {
        return callable ? connection.prepareCall("CALL ABS(?)") : connection.prepareStatement(database.selectParameter());
    }

    @ParameterizedTest
    @MethodSource("cleanupCases")
    void testConcurrentCleanup(final Kind kind, final Failure failure) throws Exception {
        final CountDownLatch entered = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final AtomicInteger statementCloses = new AtomicInteger();
        final AtomicInteger resultSetCloses = new AtomicInteger();
        final CallableStatement delegate = (CallableStatement) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] {CallableStatement.class}, (proxy, method, args) -> {
                    if ("close".equals(method.getName())) {
                        statementCloses.incrementAndGet();
                        if (failure == Failure.DELEGATE_SQL) {
                            throw new SQLException("Statement close failed");
                        }
                        if (failure == Failure.DELEGATE_RUNTIME) {
                            throw new IllegalStateException("Statement close failed");
                        }
                        return null;
                    }
                    throw new AssertionError("Unexpected call: " + method.getName());
                });
        try (DelegatingConnection<Connection> connection = new DelegatingConnection<>(new TesterConnection("test", "test"))) {
            final DelegatingStatement statement;
            switch (kind) {
            case PREPARED:
                statement = new DelegatingPreparedStatement(connection, delegate);
                break;
            case CALLABLE:
                statement = new DelegatingCallableStatement(connection, delegate);
                break;
            default:
                statement = new DelegatingStatement(connection, delegate);
                break;
            }
            final ResultSet resultSet = (ResultSet) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {ResultSet.class},
                    (proxy, method, args) -> {
                        if ("close".equals(method.getName())) {
                            resultSetCloses.incrementAndGet();
                            entered.countDown();
                            await(release);
                            if (failure == Failure.RESULT_SET_SQL) {
                                throw new SQLException("Result set close failed");
                            }
                            if (failure == Failure.RESULT_SET_RUNTIME) {
                                throw new IllegalStateException("Result set close failed");
                            }
                            return null;
                        }
                        throw new AssertionError("Unexpected call: " + method.getName());
                    });
            // Retain the wrapper because the trace uses weak references.
            final ResultSet traced = DelegatingResultSet.wrapResultSet(statement, resultSet);
            try (Task first = new Task(statement::close)) {
                await(entered);
                try (Task second = new Task(statement::close)) {
                    try {
                        second.awaitBlocked();
                        assertEquals(1, resultSetCloses.get());
                    } finally {
                        release.countDown();
                    }
                    if (failure == Failure.NONE) {
                        first.awaitSuccess();
                    } else {
                        assertInstanceOf(SQLExceptionList.class, first.awaitFailure());
                    }
                    second.awaitSuccess();
                }
                assertEquals(1, statementCloses.get());
                assertEquals(1, resultSetCloses.get());
                assertTrue(statement.isClosed());
                assertNull(statement.getDelegateInternal());
                assertTrue(statement.getTrace().isEmpty());
                assertTrue(connection.getTrace().isEmpty());
                statement.close();
                assertEquals(1, statementCloses.get());
                assertNull(traced.getStatement());
            } finally {
                release.countDown();
            }
        }
    }

    @ParameterizedTest
    @MethodSource("poolingCases")
    void testConcurrentCloseDoesNotReturnBorrowedStatement(final boolean callable, final boolean connectionClose, final boolean failAfterReturn)
            throws Exception {
        final CountDownLatch returned = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final AtomicInteger returns = new AtomicInteger();
        try (PoolingConnection pooling = new PoolingConnection(database.connect())) {
            final GenericKeyedObjectPool<PStmtKey, DelegatingPreparedStatement> pool = new GenericKeyedObjectPool<PStmtKey, DelegatingPreparedStatement>(pooling) {
                @Override
                public void returnObject(final PStmtKey key, final DelegatingPreparedStatement statement) {
                    super.returnObject(key, statement);
                    if (returns.incrementAndGet() == 1) {
                        returned.countDown();
                        await(release);
                        if (failAfterReturn) {
                            throw new IllegalStateException("Return completed but close failed");
                        }
                    }
                }
            };
            pooling.setStatementPool(pool);
            pool.setMaxTotal(1);
            pool.setMaxTotalPerKey(1);
            pool.setBlockWhenExhausted(false);
            // A separate wrapper lets us borrow again while the original connection wrapper is still closing.
            final DelegatingConnection<Connection> originalConnection = new DelegatingConnection<>(pooling);
            final DelegatingConnection<Connection> nextConnection = new DelegatingConnection<>(pooling);
            final PreparedStatement original = prepare(originalConnection, callable);
            try (Task first = new Task(connectionClose ? originalConnection::close : original::close)) {
                await(returned);
                try (PreparedStatement next = prepare(nextConnection, callable); Task second = new Task(original::close)) {
                    try {
                        second.awaitBlocked();
                        assertEquals(1, returns.get());
                        assertEquals(1, pool.getNumActive());
                        assertEquals(0, pool.getNumIdle());
                    } finally {
                        release.countDown();
                    }
                    if (failAfterReturn) {
                        assertInstanceOf(SQLException.class, first.awaitFailure());
                    } else {
                        first.awaitSuccess();
                    }
                    second.awaitSuccess();
                    // Closing a connection also closes its statement pool; use the assertions above for that path.
                    if (!connectionClose) {
                        assertEquals(1, pool.getNumActive());
                        assertEquals(0, pool.getNumIdle());
                        assertFalse(next.isClosed());
                        assertThrows(SQLException.class, () -> prepare(nextConnection, callable));
                        next.setInt(1, 42);
                        try (ResultSet result = next.executeQuery()) {
                            assertTrue(result.next());
                            assertEquals(42, result.getInt(1));
                        }
                    }
                    original.close();
                    assertEquals(1, returns.get());
                }
            } finally {
                release.countDown();
            }
        }
    }
}
