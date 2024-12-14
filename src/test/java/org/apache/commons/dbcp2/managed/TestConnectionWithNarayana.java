/*

  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.apache.commons.dbcp2.managed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;

import javax.transaction.RollbackException;
import javax.transaction.Status;

import org.apache.commons.dbcp2.Utils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionManagerImple;
import com.arjuna.ats.jta.common.jtaPropertyManager;

/**
 * Requires Java 8 or above.
 */
public class TestConnectionWithNarayana {
    private static final String CREATE_STMT = "CREATE TABLE TEST_DATA (KEY1 VARCHAR(100), ID BIGINT, VALUE1 DOUBLE PRECISION, INFO TEXT, TS TIMESTAMP)";
    private static final String INSERT_STMT = "INSERT INTO TEST_DATA   (KEY1, ID, VALUE1, INFO, TS) VALUES (?,?,?,?,?)";
    private static final String SELECT_STMT = "SELECT KEY1, ID, VALUE1, INFO, TS FROM TEST_DATA LIMIT 1";
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

    @BeforeEach
    public void setUp() throws Exception {
        jtaPropertyManager.getJTAEnvironmentBean().setLastResourceOptimisationInterfaceClassName(
                "org.apache.commons.dbcp2.managed.LocalXAConnectionFactory$LocalXAResource");
        mds = new BasicManagedDataSource();
        mds.setTransactionManager(new TransactionManagerImple());
        mds.setDriverClassName("org.h2.Driver");
        mds.setUrl("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");

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
            }
        }
    }

    @AfterEach
    public void tearDown() throws Exception {
        try (final Connection conn = mds.getConnection()) {
            try (final PreparedStatement ps = conn.prepareStatement(DROP_STMT)) {
                ps.execute();
            }
        }
        Utils.closeQuietly(mds);
    }

    @Test
	public void testConnectionCommitAfterTimeout() throws Exception {
		mds.getTransactionManager().setTransactionTimeout(1);
		mds.getTransactionManager().begin();
		try (Connection conn = mds.getConnection()) {
			do {
				Thread.sleep(1000);
			} while (mds.getTransactionManager().getTransaction().getStatus() != Status.STATUS_ROLLEDBACK);
			// Let the reaper do it's thing
			Thread.sleep(1000);
			final SQLException e = assertThrows(SQLException.class, conn::commit);
			assertEquals("Commit cannot be set while enrolled in a transaction", e.getMessage(),
					"Should not work after timeout");
			mds.getTransactionManager().rollback();
		}
		assertEquals(0, mds.getNumActive());
	}

    @Test
    public void testConnectionInTimeout() throws Exception {
        Connection conn = null;
        PreparedStatement ps = null;
        for (int i = 0; i < 5; i++) {
            try {
                mds.getTransactionManager().setTransactionTimeout(1);
                mds.getTransactionManager().begin();

                conn = mds.getConnection();
                ps = conn.prepareStatement(INSERT_STMT);
                ps.setString(1, Thread.currentThread().getName());
                ps.setLong(2, i);
                ps.setDouble(3, new java.util.Random().nextDouble());
                ps.setString(4, PAYLOAD);
                ps.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
                ps.execute();

                int n = 0;
                do {
                    if (mds.getTransactionManager().getTransaction().getStatus() != Status.STATUS_ACTIVE) {
                        n++;
                    }
                    try (Connection c = mds.getConnection(); PreparedStatement ps2 = c.prepareStatement(SELECT_STMT); ResultSet rs = ps2.executeQuery()) {
                        // nothing here, all auto-close.
                    }
                } while (n < 2);

                ps.close();
                ps = null;
                conn.close();
                conn = null;

                try {
                    mds.getTransactionManager().commit();
                    fail("Should not have been able to commit");
                } catch (final RollbackException e) {
                    // this is expected
                    if (mds.getTransactionManager().getTransaction() != null) {
                        // Need to pop it off the thread if a background thread rolled the transaction back
                        mds.getTransactionManager().rollback();
                    }
                }
            } catch (final Exception e) {
                if (mds.getTransactionManager().getTransaction() != null) {
                    // Need to pop it off the thread if a background thread rolled the transaction back
                    mds.getTransactionManager().rollback();
                }
            } finally {
                if (ps != null) {
                    ps.close();
                }
                if (conn != null) {
                    conn.close();
                }
            }
            Assertions.assertEquals(0, mds.getNumActive());
        }
    }

    @Test
    public void testRepeatedGetConnectionInTimeout() throws Exception {
        mds.getTransactionManager().setTransactionTimeout(1);
        mds.getTransactionManager().begin();

        try {
            do {
                Thread.sleep(1000);
            } while (mds.getTransactionManager().getTransaction().getStatus() != Status.STATUS_ROLLEDBACK);
            // Let the reaper do it's thing
            Thread.sleep(1000);
            try (Connection conn = mds.getConnection()) {
                fail("Should not get the connection 1");
            } catch (final SQLException e) {
                if (!e.getCause().getClass().equals(IllegalStateException.class)) {
                    throw e;
                }
                try (Connection conn = mds.getConnection()) {
                    fail("Should not get connection 2");
                } catch (final SQLException e2) {
                    if (!e2.getCause().getClass().equals(IllegalStateException.class)) {
                        throw e2;
                    }
                }
            }
        } finally {
            mds.getTransactionManager().rollback();
        }
        Assertions.assertEquals(0, mds.getNumActive());
    }
}