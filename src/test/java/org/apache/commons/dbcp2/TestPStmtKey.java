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

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests {@link PStmtKey}.
 *
 * @since 2.3.1
 */
public class TestPStmtKey {

    /**
     * Tests {@link org.apache.commons.dbcp2.PStmtKey#PStmtKey(String, String, int[])}.
     *
     * See https://issues.apache.org/jira/browse/DBCP-494
     */
    @Test
    public void testCtorStringStringArrayOfInts() {
        final int[] input = {0, 0 };
        final PStmtKey pStmtKey = new PStmtKey("", "", input);
        Assert.assertArrayEquals(input, pStmtKey.getColumnIndexes());
        input[0] = 1;
        input[1] = 1;
        Assert.assertFalse(Arrays.equals(input, pStmtKey.getColumnIndexes()));
    }

    /**
     * Tests {@link org.apache.commons.dbcp2.PStmtKey#PStmtKey(String, String, int[])}.
     *
     * See https://issues.apache.org/jira/browse/DBCP-494
     */
    @Test
    public void testCtorStringStringArrayOfNullInts() {
        final int[] input = null;
        final PStmtKey pStmtKey = new PStmtKey("", "", input);
        Assert.assertArrayEquals(input, pStmtKey.getColumnIndexes());
    }

    /**
     * Tests {@link org.apache.commons.dbcp2.PStmtKey#PStmtKey(String, String, String[])}.
     *
     * See https://issues.apache.org/jira/browse/DBCP-494
     */
    @Test
    public void testCtorStringStringArrayOfNullStrings() {
        final String[] input = null;
        final PStmtKey pStmtKey = new PStmtKey("", "", input);
        Assert.assertArrayEquals(input, pStmtKey.getColumnNames());
    }

    /**
     * Tests {@link org.apache.commons.dbcp2.PStmtKey#PStmtKey(String, String, String[])}.
     *
     * See https://issues.apache.org/jira/browse/DBCP-494
     */
    @Test
    public void testCtorStringStringArrayOfStrings() {
        final String[] input = {"A", "B" };
        final PStmtKey pStmtKey = new PStmtKey("", "", input);
        Assert.assertArrayEquals(input, pStmtKey.getColumnNames());
        input[0] = "C";
        input[1] = "D";
        Assert.assertFalse(Arrays.equals(input, pStmtKey.getColumnNames()));
    }
}
