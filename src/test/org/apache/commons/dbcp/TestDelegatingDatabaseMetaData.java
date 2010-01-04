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
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Test suite for {@link DelegatingDatabaseMetaData}.
 */
public class TestDelegatingDatabaseMetaData extends TestCase {
    public TestDelegatingDatabaseMetaData(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(TestDelegatingDatabaseMetaData.class);
    }

    private DelegatingConnection conn = null;
    private Connection delegateConn = null;
    private DelegatingDatabaseMetaData meta = null;
    private DatabaseMetaData delegateMeta = null;

    public void setUp() throws Exception {
        delegateConn = new TesterConnection("test", "test");
        delegateMeta = delegateConn.getMetaData();
        conn = new DelegatingConnection(delegateConn);
        meta = new DelegatingDatabaseMetaData(conn,delegateMeta);
    }

    public void testGetDelegate() throws Exception {
        assertEquals(delegateMeta,meta.getDelegate());
    }

    public void testHashCode() {
        try {
            delegateMeta = conn.getMetaData();
        } catch (SQLException e) {
            fail("No exception expected retrieving meta data");
        }
        DelegatingDatabaseMetaData meta1 =
            new DelegatingDatabaseMetaData(conn,delegateMeta);
        DelegatingDatabaseMetaData meta2 =
            new DelegatingDatabaseMetaData(conn,delegateMeta);
        assertEquals(meta1.hashCode(), meta2.hashCode());
    }
    
    public void testEquals() {
        try {
            delegateMeta = conn.getMetaData();
        } catch (SQLException e) {
            fail("No exception expected retrieving meta data");
        }
        DelegatingDatabaseMetaData meta1 =
            new DelegatingDatabaseMetaData(conn,delegateMeta);
        DelegatingDatabaseMetaData meta2 =
            new DelegatingDatabaseMetaData(conn,delegateMeta);
        DelegatingDatabaseMetaData meta3 =
            new DelegatingDatabaseMetaData(conn,null);
        
        assertTrue(!meta1.equals(null));
        assertTrue(meta1.equals(meta2));
        assertTrue(!meta1.equals(meta3));
    }
    
    /* JDBC_4_ANT_KEY_BEGIN */
    public void testCheckOpen() throws Exception {
        ResultSet rst = meta.getSchemas();
        assertTrue(!rst.isClosed());
        conn.close();
        assertTrue(rst.isClosed());
    }
    /* JDBC_4_ANT_KEY_END */
}
