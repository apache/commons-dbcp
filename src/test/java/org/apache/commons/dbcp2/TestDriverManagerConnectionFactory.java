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

import javax.sql.DataSource;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * This test *must* execute before all other tests to be effective as it tests
 * the initialisation of DriverManager.
 * Based on the test case for DBCP-212 written by Marcos Sanz
 *
 * @version $Revision$ $Date$
 */
public class TestDriverManagerConnectionFactory extends TestCase {

    public TestDriverManagerConnectionFactory(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(TestDriverManagerConnectionFactory.class);
    }

    public void testDriverManagerInit() throws Exception {
        System.setProperty("jdbc.drivers",
                "org.apache.commons.dbcp2.TesterDriver");
        GenericObjectPoolConfig config = new GenericObjectPoolConfig();
        config.setMaxTotal(10);
        config.setMaxIdle(0);
        final ConnectionFactory connectionFactory =
            new DriverManagerConnectionFactory(
                    "jdbc:apache:commons:testdriver",
                    "foo", "bar");
        final PoolableConnectionFactory poolableConnectionFactory =
            new PoolableConnectionFactory(connectionFactory, null);
        poolableConnectionFactory.setDefaultReadOnly(Boolean.FALSE);
        poolableConnectionFactory.setDefaultAutoCommit(Boolean.TRUE);

        GenericObjectPool<PoolableConnection> connectionPool =
                new GenericObjectPool<>(poolableConnectionFactory, config);
        poolableConnectionFactory.setPool(connectionPool);
        PoolingDataSource<PoolableConnection> dataSource =
                new PoolingDataSource<>(connectionPool);

        ConnectionThread[] connectionThreads = new ConnectionThread[10];
        Thread[] threads = new Thread[10];

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
                fail("Exception during getConnection");
            }
        }
    }

    private static final class ConnectionThread implements Runnable {
        private final DataSource ds;
        private volatile boolean result = true;

        private ConnectionThread(DataSource ds) {
            this.ds = ds;
        }

        @Override
        public void run() {
            Connection conn = null;
            try {
                conn = ds.getConnection();
            } catch (Exception e) {
                e.printStackTrace();
                result = false;
            } finally {
                if (conn != null) {
                    try {
                        conn.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                        result = false;
                    }
                }
            }
        }

        public boolean getResult() {
            return result;
        }
    }

}
