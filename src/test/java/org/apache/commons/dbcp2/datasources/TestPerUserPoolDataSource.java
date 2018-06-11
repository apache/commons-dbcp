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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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

import org.apache.commons.dbcp2.TestConnectionPool;
import org.apache.commons.dbcp2.TesterDriver;
import org.apache.commons.dbcp2.cpdsadapter.DriverAdapterCPDS;
import org.junit.Before;
import org.junit.Test;

/**
 */
public class TestPerUserPoolDataSource extends TestConnectionPool {

    @Override
    protected Connection getConnection() throws SQLException {
        return ds.getConnection("foo","bar");
    }

    private DataSource ds;

    @Before
    public void setUp() throws Exception {
        final DriverAdapterCPDS pcds = new DriverAdapterCPDS();
        pcds.setDriver("org.apache.commons.dbcp2.TesterDriver");
        pcds.setUrl("jdbc:apache:commons:testdriver");
        pcds.setUser("foo");
        pcds.setPassword("bar");
        pcds.setAccessToUnderlyingConnectionAllowed(true);

        final PerUserPoolDataSource tds = new PerUserPoolDataSource();
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
     * Switching 'u1 to 'u2' and 'p1' to 'p2' will
     * exhibit the bug detailed in
     * http://issues.apache.org/bugzilla/show_bug.cgi?id=18905
     */
    @Test
    public void testIncorrectPassword() throws Exception {
        // Use bad password
        try (Connection c = ds.getConnection("u1", "zlsafjk");){
            fail("Able to retrieve connection with incorrect password");
        } catch (final SQLException e1) {
            // should fail

        }

        // Use good password
        ds.getConnection("u1", "p1").close();
        try (Connection c = ds.getConnection("u1", "x")){
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
        try (Connection c = ds.getConnection("foob", "ar")) {
            fail("Should have caused an SQLException");
        } catch (final SQLException expected) {
        }
        try (Connection c = ds.getConnection("foo", "baz")){
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

    /**
     * Verify that defaultMaxWaitMillis = 0 means immediate failure when
     * pool is exhausted.
     */
    @Test
    public void testMaxWaitMillisZero() throws Exception {
        final PerUserPoolDataSource tds = (PerUserPoolDataSource) ds;
        tds.setDefaultMaxWaitMillis(0);
        tds.setPerUserMaxTotal("u1", Integer.valueOf(1));
        final Connection conn = tds.getConnection("u1", "p1");
        try (Connection c2 = tds.getConnection("u1", "p1")){
            fail("Expecting Pool Exhausted exception");
        } catch (final SQLException ex) {
            // expected
        }
        conn.close();
    }

    @Test
    public void testPerUserMethods() throws Exception {
        final PerUserPoolDataSource tds = (PerUserPoolDataSource) ds;

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

    @Test
    public void testMultipleThreads1() throws Exception {
        // Override wait time in order to allow for Thread.sleep(1) sometimes taking a lot longer on
        // some JVMs, e.g. Windows.
        final int defaultMaxWaitMillis = 430;
        ((PerUserPoolDataSource) ds).setDefaultMaxWaitMillis(defaultMaxWaitMillis);
        ((PerUserPoolDataSource) ds).setPerUserMaxWaitMillis("foo",new Long(defaultMaxWaitMillis));
        multipleThreads(1, false, false, defaultMaxWaitMillis);
    }

    @Test
    public void testMultipleThreads2() throws Exception {
        final int defaultMaxWaitMillis = 500;
        ((PerUserPoolDataSource) ds).setDefaultMaxWaitMillis(defaultMaxWaitMillis);
        ((PerUserPoolDataSource) ds).setPerUserMaxWaitMillis("foo",new Long(defaultMaxWaitMillis));
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

    @Test
    public void testSerialization() throws Exception {
        // make sure the pool has initialized
        final Connection conn = ds.getConnection();
        conn.close();

        // serialize
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final ObjectOutputStream out = new ObjectOutputStream(baos);
        out.writeObject(ds);
        final byte[] b = baos.toByteArray();
        out.close();

        final ByteArrayInputStream bais = new ByteArrayInputStream(b);
        final ObjectInputStream in = new ObjectInputStream(bais);
        final Object obj = in.readObject();
        in.close();

        assertEquals( 1, ((PerUserPoolDataSource)obj).getNumIdle() );
    }

    // see issue http://issues.apache.org/bugzilla/show_bug.cgi?id=23843
    // unregistered user is in the same pool as without user name
    @Test
    public void testUnregisteredUser() throws Exception {
        final PerUserPoolDataSource tds = (PerUserPoolDataSource) ds;

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
    @Test
    public void testDefaultUser1() throws Exception {
        TesterDriver.addUser("mkh", "password");
        TesterDriver.addUser("hanafey", "password");
        TesterDriver.addUser("jsmith", "password");

        final PerUserPoolDataSource puds = (PerUserPoolDataSource) ds;
        puds.setPerUserMaxTotal("jsmith", Integer.valueOf(2));
        final String[] users = {"mkh", "hanafey", "jsmith"};
        final String password = "password";
        final Connection[] c = new Connection[users.length];
        for (int i = 0; i < users.length; i++) {
            c[i] = puds.getConnection(users[i], password);
            assertEquals(users[i], getUsername(c[i]));
        }
        for (int i = 0; i < users.length; i++) {
            c[i].close();
        }
    }

    // see issue http://issues.apache.org/bugzilla/show_bug.cgi?id=23843
    @Test
    public void testDefaultUser2() throws Exception {
        TesterDriver.addUser("mkh", "password");
        TesterDriver.addUser("hanafey", "password");
        TesterDriver.addUser("jsmith", "password");

        final PerUserPoolDataSource puds = (PerUserPoolDataSource) ds;
        puds.setPerUserMaxTotal("jsmith", Integer.valueOf(2));
        final String[] users = {"jsmith", "hanafey", "mkh"};
        final String password = "password";
        final Connection[] c = new Connection[users.length];
        for (int i = 0; i < users.length; i++) {
            c[i] = puds.getConnection(users[i], password);
            assertEquals(users[i], getUsername(c[i]));
        }
        for (int i = 0; i < users.length; i++) {
            c[i].close();
        }
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
        try {
            final Connection con4 = ds.getConnection("foo", "bay"); // new password
            // Idle instances with old password should have been cleared
            assertEquals("Should be no idle connections in the pool",
                    0, ((PerUserPoolDataSource) ds).getNumIdle("foo"));
            con4.close();
            // Should be one idle instance with new pwd
            assertEquals("Should be one idle connection in the pool",
                    1, ((PerUserPoolDataSource) ds).getNumIdle("foo"));
            try (Connection c = ds.getConnection("foo", "bar")) { // old password
                fail("Should have generated SQLException");
            } catch (final SQLException expected) {
            }
            final Connection con5 = ds.getConnection("foo", "bay"); // take the idle one
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
