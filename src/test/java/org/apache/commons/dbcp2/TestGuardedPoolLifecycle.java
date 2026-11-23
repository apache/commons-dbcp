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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.stream.Stream;

import org.apache.commons.pool2.DestroyMode;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Verifies that the client access policy does not interfere with internal pool cleanup. */
class TestGuardedPoolLifecycle {

    private enum ConnectionState {
        OPEN, CLOSED, FAILED
    }

    private static Stream<Arguments> closeArguments() {
        return Stream.of(false, true).flatMap(allowed -> Stream.of(ConnectionState.values()).map(state -> Arguments.of(allowed, state)));
    }

    private static Stream<Arguments> statementArguments() {
        return Stream.of(false, true).flatMap(allowed -> Stream.of(false, true).map(callable -> Arguments.of(allowed, callable)));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testAbandonedConnection(final boolean allowed) throws Exception {
        try (TesterConnection physical = new TesterConnection("u", "p")) {
            final DelegatingConnection<Connection> guarded = new DelegatingConnection<Connection>(physical) {
                @Override
                protected boolean isAccessToUnderlyingConnectionAllowed() {
                    return allowed;
                }
            };
            final PoolableConnectionFactory factory = new PoolableConnectionFactory(() -> guarded, null);
            final PoolableConnection connection = new PoolableConnection(new DelegatingConnection<>(guarded), null, null);
            factory.destroyObject(new DefaultPooledObject<>(connection), DestroyMode.ABANDONED);
            assertTrue(physical.isAborted());
            assertTrue(physical.isClosed());
        }
    }

    @ParameterizedTest
    @MethodSource("closeArguments")
    void testCloseWhenPoolRejectsConnection(final boolean allowed, final ConnectionState state) throws Exception {
        try (TesterConnection physical = new TesterConnection("u", "p")) {
            final DelegatingConnection<Connection> guarded = new DelegatingConnection<Connection>(physical) {
                @Override
                protected boolean isAccessToUnderlyingConnectionAllowed() {
                    return allowed;
                }

                @Override
                public boolean isClosed() throws SQLException {
                    if (state == ConnectionState.FAILED) {
                        throw new SQLException("Failed isClosed check");
                    }
                    return state == ConnectionState.CLOSED;
                }
            };
            final PoolableConnectionFactory factory = new PoolableConnectionFactory(() -> guarded, null);
            try (GenericObjectPool<PoolableConnection> pool = new GenericObjectPool<PoolableConnection>(factory) {
                /**
                 * Always throws {@link IllegalStateException}.
                 *
                 * @throws IllegalStateException Thrown always.
                 */
                @Override
                public void invalidateObject(final PoolableConnection connection) {
                    throw new IllegalStateException("Pool rejected connection");
                }

                /**
                 * Always throws {@link IllegalStateException}.
                 *
                 * @throws IllegalStateException Thrown always.
                 */
                @Override
                public void returnObject(final PoolableConnection connection) {
                    throw new IllegalStateException("Pool rejected connection");
                }
            }) {
                final PoolableConnection connection = new PoolableConnection(guarded, pool, null);
                if (state == ConnectionState.FAILED) {
                    final SQLException exception = assertThrows(SQLException.class, connection::close);
                    assertEquals("Cannot close connection (isClosed check failed)", exception.getMessage());
                } else {
                    connection.close();
                }
                assertTrue(physical.isClosed());
                assertTrue(connection.isClosed());
            }
        }
    }

    @ParameterizedTest
    @MethodSource("statementArguments")
    void testDestroyGuardedPooledStatement(final boolean allowed, final boolean callable) throws Exception {
        try (TesterConnection physical = new TesterConnection("u", "p");
                DelegatingConnection<Connection> guarded = new DelegatingConnection<Connection>(physical) {
                    @Override
                    protected boolean isAccessToUnderlyingConnectionAllowed() {
                        return allowed;
                    }
                };
                PoolingConnection connection = new PoolingConnection(guarded) {
                    @Override
                    protected boolean isAccessToUnderlyingConnectionAllowed() {
                        return allowed;
                    }
                }) {
            final GenericKeyedObjectPool<PStmtKey, DelegatingPreparedStatement> pool = new GenericKeyedObjectPool<>(connection);
            connection.setStatementPool(pool);
            final PreparedStatement statement = callable ? connection.prepareCall("SELECT 1") : connection.prepareStatement("SELECT 1");
            final DelegatingStatement inner = (DelegatingStatement) ((DelegatingStatement) statement).getDelegateInternal();
            final PreparedStatement raw = (PreparedStatement) inner.getDelegateInternal();
            statement.close();
            assertFalse(raw.isClosed());
            pool.clear();
            assertTrue(raw.isClosed());
        }
    }
}
