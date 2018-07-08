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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.apache.commons.dbcp2.ConnectionFactory;
import org.apache.commons.dbcp2.DelegatingConnection;
import org.apache.commons.dbcp2.DriverConnectionFactory;
import org.apache.commons.dbcp2.PoolableConnection;
import org.apache.commons.dbcp2.PoolableConnectionFactory;
import org.apache.commons.dbcp2.PoolingDataSource;
import org.apache.commons.dbcp2.TestConnectionPool;
import org.apache.commons.dbcp2.TesterDriver;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.geronimo.transaction.manager.TransactionManagerImpl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.transaction.TransactionManager;

import java.sql.Connection;
import java.util.Properties;

/**
 * TestSuite for ManagedDataSource without a transaction in progress.
 */
public class TestManagedDataSource extends TestConnectionPool {

    @Override
    protected Connection getConnection() throws Exception {
        return ds.getConnection();
    }

    protected PoolingDataSource<PoolableConnection> ds = null;
    private GenericObjectPool<PoolableConnection> pool = null;
    protected TransactionManager transactionManager;

    @Before
    public void setUp() throws Exception {
        // create a GeronimoTransactionManager for testing
        transactionManager = new TransactionManagerImpl();

        // create a driver connection factory
        final Properties properties = new Properties();
        properties.setProperty("user", "userName");
        properties.setProperty("password", "password");
        final ConnectionFactory connectionFactory = new DriverConnectionFactory(new TesterDriver(), "jdbc:apache:commons:testdriver", properties);

        // wrap it with a LocalXAConnectionFactory
        final XAConnectionFactory xaConnectionFactory = new LocalXAConnectionFactory(transactionManager, connectionFactory);

        // create the pool object factory
        final PoolableConnectionFactory factory =
            new PoolableConnectionFactory(xaConnectionFactory, null);
        factory.setValidationQuery("SELECT DUMMY FROM DUAL");
        factory.setDefaultReadOnly(Boolean.TRUE);
        factory.setDefaultAutoCommit(Boolean.TRUE);

        // create the pool
        pool = new GenericObjectPool<>(factory);
        factory.setPool(pool);
        pool.setMaxTotal(getMaxTotal());
        pool.setMaxWaitMillis(getMaxWaitMillis());

        // finally create the datasource
        ds = new ManagedDataSource<>(pool, xaConnectionFactory.getTransactionRegistry());
        ds.setAccessToUnderlyingConnectionAllowed(true);
    }

    @Override
    @After
    public void tearDown() throws Exception {
        pool.close();
        super.tearDown();
    }

    /**
     * Verify the accessToUnderlyingConnectionAllowed properly limits access to the physical connection.
     */
    @Test
    public void testAccessToUnderlyingConnectionAllowed() throws Exception {
        ds.setAccessToUnderlyingConnectionAllowed(true);
        ManagedConnection<?> connection = (ManagedConnection<?>) newConnection();
        assertTrue(connection.isAccessToUnderlyingConnectionAllowed());
        assertNotNull(connection.getDelegate());
        assertNotNull(connection.getInnermostDelegate());
        connection.close();

        ds.setAccessToUnderlyingConnectionAllowed(false);
        connection = (ManagedConnection<?>) newConnection();
        assertFalse(connection.isAccessToUnderlyingConnectionAllowed());
        assertNull(connection.getDelegate());
        assertNull(connection.getInnermostDelegate());
        connection.close();
    }

    /**
     * Verify that connection sharing is working (or not working) as expected.
     */
    @Test
    public void testSharedConnection() throws Exception {
        final DelegatingConnection<?> connectionA = (DelegatingConnection<?>) newConnection();
        final DelegatingConnection<?> connectionB = (DelegatingConnection<?>) newConnection();

        assertFalse(connectionA.equals(connectionB));
        assertFalse(connectionB.equals(connectionA));
        assertFalse(connectionA.innermostDelegateEquals(connectionB.getInnermostDelegate()));
        assertFalse(connectionB.innermostDelegateEquals(connectionA.getInnermostDelegate()));

        connectionA.close();
        connectionB.close();
    }

    @Test
    public void testConnectionReturnOnCommit() throws Exception {
        transactionManager.begin();
        final DelegatingConnection<?> connectionA = (DelegatingConnection<?>) newConnection();
        connectionA.close();
        transactionManager.commit();
        assertEquals(1, pool.getBorrowedCount());
        assertEquals(1, pool.getReturnedCount());
        assertEquals(0, pool.getNumActive());
    }

    @Test
    public void testManagedConnectionEqualsSameDelegateNoUnderlyingAccess() throws Exception {
        // Get a maximal set of connections from the pool
        final Connection[] c = new Connection[getMaxTotal()];
        for (int i = 0; i < c.length; i++) {
            c[i] = newConnection();
        }
        // Close the delegate of one wrapper in the pool
        ((DelegatingConnection<?>) c[0]).getDelegate().close();

        // Disable access for the new connection
        ds.setAccessToUnderlyingConnectionAllowed(false);
        // Grab a new connection - should get c[0]'s closed connection
        // so should be delegate-equivalent
        final Connection con = newConnection();
        Assert.assertNotEquals(c[0], con);
        Assert.assertEquals(
                ((DelegatingConnection<?>) c[0]).getInnermostDelegateInternal(),
                ((DelegatingConnection<?>) con).getInnermostDelegateInternal());
        for (final Connection element : c) {
            element.close();
        }
        ds.setAccessToUnderlyingConnectionAllowed(true);
    }

    @Test
    public void testManagedConnectionEqualsSameDelegate() throws Exception {
        // Get a maximal set of connections from the pool
        final Connection[] c = new Connection[getMaxTotal()];
        for (int i = 0; i < c.length; i++) {
            c[i] = newConnection();
        }
        // Close the delegate of one wrapper in the pool
        ((DelegatingConnection<?>) c[0]).getDelegate().close();

        // Grab a new connection - should get c[0]'s closed connection
        // so should be delegate-equivalent
        final Connection con = newConnection();
        Assert.assertNotEquals(c[0], con);
        Assert.assertEquals(
                ((DelegatingConnection<?>) c[0]).getInnermostDelegateInternal(),
                ((DelegatingConnection<?>) con).getInnermostDelegateInternal());
        for (final Connection element : c) {
            element.close();
        }
    }

    /*
    * JIRA: DBCP-198
    */
    @Test
    public void testManagedConnectionEqualsReflexive() throws Exception {
        final Connection con = ds.getConnection();
        final Connection con2 = con;
        assertTrue(con2.equals(con));
        assertTrue(con.equals(con2));
        con.close();
    }

    @Test
    public void testManagedConnectionEqualsFail() throws Exception {
        final Connection con1 = ds.getConnection();
        final Connection con2 = ds.getConnection();
        assertFalse(con1.equals(con2));
        con1.close();
        con2.close();
    }

    @Test
    public void testManagedConnectionEqualsNull() throws Exception {
        final Connection con1 = ds.getConnection();
        final Connection con2 = null;
        assertFalse(con1.equals(con2));
        con1.close();
    }

    @Test
    public void testManagedConnectionEqualsType() throws Exception {
        final Connection con1 = ds.getConnection();
        final Integer con2 = Integer.valueOf(0);
        assertFalse(con1.equals(con2));
        con1.close();
    }

    @Test
    public void testManagedConnectionEqualInnermost() throws Exception {
        ds.setAccessToUnderlyingConnectionAllowed(true);
        final DelegatingConnection<?> con = (DelegatingConnection<?>) ds.getConnection();
        final Connection inner = con.getInnermostDelegate();
        ds.setAccessToUnderlyingConnectionAllowed(false);
        final DelegatingConnection<Connection> con2 = new DelegatingConnection<>(inner);
        assertFalse(con2.equals(con));
        assertTrue(con.innermostDelegateEquals(con2.getInnermostDelegate()));
        assertTrue(con2.innermostDelegateEquals(inner));
        assertFalse(con.equals(con2));
    }

    @Test
    public void testNestedConnections() throws Exception {
        transactionManager.begin();

        Connection c1 = null;
        Connection c2 = null;

        c1 = newConnection();
        c2 = newConnection();

        transactionManager.commit();

        c1.close();
        c2.close();
    }

    @Test(expected=IllegalStateException.class)
    public void testTransactionRegistryNotInitialized() throws Exception {
        try (ManagedDataSource<?> ds = new ManagedDataSource<>(pool, null)) {
            ds.getConnection();
        }
    }

    @Test(expected=IllegalStateException.class)
    public void testSetTransactionRegistryAlreadySet() {
        final ManagedDataSource<?> managed = (ManagedDataSource<?>) ds;
        managed.setTransactionRegistry(null);
    }

    @Test(expected=NullPointerException.class)
    public void testSetNullTransactionRegistry() throws Exception {
        try (ManagedDataSource<?> ds = new ManagedDataSource<>(pool, null)) {
            ds.setTransactionRegistry(null);
        }
    }

    @Test()
    public void testSetTransactionRegistry() throws Exception {
        try (ManagedDataSource<?> ds = new ManagedDataSource<>(pool, null)) {
            ds.setTransactionRegistry(new TransactionRegistry(transactionManager));
        }
    }
}
