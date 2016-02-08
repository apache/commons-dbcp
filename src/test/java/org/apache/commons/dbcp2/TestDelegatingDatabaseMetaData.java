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
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

import org.junit.Before;
import org.junit.Test;

/**
 * Test suite for {@link DelegatingDatabaseMetaData}.
 */
public class TestDelegatingDatabaseMetaData {

    private DelegatingConnection<Connection> conn = null;
    private Connection delegateConn = null;
    private DelegatingDatabaseMetaData meta = null;
    private DatabaseMetaData delegateMeta = null;

    @Before
    public void setUp() throws Exception {
        delegateConn = new TesterConnection("test", "test");
        delegateMeta = delegateConn.getMetaData();
        conn = new DelegatingConnection<>(delegateConn);
        meta = new DelegatingDatabaseMetaData(conn,delegateMeta);
    }

    @Test
    public void testGetDelegate() throws Exception {
        assertEquals(delegateMeta,meta.getDelegate());
    }

    @Test
    /* JDBC_4_ANT_KEY_BEGIN */
    public void testCheckOpen() throws Exception {
        final ResultSet rst = meta.getSchemas();
        assertTrue(!rst.isClosed());
        conn.close();
        assertTrue(rst.isClosed());
    }
    /* JDBC_4_ANT_KEY_END */
}
