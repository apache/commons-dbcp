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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * Tests for Constants.
 */
public class TestConstants {

    @Test
    public void testConstants() {
        assertNotNull(new Constants());
        assertEquals(",connectionpool=", Constants.JMX_CONNECTION_POOL_BASE_EXT);
        assertEquals("connections", Constants.JMX_CONNECTION_POOL_PREFIX);
        assertEquals(",connectionpool=connections,connection=", Constants.JMX_CONNECTION_BASE_EXT);
        assertEquals(",connectionpool=connections,connection=", Constants.JMX_STATEMENT_POOL_BASE_EXT);
        assertEquals(",statementpool=statements", Constants.JMX_STATEMENT_POOL_PREFIX);
    }
}
