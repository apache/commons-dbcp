/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.TestConnectionPool;
import org.apache.commons.dbcp2.TesterDriver;
import org.apache.commons.dbcp2.cpdsadapter.DriverAdapterCPDS;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 */
public class TestPerUserPoolDataSource extends TestConnectionPool {

    private static final Duration DURATION_1_MILLISECOND = Duration.ofMillis(1);

    private String user;

    private DataSource ds;

    @Override
    protected Connection getConnection() throws SQLException {
        return ds.getConnection(user, "bar");
    }

    @BeforeEach
    public void setUp() throws Exception {
        user = "foo";
        final DriverAdapterCPDS pcds = new DriverAdapterCPDS();
        pcds.setDriver("org.apache.commons.dbcp2.TesterDriver");
        pcds.setUrl("jdbc:apache:commons:testdriver");
        pcds.setUser(user);
        pcds.setPassword("bar");
        pcds.setAccessToUnderlyingConnectionAllowed(true);

        final PerUserPoolDataSource tds = new PerUserPoolDataSource();
        tds.setConnectionPoolDataSource(pcds);
        tds.setDefaultMaxTotal(getMaxTotal());
        tds.setDefaultMaxWait(getMaxWaitDuration());
        tds.setPerUserMaxTotal(user, getMaxTotal());
        tds.setPerUserMaxWait(user, getMaxWaitDuration());
        tds.setDefaultTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        tds.setDefaultAutoCommit(Boolean.TRUE);
        ds = tds;
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
        ((PerUserPoolDataSource) ds).close();
    }

    /**
     * See DBCP-8
     */
    @Test
    public void testChangePassword() throws Exception {
        assertThrows(SQLException.class, () -> ds.getConnection(user, "bay"));
        final Connection con1 = ds.getConnection(user, "bar");
        final Connection con2 = ds.getConnection(user, "bar");
        final Connection con3 = ds.getConnection(user, "bar");
        con1.close();
        con2.close();
        TesterDriver.addUser(user, "bay"); // change the user/password setting
        try {
            final Connection con4 = ds.getConnection(user, "bay"); // new password
            // Idle instances with old password should have been cleared
            assertEquals(0, ((PerUserPoolDataSource) ds).getNumIdle(user), "Should be no idle connections in the pool");
            con4.close();
            // Should be one idle instance with new pwd
            assertEquals(1, ((PerUserPoolDataSource) ds).getNumIdle(user), "Should be one idle connection in the pool");
            // old password
            assertThrows(SQLException.class, () -> ds.getConnection(user, "bar"), "Should have generated SQLException");
            try (Connection con5 = ds.getConnection(user, "bay")) { // take the idle one
                con3.close(); // Return a connection with the old password
                ds.getConnection(user, "bay").close(); // will try bad returned connection and destroy it
                assertEquals(1, ((PerUserPoolDataSource) ds).getNumIdle(user), "Should be one idle connection in the pool");
            }
        } finally {
            TesterDriver.addUser(user, "bar");
        }
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

    /**
     * Tests https://issues.apache.org/jira/browse/DBCP-597
     */
    @Test
    public void testDbcp597() throws SQLException {
        PerUserPoolDataSource tds = (PerUserPoolDataSource) ds;
        tds.setDefaultTestOnBorrow(true);
        tds.setValidationQuery("SELECT 1");
        // The tester statement throws a SQLTimeoutException when the timeout is > 0 and < 5.
        tds.setValidationQueryTimeout(Duration.ofSeconds(1));
        // The SQLTimeoutException is lost for now
        SQLException e = assertThrows(SQLException.class, tds::getConnection);
        assertEquals(NoSuchElementException.class, e.getCause().getClass());
        // timeout > 0 and < 1
        tds.setValidationQueryTimeout(Duration.ofMillis(999));
        // The SQLTimeoutException is lost for now
        e = assertThrows(SQLException.class, tds::getConnection);
        assertEquals(NoSuchElementException.class, e.getCause().getClass());
    }

    // see issue https://issues.apache.org/bugzilla/show_bug.cgi?id=23843
    @Test
    public void testDefaultUser1() throws Exception {
        TesterDriver.addUser("mkh", "password");
        TesterDriver.addUser("hanafey", "password");
        TesterDriver.addUser("jsmith", "password");

        final PerUserPoolDataSource puds = (PerUserPoolDataSource) ds;
        puds.setPerUserMaxTotal("jsmith", 2);
        final String[] users = { "mkh", "hanafey", "jsmith" };
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

    // see issue https://issues.apache.org/bugzilla/show_bug.cgi?id=23843
    @Test
    public void testDefaultUser2() throws Exception {
        TesterDriver.addUser("mkh", "password");
        TesterDriver.addUser("hanafey", "password");
        TesterDriver.addUser("jsmith", "password");

        final PerUserPoolDataSource puds = (PerUserPoolDataSource) ds;
        puds.setPerUserMaxTotal("jsmith", 2);
        final String[] users = { "jsmith", "hanafey", "mkh" };
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

    @SuppressWarnings("deprecation")
    @Test
    public void testDepreactedAccessors() {
        try (final PerUserPoolDataSource ds = new PerUserPoolDataSource()) {
            int i = 0;
            //
            i++;
            ds.setDefaultMaxWaitMillis(i);
            assertEquals(i, ds.getDefaultMaxWaitMillis());
            assertEquals(Duration.ofMillis(i), ds.getDefaultMaxWait());
            //
            i++;
            ds.setDefaultMinEvictableIdleTimeMillis(i);
            assertEquals(i, ds.getDefaultMinEvictableIdleTimeMillis());
            assertEquals(Duration.ofMillis(i), ds.getDefaultMinEvictableIdleDuration());
            //
            i++;
            ds.setDefaultSoftMinEvictableIdleTimeMillis(i);
            assertEquals(i, ds.getDefaultSoftMinEvictableIdleTimeMillis());
            assertEquals(Duration.ofMillis(i), ds.getDefaultSoftMinEvictableIdleDuration());
            //
            i++;
            ds.setDefaultTimeBetweenEvictionRunsMillis(i);
            assertEquals(i, ds.getDefaultTimeBetweenEvictionRunsMillis());
            assertEquals(Duration.ofMillis(i), ds.getDefaultDurationBetweenEvictionRuns());
            //
            i++;
            ds.setPerUserMaxWaitMillis(user, Long.valueOf(i));
            assertEquals(i, ds.getPerUserMaxWaitMillis(user));
            assertEquals(Duration.ofMillis(i), ds.getPerUserMaxWaitDuration(user));
            //
            i++;
            ds.setPerUserMinEvictableIdleTimeMillis(user, Long.valueOf(i));
            assertEquals(i, ds.getPerUserMinEvictableIdleTimeMillis(user));
            assertEquals(Duration.ofMillis(i), ds.getPerUserMinEvictableIdleDuration(user));
            //
            i++;
            ds.setPerUserSoftMinEvictableIdleTimeMillis(user, Long.valueOf(i));
            assertEquals(i, ds.getPerUserSoftMinEvictableIdleTimeMillis(user));
            assertEquals(Duration.ofMillis(i), ds.getPerUserSoftMinEvictableIdleDuration(user));
            //
            i++;
            ds.setPerUserTimeBetweenEvictionRunsMillis(user, Long.valueOf(i));
            assertEquals(i, ds.getPerUserTimeBetweenEvictionRunsMillis(user));
            assertEquals(Duration.ofMillis(i), ds.getPerUserDurationBetweenEvictionRuns(user));
        }
    }

    /**
     * Switching 'u1 to 'u2' and 'p1' to 'p2' will exhibit the bug detailed in https://issues.apache.org/bugzilla/show_bug.cgi?id=18905
     */
    @Test
    public void testIncorrectPassword() throws SQLException {
        // Use bad password
        assertThrows(SQLException.class, () -> ds.getConnection("u1", "zlsafjk"));

        // Use good password
        ds.getConnection("u1", "p1").close();
        final SQLException e = assertThrows(SQLException.class, () -> ds.getConnection("u1", "x"), "Able to retrieve connection with incorrect password");
        assertTrue(e.getMessage().startsWith("Given password did not match"));

        // Make sure we can still use our good password.
        ds.getConnection("u1", "p1").close();

        // Try related users and passwords
        ds.getConnection(user, "bar").close();
        assertThrows(SQLException.class, () -> ds.getConnection("foob", "ar"));
        assertThrows(SQLException.class, () -> ds.getConnection(user, "baz"));
    }

    @Override
    @Test
    public void testMaxTotal() throws Exception {
        final Connection[] c = new Connection[getMaxTotal()];
        for (int i = 0; i < c.length; i++) {
            c[i] = ds.getConnection();
            assertNotNull(c[i]);
        }
        // should only be able to open 10 connections, so this test should throw an
        // exception
        assertThrows(SQLException.class, ds::getConnection, "Allowed to open more than DefaultMaxTotal connections.");
        for (final Connection element : c) {
            element.close();
        }
    }

    /**
     * Verify that defaultMaxWaitMillis = 0 means immediate failure when pool is exhausted.
     */
    @Test
    public void testMaxWaitMillisZero() throws Exception {
        final PerUserPoolDataSource tds = (PerUserPoolDataSource) ds;
        tds.setDefaultMaxWait(Duration.ZERO);
        tds.setPerUserMaxTotal("u1", 1);
        try (final Connection conn = tds.getConnection("u1", "p1")) {
            assertThrows(SQLException.class, () -> tds.getConnection("u1", "p1"));
        }
    }

    @Test
    public void testMultipleThreads1() throws Exception {
        // Override wait time in order to allow for Thread.sleep(1) sometimes taking a lot longer on
        // some JVMs, e.g. Windows.
        final Duration defaultMaxWaitDuration = Duration.ofMillis(430);
        ((PerUserPoolDataSource) ds).setDefaultMaxWait(defaultMaxWaitDuration);
        ((PerUserPoolDataSource) ds).setPerUserMaxWait(user, defaultMaxWaitDuration);
        multipleThreads(Duration.ofMillis(1), false, false, defaultMaxWaitDuration);
    }

    @Test
    public void testMultipleThreads2() throws Exception {
        final Duration defaultMaxWaitDuration = Duration.ofMillis(500);
        ((PerUserPoolDataSource) ds).setDefaultMaxWait(defaultMaxWaitDuration);
        ((PerUserPoolDataSource) ds).setPerUserMaxWait(user, defaultMaxWaitDuration);
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
     * Test per user block when exhausted, with the backing map initialized before.
     */
    @Test
    public void testPerUserBlockWhenExhaustedMapInitialized() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        Map<String, Boolean> userDefaultBlockWhenExhausted = new HashMap<>();
        userDefaultBlockWhenExhausted.put("key", Boolean.FALSE);
        ds.setPerUserBlockWhenExhausted(userDefaultBlockWhenExhausted);
        assertEquals(Boolean.FALSE, ds.getPerUserBlockWhenExhausted("key"));
        // when the code above is executed, the backing map was initalized
        // now check if that still works. The backing map is clear'ed.
        userDefaultBlockWhenExhausted = new HashMap<>();
        userDefaultBlockWhenExhausted.put("anonymous", Boolean.FALSE);
        ds.setPerUserBlockWhenExhausted(userDefaultBlockWhenExhausted);
        // now the previously entered value was cleared, so it will be back to the
        // default value of TRUE
        assertEquals(Boolean.TRUE, ds.getPerUserBlockWhenExhausted("key"));
        // and our new value exists too
        assertEquals(Boolean.FALSE, ds.getPerUserBlockWhenExhausted("anonymous"));
    }

    /**
     * Test per user block when exhausted, with the backing map not initialized before. Instead we pass the map.
     */
    @Test
    public void testPerUserBlockWhenExhaustedMapNotInitialized() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        final Map<String, Boolean> userDefaultBlockWhenExhausted = new HashMap<>();
        userDefaultBlockWhenExhausted.put("key", Boolean.TRUE);
        ds.setPerUserBlockWhenExhausted(userDefaultBlockWhenExhausted);
        assertEquals(Boolean.TRUE, ds.getPerUserBlockWhenExhausted("key"));
    }

    /**
     * Test per user block when exhausted, with the backing map not initialized before. Instead, we pass the map. And furthermore, we are now searching for an
     * inexistent key, which should return the default value.
     */
    @Test
    public void testPerUserBlockWhenExhaustedMapNotInitializedMissingKey() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        final Map<String, Boolean> userDefaultBlockWhenExhausted = new HashMap<>();
        userDefaultBlockWhenExhausted.put("key", Boolean.FALSE);
        ds.setPerUserBlockWhenExhausted(userDefaultBlockWhenExhausted);
        assertEquals(ds.getDefaultBlockWhenExhausted(), ds.getPerUserBlockWhenExhausted("missingkey"));
    }

    /**
     * Test per user block when exhausted, with the backing map not initialized before. Instead we pass the user and value, and hence the map is initialized
     * beforehand. After that, we pass another user, so both values should still be present. The PerUserPoolDataSource does not clear the perUserPoolDataSource
     * map, unless you pass a new map, using another internal/package method.
     */
    @Test
    public void testPerUserBlockWhenExhaustedWithUserMapInitialized() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        ds.setPerUserBlockWhenExhausted(user, Boolean.FALSE);
        assertEquals(Boolean.FALSE, ds.getPerUserBlockWhenExhausted(user));
        // when the code above is executed, the backing map was initalized
        // now check if that still works. The backing map is NOT clear'ed.
        ds.setPerUserBlockWhenExhausted("anotheruser", Boolean.FALSE);
        assertEquals(Boolean.FALSE, ds.getPerUserBlockWhenExhausted(user));
        assertEquals(Boolean.FALSE, ds.getPerUserBlockWhenExhausted("anotheruser"));
    }

    /**
     * Test per user block when exhausted, with the backing map not initialized before. Instead we pass the user and value, and hence the map is initialized
     * beforehand.
     */
    @Test
    public void testPerUserBlockWhenExhaustedWithUserMapNotInitialized() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        ds.setPerUserBlockWhenExhausted(user, Boolean.FALSE);
        assertEquals(Boolean.FALSE, ds.getPerUserBlockWhenExhausted(user));
    }

    /**
     * Test per user block when exhausted, with the backing map not initialized before. Instead we pass the user and value, and hence the map is initialized
     * beforehand. Furthermore, we are now searching for an inexistent key, which should return the default value.
     */
    @Test
    public void testPerUserBlockWhenExhaustedWithUserMapNotInitializedMissingKey() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        ds.setPerUserBlockWhenExhausted("whatismyuseragain?", Boolean.FALSE);
        assertEquals(Boolean.TRUE, ds.getPerUserBlockWhenExhausted("missingkey"));
    }

    @Test
    public void testPerUserDefaultAutoCommitMapInitialized() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        Map<String, Boolean> values = new HashMap<>();
        values.put("key", Boolean.FALSE);
        ds.setPerUserDefaultAutoCommit(values);
        assertEquals(Boolean.FALSE, ds.getPerUserDefaultAutoCommit("key"));
        values = new HashMap<>();
        values.put("anonymous", Boolean.FALSE);
        ds.setPerUserDefaultAutoCommit(values);
        assertNull(ds.getPerUserDefaultAutoCommit("key"));
        assertEquals(Boolean.FALSE, ds.getPerUserDefaultAutoCommit("anonymous"));
    }

    // getters and setters. Most follow the same pattern. The initial tests contain a more
    // complete documentation, which can be helpful when write/understanding the other methods.

    // -- per user block when exhausted

    @Test
    public void testPerUserDefaultAutoCommitMapNotInitialized() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        final Map<String, Boolean> values = new HashMap<>();
        values.put("key", Boolean.TRUE);
        ds.setPerUserDefaultAutoCommit(values);
        assertEquals(Boolean.TRUE, ds.getPerUserDefaultAutoCommit("key"));
    }

    @Test
    public void testPerUserDefaultAutoCommitMapNotInitializedMissingKey() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        final Map<String, Boolean> values = new HashMap<>();
        values.put("key", Boolean.FALSE);
        ds.setPerUserDefaultAutoCommit(values);
        // TODO this is not consistent with the other methods
        assertNull(ds.getPerUserDefaultAutoCommit("missingkey"));
    }

    @Test
    public void testPerUserDefaultAutoCommitWithUserMapInitialized() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        ds.setPerUserDefaultAutoCommit(user, Boolean.FALSE);
        assertEquals(Boolean.FALSE, ds.getPerUserDefaultAutoCommit(user));
        ds.setPerUserDefaultAutoCommit("anotheruser", Boolean.FALSE);
        assertEquals(Boolean.FALSE, ds.getPerUserDefaultAutoCommit(user));
        assertEquals(Boolean.FALSE, ds.getPerUserDefaultAutoCommit("anotheruser"));
    }

    @Test
    public void testPerUserDefaultAutoCommitWithUserMapNotInitialized() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        ds.setPerUserDefaultAutoCommit(user, Boolean.FALSE);
        assertEquals(Boolean.FALSE, ds.getPerUserDefaultAutoCommit(user));
    }

    @Test
    public void testPerUserDefaultAutoCommitWithUserMapNotInitializedMissingKey() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        ds.setPerUserDefaultAutoCommit("whatismyuseragain?", Boolean.FALSE);
        // TODO this is not consistent with the other methods
        assertNull(ds.getPerUserDefaultAutoCommit("missingkey"));
    }

    @Test
    public void testPerUserDefaultReadOnlyMapInitialized() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        Map<String, Boolean> values = new HashMap<>();
        values.put("key", Boolean.FALSE);
        ds.setPerUserDefaultReadOnly(values);
        assertEquals(Boolean.FALSE, ds.getPerUserDefaultReadOnly("key"));
        values = new HashMap<>();
        values.put("anonymous", Boolean.FALSE);
        ds.setPerUserDefaultReadOnly(values);
        assertNull(ds.getPerUserDefaultReadOnly("key"));
        assertEquals(Boolean.FALSE, ds.getPerUserDefaultReadOnly("anonymous"));
    }

    // -- per user default auto commit

    @Test
    public void testPerUserDefaultReadOnlyMapNotInitialized() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        final Map<String, Boolean> values = new HashMap<>();
        values.put("key", Boolean.TRUE);
        ds.setPerUserDefaultReadOnly(values);
        assertEquals(Boolean.TRUE, ds.getPerUserDefaultReadOnly("key"));
    }

    @Test
    public void testPerUserDefaultReadOnlyMapNotInitializedMissingKey() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        final Map<String, Boolean> values = new HashMap<>();
        values.put("key", Boolean.FALSE);
        ds.setPerUserDefaultReadOnly(values);
        // TODO this is not consistent with the other methods
        assertNull(ds.getPerUserDefaultReadOnly("missingkey"));
    }

    @Test
    public void testPerUserDefaultReadOnlyWithUserMapInitialized() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        ds.setPerUserDefaultReadOnly(user, Boolean.FALSE);
        assertEquals(Boolean.FALSE, ds.getPerUserDefaultReadOnly(user));
        ds.setPerUserDefaultReadOnly("anotheruser", Boolean.FALSE);
        assertEquals(Boolean.FALSE, ds.getPerUserDefaultReadOnly(user));
        assertEquals(Boolean.FALSE, ds.getPerUserDefaultReadOnly("anotheruser"));
    }

    @Test
    public void testPerUserDefaultReadOnlyWithUserMapNotInitialized() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        ds.setPerUserDefaultReadOnly(user, Boolean.FALSE);
        assertEquals(Boolean.FALSE, ds.getPerUserDefaultReadOnly(user));
    }

    @Test
    public void testPerUserDefaultReadOnlyWithUserMapNotInitializedMissingKey() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        ds.setPerUserDefaultReadOnly("whatismyuseragain?", Boolean.FALSE);
        // TODO this is not consistent with the other methods
        assertNull(ds.getPerUserDefaultReadOnly("missingkey"));
    }

    @Test
    public void testPerUserDefaultTransactionIsolationMapInitialized() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        Map<String, Integer> values = new HashMap<>();
        values.put("key", 0);
        ds.setPerUserDefaultTransactionIsolation(values);
        assertEquals((Integer) 0, ds.getPerUserDefaultTransactionIsolation("key"));
        values = new HashMap<>();
        values.put("anonymous", 0);
        ds.setPerUserDefaultTransactionIsolation(values);
        // TODO this is not consistent with the other methods
        assertNull(ds.getPerUserDefaultTransactionIsolation("key"));
        assertEquals((Integer) 0, ds.getPerUserDefaultTransactionIsolation("anonymous"));
    }

    // -- per user default read only

    @Test
    public void testPerUserDefaultTransactionIsolationMapNotInitialized() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        final Map<String, Integer> values = new HashMap<>();
        values.put("key", 1);
        ds.setPerUserDefaultTransactionIsolation(values);
        assertEquals((Integer) 1, ds.getPerUserDefaultTransactionIsolation("key"));
    }

    @Test
    public void testPerUserDefaultTransactionIsolationMapNotInitializedMissingKey() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        final Map<String, Integer> values = new HashMap<>();
        values.put("key", 0);
        ds.setPerUserDefaultTransactionIsolation(values);
        // TODO this is not consistent with the other methods
        assertNull(ds.getPerUserDefaultTransactionIsolation("missingkey"));
    }

    @Test
    public void testPerUserDefaultTransactionIsolationWithUserMapInitialized() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        ds.setPerUserDefaultTransactionIsolation(user, 0);
        assertEquals((Integer) 0, ds.getPerUserDefaultTransactionIsolation(user));
        ds.setPerUserDefaultTransactionIsolation("anotheruser", 0);
        assertEquals((Integer) 0, ds.getPerUserDefaultTransactionIsolation(user));
        assertEquals((Integer) 0, ds.getPerUserDefaultTransactionIsolation("anotheruser"));
    }

    @Test
    public void testPerUserDefaultTransactionIsolationWithUserMapNotInitialized() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        ds.setPerUserDefaultTransactionIsolation(user, 0);
        assertEquals((Integer) 0, ds.getPerUserDefaultTransactionIsolation(user));
    }

    @Test
    public void testPerUserDefaultTransactionIsolationWithUserMapNotInitializedMissingKey() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        ds.setPerUserDefaultTransactionIsolation("whatismyuseragain?", 0);
        // TODO this is not consistent with the other methods
        assertNull(ds.getPerUserDefaultTransactionIsolation("missingkey"));
    }

    @Test
    public void testPerUserDurationBetweenEvictionRunsMapInitialized() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        Map<String, Duration> values = new HashMap<>();
        values.put("key", Duration.ZERO);
        ds.setPerUserDurationBetweenEvictionRuns(values);
        assertEquals(0L, ds.getPerUserTimeBetweenEvictionRunsMillis("key"));
        assertEquals(Duration.ZERO, ds.getPerUserDurationBetweenEvictionRuns("key"));
        values = new HashMap<>();
        values.put("anonymous", Duration.ZERO);
        ds.setPerUserDurationBetweenEvictionRuns(values);
        assertEquals(ds.getDefaultTimeBetweenEvictionRunsMillis(), ds.getPerUserTimeBetweenEvictionRunsMillis("key"));
        assertEquals(ds.getDefaultDurationBetweenEvictionRuns(), ds.getPerUserDurationBetweenEvictionRuns("key"));
        assertEquals(0L, ds.getPerUserTimeBetweenEvictionRunsMillis("anonymous"));
        assertEquals(Duration.ZERO, ds.getPerUserDurationBetweenEvictionRuns("anonymous"));
    }

    // -- per user default transaction isolation

    @Test
    public void testPerUserDurationBetweenEvictionRunsMapNotInitialized() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        final Map<String, Duration> values = new HashMap<>();
        values.put("key", DURATION_1_MILLISECOND);
        ds.setPerUserDurationBetweenEvictionRuns(values);
        assertEquals(1L, ds.getPerUserTimeBetweenEvictionRunsMillis("key"));
        assertEquals(DURATION_1_MILLISECOND, ds.getPerUserDurationBetweenEvictionRuns("key"));
    }

    @Test
    public void testPerUserDurationBetweenEvictionRunsMapNotInitializedMissingKey() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        final Map<String, Duration> values = new HashMap<>();
        values.put("key", Duration.ZERO);
        ds.setPerUserDurationBetweenEvictionRuns(values);
        assertEquals(ds.getDefaultTimeBetweenEvictionRunsMillis(), ds.getPerUserTimeBetweenEvictionRunsMillis("missingkey"));
        assertEquals(ds.getDefaultDurationBetweenEvictionRuns(), ds.getPerUserDurationBetweenEvictionRuns("missingkey"));
    }

    @Test
    public void testPerUserEvictionPolicyClassNameMapInitialized() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        Map<String, String> values = new HashMap<>();
        values.put("key", "bar");
        ds.setPerUserEvictionPolicyClassName(values);
        assertEquals("bar", ds.getPerUserEvictionPolicyClassName("key"));
        values = new HashMap<>();
        values.put("anonymous", "bar");
        ds.setPerUserEvictionPolicyClassName(values);
        assertEquals(ds.getDefaultEvictionPolicyClassName(), ds.getPerUserEvictionPolicyClassName("key"));
        assertEquals("bar", ds.getPerUserEvictionPolicyClassName("anonymous"));
    }

    @Test
    public void testPerUserEvictionPolicyClassNameMapNotInitialized() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        final Map<String, String> values = new HashMap<>();
        values.put("key", "test");
        ds.setPerUserEvictionPolicyClassName(values);
        assertEquals("test", ds.getPerUserEvictionPolicyClassName("key"));
    }

    @Test
    public void testPerUserEvictionPolicyClassNameMapNotInitializedMissingKey() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        final Map<String, String> values = new HashMap<>();
        values.put("key", "bar");
        ds.setPerUserEvictionPolicyClassName(values);
        assertEquals(ds.getDefaultEvictionPolicyClassName(), ds.getPerUserEvictionPolicyClassName("missingkey"));
    }

    @Test
    public void testPerUserEvictionPolicyClassNameWithUserMapInitialized() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        ds.setPerUserEvictionPolicyClassName(user, "bar");
        assertEquals("bar", ds.getPerUserEvictionPolicyClassName(user));
        ds.setPerUserEvictionPolicyClassName("anotheruser", "bar");
        assertEquals("bar", ds.getPerUserEvictionPolicyClassName(user));
        assertEquals("bar", ds.getPerUserEvictionPolicyClassName("anotheruser"));
    }

    // -- per user eviction policy class name

    @Test
    public void testPerUserEvictionPolicyClassNameWithUserMapNotInitialized() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        ds.setPerUserEvictionPolicyClassName(user, "bar");
        assertEquals("bar", ds.getPerUserEvictionPolicyClassName(user));
    }

    @Test
    public void testPerUserEvictionPolicyClassNameWithUserMapNotInitializedMissingKey() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        ds.setPerUserEvictionPolicyClassName("whatismyuseragain?", "bar");
        assertEquals(ds.getDefaultEvictionPolicyClassName(), ds.getPerUserEvictionPolicyClassName("missingkey"));
    }

    @Test
    public void testPerUserLifoMapInitialized() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        Map<String, Boolean> values = new HashMap<>();
        values.put("key", Boolean.FALSE);
        ds.setPerUserLifo(values);
        assertEquals(Boolean.FALSE, ds.getPerUserLifo("key"));
        values = new HashMap<>();
        values.put("anonymous", Boolean.FALSE);
        ds.setPerUserLifo(values);
        assertEquals(ds.getDefaultLifo(), ds.getPerUserLifo("key"));
        assertEquals(Boolean.FALSE, ds.getPerUserLifo("anonymous"));
    }

    @Test
    public void testPerUserLifoMapNotInitialized() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        final Map<String, Boolean> values = new HashMap<>();
        values.put("key", Boolean.TRUE);
        ds.setPerUserLifo(values);
        assertEquals(Boolean.TRUE, ds.getPerUserLifo("key"));
    }

    @Test
    public void testPerUserLifoMapNotInitializedMissingKey() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        final Map<String, Boolean> values = new HashMap<>();
        values.put("key", Boolean.FALSE);
        ds.setPerUserLifo(values);
        assertEquals(ds.getDefaultLifo(), ds.getPerUserLifo("missingkey"));
    }

    @Test
    public void testPerUserLifoWithUserMapInitialized() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        ds.setPerUserLifo(user, Boolean.FALSE);
        assertEquals(Boolean.FALSE, ds.getPerUserLifo(user));
        ds.setPerUserLifo("anotheruser", Boolean.FALSE);
        assertEquals(Boolean.FALSE, ds.getPerUserLifo(user));
        assertEquals(Boolean.FALSE, ds.getPerUserLifo("anotheruser"));
    }

    // -- per user lifo

    @Test
    public void testPerUserLifoWithUserMapNotInitialized() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        ds.setPerUserLifo(user, Boolean.FALSE);
        assertEquals(Boolean.FALSE, ds.getPerUserLifo(user));
    }

    @Test
    public void testPerUserLifoWithUserMapNotInitializedMissingKey() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        ds.setPerUserLifo("whatismyuseragain?", Boolean.FALSE);
        assertEquals(ds.getDefaultLifo(), ds.getPerUserLifo("missingkey"));
    }

    @Test
    public void testPerUserMaxIdleMapInitialized() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        Map<String, Integer> values = new HashMap<>();
        values.put("key", 0);
        ds.setPerUserMaxIdle(values);
        assertEquals((Integer) 0, (Integer) ds.getPerUserMaxIdle("key"));
        values = new HashMap<>();
        values.put("anonymous", 0);
        ds.setPerUserMaxIdle(values);
        assertEquals((Integer) ds.getDefaultMaxIdle(), (Integer) ds.getPerUserMaxIdle("key"));
        assertEquals((Integer) 0, (Integer) ds.getPerUserMaxIdle("anonymous"));
    }

    @Test
    public void testPerUserMaxIdleMapNotInitialized() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        final Map<String, Integer> values = new HashMap<>();
        values.put("key", 1);
        ds.setPerUserMaxIdle(values);
        assertEquals((Integer) 1, (Integer) ds.getPerUserMaxIdle("key"));
    }

    @Test
    public void testPerUserMaxIdleMapNotInitializedMissingKey() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        final Map<String, Integer> values = new HashMap<>();
        values.put("key", 0);
        ds.setPerUserMaxIdle(values);
        assertEquals((Integer) ds.getDefaultMaxIdle(), (Integer) ds.getPerUserMaxIdle("missingkey"));
    }

    @Test
    public void testPerUserMaxIdleWithUserMapInitialized() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        ds.setPerUserMaxIdle(user, 0);
        assertEquals((Integer) 0, (Integer) ds.getPerUserMaxIdle(user));
        ds.setPerUserMaxIdle("anotheruser", 0);
        assertEquals((Integer) 0, (Integer) ds.getPerUserMaxIdle(user));
        assertEquals((Integer) 0, (Integer) ds.getPerUserMaxIdle("anotheruser"));
    }

    // -- per user max idle

    @Test
    public void testPerUserMaxIdleWithUserMapNotInitialized() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        ds.setPerUserMaxIdle(user, 0);
        assertEquals((Integer) 0, (Integer) ds.getPerUserMaxIdle(user));
    }

    @Test
    public void testPerUserMaxIdleWithUserMapNotInitializedMissingKey() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        ds.setPerUserMaxIdle("whatismyuseragain?", 0);
        assertEquals((Integer) ds.getDefaultMaxIdle(), (Integer) ds.getPerUserMaxIdle("missingkey"));
    }

    @Test
    public void testPerUserMaxTotalMapInitialized() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        Map<String, Integer> values = new HashMap<>();
        values.put("key", 0);
        ds.setPerUserMaxTotal(values);
        assertEquals((Integer) 0, (Integer) ds.getPerUserMaxTotal("key"));
        values = new HashMap<>();
        values.put("anonymous", 0);
        ds.setPerUserMaxTotal(values);
        assertEquals((Integer) ds.getDefaultMaxTotal(), (Integer) ds.getPerUserMaxTotal("key"));
        assertEquals((Integer) 0, (Integer) ds.getPerUserMaxTotal("anonymous"));
    }

    @Test
    public void testPerUserMaxTotalMapNotInitialized() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        final Map<String, Integer> values = new HashMap<>();
        values.put("key", 1);
        ds.setPerUserMaxTotal(values);
        assertEquals((Integer) 1, (Integer) ds.getPerUserMaxTotal("key"));
    }

    @Test
    public void testPerUserMaxTotalMapNotInitializedMissingKey() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        final Map<String, Integer> values = new HashMap<>();
        values.put("key", 0);
        ds.setPerUserMaxTotal(values);
        assertEquals((Integer) ds.getDefaultMaxTotal(), (Integer) ds.getPerUserMaxTotal("missingkey"));
    }

    @Test
    public void testPerUserMaxTotalWithUserMapInitialized() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        ds.setPerUserMaxTotal(user, 0);
        assertEquals((Integer) 0, (Integer) ds.getPerUserMaxTotal(user));
        ds.setPerUserMaxTotal("anotheruser", 0);
        assertEquals((Integer) 0, (Integer) ds.getPerUserMaxTotal(user));
        assertEquals((Integer) 0, (Integer) ds.getPerUserMaxTotal("anotheruser"));
    }

    @Test
    public void testPerUserMaxTotalWithUserMapNotInitialized() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        ds.setPerUserMaxTotal(user, 0);
        assertEquals((Integer) 0, (Integer) ds.getPerUserMaxTotal(user));
    }

    @Test
    public void testPerUserMaxTotalWithUserMapNotInitializedMissingKey() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        ds.setPerUserMaxTotal("whatismyuseragain?", 0);
        assertEquals((Integer) ds.getDefaultMaxTotal(), (Integer) ds.getPerUserMaxTotal("missingkey"));
    }

    @Test
    public void testPerUserMaxWaitDurationMapInitialized() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        Map<String, Duration> values = new HashMap<>();
        values.put("key", Duration.ZERO);
        ds.setPerUserMaxWaitDuration(values);
        assertEquals(Duration.ZERO, ds.getPerUserMaxWaitDuration("key"));
        values = new HashMap<>();
        values.put("anonymous", Duration.ZERO);
        ds.setPerUserMaxWaitDuration(values);
        assertEquals(ds.getDefaultMaxWait(), ds.getPerUserMaxWaitDuration("key"));
        assertEquals(Duration.ZERO, ds.getPerUserMaxWaitDuration("anonymous"));
    }

    @Test
    public void testPerUserMaxWaitDurationMapNotInitialized() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        final Map<String, Duration> values = new HashMap<>();
        values.put("key", DURATION_1_MILLISECOND);
        ds.setPerUserMaxWaitDuration(values);
        assertEquals(DURATION_1_MILLISECOND, ds.getPerUserMaxWaitDuration("key"));
    }

    @Test
    public void testPerUserMaxWaitDurationMapNotInitializedMissingKey() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        final Map<String, Duration> values = new HashMap<>();
        values.put("key", Duration.ZERO);
        ds.setPerUserMaxWaitDuration(values);
        assertEquals(ds.getDefaultMaxWait(), ds.getPerUserMaxWaitDuration("missingkey"));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testPerUserMaxWaitMillisWithUserMapInitializedDeprecated() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        ds.setPerUserMaxWaitMillis(user, 0L);
        assertEquals(0L, ds.getPerUserMaxWaitMillis(user));
        ds.setPerUserMaxWaitMillis("anotheruser", 0L);
        assertEquals(0L, ds.getPerUserMaxWaitMillis(user));
        assertEquals(0L, ds.getPerUserMaxWaitMillis("anotheruser"));
    }

    // -- per user max wait millis

    @Test
    @SuppressWarnings("deprecation")
    public void testPerUserMaxWaitMillisWithUserMapNotInitializedDeprecated() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        ds.setPerUserMaxWaitMillis(user, 0L);
        assertEquals(0L, ds.getPerUserMaxWaitMillis(user));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testPerUserMaxWaitMillisWithUserMapNotInitializedMissingKeyDeprecated() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        ds.setPerUserMaxWaitMillis("whatismyuseragain?", 0L);
        assertEquals(ds.getDefaultMaxWaitMillis(), ds.getPerUserMaxWaitMillis("missingkey"));
    }

    @Test
    public void testPerUserMethods() throws Exception {
        final PerUserPoolDataSource tds = (PerUserPoolDataSource) ds;

        // you need to set per user maxTotal otherwise there is no accounting
        tds.setPerUserMaxTotal("u1", 5);
        tds.setPerUserMaxTotal("u2", 5);

        assertEquals(0, tds.getNumActive());
        assertEquals(0, tds.getNumActive("u1"));
        assertEquals(0, tds.getNumActive("u2"));
        assertEquals(0, tds.getNumIdle());
        assertEquals(0, tds.getNumIdle("u1"));
        assertEquals(0, tds.getNumIdle("u2"));

        try (Connection conn = tds.getConnection()) {
            assertNotNull(conn);
            assertEquals(1, tds.getNumActive());
            assertEquals(0, tds.getNumActive("u1"));
            assertEquals(0, tds.getNumActive("u2"));
            assertEquals(0, tds.getNumIdle());
            assertEquals(0, tds.getNumIdle("u1"));
            assertEquals(0, tds.getNumIdle("u2"));
        }
        assertEquals(0, tds.getNumActive());
        assertEquals(0, tds.getNumActive("u1"));
        assertEquals(0, tds.getNumActive("u2"));
        assertEquals(1, tds.getNumIdle());
        assertEquals(0, tds.getNumIdle("u1"));
        assertEquals(0, tds.getNumIdle("u2"));

        try (Connection conn = tds.getConnection("u1", "p1")) {
            assertNotNull(conn);
            assertEquals(0, tds.getNumActive());
            assertEquals(1, tds.getNumActive("u1"));
            assertEquals(0, tds.getNumActive("u2"));
            assertEquals(1, tds.getNumIdle());
            assertEquals(0, tds.getNumIdle("u1"));
            assertEquals(0, tds.getNumIdle("u2"));
        }

        assertEquals(0, tds.getNumActive());
        assertEquals(0, tds.getNumActive("u1"));
        assertEquals(0, tds.getNumActive("u2"));
        assertEquals(1, tds.getNumIdle());
        assertEquals(1, tds.getNumIdle("u1"));
        assertEquals(0, tds.getNumIdle("u2"));
    }

    @Test
    public void testPerUserMinEvictableIdleDurationMapInitialized() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        Map<String, Duration> values = new HashMap<>();
        values.put("key", Duration.ZERO);
        ds.setPerUserMinEvictableIdle(values);
        assertEquals(0L, ds.getPerUserMinEvictableIdleTimeMillis("key"));
        assertEquals(Duration.ZERO, ds.getPerUserMinEvictableIdleDuration("key"));
        values = new HashMap<>();
        values.put("anonymous", Duration.ZERO);
        ds.setPerUserMinEvictableIdle(values);
        assertEquals(ds.getDefaultMinEvictableIdleTimeMillis(), ds.getPerUserMinEvictableIdleTimeMillis("key"));
        assertEquals(ds.getDefaultMinEvictableIdleDuration(), ds.getPerUserMinEvictableIdleDuration("key"));
        assertEquals(0L, ds.getPerUserMinEvictableIdleTimeMillis("anonymous"));
        assertEquals(Duration.ZERO, ds.getPerUserMinEvictableIdleDuration("anonymous"));
    }

    @Test
    public void testPerUserMinEvictableIdleDurationMapNotInitialized() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        final Map<String, Duration> values = new HashMap<>();
        values.put("key", DURATION_1_MILLISECOND);
        ds.setPerUserMinEvictableIdle(values);
        assertEquals(1L, ds.getPerUserMinEvictableIdleTimeMillis("key"));
        assertEquals(DURATION_1_MILLISECOND, ds.getPerUserMinEvictableIdleDuration("key"));
    }

    @Test
    public void testPerUserMinEvictableIdleDurationMapNotInitializedMissingKey() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        final Map<String, Duration> values = new HashMap<>();
        values.put("key", Duration.ZERO);
        ds.setPerUserMinEvictableIdle(values);
        assertEquals(ds.getDefaultMinEvictableIdleTimeMillis(), ds.getPerUserMinEvictableIdleTimeMillis("missingkey"));
        assertEquals(ds.getDefaultMinEvictableIdleDuration(), ds.getPerUserMinEvictableIdleDuration("missingkey"));
    }

    // -- per user min evictable idle time millis

    @Test
    public void testPerUserMinEvictableIdleTimeMillisWithUserMapInitialized() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        ds.setPerUserMinEvictableIdleTimeMillis(user, 0L);
        assertEquals(0L, ds.getPerUserMinEvictableIdleTimeMillis(user));
        ds.setPerUserMinEvictableIdleTimeMillis("anotheruser", 0L);
        assertEquals(0L, ds.getPerUserMinEvictableIdleTimeMillis(user));
        assertEquals(0L, ds.getPerUserMinEvictableIdleTimeMillis("anotheruser"));
    }

    @Test
    public void testPerUserMinEvictableIdleTimeMillisWithUserMapNotInitialized() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        ds.setPerUserMinEvictableIdleTimeMillis(user, 0L);
        assertEquals(0L, ds.getPerUserMinEvictableIdleTimeMillis(user));
    }

    @Test
    public void testPerUserMinEvictableIdleTimeMillisWithUserMapNotInitializedMissingKey() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        ds.setPerUserMinEvictableIdleTimeMillis("whatismyuseragain?", 0L);
        assertEquals(ds.getDefaultMinEvictableIdleTimeMillis(), ds.getPerUserMinEvictableIdleTimeMillis("missingkey"));
    }

    @Test
    public void testPerUserMinIdleMapInitialized() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        Map<String, Integer> values = new HashMap<>();
        values.put("key", 0);
        ds.setPerUserMinIdle(values);
        assertEquals((Integer) 0, (Integer) ds.getPerUserMinIdle("key"));
        values = new HashMap<>();
        values.put("anonymous", 0);
        ds.setPerUserMinIdle(values);
        assertEquals((Integer) ds.getDefaultMinIdle(), (Integer) ds.getPerUserMinIdle("key"));
        assertEquals((Integer) 0, (Integer) ds.getPerUserMinIdle("anonymous"));
    }

    @Test
    public void testPerUserMinIdleMapNotInitialized() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        final Map<String, Integer> values = new HashMap<>();
        values.put("key", 1);
        ds.setPerUserMinIdle(values);
        assertEquals((Integer) 1, (Integer) ds.getPerUserMinIdle("key"));
    }

    @Test
    public void testPerUserMinIdleMapNotInitializedMissingKey() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        final Map<String, Integer> values = new HashMap<>();
        values.put("key", 0);
        ds.setPerUserMinIdle(values);
        assertEquals((Integer) ds.getDefaultMinIdle(), (Integer) ds.getPerUserMinIdle("missingkey"));
    }

    // -- per user min idle

    @Test
    public void testPerUserMinIdleWithUserMapInitialized() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        ds.setPerUserMinIdle(user, 0);
        assertEquals((Integer) 0, (Integer) ds.getPerUserMinIdle(user));
        ds.setPerUserMinIdle("anotheruser", 0);
        assertEquals((Integer) 0, (Integer) ds.getPerUserMinIdle(user));
        assertEquals((Integer) 0, (Integer) ds.getPerUserMinIdle("anotheruser"));
    }

    @Test
    public void testPerUserMinIdleWithUserMapNotInitialized() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        ds.setPerUserMinIdle(user, 0);
        assertEquals((Integer) 0, (Integer) ds.getPerUserMinIdle(user));
    }

    @Test
    public void testPerUserMinIdleWithUserMapNotInitializedMissingKey() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        ds.setPerUserMinIdle("whatismyuseragain?", 0);
        assertEquals((Integer) ds.getDefaultMinIdle(), (Integer) ds.getPerUserMinIdle("missingkey"));
    }

    @Test
    public void testPerUserNumTestsPerEvictionRunMapInitialized() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        Map<String, Integer> values = new HashMap<>();
        values.put("key", 0);
        ds.setPerUserNumTestsPerEvictionRun(values);
        assertEquals((Integer) 0, (Integer) ds.getPerUserNumTestsPerEvictionRun("key"));
        values = new HashMap<>();
        values.put("anonymous", 0);
        ds.setPerUserNumTestsPerEvictionRun(values);
        assertEquals((Integer) ds.getDefaultNumTestsPerEvictionRun(), (Integer) ds.getPerUserNumTestsPerEvictionRun("key"));
        assertEquals((Integer) 0, (Integer) ds.getPerUserNumTestsPerEvictionRun("anonymous"));
    }

    @Test
    public void testPerUserNumTestsPerEvictionRunMapNotInitialized() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        final Map<String, Integer> values = new HashMap<>();
        values.put("key", 1);
        ds.setPerUserNumTestsPerEvictionRun(values);
        assertEquals((Integer) 1, (Integer) ds.getPerUserNumTestsPerEvictionRun("key"));
    }

    @Test
    public void testPerUserNumTestsPerEvictionRunMapNotInitializedMissingKey() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        final Map<String, Integer> values = new HashMap<>();
        values.put("key", 0);
        ds.setPerUserNumTestsPerEvictionRun(values);
        assertEquals((Integer) ds.getDefaultNumTestsPerEvictionRun(), (Integer) ds.getPerUserNumTestsPerEvictionRun("missingkey"));
    }

    // -- per user num tests per eviction run

    @Test
    public void testPerUserNumTestsPerEvictionRunWithUserMapInitialized() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        ds.setPerUserNumTestsPerEvictionRun(user, 0);
        assertEquals((Integer) 0, (Integer) ds.getPerUserNumTestsPerEvictionRun(user));
        ds.setPerUserNumTestsPerEvictionRun("anotheruser", 0);
        assertEquals((Integer) 0, (Integer) ds.getPerUserNumTestsPerEvictionRun(user));
        assertEquals((Integer) 0, (Integer) ds.getPerUserNumTestsPerEvictionRun("anotheruser"));
    }

    @Test
    public void testPerUserNumTestsPerEvictionRunWithUserMapNotInitialized() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        ds.setPerUserNumTestsPerEvictionRun(user, 0);
        assertEquals((Integer) 0, (Integer) ds.getPerUserNumTestsPerEvictionRun(user));
    }

    @Test
    public void testPerUserNumTestsPerEvictionRunWithUserMapNotInitializedMissingKey() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        ds.setPerUserNumTestsPerEvictionRun("whatismyuseragain?", 0);
        assertEquals((Integer) ds.getDefaultNumTestsPerEvictionRun(), (Integer) ds.getPerUserNumTestsPerEvictionRun("missingkey"));
    }

    @Test
    public void testPerUserSoftMinEvictableIdleDurationMapInitialized() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        Map<String, Duration> values = new HashMap<>();
        values.put("key", Duration.ZERO);
        ds.setPerUserSoftMinEvictableIdle(values);
        assertEquals(0L, ds.getPerUserSoftMinEvictableIdleTimeMillis("key"));
        assertEquals(Duration.ZERO, ds.getPerUserSoftMinEvictableIdleDuration("key"));
        values = new HashMap<>();
        values.put("anonymous", Duration.ZERO);
        ds.setPerUserSoftMinEvictableIdle(values);
        assertEquals(ds.getDefaultSoftMinEvictableIdleTimeMillis(), ds.getPerUserSoftMinEvictableIdleTimeMillis("key"));
        assertEquals(ds.getDefaultSoftMinEvictableIdleDuration(), ds.getPerUserSoftMinEvictableIdleDuration("key"));
        assertEquals(0L, ds.getPerUserSoftMinEvictableIdleTimeMillis("anonymous"));
        assertEquals(Duration.ZERO, ds.getPerUserSoftMinEvictableIdleDuration("anonymous"));
    }

    @Test
    public void testPerUserSoftMinEvictableIdleDurationMapNotInitialized() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        final Map<String, Duration> values = new HashMap<>();
        values.put("key", DURATION_1_MILLISECOND);
        ds.setPerUserSoftMinEvictableIdle(values);
        assertEquals(1L, ds.getPerUserSoftMinEvictableIdleTimeMillis("key"));
        assertEquals(DURATION_1_MILLISECOND, ds.getPerUserSoftMinEvictableIdleDuration("key"));
    }

    @Test
    public void testPerUserSoftMinEvictableIdleDurationMapNotInitializedMissingKey() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        final Map<String, Duration> values = new HashMap<>();
        values.put("key", Duration.ZERO);
        ds.setPerUserSoftMinEvictableIdle(values);
        assertEquals(ds.getDefaultSoftMinEvictableIdleTimeMillis(), ds.getPerUserSoftMinEvictableIdleTimeMillis("missingkey"));
        assertEquals(ds.getDefaultSoftMinEvictableIdleDuration(), ds.getPerUserSoftMinEvictableIdleDuration("missingkey"));
    }

    // -- per user soft min evictable idle time millis

    @Test
    public void testPerUserSoftMinEvictableIdleTimeMillisWithUserMapInitialized() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        ds.setPerUserSoftMinEvictableIdleTimeMillis(user, 0L);
        assertEquals(0L, ds.getPerUserSoftMinEvictableIdleTimeMillis(user));
        ds.setPerUserSoftMinEvictableIdleTimeMillis("anotheruser", 0L);
        assertEquals(0L, ds.getPerUserSoftMinEvictableIdleTimeMillis(user));
        assertEquals(0L, ds.getPerUserSoftMinEvictableIdleTimeMillis("anotheruser"));
    }

    @Test
    public void testPerUserSoftMinEvictableIdleTimeMillisWithUserMapNotInitialized() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        ds.setPerUserSoftMinEvictableIdleTimeMillis(user, 0L);
        assertEquals(0L, ds.getPerUserSoftMinEvictableIdleTimeMillis(user));
    }

    @Test
    public void testPerUserSoftMinEvictableIdleTimeMillisWithUserMapNotInitializedMissingKey() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        ds.setPerUserSoftMinEvictableIdleTimeMillis("whatismyuseragain?", 0L);
        assertEquals(ds.getDefaultSoftMinEvictableIdleTimeMillis(), ds.getPerUserSoftMinEvictableIdleTimeMillis("missingkey"));
    }

    @Test
    public void testPerUserTestOnBorrowMapInitialized() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        Map<String, Boolean> values = new HashMap<>();
        values.put("key", Boolean.FALSE);
        ds.setPerUserTestOnBorrow(values);
        assertEquals(Boolean.FALSE, ds.getPerUserTestOnBorrow("key"));
        values = new HashMap<>();
        values.put("anonymous", Boolean.FALSE);
        ds.setPerUserTestOnBorrow(values);
        assertEquals(ds.getDefaultTestOnBorrow(), ds.getPerUserTestOnBorrow("key"));
        assertEquals(Boolean.FALSE, ds.getPerUserTestOnBorrow("anonymous"));
    }

    @Test
    public void testPerUserTestOnBorrowMapNotInitialized() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        final Map<String, Boolean> values = new HashMap<>();
        values.put("key", Boolean.TRUE);
        ds.setPerUserTestOnBorrow(values);
        assertEquals(Boolean.TRUE, ds.getPerUserTestOnBorrow("key"));
    }

    @Test
    public void testPerUserTestOnBorrowMapNotInitializedMissingKey() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        final Map<String, Boolean> values = new HashMap<>();
        values.put("key", Boolean.FALSE);
        ds.setPerUserTestOnBorrow(values);
        assertEquals(ds.getDefaultTestOnBorrow(), ds.getPerUserTestOnBorrow("missingkey"));
    }

    // -- per user test on borrow

    @Test
    public void testPerUserTestOnBorrowWithUserMapInitialized() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        ds.setPerUserTestOnBorrow(user, Boolean.FALSE);
        assertEquals(Boolean.FALSE, ds.getPerUserTestOnBorrow(user));
        ds.setPerUserTestOnBorrow("anotheruser", Boolean.FALSE);
        assertEquals(Boolean.FALSE, ds.getPerUserTestOnBorrow(user));
        assertEquals(Boolean.FALSE, ds.getPerUserTestOnBorrow("anotheruser"));
    }

    @Test
    public void testPerUserTestOnBorrowWithUserMapNotInitialized() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        ds.setPerUserTestOnBorrow(user, Boolean.FALSE);
        assertEquals(Boolean.FALSE, ds.getPerUserTestOnBorrow(user));
    }

    @Test
    public void testPerUserTestOnBorrowWithUserMapNotInitializedMissingKey() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        ds.setPerUserTestOnBorrow("whatismyuseragain?", Boolean.FALSE);
        assertEquals(ds.getDefaultTestOnBorrow(), ds.getPerUserTestOnBorrow("missingkey"));
    }

    @Test
    public void testPerUserTestOnCreateMapInitialized() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        Map<String, Boolean> values = new HashMap<>();
        values.put("key", Boolean.FALSE);
        ds.setPerUserTestOnCreate(values);
        assertEquals(Boolean.FALSE, ds.getPerUserTestOnCreate("key"));
        values = new HashMap<>();
        values.put("anonymous", Boolean.FALSE);
        ds.setPerUserTestOnCreate(values);
        assertEquals(ds.getDefaultTestOnCreate(), ds.getPerUserTestOnCreate("key"));
        assertEquals(Boolean.FALSE, ds.getPerUserTestOnCreate("anonymous"));
    }

    @Test
    public void testPerUserTestOnCreateMapNotInitialized() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        final Map<String, Boolean> values = new HashMap<>();
        values.put("key", Boolean.TRUE);
        ds.setPerUserTestOnCreate(values);
        assertEquals(Boolean.TRUE, ds.getPerUserTestOnCreate("key"));
    }

    @Test
    public void testPerUserTestOnCreateMapNotInitializedMissingKey() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        final Map<String, Boolean> values = new HashMap<>();
        values.put("key", Boolean.FALSE);
        ds.setPerUserTestOnCreate(values);
        assertEquals(ds.getDefaultTestOnCreate(), ds.getPerUserTestOnCreate("missingkey"));
    }

    // -- per user test on create

    @Test
    public void testPerUserTestOnCreateWithUserMapInitialized() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        ds.setPerUserTestOnCreate(user, Boolean.FALSE);
        assertEquals(Boolean.FALSE, ds.getPerUserTestOnCreate(user));
        ds.setPerUserTestOnCreate("anotheruser", Boolean.FALSE);
        assertEquals(Boolean.FALSE, ds.getPerUserTestOnCreate(user));
        assertEquals(Boolean.FALSE, ds.getPerUserTestOnCreate("anotheruser"));
    }

    @Test
    public void testPerUserTestOnCreateWithUserMapNotInitialized() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        ds.setPerUserTestOnCreate(user, Boolean.FALSE);
        assertEquals(Boolean.FALSE, ds.getPerUserTestOnCreate(user));
    }

    @Test
    public void testPerUserTestOnCreateWithUserMapNotInitializedMissingKey() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        ds.setPerUserTestOnCreate("whatismyuseragain?", Boolean.FALSE);
        assertEquals(ds.getDefaultTestOnCreate(), ds.getPerUserTestOnCreate("missingkey"));
    }

    @Test
    public void testPerUserTestOnReturnMapInitialized() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        Map<String, Boolean> values = new HashMap<>();
        values.put("key", Boolean.FALSE);
        ds.setPerUserTestOnReturn(values);
        assertEquals(Boolean.FALSE, ds.getPerUserTestOnReturn("key"));
        values = new HashMap<>();
        values.put("anonymous", Boolean.FALSE);
        ds.setPerUserTestOnReturn(values);
        assertEquals(ds.getDefaultTestOnReturn(), ds.getPerUserTestOnReturn("key"));
        assertEquals(Boolean.FALSE, ds.getPerUserTestOnReturn("anonymous"));
    }

    @Test
    public void testPerUserTestOnReturnMapNotInitialized() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        final Map<String, Boolean> values = new HashMap<>();
        values.put("key", Boolean.TRUE);
        ds.setPerUserTestOnReturn(values);
        assertEquals(Boolean.TRUE, ds.getPerUserTestOnReturn("key"));
    }

    @Test
    public void testPerUserTestOnReturnMapNotInitializedMissingKey() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        final Map<String, Boolean> values = new HashMap<>();
        values.put("key", Boolean.FALSE);
        ds.setPerUserTestOnReturn(values);
        assertEquals(ds.getDefaultTestOnReturn(), ds.getPerUserTestOnReturn("missingkey"));
    }

    // -- per user test on return

    @Test
    public void testPerUserTestOnReturnWithUserMapInitialized() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        ds.setPerUserTestOnReturn(user, Boolean.FALSE);
        assertEquals(Boolean.FALSE, ds.getPerUserTestOnReturn(user));
        ds.setPerUserTestOnReturn("anotheruser", Boolean.FALSE);
        assertEquals(Boolean.FALSE, ds.getPerUserTestOnReturn(user));
        assertEquals(Boolean.FALSE, ds.getPerUserTestOnReturn("anotheruser"));
    }

    @Test
    public void testPerUserTestOnReturnWithUserMapNotInitialized() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        ds.setPerUserTestOnReturn(user, Boolean.FALSE);
        assertEquals(Boolean.FALSE, ds.getPerUserTestOnReturn(user));
    }

    @Test
    public void testPerUserTestOnReturnWithUserMapNotInitializedMissingKey() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        ds.setPerUserTestOnReturn("whatismyuseragain?", Boolean.FALSE);
        assertEquals(ds.getDefaultTestOnReturn(), ds.getPerUserTestOnReturn("missingkey"));
    }

    @Test
    public void testPerUserTestWhileIdleMapInitialized() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        Map<String, Boolean> values = new HashMap<>();
        values.put("key", Boolean.FALSE);
        ds.setPerUserTestWhileIdle(values);
        assertEquals(Boolean.FALSE, ds.getPerUserTestWhileIdle("key"));
        values = new HashMap<>();
        values.put("anonymous", Boolean.FALSE);
        ds.setPerUserTestWhileIdle(values);
        assertEquals(ds.getDefaultTestWhileIdle(), ds.getPerUserTestWhileIdle("key"));
        assertEquals(Boolean.FALSE, ds.getPerUserTestWhileIdle("anonymous"));
    }

    @Test
    public void testPerUserTestWhileIdleMapNotInitialized() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        final Map<String, Boolean> values = new HashMap<>();
        values.put("key", Boolean.TRUE);
        ds.setPerUserTestWhileIdle(values);
        assertEquals(Boolean.TRUE, ds.getPerUserTestWhileIdle("key"));
    }

    @Test
    public void testPerUserTestWhileIdleMapNotInitializedMissingKey() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        final Map<String, Boolean> values = new HashMap<>();
        values.put("key", Boolean.FALSE);
        ds.setPerUserTestWhileIdle(values);
        assertEquals(ds.getDefaultTestWhileIdle(), ds.getPerUserTestWhileIdle("missingkey"));
    }

    @Test
    public void testPerUserTestWhileIdleWithUserMapInitialized() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        ds.setPerUserTestWhileIdle(user, Boolean.FALSE);
        assertEquals(Boolean.FALSE, ds.getPerUserTestWhileIdle(user));
        ds.setPerUserTestWhileIdle("anotheruser", Boolean.FALSE);
        assertEquals(Boolean.FALSE, ds.getPerUserTestWhileIdle(user));
        assertEquals(Boolean.FALSE, ds.getPerUserTestWhileIdle("anotheruser"));
    }

    @Test
    public void testPerUserTestWhileIdleWithUserMapNotInitialized() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        ds.setPerUserTestWhileIdle(user, Boolean.FALSE);
        assertEquals(Boolean.FALSE, ds.getPerUserTestWhileIdle(user));
    }

    @Test
    public void testPerUserTestWhileIdleWithUserMapNotInitializedMissingKey() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        ds.setPerUserTestWhileIdle("whatismyuseragain?", Boolean.FALSE);
        assertEquals(ds.getDefaultTestWhileIdle(), ds.getPerUserTestWhileIdle("missingkey"));
    }

    @Test
    public void testPerUserTimeBetweenEvictionRunsMillisWithUserMapInitialized() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        ds.setPerUserTimeBetweenEvictionRunsMillis(user, 0L);
        assertEquals(0L, ds.getPerUserTimeBetweenEvictionRunsMillis(user));
        ds.setPerUserTimeBetweenEvictionRunsMillis("anotheruser", 0L);
        assertEquals(0L, ds.getPerUserTimeBetweenEvictionRunsMillis(user));
        assertEquals(0L, ds.getPerUserTimeBetweenEvictionRunsMillis("anotheruser"));
    }

    @Test
    public void testPerUserTimeBetweenEvictionRunsMillisWithUserMapNotInitialized() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        ds.setPerUserTimeBetweenEvictionRunsMillis(user, 0L);
        assertEquals(0L, ds.getPerUserTimeBetweenEvictionRunsMillis(user));
    }

    @Test
    public void testPerUserTimeBetweenEvictionRunsMillisWithUserMapNotInitializedMissingKey() {
        final PerUserPoolDataSource ds = (PerUserPoolDataSource) this.ds;
        ds.setPerUserTimeBetweenEvictionRunsMillis("whatismyuseragain?", 0L);
        assertEquals(ds.getDefaultTimeBetweenEvictionRunsMillis(), ds.getPerUserTimeBetweenEvictionRunsMillis("missingkey"));
    }

    // -- per user time between eviction runs millis

    @Test
    public void testSerialization() throws Exception {
        // make sure the pool has initialized
        final Connection conn = ds.getConnection();
        conn.close();

        // serialize
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final ObjectOutputStream out = new ObjectOutputStream(baos);
        out.writeObject(ds);
        out.close();
        final byte[] b = baos.toByteArray();

        final ByteArrayInputStream bais = new ByteArrayInputStream(b);
        final ObjectInputStream in = new ObjectInputStream(bais);
        final Object obj = in.readObject();
        in.close();

        assertEquals(1, ((PerUserPoolDataSource) obj).getNumIdle());
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
    public void testSimple2() throws Exception {
        Connection conn = ds.getConnection();
        assertNotNull(conn);

        PreparedStatement stmt = conn.prepareStatement("select * from dual");
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
        assertThrows(SQLException.class, conn::createStatement, "Can't use closed connections");

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
        try (Connection conn = getConnection()) {
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

    // see issue https://issues.apache.org/bugzilla/show_bug.cgi?id=23843
    // unregistered user is in the same pool as without user name
    @Test
    public void testUnregisteredUser() throws Exception {
        final PerUserPoolDataSource tds = (PerUserPoolDataSource) ds;

        assertEquals(0, tds.getNumActive());
        assertEquals(0, tds.getNumIdle());

        try (Connection conn = tds.getConnection()) {
            assertNotNull(conn);
            assertEquals(1, tds.getNumActive());
            assertEquals(0, tds.getNumIdle());
        }
        assertEquals(0, tds.getNumActive());
        assertEquals(1, tds.getNumIdle());

        try (Connection conn = tds.getConnection("u1", "p1")) {
            assertNotNull(conn);
            assertEquals(0, tds.getNumActive());
            assertEquals(1, tds.getNumIdle());
            assertEquals(1, tds.getNumActive("u1"));
            assertEquals(0, tds.getNumIdle("u1"));
        }
        assertEquals(0, tds.getNumActive());
        assertEquals(1, tds.getNumIdle());
        assertEquals(0, tds.getNumActive("u1"));
        assertEquals(1, tds.getNumIdle("u1"));
    }

}
