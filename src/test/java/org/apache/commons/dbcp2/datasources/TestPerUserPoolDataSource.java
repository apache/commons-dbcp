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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.commons.dbcp2.TestConnectionPool;
import org.apache.commons.dbcp2.TesterDriver;
import org.apache.commons.dbcp2.cpdsadapter.DriverAdapterCPDS;

/**
 * @author John McNally
 * @author Dirk Verbeeck
 * @version $Revision$ $Date$
 */
public class TestPerUserPoolDataSource extends TestConnectionPool {
    public TestPerUserPoolDataSource(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(TestPerUserPoolDataSource.class);
    }

    @Override
    protected Connection getConnection() throws SQLException {
        return ds.getConnection("foo","bar");
    }

    private DataSource ds;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        DriverAdapterCPDS pcds = new DriverAdapterCPDS();
        pcds.setDriver("org.apache.commons.dbcp2.TesterDriver");
        pcds.setUrl("jdbc:apache:commons:testdriver");
        pcds.setUser("foo");
        pcds.setPassword("bar");
        pcds.setAccessToUnderlyingConnectionAllowed(true);

        PerUserPoolDataSource tds = new PerUserPoolDataSource();
        tds.setConnectionPoolDataSource(pcds);
        tds.setDefaultMaxTotal(getMaxTotal());
        tds.setDefaultMaxWaitMillis((int)getMaxWaitMillis());
        tds.setPerUserMaxTotal("foo", Integer.valueOf(getMaxTotal()));
        tds.setPerUserMaxWaitMillis("foo", Long.valueOf(getMaxWaitMillis()));
        tds.setDefaultTransactionIsolation(
            Connection.TRANSACTION_READ_COMMITTED);
        tds.setDefaultAutoCommit(Boolean.TRUE);
        ds = tds;
    }


    /**
     * Switching 'u1 -> 'u2' and 'p1' -> 'p2' will
     * exhibit the bug detailed in
     * http://issues.apache.org/bugzilla/show_bug.cgi?id=18905
     */
    public void testIncorrectPassword() throws Exception {
        // Use bad password
        try (Connection c = ds.getConnection("u1", "zlsafjk");){
            fail("Able to retrieve connection with incorrect password");
        } catch (SQLException e1) {
            // should fail

        }

        // Use good password
        ds.getConnection("u1", "p1").close();
        try (Connection c = ds.getConnection("u1", "x")){
            fail("Able to retrieve connection with incorrect password");
        } catch (SQLException e) {
            if (!e.getMessage().startsWith("Given password did not match")) {
                throw e;
            }
            // else the exception was expected
        }

        // Make sure we can still use our good password.
        ds.getConnection("u1", "p1").close();

        // Try related users and passwords
        ds.getConnection("foo", "bar").close();
        try (Connection c = ds.getConnection("foob", "ar")) {
            fail("Should have caused an SQLException");
        } catch (SQLException expected) {
        }
        try (Connection c = ds.getConnection("foo", "baz")){
            fail("Should have generated SQLException");
        } catch (SQLException expected) {
        }
    }


    @Override
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
        Connection[] c = new Connection[getMaxTotal()];
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

        for (Connection element : c) {
            element.close();
        }

        // open the maximum connections
        for (int i=0; i<c.length; i++)
        {
            c[i] = ds.getConnection("u1", "p1");
        }
        for (Connection element : c) {
            element.close();
        }
    }

    @Override
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
        } catch(SQLException e) {
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
    public void testOpening()
        throws Exception
    {
        Connection[] c = new Connection[getMaxTotal()];
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

        for (Connection element : c) {
            element.close();
        }
    }

    @Override
    public void testClosing()
        throws Exception
    {
        Connection[] c = new Connection[getMaxTotal()];
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

        for (Connection element : c) {
            element.close();
        }
    }

    @Override
    public void testMaxTotal() throws Exception {
        Connection[] c = new Connection[getMaxTotal()];
        for (int i=0; i<c.length; i++) {
            c[i] = ds.getConnection();
            assertTrue(c[i] != null);
        }

        try (Connection conn = ds.getConnection()){
            fail("Allowed to open more than DefaultMaxTotal connections.");
        } catch(java.sql.SQLException e) {
            // should only be able to open 10 connections, so this test should
            // throw an exception
        }

        for (Connection element : c) {
            element.close();
        }
    }

    /**
     * Verify that defaultMaxWaitMillis = 0 means immediate failure when
     * pool is exhausted.
     */
    public void testMaxWaitMillisZero() throws Exception {
        PerUserPoolDataSource tds = (PerUserPoolDataSource) ds;
        tds.setDefaultMaxWaitMillis(0);
        tds.setPerUserMaxTotal("u1", Integer.valueOf(1));
        Connection conn = tds.getConnection("u1", "p1");
        try (Connection c2 = tds.getConnection("u1", "p1")){
            fail("Expecting Pool Exhausted exception");
        } catch (SQLException ex) {
            // expected
        }
        conn.close();
    }

    public void testPerUserMethods() throws Exception {
        PerUserPoolDataSource tds = (PerUserPoolDataSource) ds;

        // you need to set per user maxTotal otherwise there is no accounting
        tds.setPerUserMaxTotal("u1", Integer.valueOf(5));
        tds.setPerUserMaxTotal("u2", Integer.valueOf(5));

        assertEquals(0, tds.getNumActive());
        assertEquals(0, tds.getNumActive("u1"));
        assertEquals(0, tds.getNumActive("u2"));
        assertEquals(0, tds.getNumIdle());
        assertEquals(0, tds.getNumIdle("u1"));
        assertEquals(0, tds.getNumIdle("u2"));

        Connection conn = tds.getConnection();
        assertNotNull(conn);
        assertEquals(1, tds.getNumActive());
        assertEquals(0, tds.getNumActive("u1"));
        assertEquals(0, tds.getNumActive("u2"));
        assertEquals(0, tds.getNumIdle());
        assertEquals(0, tds.getNumIdle("u1"));
        assertEquals(0, tds.getNumIdle("u2"));

        conn.close();
        assertEquals(0, tds.getNumActive());
        assertEquals(0, tds.getNumActive("u1"));
        assertEquals(0, tds.getNumActive("u2"));
        assertEquals(1, tds.getNumIdle());
        assertEquals(0, tds.getNumIdle("u1"));
        assertEquals(0, tds.getNumIdle("u2"));

        conn = tds.getConnection("u1", "p1");
        assertNotNull(conn);
        assertEquals(0, tds.getNumActive());
        assertEquals(1, tds.getNumActive("u1"));
        assertEquals(0, tds.getNumActive("u2"));
        assertEquals(1, tds.getNumIdle());
        assertEquals(0, tds.getNumIdle("u1"));
        assertEquals(0, tds.getNumIdle("u2"));

        conn.close();
        assertEquals(0, tds.getNumActive());
        assertEquals(0, tds.getNumActive("u1"));
        assertEquals(0, tds.getNumActive("u2"));
        assertEquals(1, tds.getNumIdle());
        assertEquals(1, tds.getNumIdle("u1"));
        assertEquals(0, tds.getNumIdle("u2"));
    }

    public void testMultipleThreads1() throws Exception {
        // Override wait time in order to allow for Thread.sleep(1) sometimes taking a lot longer on
        // some JVMs, e.g. Windows.
        final int defaultMaxWaitMillis = 430;
        ((PerUserPoolDataSource) ds).setDefaultMaxWaitMillis(defaultMaxWaitMillis);
        ((PerUserPoolDataSource) ds).setPerUserMaxWaitMillis("foo",new Long(defaultMaxWaitMillis));
        multipleThreads(1, false, false, defaultMaxWaitMillis);
    }

    public void testMultipleThreads2() throws Exception {
        final int defaultMaxWaitMillis = 500;
        ((PerUserPoolDataSource) ds).setDefaultMaxWaitMillis(defaultMaxWaitMillis);
        ((PerUserPoolDataSource) ds).setPerUserMaxWaitMillis("foo",new Long(defaultMaxWaitMillis));
        multipleThreads(2 * defaultMaxWaitMillis, true, true, defaultMaxWaitMillis);
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

    public void testSerialization() throws Exception {
        // make sure the pool has initialized
        Connection conn = ds.getConnection();
        conn.close();

        // serialize
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(baos);
        out.writeObject(ds);
        byte[] b = baos.toByteArray();
        out.close();

        ByteArrayInputStream bais = new ByteArrayInputStream(b);
        ObjectInputStream in = new ObjectInputStream(bais);
        Object obj = in.readObject();
        in.close();

        assertEquals( 1, ((PerUserPoolDataSource)obj).getNumIdle() );
    }

    // see issue http://issues.apache.org/bugzilla/show_bug.cgi?id=23843
    // unregistered user is in the same pool as without username
    public void testUnregisteredUser() throws Exception {
        PerUserPoolDataSource tds = (PerUserPoolDataSource) ds;

        assertEquals(0, tds.getNumActive());
        assertEquals(0, tds.getNumIdle());

        Connection conn = tds.getConnection();
        assertNotNull(conn);
        assertEquals(1, tds.getNumActive());
        assertEquals(0, tds.getNumIdle());

        conn.close();
        assertEquals(0, tds.getNumActive());
        assertEquals(1, tds.getNumIdle());

        conn = tds.getConnection("u1", "p1");
        assertNotNull(conn);
        assertEquals(0, tds.getNumActive());
        assertEquals(1, tds.getNumIdle());
        assertEquals(1, tds.getNumActive("u1"));
        assertEquals(0, tds.getNumIdle("u1"));

        conn.close();
        assertEquals(0, tds.getNumActive());
        assertEquals(1, tds.getNumIdle());
        assertEquals(0, tds.getNumActive("u1"));
        assertEquals(1, tds.getNumIdle("u1"));
    }

    // see issue http://issues.apache.org/bugzilla/show_bug.cgi?id=23843
    public void testDefaultUser1() throws Exception {
        TesterDriver.addUser("mkh", "password");
        TesterDriver.addUser("hanafey", "password");
        TesterDriver.addUser("jsmith", "password");

        PerUserPoolDataSource puds = (PerUserPoolDataSource) ds;
        puds.setPerUserMaxTotal("jsmith", Integer.valueOf(2));
        String[] users = {"mkh", "hanafey", "jsmith"};
        String password = "password";
        Connection[] c = new Connection[users.length];
        for (int i = 0; i < users.length; i++) {
            c[i] = puds.getConnection(users[i], password);
            assertEquals(users[i], getUsername(c[i]));
        }
        for (int i = 0; i < users.length; i++) {
            c[i].close();
        }
    }

    // see issue http://issues.apache.org/bugzilla/show_bug.cgi?id=23843
    public void testDefaultUser2() throws Exception {
        TesterDriver.addUser("mkh", "password");
        TesterDriver.addUser("hanafey", "password");
        TesterDriver.addUser("jsmith", "password");

        PerUserPoolDataSource puds = (PerUserPoolDataSource) ds;
        puds.setPerUserMaxTotal("jsmith", Integer.valueOf(2));
        String[] users = {"jsmith", "hanafey", "mkh"};
        String password = "password";
        Connection[] c = new Connection[users.length];
        for (int i = 0; i < users.length; i++) {
            c[i] = puds.getConnection(users[i], password);
            assertEquals(users[i], getUsername(c[i]));
        }
        for (int i = 0; i < users.length; i++) {
            c[i].close();
        }
    }

    // See DBCP-8
    public void testChangePassword() throws Exception {
        try (Connection c = ds.getConnection("foo", "bay")){
            fail("Should have generated SQLException");
        } catch (SQLException expected) {
        }
        Connection con1 = ds.getConnection("foo", "bar");
        Connection con2 = ds.getConnection("foo", "bar");
        Connection con3 = ds.getConnection("foo", "bar");
        con1.close();
        con2.close();
        TesterDriver.addUser("foo","bay"); // change the user/password setting
        try {
            Connection con4 = ds.getConnection("foo", "bay"); // new password
            // Idle instances with old password should have been cleared
            assertEquals("Should be no idle connections in the pool",
                    0, ((PerUserPoolDataSource) ds).getNumIdle("foo"));
            con4.close();
            // Should be one idle instance with new pwd
            assertEquals("Should be one idle connection in the pool",
                    1, ((PerUserPoolDataSource) ds).getNumIdle("foo"));
            try (Connection c = ds.getConnection("foo", "bar")) { // old password
                fail("Should have generated SQLException");
            } catch (SQLException expected) {
            }
            Connection con5 = ds.getConnection("foo", "bay"); // take the idle one
            con3.close(); // Return a connection with the old password
            ds.getConnection("foo", "bay").close();  // will try bad returned connection and destroy it
            assertEquals("Should be one idle connection in the pool",
                    1, ((PerUserPoolDataSource) ds).getNumIdle("foo"));
            con5.close();
        } finally {
            TesterDriver.addUser("foo","bar");
        }
    }
}
