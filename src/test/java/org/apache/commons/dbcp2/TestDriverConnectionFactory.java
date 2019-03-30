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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;

/**
 * Tests for DriverConnectionFactory.
 */
public class TestDriverConnectionFactory {

    @Test
    public void testDriverConnectionFactoryToString() {
        final DriverConnectionFactory cf = new DriverConnectionFactory(
                new TesterDriver(), "jdbc:apache:commons:testdriver", null);
        final String toString = cf.toString();
        assertTrue(toString.contains("jdbc:apache:commons:testdriver"));
    }

    @Test
    public void testCreateConnection() throws SQLException {
        final DriverConnectionFactory cf = new DriverConnectionFactory(
                new TesterDriver(), "jdbc:apache:commons:testdriver", null);
        final Connection conn = cf.createConnection();
        assertEquals(0, conn.getMetaData().getDriverMajorVersion());
    }
}
