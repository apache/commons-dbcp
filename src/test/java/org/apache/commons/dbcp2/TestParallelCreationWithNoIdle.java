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

import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test if the pooling if no idle objects are used
 */
public class TestParallelCreationWithNoIdle  {


    protected BasicDataSource ds = null;
    private static final String CATALOG = "test catalog";

    @BeforeAll
    public static void setUpClass() {
        // register a custom logger which supports inspection of the log messages
        LogFactory.getFactory().setAttribute("org.apache.commons.logging.Log", "org.apache.commons.dbcp2.StackMessageLog");
    }

    @BeforeEach
    public void setUp() throws Exception {
        ds = new BasicDataSource();
        ds.setDriverClassName("org.apache.commons.dbcp2.TesterConnectionDelayDriver");
        ds.setUrl("jdbc:apache:commons:testerConnectionDelayDriver:50");
        ds.setMaxTotal(10);

        // this one is actually very important.
        // see DBCP-513
        ds.setMaxIdle(0);

        // wait a minute. Usually the test runs in ~ 1 second
        // but often it's getting stuck ^^
        // you have one second to get a thread dump ;)
        ds.setMaxWaitMillis(60000);

        ds.setDefaultAutoCommit(Boolean.TRUE);
        ds.setDefaultReadOnly(Boolean.FALSE);
        ds.setDefaultTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        ds.setDefaultCatalog(CATALOG);
        ds.setUsername("userName");
        ds.setPassword("password");
        ds.setValidationQuery("SELECT DUMMY FROM DUAL");
        ds.setConnectionInitSqls(Arrays.asList(new String[] { "SELECT 1", "SELECT 2"}));
        ds.setDriverClassLoader(new TesterClassLoader());
        ds.setJmxName("org.apache.commons.dbcp2:name=test");
    }

    /**
     * Fire up 100 Threads but only have 10 maxActive and forcedBlock.
     * See
     * @throws Exception
     */
    @Test
    public void testMassiveConcurrentInitBorrow() throws Exception {
        final int numThreads = 200;
        ds.setDriverClassName("org.apache.commons.dbcp2.TesterConnectionDelayDriver");
        ds.setUrl("jdbc:apache:commons:testerConnectionDelayDriver:20");
        ds.setInitialSize(8);
        final List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

        final Thread[] threads = new Thread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            threads[i] = new TestThread(2, 0, 50);
            threads[i].setUncaughtExceptionHandler((t, e) -> errors.add(e));
        }

        for (int i = 0; i < numThreads; i++) {
            threads[i].start();

            if (i%4 == 0) {
                Thread.sleep(20);
            }
        }

        for (int i = 0; i < numThreads; i++) {
            threads[i].join();
        }

        assertEquals(0, errors.size());
        ds.close();
    }



    class TestThread extends Thread {
        java.util.Random _random = new java.util.Random();
        final int iter;
        final int delay;
        final int delayAfter;


        public TestThread(final int iter, final int delay, final int delayAfter) {
            this.iter = iter;
            this.delay = delay;
            this.delayAfter = delayAfter;
        }


        @Override
        public void run() {
            // System.out.println("Thread started " + Thread.currentThread().toString());
            for (int i = 0; i < iter; i++) {
                sleepMax(delay);
                try (Connection conn = ds.getConnection();
                        PreparedStatement stmt = conn.prepareStatement("select 'literal', SYSDATE from dual");) {
                    // System.out.println("Got Connection " + Thread.currentThread().toString());
                    final ResultSet rset = stmt.executeQuery();
                    rset.next();
                    sleepMax(delayAfter);
                    rset.close();
                } catch (final Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }
            // System.out.println("Thread done " + Thread.currentThread().toString());
        }

        private void sleepMax(final int timeMax) {
            if (timeMax == 0) {
                return;
            }
            try {
                Thread.sleep(_random.nextInt(timeMax));
            } catch(final Exception e) {
                // ignored
            }
        }
    }

}
