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

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Tests every innermost-delegate traversal with cyclic and acyclic chains. */
class TestDelegateChains {

    private enum Kind {
        CONNECTION(Connection.class, DelegatingConnection.class, "connection", false),
        CONNECTION_INTERNAL(Connection.class, DelegatingConnection.class, "connection", true),
        STATEMENT(Statement.class, DelegatingStatement.class, "statement", false),
        STATEMENT_INTERNAL(Statement.class, DelegatingStatement.class, "statement", true),
        RESULT_SET(ResultSet.class, DelegatingResultSet.class, "resultSet", false),
        METADATA(DatabaseMetaData.class, DelegatingDatabaseMetaData.class, "databaseMetaData", false);

        private final Class<?> jdbcType;
        private final Class<?> wrapperType;
        private final String fieldName;
        private final boolean internal;

        Kind(final Class<?> jdbcType, final Class<?> wrapperType, final String fieldName, final boolean internal) {
            this.jdbcType = jdbcType;
            this.wrapperType = wrapperType;
            this.fieldName = fieldName;
            this.internal = internal;
        }

        private Object create(final Object delegate, final boolean allowed) throws ReflectiveOperationException {
            if (jdbcType == Connection.class) {
                return new DelegatingConnection<Connection>((Connection) delegate) {
                    @Override
                    protected boolean isAccessToUnderlyingConnectionAllowed() {
                        return allowed;
                    }
                };
            }
            final DelegatingConnection<Connection> parent = new DelegatingConnection<Connection>(null) {
                @Override
                protected boolean isAccessToUnderlyingConnectionAllowed() {
                    return allowed;
                }
            };
            if (jdbcType == Statement.class) {
                return new DelegatingStatement(parent, (Statement) delegate);
            }
            if (jdbcType == DatabaseMetaData.class) {
                return new LinkedDatabaseMetaData(parent, (DatabaseMetaData) terminal(), (DatabaseMetaData) delegate);
            }
            final Object wrapper = DelegatingResultSet.wrapResultSet(parent, (ResultSet) terminal());
            setDelegate(wrapper, delegate);
            return wrapper;
        }

        private Object innermost(final Object wrapper) throws ReflectiveOperationException {
            return wrapperType.getMethod(internal ? "getInnermostDelegateInternal" : "getInnermostDelegate").invoke(wrapper);
        }

        private void setDelegate(final Object wrapper, final Object delegate) throws ReflectiveOperationException {
            if (wrapper instanceof DelegatingConnection) {
                @SuppressWarnings("unchecked")
                final DelegatingConnection<Connection> connection = (DelegatingConnection<Connection>) wrapper;
                connection.setDelegate((Connection) delegate);
            } else if (wrapper instanceof DelegatingStatement) {
                ((DelegatingStatement) wrapper).setDelegate((Statement) delegate);
            } else if (wrapper instanceof LinkedDatabaseMetaData) {
                ((LinkedDatabaseMetaData) wrapper).next = (DatabaseMetaData) delegate;
            } else {
                // Result-set delegates are final: inject malformed graphs solely to exercise defensive traversal.
                final Field field = wrapperType.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(wrapper, delegate);
            }
        }

        private Object terminal() {
            return Proxy.newProxyInstance(jdbcType.getClassLoader(), new Class<?>[] {jdbcType}, (proxy, method, args) -> {
                if ("toString".equals(method.getName())) {
                    return "Terminal JDBC delegate";
                }
                throw new AssertionError("Traversal must not call JDBC methods: " + method.getName());
            });
        }
    }

    private static final class LinkedDatabaseMetaData extends DelegatingDatabaseMetaData {
        private volatile DatabaseMetaData next;

        private LinkedDatabaseMetaData(final DelegatingConnection<?> connection, final DatabaseMetaData terminal, final DatabaseMetaData next) {
            super(connection, terminal);
            this.next = next;
        }

        @Override
        public DatabaseMetaData getDelegate() {
            return super.getDelegate() == null ? null : next;
        }
    }

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private static Stream<Arguments> acyclicArguments() {
        return Arrays.stream(Kind.values()).flatMap(kind -> Stream.of(1, 2, 16, 256).flatMap(length ->
                Stream.of(false, true).map(nullTerminal -> Arguments.of(kind, length, nullTerminal))));
    }

    private static Stream<Arguments> cyclicArguments() {
        return Arrays.stream(Kind.values()).flatMap(kind -> Stream.of(0, 1, 8).flatMap(prefix ->
                Stream.of(1, 2, 3, 16).map(length -> Arguments.of(kind, prefix, length))));
    }

    private static Stream<Arguments> guardedArguments() {
        return Arrays.stream(Kind.values()).flatMap(kind -> Stream.of(0, 1, 8).map(prefix -> Arguments.of(kind, prefix)));
    }

    @ParameterizedTest(name = "{0}, length={1}, null={2}")
    @MethodSource("acyclicArguments")
    void testAcyclicChain(final Kind kind, final int length, final boolean nullTerminal) throws Exception {
        final Object terminal = nullTerminal ? null : kind.terminal();
        Object head = terminal;
        for (int i = 0; i < length; i++) {
            head = kind.create(head, true);
        }
        final Object root = head;
        assertSame(terminal, assertTimeoutPreemptively(TIMEOUT, () -> kind.innermost(root)));
    }

    @ParameterizedTest(name = "{0}, prefix={1}, cycle={2}")
    @MethodSource("cyclicArguments")
    void testCyclicChain(final Kind kind, final int prefix, final int length) throws Exception {
        final List<Object> nodes = new ArrayList<>();
        for (int i = 0; i < prefix + length; i++) {
            nodes.add(kind.create(null, true));
        }
        for (int i = 0; i < nodes.size() - 1; i++) {
            kind.setDelegate(nodes.get(i), nodes.get(i + 1));
        }
        kind.setDelegate(nodes.get(nodes.size() - 1), nodes.get(prefix));
        try {
            assertNull(assertTimeoutPreemptively(TIMEOUT, () -> kind.innermost(nodes.get(0))));
        } finally {
            // Never close a cyclic JDBC graph: first break its links, including after a failed traversal.
            for (final Object node : nodes) {
                kind.setDelegate(node, null);
            }
        }
    }

    @ParameterizedTest(name = "{0}, guarded depth={1}")
    @MethodSource("guardedArguments")
    void testGuardedChain(final Kind kind, final int prefix) throws Exception {
        final Object terminal = kind.terminal();
        Object head = kind.create(terminal, false);
        for (int i = 0; i < prefix; i++) {
            head = kind.create(head, true);
        }
        assertSame(kind.internal ? terminal : null, kind.innermost(head));
    }

    @Test
    void testInteriorConnectionCycle() throws Exception {
        testCyclicChain(Kind.CONNECTION, 1, 2);
    }

    @Test
    void testIdentityRatherThanEquality() throws Exception {
        final Object terminal = Kind.CONNECTION.terminal();
        Connection head = (Connection) terminal;
        for (int i = 0; i < 16; i++) {
            head = new DelegatingConnection<Connection>(head) {
                @Override
                public boolean equals(final Object obj) {
                    return obj instanceof DelegatingConnection;
                }

                @Override
                public int hashCode() {
                    return 1;
                }
            };
        }
        assertSame(terminal, Kind.CONNECTION.innermost(head));
        assertSame(terminal, Kind.CONNECTION_INTERNAL.innermost(head));
    }
}
