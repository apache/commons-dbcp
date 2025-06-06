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

package org.apache.commons.dbcp2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;

import org.apache.commons.pool2.KeyedObjectPool;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * TestSuite for BasicDataSource with abandoned connection trace enabled
 */
public class TestAbandonedBasicDataSource extends TestBasicDataSource {

    private StringWriter sw;

    /**
     * Verifies that con.lastUsed has been updated and then resets it to 0
     */
    private void assertAndReset(final DelegatingConnection<?> con) {
        assertTrue(con.getLastUsedInstant().compareTo(Instant.EPOCH) > 0);
        con.setLastUsed(Instant.EPOCH);
    }

    /**
     * Verifies that PreparedStatement executeXxx methods update lastUsed on the parent connection
     */
    private void checkLastUsedPreparedStatement(final PreparedStatement ps, final DelegatingConnection<?> conn) throws Exception {
        ps.execute();
        assertAndReset(conn);
        try (ResultSet rs = ps.executeQuery()) {
            Assertions.assertNotNull(rs);
        }
        assertAndReset(conn);
        ps.executeUpdate();
        assertAndReset(conn);
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
        try (ResultSet rs = st.executeQuery("")) {
            Assertions.assertNotNull(rs);
        }
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

    private void createStatement(final Connection conn) throws Exception {
        final PreparedStatement ps = conn.prepareStatement("");
        Assertions.assertNotNull(ps);
    }

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        // abandoned enabled but should not affect the basic tests
        // (very high timeout)
        ds.setLogAbandoned(true);
        ds.setRemoveAbandonedOnBorrow(true);
        ds.setRemoveAbandonedOnMaintenance(true);
        ds.setRemoveAbandonedTimeout(Duration.ofSeconds(10));
        sw = new StringWriter();
        ds.setAbandonedLogWriter(new PrintWriter(sw));
    }

    @Test
    void testAbandoned() throws Exception {
        // force abandoned
        ds.setRemoveAbandonedTimeout(Duration.ZERO);
        ds.setMaxTotal(1);

        for (int i = 0; i < 3; i++) {
            assertNotNull(ds.getConnection());
        }
    }

    @Test
    void testAbandonedClose() throws Exception {
        // force abandoned
        ds.setRemoveAbandonedTimeout(Duration.ZERO);
        ds.setMaxTotal(1);
        ds.setAccessToUnderlyingConnectionAllowed(true);

        try (Connection conn1 = getConnection()) {
            assertNotNull(conn1);
            assertEquals(1, ds.getNumActive());

            try (Connection conn2 = getConnection()) {
                // Attempt to borrow object triggers abandoned cleanup
                // conn1 should be closed by the pool to make room
                assertNotNull(conn2);
                assertEquals(1, ds.getNumActive());
                // Verify that conn1 is closed
                assertTrue(((DelegatingConnection<?>) conn1).getInnermostDelegate().isClosed());
                // Verify that conn1 is aborted
                final TesterConnection tCon = (TesterConnection) ((DelegatingConnection<?>) conn1).getInnermostDelegate();
                assertTrue(tCon.isAborted());

            }
            assertEquals(0, ds.getNumActive());

            // Second close on conn1 is OK as of dbcp 1.3
        }
        assertEquals(0, ds.getNumActive());
        final String string = sw.toString();
        assertTrue(string.contains("testAbandonedClose"), string);
    }

    @Test
    void testAbandonedCloseWithExceptions() throws Exception {
        // force abandoned
        ds.setRemoveAbandonedTimeout(Duration.ZERO);
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
            // ignore
        }
        assertEquals(0, ds.getNumActive());
        final String string = sw.toString();
        assertTrue(string.contains("testAbandonedCloseWithExceptions"), string);
    }

    @Test
    void testAbandonedStackTraces() throws Exception {
        // force abandoned
        ds.setRemoveAbandonedTimeout(Duration.ZERO);
        ds.setMaxTotal(1);
        ds.setAccessToUnderlyingConnectionAllowed(true);
        ds.setAbandonedUsageTracking(true);

        try (Connection conn1 = getConnection()) {
            assertNotNull(conn1);
            assertEquals(1, ds.getNumActive());
            // Use the connection
            try (Statement stmt = conn1.createStatement()) {
                assertNotNull(stmt);
                stmt.execute("SELECT 1 FROM DUAL");
            }

            try (Connection conn2 = getConnection()) {
                // Attempt to borrow object triggers abandoned cleanup
                // conn1 should be closed by the pool to make room
                assertNotNull(conn2);
                assertEquals(1, ds.getNumActive());
                // Verify that conn1 is closed
                assertTrue(((DelegatingConnection<?>) conn1).getInnermostDelegate().isClosed());
                // Verify that conn1 is aborted
                final TesterConnection tCon = (TesterConnection) ((DelegatingConnection<?>) conn1).getInnermostDelegate();
                assertTrue(tCon.isAborted());

            }
            assertEquals(0, ds.getNumActive());
        }
        assertEquals(0, ds.getNumActive());
        final String stackTrace = sw.toString();
        assertTrue(stackTrace.contains("testAbandonedStackTraces"), stackTrace);
        assertTrue(stackTrace.contains("Pooled object created"), stackTrace);
        assertTrue(stackTrace.contains("The last code to use this object was:"), stackTrace);
    }

    /**
     * DBCP-180 - verify that a GC can clean up an unused Statement when it is no longer referenced even when it is tracked via the AbandonedTrace mechanism.
     */
    @Test
    void testGarbageCollectorCleanUp01() throws Exception {
        try (DelegatingConnection<?> conn = (DelegatingConnection<?>) ds.getConnection()) {
            Assertions.assertEquals(0, conn.getTrace().size());
            createStatement(conn);
            Assertions.assertEquals(1, conn.getTrace().size());
            System.gc();
            Assertions.assertEquals(0, conn.getTrace().size());
        }
    }

    /**
     * DBCP-180 - things get more interesting with statement pooling.
     */
    @Test
    void testGarbageCollectorCleanUp02() throws Exception {
        ds.setPoolPreparedStatements(true);
        ds.setAccessToUnderlyingConnectionAllowed(true);
        final DelegatingConnection<?> conn = (DelegatingConnection<?>) ds.getConnection();
        final PoolableConnection poolableConn = (PoolableConnection) conn.getDelegate();
        final PoolingConnection poolingConn = (PoolingConnection) poolableConn.getDelegate();
        final KeyedObjectPool<PStmtKey, DelegatingPreparedStatement> gkop = poolingConn.getStatementPool();
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

    /**
     * Verify that lastUsed property is updated when a connection creates or prepares a statement
     */
    @Test
    void testLastUsed() throws Exception {
        ds.setRemoveAbandonedTimeout(Duration.ofSeconds(1));
        ds.setMaxTotal(2);
        try (Connection conn1 = ds.getConnection()) {
            Thread.sleep(500);
            try (Statement s = conn1.createStatement()) {
                // Should reset lastUsed
            }
            Thread.sleep(800);
            final Connection conn2 = ds.getConnection(); // triggers abandoned cleanup
            try (Statement s = conn1.createStatement()) {
                // Should still be OK
            }
            conn2.close();
            Thread.sleep(500);
            try (PreparedStatement ps = conn1.prepareStatement("SELECT 1 FROM DUAL")) {
                // reset
            }
            Thread.sleep(800);
            try (Connection c = ds.getConnection()) {
                // trigger abandoned cleanup again
            }
            try (Statement s = conn1.createStatement()) {
                // empty
            }
        }
    }

    /**
     * DBCP-343 - verify that using a DelegatingStatement updates the lastUsed on the parent connection
     */
    @Test
    void testLastUsedLargePreparedStatementUse() throws Exception {
        ds.setRemoveAbandonedTimeout(Duration.ofSeconds(1));
        ds.setMaxTotal(2);
        try (Connection conn1 = ds.getConnection(); Statement st = conn1.createStatement()) {
            final String querySQL = "SELECT 1 FROM DUAL";
            Thread.sleep(500);
            try (ResultSet rs = st.executeQuery(querySQL)) {
                Assertions.assertNotNull(rs); // Should reset lastUsed
            }
            Thread.sleep(800);
            try (final Connection conn2 = ds.getConnection()) { // triggers abandoned cleanup
                try (ResultSet rs = st.executeQuery(querySQL)) {
                    Assertions.assertNotNull(rs); // Should still be OK
                }
            }
            Thread.sleep(500);
            st.executeLargeUpdate(""); // Should also reset
            Thread.sleep(800);
            try (Connection c = ds.getConnection()) {
                // trigger abandoned cleanup again
            }
            try (Statement s = conn1.createStatement()) {
                // Connection should still be good
            }
        }
    }

    /**
     * Verify that lastUsed property is updated when a connection prepares a callable statement.
     */
    @Test
    void testLastUsedPrepareCall() throws Exception {
        ds.setRemoveAbandonedTimeout(Duration.ofSeconds(1));
        ds.setMaxTotal(2);
        try (Connection conn1 = ds.getConnection()) {
            Thread.sleep(500);
            try (CallableStatement cs = conn1.prepareCall("{call home}")) {
                // Should reset lastUsed
            }
            Thread.sleep(800);
            final Connection conn2 = ds.getConnection(); // triggers abandoned cleanup
            try (CallableStatement cs = conn1.prepareCall("{call home}")) {
                // Should still be OK
            }
            conn2.close();
            Thread.sleep(500);
            try (CallableStatement cs = conn1.prepareCall("{call home}")) {
                // reset
            }
            Thread.sleep(800);
            try (Connection c = ds.getConnection()) {
                // empty
            }
            try (Statement s = conn1.createStatement()) {
                // trigger abandoned cleanup again
            }
        }
    }

    /**
     * DBCP-343 - verify that using a DelegatingStatement updates the lastUsed on the parent connection
     */
    @Test
    void testLastUsedPreparedStatementUse() throws Exception {
        ds.setRemoveAbandonedTimeout(Duration.ofSeconds(1));
        ds.setMaxTotal(2);
        try (Connection conn1 = ds.getConnection(); Statement st = conn1.createStatement()) {
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
            try (Connection c = ds.getConnection()) {
            } // trigger abandoned cleanup again
            try (Statement s = conn1.createStatement()) {
            } // Connection should still be good
        }
    }

    /**
     * DBCP-343 - verify additional operations reset lastUsed on the parent connection
     */
    @Test
    void testLastUsedUpdate() throws Exception {
        try (DelegatingConnection<?> conn = (DelegatingConnection<?>) ds.getConnection();
                final PreparedStatement ps = conn.prepareStatement("");
                final CallableStatement cs = conn.prepareCall("");
                final Statement st = conn.prepareStatement("")) {
            checkLastUsedStatement(ps, conn);
            checkLastUsedPreparedStatement(ps, conn);
            checkLastUsedStatement(cs, conn);
            checkLastUsedPreparedStatement(cs, conn);
            checkLastUsedStatement(st, conn);
        }
    }
}
