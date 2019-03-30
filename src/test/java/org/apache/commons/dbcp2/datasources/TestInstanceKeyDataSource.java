/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.commons.dbcp2.cpdsadapter.DriverAdapterCPDS;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 */
public class TestInstanceKeyDataSource {

    private DriverAdapterCPDS pcds;
    private SharedPoolDataSource spds;

    private final static String DRIVER = "org.apache.commons.dbcp2.TesterDriver";
    private final static String URL = "jdbc:apache:commons:testdriver";
    private final static String USER = "foo";
    private final static String PASS = "bar";

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

    /**
     * Verify that exception on setupDefaults does not leak PooledConnection
     *
     * JIRA: DBCP-237
     * @throws Exception
     */
    @Test
    public void testExceptionOnSetupDefaults() throws Exception {
        final ThrowOnSetupDefaultsDataSource tds = new ThrowOnSetupDefaultsDataSource();
        final int numConnections = tds.getNumActive();
        try {
            tds.getConnection(USER, PASS);
            fail("Expecting SQLException");
        } catch (final SQLException ex) {
           //Expected
        }
        assertEquals(numConnections,tds.getNumActive());
        tds.close();
    }

    private static class ThrowOnSetupDefaultsDataSource
    extends SharedPoolDataSource {
        private static final long serialVersionUID = -448025812063133259L;

        ThrowOnSetupDefaultsDataSource() {
            super();
        }
        @Override
        protected void setupDefaults(final Connection connection, final String userName)
        throws  SQLException {
            throw new SQLException("bang!");
        }
    }

    @Test
    public void testConnectionPoolDataSource() {
        assertEquals(pcds, spds.getConnectionPoolDataSource());
    }

    @Test
    public void testConnectionPoolDataSourceAlreadySet() {
        assertThrows(IllegalStateException.class, () -> spds.setConnectionPoolDataSource(new DriverAdapterCPDS()));
    }

    @Test
    public void testConnectionPoolDataSourceAlreadySetUsingJndi() {
        spds = new SharedPoolDataSource();
        spds.setDataSourceName("anything");
        assertThrows(IllegalStateException.class, () -> spds.setConnectionPoolDataSource(new DriverAdapterCPDS()));
    }

    @Test
    public void testDataSourceName() {
        spds = new SharedPoolDataSource();
        assertNull(spds.getDataSourceName());
        spds.setDataSourceName("anything");
        assertEquals("anything", spds.getDataSourceName());
    }

    @Test
    public void testDataSourceNameAlreadySet() {
        assertThrows(IllegalStateException.class, () -> spds.setDataSourceName("anything"));
    }

    @Test
    public void testDataSourceNameAlreadySetUsingJndi() {
        spds = new SharedPoolDataSource();
        spds.setDataSourceName("anything");
        assertThrows(IllegalStateException.class, () -> spds.setDataSourceName("anything"));
    }

    @Test
    public void testDefaultTransactionIsolation() {
        assertEquals(InstanceKeyDataSource.UNKNOWN_TRANSACTIONISOLATION, spds.getDefaultTransactionIsolation());
        spds.setDefaultTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        assertEquals(Connection.TRANSACTION_READ_COMMITTED, spds.getDefaultTransactionIsolation());
    }

    @Test
    public void testDefaultTransactionIsolationInvalid() {
        assertEquals(InstanceKeyDataSource.UNKNOWN_TRANSACTIONISOLATION, spds.getDefaultTransactionIsolation());
        assertThrows(IllegalArgumentException.class, () -> spds.setDefaultTransactionIsolation(Integer.MAX_VALUE));
    }

    @Test
    public void testDescription() {
        spds.setDescription("anything");
        assertEquals("anything", spds.getDescription());
    }

    @Test
    public void testJndiNullProperties() {
        assertThrows(NullPointerException.class, () -> spds.setJndiEnvironment(null));
    }

    @Test
    public void testJndiPropertiesNotInitialized() {
        assertNull(spds.getJndiEnvironment("name"));
        spds.setJndiEnvironment("name", "king");
        assertEquals("king", spds.getJndiEnvironment("name"));
    }

    @Test
    public void testJndiPropertiesCleared() {
        spds.setJndiEnvironment("name", "king");
        assertEquals("king", spds.getJndiEnvironment("name"));
        final Properties properties = new Properties();
        properties.setProperty("fish", "kohi");
        spds.setJndiEnvironment(properties);
        assertNull(spds.getJndiEnvironment("name"));
    }

    @Test
    public void testJndiEnvironment() {
        assertNull(spds.getJndiEnvironment("name"));
        final Properties properties = new Properties();
        properties.setProperty("name", "clarke");
        spds.setJndiEnvironment(properties);
        assertEquals("clarke", spds.getJndiEnvironment("name"));
        spds.setJndiEnvironment("name", "asimov");
        assertEquals("asimov", spds.getJndiEnvironment("name"));
    }

    @Test
    public void testLoginTimeout() {
        spds.setLoginTimeout(10);
        assertEquals(10, spds.getLoginTimeout());
    }

    @Test
    public void testLogWriterAutoInitialized() {
        assertNotNull(spds.getLogWriter());
    }

    @Test
    public void testLogWriter() {
        spds.setLogWriter(new PrintWriter(System.out));
        assertNotNull(spds.getLogWriter());
    }

    @Test
    public void testValidationQuery() {
        assertNull(spds.getValidationQuery());
        spds.setValidationQuery("anything");
        assertEquals("anything", spds.getValidationQuery());
    }

    @Test
    public void testValidationQueryWithConnectionCalled() throws SQLException {
        spds.getConnection();
        assertNull(spds.getValidationQuery());
        assertThrows(IllegalStateException.class, () -> spds.setValidationQuery("anything"));
    }

    @Test
    public void testValidationQueryTimeout() {
        assertEquals(-1, spds.getValidationQueryTimeout());
        spds.setValidationQueryTimeout(10);
        assertEquals(10, spds.getValidationQueryTimeout());
    }

    @Test
    public void testRollbackAfterValidation() {
        assertFalse(spds.isRollbackAfterValidation());
        spds.setRollbackAfterValidation(true);
        assertEquals(true, spds.isRollbackAfterValidation());
    }

    @Test
    public void testRollbackAfterValidationWithConnectionCalled() throws SQLException {
        spds.getConnection();
        assertFalse(spds.isRollbackAfterValidation());
        assertThrows(IllegalStateException.class, () -> spds.setRollbackAfterValidation(true));
    }

    @Test
    public void testMaxConnLifetimeMillis() {
        assertEquals(-1, spds.getMaxConnLifetimeMillis());
        spds.setMaxConnLifetimeMillis(10);
        assertEquals(10, spds.getMaxConnLifetimeMillis());
    }

    @Test
    public void testDefaultReadOnly() {
        spds.setDefaultReadOnly(true);
        assertTrue(spds.isDefaultReadOnly());
        spds.setDefaultReadOnly(false);
        assertFalse(spds.isDefaultReadOnly());
    }

    @Test
    public void testDefaultTestOnCreate() {
        spds.setDefaultTestOnCreate(false);
        assertFalse(spds.getDefaultTestOnCreate());
        spds.setDefaultTestOnCreate(true);
        assertTrue(spds.getDefaultTestOnCreate());
    }

    @Test
    public void testDefaultSoftMinEvictableIdleTimeMillis() {
        spds.setDefaultSoftMinEvictableIdleTimeMillis(10);
        assertEquals(10, spds.getDefaultSoftMinEvictableIdleTimeMillis());
    }

    @Test
    public void testDefaultMinIdle() {
        spds.setDefaultMinIdle(10);
        assertEquals(10, spds.getDefaultMinIdle());
    }

    @Test
    public void testDefaultLifo() {
        spds.setDefaultLifo(true);
        assertTrue(spds.getDefaultLifo());
        spds.setDefaultLifo(false);
        assertFalse(spds.getDefaultLifo());
    }

    @Test
    public void testDefaultEvictionPolicyClassName() {
        spds.setDefaultEvictionPolicyClassName(Object.class.getName());
        assertEquals(Object.class.getName(), spds.getDefaultEvictionPolicyClassName());
    }

    @Test
    public void testDefaultBlockWhenExhausted() {
        spds.setDefaultBlockWhenExhausted(true);
        assertTrue(spds.getDefaultBlockWhenExhausted());
        spds.setDefaultBlockWhenExhausted(false);
        assertFalse(spds.getDefaultBlockWhenExhausted());
    }

    @Test
    public void testConnection() throws SQLException, ClassNotFoundException {
        spds = new SharedPoolDataSource();
        pcds.setDriver(DRIVER);
        pcds.setUrl(URL);
        spds.setConnectionPoolDataSource(pcds);
        final PooledConnectionAndInfo info = spds.getPooledConnectionAndInfo(null, null);
        assertNull(info.getUsername());
        assertNull(info.getPassword());
        final Connection conn = spds.getConnection();
        assertNotNull(conn);
    }
}
