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
import java.sql.SQLException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.pool.impl.GenericKeyedObjectPool;

/**
 * @author Dirk Verbeeck
 * @version $Revision$ $Date$
 */
public class TestDelegatingConnection extends TestCase {
    public TestDelegatingConnection(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(TestDelegatingConnection.class);
    }

    private DelegatingConnection conn = null;
    private Connection delegateConn = null;
    private Connection delegateConn2 = null;

    public void setUp() throws Exception {
        delegateConn = new TesterConnection("test", "test");
        delegateConn2 = new TesterConnection("test", "test");
        conn = new DelegatingConnection(delegateConn);
    }


    public void testGetDelegate() throws Exception {
        assertEquals(delegateConn,conn.getDelegate());
    }

    public void testConnectionToString() throws Exception {
        String s = conn.toString();
        assertNotNull(s);
        assertTrue(s.length() > 0);
    }

    public void testHashCodeEqual() {
        DelegatingConnection conn = new DelegatingConnection(delegateConn);
        DelegatingConnection conn2 = new DelegatingConnection(delegateConn);
        assertEquals(conn.hashCode(), conn2.hashCode());
    }

    public void testHashCodeNotEqual() {
        DelegatingConnection conn = new DelegatingConnection(delegateConn);
        DelegatingConnection conn2 = new DelegatingConnection(delegateConn2);
        assertTrue(conn.hashCode() != conn2.hashCode());
    }
    
    public void testEquals() {
        DelegatingConnection conn = new DelegatingConnection(delegateConn);
        DelegatingConnection conn2 = new DelegatingConnection(delegateConn);
        DelegatingConnection conn3 = new DelegatingConnection(null);
        
        assertTrue(!conn.equals(null));
        assertTrue(conn.equals(conn2));
        assertTrue(!conn.equals(conn3));
        assertTrue(conn.equals(conn));
    }
    
    public void testCheckOpen() throws Exception {
        conn.checkOpen();
        conn.close();
        try {
            conn.checkOpen();
            fail("Expecting SQLException");
        } catch (SQLException ex) {
            // expected
        }      
    }
    
    /**
     * Verify fix for DBCP-241
     */
    public void testCheckOpenNull() throws Exception {
        try {
            conn.close();
            conn.checkOpen();
            fail("Expecting SQLException");
        } catch (SQLException ex) {
            assertTrue(ex.getMessage().endsWith("is closed."));
        }

        try {
            conn = new DelegatingConnection(null);
            conn._closed = true;  
            conn.checkOpen();
            fail("Expecting SQLException");
        } catch (SQLException ex) {
            assertTrue(ex.getMessage().endsWith("is null."));
        }

        try {
            PoolingConnection pc = new PoolingConnection
                (delegateConn2, new GenericKeyedObjectPool());
            conn = new DelegatingConnection(pc);
            pc.close();
            conn.close();
            conn.prepareStatement("");
            fail("Expecting SQLException");
        } catch (SQLException ex) {
            assertTrue(ex.getMessage().endsWith("invalid PoolingConnection."));
        }   
    }
}
