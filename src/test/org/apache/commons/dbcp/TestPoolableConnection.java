/*
 * Copyright 2006 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import java.sql.Connection; // 
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.commons.pool.KeyedObjectPoolFactory;
import org.apache.commons.pool.impl.GenericKeyedObjectPoolFactory;


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

    public void testConnectionPool() {
        // Grab a new connection from the pool
        Connection c = null;
        try {
            c = (Connection)pool.borrowObject();
        } catch (Exception e) {
            fail("Could not fetch Connection from pool: " + e.getMessage());
        }

        assertTrue("Connection should be created and should not be null", c != null);
        assertEquals("There should be exactly one active object in the pool", 1, pool.getNumActive());

        // Now return the connection by closing it
        try {
            c.close();
        } catch (SQLException e) {
            fail("Could not close connection: " + e.getMessage());
        }

        assertEquals("There should now be zero active objects in the pool", 0, pool.getNumActive());
    }

    // Bugzilla Bug 33591: PoolableConnection leaks connections if the
    // delegated connection closes itself.
    public void testPoolableConnectionLeak() {
        Connection conn = null;
        try {
            // 'Borrow' a connection from the pool
            conn = (Connection)pool.borrowObject();

            // Now close our innermost delegate, simulating the case where the
            // underlying connection closes itself
            ((PoolableConnection)conn).getInnermostDelegate().close();
            
            // At this point, we can close the pooled connection. The
            // PoolableConnection *should* realise that its underlying
            // connection is gone and invalidate itself. The pool should have no
            // active connections.
        } catch (Exception e) {
            fail("Exception occured while testing connection leak: " + e.getMessage());
        }

        try {
            conn.close();
        } catch (Exception e) {
            // Here we expect 'connection already closed', but the connection
            // should *NOT* be returned to the pool
        }

        assertEquals("The pool should have no active connections", 
            0, pool.getNumActive());
    }
    
    public void testDeadlock() {
        System.out.println("Loading drivers");
        try {
            Class.forName("org.hsqldb.jdbcDriver");
            Class.forName("org.apache.commons.dbcp.PoolingDriver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        System.out.println("Setting up pool");

        try {
            GenericObjectPool.Config config = new GenericObjectPool.Config();
            config.maxActive = 10;
            config.minIdle = 2; // Idle limits are low to allow more possibility of locking.
            config.maxIdle = 4; // Locking only occurs when there are 0 idle connections in the pool.
            config.maxWait = 5000L;
            config.testOnBorrow = true;
            config.testOnReturn = false;
            config.testWhileIdle = true;
            // Locking still occurs whether these tests are performed or not.
            config.whenExhaustedAction = GenericObjectPool.WHEN_EXHAUSTED_BLOCK;
            // Locking still occurs regardless of the exhausted action.
            config.timeBetweenEvictionRunsMillis = 3000L; // The Evictor thread is involved in the lock, so run it quite often.
            config.minEvictableIdleTimeMillis = 6000L;
            config.numTestsPerEvictionRun = 3;

            ObjectPool op = new GenericObjectPool(null, config);
            KeyedObjectPoolFactory kp = new GenericKeyedObjectPoolFactory(null);

            ConnectionFactory cf = new DriverManagerConnectionFactory(
                    "jdbc:hsqldb:target/hsqldb", "sa", "");
            PoolableConnectionFactory pcf = new PoolableConnectionFactory(cf,
                    op, null, null, false, true);
            // Locking still occurs whether there is a validation query or not.
            PoolingDriver pd = (PoolingDriver) DriverManager
                    .getDriver("jdbc:apache:commons:dbcp:");
            pd.registerPool("PoolName", op);

        } catch (SQLException e) {
            e.printStackTrace();
        }
        System.out.println("Initialized");

        int ACTIVE = 10;

        Connection[] c = new Connection[ACTIVE];

        try {
            printPoolStatus();

            int j = 0;
            // Loop forever to create a high load.
            while (j < 5000) {
                // Create a number of connections.
                for (int i = 0; i < ACTIVE; ++i) {
                    c[i] = DriverManager
                            .getConnection("jdbc:apache:commons:dbcp:PoolName");
                    //printPoolStatus();
                }
                // Then immmediately drop them.
                for (int i = 0; i < ACTIVE; ++i) {
                    try {
                        if (c[i] != null) {
                            c[i].close();
                            //printPoolStatus();
                            c[i] = null;
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
                j++;
            }

        } catch (Throwable e) {
            e.printStackTrace();

        } finally {
            // Close down any open connetions.
            for (int i = 0; i < ACTIVE; ++i) {
                try {
                    if (c[i] != null)
                        c[i].close();
                } catch (SQLException e) {
                }
            }

            System.out.println("Closing pool");
            try {
                PoolingDriver pd = (PoolingDriver) DriverManager
                        .getDriver("jdbc:apache:commons:dbcp:");
                pd.closePool("PoolName");
            } catch (SQLException e) {
                e.printStackTrace();
            }
            System.out.println("Pool closed");
        }
    }

    /**
     * Display pool status.  Locks still occur even if this method is never
     called.
     */
    private static void printPoolStatus() throws SQLException {
        PoolingDriver pd = (PoolingDriver) DriverManager
                .getDriver("jdbc:apache:commons:dbcp:");
        ObjectPool op = pd.getConnectionPool("PoolName");

        System.out.println("Active / idle: " + op.getNumActive() + " / "
                + op.getNumIdle());
    }
}
