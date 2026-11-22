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

package org.apache.commons.dbcp2.datasources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;

import org.apache.commons.dbcp2.TesterDriver;
import org.apache.commons.dbcp2.cpdsadapter.DriverAdapterCPDS;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests JNDI credential redaction in datasource diagnostic strings and exception messages.
 */
public class TestInstanceKeyDataSourceToString {

    /**
     * Checks that diagnostic rendering preserves the credentials supplied to JNDI.
     */
    public static final class CredentialCheckingContextFactory implements InitialContextFactory {

        /**
         * Constructs a factory for JNDI to instantiate reflectively.
         */
        public CredentialCheckingContextFactory() {
        }

        @Override
        public Context getInitialContext(final Hashtable<?, ?> environment) throws NamingException {
            for (final String key : CREDENTIAL_KEYS) {
                assertEquals(credentialValue(key), environment.get(key), key);
            }
            final DriverAdapterCPDS cpds = new DriverAdapterCPDS();
            try {
                cpds.setDriver(TesterDriver.class.getName());
            } catch (final ClassNotFoundException e) {
                final NamingException exception = new NamingException("Cannot load the test JDBC driver.");
                exception.initCause(e);
                throw exception;
            }
            cpds.setUrl("jdbc:apache:commons:testdriver");
            cpds.setUser("foo");
            cpds.setPassword("bar");
            return new InitialContext(true) {
                @Override
                public Object lookup(final String name) {
                    assertEquals("testCPDS", name);
                    return cpds;
                }
            };
        }
    }

    private enum DataSourceType {
        PER_USER(PerUserPoolDataSource::new),
        SHARED(SharedPoolDataSource::new);

        private final Supplier<InstanceKeyDataSource> factory;

        DataSourceType(final Supplier<InstanceKeyDataSource> factory) {
            this.factory = factory;
        }

        InstanceKeyDataSource create() {
            return factory.get();
        }
    }

    private static final List<String> CREDENTIAL_KEYS = Arrays.asList("user", "password", Context.SECURITY_PRINCIPAL, Context.SECURITY_CREDENTIALS);

    private static void assertDiagnostics(final InstanceKeyDataSource dataSource, final Properties environment) {
        for (int repeat = 0; repeat < 2; repeat++) {
            assertRedacted(dataSource.toString(), environment);
            assertEnvironmentUnchanged(dataSource, environment);
        }
        final SQLException exception = assertThrows(SQLException.class, () -> dataSource.unwrap(Runnable.class));
        assertTrue(exception.getMessage().contains("is not a wrapper for"));
        assertRedacted(exception.getMessage(), environment);
        assertEnvironmentUnchanged(dataSource, environment);
    }

    private static void assertEnvironmentUnchanged(final InstanceKeyDataSource dataSource, final Properties environment) {
        for (final String key : CREDENTIAL_KEYS) {
            assertEquals(environment.getProperty(key), dataSource.getJndiEnvironment(key), key);
        }
        for (final String key : environment.stringPropertyNames()) {
            assertEquals(environment.getProperty(key), dataSource.getJndiEnvironment(key), key);
        }
    }

    private static void assertRedacted(final String text, final Properties environment) {
        assertTrue(text.contains(", jndiEnvironment="));
        for (final String key : CREDENTIAL_KEYS) {
            assertFalse(text.contains(key + "="), key);
            final String value = environment.getProperty(key);
            if (value != null && !value.isEmpty()) {
                assertFalse(text.contains(value), key);
            }
        }
        for (final String key : environment.stringPropertyNames()) {
            if (!CREDENTIAL_KEYS.contains(key)) {
                assertTrue(text.contains(key + "=" + environment.getProperty(key)), key);
            }
        }
    }

    private static String credentialValue(final String key) {
        return "secret-for-" + key + "-with-=,{}[]\n\u00e9";
    }

    private static Properties environment() {
        final Properties properties = new Properties();
        properties.setProperty(Context.PROVIDER_URL, "ldap://directory.invalid:389");
        properties.setProperty(Context.SECURITY_AUTHENTICATION, "simple");
        properties.setProperty(Context.SECURITY_PROTOCOL, "ssl");
        for (final String key : CREDENTIAL_KEYS) {
            properties.setProperty(key, credentialValue(key));
        }
        return properties;
    }

    static Stream<Arguments> environmentSetters() {
        return Arrays.stream(DataSourceType.values()).flatMap(type -> Stream.of(false, true).map(bulk -> Arguments.of(type, bulk)));
    }

    private static void setEnvironment(final InstanceKeyDataSource dataSource, final Properties environment, final boolean bulk) {
        if (bulk) {
            dataSource.setJndiEnvironment(environment);
        } else {
            for (final String key : environment.stringPropertyNames()) {
                dataSource.setJndiEnvironment(key, environment.getProperty(key));
            }
        }
    }

    @ParameterizedTest
    @MethodSource("environmentSetters")
    void testDiagnosticsPreserveJndiLookup(final DataSourceType type, final boolean bulk) throws Exception {
        try (final InstanceKeyDataSource dataSource = type.create()) {
            final Properties properties = environment();
            properties.setProperty(Context.INITIAL_CONTEXT_FACTORY, CredentialCheckingContextFactory.class.getName());
            final Properties original = (Properties) properties.clone();
            setEnvironment(dataSource, properties, bulk);
            dataSource.setDataSourceName("testCPDS");
            assertDiagnostics(dataSource, original);
            assertNotNull(dataSource.testCPDS(null, null));
            assertDiagnostics(dataSource, original);
            assertEquals(original, properties);
        }
    }

    @ParameterizedTest
    @MethodSource("environmentSetters")
    void testToStringEmptyCredentials(final DataSourceType type, final boolean bulk) throws Exception {
        try (final InstanceKeyDataSource dataSource = type.create()) {
            final Properties properties = new Properties();
            CREDENTIAL_KEYS.forEach(key -> properties.setProperty(key, ""));
            setEnvironment(dataSource, properties, bulk);
            assertDiagnostics(dataSource, properties);
            assertTrue(dataSource.toString().contains(", jndiEnvironment={},"));
            assertEquals(CREDENTIAL_KEYS.size(), properties.size());
        }
    }

    @ParameterizedTest
    @EnumSource(DataSourceType.class)
    void testToStringNonStringCredentials(final DataSourceType type) throws Exception {
        try (final InstanceKeyDataSource dataSource = type.create()) {
            final Object secret = new Object() {
                @Override
                public String toString() {
                    throw new AssertionError("Credential values must not be stringified.");
                }
            };
            final Properties properties = new Properties();
            CREDENTIAL_KEYS.forEach(key -> properties.put(key, secret));
            dataSource.setJndiEnvironment(properties);
            assertTrue(dataSource.toString().contains(", jndiEnvironment={},"));
            final SQLException exception = assertThrows(SQLException.class, () -> dataSource.unwrap(Runnable.class));
            assertTrue(exception.getMessage().contains(", jndiEnvironment={},"));
            for (final String key : CREDENTIAL_KEYS) {
                assertSame(secret, properties.get(key));
            }
        }
    }

    @ParameterizedTest
    @EnumSource(DataSourceType.class)
    void testToStringNullAndEmptyEnvironment(final DataSourceType type) throws Exception {
        try (final InstanceKeyDataSource dataSource = type.create()) {
            assertTrue(dataSource.toString().contains(", jndiEnvironment=null,"));
            assertDiagnostics(dataSource, new Properties());
            dataSource.setJndiEnvironment(new Properties());
            assertTrue(dataSource.toString().contains(", jndiEnvironment={},"));
            assertDiagnostics(dataSource, new Properties());
        }
    }

    @ParameterizedTest
    @MethodSource("environmentSetters")
    void testToStringReflectsEnvironmentUpdates(final DataSourceType type, final boolean bulk) throws Exception {
        try (final InstanceKeyDataSource dataSource = type.create()) {
            final Properties properties = environment();
            setEnvironment(dataSource, properties, bulk);
            assertDiagnostics(dataSource, properties);
            CREDENTIAL_KEYS.forEach(key -> properties.setProperty(key, "updated-" + credentialValue(key)));
            properties.setProperty(Context.PROVIDER_URL, "ldap://updated.invalid:389");
            setEnvironment(dataSource, properties, bulk);
            assertDiagnostics(dataSource, properties);
            dataSource.setJndiEnvironment(new Properties());
            assertTrue(dataSource.toString().contains(", jndiEnvironment={},"));
            assertDiagnostics(dataSource, new Properties());
        }
    }

    @ParameterizedTest
    @MethodSource("environmentSetters")
    void testToStringRetainsOtherKeys(final DataSourceType type, final boolean bulk) throws Exception {
        try (final InstanceKeyDataSource dataSource = type.create()) {
            final Properties properties = new Properties();
            for (final String key : Arrays.asList("provider.password", "credentials", "User", "Password", "JAVA.NAMING.SECURITY.CREDENTIALS",
                    "java.naming.security.credentials.extra", Context.SECURITY_AUTHENTICATION, Context.SECURITY_PROTOCOL)) {
                properties.setProperty(key, "retained-value");
            }
            setEnvironment(dataSource, properties, bulk);
            final String text = dataSource.toString();
            final SQLException exception = assertThrows(SQLException.class, () -> dataSource.unwrap(Runnable.class));
            for (final String key : properties.stringPropertyNames()) {
                assertTrue(text.contains(key + "=retained-value"), key);
                assertTrue(exception.getMessage().contains(key + "=retained-value"), key);
            }
            assertEnvironmentUnchanged(dataSource, properties);
        }
    }

    @ParameterizedTest
    @EnumSource(DataSourceType.class)
    void testToStringWithConnectionPoolDataSource(final DataSourceType type) throws Exception {
        try (final InstanceKeyDataSource dataSource = type.create()) {
            final DriverAdapterCPDS cpds = new DriverAdapterCPDS();
            cpds.setDriver(TesterDriver.class.getName());
            cpds.setUrl("jdbc:apache:commons:testdriver");
            cpds.setUser("foo");
            cpds.setPassword("bar");
            cpds.setPoolPreparedStatements(false);
            dataSource.setConnectionPoolDataSource(cpds);
            final Properties properties = environment();
            setEnvironment(dataSource, properties, false);
            assertDiagnostics(dataSource, properties);
        }
    }
}
