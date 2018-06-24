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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.Statement;
import java.util.Arrays;

import javax.resource.cci.ResultSet;

import org.apache.commons.dbcp2.PoolingConnection.StatementType;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests {@link PStmtKey}.
 *
 * @since 2.4.0
 */
public class TestPStmtKey {

    /**
     * Tests constructors with different schemas.
     */
    @Test
    public void testCtorDifferentSchema() {
        Assert.assertNotEquals(new PStmtKey("sql", "catalog1", "schema1"), new PStmtKey("sql", "catalog1", "schema2"));
        Assert.assertNotEquals(new PStmtKey("sql", "catalog1", "schema1", 0),
                new PStmtKey("sql", "catalog1", "schema2", 0));
        Assert.assertNotEquals(new PStmtKey("sql", "catalog1", "schema1", 0, 0),
                new PStmtKey("sql", "catalog1", "schema2", 0, 0));
        Assert.assertNotEquals(new PStmtKey("sql", "catalog1", "schema1", 0, 0, 0),
                new PStmtKey("sql", "catalog1", "schema2", 0, 0, 0));
        //
        Assert.assertNotEquals(new PStmtKey("sql", "catalog1", "schema1", 0, 0, 0, null),
                new PStmtKey("sql", "catalog1", "schema2", 0, 0, 0, null));
        Assert.assertNotEquals(new PStmtKey("sql", "catalog1", "schema1", 0, 0, 0, StatementType.PREPARED_STATEMENT),
                new PStmtKey("sql", "catalog1", "schema2", 0, 0, 0, StatementType.PREPARED_STATEMENT));
        //
        Assert.assertNotEquals(new PStmtKey("sql", "catalog1", "schema1", 0, 0, null),
                new PStmtKey("sql", "catalog1", "schema2", 0, 0, null));
        Assert.assertNotEquals(new PStmtKey("sql", "catalog1", "schema1", 0, 0, StatementType.PREPARED_STATEMENT),
                new PStmtKey("sql", "catalog1", "schema2", 0, 0, StatementType.PREPARED_STATEMENT));
        //
        Assert.assertNotEquals(new PStmtKey("sql", "catalog1", "schema1", (int[]) null),
                new PStmtKey("sql", "catalog1", "schema2", (int[]) null));
        Assert.assertNotEquals(new PStmtKey("sql", "catalog1", "schema1", new int[1]),
                new PStmtKey("sql", "catalog1", "schema2", new int[1]));
        Assert.assertNotEquals(new PStmtKey("sql", "catalog1", "schema1", (String[]) null),
                new PStmtKey("sql", "catalog1", "schema2", (String[]) null));
        Assert.assertNotEquals(new PStmtKey("sql", "catalog1", "schema1", new String[] {"A" }),
                new PStmtKey("sql", "catalog1", "schema2", new String[] {"A" }));
        Assert.assertNotEquals(new PStmtKey("sql", "catalog1", "schema1", StatementType.PREPARED_STATEMENT),
                new PStmtKey("sql", "catalog1", "schema2", StatementType.PREPARED_STATEMENT));
        Assert.assertNotEquals(
                new PStmtKey("sql", "catalog1", "schema1", StatementType.PREPARED_STATEMENT, Integer.MAX_VALUE),
                new PStmtKey("sql", "catalog1", "schema2", StatementType.PREPARED_STATEMENT, Integer.MAX_VALUE));
    }

    /**
     * Tests constructors with different catalog.
     */
    @Test
    public void testCtorDifferentCatalog() {
        Assert.assertNotEquals(new PStmtKey("sql", "catalog1", "schema1"), new PStmtKey("sql", "catalog2", "schema1"));
        Assert.assertNotEquals(new PStmtKey("sql", "catalog1", "schema1", 0),
                new PStmtKey("sql", "catalog2", "schema1", 0));
        Assert.assertNotEquals(new PStmtKey("sql", "catalog1", "schema1", 0, 0),
                new PStmtKey("sql", "catalog2", "schema1", 0, 0));
        Assert.assertNotEquals(new PStmtKey("sql", "catalog1", "schema1", 0, 0, 0),
                new PStmtKey("sql", "catalog2", "schema1", 0, 0, 0));
        //
        Assert.assertNotEquals(new PStmtKey("sql", "catalog1", "schema1", 0, 0, 0, null),
                new PStmtKey("sql", "catalog2", "schema1", 0, 0, 0, null));
        Assert.assertNotEquals(new PStmtKey("sql", "catalog1", "schema1", 0, 0, 0, StatementType.PREPARED_STATEMENT),
                new PStmtKey("sql", "catalog2", "schema1", 0, 0, 0, StatementType.PREPARED_STATEMENT));
        //
        Assert.assertNotEquals(new PStmtKey("sql", "catalog1", "schema1", 0, 0, null),
                new PStmtKey("sql", "catalog2", "schema1", 0, 0, null));
        Assert.assertNotEquals(new PStmtKey("sql", "catalog1", "schema1", 0, 0, StatementType.PREPARED_STATEMENT),
                new PStmtKey("sql", "catalog2", "schema1", 0, 0, StatementType.PREPARED_STATEMENT));
        //
        Assert.assertNotEquals(new PStmtKey("sql", "catalog1", "schema1", (int[]) null),
                new PStmtKey("sql", "catalog2", "schema1", (int[]) null));
        Assert.assertNotEquals(new PStmtKey("sql", "catalog1", "schema1", new int[1]),
                new PStmtKey("sql", "catalog2", "schema1", new int[1]));
        Assert.assertNotEquals(new PStmtKey("sql", "catalog1", "schema1", (String[]) null),
                new PStmtKey("sql", "catalog2", "schema1", (String[]) null));
        Assert.assertNotEquals(new PStmtKey("sql", "catalog1", "schema1", new String[] {"A" }),
                new PStmtKey("sql", "catalog2", "schema1", new String[] {"A" }));
        Assert.assertNotEquals(new PStmtKey("sql", "catalog1", "schema1", StatementType.PREPARED_STATEMENT),
                new PStmtKey("sql", "catalog2", "schema1", StatementType.PREPARED_STATEMENT));
        Assert.assertNotEquals(
                new PStmtKey("sql", "catalog1", "schema1", StatementType.PREPARED_STATEMENT, Integer.MAX_VALUE),
                new PStmtKey("sql", "catalog2", "schema1", StatementType.PREPARED_STATEMENT, Integer.MAX_VALUE));
    }

    /**
     * Tests constructors with different catalog.
     */
    @Test
    public void testCtorEquals() {
        Assert.assertEquals(new PStmtKey("sql", "catalog1", "schema1"), new PStmtKey("sql", "catalog1", "schema1"));
        Assert.assertEquals(new PStmtKey("sql", "catalog1", "schema1", 0),
                new PStmtKey("sql", "catalog1", "schema1", 0));
        Assert.assertEquals(new PStmtKey("sql", "catalog1", "schema1", 0, 0),
                new PStmtKey("sql", "catalog1", "schema1", 0, 0));
        Assert.assertEquals(new PStmtKey("sql", "catalog1", "schema1", 0, 0, 0),
                new PStmtKey("sql", "catalog1", "schema1", 0, 0, 0));
        //
        Assert.assertEquals(new PStmtKey("sql", "catalog1", "schema1", 0, 0, 0, null),
                new PStmtKey("sql", "catalog1", "schema1", 0, 0, 0, null));
        Assert.assertEquals(new PStmtKey("sql", "catalog1", "schema1", 0, 0, 0, StatementType.PREPARED_STATEMENT),
                new PStmtKey("sql", "catalog1", "schema1", 0, 0, 0, StatementType.PREPARED_STATEMENT));
        //
        Assert.assertEquals(new PStmtKey("sql", "catalog1", "schema1", 0, 0, null),
                new PStmtKey("sql", "catalog1", "schema1", 0, 0, null));
        Assert.assertEquals(new PStmtKey("sql", "catalog1", "schema1", 0, 0, StatementType.PREPARED_STATEMENT),
                new PStmtKey("sql", "catalog1", "schema1", 0, 0, StatementType.PREPARED_STATEMENT));
        //
        Assert.assertEquals(new PStmtKey("sql", "catalog1", "schema1", (int[]) null),
                new PStmtKey("sql", "catalog1", "schema1", (int[]) null));
        Assert.assertEquals(new PStmtKey("sql", "catalog1", "schema1", new int[1]),
                new PStmtKey("sql", "catalog1", "schema1", new int[1]));
        Assert.assertEquals(new PStmtKey("sql", "catalog1", "schema1", (String[]) null),
                new PStmtKey("sql", "catalog1", "schema1", (String[]) null));
        Assert.assertEquals(new PStmtKey("sql", "catalog1", "schema1", new String[] {"A" }),
                new PStmtKey("sql", "catalog1", "schema1", new String[] {"A" }));
        Assert.assertEquals(new PStmtKey("sql", "catalog1", "schema1", StatementType.PREPARED_STATEMENT),
                new PStmtKey("sql", "catalog1", "schema1", StatementType.PREPARED_STATEMENT));
        Assert.assertEquals(
                new PStmtKey("sql", "catalog1", "schema1", StatementType.PREPARED_STATEMENT, Integer.MAX_VALUE),
                new PStmtKey("sql", "catalog1", "schema1", StatementType.PREPARED_STATEMENT, Integer.MAX_VALUE));
    }

    /**
     * Tests {@link org.apache.commons.dbcp2.PStmtKey#PStmtKey(String, String, String, int[])}.
     *
     * See https://issues.apache.org/jira/browse/DBCP-494
     */
    @Test
    public void testCtorStringStringArrayOfInts() {
        final int[] input = {0, 0 };
        final PStmtKey pStmtKey = new PStmtKey("", "", "", input);
        Assert.assertArrayEquals(input, pStmtKey.getColumnIndexes());
        input[0] = 1;
        input[1] = 1;
        Assert.assertFalse(Arrays.equals(input, pStmtKey.getColumnIndexes()));
    }

    /**
     * Tests {@link org.apache.commons.dbcp2.PStmtKey#PStmtKey(String, String, String, int[])}.
     *
     * See https://issues.apache.org/jira/browse/DBCP-494
     */
    @Test
    public void testCtorStringStringArrayOfNullInts() {
        final int[] input = null;
        final PStmtKey pStmtKey = new PStmtKey("", "", "", input);
        Assert.assertArrayEquals(input, pStmtKey.getColumnIndexes());
    }

    /**
     * Tests {@link org.apache.commons.dbcp2.PStmtKey#PStmtKey(String, String, String, String[])}.
     *
     * See https://issues.apache.org/jira/browse/DBCP-494
     */
    @Test
    public void testCtorStringStringArrayOfNullStrings() {
        final String[] input = null;
        final PStmtKey pStmtKey = new PStmtKey("", "", "", input);
        Assert.assertArrayEquals(input, pStmtKey.getColumnNames());
    }

    /**
     * Tests {@link org.apache.commons.dbcp2.PStmtKey#PStmtKey(String, String, String, String[])}.
     *
     * See https://issues.apache.org/jira/browse/DBCP-494
     */
    @Test
    public void testCtorStringStringArrayOfStrings() {
        final String[] input = {"A", "B" };
        final PStmtKey pStmtKey = new PStmtKey("", "", "", input);
        Assert.assertArrayEquals(input, pStmtKey.getColumnNames());
        input[0] = "C";
        input[1] = "D";
        Assert.assertFalse(Arrays.equals(input, pStmtKey.getColumnNames()));
    }

    @Test
    public void testGettersSetters() {
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
    public void testEquals() {
        final PStmtKey pStmtKey = new PStmtKey("SELECT 1", "catalog", "public",
                ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY,
                StatementType.CALLABLE_STATEMENT);
        assertTrue(pStmtKey.equals(pStmtKey));
        assertFalse(pStmtKey.equals(null));
        assertFalse(pStmtKey.equals(new Object()));
        assertFalse(pStmtKey.equals(new PStmtKey("SELECT 2", "catalog", "public",
                ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY,
                StatementType.CALLABLE_STATEMENT)));
        assertFalse(pStmtKey.equals(new PStmtKey("SELECT 1", "anothercatalog", "public",
                ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY,
                StatementType.CALLABLE_STATEMENT)));
        assertFalse(pStmtKey.equals(new PStmtKey("SELECT 1", "catalog", "private",
                ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY,
                StatementType.CALLABLE_STATEMENT)));
        assertFalse(pStmtKey.equals(new PStmtKey("SELECT 1", "catalog", "public",
                ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY,
                StatementType.CALLABLE_STATEMENT)));
        assertFalse(pStmtKey.equals(new PStmtKey("SELECT 1", "catalog", "public",
                ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE,
                StatementType.CALLABLE_STATEMENT)));
        assertFalse(pStmtKey.equals(new PStmtKey("SELECT 1", "catalog", "public",
                ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY,
                StatementType.PREPARED_STATEMENT)));
        assertTrue(pStmtKey.equals(new PStmtKey("SELECT 1", "catalog", "public",
                ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY,
                StatementType.CALLABLE_STATEMENT)));
        assertEquals(pStmtKey.hashCode(), new PStmtKey("SELECT 1", "catalog", "public",
                ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY,
                StatementType.CALLABLE_STATEMENT).hashCode());
    }

    @Test
    public void testToString() {
        final PStmtKey pStmtKey = new PStmtKey("SELECT 1", "catalog", "public",
                StatementType.CALLABLE_STATEMENT, Statement.RETURN_GENERATED_KEYS);
        assertTrue(pStmtKey.toString().contains("sql=SELECT 1"));
        assertTrue(pStmtKey.toString().contains("schema=public"));
        assertTrue(pStmtKey.toString().contains("autoGeneratedKeys=1"));
        assertTrue(pStmtKey.toString().contains("statementType=CALLABLE_STATEMENT"));
    }
}
