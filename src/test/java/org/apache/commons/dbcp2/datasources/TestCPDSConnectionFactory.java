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

package org.apache.commons.dbcp2.datasources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;

import javax.sql.PooledConnection;

import org.apache.commons.dbcp2.cpdsadapter.DriverAdapterCPDS;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 */
public class TestCPDSConnectionFactory {

    protected ConnectionPoolDataSourceProxy cpds;

    @BeforeEach
    public void setUp() throws Exception {
        cpds = new ConnectionPoolDataSourceProxy(new DriverAdapterCPDS());
        final DriverAdapterCPDS delegate = (DriverAdapterCPDS) cpds.getDelegate();
        delegate.setDriver("org.apache.commons.dbcp2.TesterDriver");
        delegate.setUrl("jdbc:apache:commons:testdriver");
        delegate.setUser("userName");
        delegate.setPassword("password");
    }

    private void checkPoolLimits(final GenericObjectPool<PooledConnectionAndInfo> pool) {
        assertTrue(pool.getNumActive() + pool.getNumIdle() <= pool.getMaxTotal(),
                "Active + Idle should be <= MaxTotal");
        assertTrue(pool.getNumIdle() <= pool.getMaxIdle(), "Idle should be <= MaxIdle");
    }

    /**
     * JIRA DBCP-216
     *
     * Verify that pool counters are maintained properly and listeners are
     * cleaned up when a PooledConnection throws a connectionError event.
     */
    @Test
    void testConnectionErrorCleanup() throws Exception {
        // Setup factory
        final CPDSConnectionFactory factory = new CPDSConnectionFactory(cpds, null, Duration.ofMillis(-1), false, "userName", "password".toCharArray());
        try (final GenericObjectPool<PooledConnectionAndInfo> pool = new GenericObjectPool<>(factory)) {
            factory.setPool(pool);

            // Checkout a pair of connections
            final PooledConnection pcon1 = pool.borrowObject().getPooledConnection();
            try (final Connection con1 = pcon1.getConnection()) {
                final PooledConnection pcon2 = pool.borrowObject().getPooledConnection();
                assertEquals(2, pool.getNumActive());
                assertEquals(0, pool.getNumIdle());

                // Verify listening
                final PooledConnectionProxy pc = (PooledConnectionProxy) pcon1;
                assertTrue(pc.getListeners().contains(factory));

                // Throw connectionError event
                pc.throwConnectionError();

                // Active count should be reduced by 1
                assertEquals(1, pool.getNumActive());
                checkPoolLimits(pool);

                // Throw another one - should be ignored
                pc.throwConnectionError();
                assertEquals(1, pool.getNumActive());
                checkPoolLimits(pool);

                // Ask for another connection
                final PooledConnection pcon3 = pool.borrowObject().getPooledConnection();
                assertNotEquals(pcon3, pcon1); // better not get baddie back
                assertFalse(pc.getListeners().contains(factory)); // verify cleanup
                assertEquals(2, pool.getNumActive());
                assertEquals(0, pool.getNumIdle());

                // Return good connections back to pool
                pcon2.getConnection().close();
                pcon3.getConnection().close();
                assertEquals(2, pool.getNumIdle());
                assertEquals(0, pool.getNumActive());

                // Verify pc is closed
                assertThrows(SQLException.class, pc::getConnection, "Expecting SQLException using closed PooledConnection");

                // Back from the dead - ignore the ghost!
                con1.close();
                assertEquals(2, pool.getNumIdle());
                assertEquals(0, pool.getNumActive());

                // Clear pool
                pool.clear();
                assertEquals(0, pool.getNumIdle());
            }
        }
    }

    /**
     * JIRA: DBCP-442
     */
    @Test
    void testNullValidationQuery() throws Exception {
        final CPDSConnectionFactory factory = new CPDSConnectionFactory(cpds, null, Duration.ofMillis(-1), false, "userName", "password".toCharArray());
        try (final GenericObjectPool<PooledConnectionAndInfo> pool = new GenericObjectPool<>(factory)) {
            factory.setPool(pool);
            pool.setTestOnBorrow(true);
            final PooledConnection pcon = pool.borrowObject().getPooledConnection();
            try (final Connection con = pcon.getConnection()) {
            }
        }
    }

    @Test
    void testSetPasswordCharArray() {
        final CPDSConnectionFactory factory = new CPDSConnectionFactory(cpds, null, Duration.ofMillis(-1), false, "userName", "password".toCharArray());
        final char[] pwd = { 'a' };
        factory.setPassword(pwd);
        assertEquals("a", String.valueOf(factory.getPasswordCharArray()));
        pwd[0] = 'b';
        assertEquals("a", String.valueOf(factory.getPasswordCharArray()));
    }

    @Test
    void testSetPasswordString() {
        final CPDSConnectionFactory factory = new CPDSConnectionFactory(cpds, null, Duration.ofMillis(-1), false, "userName", "password".toCharArray());
        final String pwd = "a";
        factory.setPassword(pwd);
        assertEquals("a", String.valueOf(factory.getPasswordCharArray()));
    }

    /**
     * JIRA DBCP-216
     *
     * Check PoolableConnection close triggered by destroy is handled
     * properly. PooledConnectionProxy (dubiously) fires connectionClosed
     * when PooledConnection itself is closed.
     */
    @Test
    void testSharedPoolDSDestroyOnReturn() throws Exception {
        try (final PerUserPoolDataSource ds = new PerUserPoolDataSource()) {
            ds.setConnectionPoolDataSource(cpds);
            ds.setPerUserMaxTotal("userName", 10);
            ds.setPerUserMaxWait("userName", Duration.ofMillis(50));
            ds.setPerUserMaxIdle("userName", 2);
            final Connection conn1 = ds.getConnection("userName", "password");
            final Connection conn2 = ds.getConnection("userName", "password");
            final Connection conn3 = ds.getConnection("userName", "password");
            assertEquals(3, ds.getNumActive("userName"));
            conn1.close();
            assertEquals(1, ds.getNumIdle("userName"));
            conn2.close();
            assertEquals(2, ds.getNumIdle("userName"));
            conn3.close(); // Return to pool will trigger destroy -> close sequence
            assertEquals(2, ds.getNumIdle("userName"));
        }
    }

}
