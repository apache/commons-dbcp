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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.UUID;
import java.util.stream.Stream;

import javax.sql.PooledConnection;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.apache.commons.dbcp2.cpdsadapter.DriverAdapterCPDS;
import org.apache.commons.dbcp2.managed.BasicManagedDataSource;
import org.apache.geronimo.transaction.manager.TransactionManagerImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionManagerImple;
import com.arjuna.ats.jta.common.jtaPropertyManager;

/**
 * Shared connection-pool behavior exercised against every supported test database.
 */
@Timeout(30)
abstract class AbstractDatabaseConnectionTest {

    /** Forces the transaction manager to use two-phase commit with the real database resource. */
    private static final class AdditionalResource implements XAResource {
        /** Number of prepare calls. */
        private int prepares;
        /** Number of commit calls. */
        private int commits;

        @Override
        public void commit(final Xid xid, final boolean onePhase) {
            assertFalse(onePhase);
            commits++;
        }

        @Override
        public void end(final Xid xid, final int flags) {
        }

        @Override
        public void forget(final Xid xid) {
        }

        @Override
        public int getTransactionTimeout() {
            return 0;
        }

        @Override
        public boolean isSameRM(final XAResource other) {
            return other == this;
        }

        @Override
        public int prepare(final Xid xid) {
            prepares++;
            return XA_OK;
        }

        @Override
        public Xid[] recover(final int flags) {
            return new Xid[0];
        }

        @Override
        public void rollback(final Xid xid) {
        }

        @Override
        public boolean setTransactionTimeout(final int seconds) {
            return true;
        }

        @Override
        public void start(final Xid xid, final int flags) {
        }
    }

    /** Database selected by the concrete test class. */
    @Parameter
    TestDatabase database;

    /** Keeps embedded state alive across physical connection replacement. */
    private Connection anchor;
    /** URL shared by this test's physical connections. */
    private String url;
    /** Unique table name, also isolating concurrent integration-test runs. */
    private String table;
    /** Tracks successful setup so cleanup also works after setup failures. */
    private boolean tableCreated;

    /** Combines transaction managers, resource types, and completion outcomes. */
    static Stream<Arguments> transactions() {
        return Stream.of(false, true).flatMap(narayana -> Stream.of(false, true)
                .flatMap(xa -> Stream.of(false, true).map(commit -> Arguments.of(narayana, xa, commit))));
    }

    /** Creates a small pool that validates every borrowed connection. */
    private BasicDataSource createPool() {
        final BasicDataSource pool = new BasicDataSource();
        database.configure(pool, url);
        pool.setMaxTotal(1);
        pool.setMaxWait(Duration.ofSeconds(5));
        pool.setTestOnBorrow(true);
        pool.setValidationQuery(database.select(1));
        pool.setValidationQueryTimeout(Duration.ofSeconds(5));
        return pool;
    }

    /** Counts committed rows using an independent connection. */
    private int countRows() throws SQLException {
        try (Statement statement = anchor.createStatement(); ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            assertTrue(result.next());
            return result.getInt(1);
        }
    }

    /** Inserts one row through a JDBC prepared statement. */
    private void insert(final Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO " + table + " (value_column) VALUES (?)")) {
            statement.setString(1, "dbcp");
            assertEquals(1, statement.executeUpdate());
        }
    }

    /** Creates isolated database state with a live anchor for embedded engines. */
    @BeforeEach
    void setUpDatabase() throws SQLException {
        url = database.newUrl();
        table = "dbcp_" + UUID.randomUUID().toString().replace("-", "");
        anchor = database.createDataSource(url).getConnection();
        final String identity = database == TestDatabase.MYSQL ? "INTEGER AUTO_INCREMENT"
                : "INTEGER GENERATED BY DEFAULT AS IDENTITY (START WITH 1)";
        try (Statement statement = anchor.createStatement()) {
            statement.execute("CREATE TABLE " + table + " (id " + identity + " PRIMARY KEY, value_column VARCHAR(32))");
            tableCreated = true;
        }
    }

    /** Removes state even when an assertion or transaction fails. */
    @AfterEach
    void tearDownDatabase() throws SQLException {
        if (anchor != null) {
            try (Connection connection = anchor) {
                if (tableCreated) {
                    try (Statement statement = connection.createStatement()) {
                        statement.execute("DROP TABLE " + table);
                    }
                }
            }
        }
    }

    /** Checks CPDS logical close and physical connection reuse. */
    @Test
    void testConnectionPoolDataSource() throws Exception {
        final DriverAdapterCPDS adapter = new DriverAdapterCPDS();
        database.configure(adapter, url);
        final PooledConnection pooled = adapter.getPooledConnection();
        try {
            final Connection first = pooled.getConnection();
            insert(first);
            first.close();
            assertTrue(first.isClosed());
            try (Connection second = pooled.getConnection()) {
                assertTrue(second.isValid(5));
                assertEquals(1, countRows());
            }
        } finally {
            pooled.close();
        }
        assertThrows(SQLException.class, pooled::getConnection);
    }

    /** Checks validation, physical reuse, and replacement after explicit invalidation. */
    @Test
    void testConnectionReuseAndInvalidation() throws Exception {
        try (BasicDataSource pool = createPool()) {
            pool.setAccessToUnderlyingConnectionAllowed(true);
            final Connection physical;
            try (Connection first = pool.getConnection()) {
                physical = ((DelegatingConnection<?>) first).getInnermostDelegate();
                assertNotNull(physical);
                assertTrue(first.isValid(5));
                insert(first);
            }
            try (Connection second = pool.getConnection()) {
                assertSame(physical, ((DelegatingConnection<?>) second).getInnermostDelegate());
                assertEquals(1, countRows());
                pool.invalidateConnection(second);
                assertTrue(second.isClosed());
            }
            assertTrue(physical.isClosed());
            try (Connection replacement = pool.getConnection()) {
                assertTrue(replacement.isValid(5));
                assertEquals(1, countRows());
            }
            assertEquals(0, pool.getNumActive());
        }
    }

    /** Exercises commit and rollback with both transaction managers and real local/XA connections. */
    @ParameterizedTest(name = "narayana={0}, xa={1}, commit={2}")
    @MethodSource("transactions")
    void testManagedTransactions(final boolean narayana, final boolean xa, final boolean commit) throws Exception {
        jtaPropertyManager.getJTAEnvironmentBean()
                .setLastResourceOptimisationInterfaceClassName("org.apache.commons.dbcp2.managed.LocalXAConnectionFactory$LocalXAResource");
        final TransactionManager manager = narayana ? new TransactionManagerImple() : new TransactionManagerImpl();
        final AdditionalResource additional = new AdditionalResource();
        try (BasicManagedDataSource pool = new BasicManagedDataSource()) {
            database.configure(pool, url);
            pool.setTransactionManager(manager);
            pool.setMaxTotal(1);
            pool.setMaxWait(Duration.ofSeconds(5));
            if (xa) {
                pool.setXaDataSourceInstance(database.createXaDataSource(url));
            }
            manager.begin();
            try {
                try (Connection first = pool.getConnection(); Connection second = pool.getConnection()) {
                    insert(first);
                    assertEquals(1, pool.getNumActive());
                    assertTrue(second.isValid(5));
                }
                if (xa) {
                    assertTrue(manager.getTransaction().enlistResource(additional));
                }
                if (commit) {
                    manager.commit();
                } else {
                    manager.rollback();
                }
                assertEquals(xa && commit ? 1 : 0, additional.prepares);
                assertEquals(xa && commit ? 1 : 0, additional.commits);
                assertEquals(commit ? 1 : 0, countRows());
                assertEquals(0, pool.getNumActive());
                try (Connection next = pool.getConnection()) {
                    assertTrue(next.isValid(5));
                    assertTrue(next.getAutoCommit());
                }
            } finally {
                if (manager.getTransaction() != null) {
                    manager.rollback();
                }
            }
        }
    }

    /** Checks prepared-statement reuse and generated keys without leaking result sets. */
    @Test
    void testPreparedStatementsAndGeneratedKeys() throws Exception {
        try (BasicDataSource pool = createPool()) {
            pool.setAccessToUnderlyingConnectionAllowed(true);
            pool.setPoolPreparedStatements(true);
            pool.setMaxOpenPreparedStatements(2);
            Statement physical = null;
            for (int i = 0; i < 3; i++) {
                try (Connection connection = pool.getConnection();
                        PreparedStatement statement = connection.prepareStatement("INSERT INTO " + table + " (value_column) VALUES (?)",
                                Statement.RETURN_GENERATED_KEYS)) {
                    final Statement delegate = ((DelegatingStatement) statement).getInnermostDelegate();
                    assertNotNull(delegate);
                    if (physical == null) {
                        physical = delegate;
                    } else {
                        assertSame(physical, delegate);
                    }
                    statement.setString(1, "value" + i);
                    assertEquals(1, statement.executeUpdate());
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        assertTrue(keys.next());
                        assertTrue(keys.getLong(1) > 0);
                        assertSame(statement, keys.getStatement());
                    }
                }
            }
            assertEquals(3, countRows());
            assertEquals(0, pool.getNumActive());
        }
    }

    /** Checks rollback and state restoration when a connection is returned to the pool. */
    @Test
    void testRollbackAndStateRestoration() throws Exception {
        try (BasicDataSource pool = createPool()) {
            pool.setDefaultAutoCommit(true);
            pool.setDefaultReadOnly(false);
            pool.setDefaultTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            try (Connection first = pool.getConnection()) {
                first.setReadOnly(true);
            }
            try (Connection second = pool.getConnection()) {
                assertFalse(second.isReadOnly());
                second.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
                second.setAutoCommit(false);
                insert(second);
            }
            try (Connection next = pool.getConnection()) {
                assertTrue(next.getAutoCommit());
                assertFalse(next.isReadOnly());
                assertEquals(Connection.TRANSACTION_READ_COMMITTED, next.getTransactionIsolation());
                assertEquals(0, countRows());
            }
        }
    }
}
