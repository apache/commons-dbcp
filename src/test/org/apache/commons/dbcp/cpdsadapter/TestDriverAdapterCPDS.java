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

package org.apache.commons.dbcp.cpdsadapter;

import java.util.Properties;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests for DriverAdapterCPDS
 * 
 * @version $Revision:$ $Date:$
 */
public class TestDriverAdapterCPDS extends TestCase {
    public TestDriverAdapterCPDS(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(TestDriverAdapterCPDS.class);
    }

    private DriverAdapterCPDS pcds;

    public void setUp() throws Exception {
        pcds = new DriverAdapterCPDS();
        pcds.setDriver("org.apache.commons.dbcp.TesterDriver");
        pcds.setUrl("jdbc:apache:commons:testdriver");
        pcds.setUser("foo");
        pcds.setPassword("bar");
        pcds.setPoolPreparedStatements(false);
    }

    /**
     * JIRA: DBCP-245
     */
    public void testIncorrectPassword() throws Exception 
    {
        pcds.getPooledConnection("u2", "p2").close();
        try {
            // Use bad password
            pcds.getPooledConnection("u1", "zlsafjk");
            fail("Able to retrieve connection with incorrect password");
        } catch (SQLException e1) {
            // should fail

        }
        
        // Use good password
        pcds.getPooledConnection("u1", "p1").close();
        try {
            pcds.getPooledConnection("u1", "x");
            fail("Able to retrieve connection with incorrect password");
        }
        catch (SQLException e) {
            if (!e.getMessage().startsWith("x is not the correct password")) {
                throw e;
            }
            // else the exception was expected
        }
        
        // Make sure we can still use our good password.
        pcds.getPooledConnection("u1", "p1").close();
    }


    public void testSimple() throws Exception {
        Connection conn = pcds.getPooledConnection().getConnection();
        assertNotNull(conn);
        PreparedStatement stmt = conn.prepareStatement("select * from dual");
        assertNotNull(stmt);
        ResultSet rset = stmt.executeQuery();
        assertNotNull(rset);
        assertTrue(rset.next());
        rset.close();
        stmt.close();
        conn.close();
    }

    public void testSimpleWithUsername() throws Exception {
        Connection conn = pcds.getPooledConnection("u1", "p1").getConnection();
        assertNotNull(conn);
        PreparedStatement stmt = conn.prepareStatement("select * from dual");
        assertNotNull(stmt);
        ResultSet rset = stmt.executeQuery();
        assertNotNull(rset);
        assertTrue(rset.next());
        rset.close();
        stmt.close();
        conn.close();
    }

    public void testClosingWithUserName() 
        throws Exception {
        Connection[] c = new Connection[pcds.getMaxActive()];
        // open the maximum connections
        for (int i=0; i<c.length; i++) {
            c[i] = pcds.getPooledConnection("u1", "p1").getConnection();
        }

        // close one of the connections
        c[0].close();
        assertTrue(c[0].isClosed());
        // get a new connection
        c[0] = pcds.getPooledConnection("u1", "p1").getConnection();

        for (int i=0; i<c.length; i++) {
            c[i].close();
        }

        // open the maximum connections
        for (int i=0; i<c.length; i++) {
            c[i] = pcds.getPooledConnection("u1", "p1").getConnection();
        }
        for (int i=0; i<c.length; i++) {
            c[i].close();
        }
    }
    
    public void testSetProperties() throws Exception {
        // Set user property to bad value
        pcds.setUser("bad");
        // Supply correct value in connection properties
        // This will overwrite field value
        Properties properties = new Properties();
        properties.put("user", "foo");
        pcds.setConnectionProperties(properties);
        pcds.getPooledConnection().close();
        assertEquals("foo", pcds.getUser());
        // Put bad password into properties
        properties.put("password", "bad");
        // This does not change local field
        assertEquals("bar", pcds.getPassword());
        // Supply correct password in getPooledConnection
        // Call will succeed and overwrite property
        pcds.getPooledConnection("foo", "bar").close();
        assertEquals("bar", pcds.getConnectionProperties().getProperty("password"));     
    }
}
