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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for a  {@link GenericObjectPool} based {@link PoolingDriver}.
 */
public class TestPoolingDriver extends TestConnectionPool {

    @Override
    protected Connection getConnection() throws Exception {
        return DriverManager.getConnection("jdbc:apache:commons:dbcp:test");
    }

    private PoolingDriver driver = null;

    @BeforeEach
    public void setUp() throws Exception {
        final DriverConnectionFactory cf = new DriverConnectionFactory(new TesterDriver(),"jdbc:apache:commons:testdriver",null);

        final PoolableConnectionFactory pcf = new PoolableConnectionFactory(cf, null);
        pcf.setPoolStatements(true);
        pcf.setMaxOpenPreparedStatements(10);
        pcf.setValidationQuery("SELECT COUNT(*) FROM DUAL");
        pcf.setDefaultReadOnly(Boolean.FALSE);
        pcf.setDefaultAutoCommit(Boolean.TRUE);

        final GenericObjectPoolConfig<PoolableConnection> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(getMaxTotal());
        poolConfig.setMaxWaitMillis(getMaxWaitMillis());
        poolConfig.setMinIdle(10);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setTimeBetweenEvictionRunsMillis(10000L);
        poolConfig.setNumTestsPerEvictionRun(5);
        poolConfig.setMinEvictableIdleTimeMillis(5000L);

        final GenericObjectPool<PoolableConnection> pool = new GenericObjectPool<>(pcf, poolConfig);
        pcf.setPool(pool);

        assertNotNull(pcf);
        driver = new PoolingDriver(true);
        driver.registerPool("test",pool);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        driver.closePool("test");
        super.tearDown();
    }

    @Test
    public void test1() {
        final ConnectionFactory connectionFactory = new DriverManagerConnectionFactory("jdbc:some:connect:string","userName","password");
        final PoolableConnectionFactory pcf =
            new PoolableConnectionFactory(connectionFactory, null);
        pcf.setDefaultReadOnly(Boolean.FALSE);
        pcf.setDefaultAutoCommit(Boolean.TRUE);
        final GenericObjectPool<PoolableConnection> connectionPool =
                new GenericObjectPool<>(pcf);
        pcf.setPool(connectionPool);
        final DataSource ds = new PoolingDataSource<>(connectionPool);
        Assertions.assertNotNull(ds);
    }

    @Test
    public void test2() {
        final ConnectionFactory connectionFactory = new DriverManagerConnectionFactory("jdbc:some:connect:string","userName","password");
        final PoolableConnectionFactory pcf =
            new PoolableConnectionFactory(connectionFactory, null);
        pcf.setDefaultReadOnly(Boolean.FALSE);
        pcf.setDefaultAutoCommit(Boolean.TRUE);
        final GenericObjectPool<PoolableConnection> connectionPool = new GenericObjectPool<>(pcf);
        final PoolingDriver driver2 = new PoolingDriver();
        driver2.registerPool("example",connectionPool);
    }

    /** "http://issues.apache.org/bugzilla/show_bug.cgi?id=28912" */
    @Test
    public void testReportedBug28912() throws Exception {
        final Connection conn1 = getConnection();
        assertNotNull(conn1);
        assertFalse(conn1.isClosed());
        conn1.close();

        final Connection conn2 = getConnection();
        assertNotNull(conn2);

        assertTrue(conn1.isClosed());
        assertFalse(conn2.isClosed());

        // should be able to call close multiple times with no effect
        conn1.close();

        assertTrue(conn1.isClosed());
        assertFalse(conn2.isClosed());
    }

    /** "http://issues.apache.org/bugzilla/show_bug.cgi?id=12400" */
    @Test
    public void testReportedBug12400() throws Exception {
        final GenericObjectPoolConfig<PoolableConnection> config = new GenericObjectPoolConfig<>();
        config.setMaxTotal(70);
        config.setMaxWaitMillis(60000);
        config.setMaxIdle(10);
        final ConnectionFactory connectionFactory = new DriverManagerConnectionFactory(
            "jdbc:apache:commons:testdriver",
            "userName",
            "password");
        final PoolableConnectionFactory poolableConnectionFactory =
            new PoolableConnectionFactory(connectionFactory, null);
        poolableConnectionFactory.setDefaultReadOnly(Boolean.FALSE);
        poolableConnectionFactory.setDefaultAutoCommit(Boolean.TRUE);
        final ObjectPool<PoolableConnection> connectionPool = new GenericObjectPool<>(poolableConnectionFactory,
                config);
        poolableConnectionFactory.setPool(connectionPool);
        assertNotNull(poolableConnectionFactory);
        final PoolingDriver driver2 = new PoolingDriver();
        driver2.registerPool("neusoftim",connectionPool);
        final Connection[] conn = new Connection[25];
        for(int i=0;i<25;i++) {
            conn[i] = DriverManager.getConnection("jdbc:apache:commons:dbcp:neusoftim");
            for(int j=0;j<i;j++) {
                assertTrue(conn[j] != conn[i]);
                assertTrue(!conn[j].equals(conn[i]));
            }
        }
        for(int i=0;i<25;i++) {
            conn[i].close();
        }
    }

    @Test
    public void testClosePool() throws Exception {
        final Connection conn = DriverManager.getConnection("jdbc:apache:commons:dbcp:test");
        assertNotNull(conn);
        conn.close();

        final PoolingDriver driver2 = (PoolingDriver) DriverManager.getDriver("jdbc:apache:commons:dbcp:");
        driver2.closePool("test");

        try (Connection c = DriverManager.getConnection("jdbc:apache:commons:dbcp:test")) {
            fail("expected SQLException");
        }
        catch (final SQLException e) {
            // OK
        }
    }

    @Test
    public void testInvalidateConnection() throws Exception {
        final Connection conn = DriverManager.getConnection("jdbc:apache:commons:dbcp:test");
        assertNotNull(conn);

        final ObjectPool<?> pool = driver.getConnectionPool("test");
        assertEquals(1, pool.getNumActive());
        assertEquals(0, pool.getNumIdle());

        final PoolingDriver driver2 = (PoolingDriver) DriverManager.getDriver("jdbc:apache:commons:dbcp:");
        driver2.invalidateConnection(conn);

        assertEquals(0, pool.getNumActive());
        assertEquals(0, pool.getNumIdle());
        assertTrue(conn.isClosed());
    }

    @Test
    public void testLogWriter() throws Exception {
        final PrintStream ps = new PrintStream(new ByteArrayOutputStream(), false, "UTF-8");
        final PrintWriter pw = new PrintWriter(new OutputStreamWriter(new ByteArrayOutputStream(), "UTF-8"));
        System.setErr(new PrintStream(new ByteArrayOutputStream(), false, "UTF-8"));
        SQLException ex;

        DriverManager.setLogWriter(pw);
        ex = new SQLException("A", new Exception("a"));
        ex.printStackTrace();
        ex.printStackTrace(ps);
        ex.printStackTrace(pw);
        ex = new SQLException("B");
        ex.printStackTrace();
        ex.printStackTrace(ps);
        ex.printStackTrace(pw);
        ex = new SQLException(null, new Exception("c"));
        ex.printStackTrace();
        ex.printStackTrace(ps);
        ex.printStackTrace(pw);
        ex = new SQLException((String)null);
        ex.printStackTrace();
        ex.printStackTrace(ps);
        ex.printStackTrace(pw);

        DriverManager.setLogWriter(null);
        ex = new SQLException("A", new Exception("a"));
        ex.printStackTrace();
        ex.printStackTrace(ps);
        ex.printStackTrace(pw);
        ex = new SQLException("B");
        ex.printStackTrace();
        ex.printStackTrace(ps);
        ex.printStackTrace(pw);
        ex = new SQLException(null, new Exception("c"));
        ex.printStackTrace();
        ex.printStackTrace(ps);
        ex.printStackTrace(pw);
        ex = new SQLException((String)null);
        ex.printStackTrace();
        ex.printStackTrace(ps);
        ex.printStackTrace(pw);
    }
}
