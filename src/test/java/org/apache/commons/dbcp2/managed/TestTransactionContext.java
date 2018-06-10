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
import javax.transaction.xa.XAResource;

import org.junit.Test;
import org.apache.geronimo.transaction.manager.TransactionManagerImpl;
import org.apache.geronimo.transaction.manager.TransactionImpl;

/**
 * TestSuite for TransactionContext
 */
public class TestTransactionContext {

    /**
     * JIRA: DBCP-428
     */
    @Test(expected=SQLException.class)
    public void testSetSharedConnectionEnlistFailure() throws Exception {
        final BasicManagedDataSource basicManagedDataSource = new BasicManagedDataSource();
        basicManagedDataSource.setTransactionManager(new TransactionManagerImpl());
        basicManagedDataSource.setDriverClassName("org.apache.commons.dbcp2.TesterDriver");
        basicManagedDataSource.setUrl("jdbc:apache:commons:testdriver");
        basicManagedDataSource.setUsername("userName");
        basicManagedDataSource.setPassword("password");
        basicManagedDataSource.setMaxIdle(1);
        final ManagedConnection<?> conn = (ManagedConnection<?>) basicManagedDataSource.getConnection();
        final UncooperativeTransaction transaction = new UncooperativeTransaction();
        final TransactionContext transactionContext =
                new TransactionContext(basicManagedDataSource.getTransactionRegistry(), transaction);
        transactionContext.setSharedConnection(conn);
        basicManagedDataSource.close();
    }

    /**
     * Transaction that always fails enlistResource.
     */
    private class UncooperativeTransaction extends TransactionImpl {
        public UncooperativeTransaction() {
            super(null, null);
        }
        @Override
        public synchronized boolean enlistResource(final XAResource xaRes) {
            return false;
        }
    }

}

