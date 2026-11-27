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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.XADataSource;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.dbcp2.TestBasicDataSource;
import org.apache.commons.dbcp2.TestDatabase;
import org.apache.geronimo.transaction.manager.TransactionManagerImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingSupplier;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.EnumSource;

import com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionManagerImple;
import com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionSynchronizationRegistryImple;

/**
 * TestSuite for BasicManagedDataSource
 */
@ParameterizedClass(name = "{0}")
@EnumSource(value = TestDatabase.class, names = {"H2", "HSQLDB"})
public class TestBasicManagedDataSource extends TestBasicDataSource {

    /** Database used by this invocation. */
    @Parameter
    TestDatabase database;

    /**
     * A second transaction participant that votes {@link XAResource#XA_OK} and does nothing else.
     */
    private static final class OtherXAResource implements XAResource {

        @Override
        public void commit(final Xid xid, final boolean onePhase) {
        }

        @Override
        public void end(final Xid xid, final int flags) {
        }

        @Override
        public void forget(final Xid xid) {
        }

        @Override
        public int getTransactionTimeout() {
            return 0;
        }

        @Override
        public boolean isSameRM(final XAResource xaResource) {
            return xaResource == this;
        }

        @Override
        public int prepare(final Xid xid) {
            return XA_OK;
        }

        @Override
        public Xid[] recover(final int flag) {
            return new Xid[0];
        }

        @Override
        public void rollback(final Xid xid) {
        }

        @Override
        public boolean setTransactionTimeout(final int seconds) {
            return false;
        }

        @Override
        public void start(final Xid xid, final int flags) {
        }
    }

    @Override
    protected BasicDataSource createDataSource() throws Exception {
        final BasicManagedDataSource basicManagedDataSource = new BasicManagedDataSource();
        final TransactionManagerImpl transactionManager = new TransactionManagerImpl();
        basicManagedDataSource.setTransactionManager(transactionManager);
        basicManagedDataSource.setTransactionSynchronizationRegistry(transactionManager);
        return basicManagedDataSource;
    }

    @Test
    void testCreateXaDataSourceNewInstance() throws SQLException, XAException {
        try (final BasicManagedDataSource basicManagedDataSource = new BasicManagedDataSource()) {
            basicManagedDataSource.setXADataSource(database.createXaDataSource(database.newUrl()).getClass().getName());
            basicManagedDataSource.setDriverClassName(database.getDriverClassName());
            basicManagedDataSource.setTransactionManager(new TransactionManagerImpl());
            assertNotNull(basicManagedDataSource.createConnectionFactory());
        }
    }

    @Test
    void testCreateXaDataSourceNoInstanceSetAndNoDataSource() throws SQLException, XAException {
        try (final BasicManagedDataSource basicManagedDataSource = new BasicManagedDataSource()) {
            basicManagedDataSource.setDriverClassName("org.apache.commons.dbcp2.TesterDriver");
            basicManagedDataSource.setUrl("jdbc:apache:commons:testdriver");
            basicManagedDataSource.setTransactionManager(new TransactionManagerImpl());
            assertNotNull(basicManagedDataSource.createConnectionFactory());
        }
    }

    /**
     * Tests that a read-only connection remains reusable after voting {@link XAResource#XA_RDONLY} in a two-phase commit.
     *
     * @throws Exception Thrown if the test fails.
     */
    @Test
    void testReadOnlyConnectionReusableAfterTwoPhaseCommit() throws Exception {
        final TransactionManagerImpl transactionManager = new TransactionManagerImpl();
        try (final BasicManagedDataSource basicManagedDataSource = new BasicManagedDataSource()) {
            basicManagedDataSource.setTransactionManager(transactionManager);
            basicManagedDataSource.setDriverClassName("org.apache.commons.dbcp2.TesterDriver");
            basicManagedDataSource.setUrl("jdbc:apache:commons:testdriver");
            basicManagedDataSource.setUsername("userName");
            basicManagedDataSource.setPassword("password");
            basicManagedDataSource.setDefaultReadOnly(Boolean.TRUE);
            basicManagedDataSource.setMaxTotal(1);
            for (int i = 0; i < 2; i++) {
                transactionManager.begin();
                try {
                    // A second resource makes the transaction manager use two-phase commit.
                    assertTrue(transactionManager.getTransaction().enlistResource(new OtherXAResource()));
                    try (final Connection connection = basicManagedDataSource.getConnection()) {
                        assertTrue(connection.isReadOnly());
                    }
                    transactionManager.commit();
                } finally {
                    if (transactionManager.getTransaction() != null) {
                        transactionManager.rollback();
                    }
                }
            }
            // The same pooled connection served both transactions.
            assertEquals(1, basicManagedDataSource.getConnectionPool().getCreatedCount());
            assertEquals(0, basicManagedDataSource.getConnectionPool().getDestroyedCount());
        }
    }

    /**
     * JIRA: DBCP-294
     * Verify that PoolableConnections created by BasicManagedDataSource unregister themselves
     * when reallyClosed.
     */
    @Test
    void testReallyClose() throws Exception {
        try (final BasicManagedDataSource basicManagedDataSource = new BasicManagedDataSource()) {
            basicManagedDataSource.setTransactionManager(new TransactionManagerImpl());
            basicManagedDataSource.setDriverClassName("org.apache.commons.dbcp2.TesterDriver");
            basicManagedDataSource.setUrl("jdbc:apache:commons:testdriver");
            basicManagedDataSource.setUsername("userName");
            basicManagedDataSource.setPassword("password");
            basicManagedDataSource.setMaxIdle(1);
            // Create two connections
            final ManagedConnection<?> conn = (ManagedConnection<?>) basicManagedDataSource.getConnection();
            assertNotNull(basicManagedDataSource.getTransactionRegistry().getXAResource(conn));
            final ManagedConnection<?> conn2 = (ManagedConnection<?>) basicManagedDataSource.getConnection();
            conn2.close(); // Return one connection to the pool
            conn.close(); // No room at the inn - this will trigger reallyClose(), which should unregister
            assertThrows(SQLException.class, () -> basicManagedDataSource.getTransactionRegistry().getXAResource(conn),
                    "Expecting SQLException - XAResources orphaned");
            conn2.close();
        }
    }

    @Test
    void testRuntimeExceptionsAreRethrown() throws SQLException, XAException {
        try (final BasicManagedDataSource basicManagedDataSource = new BasicManagedDataSource()) {
            basicManagedDataSource.setTransactionManager(new TransactionManagerImpl());
            basicManagedDataSource.setDriverClassName("org.apache.commons.dbcp2.TesterDriver");
            basicManagedDataSource.setUrl("jdbc:apache:commons:testdriver");
            basicManagedDataSource.setUsername("userName");
            basicManagedDataSource.setPassword("password");
            basicManagedDataSource.setMaxIdle(1);
            // results in a NPE
            assertThrows(NullPointerException.class, () -> basicManagedDataSource.createPoolableConnectionFactory(null));
        }
    }

    @Test
    void testSetDriverName() throws SQLException {
        try (final BasicManagedDataSource basicManagedDataSource = new BasicManagedDataSource()) {
            basicManagedDataSource.setDriverClassName("adams");
            assertEquals("adams", basicManagedDataSource.getDriverClassName());
            basicManagedDataSource.setDriverClassName(null);
            assertNull(basicManagedDataSource.getDriverClassName());
        }
    }

    @Test
    void testSetNullXaDataSourceInstance() throws SQLException, XAException {
        try (final BasicManagedDataSource basicManagedDataSource = new BasicManagedDataSource()) {
            basicManagedDataSource.setTransactionManager(new TransactionManagerImpl());
            basicManagedDataSource.setDriverClassName("org.apache.commons.dbcp2.TesterDriver");
            basicManagedDataSource.setUrl("jdbc:apache:commons:testdriver");
            basicManagedDataSource.setUsername("userName");
            basicManagedDataSource.setPassword("password");
            basicManagedDataSource.setMaxIdle(1);
            basicManagedDataSource.setXaDataSourceInstance(null);
            assertNull(basicManagedDataSource.getXaDataSourceInstance());
        }
    }

    /** DBCP-564 */
    @Test
    void testSetRollbackOnlyBeforeGetConnectionDoesNotLeak() throws Exception {
        final TransactionManager transactionManager = ((BasicManagedDataSource) ds).getTransactionManager();
        final int n = 3;
        ds.setMaxIdle(n);
        ds.setMaxTotal(n);

        for (int i = 0; i <= n; i++) { // loop n+1 times
            transactionManager.begin();
            transactionManager.setRollbackOnly();
            try (final Connection conn = getConnection()) {
                assertNotNull(conn);
            }
            transactionManager.rollback();
        }

        assertEquals(0, ds.getNumActive());
        assertEquals(1, ds.getNumIdle());
    }

    @Test
    void testSetXaDataSourceInstance() throws SQLException, XAException {
        try (final BasicManagedDataSource basicManagedDataSource = new BasicManagedDataSource()) {
            basicManagedDataSource.setTransactionManager(new TransactionManagerImpl());
            basicManagedDataSource.setDriverClassName("org.apache.commons.dbcp2.TesterDriver");
            basicManagedDataSource.setUrl("jdbc:apache:commons:testdriver");
            basicManagedDataSource.setUsername("userName");
            basicManagedDataSource.setPassword("password");
            basicManagedDataSource.setMaxIdle(1);
            basicManagedDataSource.setXaDataSourceInstance(database.createXaDataSource(database.newUrl()));
            assertNotNull(basicManagedDataSource.createConnectionFactory());
        }
    }

    @Test
    void testTransactionManagerNotSet() throws SQLException {
        try (final BasicManagedDataSource basicManagedDataSource = new BasicManagedDataSource()) {
            assertThrows(SQLException.class, basicManagedDataSource::createConnectionFactory);
        }
    }

    @Test
    void testTransactionSynchronizationRegistry() throws Exception {
        try (final BasicManagedDataSource basicManagedDataSource = new BasicManagedDataSource()) {
            basicManagedDataSource.setTransactionManager(new TransactionManagerImple());
            final TransactionSynchronizationRegistry tsr = new TransactionSynchronizationRegistryImple();
            basicManagedDataSource.setTransactionSynchronizationRegistry(tsr);
            final XADataSource xaDataSource = database.createXaDataSource(database.newUrl());
            basicManagedDataSource.setXaDataSourceInstance(xaDataSource);
            basicManagedDataSource.setMaxIdle(1);

            final TransactionManager tm = basicManagedDataSource.getTransactionManager();
            tm.begin();
            tsr.registerInterposedSynchronization(new SynchronizationAdapter() {
                @Override
                public void beforeCompletion() {
                    try (Connection connection = assertDoesNotThrow((ThrowingSupplier<Connection>) basicManagedDataSource::getConnection)) {
                        assertNotNull(connection);
                    } catch (final SQLException e) {
                        fail(e);
                    }
                }
            });
            tm.commit();
        }
    }

    @Test
    void testXADataSource() throws SQLException {
        try (final BasicManagedDataSource basicManagedDataSource = new BasicManagedDataSource()) {
            basicManagedDataSource.setXADataSource("anything");
            assertEquals("anything", basicManagedDataSource.getXADataSource());
        }
    }

    @Test
    void testXaDataSourceInstance() throws SQLException {
        try (final BasicManagedDataSource basicManagedDataSource = new BasicManagedDataSource()) {
            final XADataSource ds = database.createXaDataSource(database.newUrl());
            basicManagedDataSource.setXaDataSourceInstance(ds);
            assertEquals(ds, basicManagedDataSource.getXaDataSourceInstance());
        }
    }
}
