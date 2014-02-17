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

package org.apache.commons.dbcp2.datasources;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.PooledConnection;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.dbcp2.cpdsadapter.DriverAdapterCPDS;

import org.apache.commons.pool2.KeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;

/**
 * @version $Revision$ $Date$
 */
public class TestKeyedCPDSConnectionFactory extends TestCase {

    protected ConnectionPoolDataSourceProxy cpds = null;

    public TestKeyedCPDSConnectionFactory(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(TestKeyedCPDSConnectionFactory.class);
    }

    @Override
    public void setUp() throws Exception {
        cpds = new ConnectionPoolDataSourceProxy(new DriverAdapterCPDS());
        DriverAdapterCPDS delegate = (DriverAdapterCPDS) cpds.getDelegate();
        delegate.setDriver("org.apache.commons.dbcp2.TesterDriver");
        delegate.setUrl("jdbc:apache:commons:testdriver");
        delegate.setUser("username");
        delegate.setPassword("password");
    }

    /**
     * JIRA DBCP-216
     *
     * Check PoolableConnection close triggered by destroy is handled
     * properly. PooledConnectionProxy (dubiously) fires connectionClosed
     * when PooledConnection itself is closed.
     */
    public void testSharedPoolDSDestroyOnReturn() throws Exception {
       SharedPoolDataSource ds = new SharedPoolDataSource();
       ds.setConnectionPoolDataSource(cpds);
       ds.setMaxTotal(10);
       ds.setDefaultMaxWaitMillis(50);
       ds.setDefaultMaxIdle(2);
       Connection conn1 = ds.getConnection("username", "password");
       Connection conn2 = ds.getConnection("username", "password");
       Connection conn3 = ds.getConnection("username", "password");
       assertEquals(3, ds.getNumActive());
       conn1.close();
       assertEquals(1, ds.getNumIdle());
       conn2.close();
       assertEquals(2, ds.getNumIdle());
       conn3.close(); // Return to pool will trigger destroy -> close sequence
       assertEquals(2, ds.getNumIdle());
    }

    /**
     * JIRA DBCP-216
     *
     * Verify that pool counters are maintained properly and listeners are
     * cleaned up when a PooledConnection throws a connectionError event.
     */
    public void testConnectionErrorCleanup() throws Exception {
        // Setup factory
        UserPassKey key = new UserPassKey("username", "password");
        KeyedCPDSConnectionFactory factory =
            new KeyedCPDSConnectionFactory(cpds, null, -1, false);
        KeyedObjectPool<UserPassKey, PooledConnectionAndInfo> pool = new GenericKeyedObjectPool<>(factory);
        factory.setPool(pool);

        // Checkout a pair of connections
        PooledConnection pcon1 =
            pool.borrowObject(key)
                .getPooledConnection();
        Connection con1 = pcon1.getConnection();
        PooledConnection pcon2 =
            pool.borrowObject(key)
                .getPooledConnection();
        assertEquals(2, pool.getNumActive(key));
        assertEquals(0, pool.getNumIdle(key));

        // Verify listening
        PooledConnectionProxy pc = (PooledConnectionProxy) pcon1;
        assertTrue(pc.getListeners().contains(factory));

        // Throw connectionError event
        pc.throwConnectionError();

        // Active count should be reduced by 1 and no idle increase
        assertEquals(1, pool.getNumActive(key));
        assertEquals(0, pool.getNumIdle(key));

        // Throw another one - we should be on cleanup list, so ignored
        pc.throwConnectionError();
        assertEquals(1, pool.getNumActive(key));
        assertEquals(0, pool.getNumIdle(key));

        // Ask for another connection - should trigger makeObject, which causes
        // cleanup, removing listeners.
        PooledConnection pcon3 =
            pool.borrowObject(key)
                .getPooledConnection();
        assertTrue(!pcon3.equals(pcon1)); // better not get baddie back
        assertTrue(!pc.getListeners().contains(factory)); // verify cleanup
        assertEquals(2, pool.getNumActive(key));
        assertEquals(0, pool.getNumIdle(key));

        // Return good connections back to pool
        pcon2.getConnection().close();
        pcon3.getConnection().close();
        assertEquals(2, pool.getNumIdle(key));
        assertEquals(0, pool.getNumActive(key));

        // Verify pc is closed
        try {
           pc.getConnection();
           fail("Expecting SQLException using closed PooledConnection");
        } catch (SQLException ex) {
            // expected
        }

        // Back from the dead - ignore the ghost!
        con1.close();
        assertEquals(2, pool.getNumIdle(key));
        assertEquals(0, pool.getNumActive(key));

        // Clear pool
        factory.getPool().clear();
        assertEquals(0, pool.getNumIdle(key));
    }

}
