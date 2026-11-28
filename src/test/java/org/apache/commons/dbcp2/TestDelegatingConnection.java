/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.dbcp2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Savepoint;

import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link DelegatingConnection}.
 */
public class TestDelegatingConnection extends AbstractDatabaseTest {

    /**
     * Delegate that will throw RTE on toString Used to validate fix for DBCP-241
     */
    static final class RTEGeneratingConnection extends TesterConnection {

        public RTEGeneratingConnection() {
            super("", "");
        }

        /**
         * Always throws {@link RuntimeException}.
         *
         * @throws RuntimeException Thrown always.
         */
        @Override
        public String toString() {
            throw new RuntimeException("bang!");
        }

    }

    private DelegatingConnection<? extends Connection> delegatingConnection;
    private Connection connection;
    private Connection connection2;
    private DelegatingConnection<? extends Connection> databaseConnection;
    private TesterStatement testerStatement;
    private TesterResultSet testerResultSet;

    @AfterEach
    public void afterEach() throws SQLException {
        testerStatement.setSqlExceptionOnClose(false);
        testerResultSet.setSqlExceptionOnClose(false);
        databaseConnection.close();
    }

    @BeforeEach
    public void setUp() throws Exception {
        connection = new TesterConnection("test", "test");
        connection2 = new TesterConnection("test", "test");
        delegatingConnection = new DelegatingConnection<>(connection);
        testerStatement = new TesterStatement(delegatingConnection);
        testerResultSet = new TesterResultSet(testerStatement);
        databaseConnection = new DelegatingConnection<>(database.connect());
    }

    /**
     * A JDBC object factory that may report an unsupported feature.
     */
    @FunctionalInterface
    private interface JdbcFactory {
        Object create() throws SQLException;
    }

    /**
     * Checks successful delegation or propagation of the driver's unsupported-feature exception.
     */
    private static void assertCreation(final JdbcFactory physical, final JdbcFactory wrapped) throws SQLException {
        final Object expected;
        try {
            expected = physical.create();
        } catch (final SQLException e) {
            if (!(e instanceof SQLFeatureNotSupportedException) && !"0A000".equals(e.getSQLState())) {
                throw e;
            }
            final SQLException actual = assertThrowsExactly(e.getClass(), wrapped::create);
            assertEquals(e.getSQLState(), actual.getSQLState());
            return;
        }
        try {
            final Object actual = wrapped.create();
            try {
                assertNotNull(actual);
                assertEquals(expected.getClass(), actual.getClass());
            } finally {
                free(actual);
            }
        } finally {
            free(expected);
        }
    }

    /**
     * Frees the temporary JDBC values allocated by the factory checks.
     */
    private static void free(final Object value) throws SQLException {
        if (value instanceof java.sql.Blob) {
            ((java.sql.Blob) value).free();
        } else if (value instanceof java.sql.Clob) {
            ((java.sql.Clob) value).free();
        } else if (value instanceof java.sql.Array) {
            ((java.sql.Array) value).free();
        } else if (value instanceof java.sql.SQLXML) {
            ((java.sql.SQLXML) value).free();
        }
    }

    @Test
    void testAbort() throws Exception {
        databaseConnection.abort(Runnable::run);
    }

    @Test
    void testAbortClosedWrapperDoesNotTouchDelegate() throws SQLException {
        delegatingConnection.setClosedInternal(true);
        delegatingConnection.abort(Runnable::run);
        assertFalse(((TesterConnection) connection).isAborted());
        assertFalse(connection.isClosed());
    }

    @Test
    void testAbortMissingDelegate() throws SQLException {
        delegatingConnection.setDelegate(null);
        delegatingConnection.abort(Runnable::run);
        assertTrue(delegatingConnection.isClosed());
        assertFalse(((TesterConnection) connection).isAborted());
    }

    @Test
    void testAbortOpenWrapper() throws SQLException {
        delegatingConnection.abort(Runnable::run);
        assertTrue(((TesterConnection) connection).isAborted());
        assertTrue(delegatingConnection.isClosed());
    }

    @Test
    void testAbortPreservesSQLException() {
        final SQLException failure = new SQLException("Abort failed", "HY000", 42);
        ((TesterConnection) connection).setFailure(failure);
        assertSame(failure, assertThrows(SQLException.class, () -> delegatingConnection.abort(Runnable::run)));
    }

    @Test
    void testCheckOpen() throws Exception {
        delegatingConnection.checkOpen();
        delegatingConnection.close();
        assertThrows(SQLException.class, delegatingConnection::checkOpen);
    }

    /**
     * Verify fix for DBCP-241
     */
    @Test
    void testCheckOpenNull() throws Exception {
        delegatingConnection.close();
        SQLException e = assertThrows(SQLException.class, delegatingConnection::checkOpen);
        assertTrue(e.getMessage().endsWith("is closed."));

        delegatingConnection = new DelegatingConnection<>(null);
        delegatingConnection.setClosedInternal(true);
        e = assertThrows(SQLException.class, delegatingConnection::checkOpen);
        assertTrue(e.getMessage().endsWith("is null."));

        final PoolingConnection pc = new PoolingConnection(connection2);
        pc.setStatementPool(new GenericKeyedObjectPool<>(pc));
        delegatingConnection = new DelegatingConnection<>(pc);
        pc.close();
        delegatingConnection.close();
        e = assertThrows(SQLException.class, () -> delegatingConnection.prepareStatement(""));
        assertTrue(e.getMessage().endsWith("is closed."));

        delegatingConnection = new DelegatingConnection<>(new RTEGeneratingConnection());
        delegatingConnection.close();
        e = assertThrows(SQLException.class, delegatingConnection::checkOpen);
        assertTrue(e.getMessage().endsWith("is closed."));
    }

    @Test
    void testCommit() throws Exception {
        databaseConnection.setAutoCommit(false);
        databaseConnection.commit();
    }

    @Test
    void testConnectionToString() throws Exception {
        final String s = delegatingConnection.toString();
        assertNotNull(s);
        assertFalse(s.isEmpty());
    }

    @Test
    void testCreateArrayOf() throws Exception {
        if (database == TestDatabase.SQLSERVER) {
            // SQL Server reports unsupported array creation as a generic SQLException.
            assertThrows(SQLException.class, () -> databaseConnection.createArrayOf("VARCHAR", new Object[] {"A", "B"}));
            return;
        }
        assertCreation(() -> databaseConnection.getDelegate().createArrayOf("VARCHAR", new Object[] {"A", "B"}),
                () -> databaseConnection.createArrayOf("VARCHAR", new Object[] {"A", "B"}));
    }

    @Test
    void testCreateBlob() throws Exception {
        assertCreation(() -> databaseConnection.getDelegate().createBlob(), databaseConnection::createBlob);
    }

    @Test
    void testCreateClob() throws Exception {
        assertCreation(() -> databaseConnection.getDelegate().createClob(), databaseConnection::createClob);
    }

    @Test
    void testCreateNClob() throws Exception {
        assertCreation(() -> databaseConnection.getDelegate().createNClob(), databaseConnection::createNClob);
    }

    @Test
    void testCreateSQLXML() throws Exception {
        assertCreation(() -> databaseConnection.getDelegate().createSQLXML(), databaseConnection::createSQLXML);
    }

    @Test
    void testCreateStruct() throws Exception {
        // CHARACTER is not a user-defined SQL structured type.
        assertThrows(SQLException.class, () -> databaseConnection.createStruct("CHARACTER", new Object[] { "A", "B" }));
    }

    @Test
    void testGetCacheState() throws Exception {
        assertTrue(databaseConnection.getCacheState());
    }

    @Test
    void testGetClientInfo() throws Exception {
        if (database == TestDatabase.HSQLDB) {
            assertNull(databaseConnection.getClientInfo());
        } else {
            assertNotNull(databaseConnection.getClientInfo());
        }
    }

    @Test
    void testGetClientInfoString() throws Exception {
        if (database == TestDatabase.ORACLE) {
            assertThrows(java.sql.SQLClientInfoException.class, () -> databaseConnection.getClientInfo("xyz"));
        } else {
            assertNull(databaseConnection.getClientInfo("xyz"));
        }
    }

    @Test
    void testGetDefaultQueryTimeout() throws Exception {
        assertNull(databaseConnection.getDefaultQueryTimeout());
    }

    @Test
    void testGetDefaultQueryTimeoutDuration() throws Exception {
        assertNull(databaseConnection.getDefaultQueryTimeoutDuration());
    }

    @Test
    void testGetDelegate() throws Exception {
        assertEquals(connection, delegatingConnection.getDelegate());
    }

    @Test
    void testGetHoldability() throws Exception {
        assertEquals(databaseConnection.getDelegate().getHoldability(), databaseConnection.getHoldability());
    }

    @Test
    void testGetNetworkTimeout() throws Exception {
        assertEquals(databaseConnection.getDelegate().getNetworkTimeout(), databaseConnection.getNetworkTimeout());
    }

    @Test
    void testGetTypeMap() throws Exception {
        assertEquals(databaseConnection.getDelegate().getTypeMap(), databaseConnection.getTypeMap());
    }

    @Test
    void testIsClosed() throws Exception {
        delegatingConnection.checkOpen();
        assertFalse(delegatingConnection.isClosed());
        delegatingConnection.close();
        assertTrue(delegatingConnection.isClosed());
    }

    @Test
    void testIsClosedNullDelegate() throws Exception {
        delegatingConnection.checkOpen();
        assertFalse(delegatingConnection.isClosed());
        delegatingConnection.setDelegate(null);
        assertTrue(delegatingConnection.isClosed());
    }

    @SuppressWarnings("resource")
    @Test
    void testIsWrapperFor() throws Exception {
        assertTrue(delegatingConnection.isWrapperFor(delegatingConnection.getClass()));
        assertTrue(delegatingConnection.isWrapperFor(delegatingConnection.getDelegate().getClass()));
        assertThrows(SQLException.class, () -> delegatingConnection.isWrapperFor(Integer.class));
    }

    @Test
    void testNativeSQL() throws Exception {
        assertNotNull(databaseConnection.nativeSQL("select 1"));
    }

    @Test
    void testPassivateWithResultSetCloseException() {
        testerResultSet.setSqlExceptionOnClose(true);
        delegatingConnection.addTrace(testerResultSet);
        final SQLException e = assertThrows(SQLException.class, delegatingConnection::passivate);
        assertInstanceOf(SQLExceptionList.class, e);
        assertEquals(1, ((SQLExceptionList) e).getCauseList().size());
    }

    @Test
    void testPassivateWithResultSetCloseExceptionAndStatementCloseException() {
        testerStatement.setSqlExceptionOnClose(true);
        testerResultSet.setSqlExceptionOnClose(true);
        delegatingConnection.addTrace(testerStatement);
        delegatingConnection.addTrace(testerResultSet);
        final SQLException e = assertThrows(SQLException.class, delegatingConnection::passivate);
        assertInstanceOf(SQLExceptionList.class, e);
        assertEquals(2, ((SQLExceptionList) e).getCauseList().size());
    }

    @Test
    void testPassivateWithStatementCloseException() {
        testerStatement.setSqlExceptionOnClose(true);
        delegatingConnection.addTrace(testerStatement);
        final SQLException e = assertThrows(SQLException.class, delegatingConnection::passivate);
        assertInstanceOf(SQLExceptionList.class, e);
        assertEquals(1, ((SQLExceptionList) e).getCauseList().size());
    }

    @Test
    void testReleaseSavepoint() throws Exception {
        databaseConnection.setAutoCommit(false);
        final Savepoint s = databaseConnection.setSavepoint();
        if (database == TestDatabase.ORACLE || database == TestDatabase.SQLSERVER) {
            assertThrows(SQLFeatureNotSupportedException.class, () -> databaseConnection.releaseSavepoint(s));
        } else {
            databaseConnection.releaseSavepoint(s);
        }
    }

    @Test
    void testRollback() throws Exception {
        databaseConnection.setAutoCommit(false);
        databaseConnection.rollback();
    }

    @Test
    void testRollbackSavepoint() throws Exception {
        databaseConnection.setAutoCommit(false);
        try {
            databaseConnection.rollback(databaseConnection.setSavepoint());
        } finally {
            databaseConnection.setAutoCommit(true);
        }
    }

    @Test
    void testSetClientInfo() throws Exception {
        // TODO
        // databaseConnection.setClientInfo("ApplicationName", "app1");
    }

    @Test
    void testSetDefaultQueryTimeout() throws Exception {
        final int expected = 1;
        delegatingConnection.setDefaultQueryTimeout(expected);
        assertEquals(expected, delegatingConnection.getDefaultQueryTimeout());
    }

    @Test
    void testSetHoldability() throws Exception {
        final int expected = 1;
        databaseConnection.setHoldability(expected);
        assertEquals(expected, databaseConnection.getHoldability());
    }

    @Test
    void testSetNetworkTimeout() throws Exception {
        if (database == TestDatabase.HSQLDB) {
            assertThrows(SQLFeatureNotSupportedException.class, () -> databaseConnection.setNetworkTimeout(Runnable::run, 30_000));
        } else {
            databaseConnection.setNetworkTimeout(Runnable::run, 30_000);
        }
        assertEquals(databaseConnection.getDelegate().getNetworkTimeout(), databaseConnection.getNetworkTimeout());
    }

    @Test
    void testSetSavepoint() throws Exception {
        databaseConnection.setAutoCommit(false);
        databaseConnection.setSavepoint();
    }

    @SuppressWarnings("javadoc")
    @Test
    void testUnwrap() throws Exception {
        assertNotNull(delegatingConnection.unwrap(delegatingConnection.getClass()));
        assertNotNull(delegatingConnection.unwrap(delegatingConnection.getDelegate().getClass()));
        assertThrows(SQLException.class, () -> delegatingConnection.unwrap(Integer.class));
    }

}
