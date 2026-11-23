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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.ref.WeakReference;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests {@link AbandonedTrace}.
 */
public class TestAbandonedTrace {

    @Test
    void testDeprecated() throws SQLException {
        try (AbandonedTrace trace = new AbandonedTrace()) {
            assertEquals(trace.getLastUsedInstant().toEpochMilli(), trace.getLastUsed());
        }
    }

    /**
     * Tests that a null or absent removal target discards every cleared reference while retaining live children.
     *
     * @param nullTarget whether to remove null instead of an untracked child.
     * @throws Exception Thrown if accessing the trace list or closing a trace fails.
     */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testRemoveTraceCleansClearedReferences(final boolean nullTarget) throws Exception {
        try (final AbandonedTrace parent = new AbandonedTrace();
                final AbandonedTrace child = new AbandonedTrace(parent);
                final AbandonedTrace absent = new AbandonedTrace()) {
            parent.addTrace(child);
            parent.addTrace(child);
            final List<?> references = (List<?>) TesterUtils.getField(parent, "traceList");
            ((WeakReference<?>) references.get(0)).clear();
            ((WeakReference<?>) references.get(2)).clear();

            parent.removeTrace(nullTarget ? null : absent);

            assertEquals(1, references.size());
            assertEquals(Collections.singletonList(child), parent.getTrace());
        }
    }

    /**
     * Tests that removal stops after the first matching child and cleans up only cleared references encountered before it.
     *
     * @throws Exception Thrown if accessing the trace list or closing a trace fails.
     */
    @Test
    void testRemoveTraceStopsAtFirstMatch() throws Exception {
        try (final AbandonedTrace parent = new AbandonedTrace();
                final AbandonedTrace child = new AbandonedTrace();
                final AbandonedTrace other = new AbandonedTrace()) {
            parent.addTrace(other);
            parent.addTrace(other);
            parent.addTrace(child);
            parent.addTrace(child);
            parent.addTrace(other);
            final List<?> references = (List<?>) TesterUtils.getField(parent, "traceList");
            ((WeakReference<?>) references.get(0)).clear();
            ((WeakReference<?>) references.get(4)).clear();

            parent.removeTrace(child);

            assertEquals(3, references.size());
            assertSame(other, ((WeakReference<?>) references.get(0)).get());
            assertSame(child, ((WeakReference<?>) references.get(1)).get());
            assertNull(((WeakReference<?>) references.get(2)).get());
        }
    }
}
