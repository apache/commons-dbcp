/**
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
package org.apache.commons.dbcp.managed;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.DelegatingConnection;
import org.apache.commons.dbcp.DriverConnectionFactory;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.PoolingDataSource;
import org.apache.commons.dbcp.TestConnectionPool;
import org.apache.commons.dbcp.TesterDriver;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.geronimo.transaction.manager.TransactionManagerImpl;

import javax.transaction.TransactionManager;
import java.sql.Connection;
import java.util.Properties;

/**
 * TestSuite for ManagedDataSource without a transaction in progress.
 *
 * @author Dain Sundstrom
 * @version $Revision$
 */
public class TestManagedDataSource extends TestConnectionPool {
    public TestManagedDataSource(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(TestManagedDataSource.class);
    }

    protected Connection getConnection() throws Exception {
        return ds.getConnection();
    }

    protected PoolingDataSource ds = null;
    private GenericObjectPool pool = null;
    protected TransactionManager transactionManager;

    public void setUp() throws Exception {
        super.setUp();

        // create a GeronimoTransactionManager for testing
        transactionManager = new TransactionManagerImpl();

        // create a driver connection factory
        Properties properties = new Properties();
        properties.setProperty("user", "username");
        properties.setProperty("password", "password");
        ConnectionFactory connectionFactory = new DriverConnectionFactory(new TesterDriver(), "jdbc:apache:commons:testdriver", properties);

        // wrap it with a LocalXAConnectionFactory
        XAConnectionFactory xaConnectionFactory = new LocalXAConnectionFactory(transactionManager, connectionFactory);

        // create the pool
        pool = new GenericObjectPool();
        pool.setMaxActive(getMaxActive());
        pool.setMaxWait(getMaxWait());

        // create the pool object factory
        PoolableConnectionFactory factory = new PoolableConnectionFactory(xaConnectionFactory, pool, null, "SELECT DUMMY FROM DUAL", true, true);
        pool.setFactory(factory);

        // finally create the datasource
        ds = new ManagedDataSource(pool, xaConnectionFactory.getTransactionRegistry());
        ds.setAccessToUnderlyingConnectionAllowed(true);
    }

    public void tearDown() throws Exception {
        pool.close();
        super.tearDown();
    }

    /**
     * Verify the accessToUnderlyingConnectionAllowed propertly limits access to the physical connection.
     */
    public void testAccessToUnderlyingConnectionAllowed() throws Exception {
        ds.setAccessToUnderlyingConnectionAllowed(true);
        ManagedConnection connection = (ManagedConnection) newConnection();
        assertTrue(connection.isAccessToUnderlyingConnectionAllowed());
        assertNotNull(connection.getDelegate());
        assertNotNull(connection.getInnermostDelegate());
        connection.close();

        ds.setAccessToUnderlyingConnectionAllowed(false);
        connection = (ManagedConnection) newConnection();
        assertFalse(connection.isAccessToUnderlyingConnectionAllowed());
        assertNull(connection.getDelegate());
        assertNull(connection.getInnermostDelegate());
        connection.close();
    }

    /**
     * Verify that conection sharing is working (or not working) as expected.
     */
    public void testSharedConnection() throws Exception {
        DelegatingConnection connectionA = (DelegatingConnection) newConnection();
        DelegatingConnection connectionB = (DelegatingConnection) newConnection();

        assertFalse(connectionA.equals(connectionB));
        assertFalse(connectionB.equals(connectionA));
        assertFalse(connectionA.innermostDelegateEquals(connectionB.getInnermostDelegate()));
        assertFalse(connectionB.innermostDelegateEquals(connectionA.getInnermostDelegate()));

        connectionA.close();
        connectionB.close();
    }

    public void testManagedConnectionEqualsSameDelegateNoUnderlyingAccess() throws Exception {
        // Get a maximal set of connections from the pool
        Connection[] c = new Connection[getMaxActive()];
        for (int i = 0; i < c.length; i++) {
            c[i] = newConnection();
        }
        // Close the delegate of one wrapper in the pool
        ((DelegatingConnection) c[0]).getDelegate().close();

        // Disable access for the new connection
        ds.setAccessToUnderlyingConnectionAllowed(false);
        // Grab a new connection - should get c[0]'s closed connection
        // so should be delegate-equivalent, so equal
        Connection con = newConnection();
        assertTrue(c[0].equals(con));
        assertTrue(con.equals(c[0]));
        for (int i = 0; i < c.length; i++) {
            c[i].close();
        }
        ds.setAccessToUnderlyingConnectionAllowed(true);
    }

    public void testManagedConnectionEqualsSameDelegate() throws Exception {
        // Get a maximal set of connections from the pool
        Connection[] c = new Connection[getMaxActive()];
        for (int i = 0; i < c.length; i++) {
            c[i] = newConnection();
        }
        // Close the delegate of one wrapper in the pool
        ((DelegatingConnection) c[0]).getDelegate().close();

        // Grab a new connection - should get c[0]'s closed connection
        // so should be delegate-equivalent, so equal
        Connection con = newConnection();
        assertTrue(c[0].equals(con));
        assertTrue(con.equals(c[0]));
        for (int i = 0; i < c.length; i++) {
            c[i].close();
        }
    }


    /*
    * JIRA: DBCP-198
    */
    public void testManagedConnectionEqualsReflexive() throws Exception {
        // Statndard setup - using DelegatingConnections
        // returned from PoolableConnectionFactory
        checkManagedConnectionEqualsReflexive();

        // Force ManagedConnections to wrap non-Delegating connections
        pool.close();
        pool = new GenericObjectPool();
        pool.setMaxActive(getMaxActive());
        pool.setMaxWait(getMaxWait());
        Properties props = new Properties();
        props.setProperty("user", "username");
        props.setProperty("password", "password");
        NonDelegatingPoolableConnectionFactory factory = new NonDelegatingPoolableConnectionFactory(new DriverConnectionFactory(new TesterDriver(), "jdbc:apache:commons:testdriver", props), pool);
        pool.setFactory(factory);
        ds = new PoolingDataSource(pool);
        checkManagedConnectionEqualsReflexive();
    }

    private void checkManagedConnectionEqualsReflexive() throws Exception {
        Connection con = ds.getConnection();
        Connection con2 = con;
        assertTrue(con2.equals(con));
        assertTrue(con.equals(con2));
        con.close();
    }

    public void testManagedConnectionEqualsFail() throws Exception {
        Connection con1 = ds.getConnection();
        Connection con2 = ds.getConnection();
        assertFalse(con1.equals(con2));
        con1.close();
        con2.close();
    }

    public void testManagedConnectionEqualsNull() throws Exception {
        Connection con1 = ds.getConnection();
        Connection con2 = null;
        assertFalse(con1.equals(con2));
        con1.close();
    }

    public void testManagedConnectionEqualsType() throws Exception {
        Connection con1 = ds.getConnection();
        Integer con2 = new Integer(0);
        assertFalse(con1.equals(con2));
        con1.close();
    }

    public void testManagedConnectionEqualInnermost() throws Exception {
        ds.setAccessToUnderlyingConnectionAllowed(true);
        DelegatingConnection con = (DelegatingConnection) ds.getConnection();
        Connection inner = con.getInnermostDelegate();
        ds.setAccessToUnderlyingConnectionAllowed(false);
        DelegatingConnection con2 = new DelegatingConnection(inner);
        assertTrue(con2.equals(con));
        assertTrue(con.innermostDelegateEquals(con2.getInnermostDelegate()));
        assertTrue(con2.innermostDelegateEquals(inner));
        assertTrue(con.equals(con2));
    }

    /**
     * Factory to return non-delegating connections for DBCP-198 test
     */
    private static class NonDelegatingPoolableConnectionFactory
            extends PoolableConnectionFactory {
        public NonDelegatingPoolableConnectionFactory(ConnectionFactory connFactory, ObjectPool pool) {
            super(connFactory, pool, null, null, true, true);
        }

        synchronized public Object makeObject() throws Exception {
            return _connFactory.createConnection();
        }
    }
}
