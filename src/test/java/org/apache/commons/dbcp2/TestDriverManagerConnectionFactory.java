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

import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * This test *must* execute before all other tests to be effective as it tests
 * the initialisation of DriverManager.
 * Based on the test case for DBCP-212 written by Marcos Sanz
 */
public class TestDriverManagerConnectionFactory {

    private static final String KEY_JDBC_DRIVERS = "jdbc.drivers";

    @BeforeClass
    public static void beforeClass() {
        System.setProperty(KEY_JDBC_DRIVERS, "org.apache.commons.dbcp2.TesterDriver");
    }

    @AfterClass
    public static void afterClass() {
        System.clearProperty(KEY_JDBC_DRIVERS);
    }

    @Test
    public void testDriverManagerInitWithEmptyProperties() throws Exception {
        final ConnectionFactory connectionFactory = new DriverManagerConnectionFactory(
                "jdbc:apache:commons:testdriver;user=foo;password=bar");
        connectionFactory.createConnection();
    }

    @Test
    public void testDriverManagerInitWithProperties() throws Exception {
        testDriverManagerInit(true);
    }

    @Test
    public void testDriverManagerInitWithCredentials() throws Exception {
        testDriverManagerInit(false);
    }

    @Test(expected=IndexOutOfBoundsException.class) // thrown by TestDriver due to missing user
    public void testDriverManagerWithoutUser() throws SQLException {
        final DriverManagerConnectionFactory cf = new DriverManagerConnectionFactory("jdbc:apache:commons:testdriver", null, "pass");
        cf.createConnection();
    }

    @Test(expected=SQLException.class) // thrown by TestDriver due to invalid password
    public void testDriverManagerWithoutPassword() throws SQLException {
        final DriverManagerConnectionFactory cf = new DriverManagerConnectionFactory("jdbc:apache:commons:testdriver", "user", null);
        cf.createConnection();
    }

    @Test(expected=ArrayIndexOutOfBoundsException.class) // thrown by TestDriver due to missing user
    public void testDriverManagerWithoutCredentials() throws SQLException {
        final DriverManagerConnectionFactory cf = new DriverManagerConnectionFactory("jdbc:apache:commons:testdriver", null,  null);
        cf.createConnection();
    }

    @Test
    public void testDriverManagerCredentialsInUrl() throws SQLException {
        final DriverManagerConnectionFactory cf = new DriverManagerConnectionFactory("jdbc:apache:commons:testdriver;user=foo;password=bar", null,  null);
        cf.createConnection();
    }

    public void testDriverManagerInit(final boolean withProperties) throws Exception {
        final GenericObjectPoolConfig<PoolableConnection> config = new GenericObjectPoolConfig<>();
        config.setMaxTotal(10);
        config.setMaxIdle(0);
        final Properties properties = new Properties();
        // The names "user" and "password" are specified in java.sql.DriverManager.getConnection(String, String, String)
        properties.put("user", "foo");
        properties.put("password", "bar");
        final ConnectionFactory connectionFactory = withProperties ?
                new DriverManagerConnectionFactory("jdbc:apache:commons:testdriver", properties) :
                new DriverManagerConnectionFactory("jdbc:apache:commons:testdriver", "foo", "bar");
        final PoolableConnectionFactory poolableConnectionFactory =
            new PoolableConnectionFactory(connectionFactory, null);
        poolableConnectionFactory.setDefaultReadOnly(Boolean.FALSE);
        poolableConnectionFactory.setDefaultAutoCommit(Boolean.TRUE);

        final GenericObjectPool<PoolableConnection> connectionPool =
                new GenericObjectPool<>(poolableConnectionFactory, config);
        poolableConnectionFactory.setPool(connectionPool);
        final PoolingDataSource<PoolableConnection> dataSource =
                new PoolingDataSource<>(connectionPool);

        final ConnectionThread[] connectionThreads = new ConnectionThread[10];
        final Thread[] threads = new Thread[10];

        for (int i = 0; i < 10; i++) {
            connectionThreads[i] = new ConnectionThread(dataSource);
            threads[i] = new Thread(connectionThreads[i]);
        }
        for (int i = 0; i < 10; i++) {
            threads[i].start();
        }
        for (int i = 0; i < 10; i++) {
            while (threads[i].isAlive()){//JDK1.5: getState() != Thread.State.TERMINATED) {
                Thread.sleep(100);
            }
            if (!connectionThreads[i].getResult()) {
                fail("Exception during getConnection(): " + connectionThreads[i]);
            }
        }
    }

    private static final class ConnectionThread implements Runnable {
        private final DataSource ds;
        private volatile boolean result = true;

        private ConnectionThread(final DataSource ds) {
            this.ds = ds;
        }

        @Override
        public void run() {
            Connection conn = null;
            try {
                conn = ds.getConnection();
            } catch (final Exception e) {
                e.printStackTrace();
                result = false;
            } finally {
                if (conn != null) {
                    try {
                        conn.close();
                    } catch (final Exception e) {
                        e.printStackTrace();
                        result = false;
                    }
                }
            }
        }

        public boolean getResult() {
            return result;
        }

        @Override
        public String toString() {
            return "ConnectionThread [ds=" + ds + ", result=" + result + "]";
        }
    }

}
