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
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * TestSuite for BasicDataSource with prepared statement pooling enabled
 * 
 * @author Dirk Verbeeck
 * @version $Revision$ $Date$
 */
public class TestPStmtPoolingBasicDataSource extends TestBasicDataSource {
    public TestPStmtPoolingBasicDataSource(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(TestPStmtPoolingBasicDataSource.class);
    }

    public void setUp() throws Exception {
        super.setUp();

        // PoolPreparedStatements enabled, should not affect the basic tests
        ds.setPoolPreparedStatements(true);
        ds.setMaxOpenPreparedStatements(2);
    }

    public void tearDown() throws Exception {
        super.tearDown();
        // nothing to do here
    }

    public void testPreparedStatementPooling() throws Exception {
        Connection conn = getConnection();
        assertNotNull(conn);
        
        PreparedStatement stmt1 = conn.prepareStatement("select 'a' from dual");
        assertNotNull(stmt1);
        
        PreparedStatement stmt2 = conn.prepareStatement("select 'b' from dual");
        assertNotNull(stmt2);
        
        assertTrue(stmt1 != stmt2);
        
        // go over the maxOpen limit
        PreparedStatement stmt3 = null;
        try {
            stmt3 = conn.prepareStatement("select 'c' from dual");
            fail("expected SQLException");
        } 
        catch (SQLException e) {}
        
        // make idle
        stmt2.close();

        // test cleanup the 'b' statement
        stmt3 = conn.prepareStatement("select 'c' from dual");
        assertNotNull(stmt3);
        assertTrue(stmt3 != stmt1);
        assertTrue(stmt3 != stmt2);
        
        // normal reuse of statement
        stmt1.close();
        PreparedStatement stmt4 = conn.prepareStatement("select 'a' from dual");
        assertNotNull(stmt4);
    }

    // Bugzilla Bug 27246
    // PreparedStatement cache should be different depending on the Catalog    
    public void testPStmtCatalog() throws Exception {
        Connection conn = getConnection();
        conn.setCatalog("catalog1");
        DelegatingPreparedStatement stmt1 = (DelegatingPreparedStatement) conn.prepareStatement("select 'a' from dual");
        TesterPreparedStatement inner1 = (TesterPreparedStatement) stmt1.getInnermostDelegate();
        assertEquals("catalog1", inner1.getCatalog());
        stmt1.close();
        
        conn.setCatalog("catalog2");
        DelegatingPreparedStatement stmt2 = (DelegatingPreparedStatement) conn.prepareStatement("select 'a' from dual");
        TesterPreparedStatement inner2 = (TesterPreparedStatement) stmt2.getInnermostDelegate();
        assertEquals("catalog2", inner2.getCatalog());
        stmt2.close();

        conn.setCatalog("catalog1");
        DelegatingPreparedStatement stmt3 = (DelegatingPreparedStatement) conn.prepareStatement("select 'a' from dual");
        TesterPreparedStatement inner3 = (TesterPreparedStatement) stmt1.getInnermostDelegate();
        assertEquals("catalog1", inner3.getCatalog());
        stmt3.close();
        
        assertNotSame(inner1, inner2);
        assertSame(inner1, inner3);
    }

    public void testPStmtPoolingWithNoClose() throws Exception {
        ds.setMaxActive(1); // only one connection in pool needed
        ds.setMaxIdle(1);
        ds.setAccessToUnderlyingConnectionAllowed(true);
        Connection conn1 = getConnection();
        assertNotNull(conn1);
        assertEquals(1, ds.getNumActive());
        assertEquals(0, ds.getNumIdle());
        
        PreparedStatement stmt1 = conn1.prepareStatement("select 'a' from dual");
        assertNotNull(stmt1);
        
        Statement inner1 = ((DelegatingPreparedStatement) stmt1).getInnermostDelegate();
        assertNotNull(inner1);
        
        stmt1.close();

        Connection conn2 = conn1;
        assertNotNull(conn2);
        assertEquals(1, ds.getNumActive());
        assertEquals(0, ds.getNumIdle());
        
        PreparedStatement stmt2 = conn2.prepareStatement("select 'a' from dual");
        assertNotNull(stmt2);

        Statement inner2 = ((DelegatingPreparedStatement) stmt2).getInnermostDelegate();
        assertNotNull(inner2);
        
        assertSame(inner1, inner2);
    }
    
    public void testPStmtPoolingAccrossClose() throws Exception {
        ds.setMaxActive(1); // only one connection in pool needed
        ds.setMaxIdle(1);
        ds.setAccessToUnderlyingConnectionAllowed(true);
        Connection conn1 = getConnection();
        assertNotNull(conn1);
        assertEquals(1, ds.getNumActive());
        assertEquals(0, ds.getNumIdle());
        
        PreparedStatement stmt1 = conn1.prepareStatement("select 'a' from dual");
        assertNotNull(stmt1);
        
        Statement inner1 = ((DelegatingPreparedStatement) stmt1).getInnermostDelegate();
        assertNotNull(inner1);
        
        stmt1.close();
        conn1.close();

        assertEquals(0, ds.getNumActive());
        assertEquals(1, ds.getNumIdle());

        Connection conn2 = getConnection();
        assertNotNull(conn2);
        assertEquals(1, ds.getNumActive());
        assertEquals(0, ds.getNumIdle());
        
        PreparedStatement stmt2 = conn2.prepareStatement("select 'a' from dual");
        assertNotNull(stmt2);

        Statement inner2 = ((DelegatingPreparedStatement) stmt2).getInnermostDelegate();
        assertNotNull(inner2);
        
        assertSame(inner1, inner2);
    }
}
