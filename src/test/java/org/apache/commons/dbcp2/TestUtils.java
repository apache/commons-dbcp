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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

public class TestUtils {

    public static PStmtKey getPStmtKey(final PoolablePreparedStatement<PStmtKey> poolablePreparedStatement) {
        return poolablePreparedStatement.getKey();
    }

    @Test
    public void testCheckForConflictsBothCollectionsNull() {
        assertDoesNotThrow(() -> Utils.checkSqlCodes(null, null));
    }

    @Test
    public void testCheckForConflictsEmptyCollections() {
        final Collection<String> codes1 = Collections.emptySet();
        final Collection<String> codes2 = Collections.emptySet();
        assertDoesNotThrow(() -> Utils.checkSqlCodes(codes1, codes2));
    }

    @Test
    public void testCheckForConflictsFirstCollectionNull() {
        final Collection<String> codes1 = null;
        final Collection<String> codes2 = new HashSet<>(Arrays.asList("08005", "08007"));
        assertDoesNotThrow(() -> Utils.checkSqlCodes(codes1, codes2));
    }

    @Test
    public void testCheckForConflictsNoOverlap() {
        final Collection<String> codes1 = new HashSet<>(Arrays.asList("08003", "08006"));
        final Collection<String> codes2 = new HashSet<>(Arrays.asList("08005", "08007"));
        assertDoesNotThrow(() -> Utils.checkSqlCodes(codes1, codes2));
    }

    @Test
    public void testCheckForConflictsSecondCollectionNull() {
        final Collection<String> codes1 = new HashSet<>(Arrays.asList("08003", "08006"));
        final Collection<String> codes2 = null;
        assertDoesNotThrow(() -> Utils.checkSqlCodes(codes1, codes2));
    }

    @Test
    public void testCheckForConflictsWith1Overlap() {
        final Collection<String> codes1 = new HashSet<>(Arrays.asList("08003", "08006"));
        final Collection<String> codes2 = new HashSet<>(Arrays.asList("08005", "08006"));
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> Utils.checkSqlCodes(codes1, codes2));
        assertEquals("[08006] cannot be in both disconnectionSqlCodes and disconnectionIgnoreSqlCodes.", exception.getMessage());
    }

    @Test
    public void testCheckForConflictsWith2Overlap() {
        final Collection<String> codes1 = new HashSet<>(Arrays.asList("08003", "08006", "08007"));
        final Collection<String> codes2 = new HashSet<>(Arrays.asList("08005", "08006", "08007"));
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> Utils.checkSqlCodes(codes1, codes2));
        assertEquals("[08006, 08007] cannot be in both disconnectionSqlCodes and disconnectionIgnoreSqlCodes.", exception.getMessage());
    }

    @Test
    public void testClassLoads() {
        Utils.closeQuietly((AutoCloseable) null);
    }

    @Test
    public void testIsDisconnectionSqlCode() {
        assertTrue(Utils.isDisconnectionSqlCode("57P01"), "57P01 should be recognised as a disconnection SQL code.");
        assertTrue(Utils.isDisconnectionSqlCode("01002"), "01002 should be recognised as a disconnection SQL code.");
        assertTrue(Utils.isDisconnectionSqlCode("JZ0C0"), "JZ0C0 should be recognised as a disconnection SQL code.");

        assertFalse(Utils.isDisconnectionSqlCode("INVALID"), "INVALID should not be recognised as a disconnection SQL code.");
        assertFalse(Utils.isDisconnectionSqlCode("00000"), "00000 should not be recognised as a disconnection SQL code.");
    }
}
