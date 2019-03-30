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

package org.apache.commons.dbcp2.cpdsadapter;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Properties;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.sql.DataSource;

import org.apache.commons.dbcp2.datasources.SharedPoolDataSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for DriverAdapterCPDS
 */
public class TestDriverAdapterCPDS {

    private DriverAdapterCPDS pcds;

    @BeforeEach
    public void setUp() throws Exception {
        pcds = new DriverAdapterCPDS();
        pcds.setDriver("org.apache.commons.dbcp2.TesterDriver");
        pcds.setUrl("jdbc:apache:commons:testdriver");
        pcds.setUser("foo");
        pcds.setPassword("bar");
        pcds.setPoolPreparedStatements(false);
    }

    /**
     * JIRA: DBCP-245
     */
    @Test
    public void testIncorrectPassword() throws Exception
    {
        pcds.getPooledConnection("u2", "p2").close();
        try {
            // Use bad password
            pcds.getPooledConnection("u1", "zlsafjk");
            fail("Able to retrieve connection with incorrect password");
        } catch (final SQLException e1) {
            // should fail

        }

        // Use good password
        pcds.getPooledConnection("u1", "p1").close();
        try {
            pcds.getPooledConnection("u1", "x");
            fail("Able to retrieve connection with incorrect password");
        }
        catch (final SQLException e) {
            if (!e.getMessage().startsWith("x is not the correct password")) {
                throw e;
            }
            // else the exception was expected
        }

        // Make sure we can still use our good password.
        pcds.getPooledConnection("u1", "p1").close();
    }

    @Test
    public void testSimple() throws Exception {
        final Connection conn = pcds.getPooledConnection().getConnection();
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
    public void testSimpleWithUsername() throws Exception {
        final Connection conn = pcds.getPooledConnection("u1", "p1").getConnection();
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
        throws Exception {
        final Connection[] c = new Connection[10];
        for (int i=0; i<c.length; i++) {
            c[i] = pcds.getPooledConnection("u1", "p1").getConnection();
        }

        // close one of the connections
        c[0].close();
        assertTrue(c[0].isClosed());
        // get a new connection
        c[0] = pcds.getPooledConnection("u1", "p1").getConnection();

        for (final Connection element : c) {
            element.close();
        }

        // open all the connections
        for (int i=0; i<c.length; i++) {
            c[i] = pcds.getPooledConnection("u1", "p1").getConnection();
        }
        for (final Connection element : c) {
            element.close();
        }
    }

    @Test
    public void testSetConnectionProperties() throws Exception {
        // Set user property to bad value
        pcds.setUser("bad");
        // Supply correct value in connection properties
        // This will overwrite field value
        final Properties properties = new Properties();
        properties.put("user", "foo");
        properties.put("password", pcds.getPassword());
        pcds.setConnectionProperties(properties);
        pcds.getPooledConnection().close();
        assertEquals("foo", pcds.getUser());
        // Put bad password into properties
        properties.put("password", "bad");
        // This does not change local field
        assertEquals("bar", pcds.getPassword());
        // Supply correct password in getPooledConnection
        // Call will succeed and overwrite property
        pcds.getPooledConnection("foo", "bar").close();
        assertEquals("bar", pcds.getConnectionProperties().getProperty("password"));
    }

    @Test
    public void testSetConnectionPropertiesConnectionCalled() throws Exception {
        final Properties properties = new Properties();
        // call to the connection
        pcds.getPooledConnection().close();
        assertThrows(IllegalStateException.class, () -> pcds.setConnectionProperties(properties));
    }

    @Test
    public void testSetConnectionPropertiesNull() throws Exception {
        pcds.setConnectionProperties(null);
    }

    @Test
    public void testSetUserNull() throws Exception {
        pcds.setUser("Alice");
        assertEquals("Alice", pcds.getUser());
        pcds.setUser(null);
        assertEquals(null, pcds.getUser());
    }

    @Test
    public void testSetUserNullWithConnectionProperties() throws Exception {
        pcds.setConnectionProperties(new Properties());
        pcds.setUser("Alice");
        assertEquals("Alice", pcds.getUser());
        pcds.setUser(null);
        assertEquals(null, pcds.getUser());
    }

    @Test
    public void testSetPasswordNull() throws Exception {
        pcds.setPassword("Secret");
        assertEquals("Secret", pcds.getPassword());
        pcds.setPassword((char[]) null);
        assertEquals(null, pcds.getPassword());
    }

    @Test
    public void testSetPasswordThenModCharArray() {
        final char[] pwd = {'a' };
        pcds.setPassword(pwd);
        assertEquals("a", pcds.getPassword());
        pwd[0] = 'b';
        assertEquals("a", pcds.getPassword());
    }

    @Test
    public void testSetPasswordNullWithConnectionProperties() throws Exception {
        pcds.setConnectionProperties(new Properties());
        pcds.setPassword("Secret");
        assertEquals("Secret", pcds.getPassword());
        pcds.setPassword((char[]) null);
        assertEquals(null, pcds.getPassword());
    }

    /**
     * JIRA: DBCP-442
     */
    @Test
    public void testNullValidationQuery() throws Exception {
        final SharedPoolDataSource spds = new SharedPoolDataSource();
        spds.setConnectionPoolDataSource(pcds);
        spds.setDefaultTestOnBorrow(true);
        final Connection c = spds.getConnection();
        c.close();
        spds.close();
    }

    // https://issues.apache.org/jira/browse/DBCP-376
    @Test
    public void testDbcp367() throws Exception {
        final ThreadDbcp367[] threads = new ThreadDbcp367[200];

        pcds.setPoolPreparedStatements(true);
        pcds.setMaxPreparedStatements(-1);
        pcds.setAccessToUnderlyingConnectionAllowed(true);

        final SharedPoolDataSource spds = new SharedPoolDataSource();
        spds.setConnectionPoolDataSource(pcds);
        spds.setMaxTotal(threads.length + 10);
        spds.setDefaultMaxWaitMillis(-1);
        spds.setDefaultMaxIdle(10);
        spds.setDefaultAutoCommit(Boolean.FALSE);

        spds.setValidationQuery("SELECT 1");
        spds.setDefaultTimeBetweenEvictionRunsMillis(10000);
        spds.setDefaultNumTestsPerEvictionRun(-1);
        spds.setDefaultTestWhileIdle(true);
        spds.setDefaultTestOnBorrow(true);
        spds.setDefaultTestOnReturn(false);

        for (int i = 0; i < threads.length; i++) {
            threads[i] = new ThreadDbcp367(spds);
            threads[i].start();
        }

        for (int i = 0; i < threads.length; i++) {
            threads[i].join();
            Assertions.assertFalse(threads[i].isFailed(), "Thread " + i + " has failed");
        }
    }

    private static class ThreadDbcp367 extends Thread {

        private final DataSource ds;

        private volatile boolean failed = false;

        public ThreadDbcp367(final DataSource ds) {
            this.ds = ds;
        }

        @Override
        public void run() {
            Connection c = null;
            try {
                for (int j=0; j < 5000; j++) {
                    c = ds.getConnection();
                    c.close();
                }
            } catch (final SQLException sqle) {
                failed = true;
                sqle.printStackTrace();
            }
        }

        public boolean isFailed() {
            return failed;
        }
    }

    @Test
    public void testGetParentLogger() {
        assertThrows(SQLFeatureNotSupportedException.class, pcds::getParentLogger);
    }

    @Test
    public void testGetReference() throws NamingException {
        final Reference ref = pcds.getReference();
        assertEquals(pcds.getDriver(), ref.get("driver").getContent());
        assertEquals(pcds.getDescription(), ref.get("description").getContent());
    }

    @Test
    public void testGettersAndSetters() {
        pcds.setUser("foo");
        assertEquals("foo", pcds.getUser());
        pcds.setPassword("bar");
        assertEquals("bar", pcds.getPassword());
        pcds.setPassword(new char[] {'a', 'b'});
        assertArrayEquals(new char[] {'a', 'b'}, pcds.getPasswordCharArray());
        final PrintWriter pw = new PrintWriter(System.err);
        pcds.setLogWriter(pw);
        assertEquals(pw, pcds.getLogWriter());
        pcds.setLoginTimeout(10);
        assertEquals(10, pcds.getLoginTimeout());
        pcds.setMaxIdle(100);
        assertEquals(100, pcds.getMaxIdle());
        pcds.setTimeBetweenEvictionRunsMillis(100);
        assertEquals(100, pcds.getTimeBetweenEvictionRunsMillis());
        pcds.setNumTestsPerEvictionRun(1);
        assertEquals(1, pcds.getNumTestsPerEvictionRun());
        pcds.setMinEvictableIdleTimeMillis(11);
        assertEquals(11, pcds.getMinEvictableIdleTimeMillis());
        pcds.setDescription("jo");
        assertEquals("jo", pcds.getDescription());
    }

    @Test
    public void testGetObjectInstanceNull() throws Exception {
        final Object o = pcds.getObjectInstance(null, null, null, null);
        assertNull(o);
    }

    @Test
    public void testGetObjectInstance() throws Exception {
        final Reference ref = pcds.getReference();
        final Object o = pcds.getObjectInstance(ref, null, null, null);
        assertEquals(pcds.getDriver(), ((DriverAdapterCPDS) o).getDriver());
    }

    @Test
    public void testGetObjectInstanceChangeDescription() throws Exception {
        final Reference ref = pcds.getReference();
        for (int i = 0; i < ref.size(); i++) {
            if (ref.get(i).getType().equals("description")) {
                ref.remove(i);
                break;
            }
        }
        ref.add(new StringRefAddr("description", "anything"));
        final Object o = pcds.getObjectInstance(ref, null, null, null);
        assertEquals(pcds.getDescription(), ((DriverAdapterCPDS) o).getDescription());
    }
}
