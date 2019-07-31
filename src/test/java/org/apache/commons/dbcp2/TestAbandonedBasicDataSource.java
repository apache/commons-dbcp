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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * TestSuite for BasicDataSource with abandoned connection trace enabled
 */
public class TestAbandonedBasicDataSource extends TestBasicDataSource {

    private StringWriter sw;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        // abandoned enabled but should not affect the basic tests
        // (very high timeout)
        ds.setLogAbandoned(true);
        ds.setRemoveAbandonedOnBorrow(true);
        ds.setRemoveAbandonedOnMaintenance(true);
        ds.setRemoveAbandonedTimeout(10000);
        sw = new StringWriter();
        ds.setAbandonedLogWriter(new PrintWriter(sw));
    }

    // ---------- Abandoned Test -----------

    @Test
    public void testAbandoned() throws Exception {
        // force abandoned
        ds.setRemoveAbandonedTimeout(0);
        ds.setMaxTotal(1);

        for (int i = 0; i < 3; i++) {
            assertNotNull(ds.getConnection());
        }
    }

    @Test
    public void testAbandonedClose() throws Exception {
        // force abandoned
        ds.setRemoveAbandonedTimeout(0);
        ds.setMaxTotal(1);
        ds.setAccessToUnderlyingConnectionAllowed(true);

        final Connection conn1 = getConnection();
        assertNotNull(conn1);
        assertEquals(1, ds.getNumActive());

        final Connection conn2 = getConnection();
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
        final String string = sw.toString();
        assertTrue(string.contains("testAbandonedClose"), string);
    }

    @Test
    public void testAbandonedCloseWithExceptions() throws Exception {
        // force abandoned
        ds.setRemoveAbandonedTimeout(0);
        ds.setMaxTotal(1);
        ds.setAccessToUnderlyingConnectionAllowed(true);

        final Connection conn1 = getConnection();
        assertNotNull(conn1);
        assertEquals(1, ds.getNumActive());

        final Connection conn2 = getConnection();
        assertNotNull(conn2);
        assertEquals(1, ds.getNumActive());

        // set an IO failure causing the isClosed method to fail
        final TesterConnection tconn1 = (TesterConnection) ((DelegatingConnection<?>) conn1).getInnermostDelegate();
        tconn1.setFailure(new IOException("network error"));
        final TesterConnection tconn2 = (TesterConnection) ((DelegatingConnection<?>) conn2).getInnermostDelegate();
        tconn2.setFailure(new IOException("network error"));

        try {
            conn2.close();
        } catch (final SQLException ex) {
            /* Ignore */
        }
        assertEquals(0, ds.getNumActive());

        try {
            conn1.close();
        } catch (final SQLException ex) {
        }
        assertEquals(0, ds.getNumActive());
        final String string = sw.toString();
        assertTrue(string.contains("testAbandonedCloseWithExceptions"), string);
    }

    /**
     * Verify that lastUsed property is updated when a connection
     * creates or prepares a statement
     */
    @Test
    public void testLastUsed() throws Exception {
        ds.setRemoveAbandonedTimeout(1);
        ds.setMaxTotal(2);
        try (Connection conn1 = ds.getConnection()) {
            Thread.sleep(500);
            try (Statement s = conn1.createStatement()) {} // Should reset lastUsed
            Thread.sleep(800);
            final Connection conn2 = ds.getConnection(); // triggers abandoned cleanup
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
    @Test
    public void testLastUsedPrepareCall() throws Exception {
        ds.setRemoveAbandonedTimeout(1);
        ds.setMaxTotal(2);
        try (Connection conn1 = ds.getConnection()) {
            Thread.sleep(500);
            try (CallableStatement cs = conn1.prepareCall("{call home}")) {} // Should reset lastUsed
            Thread.sleep(800);
            final Connection conn2 = ds.getConnection(); // triggers abandoned cleanup
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
    @Test
    public void testLastUsedPreparedStatementUse() throws Exception {
        ds.setRemoveAbandonedTimeout(1);
        ds.setMaxTotal(2);
        try (Connection conn1 = ds.getConnection();
                Statement st = conn1.createStatement()) {
            final String querySQL = "SELECT 1 FROM DUAL";
            Thread.sleep(500);
            Assertions.assertNotNull(st.executeQuery(querySQL)); // Should reset lastUsed
            Thread.sleep(800);
            final Connection conn2 = ds.getConnection(); // triggers abandoned cleanup
            Assertions.assertNotNull(st.executeQuery(querySQL)); // Should still be OK
            conn2.close();
            Thread.sleep(500);
            st.executeUpdate(""); // Should also reset
            Thread.sleep(800);
            try (Connection c = ds.getConnection()) {} // trigger abandoned cleanup again
            try (Statement s = conn1.createStatement()) {}  // Connection should still be good
        }
    }

    /**
     * DBCP-343 - verify that using a DelegatingStatement updates
     * the lastUsed on the parent connection
     */
    @Test
    public void testLastUsedLargePreparedStatementUse() throws Exception {
        ds.setRemoveAbandonedTimeout(1);
        ds.setMaxTotal(2);
        try (Connection conn1 = ds.getConnection();
                Statement st = conn1.createStatement()) {
            final String querySQL = "SELECT 1 FROM DUAL";
            Thread.sleep(500);
            Assertions.assertNotNull(st.executeQuery(querySQL)); // Should reset lastUsed
            Thread.sleep(800);
            final Connection conn2 = ds.getConnection(); // triggers abandoned cleanup
            Assertions.assertNotNull(st.executeQuery(querySQL)); // Should still be OK
            conn2.close();
            Thread.sleep(500);
            st.executeLargeUpdate(""); // Should also reset
            Thread.sleep(800);
            try (Connection c = ds.getConnection()) {} // trigger abandoned cleanup again
            try (Statement s = conn1.createStatement()) {}  // Connection should still be good
        }
    }

    /**
     * DBCP-343 - verify additional operations reset lastUsed on
     * the parent connection
     */
    @Test
    public void testLastUsedUpdate() throws Exception {
        final DelegatingConnection<?> conn = (DelegatingConnection<?>) ds.getConnection();
        final PreparedStatement ps = conn.prepareStatement("");
        final CallableStatement cs = conn.prepareCall("");
        final Statement st = conn.prepareStatement("");
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
    @Test
    public void testGarbageCollectorCleanUp01() throws Exception {
        final DelegatingConnection<?> conn = (DelegatingConnection<?>) ds.getConnection();
        Assertions.assertEquals(0, conn.getTrace().size());
        createStatement(conn);
        Assertions.assertEquals(1, conn.getTrace().size());
        System.gc();
        Assertions.assertEquals(0, conn.getTrace().size());
    }

    /**
     * DBCP-180 - things get more interesting with statement pooling.
     */
    @Test
    public void testGarbageCollectorCleanUp02() throws Exception {
        ds.setPoolPreparedStatements(true);
        ds.setAccessToUnderlyingConnectionAllowed(true);
        final DelegatingConnection<?> conn = (DelegatingConnection<?>) ds.getConnection();
        final PoolableConnection poolableConn = (PoolableConnection) conn.getDelegate();
        final PoolingConnection poolingConn = (PoolingConnection) poolableConn.getDelegate();
        @SuppressWarnings("unchecked")
        final
        GenericKeyedObjectPool<PStmtKey, DelegatingPreparedStatement>  gkop =
                (GenericKeyedObjectPool<PStmtKey, DelegatingPreparedStatement>) TesterUtils.getField(poolingConn, "pstmtPool");
        Assertions.assertEquals(0, conn.getTrace().size());
        Assertions.assertEquals(0, gkop.getNumActive());
        createStatement(conn);
        Assertions.assertEquals(1, conn.getTrace().size());
        Assertions.assertEquals(1, gkop.getNumActive());
        System.gc();
        // Finalization happens in a separate thread. Give the test time for
        // that to complete.
        int count = 0;
        while (count < 50 && gkop.getNumActive() > 0) {
            Thread.sleep(100);
            count++;
        }
        Assertions.assertEquals(0, gkop.getNumActive());
        Assertions.assertEquals(0, conn.getTrace().size());
    }

    private void createStatement(final Connection conn) throws Exception{
        final PreparedStatement ps = conn.prepareStatement("");
        Assertions.assertNotNull(ps);
    }


    /**
     * Verifies that Statement executeXxx methods update lastUsed on the parent connection
     */
    private void checkLastUsedStatement(final Statement st, final DelegatingConnection<?> conn) throws Exception {
        st.execute("");
        assertAndReset(conn);
        st.execute("", new int[] {});
        assertAndReset(conn);
        st.execute("", 0);
        assertAndReset(conn);
        st.executeBatch();
        assertAndReset(conn);
        st.executeLargeBatch();
        assertAndReset(conn);
        Assertions.assertNotNull(st.executeQuery(""));
        assertAndReset(conn);
        st.executeUpdate("");
        assertAndReset(conn);
        st.executeUpdate("", new int[] {});
        assertAndReset(conn);
        st.executeLargeUpdate("", new int[] {});
        assertAndReset(conn);
        st.executeUpdate("", 0);
        assertAndReset(conn);
        st.executeLargeUpdate("", 0);
        assertAndReset(conn);
        st.executeUpdate("", new String[] {});
        assertAndReset(conn);
        st.executeLargeUpdate("", new String[] {});
        assertAndReset(conn);
    }

    /**
     * Verifies that PreparedStatement executeXxx methods update lastUsed on the parent connection
     */
    private void checkLastUsedPreparedStatement(final PreparedStatement ps, final DelegatingConnection<?> conn) throws Exception {
        ps.execute();
        assertAndReset(conn);
        Assertions.assertNotNull(ps.executeQuery());
        assertAndReset(conn);
        ps.executeUpdate();
        assertAndReset(conn);
    }

    /**
     * Verifies that con.lastUsed has been updated and then resets it to 0
     */
    private void assertAndReset(final DelegatingConnection<?> con) {
        assertTrue(con.getLastUsed() > 0);
        con.setLastUsed(0);
    }
}
