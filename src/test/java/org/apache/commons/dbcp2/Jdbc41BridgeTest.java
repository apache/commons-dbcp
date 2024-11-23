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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Tests {@link Jdbc41Bridge}.
 */
public class Jdbc41BridgeTest {

    @SuppressWarnings("resource")
    @Test
    public void testAbort() throws SQLException {
        // Normal
        try (Connection conn = getConnection()) {
            Jdbc41Bridge.abort(conn, r -> {
                // empty for now
            });
        }
        // Force AbstractMethodError
        try (Connection conn = getConnection()) {
            final Connection spy = Mockito.spy(conn);
            Mockito.doThrow(new AbstractMethodError()).when(spy).abort(r -> {
                // empty for now
            });
            Jdbc41Bridge.abort(spy, r -> {
                // empty for now
            });
        }
    }

    @SuppressWarnings("resource")
    @Test
    public void testCloseOnCompletion() throws SQLException {
        // Normal
        try (Connection conn = getConnection();
                Statement stmt = conn.createStatement()) {
            Jdbc41Bridge.closeOnCompletion(stmt);
        }
        // Force AbstractMethodError
        try (Connection conn = getConnection();
                Statement stmt = conn.createStatement()) {
            final Statement spy = Mockito.spy(stmt);
            Mockito.doThrow(new AbstractMethodError()).when(spy).closeOnCompletion();
            Jdbc41Bridge.closeOnCompletion(spy);
        }
    }

    @SuppressWarnings("resource")
    @Test
    public void testGeneratedKeyAlwaysReturned() throws SQLException {
        // Normal
        try (Connection conn = getConnection()) {
            assertTrue(Jdbc41Bridge.generatedKeyAlwaysReturned(conn.getMetaData()));
        }
        // Cannot mock a final class
        // Force AbstractMethodError
//        try (Connection conn = getConnection()) {
//            final DatabaseMetaData spy = Mockito.spy(conn.getMetaData());
//            Mockito.when(spy.generatedKeyAlwaysReturned()).thenThrow(AbstractMethodError.class);
//            assertTrue(Jdbc41Bridge.generatedKeyAlwaysReturned(spy));
//        }
    }

    @Test
    public void testGetNetworkTimeout() throws SQLException {
        // Normal
        try (Connection conn = getConnection()) {
            Jdbc41Bridge.setNetworkTimeout(conn, r -> {
            }, 30_000);
            // noop in H2
            assertEquals(0, Jdbc41Bridge.getNetworkTimeout(conn));
        }
    }

    @Test
    public void testGetObjectIndex() throws SQLException {
        // Normal
        try (Connection conn = getConnection();
                ResultSet rs = conn.getMetaData().getTypeInfo()) {
            rs.next();
            assertNotNull(Jdbc41Bridge.getObject(rs, 1, String.class));
        }
    }

    @Test
    public void testGetObjectName() throws SQLException {
        // Normal
        try (Connection conn = getConnection();
                ResultSet rs = conn.getMetaData().getTypeInfo()) {
            rs.next();
            assertNotNull(Jdbc41Bridge.getObject(rs, "TYPE_NAME", String.class));
        }
    }

    @SuppressWarnings("resource")
    @Test
    public void testGetSchema() throws SQLException {
        // Normal
        try (Connection conn = getConnection()) {
            assertNotNull(Jdbc41Bridge.getSchema(conn));
            final Connection spy = Mockito.spy(conn);
            Mockito.when(spy.getSchema()).thenThrow(AbstractMethodError.class);
            assertNull(Jdbc41Bridge.getSchema(spy));
        }
    }

    @Test
    public void testIsCloseOnCompletion() throws SQLException {
        // Normal
        try (Connection conn = getConnection();
                Statement stmt = conn.createStatement()) {
            assertFalse(Jdbc41Bridge.isCloseOnCompletion(stmt));
        }
    }

    @Test
    public void testSetNetworkTimeout() throws SQLException {
        // Normal
        try (Connection conn = getConnection();
                Statement stmt = conn.createStatement()) {
            // noop in H2
            Jdbc41Bridge.setNetworkTimeout(conn, r -> {
                // empty for now
            }, 30_0000);
            assertEquals(0, Jdbc41Bridge.getNetworkTimeout(conn));
        }
    }

    @Test
    public void testSetSchema() throws SQLException {
        // Normal
        try (Connection conn = getConnection();
                Statement stmt = conn.createStatement()) {
            Jdbc41Bridge.setSchema(conn, Jdbc41Bridge.getSchema(conn));
            final String expected = "PUBLIC";
            Jdbc41Bridge.setSchema(conn, expected);
            assertEquals(expected, Jdbc41Bridge.getSchema(conn));
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:h2:mem:test");
    }
}
