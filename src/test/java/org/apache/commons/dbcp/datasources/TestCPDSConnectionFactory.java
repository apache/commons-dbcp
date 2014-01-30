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

package org.apache.commons.dbcp.datasources;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.PooledConnection;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.dbcp.cpdsadapter.DriverAdapterCPDS;

import org.apache.commons.pool.impl.GenericObjectPool;

/**
 * @version $Revision$ $Date$
 */
public class TestCPDSConnectionFactory extends TestCase {
   
    protected ConnectionPoolDataSourceProxy cpds = null;
    
    public TestCPDSConnectionFactory(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(TestCPDSConnectionFactory.class);
    }

    public void setUp() throws Exception {
        cpds = new ConnectionPoolDataSourceProxy(new DriverAdapterCPDS());
        DriverAdapterCPDS delegate = (DriverAdapterCPDS) cpds.getDelegate();
        delegate.setDriver("org.apache.commons.dbcp.TesterDriver");
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
       PerUserPoolDataSource ds = new PerUserPoolDataSource();
       ds.setConnectionPoolDataSource(cpds);
       ds.setPerUserMaxActive("username",new Integer(10));// Integer.valueOf() is Java 1.5
       ds.setPerUserMaxWait("username",new Integer(50));
       ds.setPerUserMaxIdle("username",new Integer(2));
       Connection conn1 = ds.getConnection("username", "password");
       Connection conn2 = ds.getConnection("username", "password");
       Connection conn3 = ds.getConnection("username", "password");
       assertEquals(3, ds.getNumActive("username", "password"));
       conn1.close();
       assertEquals(1, ds.getNumIdle("username", "password"));
       conn2.close();
       assertEquals(2, ds.getNumIdle("username", "password"));
       conn3.close(); // Return to pool will trigger destroy -> close sequence
       assertEquals(2, ds.getNumIdle("username", "password"));
    }
    
    /**
     * JIRA DBCP-216
     * 
     * Verify that pool counters are maintained properly and listeners are
     * cleaned up when a PooledConnection throws a connectionError event.
     */
    public void testConnectionErrorCleanup() throws Exception {
        // Setup factory
        GenericObjectPool pool = new GenericObjectPool(null);
        CPDSConnectionFactory factory = 
            new CPDSConnectionFactory(cpds, pool, null, "username", "password");
        
        // Checkout a pair of connections
        PooledConnection pcon1 = 
            ((PooledConnectionAndInfo) pool.borrowObject())
                .getPooledConnection();
        Connection con1 = pcon1.getConnection();
        PooledConnection pcon2 = 
            ((PooledConnectionAndInfo) pool.borrowObject())
                .getPooledConnection();
        assertEquals(2, pool.getNumActive());
        assertEquals(0, pool.getNumIdle());
        
        // Verify listening
        PooledConnectionProxy pc = (PooledConnectionProxy) pcon1;
        assertTrue(pc.getListeners().contains(factory));
        
        // Throw connectionError event
        pc.throwConnectionError();
        
        // Active count should be reduced by 1 and no idle increase
        assertEquals(1, pool.getNumActive());
        assertEquals(0, pool.getNumIdle());
        
        // Throw another one - should be ignored
        pc.throwConnectionError();
        assertEquals(1, pool.getNumActive());
        assertEquals(0, pool.getNumIdle());
        
        // Ask for another connection 
        PooledConnection pcon3 = 
            ((PooledConnectionAndInfo) pool.borrowObject())
                .getPooledConnection();
        assertTrue(!pcon3.equals(pcon1)); // better not get baddie back
        assertTrue(!pc.getListeners().contains(factory)); // verify cleanup
        assertEquals(2, pool.getNumActive());
        assertEquals(0, pool.getNumIdle());
        
        // Return good connections back to pool
        pcon2.getConnection().close();
        pcon3.getConnection().close();
        assertEquals(2, pool.getNumIdle());
        assertEquals(0, pool.getNumActive());
        
        // Verify pc is closed
        try {
           pc.getConnection();
           fail("Expecting SQLException using closed PooledConnection");
        } catch (SQLException ex) {
            // expected
        }
        
        // Back from the dead - ignore the ghost!
        con1.close();
        assertEquals(2, pool.getNumIdle());
        assertEquals(0, pool.getNumActive());
        
        // Clear pool
        factory.getPool().clear();
        assertEquals(0, pool.getNumIdle());
    }

}
