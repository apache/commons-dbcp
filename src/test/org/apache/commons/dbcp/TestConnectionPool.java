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

package org.apache.commons.dbcp;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Hashtable;
import java.util.Stack;

import junit.framework.TestCase;

// XXX FIX ME XXX
// this class still needs some cleanup, but at least
// this consolidates most of the relevant test code
// in a fairly re-usable fashion
// XXX FIX ME XXX

/**
 * Base test suite for DBCP pools.
 * 
 * @author Rodney Waldhoff
 * @author Sean C. Sullivan
 * @author John McNally
 * @author Dirk Verbeeck
 * @version $Revision$ $Date$
 */
public abstract class TestConnectionPool extends TestCase {
    public TestConnectionPool(String testName) {
        super(testName);
    }

    public void tearDown() throws Exception {
        super.tearDown();
        // Close any connections opened by the test
        while (!connections.isEmpty()) {
            Connection conn = (Connection) connections.pop();
            try {
                conn.close();
            } catch (Exception ex) { 
                // ignore
            } finally {
                conn = null;
            }
        }
    }

    protected abstract Connection getConnection() throws Exception;
    
    protected int getMaxActive() {
        return 10;
    }
    
    protected long getMaxWait() {
        return 100L;
    }
    
    /** Connections opened during the course of a test */
    protected Stack connections = new Stack();
    
    /** Acquire a connection and push it onto the connections stack */
    protected Connection newConnection() throws Exception {
        Connection connection = getConnection();
        connections.push(connection);
        return connection;
    }

    // ----------- Utility Methods --------------------------------- 

    protected String getUsername(Connection conn) throws SQLException {
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("select username");
        if (rs.next()) {
            return rs.getString(1);
        }
        return null;
    }

    // ----------- tests --------------------------------- 

    public void testClearWarnings() throws Exception {
        Connection[] c = new Connection[getMaxActive()];
        for (int i = 0; i < c.length; i++) {
            c[i] = newConnection();
            assertTrue(c[i] != null);
            
            // generate SQLWarning on connection
            c[i].prepareCall("warning");
        }

        for (int i = 0; i < c.length; i++) {
            assertNotNull(c[i].getWarnings());
        }

        for (int i = 0; i < c.length; i++) {
            c[i].close();
        }
        
        for (int i = 0; i < c.length; i++) {
            c[i] = newConnection();
        }        

        for (int i = 0; i < c.length; i++) {
            // warnings should have been cleared by putting the connection back in the pool
            assertNull(c[i].getWarnings());
        }

        for (int i = 0; i < c.length; i++) {
            c[i].close();
        }
    }

    public void testIsClosed() throws Exception {
        for(int i=0;i<getMaxActive();i++) {
            Connection conn = newConnection();
            assertNotNull(conn);
            assertTrue(!conn.isClosed());
            PreparedStatement stmt = conn.prepareStatement("select * from dual");
            assertNotNull(stmt);
            ResultSet rset = stmt.executeQuery();
            assertNotNull(rset);
            assertTrue(rset.next());
            rset.close();
            stmt.close();
            conn.close();
            assertTrue(conn.isClosed());
        }
    }

    /**
     * Verify the close method can be called multiple times on a single connection without
     * an exception being thrown.
     */
    public void testCanCloseConnectionTwice() throws Exception {
        for (int i = 0; i < getMaxActive(); i++) { // loop to show we *can* close again once we've borrowed it from the pool again
            Connection conn = newConnection();
            assertNotNull(conn);
            assertTrue(!conn.isClosed());
            conn.close();
            assertTrue(conn.isClosed());
            conn.close();
            assertTrue(conn.isClosed());
        }
    }

    public void testCanCloseStatementTwice() throws Exception {
        Connection conn = newConnection();
        assertNotNull(conn);
        assertTrue(!conn.isClosed());
        for(int i=0;i<2;i++) { // loop to show we *can* close again once we've borrowed it from the pool again
            Statement stmt = conn.createStatement();
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

    public void testCanClosePreparedStatementTwice() throws Exception {
        Connection conn = newConnection();
        assertNotNull(conn);
        assertTrue(!conn.isClosed());
        for(int i=0;i<2;i++) { // loop to show we *can* close again once we've borrowed it from the pool again
            PreparedStatement stmt = conn.prepareStatement("select * from dual");
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

    public void testCanCloseCallableStatementTwice() throws Exception {
        Connection conn = newConnection();
        assertNotNull(conn);
        assertTrue(!conn.isClosed());
        for(int i=0;i<2;i++) { // loop to show we *can* close again once we've borrowed it from the pool again
            PreparedStatement stmt = conn.prepareCall("select * from dual");
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

    public void testCanCloseResultSetTwice() throws Exception {
        Connection conn = newConnection();
        assertNotNull(conn);
        assertTrue(!conn.isClosed());
        for(int i=0;i<2;i++) { // loop to show we *can* close again once we've borrowed it from the pool again
            PreparedStatement stmt = conn.prepareStatement("select * from dual");
            assertNotNull(stmt);
            ResultSet rset = stmt.executeQuery();
            assertNotNull(rset);
            assertFalse(isClosed(rset));
            rset.close();
            assertTrue(isClosed(rset));
            rset.close();
            assertTrue(isClosed(rset));
            rset.close();
            assertTrue(isClosed(rset));
        }
        conn.close();
    }

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

    protected void assertBackPointers(Connection conn, Statement statement) throws SQLException {
        assertFalse(conn.isClosed());
        assertFalse(isClosed(statement));

        assertSame("statement.getConnection() should return the exact same connection instance that was used to create the statement",
                conn, statement.getConnection());

        ResultSet resultSet = statement.getResultSet();
        assertFalse(isClosed(resultSet));
        assertSame("resultSet.getStatement() should return the exact same statement instance that was used to create the result set",
                statement, resultSet.getStatement());

        ResultSet executeResultSet = statement.executeQuery("select * from dual");
        assertFalse(isClosed(executeResultSet));
        assertSame("resultSet.getStatement() should return the exact same statement instance that was used to create the result set",
                statement, executeResultSet.getStatement());

        ResultSet keysResultSet = statement.getGeneratedKeys();
        assertFalse(isClosed(keysResultSet));
        assertSame("resultSet.getStatement() should return the exact same statement instance that was used to create the result set",
                statement, keysResultSet.getStatement());

        ResultSet preparedResultSet = null;
        if (statement instanceof PreparedStatement) {
            PreparedStatement preparedStatement = (PreparedStatement) statement;
            preparedResultSet = preparedStatement.executeQuery();
            assertFalse(isClosed(preparedResultSet));
            assertSame("resultSet.getStatement() should return the exact same statement instance that was used to create the result set",
                    statement, preparedResultSet.getStatement());
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

    public void testSimple() throws Exception {
        Connection conn = newConnection();
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

    public void testRepeatedBorrowAndReturn() throws Exception {
        for(int i=0;i<100;i++) {
            Connection conn = newConnection();
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
    }

    public void testSimple2() throws Exception {
        Connection conn = newConnection();
        assertNotNull(conn);
        {
            PreparedStatement stmt = conn.prepareStatement("select * from dual");
            assertNotNull(stmt);
            ResultSet rset = stmt.executeQuery();
            assertNotNull(rset);
            assertTrue(rset.next());
            rset.close();
            stmt.close();
        }
        {
            PreparedStatement stmt = conn.prepareStatement("select * from dual");
            assertNotNull(stmt);
            ResultSet rset = stmt.executeQuery();
            assertNotNull(rset);
            assertTrue(rset.next());
            rset.close();
            stmt.close();
        }
        conn.close();
        try {
            conn.createStatement();
            fail("Can't use closed connections");
        } catch(SQLException e) {
            // expected
        }

        conn = newConnection();
        assertNotNull(conn);
        {
            PreparedStatement stmt = conn.prepareStatement("select * from dual");
            assertNotNull(stmt);
            ResultSet rset = stmt.executeQuery();
            assertNotNull(rset);
            assertTrue(rset.next());
            rset.close();
            stmt.close();
        }
        {
            PreparedStatement stmt = conn.prepareStatement("select * from dual");
            assertNotNull(stmt);
            ResultSet rset = stmt.executeQuery();
            assertNotNull(rset);
            assertTrue(rset.next());
            rset.close();
            stmt.close();
        }
        conn.close();
        conn = null;
    }

    public void testPooling() throws Exception {  
        // Grab a maximal set of open connections from the pool
        Connection[] c = new Connection[getMaxActive()];
        Connection[] u = new Connection[getMaxActive()];
        for (int i = 0; i < c.length; i++) {
            c[i] = newConnection();
            if (c[i] instanceof DelegatingConnection) {
                u[i] = ((DelegatingConnection) c[i]).getInnermostDelegate();
            } else {
                for (int j = 0; j <= i; j++) {
                    c[j].close();
                }
                return; // skip this test   
            }
        }        
        // Close connections one at a time and get new ones, making sure
        // the new ones come from the pool
        for (int i = 0; i < c.length; i++) {
            c[i].close();
            Connection con = newConnection();
            Connection underCon = 
                ((DelegatingConnection) con).getInnermostDelegate();
            assertTrue("Failed to get connection", underCon != null);
            boolean found = false;
            for (int j = 0; j < c.length; j++) {
                if (underCon == u[j]) {
                    found = true;
                    break;
                }
            }
            assertTrue("New connection not from pool", found);
            con.close();
        }
    }
    
    public void testAutoCommitBehavior() throws Exception {
        Connection conn = newConnection();
        assertNotNull(conn);
        assertTrue(conn.getAutoCommit());
        conn.setAutoCommit(false);
        conn.close();
        
        Connection conn2 = newConnection();
        assertTrue( conn2.getAutoCommit() );
        
        Connection conn3 = newConnection();
        assertTrue( conn3.getAutoCommit() );

        conn2.close();
        
        conn3.close();
    }
    
    /** @see "http://issues.apache.org/bugzilla/show_bug.cgi?id=12400" */
    public void testConnectionsAreDistinct() throws Exception {
        Connection[] conn = new Connection[getMaxActive()];
        for(int i=0;i<conn.length;i++) {
            conn[i] = newConnection();
            for(int j=0;j<i;j++) {
                assertTrue(conn[j] != conn[i]);
                assertTrue(!conn[j].equals(conn[i]));
            }
        }
        for(int i=0;i<conn.length;i++) {
            conn[i].close();
        }
    }


    public void testOpening() throws Exception {
        Connection[] c = new Connection[getMaxActive()];
        // test that opening new connections is not closing previous
        for (int i = 0; i < c.length; i++) {
            c[i] = newConnection();
            assertTrue(c[i] != null);
            for (int j = 0; j <= i; j++) {
                assertTrue(!c[j].isClosed());
            }
        }

        for (int i = 0; i < c.length; i++) {
            c[i].close();
        }
    }

    public void testClosing() throws Exception {
        Connection[] c = new Connection[getMaxActive()];
        // open the maximum connections
        for (int i = 0; i < c.length; i++) {
            c[i] = newConnection();
        }

        // close one of the connections
        c[0].close();
        assertTrue(c[0].isClosed());

        // get a new connection
        c[0] = newConnection();

        for (int i = 0; i < c.length; i++) {
            c[i].close();
        }
    }

    public void testMaxActive() throws Exception {
        Connection[] c = new Connection[getMaxActive()];
        for (int i = 0; i < c.length; i++) {
            c[i] = newConnection();
            assertTrue(c[i] != null);
        }

        try {
            newConnection();
            fail("Allowed to open more than DefaultMaxActive connections.");
        } catch (java.sql.SQLException e) {
            // should only be able to open 10 connections, so this test should
            // throw an exception
        }

        for (int i = 0; i < c.length; i++) {
            c[i].close();
        }
    }
    
    /**
     * DBCP-128: BasicDataSource.getConnection()
     * Connections don't work as hashtable keys 
     */
    public void testHashing() throws Exception {
        Connection con = getConnection();
        Hashtable hash = new Hashtable();
        hash.put(con, "test");
        assertEquals("test", hash.get(con));
        assertTrue(hash.containsKey(con));
        assertTrue(hash.contains("test")); 
        hash.clear();
        con.close();
    }

    public void testThreaded() {
        TestThread[] threads = new TestThread[getMaxActive()];
        for(int i=0;i<threads.length;i++) {
            threads[i] = new TestThread(50,50);
            Thread t = new Thread(threads[i]);
            t.start();
        }
        for(int i=0;i<threads.length;i++) {
            while(!(threads[i]).complete()) {
                try {
                    Thread.sleep(100L);
                } catch(Exception e) {
                    // ignored
                }
            }
            if(threads[i].failed()) {
                fail();
            }
        }
    }

    class TestThread implements Runnable {
        java.util.Random _random = new java.util.Random();
        boolean _complete = false;
        boolean _failed = false;
        int _iter = 100;
        int _delay = 50;

        public TestThread() {
        }

        public TestThread(int iter) {
            _iter = iter;
        }

        public TestThread(int iter, int delay) {
            _iter = iter;
            _delay = delay;
        }

        public boolean complete() {
            return _complete;
        }

        public boolean failed() {
            return _failed;
        }

        public void run() {
            for(int i=0;i<_iter;i++) {
                try {
                    Thread.sleep(_random.nextInt(_delay));
                } catch(Exception e) {
                    // ignored
                }
                Connection conn = null;
                PreparedStatement stmt = null;
                ResultSet rset = null;
                try {
                    conn = newConnection();
                    stmt = conn.prepareStatement("select 'literal', SYSDATE from dual");
                    rset = stmt.executeQuery();
                    try {
                        Thread.sleep(_random.nextInt(_delay));
                    } catch(Exception e) {
                        // ignored
                    }
                } catch(Exception e) {
                    e.printStackTrace();
                    _failed = true;
                    _complete = true;
                    break;
                } finally {
                    try { if (rset != null) rset.close(); } catch(Exception e) { }
                    try { if (stmt != null) stmt.close(); } catch(Exception e) { }
                    try { if (conn != null) conn.close(); } catch(Exception e) { }
                }
            }
            _complete = true;
        }
    }

    // Bugzilla Bug 24328: PooledConnectionImpl ignores resultsetType 
    // and Concurrency if statement pooling is not enabled
    // http://issues.apache.org/bugzilla/show_bug.cgi?id=24328
    public void testPrepareStatementOptions() throws Exception 
    {
        Connection conn = newConnection();
        assertNotNull(conn);
        PreparedStatement stmt = conn.prepareStatement("select * from dual", 
            ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
        assertNotNull(stmt);
        ResultSet rset = stmt.executeQuery();
        assertNotNull(rset);
        assertTrue(rset.next());
        
        assertEquals(ResultSet.TYPE_SCROLL_SENSITIVE, rset.getType());
        assertEquals(ResultSet.CONCUR_UPDATABLE, rset.getConcurrency());
        
        rset.close();
        stmt.close();
        conn.close();
    }

    // Bugzilla Bug 24966: NullPointer with Oracle 9 driver
    // wrong order of passivate/close when a rset isn't closed
    public void testNoRsetClose() throws Exception {
        Connection conn = newConnection();
        assertNotNull(conn);
        PreparedStatement stmt = conn.prepareStatement("test");
        assertNotNull(stmt);
        ResultSet rset = stmt.getResultSet();
        assertNotNull(rset);
        // forget to close the resultset: rset.close();
        stmt.close();
        conn.close();
    }
    
    // Bugzilla Bug 26966: Connectionpool's connections always returns same
    public void testHashCode() throws Exception {
        Connection conn1 = newConnection();
        assertNotNull(conn1);
        Connection conn2 = newConnection();
        assertNotNull(conn2);

        assertTrue(conn1.hashCode() != conn2.hashCode());
    }

    protected boolean isClosed(Statement statement) {
        try {
            statement.getWarnings();
            return false;
        } catch (SQLException e) {
            // getWarnings throws an exception if the statement is
            // closed, but could throw an exception for other reasons
            // in this case it is good enought to assume the statement
            // is closed
            return true;
        }
    }

    protected boolean isClosed(ResultSet resultSet) {
        try {
            resultSet.getWarnings();
            return false;
        } catch (SQLException e) {
            // getWarnings throws an exception if the statement is
            // closed, but could throw an exception for other reasons
            // in this case it is good enought to assume the result set
            // is closed
            return true;
        }
    }

    protected void multipleThreads(final int holdTime, final boolean expectError, long maxWait)
            throws Exception {
                long startTime = System.currentTimeMillis();
                final PoolTest[] pts = new PoolTest[2 * getMaxActive()];
                // Catch Exception so we can stop all threads if one fails
                ThreadGroup threadGroup = new ThreadGroup("foo") {
                    public void uncaughtException(Thread t, Throwable e) {
                        for (int i = 0; i < pts.length; i++) {
                            pts[i].stop();
                        }
                    }
                };
                for (int i = 0; i < pts.length; i++) {
                    (pts[i] = new PoolTest(threadGroup, holdTime)).start();
                }

                Thread.sleep(100L); // Wait for long enough to allow threads to start

                for (int i = 0; i < pts.length; i++) {
                    pts[i].stop();
                }
                /*
                 * Wait for all threads to terminate.
                 * This is essential to ensure that all threads have a chance to update success[0]
                 * and to ensure that the variable is published correctly.
                 */
                int done=0;
                int failed=0;
                for (int i = 0; i < pts.length; i++) {
                    final PoolTest poolTest = pts[i];
                    poolTest.thread.join();
                    final String state = poolTest.state;
                    if (DONE.equals(state)){
                        done++;
                    }
                    final Throwable thrown = poolTest.thrown;
                    if (thrown != null) {
                        failed++;
                        if (!expectError || !(thrown instanceof SQLException)){
                            System.out.println("Unexpected error: "+thrown.getMessage());
                        }
                    }
                }
            
                long time = System.currentTimeMillis() - startTime;
                System.out.println("Multithread test time = " + time
                        + " ms. Threads: " + pts.length
                        + ". Hold time: " + holdTime
                        + ". Maxwait: " + maxWait
                        + ". Done: " + done
                        + ". Failed: " + failed
                        + ". expectError: " + expectError
                        );
                if (expectError) {
                    // Perform initial sanity check:
                    assertTrue("Expected some of the threads to fail",failed > 0);
// DBCP-318 is now fixed, so disable extra debug
//                    if (pts.length/2 != failed){
//                        for (int i = 0; i < pts.length; i++) {
//                            PoolTest pt = pts[i];
//                            System.out.println(
//                                    "StartupDelay: " + (pt.started-pt.created)
//                                    + ". ConnectTime: " + (pt.connected > 0 ? Long.toString(pt.connected-pt.started) : "-")
//                                    + ". Runtime: " + (pt.ended-pt.started)
//                                    + ". Loops: " + pt.loops
//                                    + ". State: " + pt.state
//                                    + ". thrown: "+ pt.thrown
//                                    );
//                        }                        
//                    }
                    assertEquals("WARNING: Expected half the threads to fail",pts.length/2,failed);
                } else {
                    assertEquals("Did not expect any threads to fail",0,failed);
                }
            }
    private static int currentThreadCount = 0;

    private static final String DONE = "Done";

    protected class PoolTest implements Runnable {
        /**
         * The number of milliseconds to hold onto a database connection
         */
        private final int connHoldTime;

        private volatile boolean isRun;

        private String state; // No need to be volatile if it is read after the thread finishes

        private final Thread thread;

        private Throwable thrown;

        // Debug for DBCP-318
//        private final long created; // When object was created
//        private long started; // when thread started
//        private long ended; // when thread ended
//        private long connected; // when thread last connected
//        private int loops = 0;

        private final boolean stopOnException; // If true, don't rethrow Exception
        
        private PoolTest(ThreadGroup threadGroup, int connHoldTime) {
            this(threadGroup, connHoldTime, false);
        }
            
        public PoolTest(ThreadGroup threadGroup, int connHoldTime, boolean isStopOnException) {
            this.connHoldTime = connHoldTime;
            stopOnException = isStopOnException;
            isRun = true; // Must be done here so main thread is guaranteed to be able to set it false
            thrown = null;
            thread =
                new Thread(threadGroup, this, "Thread+" + currentThreadCount++);
            thread.setDaemon(false);
//            created = System.currentTimeMillis();
        }

        public void start(){
            thread.start();
        }

        public void run() {
//            started = System.currentTimeMillis();
            try {
                while (isRun) {
//                    loops++;
                    state = "Getting Connection";
                    Connection conn = getConnection();
//                    connected = System.currentTimeMillis();
                    state = "Using Connection";
                    assertNotNull(conn);
                    PreparedStatement stmt =
                        conn.prepareStatement("select * from dual");
                    assertNotNull(stmt);
                    ResultSet rset = stmt.executeQuery();
                    assertNotNull(rset);
                    assertTrue(rset.next());
                    state = "Holding Connection";
                    Thread.sleep(connHoldTime);
                    state = "Closing ResultSet";
                    rset.close();
                    state = "Closing Statement";
                    stmt.close();
                    state = "Closing Connection";
                    conn.close();
                    state = "Closed";
                }
                state = DONE;
            } catch (Throwable t) {
                thrown = t;
                if (!stopOnException) {
                    throw new RuntimeException();
                }
            } finally {
//                ended = System.currentTimeMillis();                
            }
        }

        public void stop() {
            isRun = false;
        }

        public Thread getThread() {
            return thread;
        }
    }
}
