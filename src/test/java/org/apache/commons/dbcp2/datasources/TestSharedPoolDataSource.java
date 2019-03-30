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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.DelegatingStatement;
import org.apache.commons.dbcp2.TestConnectionPool;
import org.apache.commons.dbcp2.TesterDriver;
import org.apache.commons.dbcp2.cpdsadapter.DriverAdapterCPDS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 */
public class TestSharedPoolDataSource extends TestConnectionPool {

    @Override
    protected Connection getConnection() throws Exception {
        return ds.getConnection("foo","bar");
    }

    private DriverAdapterCPDS pcds;
    private DataSource ds;

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
        tds.setDefaultMaxWaitMillis((int)getMaxWaitMillis());
        tds.setDefaultTransactionIsolation(
            Connection.TRANSACTION_READ_COMMITTED);
        tds.setDefaultAutoCommit(Boolean.TRUE);

        ds = tds;
    }


    /**
     * Starting with a successful connection, then incorrect password,
     * then correct password for same user illustrates
     * JIRA: DBCP-245
     */
    @Test
    public void testIncorrectPassword() throws Exception {

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
    public void testSimple() throws Exception
    {
        final Connection conn = ds.getConnection();
        assertNotNull(conn);
        final PreparedStatement stmt = conn.prepareStatement("select * from dual");
        assertNotNull(stmt);
        final ResultSet rset = stmt.executeQuery();
        assertNotNull(rset);
        assertTrue(rset.next());
        rset.close();
        stmt.close();
        conn.close();
    }

    @Test
    public void testSimpleWithUsername() throws Exception
    {
        final Connection conn = ds.getConnection("u1", "p1");
        assertNotNull(conn);
        final PreparedStatement stmt = conn.prepareStatement("select * from dual");
        assertNotNull(stmt);
        final ResultSet rset = stmt.executeQuery();
        assertNotNull(rset);
        assertTrue(rset.next());
        rset.close();
        stmt.close();
        conn.close();
    }

    @Test
    public void testClosingWithUserName()
        throws Exception
    {
        final Connection[] c = new Connection[getMaxTotal()];
        // open the maximum connections
        for (int i=0; i<c.length; i++)
        {
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
        for (int i=0; i<c.length; i++)
        {
            c[i] = ds.getConnection("u1", "p1");
        }
        for (final Connection element : c) {
            element.close();
        }
    }

    @Override
    @Test
    public void testSimple2()
        throws Exception
    {
        Connection conn = ds.getConnection();
        assertNotNull(conn);

        PreparedStatement stmt =
            conn.prepareStatement("select * from dual");
        assertNotNull(stmt);
        ResultSet rset = stmt.executeQuery();
        assertNotNull(rset);
        assertTrue(rset.next());
        rset.close();
        stmt.close();

        stmt = conn.prepareStatement("select * from dual");
        assertNotNull(stmt);
        rset = stmt.executeQuery();
        assertNotNull(rset);
        assertTrue(rset.next());
        rset.close();
        stmt.close();

        conn.close();
        try (Statement s = conn.createStatement()){
            fail("Can't use closed connections");
        } catch(final SQLException e) {
            // expected
        }

        conn = ds.getConnection();
        assertNotNull(conn);

        stmt = conn.prepareStatement("select * from dual");
        assertNotNull(stmt);
        rset = stmt.executeQuery();
        assertNotNull(rset);
        assertTrue(rset.next());
        rset.close();
        stmt.close();

        stmt = conn.prepareStatement("select * from dual");
        assertNotNull(stmt);
        rset = stmt.executeQuery();
        assertNotNull(rset);
        assertTrue(rset.next());
        rset.close();
        stmt.close();

        conn.close();
        conn = null;
    }

    @Override
    @Test
    public void testOpening()
        throws Exception
    {
        final Connection[] c = new Connection[getMaxTotal()];
        // test that opening new connections is not closing previous
        for (int i=0; i<c.length; i++)
        {
            c[i] = ds.getConnection();
            assertTrue(c[i] != null);
            for (int j=0; j<=i; j++)
            {
                assertTrue(!c[j].isClosed());
            }
        }

        for (final Connection element : c) {
            element.close();
        }
    }

    @Override
    @Test
    public void testClosing()
        throws Exception
    {
        final Connection[] c = new Connection[getMaxTotal()];
        // open the maximum connections
        for (int i=0; i<c.length; i++)
        {
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

    /**
     * Test pool close.  Illustrates BZ 37359.
     *
     * @throws Exception
     */
    @Test
    public void testClosePool() throws Exception {
      ((SharedPoolDataSource)ds).close();
      final SharedPoolDataSource tds = new SharedPoolDataSource();
      // NPE before BZ 37359 fix
      tds.close();
    }

    @Override
    @Test
    public void testMaxTotal() throws Exception {
        final Connection[] c = new Connection[getMaxTotal()];
        for (int i=0; i<c.length; i++) {
            c[i] = ds.getConnection();
            assertTrue(c[i] != null);
        }

        try (Connection conn = ds.getConnection()){
            fail("Allowed to open more than DefaultMaxTotal connections.");
        } catch(final java.sql.SQLException e) {
            // should only be able to open 10 connections, so this test should
            // throw an exception
        }

        for (final Connection element : c) {
            element.close();
        }
    }

    @Test
    public void testMaxWaitMillis() throws Exception {
        final int maxWaitMillis = 1000;
        final int theadCount = 20;

        ((SharedPoolDataSource)ds).setDefaultMaxWaitMillis(maxWaitMillis);
        // Obtain all the connections from the pool
        final Connection[] c = new Connection[getMaxTotal()];
        for (int i=0; i<c.length; i++) {
            c[i] = ds.getConnection("foo","bar");
            assertTrue(c[i] != null);
        }

        final long start = System.currentTimeMillis();

        // Run a thread test with minimal hold time
        // All threads should end after maxWaitMillis - DBCP-291
        final PoolTest[] pts = new PoolTest[theadCount];
        final ThreadGroup threadGroup = new ThreadGroup("testMaxWaitMillis");

        // Should take ~maxWaitMillis for threads to stop
        for (int i = 0; i < pts.length; i++) {
            (pts[i] = new PoolTest(threadGroup, 1, true)).start();
        }

        // Wait for all the threads to complete
        for (final PoolTest poolTest : pts) {
            poolTest.getThread().join();
        }


        final long end = System.currentTimeMillis();

        // System.out.println("testMaxWaitMillis took " + (end - start) + " ms. maxWaitMillis: " + maxWaitMillis);

        // Threads should time out in parallel - allow double that to be safe
        assertTrue(end-start < 2 * maxWaitMillis);

        // Put all the connections back in the pool
        for (final Connection element : c) {
            element.close();
        }
    }

    @Test
    public void testMultipleThreads1() throws Exception {
        // Override wait time in order to allow for Thread.sleep(1) sometimes taking a lot longer on
        // some JVMs, e.g. Windows.
        final int defaultMaxWaitMillis = 430;
        ((SharedPoolDataSource) ds).setDefaultMaxWaitMillis(defaultMaxWaitMillis);
        multipleThreads(1, false, false, defaultMaxWaitMillis);
    }

    @Test
    public void testMultipleThreads2() throws Exception {
        final int defaultMaxWaitMillis = 500;
        ((SharedPoolDataSource) ds).setDefaultMaxWaitMillis(defaultMaxWaitMillis);
        multipleThreads(2 * defaultMaxWaitMillis, true, true, defaultMaxWaitMillis);
    }

    @Test
    public void testTransactionIsolationBehavior() throws Exception {
        final Connection conn = getConnection();
        assertNotNull(conn);
        assertEquals(Connection.TRANSACTION_READ_COMMITTED,
                     conn.getTransactionIsolation());
        conn.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
        conn.close();

        final Connection conn2 = getConnection();
        assertEquals(Connection.TRANSACTION_READ_COMMITTED,
                     conn2.getTransactionIsolation());

        final Connection conn3 = getConnection();
        assertEquals(Connection.TRANSACTION_READ_COMMITTED,
                     conn3.getTransactionIsolation());
        conn2.close();
        conn3.close();
    }

    // Bugzilla Bug 24136 ClassCastException in DriverAdapterCPDS
    // when setPoolPreparedStatements(true)
    @Test
    public void testPoolPrepareCall() throws Exception {
        pcds.setPoolPreparedStatements(true);

        final Connection conn = ds.getConnection();
        assertNotNull(conn);
        final PreparedStatement stmt = conn.prepareCall("{call home()}");
        assertNotNull(stmt);
        final ResultSet rset = stmt.executeQuery();
        assertNotNull(rset);
        assertTrue(rset.next());
        rset.close();
        stmt.close();
        conn.close();
    }

    @Test
    public void testPoolPrepareStatement() throws Exception {
        pcds.setPoolPreparedStatements(true);

        final Connection conn = ds.getConnection();
        assertNotNull(conn);
        final PreparedStatement stmt = conn.prepareStatement("select * from dual");
        assertNotNull(stmt);
        final ResultSet rset = stmt.executeQuery();
        assertNotNull(rset);
        assertTrue(rset.next());
        rset.close();
        stmt.close();
        conn.close();
    }

    // There are 3 different prepareCall statement methods so add a little
    // complexity to reduce what would otherwise be lots of copy and paste
    private static abstract class PrepareCallCallback {
        protected Connection conn;
        void setConnection(final Connection conn) {
            this.conn = conn;
        }
        abstract CallableStatement getCallableStatement() throws SQLException;
    }

    private static class CscbString extends PrepareCallCallback {
        @Override
        CallableStatement getCallableStatement() throws SQLException {
            return conn.prepareCall("{call home()}");
        }
    }

    private static class CscbStringIntInt extends PrepareCallCallback {
        @Override
        CallableStatement getCallableStatement() throws SQLException {
            return conn.prepareCall("{call home()}", 0, 0);
        }
    }

    private static class CscbStringIntIntInt extends PrepareCallCallback {
        @Override
        CallableStatement getCallableStatement() throws SQLException {
            return conn.prepareCall("{call home()}", 0, 0, 0);
        }
    }

    // There are 6 different prepareStatement statement methods so add a little
    // complexity to reduce what would otherwise be lots of copy and paste
    private static abstract class PrepareStatementCallback {
        protected Connection conn;
        void setConnection(final Connection conn) {
            this.conn = conn;
        }
        abstract PreparedStatement getPreparedStatement() throws SQLException;
    }

    private static class PscbString extends PrepareStatementCallback {
        @Override
        PreparedStatement getPreparedStatement() throws SQLException {
            return conn.prepareStatement("select * from dual");
        }
    }

    private static class PscbStringIntInt extends PrepareStatementCallback {
        @Override
        PreparedStatement getPreparedStatement() throws SQLException {
            return conn.prepareStatement("select * from dual", 0, 0);
        }
    }

    private static class PscbStringInt extends PrepareStatementCallback {
        @Override
        PreparedStatement getPreparedStatement() throws SQLException {
            return conn.prepareStatement("select * from dual", 0);
        }
    }

    private static class PscbStringIntArray extends PrepareStatementCallback {
        @Override
        PreparedStatement getPreparedStatement() throws SQLException {
            return conn.prepareStatement("select * from dual", new int[0]);
        }
    }

    private static class PscbStringStringArray extends PrepareStatementCallback {
        @Override
        PreparedStatement getPreparedStatement() throws SQLException {
            return conn.prepareStatement("select * from dual", new String[0]);
        }
    }

    private static class PscbStringIntIntInt extends PrepareStatementCallback {
        @Override
        PreparedStatement getPreparedStatement() throws SQLException {
            return conn.prepareStatement("select * from dual", 0, 0, 0);
        }
    }

    private void doTestPoolCallableStatements(final PrepareCallCallback callBack)
    throws Exception {
        final DriverAdapterCPDS myPcds = new DriverAdapterCPDS();
        DataSource myDs = null;
        myPcds.setDriver("org.apache.commons.dbcp2.TesterDriver");
        myPcds.setUrl("jdbc:apache:commons:testdriver");
        myPcds.setUser("foo");
        myPcds.setPassword("bar");
        myPcds.setPoolPreparedStatements(true);
        myPcds.setMaxPreparedStatements(10);

        final SharedPoolDataSource spDs = new SharedPoolDataSource();
        spDs.setConnectionPoolDataSource(myPcds);
        spDs.setMaxTotal(getMaxTotal());
        spDs.setDefaultMaxWaitMillis((int) getMaxWaitMillis());
        spDs.setDefaultTransactionIsolation(
            Connection.TRANSACTION_READ_COMMITTED);

        myDs = spDs;

        Connection conn = ds.getConnection();
        callBack.setConnection(conn);
        CallableStatement stmt = null;
        ResultSet rset = null;

        assertNotNull(conn);

        stmt = callBack.getCallableStatement();
        assertNotNull(stmt);
        final long l1HashCode = ((DelegatingStatement) stmt).getDelegate().hashCode();
        rset = stmt.executeQuery();
        assertNotNull(rset);
        assertTrue(rset.next());
        rset.close();
        stmt.close();

        stmt = callBack.getCallableStatement();
        assertNotNull(stmt);
        final long l2HashCode = ((DelegatingStatement) stmt).getDelegate().hashCode();
        rset = stmt.executeQuery();
        assertNotNull(rset);
        assertTrue(rset.next());
        rset.close();
        stmt.close();

        // statement pooling is not enabled, we should get different statements
        assertTrue(l1HashCode != l2HashCode);
        conn.close();
        conn = null;

        conn = myDs.getConnection();
        callBack.setConnection(conn);

        stmt = callBack.getCallableStatement();
        assertNotNull(stmt);
        final long l3HashCode = ((DelegatingStatement) stmt).getDelegate().hashCode();
        rset = stmt.executeQuery();
        assertNotNull(rset);
        assertTrue(rset.next());
        rset.close();
        stmt.close();

        stmt = callBack.getCallableStatement();
        assertNotNull(stmt);
        final long l4HashCode = ((DelegatingStatement) stmt).getDelegate().hashCode();
        rset = stmt.executeQuery();
        assertNotNull(rset);
        assertTrue(rset.next());
        rset.close();
        stmt.close();

        // prepared statement pooling is working
        assertTrue(l3HashCode == l4HashCode);
        conn.close();
        conn = null;
        spDs.close();
    }

    private void doTestPoolPreparedStatements(final PrepareStatementCallback callBack)
    throws Exception {
        final DriverAdapterCPDS mypcds = new DriverAdapterCPDS();
        DataSource myds = null;
        mypcds.setDriver("org.apache.commons.dbcp2.TesterDriver");
        mypcds.setUrl("jdbc:apache:commons:testdriver");
        mypcds.setUser("foo");
        mypcds.setPassword("bar");
        mypcds.setPoolPreparedStatements(true);
        mypcds.setMaxPreparedStatements(10);

        final SharedPoolDataSource tds = new SharedPoolDataSource();
        tds.setConnectionPoolDataSource(mypcds);
        tds.setMaxTotal(getMaxTotal());
        tds.setDefaultMaxWaitMillis((int)getMaxWaitMillis());
        tds.setDefaultTransactionIsolation(
            Connection.TRANSACTION_READ_COMMITTED);

        myds = tds;

        Connection conn = ds.getConnection();
        callBack.setConnection(conn);
        PreparedStatement stmt = null;
        ResultSet rset = null;

        assertNotNull(conn);

        stmt = callBack.getPreparedStatement();
        assertNotNull(stmt);
        final long l1HashCode = ((DelegatingStatement) stmt).getDelegate().hashCode();
        rset = stmt.executeQuery();
        assertNotNull(rset);
        assertTrue(rset.next());
        rset.close();
        stmt.close();

        stmt = callBack.getPreparedStatement();
        assertNotNull(stmt);
        final long l2HashCode = ((DelegatingStatement) stmt).getDelegate().hashCode();
        rset = stmt.executeQuery();
        assertNotNull(rset);
        assertTrue(rset.next());
        rset.close();
        stmt.close();

        // statement pooling is not enabled, we should get different statements
        assertTrue(l1HashCode != l2HashCode);
        conn.close();
        conn = null;

        conn = myds.getConnection();
        callBack.setConnection(conn);

        stmt = callBack.getPreparedStatement();
        assertNotNull(stmt);
        final long l3HashCode = ((DelegatingStatement) stmt).getDelegate().hashCode();
        rset = stmt.executeQuery();
        assertNotNull(rset);
        assertTrue(rset.next());
        rset.close();
        stmt.close();

        stmt = callBack.getPreparedStatement();
        assertNotNull(stmt);
        final long l4HashCode = ((DelegatingStatement) stmt).getDelegate().hashCode();
        rset = stmt.executeQuery();
        assertNotNull(rset);
        assertTrue(rset.next());
        rset.close();
        stmt.close();

        // prepared statement pooling is working
        assertTrue(l3HashCode == l4HashCode);
        conn.close();
        conn = null;
        tds.close();
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

    // See DBCP-8
    @Test
    public void testChangePassword() throws Exception {
        try (Connection c = ds.getConnection("foo", "bay")){
            fail("Should have generated SQLException");
        } catch (final SQLException expected) {
        }
        final Connection con1 = ds.getConnection("foo", "bar");
        final Connection con2 = ds.getConnection("foo", "bar");
        final Connection con3 = ds.getConnection("foo", "bar");
        con1.close();
        con2.close();
        TesterDriver.addUser("foo","bay"); // change the user/password setting
        try (Connection con4 = ds.getConnection("foo", "bay")) { // new password
            // Idle instances with old password should have been cleared
            assertEquals(0, ((SharedPoolDataSource) ds).getNumIdle(),
                    "Should be no idle connections in the pool");
            con4.close();
            // Should be one idle instance with new pwd
            assertEquals(1, ((SharedPoolDataSource) ds).getNumIdle(),
                    "Should be one idle connection in the pool");
            try (Connection con4b = ds.getConnection("foo", "bar")) { // old password
                fail("Should have generated SQLException");
            } catch (final SQLException expected) {
            }
            final Connection con5 = ds.getConnection("foo", "bay"); // take the idle one
            con3.close(); // Return a connection with the old password
            ds.getConnection("foo", "bay").close();  // will try bad returned connection and destroy it
            assertEquals(1, ((SharedPoolDataSource) ds).getNumIdle(),
                    "Should be one idle connection in the pool");
            con5.close();
        } finally {
            TesterDriver.addUser("foo","bar");
        }
    }

    @Test
    public void testDbcp369() {
        final ArrayList<SharedPoolDataSource> dataSources = new ArrayList<>();
        for (int j = 0; j < 10000; j++) {
            final SharedPoolDataSource dataSource = new SharedPoolDataSource();
            dataSources.add(dataSource);
        }

        final Thread t1 = new Thread(new Runnable() {
            @Override
            public void run() {
                for (final SharedPoolDataSource dataSource : dataSources) {
                    dataSource.setDataSourceName("a");
                }
            }
        });

        final Thread t2 = new Thread(new Runnable() {
            @Override
            public void run() {
                for (final SharedPoolDataSource dataSource : dataSources) {
                    try {
                        dataSource.close();
                    } catch (final Exception e) {
                        // Ignore
                    }
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
}
