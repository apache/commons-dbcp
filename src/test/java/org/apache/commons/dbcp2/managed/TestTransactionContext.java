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

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Connection;
import java.sql.SQLException;

import javax.transaction.xa.XAResource;

import org.apache.geronimo.transaction.manager.TransactionImpl;
import org.apache.geronimo.transaction.manager.TransactionManagerImpl;
import org.junit.jupiter.api.Test;

/**
 * TestSuite for {@link TransactionContext}.
 */
public class TestTransactionContext {

    /**
     * Transaction that always fails enlistResource.
     */
    private static final class UncooperativeTransaction extends TransactionImpl {

        public UncooperativeTransaction() {
            super(null, null);
        }

        @Override
        public synchronized boolean enlistResource(final XAResource xaRes) {
            return false;
        }
    }

    /**
     * JIRA: DBCP-428
     */
    @Test
    public void testSetSharedConnectionEnlistFailure() throws Exception {
        try (final BasicManagedDataSource basicManagedDataSource = new BasicManagedDataSource()) {
            basicManagedDataSource.setTransactionManager(new TransactionManagerImpl());
            basicManagedDataSource.setDriverClassName("org.apache.commons.dbcp2.TesterDriver");
            basicManagedDataSource.setUrl("jdbc:apache:commons:testdriver");
            basicManagedDataSource.setUsername("userName");
            basicManagedDataSource.setPassword("password");
            basicManagedDataSource.setMaxIdle(1);
            try (final Connection conn = basicManagedDataSource.getConnection()) {
                assertInstanceOf(ManagedConnection.class, conn);
                final UncooperativeTransaction transaction = new UncooperativeTransaction();
                final TransactionContext transactionContext = new TransactionContext(basicManagedDataSource.getTransactionRegistry(), transaction);
                assertThrows(SQLException.class, () -> transactionContext.setSharedConnection(conn));
            }
        }
    }

}

