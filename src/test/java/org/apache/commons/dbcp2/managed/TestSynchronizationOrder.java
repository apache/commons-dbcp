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

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.dbcp2.DelegatingConnection;
import org.apache.commons.dbcp2.PoolableConnectionFactory;
import org.apache.commons.dbcp2.TesterClassLoader;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.XAConnection;
import javax.sql.XADataSource;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.xa.XAResource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class TestSynchronizationOrder {

    private boolean transactionManagerRegistered;
    private boolean transactionSynchronizationRegistryRegistered;
    private TransactionManager transactionManager;
    private TransactionSynchronizationRegistry transactionSynchronizationRegistry;
    private XADataSource xads;
    private BasicManagedDataSource bmds;
    private BasicDataSource bds;

    @Test
    public void testSessionSynchronization() throws Exception {
        final DataSourceXAConnectionFactory xaConnectionFactory = new DataSourceXAConnectionFactory(transactionManager,
                xads);

        final PoolableConnectionFactory factory = new PoolableConnectionFactory(xaConnectionFactory, null);
        factory.setValidationQuery("SELECT DUMMY FROM DUAL");
        factory.setDefaultReadOnly(Boolean.TRUE);
        factory.setDefaultAutoCommit(Boolean.TRUE);

        // create the pool
        try (final GenericObjectPool pool = new GenericObjectPool<>(factory)) {
            factory.setPool(pool);
            pool.setMaxTotal(10);
            pool.setMaxWaitMillis(1000);

            // finally create the datasource
            try (final ManagedDataSource ds = new ManagedDataSource<>(pool,
                    xaConnectionFactory.getTransactionRegistry())) {
                ds.setAccessToUnderlyingConnectionAllowed(true);

                transactionManager.begin();
                try (final DelegatingConnection<?> connectionA = (DelegatingConnection<?>) ds.getConnection()) {
                    // close right away.
                }
                transactionManager.commit();
                assertTrue(transactionManagerRegistered);
                assertFalse(transactionSynchronizationRegistryRegistered);
            }
        }
    }

    @Test
    public void testInterposedSynchronization() throws Exception {
        final DataSourceXAConnectionFactory xaConnectionFactory = new DataSourceXAConnectionFactory(transactionManager,
                xads, transactionSynchronizationRegistry);

        final PoolableConnectionFactory factory = new PoolableConnectionFactory(xaConnectionFactory, null);
        factory.setValidationQuery("SELECT DUMMY FROM DUAL");
        factory.setDefaultReadOnly(Boolean.TRUE);
        factory.setDefaultAutoCommit(Boolean.TRUE);

        // create the pool
        try (final GenericObjectPool pool = new GenericObjectPool<>(factory)) {
            factory.setPool(pool);
            pool.setMaxTotal(10);
            pool.setMaxWaitMillis(1000);

            // finally create the datasource
            try (final ManagedDataSource ds = new ManagedDataSource<>(pool,
                    xaConnectionFactory.getTransactionRegistry())) {
                ds.setAccessToUnderlyingConnectionAllowed(true);

                transactionManager.begin();
                try (final DelegatingConnection<?> connectionA = (DelegatingConnection<?>) ds.getConnection()) {
                    // Close right away.
                }
                transactionManager.commit();
                assertFalse(transactionManagerRegistered);
                assertTrue(transactionSynchronizationRegistryRegistered);
            }
        }
    }

    @AfterEach
    public void tearDown() throws SQLException {
        bds.close();
        bmds.close();
    }

    @BeforeEach
    public void setup() {
        transactionManager = new TransactionManager() {

            @Override
            public void begin() throws NotSupportedException, SystemException {

            }

            @Override
            public void commit() throws HeuristicMixedException, HeuristicRollbackException, IllegalStateException, RollbackException, SecurityException, SystemException {

            }

            @Override
            public int getStatus() throws SystemException {
                return 0;
            }

            @Override
            public Transaction getTransaction() throws SystemException {
                return new Transaction() {

                    @Override
                    public void commit() throws HeuristicMixedException, HeuristicRollbackException, RollbackException, SecurityException, SystemException {

                    }

                    @Override
                    public boolean delistResource(final XAResource xaResource, final int i) throws IllegalStateException, SystemException {
                        return false;
                    }

                    @Override
                    public boolean enlistResource(final XAResource xaResource) throws IllegalStateException, RollbackException, SystemException {
                        // Called and used
                        return true;
                    }

                    @Override
                    public int getStatus() throws SystemException {
                        return 0;
                    }

                    @Override
                    public void registerSynchronization(final Synchronization synchronization) throws IllegalStateException, RollbackException, SystemException {
                        transactionManagerRegistered = true;
                    }

                    @Override
                    public void rollback() throws IllegalStateException, SystemException {

                    }

                    @Override
                    public void setRollbackOnly() throws IllegalStateException, SystemException {

                    }
                };
            }

            @Override
            public void resume(final Transaction transaction) throws IllegalStateException, InvalidTransactionException, SystemException {

            }

            @Override
            public void rollback() throws IllegalStateException, SecurityException, SystemException {

            }

            @Override
            public void setRollbackOnly() throws IllegalStateException, SystemException {

            }

            @Override
            public void setTransactionTimeout(final int i) throws SystemException {

            }

            @Override
            public Transaction suspend() throws SystemException {
                return null;
            }
        };

        transactionSynchronizationRegistry = new TransactionSynchronizationRegistry() {

            @Override
            public Object getResource(final Object o) {
                return null;
            }

            @Override
            public boolean getRollbackOnly() {
                return false;
            }

            @Override
            public Object getTransactionKey() {
                return null;
            }

            @Override
            public int getTransactionStatus() {
                return 0;
            }

            @Override
            public void putResource(final Object o, final Object o1) {

            }

            @Override
            public void registerInterposedSynchronization(final Synchronization synchronization) {
                transactionSynchronizationRegistryRegistered = true;
            }

            @Override
            public void setRollbackOnly() {

            }
        };

        bmds = new BasicManagedDataSource();
        bmds.setTransactionManager(transactionManager);
        bmds.setTransactionSynchronizationRegistry(transactionSynchronizationRegistry);
        bmds.setXADataSource("notnull");
        bds = new BasicDataSource();
        bds.setDriverClassName("org.apache.commons.dbcp2.TesterDriver");
        bds.setUrl("jdbc:apache:commons:testdriver");
        bds.setMaxTotal(10);
        bds.setMaxWaitMillis(100L);
        bds.setDefaultAutoCommit(Boolean.TRUE);
        bds.setDefaultReadOnly(Boolean.FALSE);
        bds.setDefaultTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        bds.setDefaultCatalog("test catalog");
        bds.setUsername("userName");
        bds.setPassword("password");
        bds.setValidationQuery("SELECT DUMMY FROM DUAL");
        bds.setConnectionInitSqls(Arrays.asList(new String[]{"SELECT 1", "SELECT 2"}));
        bds.setDriverClassLoader(new TesterClassLoader());
        bds.setJmxName("org.apache.commons.dbcp2:name=test");
        final AtomicInteger closeCounter = new AtomicInteger();
        final InvocationHandler handle = new InvocationHandler() {
            @Override
            public Object invoke(final Object proxy, final Method method, final Object[] args)
                    throws Throwable {
                final String methodName = method.getName();
                if (methodName.equals("hashCode")) {
                    return Integer.valueOf(System.identityHashCode(proxy));
                }
                if (methodName.equals("equals")) {
                    return Boolean.valueOf(proxy == args[0]);
                }
                if (methodName.equals("getXAConnection")) {
                    // both zero and 2-arg signatures
                    return getXAConnection();
                }
                try {
                    return method.invoke(bds, args);
                } catch (final InvocationTargetException e) {
                    throw e.getTargetException();
                }
            }

            protected XAConnection getXAConnection() throws SQLException {
                return new TesterBasicXAConnection(bds.getConnection(), closeCounter);
            }
        };
        xads = (XADataSource) Proxy.newProxyInstance(
                TestSynchronizationOrder.class.getClassLoader(),
                new Class[]{XADataSource.class}, handle);
        bmds.setXaDataSourceInstance(xads);

    }
}
