/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.commons.dbcp2.managed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for LocalXAConnectionFactory$LocalXAResource
 */
public class TestLocalXaResource {

    private static final class TestConnection implements Connection {

        public boolean throwWhenGetAutoCommit;
        public boolean throwWhenSetAutoCommit;
        boolean autoCommit;
        boolean readOnly;
        public boolean committed;
        public boolean rolledback;
        public boolean closed;

        @Override
        public void abort(final Executor executor) throws SQLException {
        }

        @Override
        public void clearWarnings() throws SQLException {
        }

        @Override
        public void close() throws SQLException {
            closed = true;
        }

        @Override
        public void commit() throws SQLException {
            committed = true;
        }

        @Override
        public Array createArrayOf(final String typeName, final Object[] elements) throws SQLException {
            return null;
        }

        @Override
        public Blob createBlob() throws SQLException {
            return null;
        }

        @Override
        public Clob createClob() throws SQLException {
            return null;
        }

        @Override
        public NClob createNClob() throws SQLException {
            return null;
        }

        @Override
        public SQLXML createSQLXML() throws SQLException {
            return null;
        }

        @Override
        public Statement createStatement() throws SQLException {
            return null;
        }

        @Override
        public Statement createStatement(final int resultSetType, final int resultSetConcurrency) throws SQLException {
            return null;
        }

        @Override
        public Statement createStatement(final int resultSetType, final int resultSetConcurrency, final int resultSetHoldability)
                throws SQLException {
            return null;
        }

        @Override
        public Struct createStruct(final String typeName, final Object[] attributes) throws SQLException {
            return null;
        }

        @Override
        public boolean getAutoCommit() throws SQLException {
            if (throwWhenGetAutoCommit) {
                throw new SQLException();
            }
            return autoCommit;
        }

        @Override
        public String getCatalog() throws SQLException {
            return null;
        }

        @Override
        public Properties getClientInfo() throws SQLException {
            return null;
        }

        @Override
        public String getClientInfo(final String name) throws SQLException {
            return null;
        }

        @Override
        public int getHoldability() throws SQLException {
            return 0;
        }

        @Override
        public DatabaseMetaData getMetaData() throws SQLException {
            return null;
        }

        @Override
        public int getNetworkTimeout() throws SQLException {
            return 0;
        }

        @Override
        public String getSchema() throws SQLException {
            return null;
        }

        @Override
        public int getTransactionIsolation() throws SQLException {
            return 0;
        }

        @Override
        public Map<String, Class<?>> getTypeMap() throws SQLException {
            return null;
        }

        @Override
        public SQLWarning getWarnings() throws SQLException {
            return null;
        }

        @Override
        public boolean isClosed() throws SQLException {
            return closed;
        }

        @Override
        public boolean isReadOnly() throws SQLException {
            return readOnly;
        }

        @Override
        public boolean isValid(final int timeout) throws SQLException {
            return false;
        }

        @Override
        public boolean isWrapperFor(final Class<?> iface) throws SQLException {
            return false;
        }

        @Override
        public String nativeSQL(final String sql) throws SQLException {
            return null;
        }

        @Override
        public CallableStatement prepareCall(final String sql) throws SQLException {
            return null;
        }

        @Override
        public CallableStatement prepareCall(final String sql, final int resultSetType, final int resultSetConcurrency)
                throws SQLException {
            return null;
        }

        @Override
        public CallableStatement prepareCall(final String sql, final int resultSetType, final int resultSetConcurrency,
                final int resultSetHoldability) throws SQLException {
            return null;
        }

        @Override
        public PreparedStatement prepareStatement(final String sql) throws SQLException {
            return null;
        }

        @Override
        public PreparedStatement prepareStatement(final String sql, final int autoGeneratedKeys) throws SQLException {
            return null;
        }

        @Override
        public PreparedStatement prepareStatement(final String sql, final int resultSetType, final int resultSetConcurrency)
                throws SQLException {
            return null;
        }

        @Override
        public PreparedStatement prepareStatement(final String sql, final int resultSetType, final int resultSetConcurrency,
                final int resultSetHoldability) throws SQLException {
            return null;
        }

        @Override
        public PreparedStatement prepareStatement(final String sql, final int[] columnIndexes) throws SQLException {
            return null;
        }

        @Override
        public PreparedStatement prepareStatement(final String sql, final String[] columnNames) throws SQLException {
            return null;
        }

        @Override
        public void releaseSavepoint(final Savepoint savepoint) throws SQLException {
        }

        @Override
        public void rollback() throws SQLException {
            rolledback = true;
        }

        @Override
        public void rollback(final Savepoint savepoint) throws SQLException {
        }

        @Override
        public void setAutoCommit(final boolean autoCommit) throws SQLException {
            if (throwWhenSetAutoCommit) {
                throw new SQLException();
            }
            this.autoCommit = autoCommit;
        }

        @Override
        public void setCatalog(final String catalog) throws SQLException {
        }

        @Override
        public void setClientInfo(final Properties properties) throws SQLClientInfoException {
        }

        @Override
        public void setClientInfo(final String name, final String value) throws SQLClientInfoException {
        }

        @Override
        public void setHoldability(final int holdability) throws SQLException {
        }

        @Override
        public void setNetworkTimeout(final Executor executor, final int milliseconds) throws SQLException {
        }

        @Override
        public void setReadOnly(final boolean readOnly) throws SQLException {
            this.readOnly = readOnly;
        }

        @Override
        public Savepoint setSavepoint() throws SQLException {
            return null;
        }

        @Override
        public Savepoint setSavepoint(final String name) throws SQLException {
            return null;
        }

        @Override
        public void setSchema(final String schema) throws SQLException {
        }

        @Override
        public void setTransactionIsolation(final int level) throws SQLException {
        }

        @Override
        public void setTypeMap(final Map<String, Class<?>> map) throws SQLException {
        }

        @Override
        public <T> T unwrap(final Class<T> iface) throws SQLException {
            return null;
        }
    }
    private static final class TestXid implements Xid {

        @Override
        public byte[] getBranchQualifier() {
            return null;
        }

        @Override
        public int getFormatId() {
            return 0;
        }

        @Override
        public byte[] getGlobalTransactionId() {
            return null;
        }
    }

    private Connection conn;

    private LocalXAConnectionFactory.LocalXAResource resource;

    @BeforeEach
    public void setUp() {
        conn = new TestConnection();
        resource = new LocalXAConnectionFactory.LocalXAResource(conn);
    }

    @Test
    public void testCommit() throws SQLException, XAException {
        final Xid xid = new TestXid();
        ((TestConnection) conn).closed = false;
        conn.setReadOnly(false);
        resource.start(xid, XAResource.TMNOFLAGS);
        resource.commit(xid, false);
        assertTrue(((TestConnection) conn).committed);
    }

    @Test
    public void testCommitConnectionClosed() throws SQLException, XAException {
        final Xid xid = new TestXid();
        ((TestConnection) conn).closed = true;
        conn.setReadOnly(false);
        resource.start(xid, XAResource.TMNOFLAGS);
        assertThrows(XAException.class, () -> resource.commit(xid, false));
    }

    @Test
    public void testCommitConnectionNotReadOnly() throws SQLException, XAException {
        final Xid xid = new TestXid();
        ((TestConnection) conn).closed = false;
        conn.setReadOnly(true);
        resource.start(xid, XAResource.TMNOFLAGS);
        resource.commit(xid, false);
        assertFalse(((TestConnection) conn).committed);
    }

    @Test
    public void testCommitInvalidXid() throws SQLException, XAException {
        final Xid xid = new TestXid();
        ((TestConnection) conn).closed = false;
        conn.setReadOnly(false);
        resource.start(xid, XAResource.TMNOFLAGS);
        assertThrows(XAException.class, () -> resource.commit(new TestXid(), false));
    }

    @Test
    public void testCommitMissingXid() {
        assertThrows(NullPointerException.class, () -> resource.commit(null, false));
    }

    @Test
    public void testCommitNoTransaction() throws SQLException {
        ((TestConnection) conn).closed = false;
        conn.setReadOnly(false);
        assertThrows(XAException.class, () -> resource.commit(new TestXid(), false));
    }

    @Test
    public void testConstructor() {
        assertEquals(0, resource.getTransactionTimeout());
        assertNull(resource.getXid());
        // the current implementation always return false, regardless of the input value
        assertFalse(resource.setTransactionTimeout(100));
        // the current implementation always return an empty/zero'd array, regardless of the input value
        assertEquals(0, resource.recover(100).length);
    }

    @Test
    public void testForget() throws XAException {
        final Xid xid = new TestXid();
        resource.start(xid, XAResource.TMNOFLAGS);
        resource.forget(xid);
        assertNull(resource.getXid());
    }

    @Test
    public void testForgetDifferentXid() throws XAException {
        final Xid xid = new TestXid();
        resource.start(xid, XAResource.TMNOFLAGS);
        resource.forget(new TestXid());
        assertEquals(xid, resource.getXid());
    }

    @Test
    public void testForgetMissingXid() throws XAException {
        final Xid xid = new TestXid();
        resource.start(xid, XAResource.TMNOFLAGS);
        resource.forget(null);
        assertEquals(xid, resource.getXid());
    }

    @Test
    public void testIsSame() {
        assertTrue(resource.isSameRM(resource));
        assertFalse(resource.isSameRM(new LocalXAConnectionFactory.LocalXAResource(conn)));
    }

    @Test
    public void testRollback() throws SQLException, XAException {
        final Xid xid = new TestXid();
        ((TestConnection) conn).closed = false;
        conn.setReadOnly(false);
        resource.start(xid, XAResource.TMNOFLAGS);
        resource.rollback(xid);
        assertTrue(((TestConnection) conn).rolledback);
    }

    @Test
    public void testRollbackInvalidXid() throws SQLException, XAException {
        final Xid xid = new TestXid();
        ((TestConnection) conn).closed = false;
        conn.setReadOnly(false);
        resource.start(xid, XAResource.TMNOFLAGS);
        assertThrows(XAException.class, () -> resource.rollback(new TestXid()));
    }

    @Test
    public void testRollbackMissingXid() {
        assertThrows(NullPointerException.class, () -> resource.rollback(null));
    }

    /**
     * When an exception is thrown on the {@link Connection#getAutoCommit()}, then the
     * value is set to {@code true} by default.
     * @throws XAException when there are errors with the transaction
     * @throws SQLException when there are errors with other SQL/DB parts
     */
    @Test
    public void testStartExceptionOnGetAutoCommit() throws XAException, SQLException {
        final Xid xid = new TestXid();
        ((TestConnection) conn).throwWhenGetAutoCommit = true;
        conn.setAutoCommit(false);
        conn.setReadOnly(true);
        // the start method with no flag will call getAutoCommit, the exception will be thrown, and it will be set
        // to true
        resource.start(xid, XAResource.TMNOFLAGS);
        // and prepare sets the value computed in start in the connection
        resource.prepare(xid);
        ((TestConnection) conn).throwWhenGetAutoCommit = false;
        assertTrue(conn.getAutoCommit());
    }

    @Test
    public void testStartFailsWhenCannotSetAutoCommit() {
        final Xid xid = new TestXid();
        ((TestConnection) conn).throwWhenSetAutoCommit = true;
        assertThrows(XAException.class, () -> resource.start(xid, XAResource.TMNOFLAGS));
    }

    @Test
    public void testStartInvalidFlag() {
        // currently, valid values are TMNOFLAGS and TMRESUME
        assertThrows(XAException.class, () -> resource.start(null, XAResource.TMENDRSCAN));
    }

    @Test
    public void testStartNoFlagButAlreadyEnlisted() throws XAException {
        resource.start(new TestXid(), XAResource.TMNOFLAGS);
        assertThrows(XAException.class, () -> resource.start(new TestXid(), XAResource.TMNOFLAGS));
    }

    @Test
    public void testStartNoFlagResume() throws XAException {
        final Xid xid = new TestXid();
        resource.start(xid, XAResource.TMNOFLAGS);
        resource.start(xid, XAResource.TMRESUME);
        assertEquals(xid, resource.getXid());
    }

    @Test
    public void testStartNoFlagResumeButDifferentXid() throws XAException {
        resource.start(new TestXid(), XAResource.TMNOFLAGS);
        assertThrows(XAException.class, () -> resource.start(new TestXid(), XAResource.TMRESUME));
    }

    @Test
    public void testStartNoFlagResumeEnd() throws XAException {
        final Xid xid = new TestXid();
        resource.start(xid, XAResource.TMNOFLAGS);
        resource.start(xid, XAResource.TMRESUME);
        // flag is never used in the end
        resource.end(xid, 0);
        assertEquals(xid, resource.getXid());
    }

    @Test
    public void testStartNoFlagResumeEndDifferentXid() throws XAException {
        final Xid xid = new TestXid();
        resource.start(xid, XAResource.TMNOFLAGS);
        resource.start(xid, XAResource.TMRESUME);
        // flag is never used in the end
        assertThrows(XAException.class, () -> resource.end(new TestXid(), 0));
    }

    @Test
    public void testStartNoFlagResumeEndMissingXid() throws XAException {
        final Xid xid = new TestXid();
        resource.start(xid, XAResource.TMNOFLAGS);
        resource.start(xid, XAResource.TMRESUME);
        // flag is never used in the end
        assertThrows(NullPointerException.class, () -> resource.end(null, 0));
    }

    /**
     * When an exception is thrown on the {@link Connection#getAutoCommit()}, then the
     * value is set to {@code true} by default. However, if the connection is not read-only,
     * then the value set by the user in the original connection will be kept.
     * @throws XAException when there are errors with the transaction
     * @throws SQLException when there are errors with other SQL/DB parts
     */
    @Test
    public void testStartReadOnlyConnectionExceptionOnGetAutoCommit() throws XAException, SQLException {
        final Xid xid = new TestXid();
        ((TestConnection) conn).throwWhenGetAutoCommit = true;
        conn.setAutoCommit(false);
        conn.setReadOnly(false);
        // the start method with no flag will call getAutoCommit, the exception will be thrown, and it will be set
        // to true
        resource.start(xid, XAResource.TMNOFLAGS);
        // and prepare sets the value computed in start in the connection
        resource.prepare(xid);
        ((TestConnection) conn).throwWhenGetAutoCommit = false;
        assertFalse(conn.getAutoCommit());
    }

    @Test
    public void testStartReadOnlyConnectionPrepare() throws XAException, SQLException {
        final Xid xid = new TestXid();
        conn.setAutoCommit(false);
        conn.setReadOnly(true);
        resource.start(xid, XAResource.TMNOFLAGS);
        resource.prepare(xid);
        assertFalse(conn.getAutoCommit());
    }
}
