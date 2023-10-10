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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.DelegatingStatement;
import org.apache.commons.dbcp2.TestConnectionPool;
import org.apache.commons.dbcp2.TesterDriver;
import org.apache.commons.dbcp2.cpdsadapter.DriverAdapterCPDS;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 */
public class TestSharedPoolDataSource extends TestConnectionPool {

    /**
     * There are 3 different prepareCall statement methods so add a little complexity to reduce what would otherwise be lots
     * of copy and paste.
     */
    private static abstract class AbstractPrepareCallCallback {
        protected Connection conn;

        abstract CallableStatement getCallableStatement() throws SQLException;

        void setConnection(final Connection conn) {
            this.conn = conn;
        }
    }

    /**
     * There are 6 different prepareStatement statement methods so add a little complexity to reduce what would otherwise be
     * lots of copy and paste.
     */
    private static abstract class AbstractPrepareStatementCallback {
        protected Connection conn;

        abstract PreparedStatement prepareStatement() throws SQLException;

        void setConnection(final Connection conn) {
            this.conn = conn;
        }
    }

    private static final class CscbString extends AbstractPrepareCallCallback {
        @Override
        CallableStatement getCallableStatement() throws SQLException {
            return conn.prepareCall("{call home()}");
        }
    }

    private static final class CscbStringIntInt extends AbstractPrepareCallCallback {
        @Override
        CallableStatement getCallableStatement() throws SQLException {
            return conn.prepareCall("{call home()}", 0, 0);
        }
    }

    private static final class CscbStringIntIntInt extends AbstractPrepareCallCallback {
        @Override
        CallableStatement getCallableStatement() throws SQLException {
            return conn.prepareCall("{call home()}", 0, 0, 0);
        }
    }

    private static final class PscbString extends AbstractPrepareStatementCallback {
        @Override
        PreparedStatement prepareStatement() throws SQLException {
            return conn.prepareStatement("select * from dual");
        }
    }

    private static final class PscbStringInt extends AbstractPrepareStatementCallback {
        @Override
        PreparedStatement prepareStatement() throws SQLException {
            return conn.prepareStatement("select * from dual", 0);
        }
    }

    private static final class PscbStringIntArray extends AbstractPrepareStatementCallback {
        @Override
        PreparedStatement prepareStatement() throws SQLException {
            return conn.prepareStatement("select * from dual", ArrayUtils.EMPTY_INT_ARRAY);
        }
    }

    private static final class PscbStringIntInt extends AbstractPrepareStatementCallback {
        @Override
        PreparedStatement prepareStatement() throws SQLException {
            return conn.prepareStatement("select * from dual", 0, 0);
        }
    }

    private static final class PscbStringIntIntInt extends AbstractPrepareStatementCallback {
        @Override
        PreparedStatement prepareStatement() throws SQLException {
            return conn.prepareStatement("select * from dual", 0, 0, 0);
        }
    }

    private static final class PscbStringStringArray extends AbstractPrepareStatementCallback {
        @Override
        PreparedStatement prepareStatement() throws SQLException {
            return conn.prepareStatement("select * from dual", ArrayUtils.EMPTY_STRING_ARRAY);
        }
    }

    private DriverAdapterCPDS pcds;

    private DataSource ds;

    private void doTestPoolCallableStatements(final AbstractPrepareCallCallback callBack)
        throws Exception {
        final DriverAdapterCPDS myPcds = new DriverAdapterCPDS();
        myPcds.setDriver("org.apache.commons.dbcp2.TesterDriver");
        myPcds.setUrl("jdbc:apache:commons:testdriver");
        myPcds.setUser("foo");
        myPcds.setPassword("bar");
        myPcds.setPoolPreparedStatements(true);
        myPcds.setMaxPreparedStatements(10);

        try (final SharedPoolDataSource spDs = new SharedPoolDataSource()) {
            spDs.setConnectionPoolDataSource(myPcds);
            spDs.setMaxTotal(getMaxTotal());
            spDs.setDefaultMaxWait(getMaxWaitDuration());
            spDs.setDefaultTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

            @SuppressWarnings("resource")
            final DataSource myDs = spDs;

            try (Connection conn = ds.getConnection()) {
                callBack.setConnection(conn);

                assertNotNull(conn);
                final long l1HashCode;
                final long l2HashCode;
                try (CallableStatement stmt = callBack.getCallableStatement()) {
                    assertNotNull(stmt);
                    l1HashCode = getDelegateHashCode(stmt);
                    try (ResultSet rset = stmt.executeQuery()) {
                        assertNotNull(rset);
                        assertTrue(rset.next());
                    }
                }

                try (CallableStatement stmt = callBack.getCallableStatement()) {
                    assertNotNull(stmt);
                    l2HashCode = getDelegateHashCode(stmt);
                    try (ResultSet rset = stmt.executeQuery()) {
                        assertNotNull(rset);
                        assertTrue(rset.next());
                    }
                }

                // statement pooling is not enabled, we should get different statements
                assertTrue(l1HashCode != l2HashCode);
            }

            try (Connection conn = myDs.getConnection()) {
                callBack.setConnection(conn);

                final long l3HashCode;
                final long l4HashCode;
                try (CallableStatement stmt = callBack.getCallableStatement()) {
                    assertNotNull(stmt);
                    l3HashCode = getDelegateHashCode(stmt);
                    try (ResultSet rset = stmt.executeQuery()) {
                        assertNotNull(rset);
                        assertTrue(rset.next());
                    }
                }

                try (CallableStatement stmt = callBack.getCallableStatement()) {
                    assertNotNull(stmt);
                    l4HashCode = getDelegateHashCode(stmt);
                    try (ResultSet rset = stmt.executeQuery()) {
                        assertNotNull(rset);
                        assertTrue(rset.next());
                    }
                }

                // prepared statement pooling is working
                assertEquals(l3HashCode, l4HashCode);
            }
        }
    }

    private void doTestPoolPreparedStatements(final AbstractPrepareStatementCallback psCallBack) throws Exception {
        final DriverAdapterCPDS mypcds = new DriverAdapterCPDS();
        mypcds.setDriver("org.apache.commons.dbcp2.TesterDriver");
        mypcds.setUrl("jdbc:apache:commons:testdriver");
        mypcds.setUser("foo");
        mypcds.setPassword("bar");
        mypcds.setPoolPreparedStatements(true);
        mypcds.setMaxPreparedStatements(10);

        try (final SharedPoolDataSource tds = new SharedPoolDataSource()) {
            tds.setConnectionPoolDataSource(mypcds);
            tds.setMaxTotal(getMaxTotal());
            tds.setDefaultMaxWait(getMaxWaitDuration());
            tds.setDefaultTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

            @SuppressWarnings("resource")
            final
            DataSource myDs = tds;

            try (Connection conn = ds.getConnection()) {
                final long l1HashCode;
                final long l2HashCode;
                assertNotNull(conn);
                psCallBack.setConnection(conn);
                try (PreparedStatement stmt = psCallBack.prepareStatement()) {
                    assertNotNull(stmt);
                    l1HashCode = getDelegateHashCode(stmt);
                    try (ResultSet resultSet = stmt.executeQuery()) {
                        assertNotNull(resultSet);
                        assertTrue(resultSet.next());
                    }
                }

                try (PreparedStatement stmt = psCallBack.prepareStatement()) {
                    assertNotNull(stmt);
                    l2HashCode = getDelegateHashCode(stmt);
                    try (ResultSet resultSet = stmt.executeQuery()) {
                        assertNotNull(resultSet);
                        assertTrue(resultSet.next());
                    }
                }

                // statement pooling is not enabled, we should get different statements
                assertTrue(l1HashCode != l2HashCode);
            }

            try (Connection conn = myDs.getConnection()) {
                final long l3HashCode;
                final long l4HashCode;

                assertNotNull(conn);
                psCallBack.setConnection(conn);
                try (PreparedStatement stmt = psCallBack.prepareStatement()) {
                    assertNotNull(stmt);
                    l3HashCode = getDelegateHashCode(stmt);
                    try (ResultSet resultSet = stmt.executeQuery()) {
                        assertNotNull(resultSet);
                        assertTrue(resultSet.next());
                    }
                }

                try (PreparedStatement stmt = psCallBack.prepareStatement()) {
                    assertNotNull(stmt);
                    l4HashCode = getDelegateHashCode(stmt);
                    try (ResultSet resultSet = stmt.executeQuery()) {
                        assertNotNull(resultSet);
                        assertTrue(resultSet.next());
                    }
                }

                // prepared statement pooling is working
                assertEquals(l3HashCode, l4HashCode);
            }
        }
    }

    @Override
    protected Connection getConnection() throws Exception {
        return ds.getConnection("foo","bar");
    }

    @SuppressWarnings("resource")
    private int getDelegateHashCode(final Statement stmt) {
        return ((DelegatingStatement) stmt).getDelegate().hashCode();
    }

    @BeforeEach
    public void setUp() throws Exception {
        pcds = new DriverAdapterCPDS();
        pcds.setDriver("org.apache.commons.dbcp2.TesterDriver");
        pcds.setUrl("jdbc:apache:commons:testdriver");
        pcds.setUser("foo");
        pcds.setPassword("bar");
        pcds.setPoolPreparedStatements(false);
        pcds.setAccessToUnderlyingConnectionAllowed(true);

        final SharedPoolDataSource tds = new SharedPoolDataSource();
        tds.setConnectionPoolDataSource(pcds);
        tds.setMaxTotal(getMaxTotal());
        tds.setDefaultMaxWait(getMaxWaitDuration());
        tds.setDefaultTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        tds.setDefaultAutoCommit(Boolean.TRUE);

        ds = tds;
    }

    // See DBCP-8
    @Test
    public void testChangePassword() throws Exception {
        assertThrows(SQLException.class, () -> ds.getConnection("foo", "bay"));
        final Connection con1 = ds.getConnection("foo", "bar");
        final Connection con2 = ds.getConnection("foo", "bar");
        final Connection con3 = ds.getConnection("foo", "bar");
        con1.close();
        con2.close();
        TesterDriver.addUser("foo", "bay"); // change the user/password setting
        try (Connection con4 = ds.getConnection("foo", "bay")) { // new password
            // Idle instances with old password should have been cleared
            assertEquals(0, ((SharedPoolDataSource) ds).getNumIdle(), "Should be no idle connections in the pool");
            con4.close();
            // Should be one idle instance with new pwd
            assertEquals(1, ((SharedPoolDataSource) ds).getNumIdle(), "Should be one idle connection in the pool");
            assertThrows(SQLException.class, () -> ds.getConnection("foo", "bar")); // old password
            try (final Connection con5 = ds.getConnection("foo", "bay")) { // take the idle one
                con3.close(); // Return a connection with the old password
                ds.getConnection("foo", "bay").close(); // will try bad returned connection and destroy it
                assertEquals(1, ((SharedPoolDataSource) ds).getNumIdle(), "Should be one idle connection in the pool");
            }
        } finally {
            TesterDriver.addUser("foo", "bar");
        }
    }

    /**
     * Tests pool close. Illustrates BZ 37359.
     *
     * @throws Exception
     */
    @Test
    public void testClosePool() throws Exception {
        ((SharedPoolDataSource) ds).close();
        @SuppressWarnings("resource") // closed below
        final SharedPoolDataSource tds = new SharedPoolDataSource();
        // NPE before BZ 37359 fix
        tds.close();
    }

    @Override
    @Test
    public void testClosing() throws Exception {
        final Connection[] c = new Connection[getMaxTotal()];
        // open the maximum connections
        for (int i = 0; i < c.length; i++) {
            c[i] = ds.getConnection();
        }

        // close one of the connections
        c[0].close();
        assertTrue(c[0].isClosed());

        // get a new connection
        c[0] = ds.getConnection();

        for (final Connection element : c) {
            element.close();
        }
    }

    @Test
    public void testClosingWithUserName() throws Exception {
        final Connection[] c = new Connection[getMaxTotal()];
        // open the maximum connections
        for (int i = 0; i < c.length; i++) {
            c[i] = ds.getConnection("u1", "p1");
        }

        // close one of the connections
        c[0].close();
        assertTrue(c[0].isClosed());
        // get a new connection
        c[0] = ds.getConnection("u1", "p1");

        for (final Connection element : c) {
            element.close();
        }

        // open the maximum connections
        for (int i = 0; i < c.length; i++) {
            c[i] = ds.getConnection("u1", "p1");
        }
        for (final Connection element : c) {
            element.close();
        }
    }

    @Test
    public void testDbcp369() {
        final ArrayList<SharedPoolDataSource> dataSources = new ArrayList<>();
        for (int j = 0; j < 10000; j++) {
            dataSources.add(new SharedPoolDataSource());
        }

        final Thread t1 = new Thread(() -> {
            for (final SharedPoolDataSource dataSource : dataSources) {
                dataSource.setDataSourceName("a");
            }
        });

        final Thread t2 = new Thread(() -> {
            for (final SharedPoolDataSource dataSource : dataSources) {
                try {
                    dataSource.close();
                } catch (final Exception e) {
                    // Ignore
                }
            }
        });

        t1.start();
        t2.start();

        try {
            t1.join();
            t2.join();
        } catch (final InterruptedException ie) {
            // Ignore
        }
    }

    /**
     * Starting with a successful connection, then incorrect password,
     * then correct password for same user illustrates
     * JIRA: DBCP-245
     */
    @Test
    public void testIncorrectPassword() throws SQLException {
        ds.getConnection("u2", "p2").close();
        try (Connection c = ds.getConnection("u1", "zlsafjk")){ // Use bad password
            fail("Able to retrieve connection with incorrect password");
        } catch (final SQLException e1) {
            // should fail
        }

        // Use good password
        ds.getConnection("u1", "p1").close();
        try (Connection c = ds.getConnection("u1", "x")) {
            fail("Able to retrieve connection with incorrect password");
        } catch (final SQLException e) {
            if (!e.getMessage().startsWith("Given password did not match")) {
                throw e;
            }
            // else the exception was expected
        }

        // Make sure we can still use our good password.
        ds.getConnection("u1", "p1").close();

        // Try related users and passwords
        ds.getConnection("foo", "bar").close();
        try (Connection c = ds.getConnection("u1", "ar")) {
            fail("Should have caused an SQLException");
        } catch (final SQLException expected) {
        }
        try (Connection c = ds.getConnection("u1", "baz")) {
            fail("Should have generated SQLException");
        } catch (final SQLException expected) {
        }
    }

    @Override
    @Test
    public void testMaxTotal() throws Exception {
        final Connection[] c = new Connection[getMaxTotal()];
        for (int i=0; i<c.length; i++) {
            c[i] = ds.getConnection();
            assertNotNull(c[i]);
        }

        assertThrows(SQLException.class, ds::getConnection, "Allowed to open more than DefaultMaxTotal connections.");

        for (final Connection element : c) {
            element.close();
        }
    }

    @Test
    public void testMaxWaitMillis() throws Exception {
        final int maxWaitMillis = 1000;
        final int theadCount = 20;

        ((SharedPoolDataSource) ds).setDefaultMaxWait(Duration.ofMillis(maxWaitMillis));
        // Obtain all the connections from the pool
        final Connection[] c = new Connection[getMaxTotal()];
        for (int i = 0; i < c.length; i++) {
            c[i] = ds.getConnection("foo", "bar");
            assertNotNull(c[i]);
        }

        final long startMillis = System.currentTimeMillis();

        // Run a thread test with minimal hold time
        // All threads should end after maxWaitMillis - DBCP-291
        final PoolTest[] pts = new PoolTest[theadCount];
        final ThreadGroup threadGroup = new ThreadGroup("testMaxWaitMillis");

        // Should take ~maxWaitMillis for threads to stop
        for (int i = 0; i < pts.length; i++) {
            (pts[i] = new PoolTest(threadGroup, Duration.ofMillis(1), true)).start();
        }

        // Wait for all the threads to complete
        for (final PoolTest poolTest : pts) {
            poolTest.getThread().join();
        }

        final long endMillis = System.currentTimeMillis();

        // System.out.println("testMaxWaitMillis took " + (end - start) + " ms. maxWaitMillis: " + maxWaitMillis);

        // Threads should time out in parallel - allow double that to be safe
        assertTrue(endMillis - startMillis < 2 * maxWaitMillis);

        // Put all the connections back in the pool
        for (final Connection element : c) {
            element.close();
        }
    }

    @Test
    public void testMultipleThreads1() throws Exception {
        // Override wait time in order to allow for Thread.sleep(1) sometimes taking a lot longer on
        // some JVMs, e.g. Windows.
        final Duration defaultMaxWaitDuration = Duration.ofMillis(430);
        ((SharedPoolDataSource) ds).setDefaultMaxWait(defaultMaxWaitDuration);
        multipleThreads(Duration.ofMillis(1), false, false, defaultMaxWaitDuration);
    }

    @Test
    public void testMultipleThreads2() throws Exception {
        final Duration defaultMaxWaitDuration = Duration.ofMillis(500);
        ((SharedPoolDataSource) ds).setDefaultMaxWait(defaultMaxWaitDuration);
        multipleThreads(defaultMaxWaitDuration.multipliedBy(2), true, true, defaultMaxWaitDuration);
    }

    @Override
    @Test
    public void testOpening() throws Exception {
        final Connection[] c = new Connection[getMaxTotal()];
        // test that opening new connections is not closing previous
        for (int i = 0; i < c.length; i++) {
            c[i] = ds.getConnection();
            assertNotNull(c[i]);
            for (int j = 0; j <= i; j++) {
                assertFalse(c[j].isClosed());
            }
        }

        for (final Connection element : c) {
            element.close();
        }
    }

    /**
     * Bugzilla Bug 24136 ClassCastException in DriverAdapterCPDS when setPoolPreparedStatements(true)
     */
    @Test
    public void testPoolPrepareCall() throws SQLException {
        pcds.setPoolPreparedStatements(true);
        try (final Connection conn = ds.getConnection()) {
            assertNotNull(conn);
            try (final PreparedStatement stmt = conn.prepareCall("{call home()}")) {
                assertNotNull(stmt);
                try (final ResultSet rset = stmt.executeQuery()) {
                    assertNotNull(rset);
                    assertTrue(rset.next());
                }
            }
        }
    }

    @Test
    public void testPoolPreparedCalls() throws Exception {
        doTestPoolCallableStatements(new CscbString());
        doTestPoolCallableStatements(new CscbStringIntInt());
        doTestPoolCallableStatements(new CscbStringIntIntInt());
    }

    @Test
    public void testPoolPreparedStatements() throws Exception {
        doTestPoolPreparedStatements(new PscbString());
        doTestPoolPreparedStatements(new PscbStringIntInt());
        doTestPoolPreparedStatements(new PscbStringInt());
        doTestPoolPreparedStatements(new PscbStringIntArray());
        doTestPoolPreparedStatements(new PscbStringStringArray());
        doTestPoolPreparedStatements(new PscbStringIntIntInt());
    }

    @Test
    public void testPoolPrepareStatement() throws SQLException {
        pcds.setPoolPreparedStatements(true);

        try (final Connection conn = ds.getConnection()) {
            assertNotNull(conn);
            try (final PreparedStatement stmt = conn.prepareStatement("select * from dual")) {
                assertNotNull(stmt);
                try (final ResultSet rset = stmt.executeQuery()) {
                    assertNotNull(rset);
                    assertTrue(rset.next());
                }
            }
        }
    }

    @Override
    @Test
    public void testSimple() throws Exception {
        try (final Connection conn = ds.getConnection()) {
            assertNotNull(conn);
            try (final PreparedStatement stmt = conn.prepareStatement("select * from dual")) {
                assertNotNull(stmt);
                try (final ResultSet rset = stmt.executeQuery()) {
                    assertNotNull(rset);
                    assertTrue(rset.next());
                }
            }
        }
    }

    @Override
    @Test
    public void testSimple2() throws SQLException {
        {
            final Connection conn = ds.getConnection();
            assertNotNull(conn);

            try (PreparedStatement stmt = conn.prepareStatement("select * from dual")) {
                assertNotNull(stmt);
                try (ResultSet rset = stmt.executeQuery()) {
                    assertNotNull(rset);
                    assertTrue(rset.next());
                }
            }

            try (PreparedStatement stmt = conn.prepareStatement("select * from dual")) {
                assertNotNull(stmt);
                try (ResultSet rset = stmt.executeQuery()) {
                    assertNotNull(rset);
                    assertTrue(rset.next());
                }
            }

            conn.close();
            assertThrows(SQLException.class, () -> conn.createStatement(), "Can't use closed connections");
        }
        try (Connection conn = ds.getConnection()) {
            assertNotNull(conn);

            try (PreparedStatement stmt = conn.prepareStatement("select * from dual")) {
                assertNotNull(stmt);
                try (ResultSet rset = stmt.executeQuery()) {
                    assertNotNull(rset);
                    assertTrue(rset.next());
                }
            }

            try (PreparedStatement stmt = conn.prepareStatement("select * from dual")) {
                assertNotNull(stmt);
                try (ResultSet rset = stmt.executeQuery()) {
                    assertNotNull(rset);
                    assertTrue(rset.next());
                }
            }

        }
    }

    @Test
    public void testSimpleWithUsername() throws Exception {
        try (final Connection conn = ds.getConnection("u1", "p1")) {
            assertNotNull(conn);
            try (final PreparedStatement stmt = conn.prepareStatement("select * from dual")) {
                assertNotNull(stmt);
                try (final ResultSet rset = stmt.executeQuery()) {
                    assertNotNull(rset);
                    assertTrue(rset.next());
                }
            }
        }
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
}
