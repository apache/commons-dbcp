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

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * TestSuite for BasicDataSource with abandoned connection trace enabled
 * 
 * @author Dirk Verbeeck
 * @version $Revision$ $Date$
 */
public class TestAbandonedBasicDataSource extends TestBasicDataSource {
    public TestAbandonedBasicDataSource(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(TestAbandonedBasicDataSource.class);
    }

    public void setUp() throws Exception {
        super.setUp();

        // abandoned enabled but should not affect the basic tests
        // (very high timeout)
        ds.setLogAbandoned(true);
        ds.setRemoveAbandoned(true);
        ds.setRemoveAbandonedTimeout(10000);
    }

    public void tearDown() throws Exception {
        super.tearDown();
        // nothing to do here
    }

    // ---------- Abandoned Test -----------

    public void testAbandoned() throws Exception {
        // force abandoned
        ds.setRemoveAbandonedTimeout(0);
        ds.setMaxActive(1);

        for (int i = 0; i < 3; i++) {
            assertNotNull(ds.getConnection());
        }
    }
    
    public void testAbandonedClose() throws Exception {
        // force abandoned
        ds.setRemoveAbandonedTimeout(0);
        ds.setMaxActive(1);
        ds.setAccessToUnderlyingConnectionAllowed(true);

        Connection conn1 = getConnection();
        assertNotNull(conn1);
        assertEquals(1, ds.getNumActive());
        
        Connection conn2 = getConnection();
        // Attempt to borrow object triggers abandoned cleanup
        // conn1 should be closed by the pool to make room
        assertNotNull(conn2);
        assertEquals(1, ds.getNumActive());
        // Verify that conn1 is closed
        assertTrue(((DelegatingConnection) conn1).getInnermostDelegate().isClosed());
        
        conn2.close();
        assertEquals(0, ds.getNumActive());
        
        // Second close on conn1 is OK as of dbcp 1.3
        conn1.close();
        assertEquals(0, ds.getNumActive());
    }

    public void testAbandonedCloseWithExceptions() throws Exception {
        // force abandoned
        ds.setRemoveAbandonedTimeout(0);
        ds.setMaxActive(1);
        ds.setAccessToUnderlyingConnectionAllowed(true);

        Connection conn1 = getConnection();
        assertNotNull(conn1);
        assertEquals(1, ds.getNumActive());
        
        Connection conn2 = getConnection();        
        assertNotNull(conn2);
        assertEquals(1, ds.getNumActive());
        
        // set an IO failure causing the isClosed mathod to fail
        TesterConnection tconn1 = (TesterConnection) ((DelegatingConnection)conn1).getInnermostDelegate();
        tconn1.setFailure(new IOException("network error"));
        TesterConnection tconn2 = (TesterConnection) ((DelegatingConnection)conn2).getInnermostDelegate();
        tconn2.setFailure(new IOException("network error"));
        
        try { conn2.close(); } catch (SQLException ex) { }
        assertEquals(0, ds.getNumActive());
        
        try { conn1.close(); } catch (SQLException ex) { }
        assertEquals(0, ds.getNumActive());
    }
    
    /**
     * Verify that lastUsed property is updated when a connection
     * creates or prepares a statement
     */
    public void testlastUsed() throws Exception {
        ds.setRemoveAbandonedTimeout(1);
        ds.setMaxActive(2);
        Connection conn1 = ds.getConnection();
        Thread.sleep(500);
        conn1.createStatement(); // Should reset lastUsed
        Thread.sleep(800);
        Connection conn2 = ds.getConnection(); // triggers abandoned cleanup
        conn1.createStatement(); // Should still be OK
        conn2.close();
        Thread.sleep(500);
        conn1.prepareStatement("SELECT 1 FROM DUAL"); // reset
        Thread.sleep(800);
        ds.getConnection(); // trigger abandoned cleanup again
        conn1.createStatement();         
    }
    
    /**
     * Verify that lastUsed property is updated when a connection
     * prepares a callable statement.
     */
    public void testlastUsedPrepareCall() throws Exception {
        ds.setRemoveAbandonedTimeout(1);
        ds.setMaxActive(2);
        Connection conn1 = ds.getConnection();
        Thread.sleep(500);
        conn1.prepareCall("{call home}"); // Should reset lastUsed
        Thread.sleep(800);
        Connection conn2 = ds.getConnection(); // triggers abandoned cleanup
        conn1.prepareCall("{call home}"); // Should still be OK
        conn2.close();
        Thread.sleep(500);
        conn1.prepareCall("{call home}"); // reset
        Thread.sleep(800);
        ds.getConnection(); // trigger abandoned cleanup again
        conn1.createStatement();         
    }
}
