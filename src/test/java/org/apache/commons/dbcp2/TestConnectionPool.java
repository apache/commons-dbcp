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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
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
import java.util.Hashtable;
import java.util.Random;
import java.util.Stack;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

// XXX FIX ME XXX
// this class still needs some cleanup, but at least
// this consolidates most of the relevant test code
// in a fairly re-usable fashion
// XXX FIX ME XXX

/**
 * Base test suite for DBCP pools.
 */
public abstract class TestConnectionPool {

    private static final Duration MAX_WAIT_DURATION = Duration.ofMillis(100);

    protected class PoolTest implements Runnable {
        /**
         * The number of milliseconds to hold onto a database connection
         */
        private final Duration connHoldDuration;

        private final int numStatements;

        private volatile boolean isRun;

        private String state; // No need to be volatile if it is read after the thread finishes

        private final Thread thread;

        private Throwable thrown;

        private final Random random = new Random();

        // Debug for DBCP-318
        private final long createdMillis; // When object was created
        private long started; // when thread started
        private long ended; // when thread ended
        private long preconnected; // just before connect
        private long connected; // when thread last connected
        private long postconnected; // when thread released connection
        private int loops;
        private int connHash; // Connection identity hashCode (to see which one is reused)

        private final boolean stopOnException; // If true, don't rethrow Exception

        private final boolean loopOnce; // If true, don't repeat loop

        public PoolTest(final ThreadGroup threadGroup, final Duration connHoldDuration, final boolean isStopOnException) {
            this(threadGroup, connHoldDuration, isStopOnException, false, 1);
        }

        private PoolTest(final ThreadGroup threadGroup, final Duration connHoldDuration, final boolean isStopOnException, final boolean once, final int numStatements) {
            this.loopOnce = once;
            this.connHoldDuration = connHoldDuration;
            stopOnException = isStopOnException;
            isRun = true; // Must be done here so main thread is guaranteed to be able to set it false
            thrown = null;
            thread =
                new Thread(threadGroup, this, "Thread+" + currentThreadCount++);
            thread.setDaemon(false);
            createdMillis = timeStampMillis();
            this.numStatements = numStatements;
        }

        public PoolTest(final ThreadGroup threadGroup, final Duration connHoldDuration, final boolean isStopOnException, final int numStatements) {
            this(threadGroup, connHoldDuration, isStopOnException, false, numStatements);
        }

        public Thread getThread() {
            return thread;
        }

        @Override
        public void run() {
            started = timeStampMillis();
            try {
                while (isRun) {
                    loops++;
                    state = "Getting Connection";
                    preconnected = timeStampMillis();
                    try (Connection conn = getConnection()) {
                        connHash = System.identityHashCode(((DelegatingConnection<?>) conn).getInnermostDelegate());
                        connected = timeStampMillis();
                        state = "Using Connection";
                        assertNotNull(conn);
                        final String sql = numStatements == 1 ? "select * from dual" : "select count " + random.nextInt(numStatements - 1);
                        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                            assertNotNull(stmt);
                            try (ResultSet rset = stmt.executeQuery()) {
                                assertNotNull(rset);
                                assertTrue(rset.next());
                                state = "Holding Connection";
                                Thread.sleep(connHoldDuration.toMillis());
                                state = "Closing ResultSet";
                            }
                            state = "Closing Statement";
                        }
                        state = "Closing Connection";
                    }
                    postconnected = timeStampMillis();
                    state = "Closed";
                    if (loopOnce) {
                        break; // Or could set isRun=false
                    }
                }
                state = DONE;
            } catch (final Throwable t) {
                thrown = t;
                if (!stopOnException) {
                    throw new RuntimeException();
                }
            } finally {
                ended = timeStampMillis();
            }
        }

        public void start(){
            thread.start();
        }

        public void stop() {
            isRun = false;
        }
    }

    class TestThread implements Runnable {
        final java.util.Random _random = new java.util.Random();
        boolean _complete;
        boolean _failed;
        int _iter = 100;
        int _delay = 50;

        public TestThread() {
        }

        public TestThread(final int iter) {
            _iter = iter;
        }

        public TestThread(final int iter, final int delay) {
            _iter = iter;
            _delay = delay;
        }

        public boolean complete() {
            return _complete;
        }

        public boolean failed() {
            return _failed;
        }

        @Override
        public void run() {
            for (int i = 0; i < _iter; i++) {
                try {
                    Thread.sleep(_random.nextInt(_delay));
                } catch (final Exception e) {
                    // ignored
                }
                try (Connection conn = newConnection();
                        PreparedStatement stmt = conn.prepareStatement("select 'literal', SYSDATE from dual");
                        ResultSet rset = stmt.executeQuery()) {
                    try {
                        Thread.sleep(_random.nextInt(_delay));
                    } catch (final Exception ignore) {
                        // ignored
                    }
                } catch (final Exception e) {
                    e.printStackTrace();
                    _failed = true;
                    _complete = true;
                    break;
                }
            }
            _complete = true;
        }
    }

    private static final boolean DISPLAY_THREAD_DETAILS=
            Boolean.parseBoolean(System.getProperty("TestConnectionPool.display.thread.details", "false"));
    // To pass this to a Maven test, use:
    // mvn test -DargLine="-DTestConnectionPool.display.thread.details=true"
    // @see https://issues.apache.org/jira/browse/SUREFIRE-121

    private static int currentThreadCount;

    private static final String DONE = "Done";

    /** Connections opened during the course of a test */
    protected final Stack<Connection> connectionStack = new Stack<>();

    // ----------- Utility Methods ---------------------------------

    protected void assertBackPointers(final Connection conn, final Statement statement) throws SQLException {
        assertFalse(conn.isClosed());
        assertFalse(isClosed(statement));

        assertSame(conn, statement.getConnection(),
                "statement.getConnection() should return the exact same connection instance that was used to create the statement");

        final ResultSet resultSet = statement.getResultSet();
        assertFalse(isClosed(resultSet));
        assertSame(statement, resultSet.getStatement(),
                "resultSet.getStatement() should return the exact same statement instance that was used to create the result set");

        final ResultSet executeResultSet = statement.executeQuery("select * from dual");
        assertFalse(isClosed(executeResultSet));
        assertSame(statement, executeResultSet.getStatement(),
                "resultSet.getStatement() should return the exact same statement instance that was used to create the result set");

        final ResultSet keysResultSet = statement.getGeneratedKeys();
        assertFalse(isClosed(keysResultSet));
        assertSame(statement, keysResultSet.getStatement(),
                "resultSet.getStatement() should return the exact same statement instance that was used to create the result set");

        ResultSet preparedResultSet = null;
        if (statement instanceof PreparedStatement) {
            final PreparedStatement preparedStatement = (PreparedStatement) statement;
            preparedResultSet = preparedStatement.executeQuery();
            assertFalse(isClosed(preparedResultSet));
            assertSame(statement, preparedResultSet.getStatement(),
                    "resultSet.getStatement() should return the exact same statement instance that was used to create the result set");
        }


        resultSet.getStatement().getConnection().close();
        assertTrue(conn.isClosed());
        assertTrue(isClosed(statement));
        assertTrue(isClosed(resultSet));
        assertTrue(isClosed(executeResultSet));
        assertTrue(isClosed(keysResultSet));
        if (preparedResultSet != null) {
            assertTrue(isClosed(preparedResultSet));
        }
    }

    protected abstract Connection getConnection() throws Exception;

    protected int getMaxTotal() {
        return 10;
    }

    protected Duration getMaxWaitDuration() {
        return MAX_WAIT_DURATION;
    }

    protected String getUsername(final Connection conn) throws SQLException {
        try (final Statement stmt = conn.createStatement(); final ResultSet rs = stmt.executeQuery("select username")) {
            if (rs.next()) {
                return rs.getString(1);
            }
        }
        return null;
    }

    protected boolean isClosed(final ResultSet resultSet) {
        try {
            resultSet.getWarnings();
            return false;
        } catch (final SQLException e) {
            // getWarnings throws an exception if the statement is
            // closed, but could throw an exception for other reasons
            // in this case it is good enough to assume the result set
            // is closed
            return true;
        }
    }

    protected boolean isClosed(final Statement statement) {
        try {
            statement.getWarnings();
            return false;
        } catch (final SQLException e) {
            // getWarnings throws an exception if the statement is
            // closed, but could throw an exception for other reasons
            // in this case it is good enough to assume the statement
            // is closed
            return true;
        }
    }

    /**
     * Launches a group of 2 * getMaxTotal() threads, each of which will attempt to obtain a connection
     * from the pool, hold it for {@code holdTime} ms, and then return it to the pool.  If {@code loopOnce} is false,
     * threads will continue this process indefinitely.  If {@code expectError} is true, exactly 1/2 of the
     * threads are expected to either throw exceptions or fail to complete. If {@code expectError} is false,
     * all threads are expected to complete successfully.
     *
     * @param holdDuration Duration that a thread holds a connection before returning it to the pool
     * @param expectError whether or not an error is expected
     * @param loopOnce whether threads should complete the borrow - hold - return cycle only once, or loop indefinitely
     * @param maxWaitDuration passed in by client - has no impact on the test itself, but does get reported
     *
     * @throws Exception
     */
    protected void multipleThreads(final Duration holdDuration,
        final boolean expectError, final boolean loopOnce,
        final Duration maxWaitDuration) throws Exception {
        multipleThreads(holdDuration, expectError, loopOnce, maxWaitDuration, 1, 2 * getMaxTotal(), 300);
    }

    /**
     * Launches a group of {@code numThreads} threads, each of which will attempt to obtain a connection
     * from the pool, hold it for {@code holdTime} ms, and then return it to the pool.  If {@code loopOnce} is false,
     * threads will continue this process indefinitely.  If {@code expectError} is true, exactly 1/2 of the
     * threads are expected to either throw exceptions or fail to complete. If {@code expectError} is false,
     * all threads are expected to complete successfully.  Threads are stopped after {@code duration} ms.
     *
     * @param holdDuration Duration that a thread holds a connection before returning it to the pool
     * @param expectError whether or not an error is expected
     * @param loopOnce whether threads should complete the borrow - hold - return cycle only once, or loop indefinitely
     * @param maxWaitDuration passed in by client - has no impact on the test itself, but does get reported
     * @param numThreads the number of threads
     * @param duration duration in ms of test
     *
     * @throws Exception
     */
    protected void multipleThreads(final Duration holdDuration,
            final boolean expectError, final boolean loopOnce,
        final Duration maxWaitDuration, final int numStatements, final int numThreads, final long duration) throws Exception {
        final long startTimeMillis = timeStampMillis();
        final PoolTest[] pts = new PoolTest[numThreads];
        // Catch Exception so we can stop all threads if one fails
        final ThreadGroup threadGroup = new ThreadGroup("foo") {
            @Override
            public void uncaughtException(final Thread t, final Throwable e) {
                for (final PoolTest pt : pts) {
                    pt.stop();
                }
            }
        };
        // Create all the threads
        for (int i = 0; i < pts.length; i++) {
            pts[i] = new PoolTest(threadGroup, holdDuration, expectError, loopOnce, numStatements);
        }
        // Start all the threads
        for (final PoolTest pt : pts) {
            pt.start();
        }

        // Give all threads a chance to start and succeed
        Thread.sleep(duration);

        // Stop threads
        for (final PoolTest pt : pts) {
            pt.stop();
        }

        /*
         * Wait for all threads to terminate. This is essential to ensure that all threads have a chance to update success[0]
         * and to ensure that the variable is published correctly.
         */
        int done = 0;
        int failed = 0;
        int didNotRun = 0;
        int loops = 0;
        for (final PoolTest poolTest : pts) {
            poolTest.thread.join();
            loops += poolTest.loops;
            final String state = poolTest.state;
            if (DONE.equals(state)) {
                done++;
            }
            if (poolTest.loops == 0) {
                didNotRun++;
            }
            final Throwable thrown = poolTest.thrown;
            if (thrown != null) {
                failed++;
                if (!expectError || !(thrown instanceof SQLException)) {
                    System.err.println("Unexpected error: " + thrown.getMessage());
                }
            }
        }

        final long timeMillis = timeStampMillis() - startTimeMillis;
        // @formatter:off
        println("Multithread test time = " + timeMillis
            + " ms. Threads: " + pts.length
            + ". Loops: " + loops
            + ". Hold time: " + holdDuration
            + ". maxWaitMillis: " + maxWaitDuration
            + ". Done: " + done
            + ". Did not run: " + didNotRun
            + ". Failed: " + failed
            + ". expectError: " + expectError);
        // @formatter:on
        if (expectError) {
            if (DISPLAY_THREAD_DETAILS || pts.length / 2 != failed) {
                final long offset = pts[0].createdMillis - 1000; // To reduce size of output numbers, but ensure they have 4 digits
                println("Offset: " + offset);
                for (int i = 0; i < pts.length; i++) {
                    final PoolTest pt = pts[i];
                    // @formatter:off
                    println("Pre: " + (pt.preconnected-offset) // First, so can sort on this easily
                        + ". Post: " + (pt.postconnected != 0 ? Long.toString(pt.postconnected-offset): "-")
                        + ". Hash: " + pt.connHash
                        + ". Startup: " + (pt.started-pt.createdMillis)
                        + ". getConn(): " + (pt.connected != 0 ? Long.toString(pt.connected-pt.preconnected) : "-")
                        + ". Runtime: " + (pt.ended-pt.started)
                        + ". IDX: " + i
                        + ". Loops: " + pt.loops
                        + ". State: " + pt.state
                        + ". thrown: "+ pt.thrown
                        + ".");
                    // @formatter:on
                }
            }
            if (didNotRun > 0) {
                println("NOTE: some threads did not run the code: " + didNotRun);
            }
            // Perform initial sanity check:
            assertTrue(failed > 0, "Expected some of the threads to fail");
            // Assume that threads that did not run would have timed out.
            assertEquals(pts.length / 2, failed + didNotRun, "WARNING: Expected half the threads to fail");
        } else {
            assertEquals(0, failed, "Did not expect any threads to fail");
        }
    }

    /** 
     * Acquires a new connection and push it onto the connections stack.
     * 
     * @return a new connection.
     * @throws Exception Defined in subclasses.
     */
    @SuppressWarnings("resource") // Caller closes
    protected Connection newConnection() throws Exception {
        return connectionStack.push(getConnection());
    }

    void println(final String string) {
        if (Boolean.getBoolean(getClass().getSimpleName() + ".debug")) {
            System.out.println(string);
        }
    }

    @AfterEach
    public void tearDown() throws Exception {
        // Close any connections opened by the test
        while (!connectionStack.isEmpty()) {
            Utils.closeQuietly((AutoCloseable) connectionStack.pop());
        }
    }

    @Test
    public void testAutoCommitBehavior() throws Exception {
        final Connection conn0 = newConnection();
        assertNotNull(conn0, "connection should not be null");
        assertTrue(conn0.getAutoCommit(), "autocommit should be true for conn0");

        final Connection conn1 = newConnection();
        assertTrue(conn1.getAutoCommit(), "autocommit should be true for conn1");
        conn1.close();

        assertTrue(conn0.getAutoCommit(), "autocommit should be true for conn0");
        conn0.setAutoCommit(false);
        assertFalse(conn0.getAutoCommit(), "autocommit should be false for conn0");
        conn0.close();

        final Connection conn2 = newConnection();
        assertTrue(conn2.getAutoCommit(), "autocommit should be true for conn2");

        final Connection conn3 = newConnection();
        assertTrue(conn3.getAutoCommit(), "autocommit should be true for conn3");

        conn2.close();

        conn3.close();
    }

    @Test
    public void testBackPointers() throws Exception {
        // normal statement
        Connection conn = newConnection();
        assertBackPointers(conn, conn.createStatement());
        conn = newConnection();
        assertBackPointers(conn, conn.createStatement(0, 0));
        conn = newConnection();
        assertBackPointers(conn, conn.createStatement(0, 0, 0));

        // prepared statement
        conn = newConnection();
        assertBackPointers(conn, conn.prepareStatement("select * from dual"));
        conn = newConnection();
        assertBackPointers(conn, conn.prepareStatement("select * from dual", 0));
        conn = newConnection();
        assertBackPointers(conn, conn.prepareStatement("select * from dual", 0, 0));
        conn = newConnection();
        assertBackPointers(conn, conn.prepareStatement("select * from dual", 0, 0, 0));
        conn = newConnection();
        assertBackPointers(conn, conn.prepareStatement("select * from dual", new int[0]));
        conn = newConnection();
        assertBackPointers(conn, conn.prepareStatement("select * from dual", new String[0]));

        // callable statement
        conn = newConnection();
        assertBackPointers(conn, conn.prepareCall("select * from dual"));
        conn = newConnection();
        assertBackPointers(conn, conn.prepareCall("select * from dual", 0, 0));
        conn = newConnection();
        assertBackPointers(conn, conn.prepareCall("select * from dual", 0, 0, 0));
    }

    @Test
    public void testCanCloseCallableStatementTwice() throws Exception {
        try (Connection conn = newConnection()) {
            assertNotNull(conn);
            assertFalse(conn.isClosed());
            for (int i = 0; i < 2; i++) { // loop to show we *can* close again once we've borrowed it from the pool again
                final PreparedStatement stmt = conn.prepareCall("select * from dual");
                assertNotNull(stmt);
                assertFalse(isClosed(stmt));
                stmt.close();
                assertTrue(isClosed(stmt));
                stmt.close();
                assertTrue(isClosed(stmt));
                stmt.close();
                assertTrue(isClosed(stmt));
            }
        }
    }

    /**
     * Verify the close method can be called multiple times on a single connection without
     * an exception being thrown.
     */
    @Test
    public void testCanCloseConnectionTwice() throws Exception {
        for (int i = 0; i < getMaxTotal(); i++) { // loop to show we *can* close again once we've borrowed it from the pool again
            final Connection conn = newConnection();
            assertNotNull(conn);
            assertFalse(conn.isClosed());
            conn.close();
            assertTrue(conn.isClosed());
            conn.close();
            assertTrue(conn.isClosed());
        }
    }

    @Test
    public void testCanClosePreparedStatementTwice() throws Exception {
        try (Connection conn = newConnection()) {
            assertNotNull(conn);
            assertFalse(conn.isClosed());
            for (int i = 0; i < 2; i++) { // loop to show we *can* close again once we've borrowed it from the pool again
                final PreparedStatement stmt = conn.prepareStatement("select * from dual");
                assertNotNull(stmt);
                assertFalse(isClosed(stmt));
                stmt.close();
                assertTrue(isClosed(stmt));
                stmt.close();
                assertTrue(isClosed(stmt));
                stmt.close();
                assertTrue(isClosed(stmt));
            }
        }
    }

    @Test
    public void testCanCloseResultSetTwice() throws Exception {
        try (Connection conn = newConnection()) {
            assertNotNull(conn);
            assertFalse(conn.isClosed());
            for (int i = 0; i < 2; i++) { // loop to show we *can* close again once we've borrowed it from the pool again
                final PreparedStatement stmt = conn.prepareStatement("select * from dual");
                assertNotNull(stmt);
                final ResultSet rset = stmt.executeQuery();
                assertNotNull(rset);
                assertFalse(isClosed(rset));
                rset.close();
                assertTrue(isClosed(rset));
                rset.close();
                assertTrue(isClosed(rset));
                rset.close();
                assertTrue(isClosed(rset));
            }
        }
    }

    @Test
    public void testCanCloseStatementTwice() throws Exception {
        final Connection conn = newConnection();
        assertNotNull(conn);
        assertFalse(conn.isClosed());
        for (int i = 0; i < 2; i++) { // loop to show we *can* close again once we've borrowed it from the pool again
            final Statement stmt = conn.createStatement();
            assertNotNull(stmt);
            assertFalse(isClosed(stmt));
            stmt.close();
            assertTrue(isClosed(stmt));
            stmt.close();
            assertTrue(isClosed(stmt));
            stmt.close();
            assertTrue(isClosed(stmt));
        }
        conn.close();
    }

    @Test
    public void testClearWarnings() throws Exception {
        final Connection[] c = new Connection[getMaxTotal()];
        for (int i = 0; i < c.length; i++) {
            c[i] = newConnection();
            assertNotNull(c[i]);

            // generate SQLWarning on connection
            try (CallableStatement cs = c[i].prepareCall("warning")) {
                // empty
            }
        }

        for (final Connection element : c) {
            assertNotNull(element.getWarnings());
        }

        for (final Connection element : c) {
            element.close();
        }

        for (int i = 0; i < c.length; i++) {
            c[i] = newConnection();
        }

        for (final Connection element : c) {
            // warnings should have been cleared by putting the connection back in the pool
            assertNull(element.getWarnings());
        }

        for (final Connection element : c) {
            element.close();
        }
    }

    @Test
    public void testClosing() throws Exception {
        final Connection[] c = new Connection[getMaxTotal()];
        // open the maximum connections
        for (int i = 0; i < c.length; i++) {
            c[i] = newConnection();
        }

        // close one of the connections
        c[0].close();
        assertTrue(c[0].isClosed());

        // get a new connection
        c[0] = newConnection();

        for (final Connection element : c) {
            element.close();
        }
    }

    /** "https://issues.apache.org/bugzilla/show_bug.cgi?id=12400" */
    @Test
    public void testConnectionsAreDistinct() throws Exception {
        final Connection[] conn = new Connection[getMaxTotal()];
        for(int i=0;i<conn.length;i++) {
            conn[i] = newConnection();
            for(int j=0;j<i;j++) {
                assertNotSame(conn[j], conn[i]);
                assertNotEquals(conn[j], conn[i]);
            }
        }
        for (final Connection element : conn) {
            element.close();
        }
    }

    // Bugzilla Bug 26966: Connectionpool's connections always returns same
    @Test
    public void testHashCode() throws Exception {
        final Connection conn1 = newConnection();
        assertNotNull(conn1);
        final Connection conn2 = newConnection();
        assertNotNull(conn2);

        assertTrue(conn1.hashCode() != conn2.hashCode());
    }

    /**
     * DBCP-128: BasicDataSource.getConnection()
     * Connections don't work as hashtable keys
     */
    @Test
    public void testHashing() throws Exception {
        final Connection con = getConnection();
        final Hashtable<Connection, String> hash = new Hashtable<>();
        hash.put(con, "test");
        assertEquals("test", hash.get(con));
        assertTrue(hash.containsKey(con));
        assertTrue(hash.contains("test"));
        hash.clear();
        con.close();
    }

    @Test
    public void testIsClosed() throws Exception {
        for (int i = 0; i < getMaxTotal(); i++) {
            @SuppressWarnings("resource")
            final Connection conn = newConnection();
            try {
                assertNotNull(conn);
                assertFalse(conn.isClosed());
                try (PreparedStatement stmt = conn.prepareStatement("select * from dual")) {
                    assertNotNull(stmt);
                    try (ResultSet rset = stmt.executeQuery()) {
                        assertNotNull(rset);
                        assertTrue(rset.next());
                    }
                }
            } finally {
                conn.close();
            }
            assertTrue(conn.isClosed());
        }
    }

    @Test
    public void testMaxTotal() throws Exception {
        final Connection[] c = new Connection[getMaxTotal()];
        for (int i = 0; i < c.length; i++) {
            c[i] = newConnection();
            assertNotNull(c[i]);
        }

        // should only be able to open 10 connections, so this test should
        // throw an exception
        assertThrows(SQLException.class, this::newConnection);

        for (final Connection element : c) {
            element.close();
        }
    }

    // Bugzilla Bug 24966: NullPointer with Oracle 9 driver
    // wrong order of passivate/close when a rset isn't closed
    @Test
    public void testNoRsetClose() throws Exception {
        try (Connection conn = newConnection()) {
            assertNotNull(conn);
            try (PreparedStatement stmt = conn.prepareStatement("test")) {
                assertNotNull(stmt);
                final ResultSet rset = stmt.getResultSet();
                assertNotNull(rset);
                // forget to close the resultset: rset.close();
            }
        }
    }

    @Test
    public void testOpening() throws Exception {
        final Connection[] c = new Connection[getMaxTotal()];
        // test that opening new connections is not closing previous
        for (int i = 0; i < c.length; i++) {
            c[i] = newConnection();
            assertNotNull(c[i]);
            for (int j = 0; j <= i; j++) {
                assertFalse(c[j].isClosed());
            }
        }

        for (final Connection element : c) {
            element.close();
        }
    }

    @Test
    public void testPooling() throws Exception {
        // Grab a maximal set of open connections from the pool
        final Connection[] c = new Connection[getMaxTotal()];
        final Connection[] u = new Connection[getMaxTotal()];
        for (int i = 0; i < c.length; i++) {
            c[i] = newConnection();
            if (!(c[i] instanceof DelegatingConnection)) {
                for (int j = 0; j <= i; j++) {
                    c[j].close();
                }
                return; // skip this test
            }
            u[i] = ((DelegatingConnection<?>) c[i]).getInnermostDelegate();
        }
        // Close connections one at a time and get new ones, making sure
        // the new ones come from the pool
        for (final Connection element : c) {
            element.close();
            try (Connection con = newConnection()) {
                final Connection underCon = ((DelegatingConnection<?>) con).getInnermostDelegate();
                assertNotNull(underCon, "Failed to get connection");
                boolean found = false;
                for (int j = 0; j < c.length; j++) {
                    if (underCon == u[j]) {
                        found = true;
                        break;
                    }
                }
                assertTrue(found, "New connection not from pool");
            }
        }
    }

    // Bugzilla Bug 24328: PooledConnectionImpl ignores resultsetType
    // and Concurrency if statement pooling is not enabled
    // https://issues.apache.org/bugzilla/show_bug.cgi?id=24328
    @Test
    public void testPrepareStatementOptions() throws Exception {
        try (Connection conn = newConnection()) {
            assertNotNull(conn);
            try (PreparedStatement stmt = conn.prepareStatement("select * from dual", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)) {
                assertNotNull(stmt);
                try (ResultSet rset = stmt.executeQuery()) {
                    assertNotNull(rset);
                    assertTrue(rset.next());

                    assertEquals(ResultSet.TYPE_SCROLL_SENSITIVE, rset.getType());
                    assertEquals(ResultSet.CONCUR_UPDATABLE, rset.getConcurrency());

                }
            }
        }
    }

    @Test
    public void testRepeatedBorrowAndReturn() throws Exception {
        for (int i = 0; i < 100; i++) {
            try (Connection conn = newConnection()) {
                assertNotNull(conn);
                try (PreparedStatement stmt = conn.prepareStatement("select * from dual")) {
                    assertNotNull(stmt);
                    try (ResultSet rset = stmt.executeQuery()) {
                        assertNotNull(rset);
                        assertTrue(rset.next());
                    }
                }
            }
        }
    }

    @Test
    public void testSimple() throws Exception {
        try (Connection conn = newConnection()) {
            assertNotNull(conn);
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
    public void testSimple2() throws Exception {
        @SuppressWarnings("resource")
        final Connection conn = newConnection();
        assertNotNull(conn);
        try {
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
        } finally {
            conn.close();
        }
        assertThrows(SQLException.class, conn::createStatement, "Can't use closed connections");

        try (Connection conn2 = newConnection()) {
            assertNotNull(conn2);
            {
                try (PreparedStatement stmt = conn2.prepareStatement("select * from dual")) {
                    assertNotNull(stmt);
                    try (ResultSet rset = stmt.executeQuery()) {
                        assertNotNull(rset);
                        assertTrue(rset.next());
                    }
                }
            }
            {
                try (PreparedStatement stmt = conn2.prepareStatement("select * from dual")) {
                    assertNotNull(stmt);
                    try (ResultSet rset = stmt.executeQuery()) {
                        assertNotNull(rset);
                        assertTrue(rset.next());
                    }
                }
            }
        }
    }

    @Test
    public void testThreaded() {
        final TestThread[] threads = new TestThread[getMaxTotal()];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new TestThread(50, 50);
            final Thread t = new Thread(threads[i]);
            t.start();
        }
        for (int i = 0; i < threads.length; i++) {
            while (!threads[i].complete()) {
                try {
                    Thread.sleep(100L);
                } catch (final Exception e) {
                    // ignored
                }
            }
            if (threads[i] != null && threads[i].failed()) {
                fail("Thread failed: " + i);
            }
        }
    }

    long timeStampMillis() {
        return System.currentTimeMillis();// JVM 1.5+ System.nanoTime() / 1000000;
    }
}
