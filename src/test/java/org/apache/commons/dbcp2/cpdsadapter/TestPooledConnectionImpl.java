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

package org.apache.commons.dbcp2.cpdsadapter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.stream.Stream;

import org.apache.commons.dbcp2.DelegatingCallableStatement;
import org.apache.commons.dbcp2.DelegatingConnection;
import org.apache.commons.dbcp2.DelegatingPreparedStatement;
import org.apache.commons.dbcp2.PStmtKey;
import org.apache.commons.dbcp2.TesterCallableStatement;
import org.apache.commons.dbcp2.TesterConnection;
import org.apache.commons.dbcp2.TesterDriver;
import org.apache.commons.dbcp2.TesterPreparedStatement;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Tests physical statement disposal with a guarded underlying connection. */
class TestPooledConnectionImpl {

    private static Stream<Arguments> statementArguments() {
        return Stream.of(false, true).flatMap(allowed -> Stream.of(false, true).map(callable -> Arguments.of(allowed, callable)));
    }

    @ParameterizedTest
    @MethodSource("statementArguments")
    void testDestroyGuardedPooledStatement(final boolean allowed, final boolean callable) throws Exception {
        try (TesterConnection physical = (TesterConnection) new TesterDriver().connect("jdbc:apache:commons:testdriver", null);
                TesterPreparedStatement raw = callable ? new TesterCallableStatement(physical, "SELECT 1") : new TesterPreparedStatement(physical);
                DelegatingConnection<Connection> guarded = new DelegatingConnection<Connection>(physical) {
                    @Override
                    protected boolean isAccessToUnderlyingConnectionAllowed() {
                        return allowed;
                    }

                    @Override
                    public CallableStatement prepareCall(final String sql) {
                        return new DelegatingCallableStatement(this, (CallableStatement) raw);
                    }

                    @Override
                    public PreparedStatement prepareStatement(final String sql) {
                        return new DelegatingPreparedStatement(this, raw);
                    }
                }) {
            final PooledConnectionImpl pooled = new PooledConnectionImpl(guarded);
            try (GenericKeyedObjectPool<PStmtKey, DelegatingPreparedStatement> pool = new GenericKeyedObjectPool<>(pooled)) {
                pooled.setStatementPool(pool);
                try {
                    try (Connection connection = pooled.getConnection()) {
                        (callable ? connection.prepareCall("SELECT 1") : connection.prepareStatement("SELECT 1")).close();
                        assertFalse(raw.isClosed());
                        pool.clear();
                        assertTrue(raw.isClosed());
                    }
                } finally {
                    pooled.close();
                }
            }
        }
    }
}
