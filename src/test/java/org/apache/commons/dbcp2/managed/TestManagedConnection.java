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

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.RollbackException;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;

import org.apache.commons.dbcp2.ConnectionFactory;
import org.apache.commons.dbcp2.DelegatingConnection;
import org.apache.commons.dbcp2.DriverConnectionFactory;
import org.apache.commons.dbcp2.PoolableConnection;
import org.apache.commons.dbcp2.PoolableConnectionFactory;
import org.apache.commons.dbcp2.PoolingDataSource;
import org.apache.commons.dbcp2.TesterDriver;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.geronimo.transaction.manager.TransactionManagerImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * TestSuite for ManagedConnection.
 */
public class TestManagedConnection {

    protected PoolingDataSource<PoolableConnection> ds = null;

    private GenericObjectPool<PoolableConnection> pool = null;

    protected TransactionManager transactionManager;

    @BeforeEach
    public void setUp()
        throws Exception {
        // create a GeronimoTransactionManager for testing
        transactionManager = new TransactionManagerImpl();

        // create a driver connection factory
        final Properties properties = new Properties();
        properties.setProperty("user", "userName");
        properties.setProperty("password", "password");
        final ConnectionFactory connectionFactory = new DriverConnectionFactory(new TesterDriver(), "jdbc:apache:commons:testdriver", properties);

        // wrap it with a LocalXAConnectionFactory
        final XAConnectionFactory xaConnectionFactory = new UncooperativeLocalXAConnectionFactory(transactionManager, connectionFactory);

        // create the pool object factory
        final PoolableConnectionFactory factory = new PoolableConnectionFactory(xaConnectionFactory, null);
        factory.setValidationQuery("SELECT DUMMY FROM DUAL");
        factory.setDefaultReadOnly(Boolean.TRUE);
        factory.setDefaultAutoCommit(Boolean.TRUE);

        // create the pool
        pool = new GenericObjectPool<>(factory);
        factory.setPool(pool);
        pool.setMaxTotal(10);
        pool.setMaxWaitMillis(100);

        // finally create the datasource
        ds = new ManagedDataSource<>(pool, xaConnectionFactory.getTransactionRegistry());
        ds.setAccessToUnderlyingConnectionAllowed(true);
    }

    @AfterEach
    public void tearDown()
        throws Exception {
        pool.close();
    }

    public Connection getConnection()
        throws Exception {
        return ds.getConnection();
    }

    @Test
    public void testConnectionReturnOnErrorWhenEnlistingXAResource()
        throws Exception {
        // see DBCP-433

        transactionManager.begin();
        try {
            final DelegatingConnection<?> connectionA = (DelegatingConnection<?>) getConnection();
            connectionA.close();
        } catch (final SQLException e) {
            // expected
        }
        transactionManager.commit();
        assertEquals(1, pool.getBorrowedCount());
        // assertEquals(1, pool.getReturnedCount());
        assertEquals(1, pool.getDestroyedCount());
        assertEquals(0, pool.getNumActive());
    }

    private class UncooperativeLocalXAConnectionFactory
        extends LocalXAConnectionFactory {

        public UncooperativeLocalXAConnectionFactory(final TransactionManager transactionManager, final ConnectionFactory connectionFactory) {
            super(transactionManager, connectionFactory);

            try {
                // inject our own TransactionRegistry which returns Uncooperative Transactions which always fail to enlist a XAResource
                final Field field = LocalXAConnectionFactory.class.getDeclaredField("transactionRegistry");
                field.setAccessible(true);
                field.set(this, new UncooperativeTransactionRegistry(transactionManager));
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class UncooperativeTransactionRegistry
        extends TransactionRegistry {

        public UncooperativeTransactionRegistry(final TransactionManager transactionManager) {
            super(transactionManager);
        }

        @Override
        public TransactionContext getActiveTransactionContext()
            throws SQLException {
            try {
                return new TransactionContext(this, new UncooperativeTransaction(transactionManager.getTransaction()));
            } catch (final SystemException e) {
                return null;
            }
        }

    }

    /**
     * Transaction that always fails enlistResource.
     */
    private class UncooperativeTransaction
        implements Transaction {

        private final Transaction wrappedTransaction;

        public UncooperativeTransaction(final Transaction transaction) {
            this.wrappedTransaction = transaction;
        }

        @Override
        public void commit()
            throws HeuristicMixedException, HeuristicRollbackException, RollbackException, SecurityException,
            SystemException {
            wrappedTransaction.commit();
        }

        @Override
        public boolean delistResource(final XAResource arg0, final int arg1)
            throws IllegalStateException, SystemException {
            return wrappedTransaction.delistResource(arg0, arg1);
        }

        @Override
        public int getStatus()
            throws SystemException {
            return wrappedTransaction.getStatus();
        }

        @Override
        public void registerSynchronization(final Synchronization arg0)
            throws IllegalStateException, RollbackException, SystemException {
            wrappedTransaction.registerSynchronization(arg0);
        }

        @Override
        public void rollback()
            throws IllegalStateException, SystemException {
            wrappedTransaction.rollback();
        }

        @Override
        public void setRollbackOnly()
            throws IllegalStateException, SystemException {
            wrappedTransaction.setRollbackOnly();
        }

        @Override
        public synchronized boolean enlistResource(final XAResource xaRes) {
            return false;
        }
    }

}
