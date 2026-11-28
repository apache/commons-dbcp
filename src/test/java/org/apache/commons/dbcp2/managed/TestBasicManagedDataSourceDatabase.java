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

package org.apache.commons.dbcp2.managed;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.XADataSource;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.xa.XAException;

import org.apache.commons.dbcp2.AbstractDatabaseTest;
import org.apache.geronimo.transaction.manager.TransactionManagerImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingSupplier;

import com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionManagerImple;
import com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionSynchronizationRegistryImple;

/**
 * Tests managed data sources with real JDBC drivers.
 */
class TestBasicManagedDataSourceDatabase extends AbstractDatabaseTest {

    @Test
    void testCreateXaDataSourceNewInstance() throws SQLException, XAException {
        try (final BasicManagedDataSource basicManagedDataSource = new BasicManagedDataSource()) {
            basicManagedDataSource.setXADataSource(database.createXaDataSource(database.newUrl()).getClass().getName());
            basicManagedDataSource.setDriverClassName(database.getDriverClassName());
            basicManagedDataSource.setTransactionManager(new TransactionManagerImpl());
            assertNotNull(basicManagedDataSource.createConnectionFactory());
        }
    }

    @Test
    void testSetXaDataSourceInstance() throws SQLException, XAException {
        try (final BasicManagedDataSource basicManagedDataSource = new BasicManagedDataSource()) {
            basicManagedDataSource.setTransactionManager(new TransactionManagerImpl());
            database.configure(basicManagedDataSource, database.newUrl());
            basicManagedDataSource.setMaxIdle(1);
            basicManagedDataSource.setXaDataSourceInstance(database.createXaDataSource(database.newUrl()));
            assertNotNull(basicManagedDataSource.createConnectionFactory());
        }
    }

    @Test
    void testTransactionSynchronizationRegistry() throws Exception {
        try (final BasicManagedDataSource basicManagedDataSource = new BasicManagedDataSource()) {
            basicManagedDataSource.setTransactionManager(new TransactionManagerImple());
            final TransactionSynchronizationRegistry tsr = new TransactionSynchronizationRegistryImple();
            basicManagedDataSource.setTransactionSynchronizationRegistry(tsr);
            final XADataSource xaDataSource = database.createXaDataSource(database.newUrl());
            basicManagedDataSource.setXaDataSourceInstance(xaDataSource);
            basicManagedDataSource.setMaxIdle(1);

            final TransactionManager tm = basicManagedDataSource.getTransactionManager();
            tm.begin();
            tsr.registerInterposedSynchronization(new SynchronizationAdapter() {
                @Override
                public void beforeCompletion() {
                    try (Connection connection = assertDoesNotThrow((ThrowingSupplier<Connection>) basicManagedDataSource::getConnection)) {
                        assertNotNull(connection);
                    } catch (final SQLException e) {
                        fail(e);
                    }
                }
            });
            tm.commit();
        }
    }

    @Test
    void testXaDataSourceInstance() throws SQLException {
        try (final BasicManagedDataSource basicManagedDataSource = new BasicManagedDataSource()) {
            final XADataSource ds = database.createXaDataSource(database.newUrl());
            basicManagedDataSource.setXaDataSourceInstance(ds);
            assertEquals(ds, basicManagedDataSource.getXaDataSourceInstance());
        }
    }
}
