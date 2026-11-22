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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.stream.Stream;

import javax.management.MBeanServer;
import javax.naming.CompositeName;
import javax.naming.Name;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.StringRefAddr;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.SimpleLog;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * TestSuite for BasicDataSourceFactory
 */
public class TestBasicDataSourceFactory {

    static Stream<Arguments> loggingLevels() {
        return Stream.of(
                Arguments.of(SimpleLog.LOG_LEVEL_INFO, 3, 3),
                Arguments.of(SimpleLog.LOG_LEVEL_WARN, 3, 0),
                Arguments.of(SimpleLog.LOG_LEVEL_ERROR, 0, 0),
                Arguments.of(SimpleLog.LOG_LEVEL_OFF, 0, 0));
    }

    static Stream<Arguments> obsoletePropertyValues() {
        return Stream.of(
                new String[] {"maxActive", "use maxTotal instead."},
                new String[] {"removeAbandoned", "use one or both of removeAbandonedOnBorrow or removeAbandonedOnMaintenance instead."},
                new String[] {"maxWait", "use maxWaitMillis instead."})
                .flatMap(property -> propertyValues().flatMap(value -> Stream.of(false, true)
                        .map(named -> Arguments.of(property[0], property[1], value, named))));
    }

    private static Stream<String> propertyValues() {
        return Stream.of(null, "", "secret-password", "secret-\"quoted\"\r\nforged-log-entry", "secret-p\u00e4ssw\u00f6rd-\u20ac");
    }

    static Stream<Arguments> unknownPropertyValues() {
        return Stream.of("passwd", "pwd", "Password", "password ", "apiToken", "customProperty")
                .flatMap(property -> propertyValues().flatMap(value -> Stream.of(false, true)
                        .map(named -> Arguments.of(property, value, named))));
    }

    private void checkConnectionPoolProperties(final GenericObjectPool<PoolableConnection> cp) {
        assertEquals(10, cp.getMaxTotal());
        assertEquals(8, cp.getMaxIdle());
        assertEquals(0, cp.getMinIdle());
        assertEquals(Duration.ofMillis(500), cp.getMaxWaitDuration());
        assertEquals(5, cp.getNumIdle());
        assertTrue(cp.getTestOnBorrow());
        assertFalse(cp.getTestOnReturn());
        assertEquals(Duration.ofSeconds(1), cp.getDurationBetweenEvictionRuns());
        assertEquals(Duration.ofSeconds(2), cp.getMinEvictableIdleDuration());
        assertEquals(Duration.ofSeconds(3), cp.getSoftMinEvictableIdleDuration());
        assertEquals(2, cp.getNumTestsPerEvictionRun());
        assertTrue(cp.getTestWhileIdle());
        assertTrue(cp.getRemoveAbandonedOnBorrow());
        assertTrue(cp.getRemoveAbandonedOnMaintenance());
        assertEquals(Duration.ofSeconds(3000), cp.getRemoveAbandonedTimeoutDuration());
        assertTrue(cp.getLogAbandoned());
        assertTrue(cp.getLifo());
    }

    private void checkDataSourceProperties(final BasicDataSource ds) throws Exception {
        assertEquals("org.apache.commons.dbcp2.TesterDriver", ds.getDriverClassName());
        assertEquals("jdbc:apache:commons:testdriver", ds.getUrl());
        assertEquals(10, ds.getMaxTotal());
        assertEquals(8, ds.getMaxIdle());
        assertEquals(0, ds.getMinIdle());
        assertEquals(Duration.ofMillis(500), ds.getMaxWaitDuration());
        assertEquals(5, ds.getInitialSize());
        assertEquals(5, ds.getNumIdle());
        assertEquals(Boolean.TRUE, ds.getDefaultAutoCommit());
        assertEquals(Boolean.FALSE, ds.getDefaultReadOnly());
        assertEquals(Connection.TRANSACTION_READ_COMMITTED, ds.getDefaultTransactionIsolation());
        assertEquals("test", ds.getDefaultCatalog());
        assertEquals("testSchema", ds.getDefaultSchema());
        assertTrue(ds.getTestOnBorrow());
        assertFalse(ds.getTestOnReturn());
        assertEquals("userName", ds.getUsername());
        assertEquals("userName", ds.getUserName());
        assertEquals("password", ds.getPassword());
        assertEquals("SELECT DUMMY FROM DUAL", ds.getValidationQuery());
        assertEquals(Duration.ofSeconds(100), ds.getValidationQueryTimeoutDuration());
        assertEquals(2, ds.getConnectionInitSqls().size());
        assertEquals("SELECT 1", ds.getConnectionInitSqls().get(0));
        assertEquals("SELECT 2", ds.getConnectionInitSqls().get(1));
        assertEquals(Duration.ofMillis(1000), ds.getDurationBetweenEvictionRuns());
        assertEquals(Duration.ofMillis(2000), ds.getMinEvictableIdleDuration());
        assertEquals(Duration.ofMillis(3000), ds.getSoftMinEvictableIdleDuration());
        assertEquals(2, ds.getNumTestsPerEvictionRun());
        assertTrue(ds.getTestWhileIdle());
        assertTrue(ds.isAccessToUnderlyingConnectionAllowed());
        assertTrue(ds.getRemoveAbandonedOnBorrow());
        assertTrue(ds.getRemoveAbandonedOnMaintenance());
        assertEquals(Duration.ofSeconds(3000), ds.getRemoveAbandonedTimeoutDuration());
        assertTrue(ds.getLogAbandoned());
        assertTrue(ds.getAbandonedUsageTracking());
        assertTrue(ds.isPoolPreparedStatements());
        assertTrue(ds.isClearStatementPoolOnReturn());
        assertEquals(10, ds.getMaxOpenPreparedStatements());
        assertTrue(ds.getLifo());
        assertTrue(ds.getFastFailValidation());
        assertTrue(ds.getDisconnectionSqlCodes().contains("XXX"));
        assertTrue(ds.getDisconnectionSqlCodes().contains("YYY"));
        assertEquals("org.apache.commons.dbcp2:name=test", ds.getJmxName());

        // Unregister so subsequent calls to getTestProperties can re-register
        final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        mbs.unregisterMBean(ds.getRegisteredJmxName());
    }

    private List<String> getMessages(final Reference ref, final Name name) throws SQLException {
        try {
            StackMessageLog.lock();
            StackMessageLog.clear();
            try (final BasicDataSource ds = (BasicDataSource) new BasicDataSourceFactory().getObjectInstance(ref, name, null, null)) {
                assertNotNull(ds);
                return StackMessageLog.getAll();
            }
        } finally {
            StackMessageLog.clear();
            StackMessageLog.unLock();
        }
    }

    private Properties getTestProperties() {
        final Properties properties = new Properties();
        properties.setProperty("driverClassName", "org.apache.commons.dbcp2.TesterDriver");
        properties.setProperty("url", "jdbc:apache:commons:testdriver");
        properties.setProperty("maxTotal", "10");
        properties.setProperty("maxIdle", "8");
        properties.setProperty("minIdle", "0");
        properties.setProperty("maxWaitMillis", "500");
        properties.setProperty("initialSize", "5");
        properties.setProperty("defaultAutoCommit", "true");
        properties.setProperty("defaultReadOnly", "false");
        properties.setProperty("defaultTransactionIsolation", "READ_COMMITTED");
        properties.setProperty("defaultCatalog", "test");
        properties.setProperty("defaultSchema", "testSchema");
        properties.setProperty("testOnBorrow", "true");
        properties.setProperty("testOnReturn", "false");
        properties.setProperty("username", "userName");
        properties.setProperty("password", "password");
        properties.setProperty("validationQuery", "SELECT DUMMY FROM DUAL");
        properties.setProperty("validationQueryTimeout", "100");
        properties.setProperty("connectionInitSqls", "SELECT 1;SELECT 2");
        properties.setProperty("timeBetweenEvictionRunsMillis", "1000");
        properties.setProperty("minEvictableIdleTimeMillis", "2000");
        properties.setProperty("softMinEvictableIdleTimeMillis", "3000");
        properties.setProperty("numTestsPerEvictionRun", "2");
        properties.setProperty("testWhileIdle", "true");
        properties.setProperty("accessToUnderlyingConnectionAllowed", "true");
        properties.setProperty("removeAbandonedOnBorrow", "true");
        properties.setProperty("removeAbandonedOnMaintenance", "true");
        properties.setProperty("removeAbandonedTimeout", "3000");
        properties.setProperty("logAbandoned", "true");
        properties.setProperty("abandonedUsageTracking", "true");
        properties.setProperty("poolPreparedStatements", "true");
        properties.setProperty("clearStatementPoolOnReturn", "true");
        properties.setProperty("maxOpenPreparedStatements", "10");
        properties.setProperty("lifo", "true");
        properties.setProperty("fastFailValidation", "true");
        properties.setProperty("disconnectionSqlCodes", "XXX,YYY");
        properties.setProperty("jmxName", "org.apache.commons.dbcp2:name=test");
        return properties;
    }

    @Test
    void testAllProperties() throws Exception {
        try {
            StackMessageLog.lock();
            StackMessageLog.clear();
            final Reference ref = new Reference("javax.sql.DataSource", BasicDataSourceFactory.class.getName(), null);
            final Properties properties = getTestProperties();
            for (final Entry<Object, Object> entry : properties.entrySet()) {
                ref.add(new StringRefAddr((String) entry.getKey(), (String) entry.getValue()));
            }
            final BasicDataSourceFactory basicDataSourceFactory = new BasicDataSourceFactory();
            try (final BasicDataSource ds = (BasicDataSource) basicDataSourceFactory.getObjectInstance(ref, null, null, null)) {
                checkDataSourceProperties(ds);
                checkConnectionPoolProperties(ds.getConnectionPool());
                final List<String> messages = StackMessageLog.getAll();
                assertEquals(0, messages.size());
            }
        } finally {
            StackMessageLog.clear();
            StackMessageLog.unLock();
        }
    }

    @ParameterizedTest
    @CsvSource({"passwd, 1", "customProperty, 1", "maxActive, 1", "removeAbandoned, 1", "maxWait, 1",
            "factory, 0", "scope, 0", "singleton, 0", "auth, 0"})
    void testIgnoredPropertyContentIsNotAccessed(final String propertyName, final int messageCount) throws Exception {
        final Reference ref = new Reference("javax.sql.DataSource", BasicDataSourceFactory.class.getName(), null);
        ref.add(new RefAddr(propertyName) {
            private static final long serialVersionUID = 1L;

            @Override
            public Object getContent() {
                throw new AssertionError("Ignored property content must not be accessed");
            }
        });
        final List<String> messages = getMessages(ref, null);
        assertEquals(messageCount, messages.size(), messages.toString());
        if (messageCount != 0) {
            assertTrue(messages.get(0).contains(propertyName), messages.toString());
        }
    }

    @ParameterizedTest
    @MethodSource("loggingLevels")
    void testMixedPropertiesPreserveConfigurationAndLogLevels(final int level, final int warningCount, final int infoCount) throws Exception {
        final StackMessageLog log = (StackMessageLog) LogFactory.getLog(BasicDataSourceFactory.class);
        StackMessageLog.lock();
        final int originalLevel = log.getLevel();
        try {
            StackMessageLog.clear();
            log.setLevel(level);
            final Reference ref = new Reference("javax.sql.DataSource", BasicDataSourceFactory.class.getName(), null);
            ref.add(new StringRefAddr("username", "secret-username"));
            ref.add(new StringRefAddr("password", "secret-password"));
            ref.add(new StringRefAddr("url", "jdbc:apache:commons:testdriver"));
            ref.add(new StringRefAddr("maxTotal", "17"));
            ref.add(new StringRefAddr("maxWaitMillis", "123"));
            ref.add(new StringRefAddr("removeAbandonedOnBorrow", "true"));
            for (final String property : new String[] {"maxActive", "removeAbandoned", "maxWait", "passwd", "customProperty",
                    "factory", "scope", "singleton", "auth"}) {
                ref.add(new StringRefAddr(property, "secret-" + property));
            }
            ref.add(new StringRefAddr("passwd", "secret-duplicate"));
            ref.add(new StringRefAddr("maxWait", "secret-duplicate-obsolete"));
            final Name name = new CompositeName("jdbc/test");
            try (final BasicDataSource ds = (BasicDataSource) new BasicDataSourceFactory().getObjectInstance(ref, name, null, null)) {
                assertEquals("secret-username", ds.getUsername());
                assertEquals("secret-password", ds.getPassword());
                assertEquals("jdbc:apache:commons:testdriver", ds.getUrl());
                assertEquals(17, ds.getMaxTotal());
                assertEquals(Duration.ofMillis(123), ds.getMaxWaitDuration());
                assertTrue(ds.getRemoveAbandonedOnBorrow());
                assertNull(ds.getConnectionPool());
                final List<String> messages = StackMessageLog.getAll();
                assertEquals(warningCount + infoCount, messages.size(), messages.toString());
                assertEquals(warningCount, messages.stream().filter(message -> message.startsWith("Name = jdbc/test Property ")).count());
                assertEquals(infoCount, messages.stream().filter(message -> message.startsWith("Name = jdbc/test Ignoring unknown property: ")).count());
                if (warningCount != 0) {
                    assertTrue(messages.get(0).startsWith("Name = jdbc/test Property maxActive "), messages.toString());
                    assertTrue(messages.get(1).startsWith("Name = jdbc/test Property removeAbandoned "), messages.toString());
                    assertTrue(messages.get(2).startsWith("Name = jdbc/test Property maxWait "), messages.toString());
                }
                if (infoCount != 0) {
                    assertEquals("Name = jdbc/test Ignoring unknown property: \"passwd\"", messages.get(3));
                    assertEquals("Name = jdbc/test Ignoring unknown property: \"customProperty\"", messages.get(4));
                    assertEquals("Name = jdbc/test Ignoring unknown property: \"passwd\"", messages.get(5));
                }
                messages.forEach(message -> assertFalse(message.contains("secret-"), message));
            }
        } finally {
            log.setLevel(originalLevel);
            StackMessageLog.clear();
            StackMessageLog.unLock();
        }
    }

    @Test
    void testNoProperties() throws Exception {
        final Properties properties = new Properties();
        try (final BasicDataSource ds = BasicDataSourceFactory.createDataSource(properties)) {
            assertNotNull(ds);
        }
    }

    @ParameterizedTest
    @MethodSource("obsoletePropertyValues")
    void testObsoletePropertyValuesAreNotLogged(final String propertyName, final String guidance, final String value,
            final boolean named) throws Exception {
        final Reference ref = new Reference("javax.sql.DataSource", BasicDataSourceFactory.class.getName(), null);
        ref.add(new StringRefAddr(propertyName, value));
        final List<String> messages = getMessages(ref, named ? new CompositeName("jdbc/test") : null);
        assertEquals(1, messages.size(), messages.toString());
        final String message = messages.get(0);
        assertTrue(message.startsWith((named ? "Name = jdbc/test " : "") + "Property " + propertyName + " is not used in DBCP2"), message);
        assertTrue(message.contains(guidance), message);
        assertTrue(message.endsWith("You have set a value for \"" + propertyName + "\" property, which is being ignored."), message);
        if (value != null && !value.isEmpty()) {
            assertFalse(message.contains(value), message);
        }
    }

    @Test
    void testProperties() throws Exception {
        try (final BasicDataSource ds = BasicDataSourceFactory.createDataSource(getTestProperties())) {
            checkDataSourceProperties(ds);
        }
    }

    @ParameterizedTest
    @MethodSource("unknownPropertyValues")
    void testUnknownPropertyValuesAreNotLogged(final String propertyName, final String value, final boolean named) throws Exception {
        final Reference ref = new Reference("javax.sql.DataSource", BasicDataSourceFactory.class.getName(), null);
        ref.add(new StringRefAddr(propertyName, value));
        final List<String> messages = getMessages(ref, named ? new CompositeName("jdbc/test") : null);
        assertEquals(1, messages.size(), messages.toString());
        assertEquals((named ? "Name = jdbc/test " : "") + "Ignoring unknown property: \"" + propertyName + "\"", messages.get(0));
    }

    @Test
    void testValidateProperties() throws Exception {
        try {
            StackMessageLog.lock();
            StackMessageLog.clear();
            final Reference ref = new Reference("javax.sql.DataSource", BasicDataSourceFactory.class.getName(), null);
            ref.add(new StringRefAddr("foo", "bar")); // Unknown
            ref.add(new StringRefAddr("maxWait", "100")); // Changed
            ref.add(new StringRefAddr("driverClassName", "org.apache.commons.dbcp2.TesterDriver")); // OK
            final BasicDataSourceFactory basicDataSourceFactory = new BasicDataSourceFactory();
            basicDataSourceFactory.getObjectInstance(ref, null, null, null);
            final List<String> messages = StackMessageLog.getAll();
            assertEquals(2, messages.size(), messages.toString());
            for (final String message : messages) {
                if (message.contains("maxWait")) {
                    assertTrue(message.contains("use maxWaitMillis"));
                } else {
                    assertTrue(message.contains("foo"));
                    assertTrue(message.contains("Ignoring unknown property"));
                }
            }
        } finally {
            StackMessageLog.clear();
            StackMessageLog.unLock();
        }
    }

    @Test
    void testValuesAreNotLoggedWhenDataSourceCreationFails() {
        try {
            StackMessageLog.lock();
            StackMessageLog.clear();
            final Reference ref = new Reference("javax.sql.DataSource", BasicDataSourceFactory.class.getName(), null);
            ref.add(new StringRefAddr("passwd", "secret-password"));
            ref.add(new StringRefAddr("maxWait", "secret-obsolete"));
            ref.add(new StringRefAddr("maxTotal", "invalid-integer"));
            assertThrows(NumberFormatException.class, () -> new BasicDataSourceFactory().getObjectInstance(ref, null, null, null));
            final List<String> messages = StackMessageLog.getAll();
            assertEquals(2, messages.size(), messages.toString());
            assertTrue(messages.get(0).contains("use maxWaitMillis instead."), messages.toString());
            assertEquals("Ignoring unknown property: \"passwd\"", messages.get(1));
            messages.forEach(message -> assertFalse(message.contains("secret-"), message));
        } finally {
            StackMessageLog.clear();
            StackMessageLog.unLock();
        }
    }
}
