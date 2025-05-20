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
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests for ListException.
 */
public class TestListException {

    @Test
    public void testExceptionList() {
        final List<Throwable> exceptions = Arrays.asList(new NullPointerException(), new RuntimeException());
        final ListException list = new ListException("Internal Error", exceptions);
        assertEquals("Internal Error", list.getMessage());
        assertArrayEquals(exceptions.toArray(), list.getExceptionList().toArray());
    }

    @Test
    public void testNulls() {
        final ListException list = new ListException(null, null);
        assertNull(list.getMessage());
        assertNull(list.getExceptionList());
    }
}
