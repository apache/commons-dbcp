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

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.support.ParameterDeclarations;

/**
 * Selects databases independently for Surefire and Failsafe.
 */
class DatabaseArgumentsProvider implements ArgumentsProvider {

    /**
     * Selects databases, rejecting invalid modes and server selections.
     */
    static Stream<TestDatabase> select(final String mode, final String selection) {
        if ("embedded".equals(mode)) {
            return Stream.of(TestDatabase.H2, TestDatabase.HSQLDB);
        }
        if (!"docker".equals(mode)) {
            throw new IllegalArgumentException("Unknown dbcp.test.mode: " + mode);
        }
        if (selection == null) {
            throw new IllegalArgumentException("Missing dbcp.test.databases; run mvn -Pdocker-it verify");
        }
        return Arrays.stream(selection.split(",", -1)).map(String::trim)
                .map(name -> TestDatabase.valueOf(name.toUpperCase(Locale.ROOT))).peek(database -> {
                    if (database == TestDatabase.H2 || database == TestDatabase.HSQLDB) {
                        throw new IllegalArgumentException("Not a Docker database: " + database);
                    }
                }).distinct();
    }

    @Override
    public Stream<? extends Arguments> provideArguments(final ParameterDeclarations parameters, final ExtensionContext context) {
        return select(context.getConfigurationParameter("dbcp.test.mode").orElse("embedded"),
                System.getProperty("dbcp.test.databases")).map(Arguments::of);
    }
}
