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
import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;

/**
 * TestSuite for PoolingDataSource
 * 
 * @version $Revision: 392677 $ $Date: 2006-04-08 21:42:24 -0700 (Sat, 08 Apr 2006) $
 */
public class TestPoolingDataSource extends TestConnectionPool {
    public TestPoolingDataSource(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(TestPoolingDataSource.class);
    }

    protected Connection getConnection() throws Exception {
        return ds.getConnection();
    }

    protected PoolingDataSource ds = null;
    private GenericObjectPool pool = null;

    public void setUp() throws Exception {
        super.setUp();
        pool = new GenericObjectPool();
        pool.setMaxActive(getMaxActive());
        pool.setMaxWait(getMaxWait());
        Properties props = new Properties();
        props.setProperty("user", "username");
        props.setProperty("password", "password");
        PoolableConnectionFactory factory = 
            new PoolableConnectionFactory(
                new DriverConnectionFactory(new TesterDriver(),
                        "jdbc:apache:commons:testdriver", props),
                pool, null, "SELECT DUMMY FROM DUAL", true, true);
        pool.setFactory(factory);
        ds = new PoolingDataSource(pool);
        ds.setAccessToUnderlyingConnectionAllowed(true);
    }

    public void tearDown() throws Exception {
        pool.close();
        super.tearDown();
    }
    
    public void testPoolGuardConnectionWrapperEqualsSameDelegate() throws Exception {
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
    
    private void checkPoolGuardConnectionWrapperEqualsReflexive() throws Exception {
        Connection con = ds.getConnection();
        Connection con2 = con;
        assertTrue(con2.equals(con));
        assertTrue(con.equals(con2));
        con.close();
    }
    
    /*
     * JIRA: DBCP-198
     */
    public void testPoolGuardConnectionWrapperEqualsReflexive()
        throws Exception {
        // Statndard setup - using DelegatingConnections
        // returned from PoolableConnectionFactory
        checkPoolGuardConnectionWrapperEqualsReflexive();
        // Force PoolGuardConnectionWrappers to wrap non-Delegating connections
        pool.close();
        pool = new GenericObjectPool();
        pool.setMaxActive(getMaxActive());
        pool.setMaxWait(getMaxWait());
        Properties props = new Properties();
        props.setProperty("user", "username");
        props.setProperty("password", "password");
        NonDelegatingPoolableConnectionFactory factory = 
            new NonDelegatingPoolableConnectionFactory(
                new DriverConnectionFactory(new TesterDriver(),
                        "jdbc:apache:commons:testdriver", props), pool);
        pool.setFactory(factory);
        ds = new PoolingDataSource(pool);
        checkPoolGuardConnectionWrapperEqualsReflexive();
    }
    
    public void testPoolGuardConnectionWrapperEqualsFail() throws Exception {
        Connection con1 = ds.getConnection();
        Connection con2 = ds.getConnection();
        assertFalse(con1.equals(con2));
        con1.close();
        con2.close();
    }
    
    public void testPoolGuardConnectionWrapperEqualsNull() throws Exception {
        Connection con1 = ds.getConnection();
        Connection con2 = null;
        assertFalse(con1.equals(con2));
        con1.close();
    }
    
    public void testPoolGuardConnectionWrapperEqualsType() throws Exception {
        Connection con1 = ds.getConnection();
        Integer con2 = new Integer(0);
        assertFalse(con1.equals(con2));
        con1.close();
    }
    
    public void testestPoolGuardConnectionWrapperEqualInnermost() throws Exception {
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
    
    /** Factory to return non-delegating connections for DBCP-198 test */
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
