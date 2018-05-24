/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.commons.dbcp2.managed;

import com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionManagerImple;
import com.arjuna.ats.jta.common.jtaPropertyManager;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.transaction.RollbackException;
import javax.transaction.Status;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import static org.junit.Assert.fail;

/**
 * @author <a href="mailto:zfeng@redhat.com>Zheng Feng</a>
 */
public class TestConnectionWithNarayana {
    private static final String CREATE_STMT = "CREATE TABLE TEST_DATA (KEY VARCHAR(100), ID BIGINT, VALUE DOUBLE PRECISION, INFO TEXT, TS TIMESTAMP)";
    private static final String INSERT_STMT = "INSERT INTO TEST_DATA   (KEY, ID, VALUE, INFO, TS) VALUES (?,?,?,?,?)";
	private static final String SELECT_STMT = "SELECT KEY, ID, VALUE, INFO, TS FROM TEST_DATA LIMIT 1";
	private static String PAYLOAD;
    private static final String DROP_STMT = "DROP TABLE TEST_DATA";

	static {
		StringBuffer sb = new StringBuffer();
		sb.append("Start");
		sb.append("payload");
		for (int i = 0; i < 10000; i++) {
			sb.append("...");
			sb.append(String.valueOf(i));
		}
		sb.append("End");
		sb.append("payload");

		PAYLOAD = sb.toString();
	}

    private BasicManagedDataSource mds = null;

    @Before
    public void setUp() throws Exception {
        jtaPropertyManager.getJTAEnvironmentBean()
                .setLastResourceOptimisationInterfaceClassName("org.apache.commons.dbcp2.managed.LocalXAConnectionFactory$LocalXAResource");
        mds  = new BasicManagedDataSource();
        mds.setTransactionManager(new TransactionManagerImple());
        mds.setDriverClassName("org.h2.Driver");
        mds.setUrl("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");

        mds.setMaxTotal(80);
        mds.setMinIdle(0);
        mds.setMaxIdle(80);
        mds.setMinEvictableIdleTimeMillis(10000);
        mds.setTimeBetweenEvictionRunsMillis(10000);
        mds.setLogAbandoned(true);
        mds.setMaxWaitMillis(2000);
        mds.setRemoveAbandonedOnMaintenance(true);
        mds.setRemoveAbandonedOnBorrow(true);

        mds.setRemoveAbandonedTimeout(10);
        mds.setLogExpiredConnections(true);
        mds.setLifo(false);

        Connection conn = mds.getConnection();
        PreparedStatement ps = conn.prepareStatement(CREATE_STMT);
        ps.execute();
        ps.close();
        conn.close();
    }

    @Test
    public void testRepeatedGetConnectionInTimeout() throws Exception {
        Connection conn = null;
        mds.getTransactionManager().setTransactionTimeout(1);
        mds.getTransactionManager().begin();

        try {
            do {
                Thread.currentThread().sleep(1000);
            } while (mds.getTransactionManager().getTransaction().getStatus() != Status.STATUS_ROLLEDBACK);
            // Let the reaper do it's thing
            Thread.currentThread().sleep(1000);
            try {
                conn = mds.getConnection();
                fail("Should not get the connection");
            } catch (SQLException e) {
                if (!e.getCause().getClass().equals(IllegalStateException.class)) {
                    throw e;
                }
                try {
                    conn = mds.getConnection();
                    fail("Should not get connection 2");
                } catch (SQLException e2) {
                    if (!e2.getCause().getClass().equals(IllegalStateException.class)) {
                        throw e2;
                    }
                }
            }
            if (conn != null) {
                conn.close();
            }
        } finally {
            mds.getTransactionManager().rollback();
        }
        Assert.assertEquals(0, mds.getNumActive());
    }

    @Test
    public void testConnCommitAfterTimeout() throws Exception {
        Connection conn = null;

        try {
            mds.getTransactionManager().setTransactionTimeout(1);
            mds.getTransactionManager().begin();
            conn = mds.getConnection();
            do {
                Thread.currentThread().sleep(1000);
            } while (mds.getTransactionManager().getTransaction().getStatus() != Status.STATUS_ROLLEDBACK);
            // Let the reaper do it's thing
            Thread.currentThread().sleep(1000);
            try {
                conn.commit();
                fail("should not work after timeout");
            } catch (SQLException e) {
                // Expected
                Assert.assertEquals("Commit can not be set while enrolled in a transaction", e.getMessage());
            }
            mds.getTransactionManager().rollback();

        } finally {
            if (conn != null) conn.close();
        }

        Assert.assertEquals(0, mds.getNumActive());
    }

    @Test
    public void testConnectionInTimeout() throws Exception {
        Connection conn = null;
        PreparedStatement ps = null;
        int i = 0;

        while(i++ < 5) {
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
                    if(mds.getTransactionManager().getTransaction().getStatus() != Status.STATUS_ACTIVE) {
                        n ++;
                    }

                    Connection c = null;
                    PreparedStatement ps2 = null;
                    ResultSet rs = null;
                    try {
                        c = mds.getConnection();
                        ps2 = c.prepareStatement(SELECT_STMT);
                        rs = ps2.executeQuery();
                    } finally {
                        if (rs != null) rs.close();
                        if (ps2 != null) ps2.close();
                        if (c != null) c.close();
                    }
                } while (n < 2);

                ps.close();
                ps = null;
                conn.close();
                conn = null;

                try {
                    mds.getTransactionManager().commit();
                    fail("Should not have been able to commit");
                } catch (RollbackException e) {
                    // this is expected
                    if (mds.getTransactionManager().getTransaction() != null) {
                        // Need to pop it off the thread if a background thread rolled the transaction back
                        mds.getTransactionManager().rollback();
                    }
                }
            } catch (Exception e) {
                if (mds.getTransactionManager().getTransaction() != null) {
                    // Need to pop it off the thread if a background thread rolled the transaction back
                    mds.getTransactionManager().rollback();
                }
            } finally {
                if (ps != null) ps.close();
                if (conn != null) conn.close();
            }
            Assert.assertEquals(0, mds.getNumActive());
        }
    }

    @After
    public void tearDown() throws Exception {
        Connection conn = mds.getConnection();
        PreparedStatement ps = conn.prepareStatement(DROP_STMT);
        ps.execute();
        ps.close();
        conn.close();
        if (mds != null) {
            mds.close();
        }
    }
}
