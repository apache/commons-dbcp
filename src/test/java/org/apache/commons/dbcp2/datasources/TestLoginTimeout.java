/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.dbcp2.datasources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.PooledConnection;

import org.apache.commons.dbcp2.TesterDriver;
import org.apache.commons.dbcp2.cpdsadapter.DriverAdapterCPDS;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Tests physical connection login timeouts, including configuration after pool initialization. */
class TestLoginTimeout {

    private static final class RecordingCpds extends ConnectionPoolDataSourceProxy {
        private int timeout = 9;
        private final List<Integer> attempts = new ArrayList<>();
        private boolean enforceTimeout;

        /**
         * Constructs a provider that records login timeouts while delegating to the test JDBC driver.
         *
         * @throws ClassNotFoundException Thrown if the test JDBC driver cannot be loaded.
         */
        RecordingCpds() throws ClassNotFoundException {
            super(new DriverAdapterCPDS());
            final DriverAdapterCPDS source = (DriverAdapterCPDS) delegate;
            source.setDriver(TesterDriver.class.getName());
            source.setUrl("jdbc:apache:commons:testdriver");
            source.setUser("u1");
            source.setPassword("p1");
        }

        /**
         * Gets the current login timeout recorded by this provider.
         *
         * @return the login timeout in seconds.
         */
        @Override
        public int getLoginTimeout() {
            return timeout;
        }

        /**
         * Gets a pooled connection after recording the current login timeout.
         *
         * @return the pooled connection using the default credentials.
         * @throws SQLException Thrown if the simulated timeout expires or the delegate cannot create a connection.
         */
        @Override
        public PooledConnection getPooledConnection() throws SQLException {
            recordAttempt();
            return super.getPooledConnection();
        }

        /**
         * Gets a pooled connection with explicit credentials after recording the current login timeout.
         *
         * @param user the user name.
         * @param password the password.
         * @return the pooled connection using the supplied credentials.
         * @throws SQLException Thrown if the simulated timeout expires or the delegate cannot create a connection.
         */
        @Override
        public PooledConnection getPooledConnection(final String user, final String password) throws SQLException {
            recordAttempt();
            return super.getPooledConnection(user, password);
        }

        /**
         * Records a connection attempt and optionally simulates expiration of a one-second timeout.
         *
         * @throws SQLTimeoutException Thrown if timeout enforcement is enabled and the current timeout is one second.
         */
        private void recordAttempt() throws SQLTimeoutException {
            attempts.add(timeout);
            if (enforceTimeout && timeout == 1) {
                throw new SQLTimeoutException("Simulated connection exceeds one second");
            }
        }

        /**
         * Sets the login timeout recorded by this provider.
         *
         * @param seconds the login timeout in seconds.
         */
        @Override
        public void setLoginTimeout(final int seconds) {
            timeout = seconds;
        }
    }

    /** Serializable provider with mutable or non-identity equality, and controllable connection creation. */
    private static final class SerializableCpds extends DriverAdapterCPDS {
        private static final long serialVersionUID = 1L;

        private final List<Integer> attempts = Collections.synchronizedList(new ArrayList<>());
        private transient CountDownLatch entered;
        private transient CountDownLatch release;
        private boolean equalToOtherProviders;

        /**
         * Constructs a serializable test provider with a nine-second default login timeout.
         *
         * @throws ClassNotFoundException Thrown if the test JDBC driver cannot be loaded.
         */
        SerializableCpds() throws ClassNotFoundException {
            setDriver(TesterDriver.class.getName());
            setUrl("jdbc:apache:commons:testdriver");
            setUser("u1");
            setPassword("p1");
            setLoginTimeout(9);
        }

        /**
         * Compares providers using the configured test equality mode and, when required, their login timeouts.
         *
         * @param other the object to compare with this provider.
         * @return true if the other provider has the same equality mode and any required timeout values match.
         */
        @Override
        public boolean equals(final Object other) {
            if (!(other instanceof SerializableCpds)) {
                return false;
            }
            final SerializableCpds provider = (SerializableCpds) other;
            return equalToOtherProviders == provider.equalToOtherProviders && (equalToOtherProviders || getLoginTimeout() == provider.getLoginTimeout());
        }

        /**
         * Gets a pooled connection after recording the timeout and optionally waiting for a test signal.
         *
         * @return the pooled connection using the default credentials.
         * @throws SQLException Thrown if waiting is interrupted or the test driver cannot create the connection.
         */
        @Override
        public PooledConnection getPooledConnection() throws SQLException {
            attempts.add(getLoginTimeout());
            if (entered != null && entered.getCount() != 0) {
                entered.countDown();
                try {
                    assertTrue(release.await(10, TimeUnit.SECONDS));
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new SQLException(e);
                }
            }
            return super.getPooledConnection();
        }

        /**
         * Gets a constant or mutable hash code according to the configured test equality mode.
         *
         * @return one when all providers in this mode compare equal, or the current login timeout otherwise.
         */
        @Override
        public int hashCode() {
            return equalToOtherProviders ? 1 : getLoginTimeout();
        }
    }

    /** Isolated JNDI lookup using only the environment supplied by each test. */
    public static final class TestContextFactory implements InitialContextFactory {
        /**
         * Gets an isolated context that resolves the provider supplied in the test environment.
         *
         * @param environment the environment containing the provider under the {@code test.cpds} key.
         * @return the context exposing the provider through the {@code timeoutTest} name.
         * @throws NamingException Thrown if configuring the context lookup fails.
         */
        @Override
        public Context getInitialContext(final Hashtable<?, ?> environment) throws NamingException {
            final Context context = mock(Context.class);
            when(context.lookup("timeoutTest")).thenReturn(environment.get("test.cpds"));
            return context;
        }
    }

    /**
     * Configures a data source to obtain its provider directly or through JNDI and retain no idle connections.
     *
     * @param source the data source to configure before its pool is initialized.
     * @param jndi whether to obtain the provider through JNDI.
     * @param provider the physical connection provider.
     */
    private static void configureDataSource(final InstanceKeyDataSource source, final boolean jndi, final ConnectionPoolDataSource provider) {
        if (jndi) {
            final Properties environment = new Properties();
            environment.setProperty(Context.INITIAL_CONTEXT_FACTORY, TestContextFactory.class.getName());
            environment.put("test.cpds", provider);
            source.setJndiEnvironment(environment);
            source.setDataSourceName("timeoutTest");
        } else {
            source.setConnectionPoolDataSource(provider);
        }
        source.setDefaultMaxIdle(0);
    }

    /**
     * Deserializes a data source from its serialized representation.
     *
     * @param bytes the serialized data source.
     * @return the restored data source.
     * @throws IOException Thrown if the serialized data cannot be read.
     * @throws ClassNotFoundException Thrown if a serialized class cannot be loaded.
     */
    private static InstanceKeyDataSource deserialize(final byte[] bytes) throws IOException, ClassNotFoundException {
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            return (InstanceKeyDataSource) input.readObject();
        }
    }

    /**
     * Supplies bounded and unbounded timeout settings for interruption tests.
     *
     * @return an unset timeout, zero, and a positive sub-second timeout.
     */
    static Stream<Duration> interruptedTimeouts() {
        return Stream.of(null, Duration.ZERO, Duration.ofMillis(100));
    }

    /**
     * Supplies pool, lookup, credential, and timeout combinations for connection tests.
     *
     * @return connection configuration combinations and their expected JDBC timeout values.
     */
    static Stream<Arguments> loginTimeouts() {
        return Stream.of(false, true).flatMap(shared -> Stream.of(false, true).flatMap(jndi -> Stream.of(false, true)
                .flatMap(credentials -> Stream.of(Arguments.of(Duration.ZERO, 0), Arguments.of(Duration.ofNanos(1), 1),
                        Arguments.of(Duration.ofMillis(1900), 1), Arguments.of(Duration.ofSeconds(5), 5),
                        Arguments.of(Duration.ofSeconds(Long.MAX_VALUE), Integer.MAX_VALUE))
                        .map(timeout -> Arguments.of(shared, jndi, credentials, timeout.get()[0], timeout.get()[1])))));
    }

    /**
     * Creates a provider test double backed by a mutable login timeout.
     *
     * @param timeout the value read and updated by the timeout accessors.
     * @return the provider test double.
     * @throws SQLException Thrown if configuring the provider test double fails.
     */
    private static ConnectionPoolDataSource mockProvider(final AtomicInteger timeout) throws SQLException {
        final ConnectionPoolDataSource provider = mock(ConnectionPoolDataSource.class);
        when(provider.getLoginTimeout()).thenAnswer(invocation -> timeout.get());
        doAnswer(invocation -> {
            timeout.set(invocation.getArgument(0));
            return null;
        }).when(provider).setLoginTimeout(anyInt());
        return provider;
    }

    /**
     * Creates a data source that obtains its provider directly or through JNDI and retains no idle connections.
     *
     * @param shared whether to use a shared pool instead of per-user pools.
     * @param jndi whether to obtain the provider through JNDI.
     * @param provider the physical connection provider.
     * @return the configured data source.
     */
    private static InstanceKeyDataSource newDataSource(final boolean shared, final boolean jndi, final ConnectionPoolDataSource provider) {
        final InstanceKeyDataSource source = shared ? new SharedPoolDataSource() : new PerUserPoolDataSource();
        configureDataSource(source, jndi, provider);
        return source;
    }

    /**
     * Supplies pool types and initial timeout settings for serialization tests.
     *
     * @return shared-pool and configured-timeout flag combinations.
     */
    static Stream<Arguments> serializationTimeouts() {
        return Stream.of(false, true).flatMap(shared -> Stream.of(false, true).map(configured -> Arguments.of(shared, configured)));
    }

    /**
     * Serializes a data source without closing it or changing its registration.
     *
     * @param source the data source to serialize.
     * @return the serialized representation.
     * @throws IOException Thrown if the data source cannot be serialized.
     */
    private static byte[] serialize(final InstanceKeyDataSource source) throws IOException {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(source);
        }
        return bytes.toByteArray();
    }

    /**
     * Supplies unchecked failures that a provider may throw while connecting.
     *
     * @return a runtime exception and an error.
     */
    static Stream<Throwable> uncheckedFailures() {
        return Stream.of(new IllegalStateException("Driver failure"), new AssertionError("Driver error"));
    }

    /**
     * Supplies pool types, provider lookup modes, and timeouts for serialization before provider configuration.
     *
     * @return configuration combinations and the timeout expected by the provider after deserialization.
     */
    static Stream<Arguments> unregisteredSerializationTimeouts() {
        return Stream.of(false, true).flatMap(shared -> Stream.of(false, true).flatMap(jndi ->
                Stream.of(Arguments.of(null, 9), Arguments.of(Duration.ZERO, 0), Arguments.of(Duration.ofMillis(100), 1),
                        Arguments.of(Duration.ofSeconds(5), 5), Arguments.of(Duration.ofSeconds(Long.MAX_VALUE), Integer.MAX_VALUE))
                        .map(timeout -> Arguments.of(shared, jndi, timeout.get()[0], timeout.get()[1]))));
    }

    /**
     * Tests that a connection failure restores the provider's previous login timeout.
     *
     * @param credentials whether to use explicit user credentials.
     * @throws SQLException Thrown if JDBC setup, an operation, or cleanup unexpectedly fails.
     */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testConnectionFailureRestoresProviderTimeout(final boolean credentials) throws SQLException {
        final AtomicInteger timeout = new AtomicInteger(9);
        final ConnectionPoolDataSource provider = mockProvider(timeout);
        final SQLTimeoutException failure = new SQLTimeoutException("Login exceeded timeout");
        when(provider.getPooledConnection()).thenThrow(failure);
        when(provider.getPooledConnection("u1", "p1")).thenThrow(failure);
        assertSame(failure, assertThrows(SQLTimeoutException.class, () -> ConnectionPoolDataSourceUtils.getPooledConnection(provider,
                credentials ? "u1" : null, "p1", Duration.ofMillis(100))));
        assertEquals(9, timeout.get());
    }

    /**
     * Tests that a timeout restoration failure is suppressed on the original connection failure.
     *
     * @throws SQLException Thrown if JDBC setup, an operation, or cleanup unexpectedly fails.
     */
    @Test
    void testConnectionFailureSuppressesRestoreFailure() throws SQLException {
        final ConnectionPoolDataSource provider = mock(ConnectionPoolDataSource.class);
        final SQLTimeoutException failure = new SQLTimeoutException("Login timed out");
        final SQLException restoreFailure = new SQLException("Restore failed");
        when(provider.getLoginTimeout()).thenReturn(9);
        when(provider.getPooledConnection()).thenThrow(failure);
        doThrow(restoreFailure).when(provider).setLoginTimeout(9);
        assertSame(failure, assertThrows(SQLTimeoutException.class, () -> ConnectionPoolDataSourceUtils.getPooledConnection(provider,
                null, null, Duration.ofSeconds(1))));
        assertEquals(Arrays.asList(restoreFailure), Arrays.asList(failure.getSuppressed()));
    }

    /**
     * Tests that deserialized wrappers and their original data sources share live login timeout changes.
     *
     * @param shared whether to use a shared pool instead of per-user pools.
     * @param configured whether to configure a timeout before serializing the data source.
     * @throws Exception Thrown if serialization, deserialization, or a JDBC operation fails.
     */
    @ParameterizedTest
    @MethodSource("serializationTimeouts")
    void testDeserializedWrappersShareLiveLoginTimeout(final boolean shared, final boolean configured) throws Exception {
        final SerializableCpds provider = new SerializableCpds();
        try (InstanceKeyDataSource source = newDataSource(shared, false, provider)) {
            if (configured) {
                source.setLoginTimeout(Duration.ofSeconds(5));
            }
            try (Connection connection = source.getConnection()) {
                assertFalse(connection.isClosed());
            }
            try (InstanceKeyDataSource restored = deserialize(serialize(source))) {
                assertEquals(source.getLoginTimeoutDuration(), restored.getLoginTimeoutDuration());
                restored.setLoginTimeout(Duration.ofSeconds(3));
                assertEquals(Duration.ofSeconds(3), source.getLoginTimeoutDuration());
                provider.attempts.clear();
                try (Connection connection = restored.getConnection()) {
                    assertFalse(connection.isClosed());
                }
                assertFalse(provider.attempts.isEmpty());
                provider.attempts.forEach(value -> assertEquals(3, value.intValue()));
                source.setLoginTimeout(Duration.ofSeconds(7));
                assertEquals(Duration.ofSeconds(7), restored.getLoginTimeoutDuration());
                provider.attempts.clear();
                try (Connection connection = restored.getConnection()) {
                    assertFalse(connection.isClosed());
                }
                assertFalse(provider.attempts.isEmpty());
                provider.attempts.forEach(value -> assertEquals(7, value.intValue()));
                assertEquals(9, provider.getLoginTimeout());
            }
        }
    }

    /**
     * Tests that an unchanged login timeout avoids a provider setter call.
     *
     * @throws SQLException Thrown if JDBC setup, an operation, or cleanup unexpectedly fails.
     */
    @Test
    void testEqualTimeoutDoesNotCallSetter() throws SQLException {
        final ConnectionPoolDataSource provider = mock(ConnectionPoolDataSource.class);
        when(provider.getLoginTimeout()).thenReturn(1);
        ConnectionPoolDataSourceUtils.getPooledConnection(provider, null, null, Duration.ofMillis(100));
        verify(provider, never()).setLoginTimeout(anyInt());
    }

    /**
     * Tests timeout conversion and live configuration changes for initial and later physical connections.
     *
     * @param shared whether to use a shared pool instead of per-user pools.
     * @param jndi whether to obtain the provider through JNDI.
     * @param credentials whether to use explicit user credentials.
     * @param duration the duration under test.
     * @param expected the expected timeout passed to JDBC, in seconds.
     * @throws Exception Thrown if setting up, using, or closing the test resources fails.
     */
    @ParameterizedTest
    @MethodSource("loginTimeouts")
    void testLoginTimeoutForInitialAndLaterConnections(final boolean shared, final boolean jndi, final boolean credentials,
            final Duration duration, final int expected) throws Exception {
        final RecordingCpds provider = new RecordingCpds();
        try (InstanceKeyDataSource source = newDataSource(shared, jndi, provider)) {
            source.setLoginTimeout(duration);
            assertEquals(duration, source.getLoginTimeoutDuration());
            assertEquals(expected, source.getLoginTimeout());
            try (Connection connection = credentials ? source.getConnection("u1", "p1") : source.getConnection()) {
                assertFalse(connection.isClosed());
                assertEquals(9, provider.getLoginTimeout());
            }
            assertFalse(provider.attempts.isEmpty());
            provider.attempts.forEach(value -> assertEquals(expected, value.intValue()));
            provider.attempts.clear();
            source.setLoginTimeout(Duration.ofSeconds(3));
            try (Connection connection = credentials ? source.getConnection("u1", "p1") : source.getConnection()) {
                assertFalse(connection.isClosed());
            }
            assertFalse(provider.attempts.isEmpty());
            provider.attempts.forEach(value -> assertEquals(3, value.intValue()));
            assertEquals(9, provider.getLoginTimeout());
        }
    }

    /**
     * Tests that invalid login timeouts are rejected without changing the current setting.
     *
     * @param shared whether to use a shared pool instead of per-user pools.
     * @throws Exception Thrown if setting up, using, or closing the test resources fails.
     */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testLoginTimeoutRejectsInvalidValues(final boolean shared) throws Exception {
        try (InstanceKeyDataSource source = shared ? new SharedPoolDataSource() : new PerUserPoolDataSource()) {
            source.setLoginTimeout(Duration.ofSeconds(5));
            assertThrows(NullPointerException.class, () -> source.setLoginTimeout(null));
            assertThrows(IllegalArgumentException.class, () -> source.setLoginTimeout(Duration.ofNanos(-1)));
            assertThrows(IllegalArgumentException.class, () -> source.setLoginTimeout(-1));
            assertEquals(Duration.ofSeconds(5), source.getLoginTimeoutDuration());
        }
    }

    /**
     * Tests that provider-enforced login timeouts apply before and after pool initialization.
     *
     * @param shared whether to use a shared pool instead of per-user pools.
     * @throws Exception Thrown if setting up, using, or closing the test resources fails.
     */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testProviderEnforcesTimeoutForInitialAndLaterConnections(final boolean shared) throws Exception {
        final RecordingCpds provider = new RecordingCpds();
        provider.enforceTimeout = true;
        try (InstanceKeyDataSource source = newDataSource(shared, false, provider)) {
            source.setLoginTimeout(Duration.ofMillis(100));
            assertThrows(SQLException.class, source::getConnection);
            assertEquals(9, provider.getLoginTimeout());
        }
        try (InstanceKeyDataSource source = newDataSource(shared, false, provider)) {
            source.setLoginTimeout(5);
            try (Connection connection = source.getConnection()) {
                assertFalse(connection.isClosed());
            }
            source.setLoginTimeout(Duration.ofMillis(100));
            assertThrows(SQLException.class, source::getConnection);
            assertEquals(9, provider.getLoginTimeout());
        }
    }

    /**
     * Tests that interrupted lock acquisition preserves the interrupt flag and avoids provider access.
     *
     * @param timeout the provider lock timeout, or {@code null} for an unbounded read-lock wait.
     * @throws SQLException Thrown if JDBC setup, an operation, or cleanup unexpectedly fails.
     */
    @ParameterizedTest
    @MethodSource("interruptedTimeouts")
    void testProviderLockPreservesInterruption(final Duration timeout) throws SQLException {
        final ConnectionPoolDataSource provider = mock(ConnectionPoolDataSource.class);
        Thread.currentThread().interrupt();
        try {
            final SQLException failure = assertThrows(SQLException.class, () -> ConnectionPoolDataSourceUtils.getPooledConnection(provider,
                    null, null, timeout));
            assertInstanceOf(InterruptedException.class, failure.getCause());
            assertTrue(Thread.currentThread().isInterrupted());
            verify(provider, never()).getPooledConnection();
            verify(provider, never()).setLoginTimeout(anyInt());
        } finally {
            Thread.interrupted();
        }
    }

    /**
     * Tests that each provider failure path releases the lock for a subsequent attempt from another thread.
     *
     * @param phase the failing operation: reading, setting, connecting, or restoring the timeout.
     * @throws Exception Thrown if a JDBC operation or worker task fails, times out, or is interrupted.
     */
    @ParameterizedTest
    @ValueSource(strings = {"read", "set", "connect", "restore"})
    void testProviderLockReleasedAfterFailure(final String phase) throws Exception {
        final ConnectionPoolDataSource provider = mock(ConnectionPoolDataSource.class);
        final AtomicInteger timeout = new AtomicInteger(9);
        final AtomicBoolean failOnce = new AtomicBoolean(true);
        final SQLException failure = new SQLException("Provider failure during " + phase);
        when(provider.getLoginTimeout()).thenAnswer(invocation -> {
            if ("read".equals(phase) && failOnce.getAndSet(false)) {
                throw failure;
            }
            return timeout.get();
        });
        doAnswer(invocation -> {
            final int seconds = invocation.getArgument(0);
            timeout.set(seconds);
            if (("set".equals(phase) && seconds == 1 || "restore".equals(phase) && seconds == 9) && failOnce.getAndSet(false)) {
                throw failure;
            }
            return null;
        }).when(provider).setLoginTimeout(anyInt());
        when(provider.getPooledConnection()).thenAnswer(invocation -> {
            if ("connect".equals(phase) && failOnce.getAndSet(false)) {
                throw failure;
            }
            return mock(PooledConnection.class);
        });
        assertSame(failure, assertThrows(SQLException.class, () -> ConnectionPoolDataSourceUtils.getPooledConnection(provider,
                null, null, Duration.ofSeconds(1))));
        assertEquals(9, timeout.get());
        // A different thread must be able to acquire the provider lock after every failure path.
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            final Future<PooledConnection> next = executor.submit(() -> ConnectionPoolDataSourceUtils.getPooledConnection(provider,
                    null, null, Duration.ofSeconds(2)));
            final PooledConnection connection = next.get(5, TimeUnit.SECONDS);
            assertNotNull(connection);
            connection.close();
            assertEquals(9, timeout.get());
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    /**
     * Tests provider lock isolation by identity despite mutable hashes or equality between distinct providers.
     *
     * @param sameProvider whether both attempts use the same provider instance.
     * @throws Exception Thrown if a JDBC operation or worker task fails, times out, or is interrupted.
     */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testProviderLocksUseIdentity(final boolean sameProvider) throws Exception {
        final SerializableCpds firstProvider = new SerializableCpds();
        final SerializableCpds secondProvider = sameProvider ? firstProvider : new SerializableCpds();
        // Equal but distinct providers must not share a lock. A changing hash must not bypass a provider's own lock.
        firstProvider.equalToOtherProviders = !sameProvider;
        secondProvider.equalToOtherProviders = !sameProvider;
        firstProvider.entered = new CountDownLatch(1);
        firstProvider.release = new CountDownLatch(1);
        final ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            final Future<PooledConnection> first = executor.submit(() -> ConnectionPoolDataSourceUtils.getPooledConnection(firstProvider,
                    null, null, Duration.ofSeconds(1)));
            assertTrue(firstProvider.entered.await(10, TimeUnit.SECONDS));
            final Future<PooledConnection> second = executor.submit(() -> ConnectionPoolDataSourceUtils.getPooledConnection(secondProvider,
                    null, null, Duration.ofSeconds(2)));
            if (sameProvider) {
                assertThrows(TimeoutException.class, () -> second.get(100, TimeUnit.MILLISECONDS));
            } else {
                assertNotNull(second.get(5, TimeUnit.SECONDS));
                assertEquals(Collections.singletonList(2), secondProvider.attempts);
            }
            firstProvider.release.countDown();
            first.get(10, TimeUnit.SECONDS).close();
            second.get(10, TimeUnit.SECONDS).close();
            assertEquals(sameProvider ? Arrays.asList(1, 2) : Collections.singletonList(1), firstProvider.attempts);
            assertEquals(9, firstProvider.getLoginTimeout());
            assertEquals(9, secondProvider.getLoginTimeout());
        } finally {
            firstProvider.release.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    /**
     * Tests that registered data sources share live timeouts after deserialization even before their pools are initialized.
     *
     * @param shared whether to use a shared pool instead of per-user pools.
     * @param configured whether to configure a timeout before serializing the data source.
     * @throws Exception Thrown if serialization, deserialization, or closing the data source fails.
     */
    @ParameterizedTest
    @MethodSource("serializationTimeouts")
    void testRegisteredSerializationSharesTimeoutBeforePoolInitialization(final boolean shared, final boolean configured) throws Exception {
        try (InstanceKeyDataSource source = newDataSource(shared, false, new SerializableCpds())) {
            if (configured) {
                source.setLoginTimeout(Duration.ofSeconds(5));
            }
            final byte[] bytes = serialize(source);
            source.setLoginTimeout(Duration.ofSeconds(7));
            try (InstanceKeyDataSource restored = deserialize(bytes)) {
                assertEquals(Duration.ofSeconds(7), restored.getConfiguredLoginTimeout());
                restored.setLoginTimeout(Duration.ofSeconds(3));
                assertEquals(Duration.ofSeconds(3), source.getConfiguredLoginTimeout());
            }
        }
    }

    /**
     * Tests that reusing the same driver exception for connection and restoration failures avoids self-suppression.
     *
     * @throws SQLException Thrown if JDBC setup, an operation, or cleanup unexpectedly fails.
     */
    @Test
    void testRepeatedDriverExceptionDoesNotSuppressItself() throws SQLException {
        final ConnectionPoolDataSource provider = mock(ConnectionPoolDataSource.class);
        final SQLException failure = new SQLException("Driver failed");
        when(provider.getLoginTimeout()).thenReturn(9);
        when(provider.getPooledConnection()).thenThrow(failure);
        doThrow(failure).when(provider).setLoginTimeout(9);
        assertSame(failure, assertThrows(SQLException.class, () -> ConnectionPoolDataSourceUtils.getPooledConnection(provider,
                null, null, Duration.ofSeconds(1))));
        assertEquals(0, failure.getSuppressed().length);
    }

    /**
     * Tests that a restoration failure closes the acquired connection and retains a subsequent close failure.
     *
     * @throws SQLException Thrown if JDBC setup, an operation, or cleanup unexpectedly fails.
     */
    @Test
    void testRestoreFailureClosesConnectionAndSuppressesCloseFailure() throws SQLException {
        final ConnectionPoolDataSource provider = mock(ConnectionPoolDataSource.class);
        final PooledConnection connection = mock(PooledConnection.class);
        final SQLException restoreFailure = new SQLException("Restore failed");
        final SQLException closeFailure = new SQLException("Close failed");
        when(provider.getLoginTimeout()).thenReturn(9);
        when(provider.getPooledConnection()).thenReturn(connection);
        doThrow(restoreFailure).when(provider).setLoginTimeout(9);
        doThrow(closeFailure).when(connection).close();
        assertSame(restoreFailure, assertThrows(SQLException.class, () -> ConnectionPoolDataSourceUtils.getPooledConnection(provider,
                null, null, Duration.ofSeconds(1))));
        verify(connection).close();
        assertEquals(Arrays.asList(closeFailure), Arrays.asList(restoreFailure.getSuppressed()));
    }

    /**
     * Tests timeout restoration when the provider setter changes its state before failing.
     *
     * @throws SQLException Thrown if JDBC setup, an operation, or cleanup unexpectedly fails.
     */
    @Test
    void testSetterFailureRestoresProviderTimeout() throws SQLException {
        final AtomicInteger timeout = new AtomicInteger(9);
        final ConnectionPoolDataSource provider = mockProvider(timeout);
        final SQLException failure = new SQLException("Cannot apply timeout");
        doAnswer(invocation -> {
            timeout.set(1);
            throw failure;
        }).when(provider).setLoginTimeout(1);
        assertSame(failure, assertThrows(SQLException.class, () -> ConnectionPoolDataSourceUtils.getPooledConnection(provider,
                null, null, Duration.ofSeconds(1))));
        assertEquals(9, timeout.get());
        verify(provider, never()).getPooledConnection();
    }

    /**
     * Tests that temporary timeout overrides exclude overlapping attempts through the same provider.
     *
     * @param secondUsesProviderDefault whether the second attempt leaves the provider timeout unconfigured.
     * @throws Exception Thrown if a JDBC operation or worker task fails, times out, or is interrupted.
     */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testSharedProviderSerializesTemporaryTimeouts(final boolean secondUsesProviderDefault) throws Exception {
        final AtomicInteger timeout = new AtomicInteger(9);
        final ConnectionPoolDataSource provider = mockProvider(timeout);
        final PooledConnection connection = mock(PooledConnection.class);
        final CountDownLatch firstEntered = new CountDownLatch(1);
        final CountDownLatch releaseFirst = new CountDownLatch(1);
        final CountDownLatch secondStarted = new CountDownLatch(1);
        final List<Integer> observed = new ArrayList<>();
        when(provider.getPooledConnection()).thenAnswer(invocation -> {
            observed.add(timeout.get());
            if (observed.size() == 1) {
                firstEntered.countDown();
                assertTrue(releaseFirst.await(10, TimeUnit.SECONDS));
                assertEquals(1, timeout.get());
            }
            return connection;
        });
        final ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            final Future<PooledConnection> first = executor.submit(() -> ConnectionPoolDataSourceUtils.getPooledConnection(provider,
                    null, null, Duration.ofSeconds(1)));
            assertTrue(firstEntered.await(10, TimeUnit.SECONDS));
            final Future<PooledConnection> second = executor.submit(() -> {
                secondStarted.countDown();
                return ConnectionPoolDataSourceUtils.getPooledConnection(provider, null, null, secondUsesProviderDefault ? null : Duration.ofSeconds(2));
            });
            assertTrue(secondStarted.await(10, TimeUnit.SECONDS));
            assertThrows(TimeoutException.class, () -> second.get(100, TimeUnit.MILLISECONDS));
            releaseFirst.countDown();
            assertSame(connection, first.get(10, TimeUnit.SECONDS));
            assertSame(connection, second.get(10, TimeUnit.SECONDS));
            assertEquals(Arrays.asList(1, secondUsesProviderDefault ? 9 : 2), observed);
            assertEquals(9, timeout.get());
        } finally {
            releaseFirst.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    /**
     * Tests that unchecked connection failures preserve their identity and restore the provider timeout.
     *
     * @param failure the unchecked failure thrown by the provider.
     * @throws SQLException Thrown if JDBC setup, an operation, or cleanup unexpectedly fails.
     */
    @ParameterizedTest
    @MethodSource("uncheckedFailures")
    void testUncheckedConnectionFailureRestoresTimeout(final Throwable failure) throws SQLException {
        final AtomicInteger timeout = new AtomicInteger(9);
        final ConnectionPoolDataSource provider = mockProvider(timeout);
        when(provider.getPooledConnection()).thenThrow(failure);
        assertSame(failure, assertThrows(failure.getClass(), () -> ConnectionPoolDataSourceUtils.getPooledConnection(provider,
                null, null, Duration.ofSeconds(1))));
        assertEquals(9, timeout.get());
    }

    /**
     * Tests that an unset login timeout avoids reading or changing the provider's setting.
     *
     * @throws SQLException Thrown if JDBC setup, an operation, or cleanup unexpectedly fails.
     */
    @Test
    void testUnconfiguredTimeoutDoesNotCallProviderTimeoutMethods() throws SQLException {
        final ConnectionPoolDataSource provider = mock(ConnectionPoolDataSource.class);
        ConnectionPoolDataSourceUtils.getPooledConnection(provider, null, null, null);
        verify(provider).getPooledConnection();
        verify(provider, never()).getLoginTimeout();
        verify(provider, never()).setLoginTimeout(anyInt());
    }

    /**
     * Tests that an unset login timeout preserves the provider default during physical connection creation.
     *
     * @param shared whether to use a shared pool instead of per-user pools.
     * @throws Exception Thrown if setting up, using, or closing the test resources fails.
     */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testUnconfiguredTimeoutPreservesProviderDefault(final boolean shared) throws Exception {
        final RecordingCpds provider = new RecordingCpds();
        try (InstanceKeyDataSource source = newDataSource(shared, false, provider);
                Connection connection = source.getConnection()) {
            assertFalse(connection.isClosed());
            assertFalse(provider.attempts.isEmpty());
            provider.attempts.forEach(value -> assertEquals(9, value.intValue()));
        }
    }

    /**
     * Tests that repeated serialization before provider configuration preserves an independent local timeout setting.
     *
     * @param shared whether to use a shared pool instead of per-user pools.
     * @param jndi whether to configure the restored provider through JNDI.
     * @param timeout the serialized timeout, or {@code null} to retain the provider's setting.
     * @param expected the timeout expected by the provider, in seconds.
     * @throws Exception Thrown if serialization, provider configuration, or a JDBC operation fails.
     */
    @ParameterizedTest
    @MethodSource("unregisteredSerializationTimeouts")
    void testUnregisteredSerializationRetainsLoginTimeout(final boolean shared, final boolean jndi, final Duration timeout, final int expected)
            throws Exception {
        try (InstanceKeyDataSource source = shared ? new SharedPoolDataSource() : new PerUserPoolDataSource()) {
            if (timeout != null) {
                source.setLoginTimeout(timeout);
            }
            final byte[] bytes = serialize(source);
            source.setLoginTimeout(Duration.ofSeconds(11));
            try (InstanceKeyDataSource restored = deserialize(bytes)) {
                assertEquals(timeout, restored.getConfiguredLoginTimeout());
                assertEquals(timeout == null ? Duration.ZERO : timeout, restored.getLoginTimeoutDuration());
                try (InstanceKeyDataSource repeated = deserialize(serialize(restored))) {
                    restored.setLoginTimeout(Duration.ofSeconds(13));
                    assertEquals(timeout, repeated.getConfiguredLoginTimeout());
                    final RecordingCpds provider = new RecordingCpds();
                    configureDataSource(repeated, jndi, provider);
                    try (Connection connection = repeated.getConnection()) {
                        assertFalse(connection.isClosed());
                    }
                    assertFalse(provider.attempts.isEmpty());
                    provider.attempts.forEach(value -> assertEquals(expected, value.intValue()));
                    assertEquals(9, provider.getLoginTimeout());
                    provider.attempts.clear();
                    repeated.setLoginTimeout(Duration.ofSeconds(3));
                    try (Connection connection = repeated.getConnection()) {
                        assertFalse(connection.isClosed());
                    }
                    assertFalse(provider.attempts.isEmpty());
                    provider.attempts.forEach(value -> assertEquals(3, value.intValue()));
                    assertEquals(9, provider.getLoginTimeout());
                    assertEquals(Duration.ofSeconds(13), restored.getConfiguredLoginTimeout());
                    assertEquals(Duration.ofSeconds(11), source.getConfiguredLoginTimeout());
                }
            }
        }
    }

    /**
     * Tests that a positive login timeout bounds the wait for another attempt to release the provider lock.
     *
     * @throws Exception Thrown if a JDBC operation or worker task fails, times out, or is interrupted.
     */
    @Test
    void testWaitingForProviderHonorsPositiveTimeout() throws Exception {
        final SerializableCpds provider = new SerializableCpds();
        provider.entered = new CountDownLatch(1);
        provider.release = new CountDownLatch(1);
        final ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            final Future<PooledConnection> first = executor.submit(() -> ConnectionPoolDataSourceUtils.getPooledConnection(provider, null, null, null));
            assertTrue(provider.entered.await(10, TimeUnit.SECONDS));
            final Future<SQLTimeoutException> second = executor.submit(() -> assertThrows(SQLTimeoutException.class,
                    () -> ConnectionPoolDataSourceUtils.getPooledConnection(provider, null, null, Duration.ofMillis(100))));
            assertNotNull(second.get(5, TimeUnit.SECONDS));
            assertEquals(Collections.singletonList(9), provider.attempts);
            provider.release.countDown();
            first.get(10, TimeUnit.SECONDS).close();
            assertEquals(9, provider.getLoginTimeout());
        } finally {
            provider.release.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

}
