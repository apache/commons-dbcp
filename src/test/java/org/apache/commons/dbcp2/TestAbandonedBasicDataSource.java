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

import java.io.IOException;
import java.sql.Connection;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.junit.Assert;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * TestSuite for BasicDataSource with abandoned connection trace enabled
 *
 * @author Dirk Verbeeck
 * @version $Revision$ $Date$
 */
public class TestAbandonedBasicDataSource extends TestBasicDataSource {
    public TestAbandonedBasicDataSource(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(TestAbandonedBasicDataSource.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        // abandoned enabled but should not affect the basic tests
        // (very high timeout)
        ds.setLogAbandoned(true);
        ds.setRemoveAbandonedOnBorrow(true);
        ds.setRemoveAbandonedOnMaintenance(true);
        ds.setRemoveAbandonedTimeout(10000);
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        // nothing to do here
    }

    // ---------- Abandoned Test -----------

    public void testAbandoned() throws Exception {
        // force abandoned
        ds.setRemoveAbandonedTimeout(0);
        ds.setMaxTotal(1);

        for (int i = 0; i < 3; i++) {
            assertNotNull(ds.getConnection());
        }
    }

    public void testAbandonedClose() throws Exception {
        // force abandoned
        ds.setRemoveAbandonedTimeout(0);
        ds.setMaxTotal(1);
        ds.setAccessToUnderlyingConnectionAllowed(true);

        Connection conn1 = getConnection();
        assertNotNull(conn1);
        assertEquals(1, ds.getNumActive());

        Connection conn2 = getConnection();
        // Attempt to borrow object triggers abandoned cleanup
        // conn1 should be closed by the pool to make room
        assertNotNull(conn2);
        assertEquals(1, ds.getNumActive());
        // Verify that conn1 is closed
        assertTrue(((DelegatingConnection<?>) conn1).getInnermostDelegate().isClosed());

        conn2.close();
        assertEquals(0, ds.getNumActive());

        // Second close on conn1 is OK as of dbcp 1.3
        conn1.close();
        assertEquals(0, ds.getNumActive());
    }

    public void testAbandonedCloseWithExceptions() throws Exception {
        // force abandoned
        ds.setRemoveAbandonedTimeout(0);
        ds.setMaxTotal(1);
        ds.setAccessToUnderlyingConnectionAllowed(true);

        Connection conn1 = getConnection();
        assertNotNull(conn1);
        assertEquals(1, ds.getNumActive());

        Connection conn2 = getConnection();
        assertNotNull(conn2);
        assertEquals(1, ds.getNumActive());

        // set an IO failure causing the isClosed mathod to fail
        TesterConnection tconn1 = (TesterConnection) ((DelegatingConnection<?>)conn1).getInnermostDelegate();
        tconn1.setFailure(new IOException("network error"));
        TesterConnection tconn2 = (TesterConnection) ((DelegatingConnection<?>)conn2).getInnermostDelegate();
        tconn2.setFailure(new IOException("network error"));

        try {
            conn2.close();
        } catch (SQLException ex) {
            /* Ignore */
        }
        assertEquals(0, ds.getNumActive());

        try { conn1.close(); } catch (SQLException ex) { }
        assertEquals(0, ds.getNumActive());
    }

    /**
     * Verify that lastUsed property is updated when a connection
     * creates or prepares a statement
     */
    public void testlastUsed() throws Exception {
        ds.setRemoveAbandonedTimeout(1);
        ds.setMaxTotal(2);
        try (Connection conn1 = ds.getConnection()) {
            Thread.sleep(500);
            try (Statement s = conn1.createStatement()) {} // Should reset lastUsed
            Thread.sleep(800);
            Connection conn2 = ds.getConnection(); // triggers abandoned cleanup
            try (Statement s = conn1.createStatement()) {} // Should still be OK
            conn2.close();
            Thread.sleep(500);
            try (PreparedStatement ps = conn1.prepareStatement("SELECT 1 FROM DUAL")) {} // reset
            Thread.sleep(800);
            try (Connection c = ds.getConnection()) {} // trigger abandoned cleanup again
            try (Statement s = conn1.createStatement()) {}
        }
    }

    /**
     * Verify that lastUsed property is updated when a connection
     * prepares a callable statement.
     */
    public void testlastUsedPrepareCall() throws Exception {
        ds.setRemoveAbandonedTimeout(1);
        ds.setMaxTotal(2);
        try (Connection conn1 = ds.getConnection()) {
            Thread.sleep(500);
            try (CallableStatement cs = conn1.prepareCall("{call home}")) {} // Should reset lastUsed
            Thread.sleep(800);
            Connection conn2 = ds.getConnection(); // triggers abandoned cleanup
            try (CallableStatement cs = conn1.prepareCall("{call home}")) {} // Should still be OK
            conn2.close();
            Thread.sleep(500);
            try (CallableStatement cs = conn1.prepareCall("{call home}")) {} // reset
            Thread.sleep(800);
            try (Connection c = ds.getConnection()) {} // trigger abandoned cleanup again
            try (Statement s = conn1.createStatement()) {}
        }
    }

    /**
     * DBCP-343 - verify that using a DelegatingStatement updates
     * the lastUsed on the parent connection
     */
    public void testLastUsedPreparedStatementUse() throws Exception {
        ds.setRemoveAbandonedTimeout(1);
        ds.setMaxTotal(2);
        try (Connection conn1 = ds.getConnection();
                Statement st = conn1.createStatement()) {
            String querySQL = "SELECT 1 FROM DUAL";
            Thread.sleep(500);
            Assert.assertNotNull(st.executeQuery(querySQL)); // Should reset lastUsed
            Thread.sleep(800);
            Connection conn2 = ds.getConnection(); // triggers abandoned cleanup
            Assert.assertNotNull(st.executeQuery(querySQL)); // Should still be OK
            conn2.close();
            Thread.sleep(500);
            st.executeUpdate(""); // Should also reset
            Thread.sleep(800);
            try (Connection c = ds.getConnection()) {} // trigger abandoned cleanup again
            try (Statement s = conn1.createStatement()) {}  // Connection should still be good
        }
    }

    /**
     * DBCP-343 - verify additional operations reset lastUsed on
     * the parent connection
     */
    public void testLastUsedUpdate() throws Exception {
        DelegatingConnection<?> conn = (DelegatingConnection<?>) ds.getConnection();
        PreparedStatement ps = conn.prepareStatement("");
        CallableStatement cs = conn.prepareCall("");
        Statement st = conn.prepareStatement("");
        checkLastUsedStatement(ps, conn);
        checkLastUsedPreparedStatement(ps, conn);
        checkLastUsedStatement(cs, conn);
        checkLastUsedPreparedStatement(cs, conn);
        checkLastUsedStatement(st, conn);
    }

    /**
     * DBCP-180 - verify that a GC can clean up an unused Statement when it is
     * no longer referenced even when it is tracked via the AbandonedTrace
     * mechanism.
     */
    public void testGarbageCollectorCleanUp01() throws Exception {
        DelegatingConnection<?> conn = (DelegatingConnection<?>) ds.getConnection();
        Assert.assertEquals(0, conn.getTrace().size());
        createStatement(conn);
        Assert.assertEquals(1, conn.getTrace().size());
        System.gc();
        Assert.assertEquals(0, conn.getTrace().size());
    }

    /**
     * DBCP-180 - things get more interesting with statement pooling.
     */
    public void testGarbageCollectorCleanUp02() throws Exception {
        ds.setPoolPreparedStatements(true);
        ds.setAccessToUnderlyingConnectionAllowed(true);
        DelegatingConnection<?> conn = (DelegatingConnection<?>) ds.getConnection();
        PoolableConnection poolableConn = (PoolableConnection) conn.getDelegate();
        PoolingConnection poolingConn = (PoolingConnection) poolableConn.getDelegate();
        @SuppressWarnings("unchecked")
        GenericKeyedObjectPool<PStmtKey,DelegatingPreparedStatement>  gkop =
                (GenericKeyedObjectPool<PStmtKey,DelegatingPreparedStatement>) TesterUtils.getField(poolingConn, "_pstmtPool");
        Assert.assertEquals(0, conn.getTrace().size());
        Assert.assertEquals(0, gkop.getNumActive());
        createStatement(conn);
        Assert.assertEquals(1, conn.getTrace().size());
        Assert.assertEquals(1, gkop.getNumActive());
        System.gc();
        // Finalization happens in a separate thread. Give the test time for
        // that to complete.
        int count = 0;
        while (count < 50 && gkop.getNumActive() > 0) {
            Thread.sleep(100);
            count++;
        }
        Assert.assertEquals(0, gkop.getNumActive());
        Assert.assertEquals(0, conn.getTrace().size());
    }

    private void createStatement(Connection conn) throws Exception{
        PreparedStatement ps = conn.prepareStatement("");
        Assert.assertNotNull(ps);
    }


    /**
     * Verifies that Statement executeXxx methods update lastUsed on the parent connection
     */
    private void checkLastUsedStatement(Statement st, DelegatingConnection<?> conn) throws Exception {
        st.execute("");
        assertAndReset(conn);
        st.execute("", new int[] {});
        assertAndReset(conn);
        st.execute("", 0);
        assertAndReset(conn);
        st.executeBatch();
        assertAndReset(conn);
        Assert.assertNotNull(st.executeQuery(""));
        assertAndReset(conn);
        st.executeUpdate("");
        assertAndReset(conn);
        st.executeUpdate("", new int[] {});
        assertAndReset(conn);
        st.executeUpdate("", 0);
        assertAndReset(conn);
        st.executeUpdate("", new String[] {});
        assertAndReset(conn);
    }

    /**
     * Verifies that PreparedStatement executeXxx methods update lastUsed on the parent connection
     */
    private void checkLastUsedPreparedStatement(PreparedStatement ps, DelegatingConnection<?> conn) throws Exception {
        ps.execute();
        assertAndReset(conn);
        Assert.assertNotNull(ps.executeQuery());
        assertAndReset(conn);
        ps.executeUpdate();
        assertAndReset(conn);
    }

    /**
     * Verifies that con.lastUsed has been updated and then resets it to 0
     */
    private void assertAndReset(DelegatingConnection<?> con) {
        assertTrue(con.getLastUsed() > 0);
        con.setLastUsed(0);
    }
}
