/*

  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at

      https://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.apache.commons.dbcp2.managed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;

import javax.transaction.RollbackException;
import javax.transaction.Status;

import org.apache.commons.dbcp2.TestDatabase;
import org.apache.commons.dbcp2.Utils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.EnumSource;

import com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionManagerImple;
import com.arjuna.ats.jta.common.jtaPropertyManager;

/**
 * Requires Java 8 or above.
 */
@Timeout(30)
@ParameterizedClass(name = "{0}")
@EnumSource(value = TestDatabase.class, names = {"H2", "HSQLDB"})
public class TestConnectionWithNarayana {

    /** Database used by this invocation. */
    @Parameter
    TestDatabase database;
    private static final String CREATE_STMT = "CREATE TABLE TEST_DATA (KEY1 VARCHAR(100), ID BIGINT, VALUE1 DOUBLE PRECISION, INFO VARCHAR(100000), TS TIMESTAMP)";
    private static final String INSERT_STMT = "INSERT INTO TEST_DATA   (KEY1, ID, VALUE1, INFO, TS) VALUES (?,?,?,?,?)";
    private static final String SELECT_STMT = "SELECT KEY1, ID, VALUE1, INFO, TS FROM TEST_DATA";
    private static final String PAYLOAD;
    private static final String DROP_STMT = "DROP TABLE TEST_DATA";

    static {
        final StringBuilder sb = new StringBuilder();
        sb.append("Start");
        sb.append("payload");
        for (int i = 0; i < 10000; i++) {
            sb.append("...");
            sb.append(i);
        }
        sb.append("End");
        sb.append("payload");

        PAYLOAD = sb.toString();
    }

    private BasicManagedDataSource mds;
    /** Keeps the database alive while timeout handling discards pooled connections. */
    private Connection databaseAnchor;
    /** Timeout inherited from the current test thread before this fixture changes it. */
    private int originalTransactionTimeout;
    /** Whether setup created the table that teardown must remove. */
    private boolean tableCreated;

    @BeforeEach
    public void setUp() throws Exception {
        jtaPropertyManager.getJTAEnvironmentBean()
                .setLastResourceOptimisationInterfaceClassName("org.apache.commons.dbcp2.managed.LocalXAConnectionFactory$LocalXAResource");
        final TransactionManagerImple transactionManager = new TransactionManagerImple();
        originalTransactionTimeout = transactionManager.getTimeout();
        mds = new BasicManagedDataSource();
        mds.setTransactionManager(transactionManager);
        database.configure(mds, database.newUrl());
        databaseAnchor = database.createDataSource(mds.getUrl()).getConnection();

        mds.setMaxTotal(80);
        mds.setMinIdle(0);
        mds.setMaxIdle(80);
        mds.setMinEvictableIdle(Duration.ofSeconds(10));
        mds.setDurationBetweenEvictionRuns(Duration.ofSeconds(10));
        mds.setLogAbandoned(true);
        mds.setMaxWait(Duration.ofSeconds(2));
        mds.setRemoveAbandonedOnMaintenance(true);
        mds.setRemoveAbandonedOnBorrow(true);

        mds.setRemoveAbandonedTimeout(Duration.ofSeconds(10));
        mds.setLogExpiredConnections(true);
        mds.setLifo(false);

        try (final Connection conn = mds.getConnection()) {
            try (final PreparedStatement ps = conn.prepareStatement(CREATE_STMT)) {
                ps.execute();
                tableCreated = true;
            }
        }
    }

    @AfterEach
    public void tearDown() throws Exception {
        try (Connection connection = databaseAnchor) {
            if (mds != null && mds.getTransactionManager().getTransaction() != null) {
                mds.getTransactionManager().rollback();
            }
            if (tableCreated) {
                try (PreparedStatement statement = connection.prepareStatement(DROP_STMT)) {
                    statement.execute();
                }
            }
        } finally {
            Utils.closeQuietly(mds);
            if (mds != null) {
                mds.getTransactionManager().setTransactionTimeout(originalTransactionTimeout);
            }
        }
    }

    @Test
    void testConnectionCommitAfterTimeout() throws Exception {
        mds.getTransactionManager().setTransactionTimeout(1);
        mds.getTransactionManager().begin();
        try (Connection conn = mds.getConnection()) {
            do {
                Thread.sleep(1000);
            } while (mds.getTransactionManager().getTransaction().getStatus() != Status.STATUS_ROLLEDBACK);
            // Let the reaper do it's thing
            Thread.sleep(1000);
            final SQLException e = assertThrows(SQLException.class, conn::commit);
            assertEquals("Cannot commit while enlisted in a transaction", e.getMessage(), "Should not work after timeout");
            mds.getTransactionManager().rollback();
        }
        assertEquals(0, mds.getNumActive());
    }

    @Test
    void testConnectionInTimeout() throws Exception {
        for (int i = 0; i < 5; i++) {
            mds.getTransactionManager().setTransactionTimeout(1);
            mds.getTransactionManager().begin();
            try {
                try (Connection conn = mds.getConnection(); PreparedStatement ps = conn.prepareStatement(INSERT_STMT)) {
                    ps.setString(1, Thread.currentThread().getName());
                    ps.setLong(2, i);
                    ps.setDouble(3, new java.util.Random().nextDouble());
                    ps.setString(4, PAYLOAD);
                    ps.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
                    assertEquals(1, ps.executeUpdate());

                    try {
                        int n = 0;
                        do {
                            if (mds.getTransactionManager().getTransaction().getStatus() != Status.STATUS_ACTIVE) {
                                n++;
                            }
                            try (Connection c = mds.getConnection(); PreparedStatement ps2 = c.prepareStatement(SELECT_STMT);
                                    ResultSet rs = ps2.executeQuery()) {
                                // Exercise borrowing while the reaper rolls back the transaction.
                            }
                        } while (n < 2);
                    } catch (final SQLException e) {
                        // Reject unrelated connection or SQL failures before the transaction times out.
                        assertNotEquals(Status.STATUS_ACTIVE, mds.getTransactionManager().getTransaction().getStatus(), e::toString);
                    }
                }
                assertThrows(RollbackException.class, () -> mds.getTransactionManager().commit());
            } finally {
                if (mds.getTransactionManager().getTransaction() != null) {
                    // A background rollback leaves the transaction associated with this thread.
                    mds.getTransactionManager().rollback();
                }
            }
            assertEquals(0, mds.getNumActive());
        }
    }

    @Test
    void testRepeatedGetConnectionInTimeout() throws Exception {
        mds.getTransactionManager().setTransactionTimeout(1);
        mds.getTransactionManager().begin();
        try {
            do {
                Thread.sleep(1000);
            } while (mds.getTransactionManager().getTransaction().getStatus() != Status.STATUS_ROLLEDBACK);
            // Let the reaper do it's thing
            Thread.sleep(1000);
            final SQLException e = assertThrows(SQLException.class, mds::getConnection);
            assertTrue(e.getCause().getClass().equals(IllegalStateException.class));
            final SQLException e2 = assertThrows(SQLException.class, mds::getConnection);
            assertTrue(e2.getCause().getClass().equals(IllegalStateException.class));
        } finally {
            mds.getTransactionManager().rollback();
        }
        assertEquals(0, mds.getNumActive());
    }
}
