/*
 * Copyright 1999-2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    protected abstract Connection getConnection() throws Exception;
    
    protected int getMaxActive() {
        return 10;
    }
    
    protected long getMaxWait() {
        return 100L;
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
            c[i] = getConnection();
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
            c[i] = getConnection();
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
            Connection conn = getConnection();
            assertTrue(null != conn);
            assertTrue(!conn.isClosed());
            PreparedStatement stmt = conn.prepareStatement("select * from dual");
            assertTrue(null != stmt);
            ResultSet rset = stmt.executeQuery();
            assertTrue(null != rset);
            assertTrue(rset.next());
            rset.close();
            stmt.close();
            conn.close();
            assertTrue(conn.isClosed());
        }
    }

    public void testCantCloseConnectionTwice() throws Exception {
        for(int i=0;i<getMaxActive();i++) { // loop to show we *can* close again once we've borrowed it from the pool again
            Connection conn = getConnection();
            assertTrue(null != conn);
            assertTrue(!conn.isClosed());
            conn.close();
            assertTrue(conn.isClosed());
            try {
                conn.close();
                fail("Expected SQLException on second attempt to close (" + conn.getClass().getName() + ")");
            } catch(SQLException e) {
                // expected
            }
            assertTrue(conn.isClosed());
        }
    }

    public void testCantCloseStatementTwice() throws Exception {
        Connection conn = getConnection();
        assertTrue(null != conn);
        assertTrue(!conn.isClosed());
        for(int i=0;i<2;i++) { // loop to show we *can* close again once we've borrowed it from the pool again
            PreparedStatement stmt = conn.prepareStatement("select * from dual");
            assertTrue(null != stmt);
            stmt.close();
            try {
                stmt.close();
                fail("Expected SQLException on second attempt to close (" + stmt.getClass().getName() + ")");
            } catch(SQLException e) {
                // expected
            }
        }
        conn.close();
    }

    public void testSimple() throws Exception {
        Connection conn = getConnection();
        assertTrue(null != conn);
        PreparedStatement stmt = conn.prepareStatement("select * from dual");
        assertTrue(null != stmt);
        ResultSet rset = stmt.executeQuery();
        assertTrue(null != rset);
        assertTrue(rset.next());
        rset.close();
        stmt.close();
        conn.close();
    }

    public void testRepeatedBorrowAndReturn() throws Exception {
        for(int i=0;i<100;i++) {
            Connection conn = getConnection();
            assertTrue(null != conn);
            PreparedStatement stmt = conn.prepareStatement("select * from dual");
            assertTrue(null != stmt);
            ResultSet rset = stmt.executeQuery();
            assertTrue(null != rset);
            assertTrue(rset.next());
            rset.close();
            stmt.close();
            conn.close();
        }
    }

    public void testSimple2() throws Exception {
        Connection conn = getConnection();
        assertTrue(null != conn);
        {
            PreparedStatement stmt = conn.prepareStatement("select * from dual");
            assertTrue(null != stmt);
            ResultSet rset = stmt.executeQuery();
            assertTrue(null != rset);
            assertTrue(rset.next());
            rset.close();
            stmt.close();
        }
        {
            PreparedStatement stmt = conn.prepareStatement("select * from dual");
            assertTrue(null != stmt);
            ResultSet rset = stmt.executeQuery();
            assertTrue(null != rset);
            assertTrue(rset.next());
            rset.close();
            stmt.close();
        }
        conn.close();
        try {
            conn.createStatement();
            fail("Can't use closed connections");
        } catch(SQLException e) {
            ; // expected
        }

        conn = getConnection();
        assertTrue(null != conn);
        {
            PreparedStatement stmt = conn.prepareStatement("select * from dual");
            assertTrue(null != stmt);
            ResultSet rset = stmt.executeQuery();
            assertTrue(null != rset);
            assertTrue(rset.next());
            rset.close();
            stmt.close();
        }
        {
            PreparedStatement stmt = conn.prepareStatement("select * from dual");
            assertTrue(null != stmt);
            ResultSet rset = stmt.executeQuery();
            assertTrue(null != rset);
            assertTrue(rset.next());
            rset.close();
            stmt.close();
        }
        conn.close();
        conn = null;
    }

    public void testPooling() throws Exception {
        Connection conn = getConnection();
        Connection underconn = null;
        if(conn instanceof DelegatingConnection) {
            underconn = ((DelegatingConnection)conn).getInnermostDelegate();
        } else {
            return; // skip this test
        }
        assertTrue(underconn != null);
        Connection conn2 = getConnection();
        Connection underconn2 = null;
        if(conn2 instanceof DelegatingConnection) {
            underconn2 = ((DelegatingConnection)conn2).getInnermostDelegate();
        } else {
            return; // skip this test
        }
        assertTrue(underconn2 != null);
        assertTrue(underconn != underconn2);
        conn2.close();
        conn.close();
        Connection conn3 = getConnection();
        Connection underconn3 = null;
        if(conn3 instanceof DelegatingConnection) {
            underconn3 = ((DelegatingConnection)conn3).getInnermostDelegate();
        } else {
            return; // skip this test
        }
        assertTrue( underconn3 == underconn || underconn3 == underconn2 );
        conn3.close();
    }
    
    public void testAutoCommitBehavior() throws Exception {
        Connection conn = getConnection();
        assertTrue(conn != null);
        assertTrue(conn.getAutoCommit());
        conn.setAutoCommit(false);
        conn.close();
        
        Connection conn2 = getConnection();
        assertTrue( conn2.getAutoCommit() );
        
        Connection conn3 = getConnection();
        assertTrue( conn3.getAutoCommit() );

        conn2.close();
        
        conn3.close();
    }
    
    /** @see http://issues.apache.org/bugzilla/show_bug.cgi?id=12400 */
    public void testConnectionsAreDistinct() throws Exception {
        Connection[] conn = new Connection[getMaxActive()];
        for(int i=0;i<conn.length;i++) {
            conn[i] = getConnection();
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
            c[i] = getConnection();
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
            c[i] = getConnection();
        }

        // close one of the connections
        c[0].close();
        assertTrue(c[0].isClosed());

        // get a new connection
        c[0] = getConnection();

        for (int i = 0; i < c.length; i++) {
            c[i].close();
        }
    }

    public void testMaxActive() throws Exception {
        Connection[] c = new Connection[getMaxActive()];
        for (int i = 0; i < c.length; i++) {
            c[i] = getConnection();
            assertTrue(c[i] != null);
        }

        try {
            getConnection();
            fail("Allowed to open more than DefaultMaxActive connections.");
        } catch (java.sql.SQLException e) {
            // should only be able to open 10 connections, so this test should
            // throw an exception
        }

        for (int i = 0; i < c.length; i++) {
            c[i].close();
        }
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
                    Thread.sleep((long)_random.nextInt(_delay));
                } catch(Exception e) {
                    // ignored
                }
                Connection conn = null;
                PreparedStatement stmt = null;
                ResultSet rset = null;
                try {
                    conn = getConnection();
                    stmt = conn.prepareStatement("select 'literal', SYSDATE from dual");
                    rset = stmt.executeQuery();
                    try {
                        Thread.sleep((long)_random.nextInt(_delay));
                    } catch(Exception e) {
                        // ignored
                    }
                } catch(Exception e) {
                    e.printStackTrace();
                    _failed = true;
                    _complete = true;
                    break;
                } finally {
                    try { rset.close(); } catch(Exception e) { }
                    try { stmt.close(); } catch(Exception e) { }
                    try { conn.close(); } catch(Exception e) { }
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
        Connection conn = getConnection();
        assertTrue(null != conn);
        PreparedStatement stmt = conn.prepareStatement("select * from dual", 
            ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
        assertTrue(null != stmt);
        ResultSet rset = stmt.executeQuery();
        assertTrue(null != rset);
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
        Connection conn = getConnection();
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
        Connection conn1 = getConnection();
        assertNotNull(conn1);
        Connection conn2 = getConnection();
        assertNotNull(conn2);

        assertTrue(conn1.hashCode() != conn2.hashCode());
    }
}
