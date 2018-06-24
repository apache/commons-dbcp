/*
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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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

import org.junit.Before;
import org.junit.Test;

/**
 * Tests for LocalXAConnectionFactory$LocalXAResource
 */
public class TestLocalXaResource {

    private Connection conn;
    private LocalXAConnectionFactory.LocalXAResource resource;

    @Before
    public void setUp() {
        conn = new TestConnection();
        resource = new LocalXAConnectionFactory.LocalXAResource(conn);
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
    public void testIsSame() {
        assertTrue(resource.isSameRM(resource));
        assertFalse(resource.isSameRM(new LocalXAConnectionFactory.LocalXAResource(conn)));
    }

    @Test(expected=XAException.class)
    public void testStartInvalidFlag() throws XAException {
        // currently, valid values are TMNOFLAGS and TMRESUME
        resource.start(null, XAResource.TMENDRSCAN);
    }

    @Test(expected=XAException.class)
    public void testStartNoFlagButAlreadyEnlisted() throws XAException {
        resource.start(new TestXid(), XAResource.TMNOFLAGS);
        resource.start(new TestXid(), XAResource.TMNOFLAGS);
    }

    @Test(expected=XAException.class)
    public void testStartNoFlagResumeButDifferentXid() throws XAException {
        resource.start(new TestXid(), XAResource.TMNOFLAGS);
        resource.start(new TestXid(), XAResource.TMRESUME);
    }

    @Test
    public void testStartNoFlagResume() throws XAException {
        Xid xid = new TestXid();
        resource.start(xid, XAResource.TMNOFLAGS);
        resource.start(xid, XAResource.TMRESUME);
        assertEquals(xid, resource.getXid());
    }

    @Test
    public void testStartNoFlagResumeEnd() throws XAException {
        Xid xid = new TestXid();
        resource.start(xid, XAResource.TMNOFLAGS);
        resource.start(xid, XAResource.TMRESUME);
        // flag is never used in the end
        resource.end(xid, 0);
        assertEquals(xid, resource.getXid());
    }

    @Test(expected=NullPointerException.class)
    public void testStartNoFlagResumeEndMissingXid() throws XAException {
        Xid xid = new TestXid();
        resource.start(xid, XAResource.TMNOFLAGS);
        resource.start(xid, XAResource.TMRESUME);
        // flag is never used in the end
        resource.end(null, 0);
    }

    @Test(expected=XAException.class)
    public void testStartNoFlagResumeEndDifferentXid() throws XAException {
        Xid xid = new TestXid();
        resource.start(xid, XAResource.TMNOFLAGS);
        resource.start(xid, XAResource.TMRESUME);
        // flag is never used in the end
        resource.end(new TestXid(), 0);
    }

    @Test
    public void testForgetDifferentXid() throws XAException {
        Xid xid = new TestXid();
        resource.start(xid, XAResource.TMNOFLAGS);
        resource.forget(new TestXid());
        assertEquals(xid, resource.getXid());
    }

    @Test
    public void testForgetMissingXid() throws XAException {
        Xid xid = new TestXid();
        resource.start(xid, XAResource.TMNOFLAGS);
        resource.forget(null);
        assertEquals(xid, resource.getXid());
    }

    @Test
    public void testForget() throws XAException {
        Xid xid = new TestXid();
        resource.start(xid, XAResource.TMNOFLAGS);
        resource.forget(xid);
        assertNull(resource.getXid());
    }

    @Test
    public void testStartReadOnlyConnectionPrepare() throws XAException, SQLException {
        Xid xid = new TestXid();
        conn.setAutoCommit(false);
        conn.setReadOnly(true);
        resource.start(xid, XAResource.TMNOFLAGS);
        resource.prepare(xid);
        assertFalse(conn.getAutoCommit());
    }

    /**
     * When an exception is thrown on the {@link Connection#getAutoCommit()}, then the
     * value is set to {@code true} by default.
     * @throws XAException when there are errors with the transaction
     * @throws SQLException when there are errors with other SQL/DB parts
     */
    @Test
    public void testStartExceptionOnGetAutoCommit() throws XAException, SQLException {
        Xid xid = new TestXid();
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

    /**
     * When an exception is thrown on the {@link Connection#getAutoCommit()}, then the
     * value is set to {@code true} by default. However, if the connection is not read-only,
     * then the value set by the user in the original connection will be kept.
     * @throws XAException when there are errors with the transaction
     * @throws SQLException when there are errors with other SQL/DB parts
     */
    @Test
    public void testStartReadOnlyConnectionExceptionOnGetAutoCommit() throws XAException, SQLException {
        Xid xid = new TestXid();
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

    @Test(expected=XAException.class)
    public void testStartFailsWhenCannotSetAutoCommit() throws XAException, SQLException {
        Xid xid = new TestXid();
        ((TestConnection) conn).throwWhenSetAutoCommit = true;
        resource.start(xid, XAResource.TMNOFLAGS);
    }

    @Test(expected=NullPointerException.class)
    public void testCommitMissingXid() throws SQLException, XAException {
        resource.commit(null, false);
    }

    @Test(expected=XAException.class)
    public void testCommitNoTransaction() throws SQLException, XAException {
        ((TestConnection) conn).closed = false;
        conn.setReadOnly(false);
        resource.commit(new TestXid(), false);
    }

    @Test(expected=XAException.class)
    public void testCommitInvalidXid() throws SQLException, XAException {
        Xid xid = new TestXid();
        ((TestConnection) conn).closed = false;
        conn.setReadOnly(false);
        resource.start(xid, XAResource.TMNOFLAGS);
        resource.commit(new TestXid(), false);
    }

    @Test(expected=XAException.class)
    public void testCommitConnectionClosed() throws SQLException, XAException {
        Xid xid = new TestXid();
        ((TestConnection) conn).closed = true;
        conn.setReadOnly(false);
        resource.start(xid, XAResource.TMNOFLAGS);
        resource.commit(xid, false);
    }

    @Test
    public void testCommitConnectionNotReadOnly() throws SQLException, XAException {
        Xid xid = new TestXid();
        ((TestConnection) conn).closed = false;
        conn.setReadOnly(true);
        resource.start(xid, XAResource.TMNOFLAGS);
        resource.commit(xid, false);
        assertFalse(((TestConnection) conn).committed);
    }

    @Test
    public void testCommit() throws SQLException, XAException {
        Xid xid = new TestXid();
        ((TestConnection) conn).closed = false;
        conn.setReadOnly(false);
        resource.start(xid, XAResource.TMNOFLAGS);
        resource.commit(xid, false);
        assertTrue(((TestConnection) conn).committed);
    }

    @Test(expected=NullPointerException.class)
    public void testRollbackMissingXid() throws XAException {
        resource.rollback(null);
    }

    @Test(expected=XAException.class)
    public void testRollbackInvalidXid() throws SQLException, XAException {
        Xid xid = new TestXid();
        ((TestConnection) conn).closed = false;
        conn.setReadOnly(false);
        resource.start(xid, XAResource.TMNOFLAGS);
        resource.rollback(new TestXid());
    }

    @Test
    public void testRollback() throws SQLException, XAException {
        Xid xid = new TestXid();
        ((TestConnection) conn).closed = false;
        conn.setReadOnly(false);
        resource.start(xid, XAResource.TMNOFLAGS);
        resource.rollback(xid);
        assertTrue(((TestConnection) conn).rolledback);
    }

    private static class TestConnection implements Connection {

        public boolean throwWhenGetAutoCommit = false;
        public boolean throwWhenSetAutoCommit = false;
        boolean autoCommit = false;
        boolean readOnly = false;
        public boolean committed = false;
        public boolean rolledback = false;
        public boolean closed = false;

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            return null;
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) throws SQLException {
            return false;
        }

        @Override
        public Statement createStatement() throws SQLException {
            return null;
        }

        @Override
        public PreparedStatement prepareStatement(String sql) throws SQLException {
            return null;
        }

        @Override
        public CallableStatement prepareCall(String sql) throws SQLException {
            return null;
        }

        @Override
        public String nativeSQL(String sql) throws SQLException {
            return null;
        }

        @Override
        public void setAutoCommit(boolean autoCommit) throws SQLException {
            if (throwWhenSetAutoCommit) {
                throw new SQLException();
            }
            this.autoCommit = autoCommit;
        }

        @Override
        public boolean getAutoCommit() throws SQLException {
            if (throwWhenGetAutoCommit) {
                throw new SQLException();
            }
            return autoCommit;
        }

        @Override
        public void commit() throws SQLException {
            committed = true;
        }

        @Override
        public void rollback() throws SQLException {
            rolledback = true;
        }

        @Override
        public void close() throws SQLException {
            closed = true;
        }

        @Override
        public boolean isClosed() throws SQLException {
            return closed;
        }

        @Override
        public DatabaseMetaData getMetaData() throws SQLException {
            return null;
        }

        @Override
        public void setReadOnly(boolean readOnly) throws SQLException {
            this.readOnly = readOnly;
        }

        @Override
        public boolean isReadOnly() throws SQLException {
            return readOnly;
        }

        @Override
        public void setCatalog(String catalog) throws SQLException {
        }

        @Override
        public String getCatalog() throws SQLException {
            return null;
        }

        @Override
        public void setTransactionIsolation(int level) throws SQLException {
        }

        @Override
        public int getTransactionIsolation() throws SQLException {
            return 0;
        }

        @Override
        public SQLWarning getWarnings() throws SQLException {
            return null;
        }

        @Override
        public void clearWarnings() throws SQLException {
        }

        @Override
        public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
            return null;
        }

        @Override
        public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency)
                throws SQLException {
            return null;
        }

        @Override
        public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency)
                throws SQLException {
            return null;
        }

        @Override
        public Map<String, Class<?>> getTypeMap() throws SQLException {
            return null;
        }

        @Override
        public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        }

        @Override
        public void setHoldability(int holdability) throws SQLException {
        }

        @Override
        public int getHoldability() throws SQLException {
            return 0;
        }

        @Override
        public Savepoint setSavepoint() throws SQLException {
            return null;
        }

        @Override
        public Savepoint setSavepoint(String name) throws SQLException {
            return null;
        }

        @Override
        public void rollback(Savepoint savepoint) throws SQLException {
        }

        @Override
        public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        }

        @Override
        public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability)
                throws SQLException {
            return null;
        }

        @Override
        public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency,
                int resultSetHoldability) throws SQLException {
            return null;
        }

        @Override
        public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency,
                int resultSetHoldability) throws SQLException {
            return null;
        }

        @Override
        public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
            return null;
        }

        @Override
        public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
            return null;
        }

        @Override
        public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
            return null;
        }

        @Override
        public Clob createClob() throws SQLException {
            return null;
        }

        @Override
        public Blob createBlob() throws SQLException {
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
        public boolean isValid(int timeout) throws SQLException {
            return false;
        }

        @Override
        public void setClientInfo(String name, String value) throws SQLClientInfoException {
        }

        @Override
        public void setClientInfo(Properties properties) throws SQLClientInfoException {
        }

        @Override
        public String getClientInfo(String name) throws SQLException {
            return null;
        }

        @Override
        public Properties getClientInfo() throws SQLException {
            return null;
        }

        @Override
        public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
            return null;
        }

        @Override
        public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
            return null;
        }

        @Override
        public void setSchema(String schema) throws SQLException {
        }

        @Override
        public String getSchema() throws SQLException {
            return null;
        }

        @Override
        public void abort(Executor executor) throws SQLException {
        }

        @Override
        public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        }

        @Override
        public int getNetworkTimeout() throws SQLException {
            return 0;
        }
    }

    private static class TestXid implements Xid {

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
}
