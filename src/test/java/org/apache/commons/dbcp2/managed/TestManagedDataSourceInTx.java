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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.commons.dbcp2.DelegatingConnection;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.PreparedStatement;

import javax.transaction.Transaction;

/**
 * TestSuite for ManagedDataSource with an active transaction in progress.
 */
public class TestManagedDataSourceInTx extends TestManagedDataSource {

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        transactionManager.begin();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        if (transactionManager.getTransaction() != null) {
            transactionManager.commit();
        }
        super.tearDown();
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
    public void testNestedConnections() {
        // Not supported
    }

    @Override
    @Test
    public void testConnectionsAreDistinct() throws Exception {
        final Connection[] conn = new Connection[getMaxTotal()];
        for(int i=0;i<conn.length;i++) {
            conn[i] = newConnection();
            for(int j=0;j<i;j++) {
                // two connections should be distinct instances
                Assert.assertNotSame(conn[j], conn[i]);
                // neither should they should be equivalent even though they are
                // sharing the same underlying connection
                Assert.assertNotEquals(conn[j], conn[i]);
                // Check underlying connection is the same
                Assert.assertEquals(((DelegatingConnection<?>) conn[j]).getInnermostDelegateInternal(),
                        ((DelegatingConnection<?>) conn[i]).getInnermostDelegateInternal());
            }
        }
        for (final Connection element : conn) {
            element.close();
        }
    }

    @Override
    @Test
    public void testHashCode() throws Exception {
        final Connection conn1 = newConnection();
        assertNotNull(conn1);
        final Connection conn2 = newConnection();
        assertNotNull(conn2);

        // shared connections should not have the same hashcode
        Assert.assertNotEquals(conn1.hashCode(), conn2.hashCode());
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
        connection.close();
        sharedConnection.close();
    }

    @Override
    @Test
    public void testSharedConnection() throws Exception {
        final DelegatingConnection<?> connectionA = (DelegatingConnection<?>) newConnection();
        final DelegatingConnection<?> connectionB = (DelegatingConnection<?>) newConnection();

        assertFalse(connectionA.equals(connectionB));
        assertFalse(connectionB.equals(connectionA));
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
        assertFalse(connectionA.equals(connectionB));
        assertFalse(connectionB.equals(connectionA));
        assertTrue(connectionA.innermostDelegateEquals(connectionB.getInnermostDelegate()));
        assertTrue(connectionB.innermostDelegateEquals(connectionA.getInnermostDelegate()));

        transactionManager.commit();

        // use the connection so it adjusts to the completed transaction
        connectionA.getAutoCommit();
        connectionB.getAutoCommit();

        // no there is no transaction so inner connections should not be equal
        assertFalse(connectionA.equals(connectionB));
        assertFalse(connectionB.equals(connectionA));
        assertFalse(connectionA.innermostDelegateEquals(connectionB.getInnermostDelegate()));
        assertFalse(connectionB.innermostDelegateEquals(connectionA.getInnermostDelegate()));

        transactionManager.begin();

        // use the connection so it adjusts to the new transaction
        connectionA.getAutoCommit();
        connectionB.getAutoCommit();

        // back in a transaction so inner connections should be equal again
        assertFalse(connectionA.equals(connectionB));
        assertFalse(connectionB.equals(connectionA));
        assertTrue(connectionA.innermostDelegateEquals(connectionB.getInnermostDelegate()));
        assertTrue(connectionB.innermostDelegateEquals(connectionA.getInnermostDelegate()));

        connectionA.close();
        connectionB.close();
    }

    @Test
    public void testCloseInTransaction() throws Exception {
        final DelegatingConnection<?> connectionA = (DelegatingConnection<?>) newConnection();
        final DelegatingConnection<?> connectionB = (DelegatingConnection<?>) newConnection();

        assertFalse(connectionA.equals(connectionB));
        assertFalse(connectionB.equals(connectionA));
        assertTrue(connectionA.innermostDelegateEquals(connectionB.getInnermostDelegate()));
        assertTrue(connectionB.innermostDelegateEquals(connectionA.getInnermostDelegate()));

        connectionA.close();
        connectionB.close();

        final Connection connection = newConnection();

        assertFalse("Connection should be open", connection.isClosed());

        connection.close();

        assertTrue("Connection should be closed", connection.isClosed());
    }

    @Override
    @Test
    public void testAutoCommitBehavior() throws Exception {
        final Connection connection = newConnection();

        // auto commit should be off
        assertFalse("Auto-commit should be disabled", connection.getAutoCommit());

        // attempt to set auto commit
        try {
            connection.setAutoCommit(true);
            fail("setAutoCommit method should be disabled while enlisted in a transaction");
        } catch (final SQLException e) {
            // expected
        }

        // make sure it is still disabled
        assertFalse("Auto-commit should be disabled", connection.getAutoCommit());

        // close connection
        connection.close();
    }

    @Test
    public void testCommit() throws Exception {
        final Connection connection = newConnection();

        // connection should be open
        assertFalse("Connection should be open", connection.isClosed());

        // attempt commit directly
        try {
            connection.commit();
            fail("commit method should be disabled while enlisted in a transaction");
        } catch (final SQLException e) {
            // expected
        }

        // make sure it is still open
        assertFalse("Connection should be open", connection.isClosed());

        // close connection
        connection.close();
    }

    @Test
    public void testReadOnly() throws Exception {
        final Connection connection = newConnection();

        // NOTE: This test class uses connections that are read-only by default

        // connection should be read only
        assertTrue("Connection be read-only", connection.isReadOnly());

        // attempt to setReadOnly
        try {
            connection.setReadOnly(true);
            fail("setReadOnly method should be disabled while enlisted in a transaction");
        } catch (final SQLException e) {
            // expected
        }

        // make sure it is still read-only
        assertTrue("Connection be read-only", connection.isReadOnly());

        // attempt to setReadonly
        try {
            connection.setReadOnly(false);
            fail("setReadOnly method should be disabled while enlisted in a transaction");
        } catch (final SQLException e) {
            // expected
        }

        // make sure it is still read-only
        assertTrue("Connection be read-only", connection.isReadOnly());

        // close connection
        connection.close();
    }

    // can't actually test close in a transaction
    @Override
    protected void assertBackPointers(final Connection conn, final Statement statement) throws SQLException {
        assertFalse(conn.isClosed());
        assertFalse(isClosed(statement));

        assertSame("statement.getConnection() should return the exact same connection instance that was used to create the statement",
                conn, statement.getConnection());

        final ResultSet resultSet = statement.getResultSet();
        assertFalse(isClosed(resultSet));
        assertSame("resultSet.getStatement() should return the exact same statement instance that was used to create the result set",
                statement, resultSet.getStatement());

        final ResultSet executeResultSet = statement.executeQuery("select * from dual");
        assertFalse(isClosed(executeResultSet));
        assertSame("resultSet.getStatement() should return the exact same statement instance that was used to create the result set",
                statement, executeResultSet.getStatement());

        final ResultSet keysResultSet = statement.getGeneratedKeys();
        assertFalse(isClosed(keysResultSet));
        assertSame("resultSet.getStatement() should return the exact same statement instance that was used to create the result set",
                statement, keysResultSet.getStatement());

        ResultSet preparedResultSet = null;
        if (statement instanceof PreparedStatement) {
            final PreparedStatement preparedStatement = (PreparedStatement) statement;
            preparedResultSet = preparedStatement.executeQuery();
            assertFalse(isClosed(preparedResultSet));
            assertSame("resultSet.getStatement() should return the exact same statement instance that was used to create the result set",
                    statement, preparedResultSet.getStatement());
        }


        resultSet.getStatement().getConnection().close();
    }

    @Override
    @Test
    public void testConnectionReturnOnCommit() throws Exception {
         // override with no-op test
         return;
    }
}
