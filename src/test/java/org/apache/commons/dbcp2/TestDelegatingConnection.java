/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import static org.junit.jupiter.api.Assertions.fail;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
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
    static class NoReadOnlyOrAutoCommitConnection extends TesterConnection {
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
    static class RTEGeneratingConnection extends TesterConnection {

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
    public void testAbort() throws Exception {
        h2DConnection.abort(r -> {
        });
    }

    @Test
    public void testAutoCommitCaching() throws SQLException {
        final Connection con = new NoReadOnlyOrAutoCommitConnection();
        final DelegatingConnection<Connection> delCon = new DelegatingConnection<>(con);

        delCon.setAutoCommit(true);

        assertFalse(con.getAutoCommit());
        assertFalse(delCon.getAutoCommit());
    }

    @Test
    public void testCheckOpen() throws Exception {
        delegatingConnection.checkOpen();
        delegatingConnection.close();
        try {
            delegatingConnection.checkOpen();
            fail("Expecting SQLException");
        } catch (final SQLException ex) {
            // expected
        }
    }

    /**
     * Verify fix for DBCP-241
     */
    @Test
    public void testCheckOpenNull() throws Exception {
        try {
            delegatingConnection.close();
            delegatingConnection.checkOpen();
            fail("Expecting SQLException");
        } catch (final SQLException ex) {
            assertTrue(ex.getMessage().endsWith("is closed."));
        }

        try {
            delegatingConnection = new DelegatingConnection<>(null);
            delegatingConnection.setClosedInternal(true);
            delegatingConnection.checkOpen();
            fail("Expecting SQLException");
        } catch (final SQLException ex) {
            assertTrue(ex.getMessage().endsWith("is null."));
        }

        try {
            final PoolingConnection pc = new PoolingConnection(connection2);
            pc.setStatementPool(new GenericKeyedObjectPool<>(pc));
            delegatingConnection = new DelegatingConnection<>(pc);
            pc.close();
            delegatingConnection.close();
            try (PreparedStatement ps = delegatingConnection.prepareStatement("")) {
            }
            fail("Expecting SQLException");
        } catch (final SQLException ex) {
            assertTrue(ex.getMessage().endsWith("is closed."));
        }

        try {
            delegatingConnection = new DelegatingConnection<>(new RTEGeneratingConnection());
            delegatingConnection.close();
            delegatingConnection.checkOpen();
            fail("Expecting SQLException");
        } catch (final SQLException ex) {
            assertTrue(ex.getMessage().endsWith("is closed."));
        }
    }

    @Test
    public void testCommit() throws Exception {
        h2DConnection.commit();
    }

    @Test
    public void testConnectionToString() throws Exception {
        final String s = delegatingConnection.toString();
        assertNotNull(s);
        assertFalse(s.isEmpty());
    }

    @Test
    public void testCreateArrayOf() throws Exception {
        assertNotNull(h2DConnection.createArrayOf("CHARACTER", new Object[] { "A", "B" }));
    }

    @Test
    public void testCreateBlob() throws Exception {
        assertNotNull(h2DConnection.createBlob());
    }

    @Test
    public void testCreateClob() throws Exception {
        assertNotNull(h2DConnection.createClob());
    }

    @Test
    public void testCreateNClob() throws Exception {
        assertNotNull(h2DConnection.createNClob());
    }

    @Test
    public void testCreateSQLXML() throws Exception {
        assertNotNull(h2DConnection.createSQLXML());
    }

    @Test
    public void testCreateStruct() throws Exception {
        // not supported by H2
        assertThrows(SQLException.class, () -> h2DConnection.createStruct("CHARACTER", new Object[] { "A", "B" }));
    }

    @Test
    public void testGetCacheState() throws Exception {
        assertTrue(h2DConnection.getCacheState());
    }

    @Test
    public void testGetClientInfo() throws Exception {
        assertNotNull(h2DConnection.getClientInfo());
    }

    @Test
    public void testGetClientInfoString() throws Exception {
        assertNull(h2DConnection.getClientInfo("xyz"));
    }

    @Test
    public void testGetDefaultQueryTimeout() throws Exception {
        assertNull(h2DConnection.getDefaultQueryTimeout());
    }

    @Test
    public void testGetDefaultQueryTimeoutDuration() throws Exception {
        assertNull(h2DConnection.getDefaultQueryTimeoutDuration());
    }

    @Test
    public void testGetDelegate() throws Exception {
        assertEquals(connection, delegatingConnection.getDelegate());
    }

    @Test
    public void testGetHoldability() throws Exception {
        assertEquals(1, h2DConnection.getHoldability());
    }

    @Test
    public void testGetNetworkTimeout() throws Exception {
        assertEquals(0, h2DConnection.getNetworkTimeout());
    }

    @Test
    public void testGetTypeMap() throws Exception {
        assertNull(h2DConnection.getTypeMap());
    }

    @Test
    public void testIsClosed() throws Exception {
        delegatingConnection.checkOpen();
        assertFalse(delegatingConnection.isClosed());
        delegatingConnection.close();
        assertTrue(delegatingConnection.isClosed());
    }

    @Test
    public void testIsClosedNullDelegate() throws Exception {
        delegatingConnection.checkOpen();
        assertFalse(delegatingConnection.isClosed());
        delegatingConnection.setDelegate(null);
        assertTrue(delegatingConnection.isClosed());
    }

    @SuppressWarnings("resource")
    @Test
    public void testIsWrapperFor() throws Exception {
        assertTrue(delegatingConnection.isWrapperFor(delegatingConnection.getClass()));
        assertTrue(delegatingConnection.isWrapperFor(delegatingConnection.getDelegate().getClass()));
        assertThrows(SQLException.class, () -> delegatingConnection.isWrapperFor(Integer.class));
    }

    @Test
    public void testNativeSQL() throws Exception {
        assertNotNull(h2DConnection.nativeSQL("select 1"));
    }

    @Test
    public void testPassivateWithResultSetCloseException() {
        try {
            testerResultSet.setSqlExceptionOnClose(true);
            delegatingConnection.addTrace(testerResultSet);
            delegatingConnection.passivate();
            Assertions.fail("Expected SQLExceptionList");
        } catch (final SQLException e) {
            Assertions.assertInstanceOf(SQLExceptionList.class, e);
            Assertions.assertEquals(1, ((SQLExceptionList) e).getCauseList().size());
        } finally {
            testerResultSet.setSqlExceptionOnClose(false);
        }
    }

    @Test
    public void testPassivateWithResultSetCloseExceptionAndStatementCloseException() {
        try {
            testerStatement.setSqlExceptionOnClose(true);
            testerResultSet.setSqlExceptionOnClose(true);
            delegatingConnection.addTrace(testerStatement);
            delegatingConnection.addTrace(testerResultSet);
            delegatingConnection.passivate();
            Assertions.fail("Expected SQLExceptionList");
        } catch (final SQLException e) {
            Assertions.assertInstanceOf(SQLExceptionList.class, e);
            Assertions.assertEquals(2, ((SQLExceptionList) e).getCauseList().size());
        } finally {
            testerStatement.setSqlExceptionOnClose(false);
            testerResultSet.setSqlExceptionOnClose(false);
        }
    }

    @Test
    public void testPassivateWithStatementCloseException() {
        try {
            testerStatement.setSqlExceptionOnClose(true);
            delegatingConnection.addTrace(testerStatement);
            delegatingConnection.passivate();
            Assertions.fail("Expected SQLExceptionList");
        } catch (final SQLException e) {
            Assertions.assertInstanceOf(SQLExceptionList.class, e);
            Assertions.assertEquals(1, ((SQLExceptionList) e).getCauseList().size());
        } finally {
            testerStatement.setSqlExceptionOnClose(false);
        }
    }

    @Test
    public void testReadOnlyCaching() throws SQLException {
        final Connection con = new NoReadOnlyOrAutoCommitConnection();
        final DelegatingConnection<Connection> delCon = new DelegatingConnection<>(con);

        delCon.setReadOnly(true);

        assertFalse(con.isReadOnly());
        assertFalse(delCon.isReadOnly());
    }

    @Test
    public void testReleaseSavepoint() throws Exception {
        final Savepoint s = h2DConnection.setSavepoint();
        h2DConnection.releaseSavepoint(s);
    }

    @Test
    public void testRollback() throws Exception {
        h2DConnection.rollback();
    }

    @Test
    public void testRollbackSavepoint() throws Exception {
        h2DConnection.setAutoCommit(false);
        try {
            h2DConnection.rollback(h2DConnection.setSavepoint());
        } finally {
            h2DConnection.setAutoCommit(true);
        }
    }

    @Test
    public void testSetClientInfo() throws Exception {
        // TODO
        // h2DConnection.setClientInfo("ApplicationName", "app1");
    }

    @Test
    public void testSetDefaultQueryTimeout() throws Exception {
        final int expected = 1;
        delegatingConnection.setDefaultQueryTimeout(expected);
        assertEquals(expected, delegatingConnection.getDefaultQueryTimeout());
    }

    @Test
    public void testSetHoldability() throws Exception {
        final int expected = 1;
        h2DConnection.setHoldability(expected);
        assertEquals(expected, h2DConnection.getHoldability());
    }

    @Test
    public void testSetNetworkTimeout() throws Exception {
        h2DConnection.setNetworkTimeout(r -> {}, 1);
        assertEquals(0, h2DConnection.getNetworkTimeout());
    }

    @Test
    public void testSetSavepoint() throws Exception {
        h2DConnection.setSavepoint();
    }

    @SuppressWarnings("javadoc")
    @Test
    public void testUnwrap() throws Exception {
        assertNotNull(delegatingConnection.unwrap(delegatingConnection.getClass()));
        assertNotNull(delegatingConnection.unwrap(delegatingConnection.getDelegate().getClass()));
        assertThrows(SQLException.class, () -> delegatingConnection.unwrap(Integer.class));
    }

}
