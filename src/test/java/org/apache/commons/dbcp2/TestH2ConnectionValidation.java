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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests connection validation after H2 terminates an idle database session.
 */
@Timeout(30)
class TestH2ConnectionValidation {

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testH2ServerTermination(final boolean evict) throws Throwable {
        final String url = "jdbc:h2:mem:" + UUID.randomUUID();
        try (BasicDataSource source = new BasicDataSource(); Connection admin = DriverManager.getConnection(url)) {
            source.setUrl(url);
            source.setMaxTotal(1);
            TestConnectionValidation.assertDeadIdleConnections(source, evict, physical -> {
                final int session;
                try (Statement statement = physical.createStatement(); ResultSet result = statement.executeQuery("SELECT SESSION_ID()")) {
                    assertTrue(result.next());
                    session = result.getInt(1);
                }
                try (Statement statement = admin.createStatement(); ResultSet result = statement.executeQuery("CALL ABORT_SESSION(" + session + ")")) {
                    assertTrue(result.next());
                    assertTrue(result.getBoolean(1));
                }
            });
        }
    }
}
