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

import java.sql.SQLException;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.BeforeParameterizedClassInvocation;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Parameterizes tests over the databases selected for this Maven execution.
 *
 * @since 2.15.0
 */
@ParameterizedClass(name = "{0}")
@ArgumentsSource(DatabaseArgumentsProvider.class)
@Tag("database")
public abstract class AbstractDatabaseTest {

    /**
     * Initializes server-side support before any database test executes.
     *
     * @param database the selected database.
     * @throws SQLException if initialization fails.
     * @since 2.15.0
     */
    @BeforeParameterizedClassInvocation
    @Timeout(60)
    protected static void initializeDatabase(final TestDatabase database) throws SQLException {
        database.initialize();
    }

    /**
     * Database used by this invocation.
     *
     * @since 2.15.0
     */
    @Parameter
    protected TestDatabase database;
}
