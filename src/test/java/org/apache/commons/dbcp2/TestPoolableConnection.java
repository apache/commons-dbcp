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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.management.OperationsException;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 */
public class TestPoolableConnection {

    private GenericObjectPool<PoolableConnection> pool;

    @BeforeEach
    public void setUp() throws Exception {
        final PoolableConnectionFactory factory = new PoolableConnectionFactory(
                new DriverConnectionFactory(new TesterDriver(), "jdbc:apache:commons:testdriver", null), null);
        factory.setDefaultAutoCommit(Boolean.TRUE);
        factory.setDefaultReadOnly(Boolean.TRUE);

        pool = new GenericObjectPool<>(factory);
        factory.setPool(pool);
    }

    @AfterEach
    public void tearDown() {
        pool.close();
    }

    @Test
    public void testClosingWrappedInDelegate() throws Exception {
        Assertions.assertEquals(0, pool.getNumActive());

        final Connection conn = pool.borrowObject();
        final DelegatingConnection<Connection> outer = new DelegatingConnection<>(conn);

        Assertions.assertFalse(outer.isClosed());
        Assertions.assertFalse(conn.isClosed());
        Assertions.assertEquals(1, pool.getNumActive());

        outer.close();

        Assertions.assertTrue(outer.isClosed());
        Assertions.assertTrue(conn.isClosed());
        Assertions.assertEquals(0, pool.getNumActive());
        Assertions.assertEquals(1, pool.getNumIdle());
    }

    @Test
    public void testConnectionPool() throws Exception {
        // Grab a new connection from the pool
        final Connection c = pool.borrowObject();

        assertNotNull(c, "Connection should be created and should not be null");
        assertEquals(1, pool.getNumActive(), "There should be exactly one active object in the pool");

        // Now return the connection by closing it
        c.close(); // Can't be null

        assertEquals(0, pool.getNumActive(), "There should now be zero active objects in the pool");
    }

    @Test
    public void testDisconnectionIgnoreSqlCodes() throws Exception {
        pool.setTestOnReturn(true);
        final PoolableConnectionFactory factory = (PoolableConnectionFactory) pool.getFactory();
        factory.setFastFailValidation(true);
        factory.setDisconnectionIgnoreSqlCodes(Arrays.asList("08S02", "08007"));

        final PoolableConnection conn = pool.borrowObject();
        final TesterConnection nativeConnection = (TesterConnection) conn.getInnermostDelegate();

        // set up non-fatal exception
        nativeConnection.setFailure(new SQLException("Non-fatal connection error.", "08S02"));
        assertThrows(SQLException.class, conn::createStatement);
        nativeConnection.setFailure(null);

        // verify that non-fatal connection is returned to the pool
        conn.close();
        assertEquals(0, pool.getNumActive(), "The pool should have no active connections");
        assertEquals(1, pool.getNumIdle(), "The pool should have one idle connection");
    }

    @Test
    public void testFastFailValidation() throws Exception {
        pool.setTestOnReturn(true);
        final PoolableConnectionFactory factory = (PoolableConnectionFactory) pool.getFactory();
        factory.setFastFailValidation(true);
        final PoolableConnection conn = pool.borrowObject();
        final TesterConnection nativeConnection = (TesterConnection) conn.getInnermostDelegate();

        // Set up non-fatal exception
        nativeConnection.setFailure(new SQLException("Not fatal error.", "Invalid syntax."));
        try {
            conn.createStatement();
            fail("Should throw SQL exception.");
        } catch (final SQLException ignored) {
            // cleanup failure
            nativeConnection.setFailure(null);
        }

        // validate should not fail - error was not fatal and condition was cleaned up
        conn.validate("SELECT 1", 1000);

        // now set up fatal failure
        nativeConnection.setFailure(new SQLException("Fatal connection error.", "01002"));

        try {
            conn.createStatement();
            fail("Should throw SQL exception.");
        } catch (final SQLException ignored) {
            // cleanup failure
            nativeConnection.setFailure(null);
        }

        // validate should now fail because of previous fatal error, despite cleanup
        try {
            conn.validate("SELECT 1", 1000);
            fail("Should throw SQL exception on validation.");
        } catch (final SQLException notValid){
            // expected - fatal error && fastFailValidation
        }

        // verify that bad connection does not get returned to the pool
        conn.close();  // testOnReturn triggers validate, which should fail
        assertEquals(0, pool.getNumActive(), "The pool should have no active connections");
        assertEquals(0, pool.getNumIdle(), "The pool should have no idle connections");
    }

    @Test
    public void testFastFailValidationCustomCodes() throws Exception {
        pool.setTestOnReturn(true);
        final PoolableConnectionFactory factory = (PoolableConnectionFactory) pool.getFactory();
        factory.setFastFailValidation(true);
        final ArrayList<String> disconnectionSqlCodes = new ArrayList<>();
        disconnectionSqlCodes.add("XXX");
        factory.setDisconnectionSqlCodes(disconnectionSqlCodes);
        final PoolableConnection conn = pool.borrowObject();
        final TesterConnection nativeConnection = (TesterConnection) conn.getInnermostDelegate();

        // Set up fatal exception
        nativeConnection.setFailure(new SQLException("Fatal connection error.", "XXX"));
        assertThrows(SQLException.class, conn::createStatement);
        // cleanup failure
        nativeConnection.setFailure(null);

        // verify that bad connection does not get returned to the pool
        conn.close();  // testOnReturn triggers validate, which should fail
        assertEquals(0, pool.getNumActive(), "The pool should have no active connections");
        assertEquals(0, pool.getNumIdle(), "The pool should have no idle connections");
    }

    @Test
    public void testIsDisconnectionSqlExceptionStackOverflow() throws Exception {
        final int maxDeep = 100_000;
        final SQLException rootException = new SQLException("Data truncated", "22001");
        SQLException parentException = rootException;
        for (int i = 0; i <= maxDeep; i++) {
            final SQLException childException = new SQLException("Data truncated: " + i, "22001");
            parentException.setNextException(childException);
            parentException = childException;
        }
        final Connection conn = pool.borrowObject();
        assertFalse(((PoolableConnection) conn).isDisconnectionSqlException(rootException));
        assertFalse(((PoolableConnection) conn).isFatalException(rootException));
    }

    /**
     * Tests if the {@link PoolableConnectionMXBean} interface is a valid MXBean
     * interface.
     */
    @Test
    public void testMXBeanCompliance() throws OperationsException {
       TestBasicDataSourceMXBean.testMXBeanCompliance(PoolableConnectionMXBean.class);
    }

    // Bugzilla Bug 33591: PoolableConnection leaks connections if the
    // delegated connection closes itself.
    @Test
    public void testPoolableConnectionLeak() throws Exception {
        // 'Borrow' a connection from the pool
        final Connection conn = pool.borrowObject();

        // Now close our innermost delegate, simulating the case where the
        // underlying connection closes itself
        ((PoolableConnection)conn).getInnermostDelegate().close();

        // At this point, we can close the pooled connection. The
        // PoolableConnection *should* realize that its underlying
        // connection is gone and invalidate itself. The pool should have no
        // active connections.

        try {
            conn.close();
        } catch (final SQLException e) {
            // Here we expect 'connection already closed', but the connection
            // should *NOT* be returned to the pool
        }

        assertEquals(0, pool.getNumActive(), "The pool should have no active connections");
    }
}
