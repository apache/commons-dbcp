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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.sql.Wrapper;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import javax.sql.PooledConnection;

import org.apache.commons.dbcp2.cpdsadapter.DriverAdapterCPDS;
import org.apache.commons.dbcp2.managed.BasicManagedDataSource;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.geronimo.transaction.manager.TransactionManagerImpl;
import org.h2.Driver;
import org.h2.jdbc.JdbcCallableStatement;
import org.h2.jdbc.JdbcConnection;
import org.h2.jdbc.JdbcDatabaseMetaData;
import org.h2.jdbc.JdbcPreparedStatement;
import org.h2.jdbc.JdbcResultSet;
import org.h2.jdbc.JdbcStatement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Exercises delegate access through every pool guard and JDBC wrapper, with and without statement pooling and nested wrappers.
 */
class TestUnderlyingConnectionAccess {

    private enum CloseWith {
        RESULT_SET, STATEMENT, CONNECTION
    }

    private static final class Fixture implements AutoCloseable {
        private final Connection connection;
        private final AutoCloseable owner;

        private Fixture(final Connection connection, final AutoCloseable owner, final boolean nested) {
            this.connection = nested ? new DelegatingConnection<>(new DelegatingConnection<>(connection)) : connection;
            this.owner = owner;
        }

        @Override
        public void close() throws Exception {
            try {
                connection.close();
            } finally {
                owner.close();
            }
        }
    }

    private enum Source {
        BASIC, POOLING_DATA_SOURCE, POOLING_DRIVER, MANAGED, CPDS
    }

    private enum StatementKind {
        STATEMENT, STATEMENT_TYPE, STATEMENT_HOLDABILITY,
        PREPARED, PREPARED_TYPE, PREPARED_HOLDABILITY, PREPARED_KEYS, PREPARED_INDEXES, PREPARED_NAMES,
        CALLABLE, CALLABLE_TYPE, CALLABLE_HOLDABILITY;

        private Statement create(final Connection connection) throws SQLException {
            switch (this) {
            case STATEMENT:
                return connection.createStatement();
            case STATEMENT_TYPE:
                return connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            case STATEMENT_HOLDABILITY:
                return connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT);
            case PREPARED:
                return connection.prepareStatement(SELECT_SQL);
            case PREPARED_TYPE:
                return connection.prepareStatement(SELECT_SQL, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            case PREPARED_HOLDABILITY:
                return connection.prepareStatement(SELECT_SQL, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT);
            case PREPARED_KEYS:
                return connection.prepareStatement(SELECT_SQL, Statement.RETURN_GENERATED_KEYS);
            case PREPARED_INDEXES:
                return connection.prepareStatement(SELECT_SQL, new int[] {1});
            case PREPARED_NAMES:
                return connection.prepareStatement(SELECT_SQL, new String[] {"ID"});
            case CALLABLE:
                return connection.prepareCall(CALL_SQL);
            case CALLABLE_TYPE:
                return connection.prepareCall(CALL_SQL, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            case CALLABLE_HOLDABILITY:
                return connection.prepareCall(CALL_SQL, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT);
            default:
                throw new IllegalStateException(name());
            }
        }
    }

    private static final AtomicInteger IDS = new AtomicInteger();
    private static final String SELECT_SQL = "SELECT ? AS ID";
    private static final String CALL_SQL = "{? = call ABS(?)}";

    private static void assertAccess(final Wrapper wrapper, final Class<?> standardType, final Class<?> driverType, final boolean allowed)
            throws SQLException {
        assertTrue(wrapper.isWrapperFor(standardType));
        assertSame(wrapper, wrapper.unwrap(standardType));
        assertTrue(wrapper.isWrapperFor(wrapper.getClass()));
        assertSame(wrapper, wrapper.unwrap(wrapper.getClass()));
        assertEquals(allowed, wrapper.isWrapperFor(driverType));
        if (allowed) {
            assertNotNull(wrapper.unwrap(driverType));
        } else {
            assertThrows(SQLException.class, () -> wrapper.unwrap(driverType));
        }
    }

    private static void assertConnectionAccess(final Connection connection, final boolean allowed) throws SQLException {
        assertAccess(connection, Connection.class, JdbcConnection.class, allowed);
        final DelegatingConnection<?> delegate = (DelegatingConnection<?>) connection;
        if (allowed) {
            assertNotNull(delegate.getDelegate());
            assertSame(connection.unwrap(JdbcConnection.class), delegate.getInnermostDelegate());
        } else {
            assertNull(delegate.getInnermostDelegate());
        }
    }

    private static void assertMetadataAccess(final DatabaseMetaData metadata, final boolean allowed) throws SQLException {
        assertAccess(metadata, DatabaseMetaData.class, JdbcDatabaseMetaData.class, allowed);
        final DelegatingDatabaseMetaData delegate = (DelegatingDatabaseMetaData) metadata;
        if (allowed) {
            assertNotNull(delegate.getDelegate());
            assertSame(metadata.unwrap(JdbcDatabaseMetaData.class), delegate.getInnermostDelegate());
        } else {
            assertNull(delegate.getInnermostDelegate());
        }
    }

    private static void assertResultSetAccess(final ResultSet resultSet, final boolean allowed) throws SQLException {
        assertAccess(resultSet, ResultSet.class, JdbcResultSet.class, allowed);
        final DelegatingResultSet delegate = (DelegatingResultSet) resultSet;
        if (allowed) {
            assertNotNull(delegate.getDelegate());
            assertSame(resultSet.unwrap(JdbcResultSet.class), delegate.getInnermostDelegate());
        } else {
            assertNull(delegate.getInnermostDelegate());
        }
    }

    private static void assertStatementAccess(final Statement statement, final boolean allowed) throws SQLException {
        final Class<?> standardType = statement instanceof CallableStatement ? CallableStatement.class
                : statement instanceof PreparedStatement ? PreparedStatement.class : Statement.class;
        final Class<?> driverType = statement instanceof CallableStatement ? JdbcCallableStatement.class
                : statement instanceof PreparedStatement ? JdbcPreparedStatement.class : JdbcStatement.class;
        assertAccess(statement, standardType, driverType, allowed);
        final DelegatingStatement delegate = (DelegatingStatement) statement;
        if (allowed) {
            assertNotNull(delegate.getDelegate());
            assertSame(statement.unwrap(driverType), delegate.getInnermostDelegate());
        } else {
            assertNull(delegate.getInnermostDelegate());
        }
    }

    private static Stream<Arguments> closeArguments() {
        return Arrays.stream(Source.values()).flatMap(source -> Stream.of(false, true).flatMap(allowed ->
            Stream.of(false, true).flatMap(nested ->
                Stream.of(StatementKind.STATEMENT, StatementKind.PREPARED, StatementKind.CALLABLE).flatMap(kind ->
                    Arrays.stream(CloseWith.values()).map(closeWith -> Arguments.of(source, allowed, nested, kind, closeWith))))));
    }

    private static void configure(final BasicDataSource dataSource, final boolean allowed, final boolean poolStatements) {
        dataSource.setDriver(new Driver());
        dataSource.setUrl(newUrl());
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        dataSource.setAccessToUnderlyingConnectionAllowed(allowed);
        dataSource.setPoolPreparedStatements(poolStatements);
    }

    private static Fixture createFixture(final Source source, final boolean allowed, final boolean poolStatements, final boolean nested) throws Exception {
        if (source == Source.BASIC || source == Source.MANAGED) {
            final BasicDataSource dataSource;
            if (source == Source.MANAGED) {
                final BasicManagedDataSource managed = new BasicManagedDataSource();
                managed.setTransactionManager(new TransactionManagerImpl());
                dataSource = managed;
            } else {
                dataSource = new BasicDataSource();
            }
            configure(dataSource, allowed, poolStatements);
            return new Fixture(dataSource.getConnection(), dataSource, nested);
        }
        if (source == Source.CPDS) {
            final DriverAdapterCPDS dataSource = new DriverAdapterCPDS();
            dataSource.setDriver(Driver.class.getName());
            dataSource.setUrl(newUrl());
            dataSource.setAccessToUnderlyingConnectionAllowed(allowed);
            dataSource.setPoolPreparedStatements(poolStatements);
            final PooledConnection pooledConnection = dataSource.getPooledConnection("sa", "");
            return new Fixture(pooledConnection.getConnection(), pooledConnection::close, nested);
        }
        final GenericObjectPool<PoolableConnection> pool = newPool(poolStatements);
        if (source == Source.POOLING_DATA_SOURCE) {
            final PoolingDataSource<PoolableConnection> dataSource = new PoolingDataSource<>(pool);
            dataSource.setAccessToUnderlyingConnectionAllowed(allowed);
            return new Fixture(dataSource.getConnection(), dataSource, nested);
        }
        final PoolingDriver driver = PoolingDriver.builder().setAccessToUnderlyingConnectionAllowed(allowed).get();
        final String name = "underlying-access-" + IDS.incrementAndGet();
        driver.registerPool(name, pool);
        return new Fixture(driver.connect(PoolingDriver.URL_PREFIX + name, new Properties()), () -> driver.closePool(name), nested);
    }

    private static ResultSet execute(final Statement statement) throws SQLException {
        if (statement instanceof CallableStatement) {
            final CallableStatement callable = (CallableStatement) statement;
            callable.registerOutParameter(1, Types.INTEGER);
            callable.setInt(2, -7);
            assertTrue(callable.execute());
            return callable.getResultSet();
        }
        if (statement instanceof PreparedStatement) {
            final PreparedStatement prepared = (PreparedStatement) statement;
            prepared.setInt(1, 7);
            return prepared.executeQuery();
        }
        return statement.executeQuery("SELECT 7");
    }

    private static GenericObjectPool<PoolableConnection> newPool(final boolean poolStatements) {
        final Properties properties = new Properties();
        properties.setProperty("user", "sa");
        final PoolableConnectionFactory factory = new PoolableConnectionFactory(new DriverConnectionFactory(new Driver(), newUrl(), properties), null);
        factory.setPoolStatements(poolStatements);
        final GenericObjectPoolConfig<PoolableConnection> config = new GenericObjectPoolConfig<>();
        config.setJmxEnabled(false);
        final GenericObjectPool<PoolableConnection> pool = new GenericObjectPool<>(factory, config);
        factory.setPool(pool);
        return pool;
    }

    private static String newUrl() {
        return "jdbc:h2:mem:underlying-access-" + IDS.incrementAndGet();
    }

    private static Stream<Arguments> statementArguments() {
        return Arrays.stream(Source.values()).flatMap(source -> Stream.of(false, true).flatMap(allowed ->
            Stream.of(false, true).flatMap(poolStatements -> Stream.of(false, true).flatMap(nested ->
                Arrays.stream(StatementKind.values()).map(kind -> Arguments.of(source, allowed, poolStatements, nested, kind))))));
    }

    @ParameterizedTest
    @MethodSource("statementArguments")
    void testAccessAndExecution(final Source source, final boolean allowed, final boolean poolStatements, final boolean nested,
            final StatementKind kind) throws Exception {
        try (Fixture fixture = createFixture(source, allowed, poolStatements, nested)) {
            final Connection connection = fixture.connection;
            assertConnectionAccess(connection, allowed);
            assertFalse(connection.isWrapperFor(String.class));
            assertThrows(SQLException.class, () -> connection.unwrap(String.class));
            try (Statement statement = kind.create(connection)) {
                assertSame(connection, statement.getConnection());
                assertStatementAccess(statement, allowed);
                assertNotEquals("NULL", statement.toString());
                final ResultSet resultSet = execute(statement);
                try (ResultSet ignored = resultSet) {
                    assertSame(statement, resultSet.getStatement());
                    assertResultSetAccess(resultSet, allowed);
                    assertTrue(resultSet.next());
                    assertEquals(7, resultSet.getInt(1));
                    if (statement instanceof CallableStatement) {
                        assertEquals(7, ((CallableStatement) statement).getInt(1));
                    }
                    if (!allowed && !nested) {
                        assertNull(((DelegatingConnection<?>) connection).getDelegate());
                        assertNull(((DelegatingStatement) statement).getDelegate());
                        assertNull(((DelegatingResultSet) resultSet).getDelegate());
                    }
                }
                assertResultSetAccess(resultSet, allowed);
            }
            final DatabaseMetaData metadata = connection.getMetaData();
            assertSame(connection, metadata.getConnection());
            assertMetadataAccess(metadata, allowed);
            if (!allowed && !nested) {
                assertNull(((DelegatingDatabaseMetaData) metadata).getDelegate());
            }
            final ResultSet tables = metadata.getTables(null, null, null, null);
            try (ResultSet ignored = tables) {
                assertResultSetAccess(tables, allowed);
                assertNull(tables.getStatement());
            }
            assertResultSetAccess(tables, allowed);
        }
    }

    @ParameterizedTest
    @MethodSource("closeArguments")
    void testAccessAfterClose(final Source source, final boolean allowed, final boolean nested, final StatementKind kind,
            final CloseWith closeWith) throws Exception {
        try (Fixture fixture = createFixture(source, allowed, true, nested); Statement statement = kind.create(fixture.connection)) {
            final ResultSet resultSet = execute(statement);
            assertResultSetAccess(resultSet, allowed);
            switch (closeWith) {
            case RESULT_SET:
                resultSet.close();
                break;
            case STATEMENT:
                statement.close();
                break;
            case CONNECTION:
                fixture.connection.close();
                break;
            default:
                throw new IllegalStateException(closeWith.name());
            }
            assertResultSetAccess(resultSet, allowed);
            resultSet.close();
            assertResultSetAccess(resultSet, allowed);
            statement.close();
            assertStatementAccess(statement, false);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testAccessAfterFailedClose(final boolean allowed) throws Exception {
        try (Fixture fixture = createFixture(Source.BASIC, allowed, false, false);
                TesterConnection physical = new TesterConnection("u1", "p1"); TesterStatement physicalStatement = new TesterStatement(physical);
                DelegatingStatement statement = new DelegatingStatement((DelegatingConnection<?>) fixture.connection, physicalStatement)) {
            for (final boolean metadata : new boolean[] {false, true}) {
                final TesterResultSet physicalResultSet = new TesterResultSet(physicalStatement);
                physicalResultSet.setSqlExceptionOnClose(true);
                final DelegatingResultSet resultSet = (DelegatingResultSet) (metadata
                        ? DelegatingResultSet.wrapResultSet(fixture.connection, physicalResultSet)
                        : DelegatingResultSet.wrapResultSet(statement, physicalResultSet));
                try {
                    assertThrows(SQLException.class, resultSet::close);
                    assertFalse(physicalResultSet.isClosed());
                    assertAccess(resultSet, ResultSet.class, TesterResultSet.class, allowed);
                    if (allowed) {
                        assertSame(physical, resultSet.getInnermostDelegate().getStatement().getConnection());
                    } else {
                        assertNull(resultSet.getDelegate());
                        assertNull(resultSet.getInnermostDelegate());
                    }
                } finally {
                    physicalResultSet.setSqlExceptionOnClose(false);
                    resultSet.close();
                }
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testAccessAfterFailedStatementClose(final boolean allowed) throws Exception {
        try (Fixture fixture = createFixture(Source.BASIC, allowed, false, false);
                TesterConnection physical = new TesterConnection("u1", "p1"); TesterStatement physicalStatement = new TesterStatement(physical);
                DelegatingStatement statement = new DelegatingStatement((DelegatingConnection<?>) fixture.connection, physicalStatement)) {
            final TesterResultSet physicalResultSet = new TesterResultSet(physicalStatement);
            physicalResultSet.setSqlExceptionOnClose(true);
            final DelegatingResultSet resultSet = (DelegatingResultSet) DelegatingResultSet.wrapResultSet(statement, physicalResultSet);
            try {
                assertThrows(SQLException.class, statement::close);
                assertTrue(statement.isClosed());
                assertFalse(physicalResultSet.isClosed());
                assertAccess(resultSet, ResultSet.class, TesterResultSet.class, allowed);
                if (!allowed) {
                    assertNull(resultSet.getDelegate());
                    assertNull(resultSet.getInnermostDelegate());
                }
            } finally {
                physicalResultSet.setSqlExceptionOnClose(false);
                resultSet.close();
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testGeneratedKeys(final boolean allowed) throws Exception {
        try (Fixture fixture = createFixture(Source.BASIC, allowed, true, true);
                Statement setup = fixture.connection.createStatement()) {
            setup.execute("CREATE TABLE T (ID BIGINT GENERATED ALWAYS AS IDENTITY, VALUE_COLUMN INT)");
            try (PreparedStatement statement = fixture.connection.prepareStatement("INSERT INTO T (VALUE_COLUMN) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
                statement.setInt(1, 9);
                assertEquals(1, statement.executeUpdate());
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    assertTrue(keys.next());
                    assertEquals(1, keys.getLong(1));
                    assertSame(statement, keys.getStatement());
                    assertResultSetAccess(keys, allowed);
                }
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testInvalidateWrappedConnection(final boolean allowed) throws Exception {
        try (BasicDataSource dataSource = new BasicDataSource()) {
            configure(dataSource, allowed, true);
            try (Connection connection = dataSource.getConnection(); DelegatingConnection<Connection> nested = new DelegatingConnection<>(connection)) {
                final Connection external = (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[] {Connection.class},
                        (proxy, method, args) -> {
                            try {
                                return method.invoke(nested, args);
                            } catch (final InvocationTargetException e) {
                                throw e.getCause();
                            }
                        });
                assertEquals(1, dataSource.getNumActive());
                dataSource.invalidateConnection(external);
                assertEquals(0, dataSource.getNumActive());
                assertTrue(connection.isClosed());
            }
            dataSource.invalidateConnection(null);
            try (Connection foreign = new Driver().connect(newUrl(), new Properties())) {
                assertThrows(IllegalStateException.class, () -> dataSource.invalidateConnection(foreign));
            }
        }
    }

    @Test
    void testGuardHookAndInternalAccess() throws Exception {
        try (Connection physical = new Driver().connect(newUrl(), new Properties());
                DelegatingConnection<Connection> connection = new DelegatingConnection<Connection>(physical) {
                    @Override
                    protected boolean isAccessToUnderlyingConnectionAllowed() {
                        return false;
                    }
                }; Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery("SELECT 7")) {
            assertNull(connection.getDelegate());
            assertConnectionAccess(connection, false);
            assertStatementAccess(statement, false);
            assertResultSetAccess(resultSet, false);
            assertMetadataAccess(connection.getMetaData(), false);
            // Internal access remains available to pool lifecycle operations.
            assertSame(physical, connection.getDelegateInternal());
            assertSame(physical, connection.getInnermostDelegateInternal());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testManagedTransaction(final boolean allowed) throws Exception {
        final TransactionManagerImpl transactionManager = new TransactionManagerImpl();
        try (BasicManagedDataSource dataSource = new BasicManagedDataSource()) {
            dataSource.setTransactionManager(transactionManager);
            configure(dataSource, allowed, true);
            transactionManager.begin();
            try {
                try (Connection connection = dataSource.getConnection(); Connection shared = dataSource.getConnection();
                        PreparedStatement statement = connection.prepareStatement(SELECT_SQL)) {
                    assertConnectionAccess(connection, allowed);
                    assertConnectionAccess(shared, allowed);
                    assertMetadataAccess(shared.getMetaData(), allowed);
                    assertStatementAccess(statement, allowed);
                    try (ResultSet resultSet = execute(statement)) {
                        assertResultSetAccess(resultSet, allowed);
                        assertTrue(resultSet.next());
                        assertEquals(7, resultSet.getInt(1));
                    }
                }
                transactionManager.commit();
            } finally {
                if (transactionManager.getTransaction() != null) {
                    transactionManager.rollback();
                }
            }
            assertEquals(0, dataSource.getNumActive());
        }
    }

    @Test
    void testMutablePolicyAndClose() throws Exception {
        try (PoolingDataSource<PoolableConnection> dataSource = new PoolingDataSource<>(newPool(false));
                Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            final ResultSet resultSet = statement.executeQuery("SELECT 7");
            final DatabaseMetaData metadata = connection.getMetaData();
            assertResultSetAccess(resultSet, false);
            dataSource.setAccessToUnderlyingConnectionAllowed(true);
            assertConnectionAccess(connection, true);
            assertStatementAccess(statement, true);
            assertResultSetAccess(resultSet, true);
            assertMetadataAccess(metadata, true);
            dataSource.setAccessToUnderlyingConnectionAllowed(false);
            assertConnectionAccess(connection, false);
            assertStatementAccess(statement, false);
            assertMetadataAccess(metadata, false);
            statement.close();
            dataSource.setAccessToUnderlyingConnectionAllowed(true);
            assertResultSetAccess(resultSet, false);
        }
    }

    @Test
    void testParentlessWrappers() throws Exception {
        try (Connection physical = new Driver().connect(newUrl(), new Properties());
                DelegatingStatement statement = new DelegatingStatement(null, physical.createStatement());
                ResultSet resultSet = statement.executeQuery("SELECT 7")) {
            assertStatementAccess(statement, true);
            assertResultSetAccess(resultSet, true);
            final ResultSet metadata = DelegatingResultSet.wrapResultSet((Connection) null, physical.getMetaData().getTables(null, null, null, null));
            try (ResultSet ignored = metadata) {
                assertResultSetAccess(metadata, true);
            }
            assertResultSetAccess(metadata, true);
        }
    }

    @Test
    void testPlainWrappersAndDriverDefault() throws Exception {
        assertTrue(new PoolingDriver().isAccessToUnderlyingConnectionAllowed());
        try (Connection physical = new Driver().connect(newUrl(), new Properties());
                DelegatingConnection<Connection> connection = new DelegatingConnection<>(physical);
                Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery("SELECT 7")) {
            assertConnectionAccess(connection, true);
            assertStatementAccess(statement, true);
            assertResultSetAccess(resultSet, true);
            assertMetadataAccess(connection.getMetaData(), true);
        }
    }
}
