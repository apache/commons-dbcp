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
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * Tests {@link UserPassKey}
 */
public class UserPassKeyTest {

    @Test
    void testClear() {
        // user name
        assertNull(new UserPassKey((String) null).clear().getUserName());
        assertEquals("", new UserPassKey("").clear().getUserName());
        assertEquals("\0\0\0", new UserPassKey("foo").clear().getUserName());
        // password String
        assertNull(new UserPassKey((String) null, (String) null).clear().getPassword());
        assertEquals("", new UserPassKey("", "").clear().getPassword());
        assertEquals("\0\0\0", new UserPassKey("foo", "bar").clear().getPassword());
        // password char[]
        assertNull(new UserPassKey((String) null, (char[]) null).clear().getPasswordCharArray());
        assertArrayEquals("".toCharArray(), new UserPassKey("", "").clear().getPasswordCharArray());
        assertArrayEquals("\0\0\0".toCharArray(), new UserPassKey("foo", "bar").clear().getPasswordCharArray());
    }
}
