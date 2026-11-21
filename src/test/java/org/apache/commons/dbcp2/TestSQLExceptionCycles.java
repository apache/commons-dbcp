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
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Tests cycle detection without losing fatal errors elsewhere in a linked exception chain. */
class TestSQLExceptionCycles {

    private static final class LinkedSQLException extends SQLException {
        private static final long serialVersionUID = 1L;
        private volatile SQLException next;

        private LinkedSQLException(final boolean fatal) {
            super("test", fatal ? "08006" : "42000");
        }

        @Override
        public boolean equals(final Object obj) {
            return obj instanceof LinkedSQLException;
        }

        @Override
        public SQLException getNextException() {
            return next;
        }

        @Override
        public int hashCode() {
            return 1;
        }
    }

    private static Stream<Arguments> chainArguments() {
        return Stream.of(0, 1, 8).flatMap(prefix -> Stream.of(1, 2, 3, 16).flatMap(length ->
                Stream.of(false, true).flatMap(cyclic -> Stream.of(-1, 0, prefix + length - 1)
                        .map(fatalIndex -> Arguments.of(prefix, length, cyclic, fatalIndex)))));
    }

    @ParameterizedTest(name = "prefix={0}, length={1}, cyclic={2}, fatal index={3}")
    @MethodSource("chainArguments")
    void testExceptionChain(final int prefix, final int length, final boolean cyclic, final int fatalIndex) throws SQLException {
        final List<LinkedSQLException> nodes = new ArrayList<>();
        for (int i = 0; i < prefix + length; i++) {
            nodes.add(new LinkedSQLException(i == fatalIndex));
        }
        for (int i = 0; i < nodes.size() - 1; i++) {
            nodes.get(i).next = nodes.get(i + 1);
        }
        if (cyclic) {
            nodes.get(nodes.size() - 1).next = nodes.get(prefix);
        }
        final PoolableConnection connection = new PoolableConnection(null, null, null);
        try {
            assertEquals(fatalIndex >= 0,
                    assertTimeoutPreemptively(Duration.ofSeconds(5), () -> connection.isFatalException(nodes.get(0))));
        } finally {
            nodes.forEach(node -> node.next = null);
            connection.reallyClose();
        }
    }

    @Test
    void testInteriorExceptionCycle() throws SQLException {
        testExceptionChain(1, 2, true, -1);
    }
}
