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

package org.apache.commons.dbcp2.datasources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for PoolKey.
 */
public class TestPoolKey {

    private PoolKey poolKey;
    private PoolKey anotherPoolKey;

    @BeforeEach
    public void setUp() {
        poolKey = new PoolKey("ds", "user");
        anotherPoolKey = new PoolKey(null, null);
    }

    @Test
    void testEquals() {
        assertEquals(poolKey, poolKey);
        assertNotEquals(poolKey, null);
        assertNotEquals(poolKey, new Object());
        assertNotEquals(new PoolKey(null, "user"), poolKey);
        assertEquals(new PoolKey(null, "user"), new PoolKey(null, "user"));
        assertNotEquals(new PoolKey(null, "user"), new PoolKey(null, "foo"));
        assertNotEquals(new PoolKey("ds", null), new PoolKey("foo", null));
        assertNotEquals(new PoolKey("ds", null), poolKey);
        assertEquals(new PoolKey("ds", null), new PoolKey("ds", null));
    }

    @Test
    void testHashcode() {
        assertEquals(poolKey.hashCode(), new PoolKey("ds", "user").hashCode());
        assertNotEquals(poolKey.hashCode(), anotherPoolKey.hashCode());
    }

    @Test
    void testToString() {
        assertEquals(poolKey.toString(), new PoolKey("ds", "user").toString());
        assertNotEquals(poolKey.toString(), anotherPoolKey.toString());
    }
}
