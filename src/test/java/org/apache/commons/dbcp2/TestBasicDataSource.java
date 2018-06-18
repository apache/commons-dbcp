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

import static org.junit.Assert.*;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.sql.DataSource;

import org.apache.commons.logging.LogFactory;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * TestSuite for BasicDataSource
 */
public class TestBasicDataSource extends TestConnectionPool {

    @Override
    protected Connection getConnection() throws Exception {
        return ds.getConnection();
    }

    protected BasicDataSource ds = null;
    private static final String CATALOG = "test catalog";

    @BeforeClass
    public static void setUpClass() {
        // register a custom logger which supports inspection of the log messages
        LogFactory.getFactory().setAttribute("org.apache.commons.logging.Log", "org.apache.commons.dbcp2.StackMessageLog");
    }

    @Before
    public void setUp() throws Exception {
        ds = createDataSource();
        ds.setDriverClassName("org.apache.commons.dbcp2.TesterDriver");
        ds.setUrl("jdbc:apache:commons:testdriver");
        ds.setMaxTotal(getMaxTotal());
        ds.setMaxWaitMillis(getMaxWaitMillis());
        ds.setDefaultAutoCommit(Boolean.TRUE);
        ds.setDefaultReadOnly(Boolean.FALSE);
        ds.setDefaultTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        ds.setDefaultCatalog(CATALOG);
        ds.setUsername("userName");
        ds.setPassword("password");
        ds.setValidationQuery("SELECT DUMMY FROM DUAL");
        ds.setConnectionInitSqls(Arrays.asList(new String[] { "SELECT 1", "SELECT 2"}));
        ds.setDriverClassLoader(new TesterClassLoader());
        ds.setJmxName("org.apache.commons.dbcp2:name=test");
    }

    protected BasicDataSource createDataSource() throws Exception {
        return new BasicDataSource();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        ds.close();
        ds = null;
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
        try {
            getConnection();
            fail("Expecting SQLException");
        } catch (final SQLException ex) {
            // Expected
        }

        // Redundant close is OK
        ds.close();

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
        try {
            ds.setConnectionProperties(null);
            fail("Expected NullPointerException");
        } catch (final NullPointerException e) {
            // expected
        }
    }

    @Test
    public void testTransactionIsolationBehavior() throws Exception {
        final Connection conn = getConnection();
        assertNotNull(conn);
        assertEquals(Connection.TRANSACTION_READ_COMMITTED, conn.getTransactionIsolation());
        conn.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
        conn.close();

        final Connection conn2 = getConnection();
        assertEquals(Connection.TRANSACTION_READ_COMMITTED, conn2.getTransactionIsolation());

        final Connection conn3 = getConnection();
        assertEquals(Connection.TRANSACTION_READ_COMMITTED, conn3.getTransactionIsolation());

        conn2.close();

        conn3.close();
    }

    @Override
    @Test
    public void testPooling() throws Exception {
        // this also needs access to the underlying connection
        ds.setAccessToUnderlyingConnectionAllowed(true);
        super.testPooling();
    }

    @Test
    public void testNoAccessToUnderlyingConnectionAllowed() throws Exception {
        // default: false
        assertFalse(ds.isAccessToUnderlyingConnectionAllowed());

        final Connection conn = getConnection();
        Connection dconn = ((DelegatingConnection<?>) conn).getDelegate();
        assertNull(dconn);

        dconn = ((DelegatingConnection<?>) conn).getInnermostDelegate();
        assertNull(dconn);
    }

    @Test
    public void testAccessToUnderlyingConnectionAllowed() throws Exception {
        ds.setAccessToUnderlyingConnectionAllowed(true);
        assertTrue(ds.isAccessToUnderlyingConnectionAllowed());

        final Connection conn = getConnection();
        Connection dconn = ((DelegatingConnection<?>) conn).getDelegate();
        assertNotNull(dconn);

        dconn = ((DelegatingConnection<?>) conn).getInnermostDelegate();
        assertNotNull(dconn);

        assertTrue(dconn instanceof TesterConnection);
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
    public void testInvalidValidationQuery() {
        ds.setValidationQuery("invalid");
        try (Connection c = ds.getConnection()) {
            fail("expected SQLException");
        } catch (final SQLException e) {
            if (e.toString().indexOf("invalid") < 0) {
                fail("expected detailed error message");
            }
        }
    }

    @Test
    public void testValidationQueryTimoutFail() {
        ds.setTestOnBorrow(true);
        ds.setValidationQueryTimeout(3); // Too fast for TesterStatement
        try (Connection c = ds.getConnection()) {
            fail("expected SQLException");
        } catch (final SQLException ex) {
            if (ex.toString().indexOf("timeout") < 0) {
                fail("expected timeout error message");
            }
        }
    }

    @Test
    public void testValidationQueryTimeoutZero() throws Exception {
        ds.setTestOnBorrow(true);
        ds.setTestOnReturn(true);
        ds.setValidationQueryTimeout(0);
        final Connection con = ds.getConnection();
        con.close();
    }

    @Test
    public void testValidationQueryTimeoutNegative() throws Exception {
        ds.setTestOnBorrow(true);
        ds.setTestOnReturn(true);
        ds.setValidationQueryTimeout(-1);
        final Connection con = ds.getConnection();
        con.close();
    }

    @Test
    public void testValidationQueryTimeoutSucceed() throws Exception {
        ds.setTestOnBorrow(true);
        ds.setTestOnReturn(true);
        ds.setValidationQueryTimeout(100); // Works for TesterStatement
        final Connection con = ds.getConnection();
        con.close();
    }

    @Test
    public void testEmptyInitConnectionSql() throws Exception {
        ds.setConnectionInitSqls(Arrays.asList(new String[]{"", "   "}));
        assertNotNull(ds.getConnectionInitSqls());
        assertEquals(0, ds.getConnectionInitSqls().size());

        ds.setConnectionInitSqls(null);
        assertNotNull(ds.getConnectionInitSqls());
        assertEquals(0, ds.getConnectionInitSqls().size());
    }

    @Test
    public void testInvalidConnectionInitSql() {
        try {
            ds.setConnectionInitSqls(Arrays.asList(new String[]{"SELECT 1","invalid"}));
            try (Connection c = ds.getConnection()) {}
            fail("expected SQLException");
        }
        catch (final SQLException e) {
            if (e.toString().indexOf("invalid") < 0) {
                fail("expected detailed error message");
            }
        }
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
    public void testDefaultCatalog() throws Exception {
        final Connection[] c = new Connection[getMaxTotal()];
        for (int i = 0; i < c.length; i++) {
            c[i] = getConnection();
            assertTrue(c[i] != null);
            assertEquals(CATALOG, c[i].getCatalog());
        }

        for (final Connection element : c) {
            element.setCatalog("error");
            element.close();
        }

        for (int i = 0; i < c.length; i++) {
            c[i] = getConnection();
            assertTrue(c[i] != null);
            assertEquals(CATALOG, c[i].getCatalog());
        }

        for (final Connection element : c) {
            element.close();
        }
    }

    @Test
    public void testSetAutoCommitTrueOnClose() throws Exception {
        ds.setAccessToUnderlyingConnectionAllowed(true);
        ds.setDefaultAutoCommit(Boolean.FALSE);

        final Connection conn = getConnection();
        assertNotNull(conn);
        assertFalse(conn.getAutoCommit());

        final Connection dconn = ((DelegatingConnection<?>) conn).getInnermostDelegate();
        assertNotNull(dconn);
        assertFalse(dconn.getAutoCommit());

        conn.close();

        assertTrue(dconn.getAutoCommit());
    }

    @Test
    public void testInitialSize() throws Exception {
        ds.setMaxTotal(20);
        ds.setMaxIdle(20);
        ds.setInitialSize(10);

        final Connection conn = getConnection();
        assertNotNull(conn);
        conn.close();

        assertEquals(0, ds.getNumActive());
        assertEquals(10, ds.getNumIdle());
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
        final TesterConnection tconn = (TesterConnection) ((DelegatingConnection<?>)conn).getInnermostDelegate();
        tconn.setFailure(new IOException("network error"));

        try {
            conn.close();
            fail("Expected SQLException");
        }
        catch(final SQLException ex) { }

        assertEquals(0, ds.getNumActive());
    }

    /**
     * Verifies correct handling of exceptions generated by the underlying pool as it closes
     * connections in response to BDS#close. Exceptions have to be either swallowed by the
     * underlying pool and logged, or propagated and wrapped.
     */
    @Test
    public void testPoolCloseCheckedException() throws Exception {
        ds.setAccessToUnderlyingConnectionAllowed(true);  // Allow dirty tricks

        // Get an idle connection into the pool
        final Connection conn = ds.getConnection();
        final TesterConnection tc = (TesterConnection) ((DelegatingConnection<?>) conn).getInnermostDelegate();
        conn.close();

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
            Assert.assertNotNull(message);
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
        final Connection conn = ds.getConnection();
        final TesterConnection tc = (TesterConnection) ((DelegatingConnection<?>) conn).getInnermostDelegate();
        conn.close();
        tc.setFailure(new IllegalStateException("boom"));
        try {
            StackMessageLog.lock();
            StackMessageLog.clear();
            ds.close();
            final String message = StackMessageLog.popMessage();
            Assert.assertNotNull(message);
            assertTrue(message.indexOf("boom") > 0);
        } catch (final IllegalStateException ex) {
            assertTrue(ex.getMessage().indexOf("boom") > 0); // RTE is not wrapped by BDS#close
        } finally {
            StackMessageLog.unLock();
        }
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

        final Connection conn = ds.getConnection();
        assertNotNull(conn);

        assertFalse(ds.getConnectionPool().getTestOnBorrow());
        assertFalse(ds.getConnectionPool().getTestWhileIdle());
        assertTrue(ds.getConnectionPool().getTestOnReturn());
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

        final Connection conn = ds.getConnection();
        assertNotNull(conn);
        conn.close();
    }

    /**
     * Bugzilla Bug 29832: Broken behaviour for BasicDataSource.setMaxTotal(0)
     * MaxTotal == 0 should throw SQLException on getConnection.
     * Results from Bug 29863 in commons-pool.
     */
    @Test
    public void testMaxTotalZero() throws Exception {
        ds.setMaxTotal(0);

        try {
            final Connection conn = ds.getConnection();
            assertNotNull(conn);
            fail("SQLException expected");

        } catch (final SQLException e) {
            // test OK
        }
    }

    @Test
    public void testInvalidateConnection() throws Exception {
    	ds.setMaxTotal(2);
    	final Connection conn1 = ds.getConnection();
    	final Connection conn2 = ds.getConnection();
    	ds.invalidateConnection(conn1);
    	assertTrue(conn1.isClosed());
    	assertEquals(1, ds.getNumActive());
    	assertEquals(0, ds.getNumIdle());
    	final Connection conn3 = ds.getConnection();
    	conn2.close();
    	conn3.close();
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
        ds.setMaxWaitMillis(getMaxWaitMillis());
        ds.setDefaultAutoCommit(Boolean.TRUE);
        ds.setDefaultReadOnly(Boolean.FALSE);
        ds.setDefaultTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        ds.setDefaultCatalog(CATALOG);
        ds.setUsername("userName");
        // Set timeBetweenEvictionRuns > 0, so evictor is created
        ds.setTimeBetweenEvictionRunsMillis(100);
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

    /**
     * JIRA DBCP-333: Check that a custom class loader is used.
     * @throws Exception
     */
    @Test
    public void testDriverClassLoader() throws Exception {
        getConnection();
        final ClassLoader cl = ds.getDriverClassLoader();
        assertNotNull(cl);
        assertTrue(cl instanceof TesterClassLoader);
        assertTrue(((TesterClassLoader) cl).didLoad(ds.getDriverClassName()));
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
        ds.setTimeBetweenEvictionRunsMillis(100);
        // Set min idle > 0, so evictor will try to make connection as many as idle count
        ds.setMinIdle(2);

        // Prevent concurrent execution of threads executing test subclasses
        synchronized (TesterConnRequestCountDriver.class) {
            TesterConnRequestCountDriver.initConnRequestCount();

            // user request 10 times
            for (int i=0; i<10; i++) {
                try {
                    @SuppressWarnings("unused")
                    final
                    DataSource ds2 = ds.createDataSource();
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

    @Test
    public void testMaxConnLifetimeExceeded() throws Exception {
        try {
            StackMessageLog.lock();
            ds.setMaxConnLifetimeMillis(100);
            final Connection conn = ds.getConnection();
            assertEquals(1, ds.getNumActive());
            Thread.sleep(500);
            conn.close();
            assertEquals(0, ds.getNumIdle());
            final String message = StackMessageLog.popMessage();
            Assert.assertNotNull(message);
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
            ds.setMaxConnLifetimeMillis(100);
            ds.setLogExpiredConnections(false);
            try (final Connection conn = ds.getConnection()) {
                assertEquals(1, ds.getNumActive());
                Thread.sleep(500);
            }
            assertEquals(0, ds.getNumIdle());
            assertTrue(StackMessageLog.getAll().toString(), StackMessageLog.isEmpty());
        } finally {
            StackMessageLog.clear();
            StackMessageLog.unLock();
        }
    }

    @Test
    public void testConcurrentInitBorrow() throws Exception {
        ds.setDriverClassName("org.apache.commons.dbcp2.TesterConnectionDelayDriver");
        ds.setUrl("jdbc:apache:commons:testerConnectionDelayDriver:50");
        ds.setInitialSize(8);

        // Launch a request to trigger pool initialization
        final TestThread testThread = new TestThread(1,0);
        final Thread t = new Thread(testThread);
        t.start();

        // Get another connection (should wait for pool init)
        Thread.sleep(100); // Make sure t gets into init first
        ds.getConnection();

        // Pool should have at least 6 idle connections now
        // Use underlying pool getNumIdle to avoid waiting for ds lock
        assertTrue(ds.getConnectionPool().getNumIdle() > 5);

        // Make sure t completes successfully
        t.join();
        assertFalse(testThread.failed());

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
        ds.setMaxWaitMillis(-1);

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
        ds.setJmxName(null);  // Should disable JMX for both connection and statement pools
        ds.setPoolPreparedStatements(true);
        ds.getConnection();  // Trigger initialization
        // Nothing should be registered
        assertEquals(0, mbs.queryNames(commons, null).size());
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
        ds.getConnection();  // Triggers initialization - pcf creation
        // Make sure factory got the properties
        final PoolableConnectionFactory pcf =
                (PoolableConnectionFactory) ds.getConnectionPool().getFactory();
        assertTrue(pcf.isFastFailValidation());
        assertTrue(pcf.getDisconnectionSqlCodes().contains("XXX"));
        assertEquals(1, pcf.getDisconnectionSqlCodes().size());
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
        final BasicDataSource ds = BasicDataSourceFactory.createDataSource(properties);
        final boolean original = ds.getConnectionPool().getLogAbandoned();
        ds.setLogAbandoned(!original);
        Assert.assertNotEquals(Boolean.valueOf(original),
                Boolean.valueOf(ds.getConnectionPool().getLogAbandoned()));
    }
}

/**
 * TesterDriver that keeps a static count of connection requests.
 */
class TesterConnRequestCountDriver extends TesterDriver {
    private static final String CONNECT_STRING = "jdbc:apache:commons:testerConnRequestCountDriver";
    private static AtomicInteger connectionRequestCount = new AtomicInteger(0);

    public TesterConnRequestCountDriver() {
        // DBCP expects an explicit no-arg constructor
    }

    @Override
    public Connection connect(final String url, final Properties info) throws SQLException {
        connectionRequestCount.incrementAndGet();
        return super.connect(url, info);
    }

    @Override
    public boolean acceptsURL(final String url) throws SQLException {
        return CONNECT_STRING.startsWith(url);
    }

    public static int getConnectionRequestCount() {
        return connectionRequestCount.get();
    }

    public static void initConnRequestCount() {
        connectionRequestCount.set(0);
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
    public Connection connect(final String url, final Properties info) throws SQLException {
        final String[] parsedUrl = url.split(":");
        final int delay = Integer.parseInt(parsedUrl[parsedUrl.length - 1]);
        try {
            Thread.sleep(delay);
        } catch(final InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        return super.connect(url, info);
    }

    @Override
    public boolean acceptsURL(final String url) throws SQLException {
        return url.startsWith(CONNECT_STRING);
    }

}
