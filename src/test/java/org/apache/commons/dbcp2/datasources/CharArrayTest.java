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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * Tests {@link CharArray}.
 */
public class CharArrayTest {

    @Test
    public void testAsString() {
        assertEquals("foo", new CharArray("foo").asString());
    }

    @Test
    public void testEquals() {
        assertEquals(new CharArray("foo"), new CharArray("foo"));
        assertNotEquals(new CharArray("foo"), new CharArray("bar"));
    }

    @Test
    public void testGet() {
        assertArrayEquals("foo".toCharArray(), new CharArray("foo").get());
    }

    @Test
    public void testClear() {
        assertNull(new CharArray((String) null).clear().get());
        assertArrayEquals("".toCharArray(), new CharArray("").clear().get());
        assertArrayEquals("\0\0\0".toCharArray(), new CharArray("foo").clear().get());
    }

    @Test
    public void testHashCode() {
        assertEquals(new CharArray("foo").hashCode(), new CharArray("foo").hashCode());
        assertNotEquals(new CharArray("foo").hashCode(), new CharArray("bar").hashCode());
    }

    @Test
    public void testToString() {
        assertFalse(new CharArray("foo").toString().contains("foo"));
    }
}
