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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.junit.Before;
import org.junit.Test;

/**
 */
public class TestDelegatingConnection {

    private DelegatingConnection<? extends Connection> conn = null;
    private Connection delegateConn = null;
    private Connection delegateConn2 = null;

    @Before
    public void setUp() throws Exception {
        delegateConn = new TesterConnection("test", "test");
        delegateConn2 = new TesterConnection("test", "test");
        conn = new DelegatingConnection<>(delegateConn);
    }

    @Test
    public void testGetDelegate() throws Exception {
        assertEquals(delegateConn,conn.getDelegate());
    }

    @Test
    public void testConnectionToString() throws Exception {
        final String s = conn.toString();
        assertNotNull(s);
        assertTrue(s.length() > 0);
    }

    @Test
    public void testCheckOpen() throws Exception {
        conn.checkOpen();
        conn.close();
        try {
            conn.checkOpen();
            fail("Expecting SQLException");
        } catch (final SQLException ex) {
            // expected
        }
    }

    /**
     * Verify fix for DBCP-241
     */
    @Test
    public void testCheckOpenNull() throws Exception {
        try {
            conn.close();
            conn.checkOpen();
            fail("Expecting SQLException");
        } catch (final SQLException ex) {
            assertTrue(ex.getMessage().endsWith("is closed."));
        }

        try {
            conn = new DelegatingConnection<>(null);
            conn.setClosedInternal(true);
            conn.checkOpen();
            fail("Expecting SQLException");
        } catch (final SQLException ex) {
            assertTrue(ex.getMessage().endsWith("is null."));
        }

        try {
            final PoolingConnection pc = new PoolingConnection(delegateConn2);
            pc.setStatementPool(new GenericKeyedObjectPool<>(pc));
            conn = new DelegatingConnection<>(pc);
            pc.close();
            conn.close();
            try (PreparedStatement ps = conn.prepareStatement("")){}
            fail("Expecting SQLException");
        } catch (final SQLException ex) {
            assertTrue(ex.getMessage().endsWith("is closed."));
        }

        try {
            conn = new DelegatingConnection<>(new RTEGeneratingConnection());
            conn.close();
            conn.checkOpen();
            fail("Expecting SQLException");
        } catch (final SQLException ex) {
            assertTrue(ex.getMessage().endsWith("is closed."));
        }
    }

    /**
     * Delegate that will throw RTE on toString
     * Used to validate fix for DBCP-241
     */
    static class RTEGeneratingConnection extends TesterConnection {
        public RTEGeneratingConnection() {
            super("","");
        }
        @Override
        public String toString() {
            throw new RuntimeException("bang!");
        }

    }

    @Test
    public void testIsClosed() throws Exception {
        conn.checkOpen();
        assertFalse(conn.isClosed());
        conn.close();
        assertTrue(conn.isClosed());
    }

    @Test
    public void testIsClosedNullDelegate() throws Exception {
        conn.checkOpen();
        assertFalse(conn.isClosed());
        conn.setDelegate(null);
        assertTrue(conn.isClosed());
    }

}
