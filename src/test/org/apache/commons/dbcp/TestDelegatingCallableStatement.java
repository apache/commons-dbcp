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

import java.sql.CallableStatement;
import java.sql.Connection;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @version $Revision$ $Date$
 */
public class TestDelegatingCallableStatement extends TestCase {
    public TestDelegatingCallableStatement(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(TestDelegatingCallableStatement.class);
    }

    private DelegatingConnection conn = null;
    private Connection delegateConn = null;
    private DelegatingCallableStatement stmt = null;
    private CallableStatement delegateStmt = null;

    public void setUp() throws Exception {
        delegateConn = new TesterConnection("test", "test");
        conn = new DelegatingConnection(delegateConn);
    }

    public void testExecuteQueryReturnsNull() throws Exception {
        delegateStmt = new TesterCallableStatement(delegateConn,"null");
        stmt = new DelegatingCallableStatement(conn,delegateStmt);
        assertNull(stmt.executeQuery());
    }

    public void testExecuteQueryReturnsNotNull() throws Exception {
        delegateStmt = new TesterCallableStatement(delegateConn,"select * from foo");
        stmt = new DelegatingCallableStatement(conn,delegateStmt);
        assertTrue(null != stmt.executeQuery());
    }

    public void testGetDelegate() throws Exception {
        delegateStmt = new TesterCallableStatement(delegateConn,"select * from foo");
        stmt = new DelegatingCallableStatement(conn,delegateStmt);
        assertEquals(delegateStmt,stmt.getDelegate());
    }

    public void testHashCodeNull() {
        stmt = new DelegatingCallableStatement(conn, null);
        assertEquals(0, stmt.hashCode());
    }
    
    public void testHashCode() {
        delegateStmt = new TesterCallableStatement(delegateConn,"select * from foo");
        DelegatingCallableStatement stmt1 = new DelegatingCallableStatement(conn,delegateStmt);
        DelegatingCallableStatement stmt2 = new DelegatingCallableStatement(conn,delegateStmt);
        assertEquals(stmt1.hashCode(), stmt2.hashCode());
    }
    
    public void testEquals() {
        delegateStmt = new TesterCallableStatement(delegateConn,"select * from foo");
        CallableStatement del = new TesterCallableStatement(delegateConn,"select * from foo");
        DelegatingCallableStatement stmt1 = new DelegatingCallableStatement(conn, delegateStmt);
        DelegatingCallableStatement stmt2 = new DelegatingCallableStatement(conn, delegateStmt);
        DelegatingCallableStatement stmt3 = new DelegatingCallableStatement(conn, null);
        DelegatingCallableStatement stmt4 = new DelegatingCallableStatement(conn, del);
        
        // Nothing is equal to null
        assertFalse(stmt1.equals(null));
        assertFalse(stmt2.equals(null));
        assertFalse(stmt3.equals(null));
        assertFalse(stmt4.equals(null));
        
        // 1 & 2 are equivalent
        assertTrue(stmt1.equals(stmt2));
        assertTrue(stmt2.equals(stmt1)); // reflexive

        // 1 & 3 are not (different statements, one null)
        assertFalse(stmt1.equals(stmt3));
        assertFalse(stmt3.equals(stmt1)); // reflexive

        // 1 & 4 are not (different statements)
        assertFalse(stmt1.equals(stmt4));
        assertFalse(stmt4.equals(stmt1)); // reflexive

        // Check self-equals
        assertTrue(stmt1.equals(stmt1));
        assertTrue(stmt2.equals(stmt2));
        assertFalse(stmt3.equals(stmt3)); // because underlying statement is null
        assertTrue(stmt4.equals(stmt4));
        
        DelegatingStatement dstmt1 = stmt1;
        
        // 1 & 2 are equivalent
        assertTrue(dstmt1.equals(stmt2));
        assertTrue(stmt2.equals(dstmt1)); // reflexive

    }

}
