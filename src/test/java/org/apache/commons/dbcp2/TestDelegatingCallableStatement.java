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

package org.apache.commons.dbcp2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.CallableStatement;
import java.sql.Connection;

import org.junit.Before;
import org.junit.Test;

/**
 * @version $Id$
 */
public class TestDelegatingCallableStatement {

    private DelegatingConnection<Connection> conn = null;
    private Connection delegateConn = null;
    private DelegatingCallableStatement stmt = null;
    private CallableStatement delegateStmt = null;

    @Before
    public void setUp() throws Exception {
        delegateConn = new TesterConnection("test", "test");
        conn = new DelegatingConnection<>(delegateConn);
    }

    @Test
    public void testExecuteQueryReturnsNull() throws Exception {
        delegateStmt = new TesterCallableStatement(delegateConn,"null");
        stmt = new DelegatingCallableStatement(conn,delegateStmt);
        assertNull(stmt.executeQuery());
    }

    @Test
    public void testExecuteQueryReturnsNotNull() throws Exception {
        delegateStmt = new TesterCallableStatement(delegateConn,"select * from foo");
        stmt = new DelegatingCallableStatement(conn,delegateStmt);
        assertTrue(null != stmt.executeQuery());
    }

    @Test
    public void testGetDelegate() throws Exception {
        delegateStmt = new TesterCallableStatement(delegateConn,"select * from foo");
        stmt = new DelegatingCallableStatement(conn,delegateStmt);
        assertEquals(delegateStmt,stmt.getDelegate());
    }
}
