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

package org.apache.commons.dbcp.datasources;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.commons.dbcp.TestConnectionPool;
import org.apache.commons.dbcp.cpdsadapter.DriverAdapterCPDS;

/**
 * @author John McNally
 * @author Dirk Verbeeck
 * @version $Revision$ $Date$
 */
public class TestSharedPoolDataSource extends TestConnectionPool {
    public TestSharedPoolDataSource(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(TestSharedPoolDataSource.class);
    }

    protected Connection getConnection() throws Exception {
        return ds.getConnection("foo","bar");
    }

    private DriverAdapterCPDS pcds;
    private DataSource ds;

    public void setUp() throws Exception {
        super.setUp();
        pcds = new DriverAdapterCPDS();
        pcds.setDriver("org.apache.commons.dbcp.TesterDriver");
        pcds.setUrl("jdbc:apache:commons:testdriver");
        pcds.setUser("foo");
        pcds.setPassword("bar");
        pcds.setPoolPreparedStatements(false);
        pcds.setAccessToUnderlyingConnectionAllowed(true);

        SharedPoolDataSource tds = new SharedPoolDataSource();
        tds.setConnectionPoolDataSource(pcds);
        tds.setMaxActive(getMaxActive());
        tds.setMaxWait((int)(getMaxWait()));
        tds.setDefaultTransactionIsolation(
            Connection.TRANSACTION_READ_COMMITTED);

        ds = tds;
    }


    public void testBackPointers() throws Exception {
        // todo disabled until a wrapping issuen in SharedPoolDataSource are resolved
    }

    /**
     * Switching 'u1 -> 'u2' and 'p1' -> 'p2' will
     * exhibit the bug detailed in 
     * http://issues.apache.org/bugzilla/show_bug.cgi?id=18905
     * 
     * Starting with a successful connection, then incorrect password,
     * then correct password for same user illustrates
     * JIRA: DBCP-245
     */
    public void testIncorrectPassword() throws Exception 
    {
        ds.getConnection("u2", "p2").close();
        try {
            // Use bad password
            ds.getConnection("u1", "zlsafjk").close();
            fail("Able to retrieve connection with incorrect password");
        } catch (SQLException e1) {
            // should fail

        }
        
        // Use good password
        ds.getConnection("u1", "p1").close();
        try 
        {
            ds.getConnection("u1", "x").close();
            fail("Able to retrieve connection with incorrect password");
        }
        catch (SQLException e)
        {
            Throwable t = e.getCause();
            if (!t.getMessage().startsWith("x is not the correct password"))
            {
                throw e;
            }
            // else the exception was expected
        }
        
        // Make sure we can still use our good password.
        ds.getConnection("u1", "p1").close();
    }


    public void testSimple() throws Exception 
    {
        Connection conn = ds.getConnection();
        assertNotNull(conn);
        PreparedStatement stmt = conn.prepareStatement("select * from dual");
        assertNotNull(stmt);
        ResultSet rset = stmt.executeQuery();
        assertNotNull(rset);
        assertTrue(rset.next());
        rset.close();
        stmt.close();
        conn.close();
    }

    public void testSimpleWithUsername() throws Exception 
    {
        Connection conn = ds.getConnection("u1", "p1");
        assertNotNull(conn);
        PreparedStatement stmt = conn.prepareStatement("select * from dual");
        assertNotNull(stmt);
        ResultSet rset = stmt.executeQuery();
        assertNotNull(rset);
        assertTrue(rset.next());
        rset.close();
        stmt.close();
        conn.close();
    }

    public void testClosingWithUserName() 
        throws Exception 
    {
        Connection[] c = new Connection[getMaxActive()];
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

        for (int i=0; i<c.length; i++) 
        {
            c[i].close();
        }

        // open the maximum connections
        for (int i=0; i<c.length; i++) 
        {
            c[i] = ds.getConnection("u1", "p1");
        }
        for (int i=0; i<c.length; i++) 
        {
            c[i].close();
        }
    }

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
        try 
        {
            conn.createStatement();
            fail("Can't use closed connections");
        } 
        catch(SQLException e) 
        {
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

    public void testOpening() 
        throws Exception 
    {
        Connection[] c = new Connection[getMaxActive()];
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

        for (int i=0; i<c.length; i++) 
        {
            c[i].close();
        }
    }

    public void testClosing() 
        throws Exception 
    {
        Connection[] c = new Connection[getMaxActive()];
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

        for (int i=0; i<c.length; i++) 
        {
            c[i].close();
        }
    }
    
    /**
     * Test pool close.  Illustrates BZ 37359.
     * 
     * @throws Exception
     */
    public void testClosePool() throws Exception {
      ((SharedPoolDataSource)ds).close();
      SharedPoolDataSource tds = new SharedPoolDataSource();
      // NPE before BZ 37359 fix 
      tds.close();
    }

    public void testMaxActive() 
        throws Exception 
    {
        Connection[] c = new Connection[getMaxActive()];
        for (int i=0; i<c.length; i++) 
        {
            c[i] = ds.getConnection();
            assertTrue(c[i] != null);            
        }

        try
        {
            ds.getConnection();
            fail("Allowed to open more than DefaultMaxActive connections.");
        }
        catch(java.sql.SQLException e)
        {
            // should only be able to open 10 connections, so this test should
            // throw an exception
        }

        for (int i=0; i<c.length; i++) 
        {
            c[i].close();
        }
    }
    
    public void testMaxWait() throws Exception {
        final int maxWait = 1000;
        final int theadCount = 20;
        
        ((SharedPoolDataSource)ds).setMaxWait(maxWait);
        // Obtain all the connections from the pool
        Connection[] c = new Connection[getMaxActive()];
        for (int i=0; i<c.length; i++) {
            c[i] = ds.getConnection("foo","bar");
            assertTrue(c[i] != null);            
        }

        long start = System.currentTimeMillis();
        
        // Run a thread test with minimal hold time
        // All threads should end after maxWait - DBCP-291
        final PoolTest[] pts = new PoolTest[theadCount];
        ThreadGroup threadGroup = new ThreadGroup("testMaxWait");

        // Should take ~maxWait for threads to stop 
        for (int i = 0; i < pts.length; i++) {
            (pts[i] = new PoolTest(threadGroup, 1, true)).start();
        }
        
        // Wait for all the threads to complete
        for (int i = 0; i < pts.length; i++) {
            final PoolTest poolTest = pts[i];
            poolTest.getThread().join();
        }

        
        long end = System.currentTimeMillis();
        
        System.out.println("testMaxWait took " + (end-start) + " ms. Maxwait: "+maxWait);
        
        // Threads should time out in parallel - allow double that to be safe
        assertTrue((end-start) < (2 * maxWait)); 

        // Put all the connections back in the pool
        for (int i=0; i<c.length; i++) {
            c[i].close();
        }
    }

    public void testMultipleThreads1() throws Exception {
        // Override wait time in order to allow for Thread.sleep(1) sometimes taking a lot longer on
        // some JVMs, e.g. Windows.
        final int defaultMaxWait = 430;
        ((SharedPoolDataSource) ds).setMaxWait(defaultMaxWait);
        multipleThreads(1, false, true, defaultMaxWait);
    }

    public void testMultipleThreads2() throws Exception {
        multipleThreads(2 * (int)(getMaxWait()), true, true, getMaxWait());
    }

    public void testTransactionIsolationBehavior() throws Exception {
        Connection conn = getConnection();
        assertNotNull(conn);
        assertEquals(Connection.TRANSACTION_READ_COMMITTED, 
                     conn.getTransactionIsolation());
        conn.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
        conn.close();
        
        Connection conn2 = getConnection();
        assertEquals(Connection.TRANSACTION_READ_COMMITTED, 
                     conn2.getTransactionIsolation());
        
        Connection conn3 = getConnection();
        assertEquals(Connection.TRANSACTION_READ_COMMITTED, 
                     conn3.getTransactionIsolation());
        conn2.close();
        conn3.close();
    }     

    // Bugzilla Bug 24136 ClassCastException in DriverAdapterCPDS 
    // when setPoolPreparedStatements(true)
    public void testPoolPrepareStatement() throws Exception 
    {
        pcds.setPoolPreparedStatements(true);

        Connection conn = ds.getConnection();
        assertNotNull(conn);
        PreparedStatement stmt = conn.prepareStatement("select * from dual");
        assertNotNull(stmt);
        ResultSet rset = stmt.executeQuery();
        assertNotNull(rset);
        assertTrue(rset.next());
        rset.close();
        stmt.close();
        conn.close();
    }

    // There are 6 different prepareStatement statement methods so add a little
    // complexity to reduce what would otherwise be lots of copy and paste
    private static abstract class PrepareStatementCallback {
        protected Connection conn;
        void setConnection(Connection conn) {
            this.conn = conn;
        }
        abstract PreparedStatement getPreparedStatement() throws SQLException;
    }
    private static class PscbString extends PrepareStatementCallback {
        PreparedStatement getPreparedStatement() throws SQLException {
            return conn.prepareStatement("select * from dual");
        }
    }
    private static class PscbStringIntInt extends PrepareStatementCallback {
        PreparedStatement getPreparedStatement() throws SQLException {
            return conn.prepareStatement("select * from dual",0,0);
        }
    }
    private static class PscbStringInt extends PrepareStatementCallback {
        PreparedStatement getPreparedStatement() throws SQLException {
            return conn.prepareStatement("select * from dual",0);
        }
    }
    private static class PscbStringIntArray extends PrepareStatementCallback {
        PreparedStatement getPreparedStatement() throws SQLException {
            return conn.prepareStatement("select * from dual", new int[0]);
        }
    }
    private static class PscbStringStringArray extends PrepareStatementCallback {
        PreparedStatement getPreparedStatement() throws SQLException {
            return conn.prepareStatement("select * from dual",new String[0]);
        }
    }
    private static class PscbStringIntIntInt extends PrepareStatementCallback {
        PreparedStatement getPreparedStatement() throws SQLException {
            return conn.prepareStatement("select * from dual",0,0,0);
        }
    }
    private void doTestPoolPreparedStatements(PrepareStatementCallback callBack)
    throws Exception {
        DriverAdapterCPDS mypcds = new DriverAdapterCPDS();
        DataSource myds = null;
        mypcds.setDriver("org.apache.commons.dbcp.TesterDriver");
        mypcds.setUrl("jdbc:apache:commons:testdriver");
        mypcds.setUser("foo");
        mypcds.setPassword("bar");
        mypcds.setPoolPreparedStatements(true);
        mypcds.setMaxPreparedStatements(10);

        SharedPoolDataSource tds = new SharedPoolDataSource();
        tds.setConnectionPoolDataSource(mypcds);
        tds.setMaxActive(getMaxActive());
        tds.setMaxWait((int)(getMaxWait()));
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
        long l1HashCode = stmt.hashCode();
        rset = stmt.executeQuery();
        assertNotNull(rset);
        assertTrue(rset.next());
        rset.close();
        stmt.close();

        stmt = callBack.getPreparedStatement();
        assertNotNull(stmt);
        long l2HashCode = stmt.hashCode();
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
        long l3HashCode = stmt.hashCode();
        rset = stmt.executeQuery();
        assertNotNull(rset);
        assertTrue(rset.next());
        rset.close();
        stmt.close();

        stmt = callBack.getPreparedStatement();
        assertNotNull(stmt);
        long l4HashCode = stmt.hashCode();
        rset = stmt.executeQuery();
        assertNotNull(rset);
        assertTrue(rset.next());
        rset.close();
        stmt.close();

        // prepared statement pooling is working
        assertTrue(l3HashCode == l4HashCode);
        conn.close();
        conn = null;
    }
    public void testPoolPreparedStatements() throws Exception {
        doTestPoolPreparedStatements(new PscbString());
        doTestPoolPreparedStatements(new PscbStringIntInt());
        doTestPoolPreparedStatements(new PscbStringInt());
        doTestPoolPreparedStatements(new PscbStringIntArray());
        doTestPoolPreparedStatements(new PscbStringStringArray());
        doTestPoolPreparedStatements(new PscbStringIntIntInt());
    }
}
