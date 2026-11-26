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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests cached connection properties across successful writes and driver failures.
 */
@ParameterizedClass(name = "{0}")
@EnumSource(value = TestDatabase.class, names = {"H2", "HSQLDB"})
class TestDelegatingConnectionCachedState {

    /** Database used by this invocation. */
    @Parameter
    TestDatabase database;

    private static final class CacheConfig {
        private final boolean enabled;
        private final boolean primed;
        private final boolean nested;
        private final boolean innerEnabled;

        private CacheConfig(final int flags) {
            enabled = (flags & 1) != 0;
            primed = (flags & 2) != 0;
            nested = (flags & 4) != 0;
            innerEnabled = (flags & 8) != 0;
        }

        private boolean hasEnabledCache() {
            return enabled || nested && innerEnabled;
        }

        @Override
        public String toString() {
            return "cache=" + enabled + ", primed=" + primed + ", nested=" + nested + (nested ? ", inner cache=" + innerEnabled : "");
        }
    }

    private enum CachedProperty {
        AUTO_COMMIT(true, false, Connection::getAutoCommit, (connection, value) -> connection.setAutoCommit((Boolean) value)),
        READ_ONLY(true, false, Connection::isReadOnly, (connection, value) -> connection.setReadOnly((Boolean) value)),
        CATALOG("catalog_1", "catalog_2", Connection::getCatalog, (connection, value) -> connection.setCatalog((String) value)),
        SCHEMA("schema_1", "schema_2", Connection::getSchema, (connection, value) -> connection.setSchema((String) value));

        private final Object initial;
        private final Object updated;
        private final Getter getter;
        private final Setter setter;

        CachedProperty(final Object initial, final Object updated, final Getter getter, final Setter setter) {
            this.initial = initial;
            this.updated = updated;
            this.getter = getter;
            this.setter = setter;
        }

        private Object get(final Connection connection) throws SQLException {
            return getter.get(connection);
        }

        private void set(final Connection connection, final Object value) throws SQLException {
            setter.set(connection, value);
        }
    }

    private enum FailureKind {
        ERROR(() -> new AssertionError("Controlled driver error")),
        RUNTIME(() -> new IllegalStateException("Controlled driver exception")),
        SQL(() -> new SQLException("Controlled driver SQL exception", "HY000", 42));

        private final Supplier<Throwable> supplier;

        FailureKind(final Supplier<Throwable> supplier) {
            this.supplier = supplier;
        }
    }

    private enum FailurePoint {
        BEFORE_SET, AFTER_SET, GETTER
    }

    @FunctionalInterface
    private interface Getter {
        Object get(Connection connection) throws SQLException;
    }

    /**
     * Delegate that doesn't support read-only or auto-commit. It will merely take the input value of setReadOnly and setAutoCommit and discard it, to keep
     * false.
     */
    static final class NoReadOnlyOrAutoCommitConnection extends TesterConnection {
        private final boolean readOnly = false;
        private final boolean autoCommit = false;

        public NoReadOnlyOrAutoCommitConnection() {
            super("", "");
        }

        @Override
        public boolean getAutoCommit() {
            return autoCommit;
        }

        @Override
        public boolean isReadOnly() throws SQLException {
            return readOnly;
        }

        @Override
        public void setAutoCommit(final boolean autoCommit) {
            // Do nothing
        }

        @Override
        public void setReadOnly(final boolean readOnly) {
            // Do nothing
        }
    }

    private static final class PropertyChange {
        private final CachedProperty property;
        private final Object initial;
        private final Object requested;

        private PropertyChange(final CachedProperty property, final Object initial, final Object requested) {
            this.property = property;
            this.initial = initial;
            this.requested = requested;
        }

        @Override
        public String toString() {
            return property + ": " + initial + " -> " + requested;
        }
    }

    @FunctionalInterface
    private interface Setter {
        void set(Connection connection, Object value) throws SQLException;
    }

    /**
     * A deterministic driver substitute that honors read-only and catalog changes.
     */
    private static final class StateConnection extends TesterConnection {
        private final Object[] values = Stream.of(CachedProperty.values()).map(property -> property.initial).toArray();
        private final int[] getterCalls = new int[CachedProperty.values().length];
        private final int[] setterCalls = new int[CachedProperty.values().length];
        private CachedProperty failureProperty;
        private FailurePoint failurePoint;
        private Throwable failure;
        private boolean ignoreWrites;
        private int rollbacks;

        private StateConnection() {
            super("test", "test");
        }

        private void arm(final CachedProperty property, final FailurePoint point, final Throwable throwable) {
            failureProperty = property;
            failurePoint = point;
            failure = throwable;
        }

        private void fail(final CachedProperty property, final FailurePoint point) throws SQLException {
            if (failure != null && failureProperty == property && failurePoint == point) {
                final Throwable throwable = failure;
                failure = null;
                if (throwable instanceof Error) {
                    throw (Error) throwable;
                }
                if (throwable instanceof RuntimeException) {
                    throw (RuntimeException) throwable;
                }
                throw (SQLException) throwable;
            }
        }

        @Override
        public boolean getAutoCommit() throws SQLException {
            return (Boolean) read(CachedProperty.AUTO_COMMIT);
        }

        @Override
        public String getCatalog() throws SQLException {
            return (String) read(CachedProperty.CATALOG);
        }

        @Override
        public String getSchema() throws SQLException {
            return (String) read(CachedProperty.SCHEMA);
        }

        @Override
        public boolean isReadOnly() throws SQLException {
            return (Boolean) read(CachedProperty.READ_ONLY);
        }

        private Object read(final CachedProperty property) throws SQLException {
            getterCalls[property.ordinal()]++;
            checkOpen();
            fail(property, FailurePoint.GETTER);
            return values[property.ordinal()];
        }

        @Override
        public void rollback() throws SQLException {
            super.rollback();
            rollbacks++;
        }

        @Override
        public void setAutoCommit(final boolean autoCommit) throws SQLException {
            write(CachedProperty.AUTO_COMMIT, autoCommit);
        }

        @Override
        public void setCatalog(final String catalog) throws SQLException {
            write(CachedProperty.CATALOG, catalog);
        }

        @Override
        public void setReadOnly(final boolean readOnly) throws SQLException {
            write(CachedProperty.READ_ONLY, readOnly);
        }

        @Override
        public void setSchema(final String schema) throws SQLException {
            write(CachedProperty.SCHEMA, schema);
        }

        private void write(final CachedProperty property, final Object value) throws SQLException {
            setterCalls[property.ordinal()]++;
            checkOpen();
            fail(property, FailurePoint.BEFORE_SET);
            if (!ignoreWrites) {
                values[property.ordinal()] = value;
            }
            fail(property, FailurePoint.AFTER_SET);
        }
    }

    private static final class TrackingConnection extends DelegatingConnection<Connection> {
        private SQLException handled;

        private TrackingConnection(final Connection connection) {
            super(connection);
        }

        @Override
        protected void handleException(final SQLException exception) throws SQLException {
            handled = exception;
            super.handleException(exception);
        }
    }

    private static Stream<CacheConfig> configurations() {
        return IntStream.range(0, 16).mapToObj(CacheConfig::new).filter(config -> config.nested || !config.innerEnabled);
    }

    private static Stream<Arguments> failureCases() {
        return propertyChanges().flatMap(change -> configurations().flatMap(config -> Stream.of(FailurePoint.values())
                .flatMap(point -> Stream.of(FailureKind.values()).map(kind -> Arguments.of(change, config, point, kind)))));
    }

    private static Stream<Arguments> poolCases() {
        return Stream.of(CachedProperty.values()).flatMap(property -> Stream.of(FailurePoint.AFTER_SET, FailurePoint.GETTER)
                .flatMap(point -> Stream.of(FailureKind.values()).map(kind -> Arguments.of(property, point, kind))));
    }

    private static Stream<PropertyChange> propertyChanges() {
        return Stream.of(CachedProperty.values()).flatMap(property -> {
            final Stream<PropertyChange> nonNull = Stream.of(new PropertyChange(property, property.initial, property.updated),
                    new PropertyChange(property, property.updated, property.initial), new PropertyChange(property, property.initial, property.initial),
                    new PropertyChange(property, property.updated, property.updated));
            if (property == CachedProperty.CATALOG || property == CachedProperty.SCHEMA) {
                return Stream.concat(nonNull, Stream.of(new PropertyChange(property, property.initial, null),
                        new PropertyChange(property, null, property.updated), new PropertyChange(property, null, null)));
            }
            return nonNull;
        });
    }

    private static Stream<Arguments> successCases() {
        return propertyChanges().flatMap(change -> configurations()
                .flatMap(config -> Stream.of(false, true).map(ignoreWrites -> Arguments.of(change, config, ignoreWrites))));
    }

    private static TrackingConnection wrap(final StateConnection physical, final CacheConfig config) {
        Connection delegate = physical;
        if (config.nested) {
            final DelegatingConnection<Connection> inner = new DelegatingConnection<>(delegate);
            inner.setCacheState(config.innerEnabled);
            delegate = inner;
        }
        final TrackingConnection wrapper = new TrackingConnection(delegate);
        wrapper.setCacheState(config.enabled);
        return wrapper;
    }

    @Test
    void testAutoCommitCaching() throws SQLException {
        final Connection con = new NoReadOnlyOrAutoCommitConnection();
        final DelegatingConnection<Connection> delCon = new DelegatingConnection<>(con);

        delCon.setAutoCommit(true);

        assertFalse(con.getAutoCommit());
        assertFalse(delCon.getAutoCommit());
    }

    @ParameterizedTest
    @EnumSource(CachedProperty.class)
    void testClosedSetterDoesNotTouchDelegateOrCache(final CachedProperty property) throws SQLException {
        try (StateConnection physical = new StateConnection(); TrackingConnection wrapper = new TrackingConnection(physical)) {
            assertEquals(property.initial, property.get(wrapper));
            wrapper.setClosedInternal(true);
            assertThrows(SQLException.class, () -> property.set(wrapper, property.updated));
            assertEquals(0, physical.setterCalls[property.ordinal()]);
            assertEquals(1, physical.getterCalls[property.ordinal()]);
            assertEquals(property.initial, physical.values[property.ordinal()]);
            wrapper.setClosedInternal(false);
            assertEquals(property.initial, property.get(wrapper));
            assertEquals(1, physical.getterCalls[property.ordinal()]);
        }
    }

    @ParameterizedTest(name = "{0}; {1}; {2}; {3}")
    @MethodSource("failureCases")
    void testFailureInvalidatesOnlyUncertainState(final PropertyChange change, final CacheConfig config, final FailurePoint point,
            final FailureKind kind) throws SQLException {
        final CachedProperty property = change.property;
        try (StateConnection physical = new StateConnection(); TrackingConnection wrapper = wrap(physical, config)) {
            physical.values[property.ordinal()] = change.initial;
            if (config.primed) {
                assertEquals(change.initial, property.get(wrapper));
            }
            final Throwable failure = kind.supplier.get();
            physical.arm(property, point, failure);
            if (point == FailurePoint.GETTER && !config.hasEnabledCache()) {
                // With caching disabled there is no follow-up getter inside the setter.
                property.set(wrapper, change.requested);
                assertSame(failure, assertThrows(failure.getClass(), () -> property.get(wrapper)));
            } else {
                assertSame(failure, assertThrows(failure.getClass(), () -> property.set(wrapper, change.requested)));
            }
            assertSame(kind == FailureKind.SQL ? failure : null, wrapper.handled);
            assertEquals(1, physical.setterCalls[property.ordinal()]);
            assertNull(physical.failure, "The controlled failure must have been triggered");
            final Object actual = point == FailurePoint.BEFORE_SET ? change.initial : change.requested;
            assertEquals(actual, physical.values[property.ordinal()]);
            assertEquals(actual, property.get(wrapper), "A failed write must not leave stale cached state");
            final int getterCalls = physical.getterCalls[property.ordinal()];
            assertEquals(actual, property.get(wrapper));
            assertEquals(getterCalls + (config.hasEnabledCache() && actual != null ? 0 : 1), physical.getterCalls[property.ordinal()]);
            assertEquals(1, physical.setterCalls[property.ordinal()], "Reading state must not retry the failed setter");
        }
    }

    @ParameterizedTest
    @EnumSource(CachedProperty.class)
    void testFailurePreservesOtherCachedProperties(final CachedProperty property) throws SQLException {
        try (StateConnection physical = new StateConnection(); TrackingConnection wrapper = new TrackingConnection(physical)) {
            for (final CachedProperty cached : CachedProperty.values()) {
                assertEquals(cached.initial, cached.get(wrapper));
            }
            final AssertionError failure = new AssertionError("Controlled driver error");
            physical.arm(property, FailurePoint.AFTER_SET, failure);
            assertSame(failure, assertThrows(AssertionError.class, () -> property.set(wrapper, property.updated)));
            for (final CachedProperty cached : CachedProperty.values()) {
                assertEquals(cached == property ? cached.updated : cached.initial, cached.get(wrapper));
                assertEquals(cached == property ? 2 : 1, physical.getterCalls[cached.ordinal()]);
            }
        }
    }

    @ParameterizedTest(name = "{0}; {1}; {2}")
    @MethodSource("poolCases")
    void testPoolReuseRestoresStateAfterFailure(final CachedProperty property, final FailurePoint point, final FailureKind kind) throws Exception {
        try (StateConnection physical = new StateConnection()) {
            physical.values[CachedProperty.READ_ONLY.ordinal()] = property == CachedProperty.READ_ONLY;
            final PoolableConnectionFactory factory = new PoolableConnectionFactory(() -> physical, null);
            factory.setCacheState(true);
            factory.setDefaultAutoCommit(Boolean.TRUE);
            factory.setDefaultReadOnly(property == CachedProperty.READ_ONLY);
            factory.setDefaultCatalog((String) CachedProperty.CATALOG.initial);
            factory.setDefaultSchema((String) CachedProperty.SCHEMA.initial);
            factory.setRollbackOnReturn(true);
            factory.setAutoCommitOnReturn(true);
            try (GenericObjectPool<PoolableConnection> pool = new GenericObjectPool<>(factory)) {
                factory.setPool(pool);
                pool.setMaxTotal(1);
                final PoolableConnection first = pool.borrowObject();
                try (Connection borrowed = first) {
                    assertEquals(property.initial, property.get(borrowed));
                    if (property != CachedProperty.AUTO_COMMIT) {
                        borrowed.setAutoCommit(false);
                    }
                    final Throwable failure = kind.supplier.get();
                    physical.arm(property, point, failure);
                    assertSame(failure, assertThrows(failure.getClass(), () -> property.set(borrowed, property.updated)));
                    assertEquals(property.updated, physical.values[property.ordinal()]);
                }
                assertEquals(1, physical.rollbacks, "Returning the connection must not skip rollback because of a stale cache");
                assertEquals(Boolean.TRUE, physical.values[CachedProperty.AUTO_COMMIT.ordinal()]);
                assertEquals(0, pool.getNumActive());
                assertEquals(1, pool.getNumIdle());
                try (PoolableConnection second = pool.borrowObject()) {
                    assertSame(first, second);
                    assertEquals(property.initial, physical.values[property.ordinal()], "Activation must restore the configured default");
                    assertEquals(property.initial, property.get(second));
                }
            }
        }
    }

    @ParameterizedTest(name = "{0}; {1}; {2}")
    @MethodSource("poolCases")
    void testReadFailureKeepsCacheInvalid(final CachedProperty property, final FailurePoint point, final FailureKind kind) throws SQLException {
        try (StateConnection physical = new StateConnection(); TrackingConnection wrapper = new TrackingConnection(physical)) {
            assertEquals(property.initial, property.get(wrapper));
            final Throwable writeFailure = kind.supplier.get();
            physical.arm(property, point, writeFailure);
            assertSame(writeFailure, assertThrows(writeFailure.getClass(), () -> property.set(wrapper, property.updated)));
            final Throwable readFailure = kind.supplier.get();
            physical.arm(property, FailurePoint.GETTER, readFailure);
            assertSame(readFailure, assertThrows(readFailure.getClass(), () -> property.get(wrapper)));
            assertEquals(property.updated, property.get(wrapper), "A failed recovery read must not resurrect an old cached value");
        }
    }

    @Test
    void testReadOnlyCaching() throws SQLException {
        final Connection con = new NoReadOnlyOrAutoCommitConnection();
        final DelegatingConnection<Connection> delCon = new DelegatingConnection<>(con);

        delCon.setReadOnly(true);

        assertFalse(con.isReadOnly());
        assertFalse(delCon.isReadOnly());
    }

    @ParameterizedTest
    @EnumSource(CachedProperty.class)
    void testReenableCacheAfterUncachedWrite(final CachedProperty property) throws SQLException {
        try (StateConnection physical = new StateConnection(); TrackingConnection wrapper = new TrackingConnection(physical)) {
            assertEquals(property.initial, property.get(wrapper));
            wrapper.setCacheState(false);
            property.set(wrapper, property.updated);
            assertEquals(1, physical.getterCalls[property.ordinal()], "An uncached setter must not call the getter");
            wrapper.setCacheState(true);
            assertEquals(property.updated, property.get(wrapper));
            assertEquals(2, physical.getterCalls[property.ordinal()]);

            wrapper.setCacheState(false);
            final AssertionError failure = new AssertionError("Controlled driver error");
            physical.arm(property, FailurePoint.AFTER_SET, failure);
            assertSame(failure, assertThrows(AssertionError.class, () -> property.set(wrapper, property.initial)));
            wrapper.setCacheState(true);
            assertEquals(property.initial, property.get(wrapper));
            assertEquals(3, physical.getterCalls[property.ordinal()]);
        }
    }

    @Test
    void testReturnAfterAutoCommitErrorRollsBackAndRestoresState() throws Exception {
        try (Connection physical = database.connect()) {
            try (Statement statement = physical.createStatement()) {
                statement.execute("CREATE TABLE items (id INT)");
            }
            final AssertionError failure = new AssertionError("setAutoCommit failed");
            final Connection faulty = new DelegatingConnection<Connection>(physical) {
                private boolean failedOnce;

                @Override
                public void setAutoCommit(final boolean autoCommit) throws SQLException {
                    super.setAutoCommit(autoCommit);
                    if (!autoCommit && !failedOnce) {
                        failedOnce = true;
                        throw failure;
                    }
                }
            };
            final PoolableConnectionFactory factory = new PoolableConnectionFactory(() -> faulty, null);
            factory.setCacheState(true);
            factory.setDefaultAutoCommit(Boolean.TRUE);
            factory.setRollbackOnReturn(true);
            factory.setAutoCommitOnReturn(true);
            try (GenericObjectPool<PoolableConnection> connectionPool = new GenericObjectPool<>(factory)) {
                factory.setPool(connectionPool);
                connectionPool.setMaxTotal(1);
                final PoolableConnection first = connectionPool.borrowObject();
                try (Connection connection = first) {
                    assertTrue(connection.getAutoCommit());
                    assertSame(failure, assertThrows(AssertionError.class, () -> connection.setAutoCommit(false)));
                    assertFalse(physical.getAutoCommit());
                    // DBCP-522: An application may catch the Error and continue using the connection.
                    try (Statement statement = connection.createStatement()) {
                        statement.executeUpdate("INSERT INTO items VALUES (1)");
                    }
                }

                assertEquals(0, connectionPool.getNumActive());
                assertEquals(1, connectionPool.getNumIdle());
                assertTrue(physical.getAutoCommit(), "Returning the connection should restore auto-commit");
                try (PoolableConnection second = connectionPool.borrowObject()) {
                    assertSame(first, second, "The test must reuse the same pooled connection");
                    assertTrue(second.getAutoCommit());
                    try (Statement statement = second.createStatement(); ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM items")) {
                        assertTrue(resultSet.next());
                        assertEquals(0, resultSet.getInt(1), "Returning the connection should roll back the previous borrower's transaction");
                    }
                }
            }
        }
    }
    @Test
    void testSchemaGetterCompatibilityFallbackDoesNotRetainOldCache() throws SQLException {
        try (StateConnection physical = new StateConnection(); TrackingConnection wrapper = new TrackingConnection(physical)) {
            assertEquals(CachedProperty.SCHEMA.initial, wrapper.getSchema());
            physical.arm(CachedProperty.SCHEMA, FailurePoint.GETTER, new AbstractMethodError("getSchema is unavailable"));
            wrapper.setSchema((String) CachedProperty.SCHEMA.updated);
            assertNull(physical.failure);
            assertEquals(CachedProperty.SCHEMA.updated, wrapper.getSchema());
            assertEquals(3, physical.getterCalls[CachedProperty.SCHEMA.ordinal()]);
        }
    }

    @Test
    void testSchemaSetterCompatibilityFallbackPreservesActualState() throws SQLException {
        try (StateConnection physical = new StateConnection(); TrackingConnection wrapper = new TrackingConnection(physical)) {
            assertEquals(CachedProperty.SCHEMA.initial, wrapper.getSchema());
            physical.arm(CachedProperty.SCHEMA, FailurePoint.BEFORE_SET, new AbstractMethodError("setSchema is unavailable"));
            wrapper.setSchema((String) CachedProperty.SCHEMA.updated);
            assertNull(physical.failure);
            assertEquals(CachedProperty.SCHEMA.initial, physical.values[CachedProperty.SCHEMA.ordinal()]);
            assertEquals(CachedProperty.SCHEMA.initial, wrapper.getSchema());
            assertEquals(2, physical.getterCalls[CachedProperty.SCHEMA.ordinal()]);
        }
    }

    @Test
    void testSetAutoCommitErrorAfterPhysicalChange() throws SQLException {
        try (Connection physical = database.connect()) {
            physical.setAutoCommit(true);
            final AssertionError failure = new AssertionError("setAutoCommit failed");
            final Connection faulty = new DelegatingConnection<Connection>(physical) {
                @Override
                public void setAutoCommit(final boolean autoCommit) throws SQLException {
                    super.setAutoCommit(autoCommit);
                    throw failure;
                }
            };
            try (DelegatingConnection<Connection> outer = new DelegatingConnection<>(faulty)) {
                assertTrue(outer.getAutoCommit());
                final AssertionError thrown = assertThrows(AssertionError.class, () -> outer.setAutoCommit(false));
                assertSame(failure, thrown);
                assertFalse(physical.getAutoCommit());
                assertFalse(outer.getAutoCommit());
            }
        }
    }

    @Test
    void testSetAutoCommitErrorFromDelegateGetter() throws SQLException {
        try (Connection physical = database.connect()) {
            physical.setAutoCommit(true);
            final AssertionError failure = new AssertionError("getAutoCommit failed");
            final Connection faulty = new DelegatingConnection<Connection>(physical) {
                private boolean failedOnce;

                @Override
                public boolean getAutoCommit() throws SQLException {
                    if (!failedOnce && !physical.getAutoCommit()) {
                        failedOnce = true;
                        throw failure;
                    }
                    return physical.getAutoCommit();
                }
            };
            try (DelegatingConnection<Connection> outer = new DelegatingConnection<>(faulty)) {
                assertTrue(outer.getAutoCommit());
                final AssertionError thrown = assertThrows(AssertionError.class, () -> outer.setAutoCommit(false));
                assertSame(failure, thrown);
                assertFalse(physical.getAutoCommit());
                assertFalse(outer.getAutoCommit());
            }
        }
    }

    @Test
    void testSetAutoCommitErrorWithCacheStateDisabled() throws SQLException {
        try (Connection physical = database.connect()) {
            physical.setAutoCommit(true);
            final AssertionError failure = new AssertionError("setAutoCommit failed");
            final DelegatingConnection<Connection> faulty = new DelegatingConnection<Connection>(physical) {
                @Override
                public void setAutoCommit(final boolean autoCommit) throws SQLException {
                    super.setAutoCommit(autoCommit);
                    throw failure;
                }
            };
            faulty.setCacheState(false);
            try (DelegatingConnection<DelegatingConnection<Connection>> outer = new DelegatingConnection<>(faulty)) {
                outer.setCacheState(false);
                assertTrue(outer.getAutoCommit());
                final AssertionError thrown = assertThrows(AssertionError.class, () -> outer.setAutoCommit(false));
                assertSame(failure, thrown);
                assertFalse(physical.getAutoCommit());
                assertFalse(outer.getAutoCommit());
            }
        }
    }

    @Test
    void testSetAutoCommitSqlExceptionAfterPhysicalChange() throws SQLException {
        try (Connection physical = database.connect()) {
            physical.setAutoCommit(true);
            final SQLException failure = new SQLException("setAutoCommit failed");
            final Connection faulty = new DelegatingConnection<Connection>(physical) {
                @Override
                public void setAutoCommit(final boolean autoCommit) throws SQLException {
                    super.setAutoCommit(autoCommit);
                    throw failure;
                }
            };
            try (DelegatingConnection<Connection> outer = new DelegatingConnection<>(faulty)) {
                assertTrue(outer.getAutoCommit());
                final SQLException thrown = assertThrows(SQLException.class, () -> outer.setAutoCommit(false));
                assertSame(failure, thrown);
                assertFalse(physical.getAutoCommit());
                assertFalse(outer.getAutoCommit());
            }
        }
    }

    @Test
    void testSetAutoCommitSuccessUpdatesCache() throws SQLException {
        try (Connection physical = database.connect()) {
            final AtomicInteger getAutoCommitCalls = new AtomicInteger();
            final Connection counting = new DelegatingConnection<Connection>(physical) {
                @Override
                public boolean getAutoCommit() throws SQLException {
                    getAutoCommitCalls.incrementAndGet();
                    return physical.getAutoCommit();
                }
            };
            try (DelegatingConnection<Connection> outer = new DelegatingConnection<>(counting)) {
                assertTrue(outer.getAutoCommit());
                assertEquals(1, getAutoCommitCalls.get());

                outer.setAutoCommit(false);
                assertEquals(2, getAutoCommitCalls.get());
                assertFalse(physical.getAutoCommit());
                assertFalse(outer.getAutoCommit());
                assertEquals(2, getAutoCommitCalls.get(), "The getter should use the state cached by the setter");

                outer.setAutoCommit(true);
                assertEquals(3, getAutoCommitCalls.get());
                assertTrue(physical.getAutoCommit());
                assertTrue(outer.getAutoCommit());
                assertEquals(3, getAutoCommitCalls.get(), "The getter should use the updated cached state");
            }
        }
    }

    @ParameterizedTest(name = "{0}; {1}; ignored write={2}")
    @MethodSource("successCases")
    void testSuccessfulWriteCachesActualDriverState(final PropertyChange change, final CacheConfig config, final boolean ignoreWrites) throws SQLException {
        final CachedProperty property = change.property;
        try (StateConnection physical = new StateConnection(); TrackingConnection wrapper = wrap(physical, config)) {
            physical.values[property.ordinal()] = change.initial;
            physical.ignoreWrites = ignoreWrites;
            if (config.primed) {
                assertEquals(change.initial, property.get(wrapper));
            }
            final int beforeSetter = physical.getterCalls[property.ordinal()];
            property.set(wrapper, change.requested);
            assertEquals(1, physical.setterCalls[property.ordinal()]);
            final Object actual = ignoreWrites ? change.initial : change.requested;
            assertEquals(actual, physical.values[property.ordinal()]);
            final int afterSetter = physical.getterCalls[property.ordinal()];
            if (config.hasEnabledCache()) {
                assertTrue(afterSetter > beforeSetter, "The setter must read the actual driver state");
            } else {
                assertEquals(beforeSetter, afterSetter);
            }
            assertEquals(actual, property.get(wrapper));
            assertEquals(actual, property.get(wrapper));
            assertEquals(afterSetter + (config.hasEnabledCache() && actual != null ? 0 : 2), physical.getterCalls[property.ordinal()],
                    "Only non-null values should be served from an enabled cache");
        }
    }
}
