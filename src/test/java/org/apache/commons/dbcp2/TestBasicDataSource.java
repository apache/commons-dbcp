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

package org.apache.commons.dbcp2;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import javax.management.AttributeNotFoundException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.sql.DataSource;

import org.apache.commons.logging.LogFactory;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * TestSuite for BasicDataSource
 */
public class TestBasicDataSource extends TestConnectionPool {

    private static final String CATALOG = "test catalog";

    @BeforeAll
    public static void setUpClass() {
        // register a custom logger which supports inspection of the log messages
        LogFactory.getFactory().setAttribute("org.apache.commons.logging.Log", "org.apache.commons.dbcp2.StackMessageLog");
    }
    protected BasicDataSource ds;

    protected BasicDataSource createDataSource() throws Exception {
        return new BasicDataSource();
    }

    @Override
    protected Connection getConnection() throws Exception {
        return ds.getConnection();
    }

    @BeforeEach
    public void setUp() throws Exception {
        ds = createDataSource();
        ds.setDriverClassName("org.apache.commons.dbcp2.TesterDriver");
        ds.setUrl("jdbc:apache:commons:testdriver");
        ds.setMaxTotal(getMaxTotal());
        ds.setMaxWait(getMaxWaitDuration());
        ds.setDefaultAutoCommit(Boolean.TRUE);
        ds.setDefaultReadOnly(Boolean.FALSE);
        ds.setDefaultTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        ds.setDefaultCatalog(CATALOG);
        ds.setUsername("userName");
        ds.setPassword("password");
        ds.setValidationQuery("SELECT DUMMY FROM DUAL");
        ds.setConnectionInitSqls(Arrays.asList("SELECT 1", "SELECT 2"));
        ds.setDriverClassLoader(new TesterClassLoader());
        ds.setJmxName("org.apache.commons.dbcp2:name=test");
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
        ds.close();
        ds = null;
    }

    @Test
    public void testAccessToUnderlyingConnectionAllowed() throws Exception {
        ds.setAccessToUnderlyingConnectionAllowed(true);
        assertTrue(ds.isAccessToUnderlyingConnectionAllowed());

        try (final Connection conn = getConnection()) {
            Connection dconn = ((DelegatingConnection<?>) conn).getDelegate();
            assertNotNull(dconn);

            dconn = ((DelegatingConnection<?>) conn).getInnermostDelegate();
            assertNotNull(dconn);

            assertTrue(dconn instanceof TesterConnection);
        }
    }

    @Test
    public void testClose() throws Exception {
        ds.setAccessToUnderlyingConnectionAllowed(true);

        // active connection is held open when ds is closed
        final Connection activeConnection = getConnection();
        final Connection rawActiveConnection = ((DelegatingConnection<?>) activeConnection).getInnermostDelegate();
        assertFalse(activeConnection.isClosed());
        assertFalse(rawActiveConnection.isClosed());

        // idle connection is in pool but closed
        final Connection idleConnection = getConnection();
        final Connection rawIdleConnection = ((DelegatingConnection<?>) idleConnection).getInnermostDelegate();
        assertFalse(idleConnection.isClosed());
        assertFalse(rawIdleConnection.isClosed());

        // idle wrapper should be closed but raw connection should be open
        idleConnection.close();
        assertTrue(idleConnection.isClosed());
        assertFalse(rawIdleConnection.isClosed());

        ds.close();

        // raw idle connection should now be closed
        assertTrue(rawIdleConnection.isClosed());

        // active connection should still be open
        assertFalse(activeConnection.isClosed());
        assertFalse(rawActiveConnection.isClosed());

        // now close the active connection
        activeConnection.close();

        // both wrapper and raw active connection should be closed
        assertTrue(activeConnection.isClosed());
        assertTrue(rawActiveConnection.isClosed());

        // Verify SQLException on getConnection after close
        assertThrows(SQLException.class, () -> getConnection());

        // Redundant close is OK
        ds.close();

    }

    @Test
    public void testConcurrentInitBorrow() throws Exception {
        ds.setDriverClassName("org.apache.commons.dbcp2.TesterConnectionDelayDriver");
        ds.setUrl("jdbc:apache:commons:testerConnectionDelayDriver:50");
        ds.setInitialSize(8);

        // Launch a request to trigger pool initialization
        final TestThread testThread = new TestThread(1, 0);
        final Thread t = new Thread(testThread);
        t.start();

        // Get another connection (should wait for pool init)
        Thread.sleep(100); // Make sure t gets into init first
        try (Connection conn = ds.getConnection()) {

            // Pool should have at least 6 idle connections now
            // Use underlying pool getNumIdle to avoid waiting for ds lock
            assertTrue(ds.getConnectionPool().getNumIdle() > 5);

            // Make sure t completes successfully
            t.join();
            assertFalse(testThread.failed());
        }
        ds.close();
    }

    /**
     * JIRA: DBCP-444
     * Verify that invalidate does not return closed connection to the pool.
     */
    @Test
    public void testConcurrentInvalidateBorrow() throws Exception {
        ds.setDriverClassName("org.apache.commons.dbcp2.TesterConnRequestCountDriver");
        ds.setUrl("jdbc:apache:commons:testerConnRequestCountDriver");
        ds.setTestOnBorrow(true);
        ds.setValidationQuery("SELECT DUMMY FROM DUAL");
        ds.setMaxTotal(8);
        ds.setLifo(true);
        ds.setMaxWait(Duration.ofMillis(-1));

        // Threads just borrow and return - validation will trigger close check
        final TestThread testThread1 = new TestThread(1000,0);
        final Thread t1 = new Thread(testThread1);
        t1.start();
        final TestThread testThread2 = new TestThread(1000,0);
        final Thread t2 = new Thread(testThread1);
        t2.start();

        // Grab and invalidate connections
        for (int i = 0; i < 1000; i++) {
            final Connection conn = ds.getConnection();
            ds.invalidateConnection(conn);
        }

        // Make sure borrow threads complete successfully
        t1.join();
        t2.join();
        assertFalse(testThread1.failed());
        assertFalse(testThread2.failed());

        ds.close();
    }

    /**
     * JIRA: DBCP-547
     * Verify that ConnectionFactory interface in BasicDataSource.createConnectionFactory().
     */
    @Test
    public void testCreateConnectionFactoryWithConnectionFactoryClassName() throws Exception {
        Properties properties = new Properties();
        // set ConnectionFactoryClassName
        properties = new Properties();
        properties.put("initialSize", "1");
        properties.put("driverClassName", "org.apache.commons.dbcp2.TesterDriver");
        properties.put("url", "jdbc:apache:commons:testdriver");
        properties.put("username", "foo");
        properties.put("password", "bar");
        properties.put("connectionFactoryClassName", "org.apache.commons.dbcp2.TesterConnectionFactory");
        try (BasicDataSource ds = BasicDataSourceFactory.createDataSource(properties)) {
            try (Connection conn = ds.getConnection()) {
                assertNotNull(conn);
            }
        }
    }

    /**
     * JIRA: DBCP-547
     * Verify that ConnectionFactory interface in BasicDataSource.createConnectionFactory().
     */
    @Test
    public void testCreateConnectionFactoryWithoutConnectionFactoryClassName() throws Exception {
        // not set ConnectionFactoryClassName
        final Properties properties = new Properties();
        properties.put("initialSize", "1");
        properties.put("driverClassName", "org.apache.commons.dbcp2.TesterDriver");
        properties.put("url", "jdbc:apache:commons:testdriver");
        properties.put("username", "foo");
        properties.put("password", "bar");
        try (BasicDataSource ds = BasicDataSourceFactory.createDataSource(properties)) {
            try (Connection conn = ds.getConnection()) {
                assertNotNull(conn);
            }
        }
    }

    /**
     * JIRA: DBCP-342, DBCP-93
     * Verify that when errors occur during BasicDataSource initialization, GenericObjectPool
     * Evictors are cleaned up.
     */
    @Test
    public void testCreateDataSourceCleanupEvictor() throws Exception {
        ds.close();
        ds = null;
        ds = createDataSource();
        ds.setDriverClassName("org.apache.commons.dbcp2.TesterConnRequestCountDriver");
        ds.setUrl("jdbc:apache:commons:testerConnRequestCountDriver");
        ds.setValidationQuery("SELECT DUMMY FROM DUAL");
        ds.setUsername("userName");

        // Make password incorrect, so createDataSource will throw
        ds.setPassword("wrong");
        // Set timeBetweenEvictionRuns > 0, so evictor will be created
        ds.setDurationBetweenEvictionRuns(Duration.ofMillis(100));
        // Set min idle > 0, so evictor will try to make connection as many as idle count
        ds.setMinIdle(2);

        // Prevent concurrent execution of threads executing test subclasses
        synchronized (TesterConnRequestCountDriver.class) {
            TesterConnRequestCountDriver.initConnRequestCount();

            // user request 10 times
            for (int i = 0; i < 10; i++) {
                try {
                    @SuppressWarnings("unused")
                    final DataSource ds2 = ds.createDataSource();
                } catch (final SQLException e) {
                    // Ignore
                }
            }

            // sleep 1000ms. evictor will be invoked 10 times if running.
            Thread.sleep(1000);

            // Make sure there have been no Evictor-generated requests (count should be 10, from requests above)
            assertEquals(10, TesterConnRequestCountDriver.getConnectionRequestCount());
        }

        // make sure cleanup is complete
        assertNull(ds.getConnectionPool());
    }

    /**
     * JIRA DBCP-93: If an SQLException occurs after the GenericObjectPool is
     * initialized in createDataSource, the evictor task is not cleaned up.
     */
    @Test
    public void testCreateDataSourceCleanupThreads() throws Exception {
        ds.close();
        ds = null;
        ds = createDataSource();
        ds.setDriverClassName("org.apache.commons.dbcp2.TesterDriver");
        ds.setUrl("jdbc:apache:commons:testdriver");
        ds.setMaxTotal(getMaxTotal());
        ds.setMaxWait(getMaxWaitDuration());
        ds.setDefaultAutoCommit(Boolean.TRUE);
        ds.setDefaultReadOnly(Boolean.FALSE);
        ds.setDefaultTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        ds.setDefaultCatalog(CATALOG);
        ds.setUsername("userName");
        // Set timeBetweenEvictionRuns > 0, so evictor is created
        ds.setDurationBetweenEvictionRuns(Duration.ofMillis(100));
        // Make password incorrect, so createDataSource will throw
        ds.setPassword("wrong");
        ds.setValidationQuery("SELECT DUMMY FROM DUAL");
        final int threadCount = Thread.activeCount();
        for (int i = 0; i < 10; i++) {
            try (Connection c = ds.getConnection()){
            } catch (final SQLException ex) {
                // ignore
            }
        }
        // Allow one extra thread for JRockit compatibility
        assertTrue(Thread.activeCount() <= threadCount + 1);
    }

    @Test
    public void testDefaultCatalog() throws Exception {
        final Connection[] c = new Connection[getMaxTotal()];
        for (int i = 0; i < c.length; i++) {
            c[i] = getConnection();
            assertNotNull(c[i]);
            assertEquals(CATALOG, c[i].getCatalog());
        }

        for (final Connection element : c) {
            element.setCatalog("error");
            element.close();
        }

        for (int i = 0; i < c.length; i++) {
            c[i] = getConnection();
            assertNotNull(c[i]);
            assertEquals(CATALOG, c[i].getCatalog());
        }

        for (final Connection element : c) {
            element.close();
        }
    }

    @Test
    public void testDeprecatedAccessors() throws SQLException {
        try (BasicDataSource bds = new BasicDataSource()) {
            int i = 0;
            //
            i++;
            bds.setDefaultQueryTimeout(i);
            assertEquals(i, bds.getDefaultQueryTimeout());
            assertEquals(Duration.ofSeconds(i), bds.getDefaultQueryTimeoutDuration());
            //
            i++;
            bds.setMaxConnLifetimeMillis(i);
            assertEquals(i, bds.getMaxConnLifetimeMillis());
            assertEquals(Duration.ofMillis(i), bds.getMaxConnDuration());
            //
            i++;
            bds.setMaxWaitMillis(i);
            assertEquals(i, bds.getMaxWaitMillis());
            assertEquals(Duration.ofMillis(i), bds.getMaxWaitDuration());
            //
            i++;
            bds.setMinEvictableIdleTimeMillis(i);
            assertEquals(i, bds.getMinEvictableIdleTimeMillis());
            assertEquals(Duration.ofMillis(i), bds.getMinEvictableIdleDuration());
            //
            i++;
            bds.setRemoveAbandonedTimeout(i);
            assertEquals(i, bds.getRemoveAbandonedTimeout());
            assertEquals(Duration.ofSeconds(i), bds.getRemoveAbandonedTimeoutDuration());
            //
            i++;
            bds.setSoftMinEvictableIdleTimeMillis(i);
            assertEquals(i, bds.getSoftMinEvictableIdleTimeMillis());
            assertEquals(Duration.ofMillis(i), bds.getSoftMinEvictableIdleDuration());
            //
            i++;
            bds.setTimeBetweenEvictionRunsMillis(i);
            assertEquals(i, bds.getTimeBetweenEvictionRunsMillis());
            assertEquals(Duration.ofMillis(i), bds.getDurationBetweenEvictionRuns());
            //
            i++;
            bds.setValidationQueryTimeout(1);
            assertEquals(1, bds.getValidationQueryTimeout());
            assertEquals(Duration.ofSeconds(1), bds.getValidationQueryTimeoutDuration());
        }
    }

    /**
     * JIRA: DBCP-437
     * Verify that BasicDataSource sets disconnect codes properties.
     * Functionality is verified in pcf tests.
     */
    @Test
    public void testDisconnectSqlCodes() throws Exception {
        final ArrayList<String> disconnectionSqlCodes = new ArrayList<>();
        disconnectionSqlCodes.add("XXX");
        ds.setDisconnectionSqlCodes(disconnectionSqlCodes);
        ds.setFastFailValidation(true);
        try (Connection conn = ds.getConnection()) { // Triggers initialization - pcf creation
            // Make sure factory got the properties
            final PoolableConnectionFactory pcf = (PoolableConnectionFactory) ds.getConnectionPool().getFactory();
            assertTrue(pcf.isFastFailValidation());
            assertTrue(pcf.getDisconnectionSqlCodes().contains("XXX"));
            assertEquals(1, pcf.getDisconnectionSqlCodes().size());
        }
    }

    /**
     * JIRA DBCP-333: Check that a custom class loader is used.
     * @throws Exception
     */
    @Test
    public void testDriverClassLoader() throws Exception {
        try (Connection conn = getConnection()) {
            final ClassLoader cl = ds.getDriverClassLoader();
            assertNotNull(cl);
            assertTrue(cl instanceof TesterClassLoader);
            assertTrue(((TesterClassLoader) cl).didLoad(ds.getDriverClassName()));
        }
    }

    @Test
    public void testEmptyInitConnectionSql() throws Exception {
        ds.setConnectionInitSqls(Arrays.asList("", "   "));
        assertNotNull(ds.getConnectionInitSqls());
        assertEquals(0, ds.getConnectionInitSqls().size());

        ds.setConnectionInitSqls(null);
        assertNotNull(ds.getConnectionInitSqls());
        assertEquals(0, ds.getConnectionInitSqls().size());
    }

    @Test
    public void testEmptyValidationQuery() throws Exception {
        assertNotNull(ds.getValidationQuery());

        ds.setValidationQuery("");
        assertNull(ds.getValidationQuery());

        ds.setValidationQuery("   ");
        assertNull(ds.getValidationQuery());
    }

    @Test
    @Disabled
    public void testEvict() throws Exception {
        final long delay = 1000;

        ds.setInitialSize(10);
        ds.setMaxIdle(10);
        ds.setMaxTotal(10);
        ds.setMinIdle(5);
        ds.setNumTestsPerEvictionRun(3);
        ds.setMinEvictableIdle(Duration.ofMillis(100));
        ds.setDurationBetweenEvictionRuns(Duration.ofMillis(delay));
        ds.setPoolPreparedStatements(true);

        try (Connection conn = ds.getConnection()) {
            // empty
        }

        final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        while (Stream.of(threadBean.getThreadInfo(threadBean.getAllThreadIds())).anyMatch(t -> t.getThreadName().equals("commons-pool-evictor-thread"))) {
            if (ds.getNumIdle() <= ds.getMinIdle()) {
                break;
            }
            Thread.sleep(delay);
        }
        if (ds.getNumIdle() > ds.getMinIdle()) {
            fail("EvictionTimer thread was destroyed with numIdle=" + ds.getNumIdle() + "(expected: less or equal than " + ds.getMinIdle() + ")");
        }
    }

    @Test
    public void testInitialSize() throws Exception {
        ds.setMaxTotal(20);
        ds.setMaxIdle(20);
        ds.setInitialSize(10);

        try (Connection conn = getConnection()) {
            assertNotNull(conn);
        }

        assertEquals(0, ds.getNumActive());
        assertEquals(10, ds.getNumIdle());
    }

    /**
     * JIRA: DBCP-482
     * Verify warning not logged if JMX MBean unregistered before close() called.
     */
    @Test
    public void testInstanceNotFoundExceptionLogSuppressed() throws Exception {
        final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        try (Connection c = ds.getConnection()) {
            // nothing
        }
        final ObjectName objectName = new ObjectName(ds.getJmxName());
        if (mbs.isRegistered(objectName)) {
            mbs.unregisterMBean(objectName);
        }
        StackMessageLog.clear();
        ds.close();
        assertThat(StackMessageLog.popMessage(),
                CoreMatchers.not(CoreMatchers.containsString("InstanceNotFoundException")));
        assertNull(ds.getRegisteredJmxName());
    }

    @Test
    public void testInvalidateConnection() throws Exception {
        ds.setMaxTotal(2);
        try (final Connection conn1 = ds.getConnection()) {
            try (final Connection conn2 = ds.getConnection()) {
                ds.invalidateConnection(conn1);
                assertTrue(conn1.isClosed());
                assertEquals(1, ds.getNumActive());
                assertEquals(0, ds.getNumIdle());
                try (final Connection conn3 = ds.getConnection()) {
                    conn2.close();
                }
            }
        }
    }

    @Test
    public void testInvalidConnectionInitSql() {
        ds.setConnectionInitSqls(Arrays.asList("SELECT 1", "invalid"));
        final SQLException e = assertThrows(SQLException.class, ds::getConnection);
        assertTrue(e.toString().contains("invalid"));
    }

    @Test
    public void testInvalidValidationQuery() {
        ds.setValidationQuery("invalid");
        final SQLException e = assertThrows(SQLException.class, ds::getConnection);
        assertTrue(e.toString().contains("invalid"));
    }

    // Bugzilla Bug 28251:  Returning dead database connections to BasicDataSource
    // isClosed() failure blocks returning a connection to the pool
    @Test
    public void testIsClosedFailure() throws SQLException {
        ds.setAccessToUnderlyingConnectionAllowed(true);
        final Connection conn = ds.getConnection();
        assertNotNull(conn);
        assertEquals(1, ds.getNumActive());

        // set an IO failure causing the isClosed method to fail
        final TesterConnection tconn = (TesterConnection) ((DelegatingConnection<?>) conn).getInnermostDelegate();
        tconn.setFailure(new IOException("network error"));

        assertThrows(SQLException.class, () -> conn.close());

        assertEquals(0, ds.getNumActive());
    }

    @Test
    public void testIsWrapperFor() throws Exception {
        assertTrue(ds.isWrapperFor(BasicDataSource.class));
        assertTrue(ds.isWrapperFor(AutoCloseable.class));
        assertFalse(ds.isWrapperFor(String.class));
        assertFalse(ds.isWrapperFor(null));
    }

    /**
     * Make sure setting jmxName to null suppresses JMX registration of connection and statement pools.
     * JIRA: DBCP-434
     */
    @Test
    public void testJmxDisabled() throws Exception {
        final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        // Unregister leftovers from other tests (TODO: worry about concurrent test execution)
        final ObjectName commons = new ObjectName("org.apache.commons.*:*");
        final Set<ObjectName> results = mbs.queryNames(commons, null);
        for (final ObjectName result : results) {
            mbs.unregisterMBean(result);
        }
        ds.setJmxName(null); // Should disable JMX for both connection and statement pools
        ds.setPoolPreparedStatements(true);
        try (Connection conn = ds.getConnection()) { // Trigger initialization
            // Nothing should be registered
            assertEquals(0, mbs.queryNames(commons, null).size());
        }
    }

    /**
     * Test disabling MBean registration for Connection objects.
     * JIRA: DBCP-585
     */
    @Test
    public void testConnectionMBeansDisabled() throws Exception {
        final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        // Unregister leftovers from other tests (TODO: worry about concurrent test execution)
        final ObjectName commons = new ObjectName("org.apache.commons.*:*");
        final Set<ObjectName> results = mbs.queryNames(commons, null);
        for (final ObjectName result : results) {
            mbs.unregisterMBean(result);
        }
        ds.setRegisterConnectionMBean(false); // Should disable Connection MBean registration
        try (Connection conn = ds.getConnection()) { // Trigger initialization
            // No Connection MBeans shall be registered
            final ObjectName connections = new ObjectName("org.apache.commons.*:connection=*,*");
            assertEquals(0, mbs.queryNames(connections, null).size());
        }
    }

    /**
     * Tests JIRA <a href="https://issues.apache.org/jira/browse/DBCP-562">DBCP-562</a>.
     * <p>
     * Make sure Password Attribute is not exported via JMXBean.
     * </p>
     */
    @Test
    public void testJmxDoesNotExposePassword() throws Exception {
        final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        try (Connection c = ds.getConnection()) {
            // nothing
        }
        final ObjectName objectName = new ObjectName(ds.getJmxName());

        final MBeanAttributeInfo[] attributes = mbs.getMBeanInfo(objectName).getAttributes();

        assertTrue(attributes != null && attributes.length > 0);

        Arrays.asList(attributes).forEach(attrInfo -> {
            assertFalse("password".equalsIgnoreCase(attrInfo.getName()));
        });

        assertThrows(AttributeNotFoundException.class, () -> {
            mbs.getAttribute(objectName, "Password");
        });
    }

    @Test
    public void testManualConnectionEvict() throws Exception {
        ds.setMinIdle(0);
        ds.setMaxIdle(4);
        ds.setMinEvictableIdle(Duration.ofMillis(10));
        ds.setNumTestsPerEvictionRun(2);

        try (Connection ds2 = ds.createDataSource().getConnection(); Connection ds3 = ds.createDataSource().getConnection()) {
            assertEquals(0, ds.getNumIdle());
        }

        // Make sure MinEvictableIdleTimeMillis has elapsed
        Thread.sleep(100);

        // Ensure no connections evicted by eviction thread
        assertEquals(2, ds.getNumIdle());

        // Force Eviction
        ds.evict();

        // Ensure all connections evicted
        assertEquals(0, ds.getNumIdle());
    }

    @Test
    public void testMaxConnLifetimeExceeded() throws Exception {
        try {
            StackMessageLog.lock();
            ds.setMaxConn(Duration.ofMillis(100));
            try (Connection conn = ds.getConnection()) {
                assertEquals(1, ds.getNumActive());
                Thread.sleep(500);
            }
            assertEquals(0, ds.getNumIdle());
            final String message = StackMessageLog.popMessage();
            Assertions.assertNotNull(message);
            assertTrue(message.indexOf("exceeds the maximum permitted value") > 0);
        } finally {
            StackMessageLog.clear();
            StackMessageLog.unLock();
        }
    }

    @Test
    public void testMaxConnLifetimeExceededMutedLog() throws Exception {
        try {
            StackMessageLog.lock();
            StackMessageLog.clear();
            ds.setMaxConn(Duration.ofMillis(100));
            ds.setLogExpiredConnections(false);
            try (final Connection conn = ds.getConnection()) {
                assertEquals(1, ds.getNumActive());
                Thread.sleep(500);
            }
            assertEquals(0, ds.getNumIdle());
            assertTrue(StackMessageLog.isEmpty(), StackMessageLog.getAll().toString());
        } finally {
            StackMessageLog.clear();
            StackMessageLog.unLock();
        }
    }

    /**
     * Bugzilla Bug 29832: Broken behavior for BasicDataSource.setMaxTotal(0)
     * MaxTotal == 0 should throw SQLException on getConnection.
     * Results from Bug 29863 in commons-pool.
     */
    @Test
    public void testMaxTotalZero() throws Exception {
        ds.setMaxTotal(0);
        assertThrows(SQLException.class, ds::getConnection);
    }

    /**
     * JIRA: DBCP-457
     * Verify that changes made to abandoned config are passed to the underlying
     * pool.
     */
    @Test
    public void testMutateAbandonedConfig() throws Exception {
        final Properties properties = new Properties();
        properties.put("initialSize", "1");
        properties.put("driverClassName", "org.apache.commons.dbcp2.TesterDriver");
        properties.put("url", "jdbc:apache:commons:testdriver");
        properties.put("username", "foo");
        properties.put("password", "bar");
        try (BasicDataSource ds = BasicDataSourceFactory.createDataSource(properties)) {
            final boolean original = ds.getConnectionPool().getLogAbandoned();
            ds.setLogAbandoned(!original);
            Assertions.assertNotEquals(original, ds.getConnectionPool().getLogAbandoned());
        }
    }

    @Test
    public void testNoAccessToUnderlyingConnectionAllowed() throws Exception {
        // default: false
        assertFalse(ds.isAccessToUnderlyingConnectionAllowed());

        try (Connection conn = getConnection()) {
            Connection dconn = ((DelegatingConnection<?>) conn).getDelegate();
            assertNull(dconn);

            dconn = ((DelegatingConnection<?>) conn).getInnermostDelegate();
            assertNull(dconn);
        }
    }

    /**
     * Verifies correct handling of exceptions generated by the underlying pool as it closes
     * connections in response to BDS#close. Exceptions have to be either swallowed by the
     * underlying pool and logged, or propagated and wrapped.
     */
    @Test
    public void testPoolCloseCheckedException() throws Exception {
        ds.setAccessToUnderlyingConnectionAllowed(true);  // Allow dirty tricks

        final TesterConnection tc;
        // Get an idle connection into the pool
        try (Connection conn = ds.getConnection()) {
            tc = (TesterConnection) ((DelegatingConnection<?>) conn).getInnermostDelegate();
        }

        // After returning the connection to the pool, bork it.
        // Don't try this at home - bad violation of pool contract!
        tc.setFailure(new SQLException("bang"));

        // Now close Datasource, which will cause tc to be closed, triggering SQLE
        // Pool 2.x swallows and logs exceptions on pool close.  Below verifies that
        // Either exceptions get logged or wrapped appropriately.
        try {
            StackMessageLog.lock();
            StackMessageLog.clear();
            ds.close();
            // Exception must have been swallowed by the pool - verify it is logged
            final String message = StackMessageLog.popMessage();
            Assertions.assertNotNull(message);
            assertTrue(message.indexOf("bang") > 0);
        } catch (final SQLException ex) {
            assertTrue(ex.getMessage().indexOf("Cannot close") > 0);
            assertTrue(ex.getCause().getMessage().indexOf("bang") > 0);
        } finally {
            StackMessageLog.unLock();
        }
    }

    @Test
    public void testPoolCloseRTE() throws Exception {
        // RTE version of testPoolCloseCheckedException - see comments there.
        ds.setAccessToUnderlyingConnectionAllowed(true);
        final TesterConnection tc;
        try (Connection conn = ds.getConnection()) {
            tc = (TesterConnection) ((DelegatingConnection<?>) conn).getInnermostDelegate();
        }
        tc.setFailure(new IllegalStateException("boom"));
        try {
            StackMessageLog.lock();
            StackMessageLog.clear();
            ds.close();
            final String message = StackMessageLog.popMessage();
            Assertions.assertNotNull(message);
            assertTrue(message.indexOf("boom") > 0);
        } catch (final IllegalStateException ex) {
            assertTrue(ex.getMessage().indexOf("boom") > 0); // RTE is not wrapped by BDS#close
        } finally {
            StackMessageLog.unLock();
        }
    }

    @Override
    @Test
    public void testPooling() throws Exception {
        // this also needs access to the underlying connection
        ds.setAccessToUnderlyingConnectionAllowed(true);
        super.testPooling();
    }

    /**
     * Bugzilla Bug 29054:
     * The BasicDataSource.setTestOnReturn(boolean) is not carried through to
     * the GenericObjectPool variable _testOnReturn.
     */
    @Test
    public void testPropertyTestOnReturn() throws Exception {
        ds.setValidationQuery("select 1 from dual");
        ds.setTestOnBorrow(false);
        ds.setTestWhileIdle(false);
        ds.setTestOnReturn(true);

        try (Connection conn = ds.getConnection()) {
            assertNotNull(conn);

            assertFalse(ds.getConnectionPool().getTestOnBorrow());
            assertFalse(ds.getConnectionPool().getTestWhileIdle());
            assertTrue(ds.getConnectionPool().getTestOnReturn());
        }
    }

    @Test
    public void testRestart() throws Exception {
        ds.setMaxTotal(2);
        ds.setDurationBetweenEvictionRuns(Duration.ofMillis(100));
        ds.setNumTestsPerEvictionRun(2);
        ds.setMinEvictableIdle(Duration.ofMinutes(1));
        ds.setInitialSize(2);
        ds.setDefaultCatalog("foo");
        try (Connection conn1 = ds.getConnection()) {
            Thread.sleep(200);
            // Now set some property that will not have effect until restart
            ds.setDefaultCatalog("bar");
            ds.setInitialSize(1);
            // restart will load new properties
            ds.restart();
            assertEquals("bar", ds.getDefaultCatalog());
            assertEquals(1, ds.getInitialSize());
            ds.getLogWriter(); // side effect is to init
            assertEquals(0, ds.getNumActive());
            assertEquals(1, ds.getNumIdle());
        }
        // verify old pool connection is not returned to pool
        assertEquals(1, ds.getNumIdle());
        ds.close();
    }

    /**
     * Bugzilla Bug 29055: AutoCommit and ReadOnly
     * The DaffodilDB driver throws an SQLException if
     * trying to commit or rollback a readOnly connection.
     */
    @Test
    public void testRollbackReadOnly() throws Exception {
        ds.setDefaultReadOnly(Boolean.TRUE);
        ds.setDefaultAutoCommit(Boolean.FALSE);

        try (Connection conn = ds.getConnection()) {
            assertNotNull(conn);
        }
    }

    @Test
    public void testSetAutoCommitTrueOnClose() throws Exception {
        ds.setAccessToUnderlyingConnectionAllowed(true);
        ds.setDefaultAutoCommit(Boolean.FALSE);
        final Connection dconn;
        try (Connection conn = getConnection()) {
            assertNotNull(conn);
            assertFalse(conn.getAutoCommit());

            dconn = ((DelegatingConnection<?>) conn).getInnermostDelegate();
            assertNotNull(dconn);
            assertFalse(dconn.getAutoCommit());

        }

        assertTrue(dconn.getAutoCommit());
    }

    @Test
    public void testSetProperties() throws Exception {
        // normal
        ds.setConnectionProperties("name1=value1;name2=value2;name3=value3");
        assertEquals(3, ds.getConnectionProperties().size());
        assertEquals("value1", ds.getConnectionProperties().getProperty("name1"));
        assertEquals("value2", ds.getConnectionProperties().getProperty("name2"));
        assertEquals("value3", ds.getConnectionProperties().getProperty("name3"));

        // make sure all properties are replaced
        ds.setConnectionProperties("name1=value1;name2=value2");
        assertEquals(2, ds.getConnectionProperties().size());
        assertEquals("value1", ds.getConnectionProperties().getProperty("name1"));
        assertEquals("value2", ds.getConnectionProperties().getProperty("name2"));
        assertFalse(ds.getConnectionProperties().containsKey("name3"));

        // no value is empty string
        ds.setConnectionProperties("name1=value1;name2");
        assertEquals(2, ds.getConnectionProperties().size());
        assertEquals("value1", ds.getConnectionProperties().getProperty("name1"));
        assertEquals("", ds.getConnectionProperties().getProperty("name2"));

        // no value (with equals) is empty string
        ds.setConnectionProperties("name1=value1;name2=");
        assertEquals(2, ds.getConnectionProperties().size());
        assertEquals("value1", ds.getConnectionProperties().getProperty("name1"));
        assertEquals("", ds.getConnectionProperties().getProperty("name2"));

        // single value
        ds.setConnectionProperties("name1=value1");
        assertEquals(1, ds.getConnectionProperties().size());
        assertEquals("value1", ds.getConnectionProperties().getProperty("name1"));

        // single value with trailing ;
        ds.setConnectionProperties("name1=value1;");
        assertEquals(1, ds.getConnectionProperties().size());
        assertEquals("value1", ds.getConnectionProperties().getProperty("name1"));

        // single value wit no value
        ds.setConnectionProperties("name1");
        assertEquals(1, ds.getConnectionProperties().size());
        assertEquals("", ds.getConnectionProperties().getProperty("name1"));

        // null should throw a NullPointerException
        assertThrows(NullPointerException.class, () -> ds.setConnectionProperties(null));
    }

    @Test
    public void testSetValidationTestProperties() {
        // defaults
        assertTrue(ds.getTestOnBorrow());
        assertFalse(ds.getTestOnReturn());
        assertFalse(ds.getTestWhileIdle());

        ds.setTestOnBorrow(true);
        ds.setTestOnReturn(true);
        ds.setTestWhileIdle(true);
        assertTrue(ds.getTestOnBorrow());
        assertTrue(ds.getTestOnReturn());
        assertTrue(ds.getTestWhileIdle());

        ds.setTestOnBorrow(false);
        ds.setTestOnReturn(false);
        ds.setTestWhileIdle(false);
        assertFalse(ds.getTestOnBorrow());
        assertFalse(ds.getTestOnReturn());
        assertFalse(ds.getTestWhileIdle());
    }

    @Test
    public void testStart() throws Exception {
        ds.setAccessToUnderlyingConnectionAllowed(true);
        ds.setMaxTotal(2);
        final DelegatingConnection<?> conn1 = (DelegatingConnection<?>) ds.getConnection();
        final DelegatingConnection<?> conn2 = (DelegatingConnection<?>) ds.getConnection();
        final Connection inner1 = conn1.getInnermostDelegate();
        final Connection inner2 = conn2.getInnermostDelegate();
        assertFalse(inner2.isClosed());
        conn2.close();
        assertFalse(inner2.isClosed());
        // One active, one idle in the pool
        ds.close();
        // Idle connection should be physically closed, checked out unaffected
        assertFalse(conn1.isClosed());
        assertTrue(inner2.isClosed());
        assertEquals(0, ds.getNumIdle());

        // Reopen creates a new pool, so we can have three out
        ds.start();
        final Connection conn3 = ds.getConnection();
        final Connection conn4 = ds.getConnection();
        conn3.close();
        conn4.close();

        // Old pool's orphan should get physically closed on return
        conn1.close();
        assertTrue(inner1.isClosed());
    }

    @Test
    public void testStartInitializes() throws Exception {
        ds.setInitialSize(2);
        // Note: if we ever move away from lazy init, next two will fail
        assertEquals(0, ds.getNumIdle());
        assertNull(ds.getRegisteredJmxName());

        // Start forces init
        ds.start();
        assertEquals(2, ds.getNumIdle());
        assertNotNull(ds.getRegisteredJmxName());
    }

    @Test
    public void testTransactionIsolationBehavior() throws Exception {
        try (final Connection conn = getConnection()) {
            assertNotNull(conn);
            assertEquals(Connection.TRANSACTION_READ_COMMITTED, conn.getTransactionIsolation());
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
        }

        final Connection conn2 = getConnection();
        assertEquals(Connection.TRANSACTION_READ_COMMITTED, conn2.getTransactionIsolation());

        final Connection conn3 = getConnection();
        assertEquals(Connection.TRANSACTION_READ_COMMITTED, conn3.getTransactionIsolation());

        conn2.close();

        conn3.close();
    }

    @Test
    public void testUnwrap() throws Exception {
        assertSame(ds.unwrap(BasicDataSource.class), ds);
        assertSame(ds.unwrap(AutoCloseable.class), ds);
        assertThrows(SQLException.class, () -> ds.unwrap(String.class));
        assertThrows(SQLException.class, () -> ds.unwrap(null));
    }

    @Test
    public void testValidationQueryTimeoutNegative() throws Exception {
        ds.setTestOnBorrow(true);
        ds.setTestOnReturn(true);
        ds.setValidationQueryTimeout(Duration.ofSeconds(-1));
        try (final Connection con = ds.getConnection()) {
            // close right away.
        }
    }

    @Test
    public void testValidationQueryTimeoutSucceed() throws Exception {
        ds.setTestOnBorrow(true);
        ds.setTestOnReturn(true);
        ds.setValidationQueryTimeout(Duration.ofMillis(100)); // Works for TesterStatement
        try (final Connection con = ds.getConnection()) {
            // close right away.
        }
    }

    @Test
    public void testValidationQueryTimeoutZero() throws Exception {
        ds.setTestOnBorrow(true);
        ds.setTestOnReturn(true);
        ds.setValidationQueryTimeout(Duration.ZERO);
        try (final Connection con = ds.getConnection()) {
            // close right away.
        }
    }

    @Test
    public void testValidationQueryTimoutFail() {
        ds.setTestOnBorrow(true);
        ds.setValidationQueryTimeout(Duration.ofSeconds(3)); // Too fast for TesterStatement
        final SQLException e = assertThrows(SQLException.class, ds::getConnection);
        assertTrue(e.toString().contains("timeout"));
    }
}



/**
 * TesterDriver that adds latency to connection requests. Latency (in ms) is the
 * last component of the URL.
 */
class TesterConnectionDelayDriver extends TesterDriver {
    private static final String CONNECT_STRING = "jdbc:apache:commons:testerConnectionDelayDriver";

    public TesterConnectionDelayDriver() {
        // DBCP expects an explicit no-arg constructor
    }

    @Override
    public boolean acceptsURL(final String url) throws SQLException {
        return url.startsWith(CONNECT_STRING);
    }

    @Override
    public Connection connect(final String url, final Properties info) throws SQLException {
        final String[] parsedUrl = url.split(":");
        final int delay = Integer.parseInt(parsedUrl[parsedUrl.length - 1]);
        try {
            Thread.sleep(delay);
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        return super.connect(url, info);
    }

}

/**
 * TesterDriver that keeps a static count of connection requests.
 */
class TesterConnRequestCountDriver extends TesterDriver {
    private static final String CONNECT_STRING = "jdbc:apache:commons:testerConnRequestCountDriver";
    private static final AtomicInteger connectionRequestCount = new AtomicInteger(0);

    public static int getConnectionRequestCount() {
        return connectionRequestCount.get();
    }

    public static void initConnRequestCount() {
        connectionRequestCount.set(0);
    }

    public TesterConnRequestCountDriver() {
        // DBCP expects an explicit no-arg constructor
    }

    @Override
    public boolean acceptsURL(final String url) throws SQLException {
        return CONNECT_STRING.startsWith(url);
    }

    @Override
    public Connection connect(final String url, final Properties info) throws SQLException {
        connectionRequestCount.incrementAndGet();
        return super.connect(url, info);
    }
}
