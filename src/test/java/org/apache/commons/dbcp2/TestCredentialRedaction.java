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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.naming.Context;
import javax.sql.PooledConnection;

import org.apache.commons.dbcp2.cpdsadapter.DriverAdapterCPDS;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests the shared credential filter and JDBC callers that use it for diagnostic output.
 */
class TestCredentialRedaction {

    private enum Caller {
        DRIVER_ADAPTER,
        DRIVER_CONNECTION_FACTORY;

        Supplier<String> renderer(final Properties properties) {
            if (this == DRIVER_ADAPTER) {
                final DriverAdapterCPDS dataSource = new DriverAdapterCPDS();
                dataSource.setConnectionProperties(properties);
                return dataSource::toString;
            }
            final DriverConnectionFactory factory = new DriverConnectionFactory(new TesterDriver(), "jdbc:apache:commons:testdriver", properties);
            return factory::toString;
        }

        String segment(final String properties) {
            return this == DRIVER_ADAPTER ? ", connectionProperties=" + properties + "," : ";" + properties + "]";
        }
    }

    private enum CredentialValue {
        EMPTY_STRING,
        STRING,
        CHAR_ARRAY,
        BYTE_ARRAY,
        OBJECT;

        Object create(final String key) {
            final String value = "secret-for-" + key + "-=,{}[]\n\u00e9";
            switch (this) {
            case EMPTY_STRING:
                return "";
            case STRING:
                return value;
            case CHAR_ARRAY:
                return value.toCharArray();
            case BYTE_ARRAY:
                return value.getBytes(StandardCharsets.UTF_8);
            default:
                return new Object() {
                    @Override
                    public String toString() {
                        throw new AssertionError("Credential values must not be stringified.");
                    }
                };
            }
        }
    }

    private static final class RecordingDriver extends TesterDriver implements AutoCloseable {

        private final String url = "jdbc:apache:commons:credential-redaction:" + UUID.randomUUID();
        private final TesterDriver delegate = new TesterDriver();
        private Properties received;

        RecordingDriver() throws SQLException {
            DriverManager.registerDriver(this);
        }

        @Override
        public boolean acceptsURL(final String connectionUrl) {
            return url.equals(connectionUrl);
        }

        @Override
        public void close() throws SQLException {
            DriverManager.deregisterDriver(this);
        }

        @Override
        public Connection connect(final String connectionUrl, final Properties properties) throws SQLException {
            if (!acceptsURL(connectionUrl)) {
                return null;
            }
            received = (Properties) properties.clone();
            return delegate.connect("jdbc:apache:commons:testdriver", properties);
        }
    }

    private static final List<String> CREDENTIAL_KEYS = Arrays.asList("user", "password", Context.SECURITY_PRINCIPAL, Context.SECURITY_CREDENTIALS);
    private static final int ALL_CREDENTIALS = (1 << CREDENTIAL_KEYS.size()) - 1;

    private static void assertDiagnostics(final Supplier<String> renderer, final Properties properties) {
        final Properties original = (Properties) properties.clone();
        for (int repeat = 0; repeat < 2; repeat++) {
            final String text = renderer.get();
            for (final String key : CREDENTIAL_KEYS) {
                assertFalse(text.contains("{" + key + "=") || text.contains(", " + key + "="), key);
                final String value = properties.getProperty(key);
                if (value != null && !value.isEmpty()) {
                    assertFalse(text.contains(value), key);
                }
            }
            for (final String key : properties.stringPropertyNames()) {
                if (properties.containsKey(key) && !CREDENTIAL_KEYS.contains(key)) {
                    assertTrue(text.contains(key + "=" + properties.getProperty(key)), key);
                }
            }
            assertEquals(original, properties);
            original.forEach((key, value) -> assertSame(value, properties.get(key), key.toString()));
        }
    }

    private static Properties credentialProperties(final CredentialValue value, final int mask) {
        final Properties properties = visibleProperties();
        for (int index = 0; index < CREDENTIAL_KEYS.size(); index++) {
            if ((mask & 1 << index) != 0) {
                final String key = CREDENTIAL_KEYS.get(index);
                properties.put(key, value.create(key));
            }
        }
        return properties;
    }

    static Stream<Arguments> utilityCases() {
        // With no credential keys, the value type does not affect the input.
        return Stream.concat(Stream.of(Arguments.of(CredentialValue.STRING, 0)), Arrays.stream(CredentialValue.values())
                .flatMap(value -> IntStream.rangeClosed(1, ALL_CREDENTIALS).mapToObj(mask -> Arguments.of(value, mask))));
    }

    private static Properties visibleProperties() {
        final Properties properties = new Properties();
        properties.setProperty("ApplicationName", "diagnostic-app");
        properties.setProperty("socketTimeout", "123");
        return properties;
    }

    @ParameterizedTest
    @EnumSource(Caller.class)
    void testCallerConnectionAfterDiagnostics(final Caller caller) throws Exception {
        try (final RecordingDriver driver = new RecordingDriver()) {
            final Properties properties = credentialProperties(CredentialValue.STRING, ALL_CREDENTIALS);
            properties.setProperty("user", "foo");
            properties.setProperty("password", "bar");
            final Properties original = (Properties) properties.clone();
            if (caller == Caller.DRIVER_ADAPTER) {
                final DriverAdapterCPDS dataSource = new DriverAdapterCPDS();
                dataSource.setUrl(driver.url);
                dataSource.setConnectionProperties(properties);
                assertDiagnostics(dataSource::toString, properties);
                final PooledConnection pooled = dataSource.getPooledConnection();
                try (final Connection connection = pooled.getConnection()) {
                    assertFalse(connection.isClosed());
                } finally {
                    pooled.close();
                }
                assertDiagnostics(dataSource::toString, properties);
                assertSame(properties, dataSource.getConnectionProperties());
            } else {
                final DriverConnectionFactory factory = new DriverConnectionFactory(driver, driver.url, properties);
                assertDiagnostics(factory::toString, properties);
                try (final Connection connection = factory.createConnection()) {
                    assertNotNull(connection);
                    assertFalse(connection.isClosed());
                }
                assertDiagnostics(factory::toString, properties);
                assertSame(properties, factory.getProperties());
            }
            assertEquals(original, driver.received);
            assertEquals(original, properties);
        }
    }

    @ParameterizedTest
    @EnumSource(Caller.class)
    void testCallerInheritedDefaults(final Caller caller) {
        final Properties defaults = credentialProperties(CredentialValue.STRING, ALL_CREDENTIALS);
        final Properties originalDefaults = (Properties) defaults.clone();
        final Properties properties = new Properties(defaults);
        properties.setProperty("ApplicationName", "explicit-app");
        final Supplier<String> renderer = caller.renderer(properties);
        assertDiagnostics(renderer, properties);
        assertFalse(renderer.get().contains("socketTimeout="));
        for (final String key : CREDENTIAL_KEYS) {
            assertEquals(defaults.getProperty(key), properties.getProperty(key), key);
        }
        assertEquals(originalDefaults, defaults);
    }

    @ParameterizedTest
    @EnumSource(Caller.class)
    void testCallerNonStringCredentials(final Caller caller) {
        final Properties properties = credentialProperties(CredentialValue.OBJECT, ALL_CREDENTIALS);
        assertDiagnostics(caller.renderer(properties), properties);
    }

    @ParameterizedTest
    @EnumSource(Caller.class)
    void testCallerNullAndEmptyProperties(final Caller caller) {
        assertTrue(caller.renderer(null).get().contains(caller.segment("null")));
        final Properties properties = new Properties();
        final Supplier<String> renderer = caller.renderer(properties);
        assertTrue(renderer.get().contains(caller.segment("{}")));
        assertDiagnostics(renderer, properties);
    }

    @ParameterizedTest
    @EnumSource(Caller.class)
    void testCallerReflectsPropertyUpdates(final Caller caller) {
        final Properties properties = credentialProperties(CredentialValue.STRING, ALL_CREDENTIALS);
        final Supplier<String> renderer = caller.renderer(properties);
        assertDiagnostics(renderer, properties);
        CREDENTIAL_KEYS.forEach(key -> properties.setProperty(key, "updated-secret-" + key));
        properties.setProperty("ApplicationName", "updated-app");
        assertDiagnostics(renderer, properties);
        properties.clear();
        assertTrue(renderer.get().contains(caller.segment("{}")));
        assertDiagnostics(renderer, properties);
    }

    @ParameterizedTest
    @EnumSource(Caller.class)
    void testCallerWithoutCredentials(final Caller caller) {
        final Properties properties = visibleProperties();
        properties.setProperty("provider.password", "retained-option");
        assertDiagnostics(caller.renderer(properties), properties);
    }

    @Test
    void testCloneContainersAreIndependent() {
        final Properties properties = credentialProperties(CredentialValue.STRING, ALL_CREDENTIALS);
        final Properties first = Utils.cloneWithoutCredentials(properties);
        final Properties second = Utils.cloneWithoutCredentials(properties);
        assertNotSame(first, second);
        first.setProperty("ApplicationName", "first-clone");
        properties.setProperty("socketTimeout", "456");
        assertEquals("diagnostic-app", properties.getProperty("ApplicationName"));
        assertEquals("diagnostic-app", second.getProperty("ApplicationName"));
        assertEquals("123", first.getProperty("socketTimeout"));
        assertEquals("123", second.getProperty("socketTimeout"));
        assertEquals(visibleProperties(), second);
    }

    @ParameterizedTest(name = "value={0}, credentialMask={1}")
    @MethodSource("utilityCases")
    void testCloneEveryCredentialCombination(final CredentialValue value, final int mask) {
        final Properties properties = credentialProperties(value, mask);
        final Properties original = (Properties) properties.clone();
        final Properties filtered = Utils.cloneWithoutCredentials(properties);
        assertNotSame(properties, filtered);
        for (final String key : CREDENTIAL_KEYS) {
            assertFalse(filtered.containsKey(key), key);
        }
        assertEquals(visibleProperties(), filtered);
        assertEquals(original, properties);
        original.forEach((key, item) -> assertSame(item, properties.get(key), key.toString()));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testCloneInheritedDefaultsRemainShared(final boolean overrideCredentials) {
        final Properties defaults = credentialProperties(CredentialValue.STRING, ALL_CREDENTIALS);
        final Properties originalDefaults = (Properties) defaults.clone();
        final Properties properties = new Properties(defaults);
        if (overrideCredentials) {
            CREDENTIAL_KEYS.forEach(key -> properties.setProperty(key, "direct-secret-" + key));
        }
        properties.setProperty("ApplicationName", "explicit-app");
        final Properties original = (Properties) properties.clone();
        final Properties filtered = Utils.cloneWithoutCredentials(properties);
        assertEquals(1, filtered.size());
        assertEquals("explicit-app", filtered.getProperty("ApplicationName"));
        for (final String key : CREDENTIAL_KEYS) {
            assertFalse(filtered.containsKey(key), key);
            assertEquals(defaults.getProperty(key), filtered.getProperty(key), key);
            assertFalse(filtered.toString().contains(defaults.getProperty(key)), key);
        }
        assertEquals(original, properties);
        assertEquals(originalDefaults, defaults);
        defaults.setProperty("socketTimeout", "789");
        assertEquals("789", properties.getProperty("socketTimeout"));
        assertEquals("789", filtered.getProperty("socketTimeout"));
    }

    @Test
    void testCloneNullAndEmptyProperties() {
        assertNull(Utils.cloneWithoutCredentials(null));
        final Properties properties = new Properties();
        final Properties filtered = Utils.cloneWithoutCredentials(properties);
        assertNotSame(properties, filtered);
        assertTrue(filtered.isEmpty());
        filtered.setProperty("option", "value");
        assertTrue(properties.isEmpty());
    }

    @Test
    void testCloneRetainsOtherObjectEntries() {
        final Properties properties = credentialProperties(CredentialValue.OBJECT, ALL_CREDENTIALS);
        final Object key = new Object();
        final Object value = new Object();
        properties.put(key, value);
        properties.put("opaque-option", value);
        final Properties filtered = Utils.cloneWithoutCredentials(properties);
        assertSame(value, filtered.get(key));
        assertSame(value, filtered.get("opaque-option"));
        assertSame(value, properties.get(key));
        assertEquals(4, filtered.size());
    }

    @ParameterizedTest
    @ValueSource(strings = {"user", "password", Context.SECURITY_PRINCIPAL, Context.SECURITY_CREDENTIALS})
    void testExactKeyMatching(final String credentialKey) {
        final Properties properties = credentialProperties(CredentialValue.STRING, ALL_CREDENTIALS);
        final Properties expected = visibleProperties();
        for (final String key : Arrays.asList(credentialKey.toUpperCase(Locale.ROOT), "provider." + credentialKey, credentialKey + ".extra")) {
            properties.setProperty(key, "retained-option");
            expected.setProperty(key, "retained-option");
        }
        assertEquals(expected, Utils.cloneWithoutCredentials(properties));
    }
}
