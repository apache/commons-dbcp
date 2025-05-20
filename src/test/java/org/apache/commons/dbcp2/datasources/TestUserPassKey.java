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
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.apache.commons.dbcp2.Utils;
import org.apache.commons.lang3.SerializationUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for UserPassKey.
 */
public class TestUserPassKey {

    private UserPassKey userPassKey;
    private UserPassKey anotherUserPassKey;

    @BeforeEach
    public void setUp() {
        userPassKey = new UserPassKey("user", "pass");
        anotherUserPassKey = new UserPassKey((String) null, "");
    }

    @Test
    public void testEquals() {
        assertEquals(new UserPassKey("user"), new UserPassKey("user", (char[]) null));
        assertEquals(userPassKey, userPassKey);
        assertNotEquals(userPassKey, null);
        assertNotEquals(userPassKey, new Object());
        assertNotEquals(new UserPassKey(null), userPassKey);
        assertEquals(new UserPassKey(null), new UserPassKey(null));
        assertNotEquals(new UserPassKey("user", "pass"), new UserPassKey("foo", "pass"));
    }

    @Test
    public void testGettersAndSetters() {
        assertEquals("user", userPassKey.getUserName());
        assertEquals("pass", userPassKey.getPassword());
        assertArrayEquals(Utils.toCharArray("pass"), userPassKey.getPasswordCharArray());
    }

    @Test
    public void testHashcode() {
        assertEquals(userPassKey.hashCode(), new UserPassKey("user", "pass").hashCode());
        assertNotEquals(userPassKey.hashCode(), anotherUserPassKey.hashCode());
    }

    @Test
    public void testSerialization() {
        assertEquals(userPassKey, SerializationUtils.roundtrip(userPassKey));
        assertEquals(anotherUserPassKey, SerializationUtils.roundtrip(anotherUserPassKey));
    }

    @Test
    public void testToString() {
        assertEquals(userPassKey.toString(), new UserPassKey("user", "pass").toString());
        assertNotEquals(userPassKey.toString(), anotherUserPassKey.toString());
    }
}
