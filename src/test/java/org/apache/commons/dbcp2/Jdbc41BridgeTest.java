/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.concurrent.Executor;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Tests {@link Jdbc41Bridge}.
 */
class Jdbc41BridgeTest extends AbstractDatabaseTest {

    private Connection getConnection() throws SQLException {
        return database.connect();
    }

    @SuppressWarnings("resource")
    @Test
    void testAbort() throws SQLException {
        // Normal
        try (Connection conn = getConnection(); Connection physical = getConnection()) {
            physical.abort(Runnable::run);
            Jdbc41Bridge.abort(conn, Runnable::run);
            assertEquals(physical.isClosed(), conn.isClosed());
        }
        // Force AbstractMethodError
        try (Connection conn = getConnection()) {
            final Connection legacy = new DelegatingConnection<Connection>(conn) {
                @Override
                public void abort(final Executor executor) {
                    throw new AbstractMethodError();
                }
            };
            Jdbc41Bridge.abort(legacy, Runnable::run);
            assertTrue(conn.isClosed());
        }
    }

    @SuppressWarnings("resource")
    @Test
    void testCloseOnCompletion() throws SQLException {
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
    void testGeneratedKeyAlwaysReturned() throws SQLException {
        // Normal
        try (Connection conn = getConnection()) {
            assertEquals(conn.getMetaData().generatedKeyAlwaysReturned(), Jdbc41Bridge.generatedKeyAlwaysReturned(conn.getMetaData()));
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
    void testGetNetworkTimeout() throws SQLException {
        // Normal
        try (Connection conn = getConnection()) {
            if (database == TestDatabase.HSQLDB) {
                assertThrows(SQLFeatureNotSupportedException.class, () -> Jdbc41Bridge.setNetworkTimeout(conn, Runnable::run, 30_000));
            } else {
                Jdbc41Bridge.setNetworkTimeout(conn, Runnable::run, 30_000);
            }
            // Check the behavior reported by the selected driver.
            assertEquals(conn.getNetworkTimeout(), Jdbc41Bridge.getNetworkTimeout(conn));
        }
    }

    /**
     * A typed ResultSet lookup.
     */
    @FunctionalInterface
    private interface JdbcLookup {
        Object get() throws SQLException;
    }

    /**
     * Checks typed values and driver-specific conversion exceptions against the JDBC implementation.
     */
    private static void assertLookup(final JdbcLookup physical, final JdbcLookup bridge) throws SQLException {
        final Object expected;
        try {
            expected = physical.get();
        } catch (final SQLException e) {
            final SQLException actual = assertThrowsExactly(e.getClass(), bridge::get);
            assertEquals(e.getSQLState(), actual.getSQLState());
            return;
        }
        assertEquals(expected, bridge.get());
    }

    /**
     * Finds a metadata type whose JDBC code also fits in a byte.
     */
    private static void findType(final ResultSet rs) throws SQLException {
        while (rs.next()) {
            final int type = rs.getInt("DATA_TYPE");
            if (type >= Byte.MIN_VALUE && type <= Byte.MAX_VALUE) {
                return;
            }
        }
        org.junit.jupiter.api.Assertions.fail("No JDBC type with a byte-sized code");
    }

    @Test
    void testGetObjectIndex() throws SQLException {
        try (Connection conn = getConnection(); ResultSet rs = conn.getMetaData().getTypeInfo()) {
            findType(rs);
            assertLookup(() -> rs.getObject(1, String.class), () -> Jdbc41Bridge.getObject(rs, 1, String.class));
            for (Class<?> type : new Class<?>[] {Integer.class, Long.class, Double.class, Float.class, Byte.class, BigDecimal.class}) {
                assertLookup(() -> rs.getObject(2, type), () -> Jdbc41Bridge.getObject(rs, 2, type));
            }
            assertLookup(() -> rs.getObject(7, Short.class), () -> Jdbc41Bridge.getObject(rs, 7, Short.class));
            assertLookup(() -> rs.getObject(8, Boolean.class), () -> Jdbc41Bridge.getObject(rs, 8, Boolean.class));
        }
    }

    @Test
    void testGetObjectName() throws SQLException {
        try (Connection conn = getConnection(); ResultSet rs = conn.getMetaData().getTypeInfo()) {
            findType(rs);
            assertLookup(() -> rs.getObject("TYPE_NAME", String.class), () -> Jdbc41Bridge.getObject(rs, "TYPE_NAME", String.class));
            for (Class<?> type : new Class<?>[] {Integer.class, Long.class, Double.class, Float.class, Byte.class, BigDecimal.class}) {
                assertLookup(() -> rs.getObject("DATA_TYPE", type), () -> Jdbc41Bridge.getObject(rs, "DATA_TYPE", type));
            }
            assertLookup(() -> rs.getObject("NULLABLE", Short.class), () -> Jdbc41Bridge.getObject(rs, "NULLABLE", Short.class));
            assertLookup(() -> rs.getObject("CASE_SENSITIVE", Boolean.class), () -> Jdbc41Bridge.getObject(rs, "CASE_SENSITIVE", Boolean.class));
        }
    }

    @Test
    void testGetParentLogger() throws SQLException {
        // Normal
        try (Connection conn = getConnection();
                Statement stmt = conn.createStatement()) {
            // returns null for H2 (not supported).
            if (database == TestDatabase.HSQLDB) {
                assertThrows(SQLFeatureNotSupportedException.class, () -> Jdbc41Bridge.getParentLogger(database.createDataSource(database.newUrl())));
            } else {
                final javax.sql.DataSource source = database.createDataSource(database.newUrl());
                assertEquals(source.getParentLogger(), Jdbc41Bridge.getParentLogger(source));
            }
        }
    }

    @SuppressWarnings("resource")
    @Test
    void testGetSchema() throws SQLException {
        // Normal
        try (Connection conn = getConnection()) {
            assertEquals(conn.getSchema(), Jdbc41Bridge.getSchema(conn));
            final Connection spy = Mockito.spy(conn);
            Mockito.when(spy.getSchema()).thenThrow(AbstractMethodError.class);
            assertNull(Jdbc41Bridge.getSchema(spy));
        }
    }

    @Test
    void testIsCloseOnCompletion() throws SQLException {
        // Normal
        try (Connection conn = getConnection();
                Statement stmt = conn.createStatement()) {
            assertFalse(Jdbc41Bridge.isCloseOnCompletion(stmt));
        }
    }

    @Test
    void testSetNetworkTimeout() throws SQLException {
        // Normal
        try (Connection conn = getConnection();
                Statement stmt = conn.createStatement()) {
            // Check the behavior reported by the selected driver.
            if (database == TestDatabase.HSQLDB) {
                assertThrows(SQLFeatureNotSupportedException.class, () -> Jdbc41Bridge.setNetworkTimeout(conn, Runnable::run, 30_000));
            } else {
                Jdbc41Bridge.setNetworkTimeout(conn, Runnable::run, 30_000);
            }
            assertEquals(conn.getNetworkTimeout(), Jdbc41Bridge.getNetworkTimeout(conn));
        }
    }

    @Test
    void testSetSchema() throws SQLException {
        // Normal
        try (Connection conn = getConnection();
                Statement stmt = conn.createStatement()) {
            Jdbc41Bridge.setSchema(conn, Jdbc41Bridge.getSchema(conn));
            final String expected = conn.getSchema();
            Jdbc41Bridge.setSchema(conn, expected);
            assertEquals(expected, Jdbc41Bridge.getSchema(conn));
        }
    }
}
