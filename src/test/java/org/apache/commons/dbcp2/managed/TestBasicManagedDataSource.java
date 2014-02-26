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

import java.sql.SQLException;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.dbcp2.TestBasicDataSource;
import org.apache.geronimo.transaction.manager.TransactionManagerImpl;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * TestSuite for BasicManagedDataSource
 */
public class TestBasicManagedDataSource extends TestBasicDataSource {
    public TestBasicManagedDataSource(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(TestBasicManagedDataSource.class);
    }

    @Override
    protected BasicDataSource createDataSource() throws Exception {
        BasicManagedDataSource basicManagedDataSource = new BasicManagedDataSource();
        basicManagedDataSource.setTransactionManager(new TransactionManagerImpl());
        return basicManagedDataSource;
    }

    /**
     * JIRA: DBCP-294
     * Verify that PoolableConnections created by BasicManagedDataSource unregister themselves
     * when reallyClosed.
     */
    public void testReallyClose() throws Exception {
        BasicManagedDataSource basicManagedDataSource = new BasicManagedDataSource();
        basicManagedDataSource.setTransactionManager(new TransactionManagerImpl());
        basicManagedDataSource.setDriverClassName("org.apache.commons.dbcp2.TesterDriver");
        basicManagedDataSource.setUrl("jdbc:apache:commons:testdriver");
        basicManagedDataSource.setUsername("username");
        basicManagedDataSource.setPassword("password");
        basicManagedDataSource.setMaxIdle(1);
        // Create two connections
        ManagedConnection<?> conn = (ManagedConnection<?>) basicManagedDataSource.getConnection();
        assertNotNull(basicManagedDataSource.getTransactionRegistry().getXAResource(conn));
        ManagedConnection<?> conn2 = (ManagedConnection<?>) basicManagedDataSource.getConnection();
        conn2.close(); // Return one connection to the pool
        conn.close();  // No room at the inn - this will trigger reallyClose(), which should unregister
        try {
            basicManagedDataSource.getTransactionRegistry().getXAResource(conn);
            fail("Expecting SQLException - XAResources orphaned");
        } catch (SQLException ex) {
            // expected
        }
        conn2.close();
        basicManagedDataSource.close();
    }
}
