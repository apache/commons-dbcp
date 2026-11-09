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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Properties;

import org.apache.commons.dbcp2.cpdsadapter.DriverAdapterCPDS;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 */
public class TestInstanceKeyDataSource {

    private static final class ThrowOnSetupDefaultsDataSource extends SharedPoolDataSource {

        private static final long serialVersionUID = -448025812063133259L;

        ThrowOnSetupDefaultsDataSource() {
        }

        @Override
        protected void setupDefaults(final Connection connection, final String userName) throws SQLException {
            throw new SQLException("bang!");
        }
    }

    private static final String DRIVER = "org.apache.commons.dbcp2.TesterDriver";
    private static final String URL = "jdbc:apache:commons:testdriver";
    private static final String USER = "foo";
    private static final String PASS = "bar";

    private DriverAdapterCPDS pcds;
    private SharedPoolDataSource spds;

    @BeforeEach
    public void setUp() throws ClassNotFoundException {
        pcds = new DriverAdapterCPDS();
        pcds.setDriver(DRIVER);
        pcds.setUrl(URL);
        pcds.setUser(USER);
        pcds.setPassword(PASS);
        pcds.setPoolPreparedStatements(false);
        spds = new SharedPoolDataSource();
        spds.setConnectionPoolDataSource(pcds);
    }

    @AfterEach
    public void tearDown() throws Exception {
        spds.close();
    }

    @Test
    void testConnection() throws SQLException, ClassNotFoundException {
        spds = new SharedPoolDataSource();
        pcds.setDriver(DRIVER);
        pcds.setUrl(URL);
        spds.setConnectionPoolDataSource(pcds);
        final PooledConnectionAndInfo info = spds.getPooledConnectionAndInfo(null, null);
        assertNull(info.getUserName());
        assertNull(info.getPassword());
        try (final Connection conn = spds.getConnection()) {
            assertNotNull(conn);
        }
    }

    @Test
    void testConnectionPoolDataSource() {
        assertEquals(pcds, spds.getConnectionPoolDataSource());
    }

    @Test
    void testConnectionPoolDataSourceAlreadySet() {
        assertThrows(IllegalStateException.class, () -> spds.setConnectionPoolDataSource(new DriverAdapterCPDS()));
    }

    @Test
    void testConnectionPoolDataSourceAlreadySetUsingJndi() {
        spds = new SharedPoolDataSource();
        spds.setDataSourceName("anything");
        assertThrows(IllegalStateException.class, () -> spds.setConnectionPoolDataSource(new DriverAdapterCPDS()));
    }

    @Test
    void testDataSourceName() {
        spds = new SharedPoolDataSource();
        assertNull(spds.getDataSourceName());
        spds.setDataSourceName("anything");
        assertEquals("anything", spds.getDataSourceName());
    }

    @Test
    void testDataSourceNameAlreadySet() {
        assertThrows(IllegalStateException.class, () -> spds.setDataSourceName("anything"));
    }

    @Test
    void testDataSourceNameAlreadySetUsingJndi() {
        spds = new SharedPoolDataSource();
        spds.setDataSourceName("anything");
        assertThrows(IllegalStateException.class, () -> spds.setDataSourceName("anything"));
    }

    @Test
    void testDefaultBlockWhenExhausted() {
        spds.setDefaultBlockWhenExhausted(true);
        assertTrue(spds.getDefaultBlockWhenExhausted());
        spds.setDefaultBlockWhenExhausted(false);
        assertFalse(spds.getDefaultBlockWhenExhausted());
    }

    @Test
    void testDefaultEvictionPolicyClassName() {
        spds.setDefaultEvictionPolicyClassName(Object.class.getName());
        assertEquals(Object.class.getName(), spds.getDefaultEvictionPolicyClassName());
    }

    @Test
    void testDefaultLifo() {
        spds.setDefaultLifo(true);
        assertTrue(spds.getDefaultLifo());
        spds.setDefaultLifo(false);
        assertFalse(spds.getDefaultLifo());
    }

    @Test
    void testDefaultMinIdle() {
        spds.setDefaultMinIdle(10);
        assertEquals(10, spds.getDefaultMinIdle());
    }

    @Test
    void testDefaultReadOnly() {
        spds.setDefaultReadOnly(true);
        assertTrue(spds.isDefaultReadOnly());
        spds.setDefaultReadOnly(false);
        assertFalse(spds.isDefaultReadOnly());
    }

    @Test
    void testDefaultSoftMinEvictableIdleTimeMillis() {
        spds.setDefaultSoftMinEvictableIdleTimeMillis(10);
        assertEquals(10, spds.getDefaultSoftMinEvictableIdleTimeMillis());
    }

    @Test
    void testDefaultTestOnCreate() {
        spds.setDefaultTestOnCreate(false);
        assertFalse(spds.getDefaultTestOnCreate());
        spds.setDefaultTestOnCreate(true);
        assertTrue(spds.getDefaultTestOnCreate());
    }

    @Test
    void testDefaultTransactionIsolation() {
        assertEquals(InstanceKeyDataSource.UNKNOWN_TRANSACTIONISOLATION, spds.getDefaultTransactionIsolation());
        spds.setDefaultTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        assertEquals(Connection.TRANSACTION_READ_COMMITTED, spds.getDefaultTransactionIsolation());
    }

    @Test
    void testDefaultTransactionIsolationInvalid() {
        assertEquals(InstanceKeyDataSource.UNKNOWN_TRANSACTIONISOLATION, spds.getDefaultTransactionIsolation());
        assertThrows(IllegalArgumentException.class, () -> spds.setDefaultTransactionIsolation(Integer.MAX_VALUE));
    }

    @Test
    void testDescription() {
        spds.setDescription("anything");
        assertEquals("anything", spds.getDescription());
    }

    /**
     * Verify that exception on setupDefaults does not leak PooledConnection
     *
     * JIRA: DBCP-237
     * @throws Exception
     */
    @Test
    void testExceptionOnSetupDefaults() throws Exception {
        try (final ThrowOnSetupDefaultsDataSource tds = new ThrowOnSetupDefaultsDataSource()) {
            final int numConnections = tds.getNumActive();
            assertThrows(SQLException.class, () -> tds.getConnection(USER, PASS));
            assertEquals(numConnections, tds.getNumActive());
        }
    }

    @Test
    void testIsWrapperFor() throws Exception {
        assertTrue(spds.isWrapperFor(InstanceKeyDataSource.class));
        assertTrue(spds.isWrapperFor(AutoCloseable.class));
    }

    @Test
    void testJndiEnvironment() {
        assertNull(spds.getJndiEnvironment("name"));
        final Properties properties = new Properties();
        properties.setProperty("name", "clarke");
        spds.setJndiEnvironment(properties);
        assertEquals("clarke", spds.getJndiEnvironment("name"));
        spds.setJndiEnvironment("name", "asimov");
        assertEquals("asimov", spds.getJndiEnvironment("name"));
    }

    @Test
    void testJndiNullProperties() {
        assertThrows(NullPointerException.class, () -> spds.setJndiEnvironment(null));
    }

    @Test
    void testJndiPropertiesCleared() {
        spds.setJndiEnvironment("name", "king");
        assertEquals("king", spds.getJndiEnvironment("name"));
        final Properties properties = new Properties();
        properties.setProperty("fish", "kohi");
        spds.setJndiEnvironment(properties);
        assertNull(spds.getJndiEnvironment("name"));
    }

    @Test
    void testJndiPropertiesNotInitialized() {
        assertNull(spds.getJndiEnvironment("name"));
        spds.setJndiEnvironment("name", "king");
        assertEquals("king", spds.getJndiEnvironment("name"));
    }

    @Test
    void testLoginTimeout() {
        spds.setLoginTimeout(10);
        assertEquals(10, spds.getLoginTimeout());
    }

    @SuppressWarnings("resource")
    @Test
    void testLogWriter() {
        spds.setLogWriter(new PrintWriter(System.out));
        assertNotNull(spds.getLogWriter());
    }

    @SuppressWarnings("resource")
    @Test
    void testLogWriterAutoInitialized() {
        assertNotNull(spds.getLogWriter());
    }

    @Test
    void testMaxConnLifetimeMillis() {
        assertEquals(-1, spds.getMaxConnLifetimeMillis());
        spds.setMaxConnLifetimeMillis(10);
        assertEquals(10, spds.getMaxConnLifetimeMillis());
    }

    @Test
    void testRollbackAfterValidation() {
        assertFalse(spds.isRollbackAfterValidation());
        spds.setRollbackAfterValidation(true);
        assertTrue(spds.isRollbackAfterValidation());
    }

    @Test
    void testRollbackAfterValidationWithConnectionCalled() throws SQLException {
        try (Connection connection = spds.getConnection()) {
            assertFalse(spds.isRollbackAfterValidation());
            assertThrows(IllegalStateException.class, () -> spds.setRollbackAfterValidation(true));
        }
    }

    @SuppressWarnings("resource")
    @Test
    void testUnwrap() throws Exception {
        assertSame(spds.unwrap(InstanceKeyDataSource.class), spds);
        assertSame(spds.unwrap(AutoCloseable.class), spds);
    }

    @Test
    void testValidationQuery() {
        assertNull(spds.getValidationQuery());
        spds.setValidationQuery("anything");
        assertEquals("anything", spds.getValidationQuery());
    }

    @Test
    void testValidationQueryTimeout() {
        assertEquals(-1, spds.getValidationQueryTimeout());
        spds.setValidationQueryTimeout(10);
        assertEquals(10, spds.getValidationQueryTimeout());
    }

    @Test
    void testValidationQueryTimeoutDuration() {
        assertEquals(Duration.ofSeconds(-1), spds.getValidationQueryTimeoutDuration());
        spds.setValidationQueryTimeout(Duration.ofSeconds(10));
        assertEquals(Duration.ofSeconds(10), spds.getValidationQueryTimeoutDuration());
    }

    @Test
    void testValidationQueryWithConnectionCalled() throws SQLException {
        try (Connection connection = spds.getConnection()) {
            assertNull(spds.getValidationQuery());
            assertThrows(IllegalStateException.class, () -> spds.setValidationQuery("anything"));
        }
    }
}
