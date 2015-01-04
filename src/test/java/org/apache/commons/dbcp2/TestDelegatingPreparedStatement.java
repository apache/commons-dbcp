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

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.junit.Before;
import org.junit.Test;

/**
 * @author Rodney Waldhoff
 * @author Dirk Verbeeck
 * @version $Id$
 */
public class TestDelegatingPreparedStatement {

    private DelegatingConnection<Connection> conn = null;
    private Connection delegateConn = null;
    private DelegatingPreparedStatement stmt = null;
    private PreparedStatement delegateStmt = null;

    @Before
    public void setUp() throws Exception {
        delegateConn = new TesterConnection("test", "test");
        conn = new DelegatingConnection<>(delegateConn);
    }

    @Test
    public void testExecuteQueryReturnsNull() throws Exception {
        delegateStmt = new TesterPreparedStatement(delegateConn,"null");
        stmt = new DelegatingPreparedStatement(conn,delegateStmt);
        assertNull(stmt.executeQuery());
    }

    @Test
    public void testExecuteQueryReturnsNotNull() throws Exception {
        delegateStmt = new TesterPreparedStatement(delegateConn,"select * from foo");
        stmt = new DelegatingPreparedStatement(conn,delegateStmt);
        assertTrue(null != stmt.executeQuery());
    }

    @Test
    public void testGetDelegate() throws Exception {
        delegateStmt = new TesterPreparedStatement(delegateConn,"select * from foo");
        stmt = new DelegatingPreparedStatement(conn,delegateStmt);
        assertEquals(delegateStmt,stmt.getDelegate());
    }
}
