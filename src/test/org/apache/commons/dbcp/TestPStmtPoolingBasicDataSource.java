/*
 * Copyright 1999-2004 The Apache Software Foundation.
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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * TestSuite for BasicDataSource with prepared statement pooling enabled
 * 
 * @author Dirk Verbeeck
 * @version $Revision: 1.3 $ $Date: 2004/02/28 11:47:52 $
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
}
