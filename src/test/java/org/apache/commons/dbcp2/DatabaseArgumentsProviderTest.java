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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests execution isolation and strict database selection.
 */
class DatabaseArgumentsProviderTest {

    @Test
    void testDockerSelection() {
        assertEquals(Arrays.asList(TestDatabase.POSTGRESQL, TestDatabase.MYSQL, TestDatabase.MARIADB, TestDatabase.ORACLE, TestDatabase.SQLSERVER),
                DatabaseArgumentsProvider.select("docker", " PostgreSQL,mysql,mariadb,oracle,SQLSERVER,mysql ").collect(Collectors.toList()));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"postgresql", "not-a-database"})
    void testEmbeddedSelectionIsIndependent(final String selection) {
        assertEquals(Arrays.asList(TestDatabase.H2, TestDatabase.HSQLDB),
                DatabaseArgumentsProvider.select("embedded", selection).collect(Collectors.toList()));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"h2", "hsqldb", "postgresql,", " ,mysql", "unknown", "mysql,h2"})
    void testRejectInvalidDockerSelection(final String selection) {
        assertThrows(IllegalArgumentException.class, () -> DatabaseArgumentsProvider.select("docker", selection).collect(Collectors.toList()));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"server", "EMBEDDED"})
    void testRejectUnknownMode(final String mode) {
        assertThrows(IllegalArgumentException.class, () -> DatabaseArgumentsProvider.select(mode, "mysql").collect(Collectors.toList()));
    }
}
