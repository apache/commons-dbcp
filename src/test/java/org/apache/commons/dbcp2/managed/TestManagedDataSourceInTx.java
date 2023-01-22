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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.transaction.Synchronization;
import javax.transaction.Transaction;

import org.apache.commons.dbcp2.DelegatingConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests ManagedDataSource with an active transaction in progress.
 */
public class TestManagedDataSourceInTx extends TestManagedDataSource {

    // can't actually test close in a transaction
    @Override
    protected void assertBackPointers(final Connection conn, final Statement statement) throws SQLException {
        assertFalse(conn.isClosed());
        assertFalse(isClosed(statement));

        assertSame(conn, statement.getConnection(),
                "statement.getConnection() should return the exact same connection instance that was used to create the statement");

        try (ResultSet resultSet = statement.getResultSet()) {
            assertFalse(isClosed(resultSet));
            assertSame(statement, resultSet.getStatement(),
                    "resultSet.getStatement() should return the exact same statement instance that was used to create the result set");

            try (ResultSet executeResultSet = statement.executeQuery("select * from dual")) {
                assertFalse(isClosed(executeResultSet));
                assertSame(statement, executeResultSet.getStatement(),
                        "resultSet.getStatement() should return the exact same statement instance that was used to create the result set");
            }

            try (ResultSet keysResultSet = statement.getGeneratedKeys()) {
                assertFalse(isClosed(keysResultSet));
                assertSame(statement, keysResultSet.getStatement(),
                        "resultSet.getStatement() should return the exact same statement instance that was used to create the result set");
            }
            if (statement instanceof PreparedStatement) {
                final PreparedStatement preparedStatement = (PreparedStatement) statement;
                try (ResultSet preparedResultSet = preparedStatement.executeQuery()) {
                    assertFalse(isClosed(preparedResultSet));
                    assertSame(statement, preparedResultSet.getStatement(),
                            "resultSet.getStatement() should return the exact same statement instance that was used to create the result set");
                }
            }

            resultSet.getStatement().getConnection().close();
        }
    }

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        transactionManager.begin();
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        if (transactionManager.getTransaction() != null) {
            transactionManager.commit();
        }
        super.tearDown();
    }

    @Override
    @Test
    public void testAutoCommitBehavior() throws Exception {
        final Connection connection = newConnection();

        // auto commit should be off
        assertFalse(connection.getAutoCommit(), "Auto-commit should be disabled");

        // attempt to set auto commit
        try {
            connection.setAutoCommit(true);
            fail("setAutoCommit method should be disabled while enlisted in a transaction");
        } catch (final SQLException e) {
            // expected
        }

        // make sure it is still disabled
        assertFalse(connection.getAutoCommit(), "Auto-commit should be disabled");

        // close connection
        connection.close();
    }

    @Override
    @Test
    public void testClearWarnings() throws Exception {
        // open a connection
        Connection connection = newConnection();
        assertNotNull(connection);

        // generate SQLWarning on connection
        final CallableStatement statement = connection.prepareCall("warning");
        assertNotNull(connection.getWarnings());

        // create a new shared connection
        final Connection sharedConnection = newConnection();

        // shared connection should see warning
        assertNotNull(sharedConnection.getWarnings());

        // close and allocate a new (original) connection
        connection.close();
        connection = newConnection();

        // warnings should not have been cleared by closing the connection
        assertNotNull(connection.getWarnings());
        assertNotNull(sharedConnection.getWarnings());

        statement.close();
        sharedConnection.close();
        connection.close();
    }

    @Test
    public void testCloseInTransaction() throws Exception {
        try (DelegatingConnection<?> connectionA = (DelegatingConnection<?>) newConnection();
                DelegatingConnection<?> connectionB = (DelegatingConnection<?>) newConnection()) {
            assertNotEquals(connectionA, connectionB);
            assertNotEquals(connectionB, connectionA);
            assertTrue(connectionA.innermostDelegateEquals(connectionB.getInnermostDelegate()));
            assertTrue(connectionB.innermostDelegateEquals(connectionA.getInnermostDelegate()));
        }

        final Connection connection = newConnection();

        assertFalse(connection.isClosed(), "Connection should be open");

        connection.close();

        assertTrue(connection.isClosed(), "Connection should be closed");
    }

    @Test
    public void testCommit() throws Exception {
        try (Connection connection = newConnection()) {

            // connection should be open
            assertFalse(connection.isClosed(), "Connection should be open");

            // attempt commit directly
            try {
                connection.commit();
                fail("commit method should be disabled while enlisted in a transaction");
            } catch (final SQLException e) {
                // expected
            }

            // make sure it is still open
            assertFalse(connection.isClosed(), "Connection should be open");

        }
    }

    @Override
    @Test
    public void testConnectionReturnOnCommit() throws Exception {
       // override with no-op test
    }

    @Override
    @Test
    public void testConnectionsAreDistinct() throws Exception {
        final Connection[] conn = new Connection[getMaxTotal()];
        for (int i = 0; i < conn.length; i++) {
            conn[i] = newConnection();
            for (int j = 0; j < i; j++) {
                // two connections should be distinct instances
                Assertions.assertNotSame(conn[j], conn[i]);
                // neither should they should be equivalent even though they are
                // sharing the same underlying connection
                Assertions.assertNotEquals(conn[j], conn[i]);
                // Check underlying connection is the same
                Assertions.assertEquals(((DelegatingConnection<?>) conn[j]).getInnermostDelegateInternal(),
                        ((DelegatingConnection<?>) conn[i]).getInnermostDelegateInternal());
            }
        }
        for (final Connection element : conn) {
            element.close();
        }
    }

    @Test
    public void testDoubleReturn() throws Exception {
        transactionManager.getTransaction().registerSynchronization(new Synchronization() {
            private ManagedConnection<?> conn;

            @Override
            public void afterCompletion(final int i) {
                final int numActive = pool.getNumActive();
                try {
                    conn.checkOpen();
                } catch (final Exception e) {
                    // Ignore
                }
                assertEquals(numActive, pool.getNumActive());
                try {
                    conn.close();
                } catch (final Exception e) {
                    fail("Should have been able to close the connection");
                }
                // TODO Requires DBCP-515 assertTrue(numActive -1 == pool.getNumActive());
            }

            @Override
            public void beforeCompletion() {
                try {
                    conn = (ManagedConnection<?>) ds.getConnection();
                    assertNotNull(conn);
                } catch (final SQLException e) {
                    fail("Could not get connection");
                }
            }
        });
        transactionManager.commit();
    }

    @Test
    public void testGetConnectionInAfterCompletion() throws Exception {

        final DelegatingConnection<?> connection = (DelegatingConnection<?>) newConnection();
        // Don't close so we can check it for warnings in afterCompletion
        transactionManager.getTransaction().registerSynchronization(new SynchronizationAdapter() {
            @Override
            public void afterCompletion(final int i) {
                try {
                    final Connection connection1 = ds.getConnection();
                    try {
                        connection1.getWarnings();
                        fail("Could operate on closed connection");
                    } catch (final SQLException e) {
                        // This is expected
                    }
                } catch (final SQLException e) {
                    fail("Should have been able to get connection");
                }
            }
        });
        connection.close();
        transactionManager.commit();
    }

    @Override
    @Test
    public void testHashCode() throws Exception {
        final Connection conn1 = newConnection();
        assertNotNull(conn1);
        final Connection conn2 = newConnection();
        assertNotNull(conn2);

        // shared connections should not have the same hashcode
        Assertions.assertNotEquals(conn1.hashCode(), conn2.hashCode());
    }

    /**
     * @see #testSharedConnection()
     */
    @Override
    @Test
    public void testManagedConnectionEqualsFail() throws Exception {
        // this test is invalid for managed connections since because
        // two connections to the same datasource are supposed to share
        // a single connection
    }

    @Override
    @Test
    public void testMaxTotal() throws Exception {
        final Transaction[] transactions = new Transaction[getMaxTotal()];
        final Connection[] c = new Connection[getMaxTotal()];
        for (int i = 0; i < c.length; i++) {
            // create a new connection in the current transaction
            c[i] = newConnection();
            assertNotNull(c[i]);

            // suspend the current transaction and start a new one
            transactions[i] = transactionManager.suspend();
            assertNotNull(transactions[i]);
            transactionManager.begin();
        }

        try {
            newConnection();
            fail("Allowed to open more than DefaultMaxTotal connections.");
        } catch (final java.sql.SQLException e) {
            // should only be able to open 10 connections, so this test should
            // throw an exception
        } finally {
            transactionManager.commit();
            for (int i = 0; i < c.length; i++) {
                transactionManager.resume(transactions[i]);
                c[i].close();
                transactionManager.commit();
            }
        }
    }

    @Override
    @Test
    public void testNestedConnections() {
        // Not supported
    }

    @Test
    public void testReadOnly() throws Exception {
        final Connection connection = newConnection();

        // NOTE: This test class uses connections that are read-only by default

        // connection should be read only
        assertTrue(connection.isReadOnly(), "Connection be read-only");

        // attempt to setReadOnly
        try {
            connection.setReadOnly(true);
            fail("setReadOnly method should be disabled while enlisted in a transaction");
        } catch (final SQLException e) {
            // expected
        }

        // make sure it is still read-only
        assertTrue(connection.isReadOnly(), "Connection be read-only");

        // attempt to setReadonly
        try {
            connection.setReadOnly(false);
            fail("setReadOnly method should be disabled while enlisted in a transaction");
        } catch (final SQLException e) {
            // expected
        }

        // make sure it is still read-only
        assertTrue(connection.isReadOnly(), "Connection be read-only");

        // close connection
        connection.close();
    }

    @Override
    @Test
    public void testSharedConnection() throws Exception {
        final DelegatingConnection<?> connectionA = (DelegatingConnection<?>) newConnection();
        final DelegatingConnection<?> connectionB = (DelegatingConnection<?>) newConnection();

        assertNotEquals(connectionA, connectionB);
        assertNotEquals(connectionB, connectionA);
        assertTrue(connectionA.innermostDelegateEquals(connectionB.getInnermostDelegate()));
        assertTrue(connectionB.innermostDelegateEquals(connectionA.getInnermostDelegate()));

        connectionA.close();
        connectionB.close();
    }

    @Test
    public void testSharedTransactionConversion() throws Exception {
        final DelegatingConnection<?> connectionA = (DelegatingConnection<?>) newConnection();
        final DelegatingConnection<?> connectionB = (DelegatingConnection<?>) newConnection();

        // in a transaction the inner connections should be equal
        assertNotEquals(connectionA, connectionB);
        assertNotEquals(connectionB, connectionA);
        assertTrue(connectionA.innermostDelegateEquals(connectionB.getInnermostDelegate()));
        assertTrue(connectionB.innermostDelegateEquals(connectionA.getInnermostDelegate()));

        transactionManager.commit();

        // use the connection so it adjusts to the completed transaction
        connectionA.getAutoCommit();
        connectionB.getAutoCommit();

        // no there is no transaction so inner connections should not be equal
        assertNotEquals(connectionA, connectionB);
        assertNotEquals(connectionB, connectionA);
        assertFalse(connectionA.innermostDelegateEquals(connectionB.getInnermostDelegate()));
        assertFalse(connectionB.innermostDelegateEquals(connectionA.getInnermostDelegate()));

        transactionManager.begin();

        // use the connection so it adjusts to the new transaction
        connectionA.getAutoCommit();
        connectionB.getAutoCommit();

        // back in a transaction so inner connections should be equal again
        assertNotEquals(connectionA, connectionB);
        assertNotEquals(connectionB, connectionA);
        assertTrue(connectionA.innermostDelegateEquals(connectionB.getInnermostDelegate()));
        assertTrue(connectionB.innermostDelegateEquals(connectionA.getInnermostDelegate()));

        connectionA.close();
        connectionB.close();
    }
}
