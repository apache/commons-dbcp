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

import java.sql.Connection;
import java.sql.SQLException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.junit.Assert;

/**
 * @author James Ring
 * @version $Revision$ $Date$
 */
public class TestPoolableConnection extends TestCase {
    public TestPoolableConnection(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(TestPoolableConnection.class);
    }

    private ObjectPool<PoolableConnection> pool = null;

    @Override
    public void setUp() throws Exception {
        PoolableConnectionFactory factory = new PoolableConnectionFactory(
                new DriverConnectionFactory(
                        new TesterDriver(),"jdbc:apache:commons:testdriver", null),
                null);
        factory.setDefaultAutoCommit(Boolean.TRUE);
        factory.setDefaultReadOnly(Boolean.TRUE);

        pool = new GenericObjectPool<>(factory);
        factory.setPool(pool);
    }

    public void testConnectionPool() throws Exception {
        // Grab a new connection from the pool
        Connection c = pool.borrowObject();

        assertNotNull("Connection should be created and should not be null", c);
        assertEquals("There should be exactly one active object in the pool", 1, pool.getNumActive());

        // Now return the connection by closing it
        c.close(); // Can't be null

        assertEquals("There should now be zero active objects in the pool", 0, pool.getNumActive());
    }

    // Bugzilla Bug 33591: PoolableConnection leaks connections if the
    // delegated connection closes itself.
    public void testPoolableConnectionLeak() throws Exception {
        // 'Borrow' a connection from the pool
        Connection conn = pool.borrowObject();

        // Now close our innermost delegate, simulating the case where the
        // underlying connection closes itself
        ((PoolableConnection)conn).getInnermostDelegate().close();

        // At this point, we can close the pooled connection. The
        // PoolableConnection *should* realize that its underlying
        // connection is gone and invalidate itself. The pool should have no
        // active connections.

        try {
            conn.close();
        } catch (SQLException e) {
            // Here we expect 'connection already closed', but the connection
            // should *NOT* be returned to the pool
        }

        assertEquals("The pool should have no active connections",
            0, pool.getNumActive());
    }

    public void testClosingWrappedInDelegate() throws Exception {
        Assert.assertEquals(0, pool.getNumActive());

        Connection conn = pool.borrowObject();
        DelegatingConnection<Connection> outer = new DelegatingConnection<>(conn);

        Assert.assertFalse(outer.isClosed());
        Assert.assertFalse(conn.isClosed());
        Assert.assertEquals(1, pool.getNumActive());

        outer.close();

        Assert.assertTrue(outer.isClosed());
        Assert.assertTrue(conn.isClosed());
        Assert.assertEquals(0, pool.getNumActive());
        Assert.assertEquals(1, pool.getNumIdle());
    }
}
