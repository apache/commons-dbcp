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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Savepoint;

import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link DelegatingConnection}.
 */
public class TestDelegatingConnection {

    /**
     * Delegate that doesn't support read-only or auto-commit. It will merely take the input value of setReadOnly and setAutoCommit and discard it, to keep
     * false.
     */
    static final class NoReadOnlyOrAutoCommitConnection extends TesterConnection {
        private final boolean readOnly = false;
        private final boolean autoCommit = false;

        public NoReadOnlyOrAutoCommitConnection() {
            super("", "");
        }

        @Override
        public boolean getAutoCommit() {
            return autoCommit;
        }

        @Override
        public boolean isReadOnly() throws SQLException {
            return readOnly;
        }

        @Override
        public void setAutoCommit(final boolean autoCommit) {
            // Do nothing
        }

        @Override
        public void setReadOnly(final boolean readOnly) {
            // Do nothing
        }
    }

    /**
     * Delegate that will throw RTE on toString Used to validate fix for DBCP-241
     */
    static final class RTEGeneratingConnection extends TesterConnection {

        public RTEGeneratingConnection() {
            super("", "");
        }

        @Override
        public String toString() {
            throw new RuntimeException("bang!");
        }

    }

    private DelegatingConnection<? extends Connection> delegatingConnection;
    private Connection connection;
    private Connection connection2;
    private DelegatingConnection<? extends Connection> h2DConnection;
    private TesterStatement testerStatement;
    private TesterResultSet testerResultSet;

    @AfterEach
    public void afterEach() throws SQLException {
        testerStatement.setSqlExceptionOnClose(false);
        testerResultSet.setSqlExceptionOnClose(false);
        h2DConnection.close();
    }

    @BeforeEach
    public void setUp() throws Exception {
        connection = new TesterConnection("test", "test");
        connection2 = new TesterConnection("test", "test");
        delegatingConnection = new DelegatingConnection<>(connection);
        testerStatement = new TesterStatement(delegatingConnection);
        testerResultSet = new TesterResultSet(testerStatement);
        h2DConnection = new DelegatingConnection<>(DriverManager.getConnection("jdbc:h2:mem:test"));
    }

    @Test
    void testAbort() throws Exception {
        h2DConnection.abort(r -> {
        });
    }

    @Test
    void testAutoCommitCaching() throws SQLException {
        final Connection con = new NoReadOnlyOrAutoCommitConnection();
        final DelegatingConnection<Connection> delCon = new DelegatingConnection<>(con);

        delCon.setAutoCommit(true);

        assertFalse(con.getAutoCommit());
        assertFalse(delCon.getAutoCommit());
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
        h2DConnection.commit();
    }

    @Test
    void testConnectionToString() throws Exception {
        final String s = delegatingConnection.toString();
        assertNotNull(s);
        assertFalse(s.isEmpty());
    }

    @Test
    void testCreateArrayOf() throws Exception {
        assertNotNull(h2DConnection.createArrayOf("CHARACTER", new Object[] { "A", "B" }));
    }

    @Test
    void testCreateBlob() throws Exception {
        assertNotNull(h2DConnection.createBlob());
    }

    @Test
    void testCreateClob() throws Exception {
        assertNotNull(h2DConnection.createClob());
    }

    @Test
    void testCreateNClob() throws Exception {
        assertNotNull(h2DConnection.createNClob());
    }

    @Test
    void testCreateSQLXML() throws Exception {
        assertNotNull(h2DConnection.createSQLXML());
    }

    @Test
    void testCreateStruct() throws Exception {
        // not supported by H2
        assertThrows(SQLException.class, () -> h2DConnection.createStruct("CHARACTER", new Object[] { "A", "B" }));
    }

    @Test
    void testGetCacheState() throws Exception {
        assertTrue(h2DConnection.getCacheState());
    }

    @Test
    void testGetClientInfo() throws Exception {
        assertNotNull(h2DConnection.getClientInfo());
    }

    @Test
    void testGetClientInfoString() throws Exception {
        assertNull(h2DConnection.getClientInfo("xyz"));
    }

    @Test
    void testGetDefaultQueryTimeout() throws Exception {
        assertNull(h2DConnection.getDefaultQueryTimeout());
    }

    @Test
    void testGetDefaultQueryTimeoutDuration() throws Exception {
        assertNull(h2DConnection.getDefaultQueryTimeoutDuration());
    }

    @Test
    void testGetDelegate() throws Exception {
        assertEquals(connection, delegatingConnection.getDelegate());
    }

    @Test
    void testGetHoldability() throws Exception {
        assertEquals(1, h2DConnection.getHoldability());
    }

    @Test
    void testGetNetworkTimeout() throws Exception {
        assertEquals(0, h2DConnection.getNetworkTimeout());
    }

    @Test
    void testGetTypeMap() throws Exception {
        assertNull(h2DConnection.getTypeMap());
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
        assertNotNull(h2DConnection.nativeSQL("select 1"));
    }

    @Test
    void testPassivateWithResultSetCloseException() {
        testerResultSet.setSqlExceptionOnClose(true);
        delegatingConnection.addTrace(testerResultSet);
        final SQLException e = assertThrows(SQLException.class, delegatingConnection::passivate);
        Assertions.assertInstanceOf(SQLExceptionList.class, e);
        Assertions.assertEquals(1, ((SQLExceptionList) e).getCauseList().size());
    }

    @Test
    void testPassivateWithResultSetCloseExceptionAndStatementCloseException() {
        testerStatement.setSqlExceptionOnClose(true);
        testerResultSet.setSqlExceptionOnClose(true);
        delegatingConnection.addTrace(testerStatement);
        delegatingConnection.addTrace(testerResultSet);
        final SQLException e = assertThrows(SQLException.class, delegatingConnection::passivate);
        Assertions.assertInstanceOf(SQLExceptionList.class, e);
        Assertions.assertEquals(2, ((SQLExceptionList) e).getCauseList().size());
    }

    @Test
    void testPassivateWithStatementCloseException() {
        testerStatement.setSqlExceptionOnClose(true);
        delegatingConnection.addTrace(testerStatement);
        final SQLException e = assertThrows(SQLException.class, delegatingConnection::passivate);
        Assertions.assertInstanceOf(SQLExceptionList.class, e);
        Assertions.assertEquals(1, ((SQLExceptionList) e).getCauseList().size());
    }

    @Test
    void testReadOnlyCaching() throws SQLException {
        final Connection con = new NoReadOnlyOrAutoCommitConnection();
        final DelegatingConnection<Connection> delCon = new DelegatingConnection<>(con);

        delCon.setReadOnly(true);

        assertFalse(con.isReadOnly());
        assertFalse(delCon.isReadOnly());
    }

    @Test
    void testReleaseSavepoint() throws Exception {
        final Savepoint s = h2DConnection.setSavepoint();
        h2DConnection.releaseSavepoint(s);
    }

    @Test
    void testRollback() throws Exception {
        h2DConnection.rollback();
    }

    @Test
    void testRollbackSavepoint() throws Exception {
        h2DConnection.setAutoCommit(false);
        try {
            h2DConnection.rollback(h2DConnection.setSavepoint());
        } finally {
            h2DConnection.setAutoCommit(true);
        }
    }

    @Test
    void testSetClientInfo() throws Exception {
        // TODO
        // h2DConnection.setClientInfo("ApplicationName", "app1");
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
        h2DConnection.setHoldability(expected);
        assertEquals(expected, h2DConnection.getHoldability());
    }

    @Test
    void testSetNetworkTimeout() throws Exception {
        h2DConnection.setNetworkTimeout(r -> {
        }, 1);
        assertEquals(0, h2DConnection.getNetworkTimeout());
    }

    @Test
    void testSetSavepoint() throws Exception {
        h2DConnection.setSavepoint();
    }

    @SuppressWarnings("javadoc")
    @Test
    void testUnwrap() throws Exception {
        assertNotNull(delegatingConnection.unwrap(delegatingConnection.getClass()));
        assertNotNull(delegatingConnection.unwrap(delegatingConnection.getDelegate().getClass()));
        assertThrows(SQLException.class, () -> delegatingConnection.unwrap(Integer.class));
    }

}
