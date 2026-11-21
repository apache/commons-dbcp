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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class TestUtils {

    public static PStmtKey getPStmtKey(final PoolablePreparedStatement<PStmtKey> poolablePreparedStatement) {
        return poolablePreparedStatement.getKey();
    }

    @Test
    void testCheckForConflictsBothCollectionsNull() {
        assertDoesNotThrow(() -> Utils.checkSqlCodes(null, null));
    }

    @Test
    void testCheckForConflictsEmptyCollections() {
        final Collection<String> codes1 = Collections.emptySet();
        final Collection<String> codes2 = Collections.emptySet();
        assertDoesNotThrow(() -> Utils.checkSqlCodes(codes1, codes2));
    }

    @Test
    void testCheckForConflictsFirstCollectionNull() {
        final Collection<String> codes1 = null;
        final Collection<String> codes2 = new HashSet<>(Arrays.asList("08005", "08007"));
        assertDoesNotThrow(() -> Utils.checkSqlCodes(codes1, codes2));
    }

    @Test
    void testCheckForConflictsNoOverlap() {
        final Collection<String> codes1 = new HashSet<>(Arrays.asList("08003", "08006"));
        final Collection<String> codes2 = new HashSet<>(Arrays.asList("08005", "08007"));
        assertDoesNotThrow(() -> Utils.checkSqlCodes(codes1, codes2));
    }

    @Test
    void testCheckForConflictsSecondCollectionNull() {
        final Collection<String> codes1 = new HashSet<>(Arrays.asList("08003", "08006"));
        final Collection<String> codes2 = null;
        assertDoesNotThrow(() -> Utils.checkSqlCodes(codes1, codes2));
    }

    @Test
    void testCheckForConflictsWith1Overlap() {
        final Collection<String> codes1 = new HashSet<>(Arrays.asList("08003", "08006"));
        final Collection<String> codes2 = new HashSet<>(Arrays.asList("08005", "08006"));
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> Utils.checkSqlCodes(codes1, codes2));
        assertEquals("[08006] cannot be in both disconnectionSqlCodes and disconnectionIgnoreSqlCodes.", exception.getMessage());
    }

    @Test
    void testCheckForConflictsWith2Overlap() {
        final Collection<String> codes1 = new HashSet<>(Arrays.asList("08003", "08006", "08007"));
        final Collection<String> codes2 = new HashSet<>(Arrays.asList("08005", "08006", "08007"));
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> Utils.checkSqlCodes(codes1, codes2));
        assertEquals("[08006, 08007] cannot be in both disconnectionSqlCodes and disconnectionIgnoreSqlCodes.", exception.getMessage());
    }

    @Test
    void testClassLoads() {
        Utils.closeQuietly((AutoCloseable) null);
    }

    @ParameterizedTest
    @ValueSource(strings = {"matches", "next", "onCycle"})
    void testFindInChainCheckedException(final String callback) {
        final Object start = new Object();
        final SQLException failure = new SQLException("Callback failed", "HY000", 42);
        final AtomicInteger visits = new AtomicInteger();
        final SQLException actual = assertThrows(SQLException.class, () -> Utils.findInChain(start, current -> {
            assertEquals(1, visits.incrementAndGet(), "Must detect the cycle before revisiting its first object");
            if ("matches".equals(callback)) {
                throw failure;
            }
            return false;
        }, current -> {
            assertFalse("matches".equals(callback), "Must not advance after the predicate fails");
            if ("next".equals(callback)) {
                throw failure;
            }
            return start;
        }, current -> {
            assertEquals("onCycle", callback);
            assertSame(start, current);
            throw failure;
        }));
        assertSame(failure, actual);
    }

    @Test
    void testFindInChainCycleHandler() {
        final Object start = new Object();
        final Object cycle = new Object();
        final Object fallback = new Object();
        final AtomicInteger matchesCalls = new AtomicInteger();
        final AtomicInteger nextCalls = new AtomicInteger();
        final AtomicInteger cycleCalls = new AtomicInteger();
        assertSame(fallback, Utils.findInChain(start, current -> {
            assertTrue(matchesCalls.incrementAndGet() <= 2, "Must detect the cycle before revisiting an object");
            return false;
        }, current -> {
            nextCalls.incrementAndGet();
            return cycle;
        }, current -> {
            cycleCalls.incrementAndGet();
            assertSame(cycle, current);
            return fallback;
        }));
        assertEquals(2, matchesCalls.get(), "Each distinct object is inspected once");
        assertEquals(2, nextCalls.get(), "The repeated object is not traversed again");
        assertEquals(1, cycleCalls.get());
    }

    @Test
    void testFindInChainNullStart() throws SQLException {
        final Predicate<Object> matches = current -> {
            throw new AssertionError("Must not inspect an empty chain");
        };
        final Function<Object, Object> next = current -> {
            throw new AssertionError("Must not advance an empty chain");
        };
        assertNull(Utils.findInChain(null, matches, next));
        assertNull(Utils.<Object, SQLException>findInChain(null, matches::test, next::apply, current -> {
            throw new AssertionError("An empty chain has no cycle");
        }));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2})
    void testFindInChainStopsAtFirstMatch(final int match) throws SQLException {
        final Predicate<Integer> matches = current -> current == match;
        final Function<Integer, Integer> next = current -> {
            assertTrue(current < match, "Must not follow a matched object's next link");
            return current + 1;
        };
        assertEquals(Integer.valueOf(match), Utils.findInChain(0, matches, next));
        assertEquals(Integer.valueOf(match), Utils.<Integer, SQLException>findInChain(0, matches::test, next::apply, current -> {
            throw new AssertionError("Must stop at a match before checking later links for cycles");
        }));
    }

    @Test
    void testIsDisconnectionSqlCode() {
        assertTrue(Utils.isDisconnectionSqlCode("57P01"), "57P01 should be recognized as a disconnection SQL code.");
        assertTrue(Utils.isDisconnectionSqlCode("01002"), "01002 should be recognized as a disconnection SQL code.");
        assertTrue(Utils.isDisconnectionSqlCode("JZ0C0"), "JZ0C0 should be recognized as a disconnection SQL code.");

        assertFalse(Utils.isDisconnectionSqlCode("INVALID"), "INVALID should not be recognized as a disconnection SQL code.");
        assertFalse(Utils.isDisconnectionSqlCode("00000"), "00000 should not be recognized as a disconnection SQL code.");
    }
}
