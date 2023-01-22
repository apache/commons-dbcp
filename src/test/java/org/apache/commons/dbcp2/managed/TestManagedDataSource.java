/*

  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.apache.commons.dbcp2.managed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.util.Properties;

import javax.transaction.TransactionManager;

import org.apache.commons.dbcp2.ConnectionFactory;
import org.apache.commons.dbcp2.Constants;
import org.apache.commons.dbcp2.DelegatingConnection;
import org.apache.commons.dbcp2.DriverConnectionFactory;
import org.apache.commons.dbcp2.PoolableConnection;
import org.apache.commons.dbcp2.PoolableConnectionFactory;
import org.apache.commons.dbcp2.PoolingDataSource;
import org.apache.commons.dbcp2.TestConnectionPool;
import org.apache.commons.dbcp2.TesterDriver;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.geronimo.transaction.manager.TransactionManagerImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * TestSuite for ManagedDataSource without a transaction in progress.
 */
public class TestManagedDataSource extends TestConnectionPool {

    protected PoolingDataSource<PoolableConnection> ds;

    protected GenericObjectPool<PoolableConnection> pool;

    protected TransactionManager transactionManager;

    @Override
    protected Connection getConnection() throws Exception {
        return ds.getConnection();
    }

    @BeforeEach
    public void setUp() throws Exception {
        // create a GeronimoTransactionManager for testing
        transactionManager = new TransactionManagerImpl();

        // create a driver connection factory
        final Properties properties = new Properties();
        properties.setProperty(Constants.KEY_USER, "userName");
        properties.setProperty(Constants.KEY_PASSWORD, "password");
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
        pool.setMaxWait(getMaxWaitDuration());

        // finally create the datasource
        ds = new ManagedDataSource<>(pool, xaConnectionFactory.getTransactionRegistry());
        ds.setAccessToUnderlyingConnectionAllowed(true);
    }

    @Override
    @AfterEach
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
        try (ManagedConnection<?> connection = (ManagedConnection<?>) newConnection()) {
            assertTrue(connection.isAccessToUnderlyingConnectionAllowed());
            assertNotNull(connection.getDelegate());
            assertNotNull(connection.getInnermostDelegate());
        }

        ds.setAccessToUnderlyingConnectionAllowed(false);
        try (ManagedConnection<?> connection = (ManagedConnection<?>) newConnection()) {
            assertFalse(connection.isAccessToUnderlyingConnectionAllowed());
            assertNull(connection.getDelegate());
            assertNull(connection.getInnermostDelegate());
        }
    }

    @Test
    public void testConnectionReturnOnCommit() throws Exception {
        transactionManager.begin();
        try (DelegatingConnection<?> connectionA = (DelegatingConnection<?>) newConnection()) {
            // auto close.
        }
        transactionManager.commit();
        assertEquals(1, pool.getBorrowedCount());
        assertEquals(1, pool.getReturnedCount());
        assertEquals(0, pool.getNumActive());
    }

    @Test
    public void testManagedConnectionEqualInnermost() throws Exception {
        ds.setAccessToUnderlyingConnectionAllowed(true);
        try (DelegatingConnection<?> con = (DelegatingConnection<?>) getConnection()) {
            @SuppressWarnings("resource")
            final Connection inner = con.getInnermostDelegate();
            ds.setAccessToUnderlyingConnectionAllowed(false);
            final DelegatingConnection<Connection> con2 = new DelegatingConnection<>(inner);
            assertNotEquals(con2, con);
            assertTrue(con.innermostDelegateEquals(con2.getInnermostDelegate()));
            assertTrue(con2.innermostDelegateEquals(inner));
            assertNotEquals(con, con2);
        }
    }

    @Test
    public void testManagedConnectionEqualsFail() throws Exception {
        try (Connection con1 = getConnection(); final Connection con2 = getConnection()) {
            assertNotEquals(con1, con2);
        }
    }

    @Test
    public void testManagedConnectionEqualsNull() throws Exception {
        try (Connection con1 = getConnection()) {
            final Connection con2 = null;
            assertNotEquals(con2, con1);
        }
    }

    /*
    * JIRA: DBCP-198
    */
    @Test
    public void testManagedConnectionEqualsReflexive() throws Exception {
        try (Connection con = getConnection()) {
            @SuppressWarnings("resource")
            final Connection con2 = con;
            assertEquals(con2, con);
            assertEquals(con, con2);
        }
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
        try (Connection con = newConnection()) {
            Assertions.assertNotEquals(c[0], con);
            Assertions.assertEquals(((DelegatingConnection<?>) c[0]).getInnermostDelegateInternal(),
                ((DelegatingConnection<?>) con).getInnermostDelegateInternal());
            for (final Connection element : c) {
                element.close();
            }
        }
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
        try (Connection con = newConnection()) {
            Assertions.assertNotEquals(c[0], con);
            Assertions.assertEquals(((DelegatingConnection<?>) c[0]).getInnermostDelegateInternal(),
                    ((DelegatingConnection<?>) con).getInnermostDelegateInternal());
            for (final Connection element : c) {
                element.close();
            }
            ds.setAccessToUnderlyingConnectionAllowed(true);
        }
    }

    @Test
    public void testManagedConnectionEqualsType() throws Exception {
        try (Connection con1 = getConnection()) {
            final Integer con2 = 0;
            assertNotEquals(con2, con1);
        }
    }

    @Test
    public void testNestedConnections() throws Exception {
        transactionManager.begin();
        try (Connection c1 = newConnection(); final Connection c2 = newConnection()) {
            transactionManager.commit();
        }
    }

    @Test
    public void testSetNullTransactionRegistry() throws Exception {
        try (ManagedDataSource<?> ds = new ManagedDataSource<>(pool, null)) {
            assertThrows(NullPointerException.class, () -> ds.setTransactionRegistry(null));
        }
    }

    @Test()
    public void testSetTransactionRegistry() throws Exception {
        try (ManagedDataSource<?> ds = new ManagedDataSource<>(pool, null)) {
            ds.setTransactionRegistry(new TransactionRegistry(transactionManager));
        }
    }

    @Test
    public void testSetTransactionRegistryAlreadySet() {
        final ManagedDataSource<?> managed = (ManagedDataSource<?>) ds;
        assertThrows(IllegalStateException.class, () -> managed.setTransactionRegistry(null));
    }

    /**
     * Verify that connection sharing is working (or not working) as expected.
     */
    @Test
    public void testSharedConnection() throws Exception {
        try (DelegatingConnection<?> connectionA = (DelegatingConnection<?>) newConnection();
                final DelegatingConnection<?> connectionB = (DelegatingConnection<?>) newConnection()) {
            assertNotEquals(connectionA, connectionB);
            assertNotEquals(connectionB, connectionA);
            assertFalse(connectionA.innermostDelegateEquals(connectionB.getInnermostDelegate()));
            assertFalse(connectionB.innermostDelegateEquals(connectionA.getInnermostDelegate()));
        }
    }

    @Test
    public void testTransactionRegistryNotInitialized() throws Exception {
        try (ManagedDataSource<?> ds = new ManagedDataSource<>(pool, null)) {
            assertThrows(IllegalStateException.class, ds::getConnection);
        }
    }
}
