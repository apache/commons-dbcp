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
package org.apache.commons.dbcp.managed;

import org.apache.commons.dbcp.DelegatingConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.PreparedStatement;

import junit.framework.Test;
import junit.framework.TestSuite;

import javax.transaction.Transaction;

/**
 * TestSuite for ManagedDataSource with an active transaction in progress.
 *
 * @author Dain Sundstrom
 * @version $Revision$
 */
public class TestManagedDataSourceInTx extends TestManagedDataSource {

    public TestManagedDataSourceInTx(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(TestManagedDataSourceInTx.class);
    }

    public void setUp() throws Exception {
        super.setUp();

        transactionManager.begin();
    }

    public void tearDown() throws Exception {
        if (transactionManager.getTransaction() != null) {
            transactionManager.commit();
        }
        super.tearDown();
    }

    /**
     * @see #testSharedConnection()
     */
    public void testManagedConnectionEqualsFail() throws Exception {
        // this test is invalid for managed conections since because
        // two connections to the same datasource are supposed to share
        // a single connection
    }

    public void testConnectionsAreDistinct() throws Exception {
        Connection[] conn = new Connection[getMaxActive()];
        for(int i=0;i<conn.length;i++) {
            conn[i] = newConnection();
            for(int j=0;j<i;j++) {
                // two connections should be distinct instances
                assertNotSame(conn[j], conn[i]);
                // but they should be equivilant since they are sharing the same underlying connection 
                assertEquals(conn[j], conn[i]);
            }
        }
        for(int i=0;i<conn.length;i++) {
            conn[i].close();
        }
    }

    public void testHashCode() throws Exception {
        Connection conn1 = newConnection();
        assertNotNull(conn1);
        Connection conn2 = newConnection();
        assertNotNull(conn2);

        // shared connections should have the same hashcode
        assertEquals(conn1.hashCode(), conn2.hashCode());
    }

    public void testMaxActive() throws Exception {
        Transaction[] transactions = new Transaction[getMaxActive()];
        Connection[] c = new Connection[getMaxActive()];
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
            fail("Allowed to open more than DefaultMaxActive connections.");
        } catch (java.sql.SQLException e) {
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

    public void testClearWarnings() throws Exception {
        // open a connection
        Connection connection = newConnection();
        assertNotNull(connection);

        // generate SQLWarning on connection
        connection.prepareCall("warning");
        assertNotNull(connection.getWarnings());

        // create a new shared connection
        Connection sharedConnection = newConnection();

        // shared connection should see warning
        assertNotNull(sharedConnection.getWarnings());

        // close and allocate a new (original) connection
        connection.close();
        connection = newConnection();

        // warnings should not have been cleared by closing the connection
        assertNotNull(connection.getWarnings());
        assertNotNull(sharedConnection.getWarnings());

        connection.close();
        sharedConnection.close();
    }

    public void testSharedConnection() throws Exception {
        DelegatingConnection connectionA = (DelegatingConnection) newConnection();
        DelegatingConnection connectionB = (DelegatingConnection) newConnection();

        assertTrue(connectionA.equals(connectionB));
        assertTrue(connectionB.equals(connectionA));
        assertTrue(connectionA.innermostDelegateEquals(connectionB.getInnermostDelegate()));
        assertTrue(connectionB.innermostDelegateEquals(connectionA.getInnermostDelegate()));

        connectionA.close();
        connectionB.close();
    }

    public void testSharedTransactionConversion() throws Exception {
        DelegatingConnection connectionA = (DelegatingConnection) newConnection();
        DelegatingConnection connectionB = (DelegatingConnection) newConnection();

        // in a transaciton the connections should be equal
        assertTrue(connectionA.equals(connectionB));
        assertTrue(connectionB.equals(connectionA));
        assertTrue(connectionA.innermostDelegateEquals(connectionB.getInnermostDelegate()));
        assertTrue(connectionB.innermostDelegateEquals(connectionA.getInnermostDelegate()));

        transactionManager.commit();

        // use the connection so it adjusts to the completed transaction
        connectionA.getAutoCommit();
        connectionB.getAutoCommit();

        // no there is no transaction so connections should not be equal
        assertFalse(connectionA.equals(connectionB));
        assertFalse(connectionB.equals(connectionA));
        assertFalse(connectionA.innermostDelegateEquals(connectionB.getInnermostDelegate()));
        assertFalse(connectionB.innermostDelegateEquals(connectionA.getInnermostDelegate()));

        transactionManager.begin();

        // use the connection so it adjusts to the new transaction
        connectionA.getAutoCommit();
        connectionB.getAutoCommit();

        // back in a transaction so should be equal again
        assertTrue(connectionA.equals(connectionB));
        assertTrue(connectionB.equals(connectionA));
        assertTrue(connectionA.innermostDelegateEquals(connectionB.getInnermostDelegate()));
        assertTrue(connectionB.innermostDelegateEquals(connectionA.getInnermostDelegate()));

        connectionA.close();
        connectionB.close();
    }

    public void testCloseInTransaction() throws Exception {
        DelegatingConnection connectionA = (DelegatingConnection) newConnection();
        DelegatingConnection connectionB = (DelegatingConnection) newConnection();

        assertTrue(connectionA.equals(connectionB));
        assertTrue(connectionB.equals(connectionA));
        assertTrue(connectionA.innermostDelegateEquals(connectionB.getInnermostDelegate()));
        assertTrue(connectionB.innermostDelegateEquals(connectionA.getInnermostDelegate()));

        connectionA.close();
        connectionB.close();

        Connection connection = newConnection();

        // conection should be open
        assertFalse("Connection should be open", connection.isClosed());

        // close the connection
        connection.close();

        // conection should be open
        assertTrue("Connection should be closed", connection.isClosed());


        // conection should be open
        assertTrue("Connection should be closed", connection.isClosed());
    }

    public void testAutoCommitBehavior() throws Exception {
        Connection connection = newConnection();

        // auto commit should be off
        assertFalse("Auto-commit should be disabled", connection.getAutoCommit());

        // attempt to set auto commit
        try {
            connection.setAutoCommit(true);
            fail("setAutoCommit method should be disabled while enlisted in a transaction");
        } catch (SQLException e) {
            // expected
        }

        // make sure it is still disabled
        assertFalse("Auto-commit should be disabled", connection.getAutoCommit());

        // close connection
        connection.close();
    }

    public void testCommit() throws Exception {
        Connection connection = newConnection();

        // conection should be open
        assertFalse("Connection should be open", connection.isClosed());

        // attempt commit directly
        try {
            connection.commit();
            fail("commit method should be disabled while enlisted in a transaction");
        } catch (SQLException e) {
            // expected
        }

        // make sure it is still open
        assertFalse("Connection should be open", connection.isClosed());

        // close connection
        connection.close();
    }

    public void testReadOnly() throws Exception {
        Connection connection = newConnection();

        // NOTE: This test class uses connections that are read-only by default
        
        // conection should be read only
        assertTrue("Connection be read-only", connection.isReadOnly());

        // attempt to setReadOnly
        try {
            connection.setReadOnly(true);
            fail("setReadOnly method should be disabled while enlisted in a transaction");
        } catch (SQLException e) {
            // expected
        }

        // make sure it is still read-only
        assertTrue("Connection be read-only", connection.isReadOnly());

        // attempt to setReadonly
        try {
            connection.setReadOnly(false);
            fail("setReadOnly method should be disabled while enlisted in a transaction");
        } catch (SQLException e) {
            // expected
        }

        // make sure it is still read-only
        assertTrue("Connection be read-only", connection.isReadOnly());

        // close connection
        connection.close();
    }

    // can't actually test close in a transaction
    protected void assertBackPointers(Connection conn, Statement statement) throws SQLException {
        assertFalse(conn.isClosed());
        assertFalse(isClosed(statement));

        assertSame("statement.getConnection() should return the exact same connection instance that was used to create the statement",
                conn, statement.getConnection());

        ResultSet resultSet = statement.getResultSet();
        assertFalse(isClosed(resultSet));
        assertSame("resultSet.getStatement() should return the exact same statement instance that was used to create the result set",
                statement, resultSet.getStatement());

        ResultSet executeResultSet = statement.executeQuery("select * from dual");
        assertFalse(isClosed(executeResultSet));
        assertSame("resultSet.getStatement() should return the exact same statement instance that was used to create the result set",
                statement, executeResultSet.getStatement());

        ResultSet keysResultSet = statement.getGeneratedKeys();
        assertFalse(isClosed(keysResultSet));
        assertSame("resultSet.getStatement() should return the exact same statement instance that was used to create the result set",
                statement, keysResultSet.getStatement());

        ResultSet preparedResultSet = null;
        if (statement instanceof PreparedStatement) {
            PreparedStatement preparedStatement = (PreparedStatement) statement;
            preparedResultSet = preparedStatement.executeQuery();
            assertFalse(isClosed(preparedResultSet));
            assertSame("resultSet.getStatement() should return the exact same statement instance that was used to create the result set",
                    statement, preparedResultSet.getStatement());
        }


        resultSet.getStatement().getConnection().close();
    }

}
