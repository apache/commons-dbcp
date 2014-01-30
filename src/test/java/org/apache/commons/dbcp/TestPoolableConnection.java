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

package org.apache.commons.dbcp;

import java.sql.Connection;
import java.sql.SQLException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;

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

    private ObjectPool pool = null;

    public void setUp() throws Exception {
        pool = new GenericObjectPool();
        PoolableConnectionFactory factory = 
            new PoolableConnectionFactory(
                new DriverConnectionFactory(new TesterDriver(),"jdbc:apache:commons:testdriver", null),
                pool, null, null, true, true);
        pool.setFactory(factory);
    }

    public void testConnectionPool() throws Exception {
        // Grab a new connection from the pool
        Connection c = (Connection)pool.borrowObject();

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
        Connection conn = (Connection)pool.borrowObject();

        // Now close our innermost delegate, simulating the case where the
        // underlying connection closes itself
        ((PoolableConnection)conn).getInnermostDelegate().close();
        
        // At this point, we can close the pooled connection. The
        // PoolableConnection *should* realise that its underlying
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
}
