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
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;

import org.apache.commons.pool2.KeyedObjectPool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * TestSuite for BasicDataSource with prepared statement pooling enabled
 */
public class TestPStmtPoolingBasicDataSource extends TestBasicDataSource {

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        // PoolPreparedStatements enabled, should not affect the basic tests
        ds.setPoolPreparedStatements(true);
        ds.setMaxOpenPreparedStatements(2);
    }

    /**
     * Verifies that the prepared statement pool behaves as an LRU cache,
     * closing least-recently-used statements idle in the pool to make room
     * for new ones if necessary.
     */
    @Test
    public void testLRUBehavior() throws Exception {
        ds.setMaxOpenPreparedStatements(3);

        final Connection conn = getConnection();
        assertNotNull(conn);

        // Open 3 statements and then close them into the pool
        final PreparedStatement stmt1 = conn.prepareStatement("select 'a' from dual");
        final PreparedStatement inner1 = (PreparedStatement) ((DelegatingPreparedStatement) stmt1).getInnermostDelegate();
        final PreparedStatement stmt2 = conn.prepareStatement("select 'b' from dual");
        final PreparedStatement inner2 = (PreparedStatement) ((DelegatingPreparedStatement) stmt2).getInnermostDelegate();
        final PreparedStatement stmt3 = conn.prepareStatement("select 'c' from dual");
        final PreparedStatement inner3 = (PreparedStatement) ((DelegatingPreparedStatement) stmt3).getInnermostDelegate();
        stmt1.close();
        Thread.sleep(100); // Make sure return timestamps are different
        stmt2.close();
        Thread.sleep(100);
        stmt3.close();

        // Pool now has three idle statements, getting another one will force oldest (stmt1) out
        final PreparedStatement stmt4 = conn.prepareStatement("select 'd' from dual");
        assertNotNull(stmt4);

        // Verify that inner1 has been closed
        try {
            inner1.clearParameters();
            fail("expecting SQLExcption - statement should be closed");
        } catch (final SQLException ex) {
            //Expected
        }
        // But others are still open
        inner2.clearParameters();
        inner3.clearParameters();

        // Now make sure stmt1 does not come back from the dead
        final PreparedStatement stmt5 = conn.prepareStatement("select 'a' from dual");
        final PreparedStatement inner5 = (PreparedStatement) ((DelegatingPreparedStatement) stmt5).getInnermostDelegate();
        assertNotSame(inner5, inner1);

        // inner2 should be closed now
        try {
            inner2.clearParameters();
            fail("expecting SQLExcption - statement should be closed");
        } catch (final SQLException ex) {
            //Expected
        }
        // But inner3 should still be open
        inner3.clearParameters();
    }

    /**
     * Tests high-concurrency contention for connections and pooled prepared statements.
     * DBCP-414
     */
    @Test
    public void testMultipleThreads1() throws Exception {
        ds.setMaxWait(Duration.ofMillis(-1));
        ds.setMaxTotal(5);
        ds.setMaxOpenPreparedStatements(-1);
        multipleThreads(Duration.ofMillis(5), false, false, Duration.ofMillis(-1), 3, 100, 10000);
    }

    @Test
    public void testPreparedStatementPooling() throws Exception {
        final Connection conn = getConnection();
        assertNotNull(conn);

        final PreparedStatement stmt1 = conn.prepareStatement("select 'a' from dual");
        assertNotNull(stmt1);

        final PreparedStatement stmt2 = conn.prepareStatement("select 'b' from dual");
        assertNotNull(stmt2);

        assertNotSame(stmt1, stmt2);

        // go over the maxOpen limit
        try (PreparedStatement ps = conn.prepareStatement("select 'c' from dual")) {
            fail("expected SQLException");
        }
        catch (final SQLException e) {}

        // make idle
        stmt2.close();

        // test cleanup the 'b' statement
        final PreparedStatement stmt3 = conn.prepareStatement("select 'c' from dual");
        assertNotNull(stmt3);
        assertNotSame(stmt3, stmt1);
        assertNotSame(stmt3, stmt2);

        // normal reuse of statement
        stmt1.close();
        try (final PreparedStatement stmt4 = conn.prepareStatement("select 'a' from dual")) {
            assertNotNull(stmt4);
        }
    }

    // Bugzilla Bug 27246
    // PreparedStatement cache should be different depending on the Catalog
    @Test
    public void testPStmtCatalog() throws Exception {
        final Connection conn = getConnection();
        conn.setCatalog("catalog1");
        final DelegatingPreparedStatement stmt1 = (DelegatingPreparedStatement) conn.prepareStatement("select 'a' from dual");
        final TesterPreparedStatement inner1 = (TesterPreparedStatement) stmt1.getInnermostDelegate();
        assertEquals("catalog1", inner1.getCatalog());
        stmt1.close();

        conn.setCatalog("catalog2");
        final DelegatingPreparedStatement stmt2 = (DelegatingPreparedStatement) conn.prepareStatement("select 'a' from dual");
        final TesterPreparedStatement inner2 = (TesterPreparedStatement) stmt2.getInnermostDelegate();
        assertEquals("catalog2", inner2.getCatalog());
        stmt2.close();

        conn.setCatalog("catalog1");
        final DelegatingPreparedStatement stmt3 = (DelegatingPreparedStatement) conn.prepareStatement("select 'a' from dual");
        final TesterPreparedStatement inner3 = (TesterPreparedStatement) stmt3.getInnermostDelegate();
        assertEquals("catalog1", inner3.getCatalog());
        stmt3.close();

        assertNotSame(inner1, inner2);
        assertSame(inner1, inner3);
    }

    @Test
    public void testPStmtPoolingAcrossClose() throws Exception {
        ds.setMaxTotal(1); // only one connection in pool needed
        ds.setMaxIdle(1);
        ds.setAccessToUnderlyingConnectionAllowed(true);
        final Connection conn1 = getConnection();
        assertNotNull(conn1);
        assertEquals(1, ds.getNumActive());
        assertEquals(0, ds.getNumIdle());

        final PreparedStatement stmt1 = conn1.prepareStatement("select 'a' from dual");
        assertNotNull(stmt1);

        final Statement inner1 = ((DelegatingPreparedStatement) stmt1).getInnermostDelegate();
        assertNotNull(inner1);

        stmt1.close();
        conn1.close();

        assertEquals(0, ds.getNumActive());
        assertEquals(1, ds.getNumIdle());

        final Connection conn2 = getConnection();
        assertNotNull(conn2);
        assertEquals(1, ds.getNumActive());
        assertEquals(0, ds.getNumIdle());

        final PreparedStatement stmt2 = conn2.prepareStatement("select 'a' from dual");
        assertNotNull(stmt2);

        final Statement inner2 = ((DelegatingPreparedStatement) stmt2).getInnermostDelegate();
        assertNotNull(inner2);

        assertSame(inner1, inner2);
    }

    /**
     * Tests clearStatementPoolOnReturn introduced with DBCP-566.
     * When turned on, the statement pool must be empty after the connection is closed.
     *
     * @throws Exception
     */
    @Test
    public void testPStmtPoolingAcrossCloseWithClearOnReturn() throws Exception {
        ds.setMaxTotal(1); // only one connection in pool needed
        ds.setMaxIdle(1);
        ds.setClearStatementPoolOnReturn(true);
        ds.setAccessToUnderlyingConnectionAllowed(true);
        final Connection conn1 = getConnection();
        assertNotNull(conn1);
        assertEquals(1, ds.getNumActive());
        assertEquals(0, ds.getNumIdle());

        @SuppressWarnings("unchecked")
        final DelegatingConnection<Connection> poolableConn =
            (DelegatingConnection<Connection>) ((DelegatingConnection<Connection>) conn1).getDelegateInternal();
        final KeyedObjectPool<PStmtKey, DelegatingPreparedStatement> stmtPool =
            ((PoolingConnection) poolableConn.getDelegateInternal()).getStatementPool();

        final PreparedStatement stmt1 = conn1.prepareStatement("select 'a' from dual");
        assertNotNull(stmt1);
        final Statement inner1 = ((DelegatingPreparedStatement) stmt1).getInnermostDelegate();
        assertNotNull(inner1);
        stmt1.close();

        final PreparedStatement stmt2 = conn1.prepareStatement("select 'a' from dual");
        assertNotNull(stmt2);
        final Statement inner2 = ((DelegatingPreparedStatement) stmt2).getInnermostDelegate();
        assertNotNull(inner2);
        assertSame(inner1, inner2); // from the same connection the statement must be pooled
        stmt2.close();

        conn1.close();
        assertTrue(inner1.isClosed());

        assertEquals(0, stmtPool.getNumActive());
        assertEquals(0, stmtPool.getNumIdle());

        assertEquals(0, ds.getNumActive());
        assertEquals(1, ds.getNumIdle());

        final Connection conn2 = getConnection();
        assertNotNull(conn2);
        assertEquals(1, ds.getNumActive());
        assertEquals(0, ds.getNumIdle());

        final PreparedStatement stmt3 = conn2.prepareStatement("select 'a' from dual");
        assertNotNull(stmt3);
        final Statement inner3 = ((DelegatingPreparedStatement) stmt3).getInnermostDelegate();
        assertNotNull(inner3);

        assertNotSame(inner1, inner3); // when acquiring the connection the next time, statement must be new

        conn2.close();
    }

    @Test
    public void testPStmtPoolingWithNoClose() throws Exception {
        ds.setMaxTotal(1); // only one connection in pool needed
        ds.setMaxIdle(1);
        ds.setAccessToUnderlyingConnectionAllowed(true);
        final Connection conn1 = getConnection();
        assertNotNull(conn1);
        assertEquals(1, ds.getNumActive());
        assertEquals(0, ds.getNumIdle());

        final PreparedStatement stmt1 = conn1.prepareStatement("select 'a' from dual");
        assertNotNull(stmt1);

        final Statement inner1 = ((DelegatingPreparedStatement) stmt1).getInnermostDelegate();
        assertNotNull(inner1);

        stmt1.close();

        assertNotNull(conn1);
        assertEquals(1, ds.getNumActive());
        assertEquals(0, ds.getNumIdle());

        final PreparedStatement stmt2 = conn1.prepareStatement("select 'a' from dual");
        assertNotNull(stmt2);

        final Statement inner2 = ((DelegatingPreparedStatement) stmt2).getInnermostDelegate();
        assertNotNull(inner2);

        assertSame(inner1, inner2);
    }
}
