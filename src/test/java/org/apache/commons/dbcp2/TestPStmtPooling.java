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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.SQLException;

import javax.management.ObjectName;
import javax.sql.DataSource;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.Assert;

/**
 * TestSuite for BasicDataSource with prepared statement pooling enabled
 *
 * @author Dirk Verbeeck
 * @version $Revision$ $Date$
 */
public class TestPStmtPooling extends TestCase {
    public TestPStmtPooling(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(TestPStmtPooling.class);
    }

    public void testStmtPool() throws Exception {
        DriverManager.registerDriver(new TesterDriver());
        ConnectionFactory connFactory = new DriverManagerConnectionFactory(
                "jdbc:apache:commons:testdriver","u1","p1");

        PoolableConnectionFactory pcf =
            new PoolableConnectionFactory(connFactory, null);
        pcf.setPoolStatements(true);
        pcf.setDefaultReadOnly(Boolean.FALSE);
        pcf.setDefaultAutoCommit(Boolean.TRUE);
        ObjectPool<PoolableConnection> connPool = new GenericObjectPool<>(pcf);
        pcf.setPool(connPool);

        DataSource ds = new PoolingDataSource<>(connPool);

        try (Connection conn = ds.getConnection()) {
            Statement stmt1 = conn.prepareStatement("select 1 from dual");
            Statement ustmt1 = ((DelegatingStatement) stmt1).getInnermostDelegate();
            stmt1.close();
            Statement stmt2 = conn.prepareStatement("select 1 from dual");
            Statement ustmt2 = ((DelegatingStatement) stmt2).getInnermostDelegate();
            stmt2.close();
            assertSame(ustmt1, ustmt2);
        }
    }

    public void testCallableStatementPooling() throws Exception {
        DriverManager.registerDriver(new TesterDriver());
        ConnectionFactory connFactory = new DriverManagerConnectionFactory(
                "jdbc:apache:commons:testdriver","u1","p1");

        ObjectName oName = new ObjectName("UnitTests:DataSource=test");
        PoolableConnectionFactory pcf =
            new PoolableConnectionFactory(connFactory, oName);
        pcf.setPoolStatements(true);
        pcf.setDefaultReadOnly(Boolean.FALSE);
        pcf.setDefaultAutoCommit(Boolean.TRUE);

        GenericObjectPoolConfig config = new GenericObjectPoolConfig();
        config.setJmxNameBase("UnitTests:DataSource=test,connectionpool=connections");
        config.setJmxNamePrefix("");
        ObjectPool<PoolableConnection> connPool =
                new GenericObjectPool<>(pcf, config);
        pcf.setPool(connPool);

        DataSource ds = new PoolingDataSource<>(connPool);

        try (Connection conn = ds.getConnection()) {
            Statement stmt1 = conn.prepareStatement("select 1 from dual");
            Statement ustmt1 = ((DelegatingStatement) stmt1).getInnermostDelegate();
            Statement cstmt1 = conn.prepareCall("{call home}");
            Statement ucstmt1 = ((DelegatingStatement) cstmt1).getInnermostDelegate();
            stmt1.close();  // Return to pool
            cstmt1.close(); // ""
            Statement stmt2 = conn.prepareStatement("select 1 from dual"); // Check out from pool
            Statement ustmt2 = ((DelegatingStatement) stmt2).getInnermostDelegate();
            Statement cstmt2 = conn.prepareCall("{call home}");
            Statement ucstmt2 = ((DelegatingStatement) cstmt2).getInnermostDelegate();
            stmt2.close();  // Return to pool
            cstmt2.close(); // ""
            assertSame(ustmt1, ustmt2);
            assertSame(ucstmt1, ucstmt2);
            // Verify key distinguishes Callable from Prepared Statements in the pool
            Statement stmt3 = conn.prepareCall("select 1 from dual");
            Statement ustmt3 = ((DelegatingStatement) stmt3).getInnermostDelegate();
            stmt3.close();
            assertNotSame(ustmt1, ustmt3);
            assertNotSame(ustmt3, ucstmt1);
        }
    }

    public void testClosePool() throws Exception {
        DriverManager.registerDriver(new TesterDriver());
        ConnectionFactory connFactory = new DriverManagerConnectionFactory(
                "jdbc:apache:commons:testdriver","u1","p1");

        PoolableConnectionFactory pcf =
            new PoolableConnectionFactory(connFactory, null);
        pcf.setPoolStatements(true);
        pcf.setDefaultReadOnly(Boolean.FALSE);
        pcf.setDefaultAutoCommit(Boolean.TRUE);

        ObjectPool<PoolableConnection> connPool = new GenericObjectPool<>(pcf);

        DataSource ds = new PoolingDataSource<>(connPool);
        ((PoolingDataSource<?>) ds).setAccessToUnderlyingConnectionAllowed(true);

        Connection conn = ds.getConnection();
        try (Statement s = conn.prepareStatement("select 1 from dual")) {}

        Connection poolableConnection = ((DelegatingConnection<?>) conn).getDelegate();
        Connection poolingConnection =
            ((DelegatingConnection<?>) poolableConnection).getDelegate();
        poolingConnection.close();
        try (PreparedStatement ps = conn.prepareStatement("select 1 from dual")) {
            fail("Expecting SQLException");
        } catch (SQLException ex) {
            assertTrue(ex.getMessage().endsWith("invalid PoolingConnection."));
        }
    }

    public void testBatchUpdate() throws Exception {
        DriverManager.registerDriver(new TesterDriver());
        ConnectionFactory connFactory = new DriverManagerConnectionFactory(
                "jdbc:apache:commons:testdriver","u1","p1");

        PoolableConnectionFactory pcf =
            new PoolableConnectionFactory(connFactory, null);
        pcf.setPoolStatements(true);
        pcf.setDefaultReadOnly(Boolean.FALSE);
        pcf.setDefaultAutoCommit(Boolean.TRUE);
        ObjectPool<PoolableConnection> connPool = new GenericObjectPool<>(pcf);
        pcf.setPool(connPool);

        DataSource ds = new PoolingDataSource<>(connPool);

        Connection conn = ds.getConnection();
        PreparedStatement ps = conn.prepareStatement("select 1 from dual");
        Statement inner = ((DelegatingPreparedStatement) ps).getInnermostDelegate();
        // Check DBCP-372
        ps.addBatch();
        ps.close();
        conn.close();
        Assert.assertFalse(inner.isClosed());
    }
}
