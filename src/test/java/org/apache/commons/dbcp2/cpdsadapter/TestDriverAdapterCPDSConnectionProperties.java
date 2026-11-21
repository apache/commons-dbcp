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

package org.apache.commons.dbcp2.cpdsadapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.sql.PooledConnection;

import org.apache.commons.dbcp2.Constants;
import org.apache.commons.dbcp2.TesterConnection;
import org.apache.commons.dbcp2.TesterDriver;
import org.apache.commons.dbcp2.datasources.PerUserPoolDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests credential isolation and connection-property compatibility in {@link DriverAdapterCPDS}.
 */
class TestDriverAdapterCPDSConnectionProperties {

    private enum PropertiesMode {
        ABSENT, EMPTY, EXPLICIT, INHERITED
    }

    private static final class RecordingDriver extends TesterDriver implements AutoCloseable {

        private final String url = "jdbc:apache:commons:connection-properties:" + UUID.randomUUID();
        private final TesterDriver delegate = new TesterDriver();
        private final List<Properties> requests = new CopyOnWriteArrayList<>();
        private final List<Connection> connections = new CopyOnWriteArrayList<>();
        private Consumer<Properties> beforeConnect = properties -> { };
        private boolean authenticate = true;

        private RecordingDriver() throws SQLException {
            DriverManager.registerDriver(this);
        }

        @Override
        public boolean acceptsURL(final String connectionUrl) {
            return url.equals(connectionUrl);
        }

        @Override
        public void close() throws SQLException {
            try {
                for (final Connection connection : connections) {
                    connection.close();
                }
            } finally {
                DriverManager.deregisterDriver(this);
            }
        }

        @Override
        public Connection connect(final String connectionUrl, final Properties info) throws SQLException {
            if (!acceptsURL(connectionUrl)) {
                return null;
            }
            requests.add(info);
            beforeConnect.accept(info);
            // Property-forwarding tests also exercise credentials the authenticating test driver does not accept.
            final Connection connection = delegate.connect("jdbc:apache:commons:testdriver", authenticate ? info : null);
            connections.add(connection);
            return connection;
        }
    }

    private static void assertConcurrentUsers(final RecordingDriver driver, final Callable<String> first, final Callable<String> second) throws Exception {
        final CountDownLatch firstEntered = new CountDownLatch(1);
        final CountDownLatch secondFinished = new CountDownLatch(1);
        driver.beforeConnect = properties -> {
            if ("u1".equals(properties.getProperty(Constants.KEY_USER))) {
                firstEntered.countDown();
                try {
                    assertTrue(secondFinished.await(10, TimeUnit.SECONDS), "Second user's connection did not complete");
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(e);
                }
            }
        };
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            final Future<String> firstUser = executor.submit(first);
            assertTrue(firstEntered.await(10, TimeUnit.SECONDS), "First user's connection did not reach the driver");
            assertEquals("u2", second.call());
            secondFinished.countDown();
            assertEquals("u1", firstUser.get(10, TimeUnit.SECONDS));
        } finally {
            secondFinished.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS), "Connection task did not terminate");
        }
    }

    private static void assertCredentials(final Properties properties, final String user, final String password) {
        assertEquals(user, properties.getProperty(Constants.KEY_USER));
        assertEquals(password, properties.getProperty(Constants.KEY_PASSWORD));
    }

    private static Stream<Arguments> credentialArguments() {
        return Stream.of(PropertiesMode.values()).flatMap(mode -> Stream.of(null, "", "u2").flatMap(user ->
                Stream.of(null, "", "p2").flatMap(password -> Stream.of(false, true).flatMap(poolStatements ->
                        Stream.of(false, true).map(useDefaultCredentials -> Arguments.of(mode, user, password, poolStatements, useDefaultCredentials))))));
    }

    private static String getUserName(final Connection connection) throws SQLException {
        try (Connection conn = connection) {
            return conn.unwrap(TesterConnection.class).getUserName();
        }
    }

    private static String getUserName(final PooledConnection connection) throws SQLException {
        try {
            return getUserName(connection.getConnection());
        } finally {
            connection.close();
        }
    }

    private static DriverAdapterCPDS newDataSource(final RecordingDriver driver, final Properties properties) {
        final DriverAdapterCPDS dataSource = new DriverAdapterCPDS();
        dataSource.setUrl(driver.url);
        dataSource.setUser("foo");
        dataSource.setPassword("bar");
        dataSource.setConnectionProperties(properties);
        // These tests inspect the physical connection's authenticated user.
        dataSource.setAccessToUnderlyingConnectionAllowed(true);
        return dataSource;
    }

    private static Properties newProperties(final PropertiesMode mode) {
        if (mode == PropertiesMode.ABSENT) {
            return null;
        }
        if (mode == PropertiesMode.EMPTY) {
            return new Properties();
        }
        final Properties credentials = new Properties();
        credentials.setProperty(Constants.KEY_USER, "foo");
        credentials.setProperty(Constants.KEY_PASSWORD, "bar");
        final Properties properties;
        if (mode == PropertiesMode.INHERITED) {
            credentials.setProperty("inheritedOption", "preserved");
            properties = new Properties(credentials);
        } else {
            properties = credentials;
        }
        properties.setProperty("ssl", "true");
        return properties;
    }

    private static Stream<Arguments> retryArguments() {
        return Stream.of(PropertiesMode.values()).flatMap(mode -> Stream.of(1, 2).map(failures -> Arguments.of(mode, failures)));
    }

    @ParameterizedTest
    @EnumSource(PropertiesMode.class)
    void testConcurrentConnections(final PropertiesMode mode) throws Exception {
        try (RecordingDriver driver = new RecordingDriver()) {
            final DriverAdapterCPDS dataSource = newDataSource(driver, newProperties(mode));
            assertConcurrentUsers(driver, () -> getUserName(dataSource.getPooledConnection("u1", "p1")),
                    () -> getUserName(dataSource.getPooledConnection("u2", "p2")));
        }
    }

    @ParameterizedTest
    @EnumSource(PropertiesMode.class)
    void testConcurrentPerUserPoolGrowth(final PropertiesMode mode) throws Exception {
        try (RecordingDriver driver = new RecordingDriver(); PerUserPoolDataSource dataSource = new PerUserPoolDataSource()) {
            dataSource.setConnectionPoolDataSource(newDataSource(driver, newProperties(mode)));
            dataSource.setDefaultMaxIdle(0);
            dataSource.setDefaultMaxTotal(2);
            // Initialize both user pools before concurrent physical connection creation.
            dataSource.getConnection("u1", "p1").close();
            dataSource.getConnection("u2", "p2").close();
            assertConcurrentUsers(driver, () -> getUserName(dataSource.getConnection("u1", "p1")),
                    () -> getUserName(dataSource.getConnection("u2", "p2")));
        }
    }

    @ParameterizedTest(name = "{0}, user={1}, password={2}, poolStatements={3}, defaultCredentials={4}")
    @MethodSource("credentialArguments")
    void testCredentialOverrides(final PropertiesMode mode, final String user, final String password, final boolean poolStatements,
            final boolean useDefaultCredentials) throws Exception {
        try (RecordingDriver driver = new RecordingDriver()) {
            driver.authenticate = false;
            final Properties properties = newProperties(mode);
            final DriverAdapterCPDS dataSource = newDataSource(driver, properties);
            dataSource.setPoolPreparedStatements(poolStatements);
            if (useDefaultCredentials) {
                dataSource.setUser(user);
                dataSource.setPassword(password);
            }
            final Properties original = properties == null ? null : (Properties) properties.clone();
            if (useDefaultCredentials) {
                dataSource.getPooledConnection().close();
            } else {
                dataSource.getPooledConnection(user, password).close();
            }
            assertEquals(1, driver.requests.size());
            final Properties request = driver.requests.get(0);
            assertNotSame(properties, request);
            assertEquals(user, request.get(Constants.KEY_USER));
            assertEquals(password, request.get(Constants.KEY_PASSWORD));
            assertCredentials(request, user == null && mode == PropertiesMode.INHERITED ? "foo" : user,
                    password == null && mode == PropertiesMode.INHERITED ? "bar" : password);
            assertEquals(properties == null ? null : properties.getProperty("ssl"), request.getProperty("ssl"));
            assertEquals(mode == PropertiesMode.INHERITED ? "preserved" : null, request.getProperty("inheritedOption"));
            assertSame(properties, dataSource.getConnectionProperties());
            assertEquals(original, properties);
            assertEquals(useDefaultCredentials ? user : "foo", dataSource.getUser());
            assertEquals(useDefaultCredentials ? password : "bar", dataSource.getPassword());
        }
    }

    @ParameterizedTest
    @EnumSource(PropertiesMode.class)
    void testDefaultCredentialsAfterExplicitCredentials(final PropertiesMode mode) throws Exception {
        try (RecordingDriver driver = new RecordingDriver()) {
            final Properties properties = newProperties(mode);
            final DriverAdapterCPDS dataSource = newDataSource(driver, properties);
            final Properties original = properties == null ? null : (Properties) properties.clone();
            assertEquals("u1", getUserName(dataSource.getPooledConnection("u1", "p1")));
            assertEquals("foo", getUserName(dataSource.getPooledConnection()));
            assertEquals("u2", getUserName(dataSource.getPooledConnection("u2", "p2")));
            assertEquals("foo", getUserName(dataSource.getPooledConnection()));
            assertCredentials(driver.requests.get(0), "u1", "p1");
            assertCredentials(driver.requests.get(1), "foo", "bar");
            assertCredentials(driver.requests.get(2), "u2", "p2");
            assertCredentials(driver.requests.get(3), "foo", "bar");
            assertEquals(original, properties);
            for (int i = 1; i < driver.requests.size(); i++) {
                assertNotSame(driver.requests.get(i - 1), driver.requests.get(i));
            }
        }
    }

    @Test
    void testDriverMutationsStayInRequestProperties() throws Exception {
        try (RecordingDriver driver = new RecordingDriver()) {
            final Properties properties = newProperties(PropertiesMode.EXPLICIT);
            final DriverAdapterCPDS dataSource = newDataSource(driver, properties);
            driver.beforeConnect = request -> {
                assertNull(request.getProperty("driverOption"));
                request.setProperty("driverOption", "modified");
                request.remove("ssl");
            };
            assertEquals("u1", getUserName(dataSource.getPooledConnection("u1", "p1")));
            assertEquals("u2", getUserName(dataSource.getPooledConnection("u2", "p2")));
            assertNull(properties.getProperty("driverOption"));
            assertEquals("true", properties.getProperty("ssl"));
            assertCredentials(properties, "foo", "bar");
        }
    }

    @ParameterizedTest
    @MethodSource("retryArguments")
    void testRetry(final PropertiesMode mode, final int failures) throws Exception {
        try (RecordingDriver driver = new RecordingDriver()) {
            final Properties properties = newProperties(mode);
            final DriverAdapterCPDS dataSource = newDataSource(driver, properties);
            final Properties original = properties == null ? null : (Properties) properties.clone();
            final AtomicInteger attempts = new AtomicInteger();
            driver.beforeConnect = request -> {
                if (attempts.incrementAndGet() <= failures) {
                    throw new ClassCircularityError("Simulated driver class loading failure");
                }
            };
            if (failures == 1) {
                assertEquals("u1", getUserName(dataSource.getPooledConnection("u1", "p1")));
            } else {
                assertThrows(ClassCircularityError.class, () -> dataSource.getPooledConnection("u1", "p1"));
            }
            assertEquals(2, attempts.get());
            assertEquals(2, driver.requests.size());
            driver.requests.forEach(request -> assertCredentials(request, "u1", "p1"));
            assertEquals(original, properties);
        }
    }

    @ParameterizedTest
    @EnumSource(PropertiesMode.class)
    void testSqlExceptionDoesNotChangeConfigurationOrRetry(final PropertiesMode mode) throws Exception {
        try (RecordingDriver driver = new RecordingDriver()) {
            final Properties properties = newProperties(mode);
            final DriverAdapterCPDS dataSource = newDataSource(driver, properties);
            final Properties original = properties == null ? null : (Properties) properties.clone();
            assertThrows(SQLException.class, () -> dataSource.getPooledConnection("u1", "incorrect"));
            assertEquals(1, driver.requests.size());
            assertEquals(original, properties);
            assertEquals("foo", getUserName(dataSource.getPooledConnection()));
        }
    }
}
