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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;

import org.apache.commons.dbcp2.PoolingConnection.StatementType;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link PStmtKey}.
 */
public class TestPStmtKey {

    /**
     * Tests constructors with different catalog.
     */
    @Test
    void testCtorDifferentCatalog() {
        assertNotEquals(new PStmtKey("sql", "catalog1", "schema1"), new PStmtKey("sql", "catalog2", "schema1"));
        assertNotEquals(new PStmtKey("sql", "catalog1", "schema1", 0),
                new PStmtKey("sql", "catalog2", "schema1", 0));
        assertNotEquals(new PStmtKey("sql", "catalog1", "schema1", 0, 0),
                new PStmtKey("sql", "catalog2", "schema1", 0, 0));
        assertNotEquals(new PStmtKey("sql", "catalog1", "schema1", 0, 0, 0),
                new PStmtKey("sql", "catalog2", "schema1", 0, 0, 0));
        //
        assertNotEquals(new PStmtKey("sql", "catalog1", "schema1", 0, 0, 0, StatementType.CALLABLE_STATEMENT),
                new PStmtKey("sql", "catalog2", "schema1", 0, 0, 0, StatementType.CALLABLE_STATEMENT));
        assertNotEquals(new PStmtKey("sql", "catalog1", "schema1", 0, 0, 0, StatementType.PREPARED_STATEMENT),
                new PStmtKey("sql", "catalog2", "schema1", 0, 0, 0, StatementType.PREPARED_STATEMENT));
        //
        assertNotEquals(new PStmtKey("sql", "catalog1", "schema1", 0, 0, StatementType.CALLABLE_STATEMENT),
                new PStmtKey("sql", "catalog2", "schema1", 0, 0, StatementType.CALLABLE_STATEMENT));
        assertNotEquals(new PStmtKey("sql", "catalog1", "schema1", 0, 0, StatementType.PREPARED_STATEMENT),
                new PStmtKey("sql", "catalog2", "schema1", 0, 0, StatementType.PREPARED_STATEMENT));
        //
        assertNotEquals(new PStmtKey("sql", "catalog1", "schema1", (int[]) null),
                new PStmtKey("sql", "catalog2", "schema1", (int[]) null));
        assertNotEquals(new PStmtKey("sql", "catalog1", "schema1", new int[1]),
                new PStmtKey("sql", "catalog2", "schema1", new int[1]));
        assertNotEquals(new PStmtKey("sql", "catalog1", "schema1", (String[]) null),
                new PStmtKey("sql", "catalog2", "schema1", (String[]) null));
        assertNotEquals(new PStmtKey("sql", "catalog1", "schema1", new String[] {"A" }),
                new PStmtKey("sql", "catalog2", "schema1", new String[] {"A" }));
        assertNotEquals(new PStmtKey("sql", "catalog1", "schema1", StatementType.PREPARED_STATEMENT),
                new PStmtKey("sql", "catalog2", "schema1", StatementType.PREPARED_STATEMENT));
        assertNotEquals(
                new PStmtKey("sql", "catalog1", "schema1", StatementType.PREPARED_STATEMENT, Integer.MAX_VALUE),
                new PStmtKey("sql", "catalog2", "schema1", StatementType.PREPARED_STATEMENT, Integer.MAX_VALUE));
    }

    /**
     * Tests constructors with different schemas.
     */
    @Test
    void testCtorDifferentSchema() {
        assertNotEquals(new PStmtKey("sql", "catalog1", "schema1"), new PStmtKey("sql", "catalog1", "schema2"));
        assertNotEquals(new PStmtKey("sql", "catalog1", "schema1", 0),
                new PStmtKey("sql", "catalog1", "schema2", 0));
        assertNotEquals(new PStmtKey("sql", "catalog1", "schema1", 0, 0),
                new PStmtKey("sql", "catalog1", "schema2", 0, 0));
        assertNotEquals(new PStmtKey("sql", "catalog1", "schema1", 0, 0, 0),
                new PStmtKey("sql", "catalog1", "schema2", 0, 0, 0));
        //
        assertNotEquals(new PStmtKey("sql", "catalog1", "schema1", 0, 0, 0, StatementType.CALLABLE_STATEMENT),
                new PStmtKey("sql", "catalog1", "schema2", 0, 0, 0, StatementType.CALLABLE_STATEMENT));
        assertNotEquals(new PStmtKey("sql", "catalog1", "schema1", 0, 0, 0, StatementType.PREPARED_STATEMENT),
                new PStmtKey("sql", "catalog1", "schema2", 0, 0, 0, StatementType.PREPARED_STATEMENT));
        //
        assertNotEquals(new PStmtKey("sql", "catalog1", "schema1", 0, 0, StatementType.CALLABLE_STATEMENT),
                new PStmtKey("sql", "catalog1", "schema2", 0, 0, StatementType.CALLABLE_STATEMENT));
        assertNotEquals(new PStmtKey("sql", "catalog1", "schema1", 0, 0, StatementType.PREPARED_STATEMENT),
                new PStmtKey("sql", "catalog1", "schema2", 0, 0, StatementType.PREPARED_STATEMENT));
        //
        assertNotEquals(new PStmtKey("sql", "catalog1", "schema1", (int[]) null),
                new PStmtKey("sql", "catalog1", "schema2", (int[]) null));
        assertNotEquals(new PStmtKey("sql", "catalog1", "schema1", new int[1]),
                new PStmtKey("sql", "catalog1", "schema2", new int[1]));
        assertNotEquals(new PStmtKey("sql", "catalog1", "schema1", (String[]) null),
                new PStmtKey("sql", "catalog1", "schema2", (String[]) null));
        assertNotEquals(new PStmtKey("sql", "catalog1", "schema1", new String[] {"A" }),
                new PStmtKey("sql", "catalog1", "schema2", new String[] {"A" }));
        assertNotEquals(new PStmtKey("sql", "catalog1", "schema1", StatementType.PREPARED_STATEMENT),
                new PStmtKey("sql", "catalog1", "schema2", StatementType.PREPARED_STATEMENT));
        assertNotEquals(
                new PStmtKey("sql", "catalog1", "schema1", StatementType.PREPARED_STATEMENT, Integer.MAX_VALUE),
                new PStmtKey("sql", "catalog1", "schema2", StatementType.PREPARED_STATEMENT, Integer.MAX_VALUE));
    }

    /**
     * Tests constructors with different catalog.
     */
    @Test
    void testCtorEquals() {
        assertEquals(new PStmtKey("sql", "catalog1", "schema1"), new PStmtKey("sql", "catalog1", "schema1"));
        assertEquals(new PStmtKey("sql", "catalog1", "schema1", 0),
                new PStmtKey("sql", "catalog1", "schema1", 0));
        assertEquals(new PStmtKey("sql", "catalog1", "schema1", 0, 0),
                new PStmtKey("sql", "catalog1", "schema1", 0, 0));
        assertEquals(new PStmtKey("sql", "catalog1", "schema1", 0, 0, 0),
                new PStmtKey("sql", "catalog1", "schema1", 0, 0, 0));
        //
        assertEquals(new PStmtKey("sql", "catalog1", "schema1", 0, 0, 0, StatementType.CALLABLE_STATEMENT),
                new PStmtKey("sql", "catalog1", "schema1", 0, 0, 0, StatementType.CALLABLE_STATEMENT));
        assertEquals(new PStmtKey("sql", "catalog1", "schema1", 0, 0, 0, StatementType.PREPARED_STATEMENT),
                new PStmtKey("sql", "catalog1", "schema1", 0, 0, 0, StatementType.PREPARED_STATEMENT));
        //
        assertEquals(new PStmtKey("sql", "catalog1", "schema1", 0, 0, StatementType.CALLABLE_STATEMENT),
                new PStmtKey("sql", "catalog1", "schema1", 0, 0, StatementType.CALLABLE_STATEMENT));
        assertEquals(new PStmtKey("sql", "catalog1", "schema1", 0, 0, StatementType.PREPARED_STATEMENT),
                new PStmtKey("sql", "catalog1", "schema1", 0, 0, StatementType.PREPARED_STATEMENT));
        //
        assertEquals(new PStmtKey("sql", "catalog1", "schema1", (int[]) null),
                new PStmtKey("sql", "catalog1", "schema1", (int[]) null));
        assertEquals(new PStmtKey("sql", "catalog1", "schema1", new int[1]),
                new PStmtKey("sql", "catalog1", "schema1", new int[1]));
        assertEquals(new PStmtKey("sql", "catalog1", "schema1", (String[]) null),
                new PStmtKey("sql", "catalog1", "schema1", (String[]) null));
        assertEquals(new PStmtKey("sql", "catalog1", "schema1", new String[] {"A" }),
                new PStmtKey("sql", "catalog1", "schema1", new String[] {"A" }));
        assertEquals(new PStmtKey("sql", "catalog1", "schema1", StatementType.PREPARED_STATEMENT),
                new PStmtKey("sql", "catalog1", "schema1", StatementType.PREPARED_STATEMENT));
        assertEquals(
                new PStmtKey("sql", "catalog1", "schema1", StatementType.PREPARED_STATEMENT, Integer.MAX_VALUE),
                new PStmtKey("sql", "catalog1", "schema1", StatementType.PREPARED_STATEMENT, Integer.MAX_VALUE));
    }

    /**
     * Tests {@link org.apache.commons.dbcp2.PStmtKey#PStmtKey(String, String, String, int[])}.
     *
     * See https://issues.apache.org/jira/browse/DBCP-494
     */
    @Test
    void testCtorStringStringArrayOfInts() {
        final int[] input = {0, 0};
        final PStmtKey pStmtKey = new PStmtKey("", "", "", input);
        assertArrayEquals(input, pStmtKey.getColumnIndexes());
        input[0] = 1;
        input[1] = 1;
        assertFalse(Arrays.equals(input, pStmtKey.getColumnIndexes()));
    }

    /**
     * Tests {@link org.apache.commons.dbcp2.PStmtKey#PStmtKey(String, String, String, int[])}.
     *
     * See https://issues.apache.org/jira/browse/DBCP-494
     */
    @Test
    void testCtorStringStringArrayOfNullInts() {
        final int[] input = null;
        final PStmtKey pStmtKey = new PStmtKey("", "", "", input);
        assertArrayEquals(input, pStmtKey.getColumnIndexes());
    }

    /**
     * Tests {@link org.apache.commons.dbcp2.PStmtKey#PStmtKey(String, String, String, String[])}.
     *
     * See https://issues.apache.org/jira/browse/DBCP-494
     */
    @Test
    void testCtorStringStringArrayOfNullStrings() {
        final String[] input = null;
        final PStmtKey pStmtKey = new PStmtKey("", "", "", input);
        assertArrayEquals(input, pStmtKey.getColumnNames());
    }

    /**
     * Tests {@link org.apache.commons.dbcp2.PStmtKey#PStmtKey(String, String, String, String[])}.
     *
     * See https://issues.apache.org/jira/browse/DBCP-494
     */
    @Test
    void testCtorStringStringArrayOfStrings() {
        final String[] input = {"A", "B"};
        final PStmtKey pStmtKey = new PStmtKey("", "", "", input);
        assertArrayEquals(input, pStmtKey.getColumnNames());
        input[0] = "C";
        input[1] = "D";
        assertFalse(Arrays.equals(input, pStmtKey.getColumnNames()));
    }

    @Test
    void testEquals() {
        final PStmtKey pStmtKey = new PStmtKey("SELECT 1", "catalog", "public",
                java.sql.ResultSet.TYPE_SCROLL_INSENSITIVE, java.sql.ResultSet.CONCUR_READ_ONLY,
                StatementType.CALLABLE_STATEMENT);
        assertEquals(pStmtKey, pStmtKey);
        assertNotEquals(null, pStmtKey);
        assertNotEquals(pStmtKey, new Object());
        assertNotEquals(pStmtKey, new PStmtKey("SELECT 2", "catalog", "public",
                ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY,
                StatementType.CALLABLE_STATEMENT));
        assertNotEquals(pStmtKey, new PStmtKey("SELECT 1", "anothercatalog", "public",
                ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY,
                StatementType.CALLABLE_STATEMENT));
        assertNotEquals(pStmtKey, new PStmtKey("SELECT 1", "catalog", "private",
                ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY,
                StatementType.CALLABLE_STATEMENT));
        assertNotEquals(pStmtKey, new PStmtKey("SELECT 1", "catalog", "public",
                ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY,
                StatementType.CALLABLE_STATEMENT));
        assertNotEquals(pStmtKey, new PStmtKey("SELECT 1", "catalog", "public",
                ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE,
                StatementType.CALLABLE_STATEMENT));
        assertNotEquals(pStmtKey, new PStmtKey("SELECT 1", "catalog", "public",
                ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY,
                StatementType.PREPARED_STATEMENT));
        assertEquals(pStmtKey, new PStmtKey("SELECT 1", "catalog", "public",
                ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY,
                StatementType.CALLABLE_STATEMENT));
        assertEquals(pStmtKey.hashCode(), new PStmtKey("SELECT 1", "catalog", "public",
                java.sql.ResultSet.TYPE_SCROLL_INSENSITIVE, java.sql.ResultSet.CONCUR_READ_ONLY,
                StatementType.CALLABLE_STATEMENT).hashCode());
    }

    @Test
    void testGettersSetters() {
        final PStmtKey pStmtKey = new PStmtKey("SELECT 1", "catalog", "public");
        assertEquals("SELECT 1", pStmtKey.getSql());
        assertEquals("public", pStmtKey.getSchema());
        assertEquals("catalog", pStmtKey.getCatalog());
        assertNull(pStmtKey.getAutoGeneratedKeys());
        assertNull(pStmtKey.getResultSetConcurrency());
        assertNull(pStmtKey.getResultSetHoldability());
        assertNull(pStmtKey.getResultSetType());
        assertEquals(StatementType.PREPARED_STATEMENT, pStmtKey.getStmtType());
    }

    @Test
    void testToString() {
        final PStmtKey pStmtKey = new PStmtKey("SELECT 1", "catalog", "public",
                StatementType.CALLABLE_STATEMENT, Statement.RETURN_GENERATED_KEYS);
        assertTrue(pStmtKey.toString().contains("sql=SELECT 1"));
        assertTrue(pStmtKey.toString().contains("schema=public"));
        assertTrue(pStmtKey.toString().contains("autoGeneratedKeys=1"));
        assertTrue(pStmtKey.toString().contains("statementType=CALLABLE_STATEMENT"));
    }
}
