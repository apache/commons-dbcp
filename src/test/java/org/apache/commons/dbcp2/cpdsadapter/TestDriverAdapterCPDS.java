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

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.time.Duration;
import java.util.Properties;

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.sql.DataSource;

import org.apache.commons.dbcp2.Constants;
import org.apache.commons.dbcp2.DelegatingPreparedStatement;
import org.apache.commons.dbcp2.DelegatingStatement;
import org.apache.commons.dbcp2.PStmtKey;
import org.apache.commons.dbcp2.PoolablePreparedStatement;
import org.apache.commons.dbcp2.TestUtils;
import org.apache.commons.dbcp2.datasources.SharedPoolDataSource;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for DriverAdapterCPDS
 */
public class TestDriverAdapterCPDS {

    private static final class ThreadDbcp367 extends Thread {

        private final DataSource dataSource;

        private volatile boolean failed;

        public ThreadDbcp367(final DataSource dataSource) {
            this.dataSource = dataSource;
        }

        public boolean isFailed() {
            return failed;
        }

        @Override
        public void run() {
            Connection conn = null;
            try {
                for (int j = 0; j < 5000; j++) {
                    conn = dataSource.getConnection();
                    conn.close();
                }
            } catch (final SQLException sqle) {
                failed = true;
                sqle.printStackTrace();
            }
        }
    }

    @SuppressWarnings("resource")
    private static void checkAfterClose(final Connection element, final PStmtKey pStmtKey) throws SQLException {
        final ConnectionImpl connectionImpl = (ConnectionImpl) element;
        assertNull(connectionImpl.getInnermostDelegate());
        assertNotNull(connectionImpl.getInnermostDelegateInternal());
        final PooledConnectionImpl pooledConnectionImpl = connectionImpl.getPooledConnectionImpl();
        assertNotNull(pooledConnectionImpl);
        // Simulate released resources, should not throw NPEs
        pooledConnectionImpl.destroyObject(pStmtKey, null);
        pooledConnectionImpl.destroyObject(pStmtKey, new DefaultPooledObject<>(null));
        pooledConnectionImpl.destroyObject(pStmtKey, new DefaultPooledObject<>(new DelegatingPreparedStatement(null, null)));
    }

    private DriverAdapterCPDS pcds;

    @BeforeEach
    public void setUp() throws Exception {
        pcds = new DriverAdapterCPDS();
        pcds.setDriver("org.apache.commons.dbcp2.TesterDriver");
        pcds.setUrl("jdbc:apache:commons:testdriver");
        pcds.setUser("foo");
        pcds.setPassword("bar");
        pcds.setPoolPreparedStatements(true);
    }

    @Test
    public void testClose()
            throws Exception {
        final Connection[] c = new Connection[10];
        for (int i = 0; i < c.length; i++) {
            c[i] = pcds.getPooledConnection().getConnection();
        }

        // close one of the connections
        c[0].close();
        assertTrue(c[0].isClosed());
        // get a new connection
        c[0] = pcds.getPooledConnection().getConnection();

        for (final Connection element : c) {
            element.close();
            checkAfterClose(element, null);
        }

        // open all the connections
        for (int i = 0; i < c.length; i++) {
            c[i] = pcds.getPooledConnection().getConnection();
        }
        for (final Connection element : c) {
            element.close();
            checkAfterClose(element, null);
        }
    }

    @Test
    public void testCloseWithUserName()
            throws Exception {
        final Connection[] c = new Connection[10];
        for (int i = 0; i < c.length; i++) {
            c[i] = pcds.getPooledConnection("u1", "p1").getConnection();
        }

        // close one of the connections
        c[0].close();
        assertTrue(c[0].isClosed());
        // get a new connection
        c[0] = pcds.getPooledConnection("u1", "p1").getConnection();

        for (final Connection element : c) {
            element.close();
            checkAfterClose(element, null);
        }

        // open all the connections
        for (int i = 0; i < c.length; i++) {
            c[i] = pcds.getPooledConnection("u1", "p1").getConnection();
        }
        for (final Connection element : c) {
            element.close();
            checkAfterClose(element, null);
        }
    }

    // https://issues.apache.org/jira/browse/DBCP-376
    @Test
    public void testDbcp367() throws Exception {
        final ThreadDbcp367[] threads = new ThreadDbcp367[200];

        pcds.setPoolPreparedStatements(true);
        pcds.setMaxPreparedStatements(-1);
        pcds.setAccessToUnderlyingConnectionAllowed(true);

        try (final SharedPoolDataSource spds = new SharedPoolDataSource()) {
            spds.setConnectionPoolDataSource(pcds);
            spds.setMaxTotal(threads.length + 10);
            spds.setDefaultMaxWait(Duration.ofMillis(-1));
            spds.setDefaultMaxIdle(10);
            spds.setDefaultAutoCommit(Boolean.FALSE);

            spds.setValidationQuery("SELECT 1");
            spds.setDefaultDurationBetweenEvictionRuns(Duration.ofSeconds(10));
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
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testDeprecatedAccessors() {
        int i = 0;
        //
        i++;
        pcds.setMinEvictableIdleTimeMillis(i);
        assertEquals(i, pcds.getMinEvictableIdleTimeMillis());
        assertEquals(Duration.ofMillis(i), pcds.getMinEvictableIdleDuration());
        //
        i++;
        pcds.setTimeBetweenEvictionRunsMillis(i);
        assertEquals(i, pcds.getTimeBetweenEvictionRunsMillis());
        assertEquals(Duration.ofMillis(i), pcds.getDurationBetweenEvictionRuns());
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

    @Test
    public void testGetObjectInstanceNull() throws Exception {
        final Object o = pcds.getObjectInstance(null, null, null, null);
        assertNull(o);
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
        @SuppressWarnings("resource")
        final PrintWriter logWriter = pcds.getLogWriter();
        assertEquals(pw, logWriter);
        pcds.setLoginTimeout(10);
        assertEquals(10, pcds.getLoginTimeout());
        pcds.setMaxIdle(100);
        assertEquals(100, pcds.getMaxIdle());
        pcds.setDurationBetweenEvictionRuns(Duration.ofMillis(100));
        assertEquals(100, pcds.getDurationBetweenEvictionRuns().toMillis());
        pcds.setNumTestsPerEvictionRun(1);
        assertEquals(1, pcds.getNumTestsPerEvictionRun());
        pcds.setMinEvictableIdleDuration(Duration.ofMillis(11));
        assertEquals(Duration.ofMillis(11), pcds.getMinEvictableIdleDuration());
        pcds.setDescription("jo");
        assertEquals("jo", pcds.getDescription());
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

    /**
     * JIRA: DBCP-442
     */
    @Test
    public void testNullValidationQuery() throws Exception {
        try (final SharedPoolDataSource spds = new SharedPoolDataSource()) {
            spds.setConnectionPoolDataSource(pcds);
            spds.setDefaultTestOnBorrow(true);
            try (final Connection c = spds.getConnection()) {
                // close right away
            }
        }
    }

    @Test
    public void testSetConnectionProperties() throws Exception {
        // Set user property to bad value
        pcds.setUser("bad");
        // Supply correct value in connection properties
        // This will overwrite field value
        final Properties properties = new Properties();
        properties.put(Constants.KEY_USER, "foo");
        properties.put(Constants.KEY_PASSWORD, pcds.getPassword());
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
    public void testSetPasswordNull() throws Exception {
        pcds.setPassword("Secret");
        assertEquals("Secret", pcds.getPassword());
        pcds.setPassword((char[]) null);
        assertNull(pcds.getPassword());
    }

    @Test
    public void testSetPasswordNullWithConnectionProperties() throws Exception {
        pcds.setConnectionProperties(new Properties());
        pcds.setPassword("Secret");
        assertEquals("Secret", pcds.getPassword());
        pcds.setPassword((char[]) null);
        assertNull(pcds.getPassword());
    }

    @Test
    public void testSetPasswordThenModCharArray() {
        final char[] pwd = {'a'};
        pcds.setPassword(pwd);
        assertEquals("a", pcds.getPassword());
        pwd[0] = 'b';
        assertEquals("a", pcds.getPassword());
    }

    @Test
    public void testSetUserNull() throws Exception {
        pcds.setUser("Alice");
        assertEquals("Alice", pcds.getUser());
        pcds.setUser(null);
        assertNull(pcds.getUser());
    }

    @Test
    public void testSetUserNullWithConnectionProperties() throws Exception {
        pcds.setConnectionProperties(new Properties());
        pcds.setUser("Alice");
        assertEquals("Alice", pcds.getUser());
        pcds.setUser(null);
        assertNull(pcds.getUser());
    }

    @Test
    public void testSimple() throws Exception {
        try (final Connection conn = pcds.getPooledConnection().getConnection()) {
            assertNotNull(conn);
            try (final PreparedStatement stmt = conn.prepareStatement("select * from dual")) {
                assertNotNull(stmt);
                try (final ResultSet resultSet = stmt.executeQuery()) {
                    assertNotNull(resultSet);
                    assertTrue(resultSet.next());
                }
            }
        }
    }

    @SuppressWarnings("resource")
    @Test
    public void testSimpleWithUsername() throws Exception {
        final Connection connCheck;
        PStmtKey pStmtKey;
        try (final Connection conn = pcds.getPooledConnection("u1", "p1").getConnection()) {
            assertNotNull(conn);
            connCheck = conn;
            try (final PreparedStatement stmt = conn.prepareStatement("select * from dual")) {
                assertNotNull(stmt);
                final DelegatingStatement delegatingStatement = (DelegatingStatement) stmt;
                final Statement delegateStatement = delegatingStatement.getDelegate();
                pStmtKey = TestUtils.getPStmtKey((PoolablePreparedStatement) delegateStatement);
                assertNotNull(pStmtKey);
                try (final ResultSet resultSet = stmt.executeQuery()) {
                    assertNotNull(resultSet);
                    assertTrue(resultSet.next());
                }
            }
        }
        checkAfterClose(connCheck, pStmtKey);
    }

    @Test
    public void testToStringWithoutConnectionProperties() throws ClassNotFoundException {
        final DriverAdapterCPDS cleanCpds = new DriverAdapterCPDS();
        cleanCpds.setDriver("org.apache.commons.dbcp2.TesterDriver");
        cleanCpds.setUrl("jdbc:apache:commons:testdriver");
        cleanCpds.setUser("foo");
        cleanCpds.setPassword("bar");
        cleanCpds.toString();
    }
}
