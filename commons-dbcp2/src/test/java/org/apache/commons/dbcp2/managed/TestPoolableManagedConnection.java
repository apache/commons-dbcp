/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.commons.dbcp2.managed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Properties;

import javax.transaction.TransactionManager;

import org.apache.commons.dbcp2.ConnectionFactory;
import org.apache.commons.dbcp2.Constants;
import org.apache.commons.dbcp2.DriverConnectionFactory;
import org.apache.commons.dbcp2.PoolableConnection;
import org.apache.commons.dbcp2.PoolableConnectionFactory;
import org.apache.commons.dbcp2.TesterDriver;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.geronimo.transaction.manager.TransactionManagerImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for PoolableManagedConnection.
 */
public class TestPoolableManagedConnection {

    private TransactionManager transactionManager;
    private TransactionRegistry transactionRegistry;
    private GenericObjectPool<PoolableConnection> pool;
    private Connection conn;
    private PoolableManagedConnection poolableManagedConnection;

    @BeforeEach
    public void setUp() throws Exception {
        // create a GeronimoTransactionManager for testing
        transactionManager = new TransactionManagerImpl();

        // create a driver connection factory
        final Properties properties = new Properties();
        properties.setProperty(Constants.KEY_USER, "userName");
        properties.setProperty(Constants.KEY_PASSWORD, "password");
        final ConnectionFactory connectionFactory = new DriverConnectionFactory(new TesterDriver(), "jdbc:apache:commons:testdriver", properties);

        // wrap it with a LocalXAConnectionFactory
        final XAConnectionFactory xaConnectionFactory = new LocalXAConnectionFactory(transactionManager, connectionFactory);

        // create transaction registry
        transactionRegistry = xaConnectionFactory.getTransactionRegistry();

        // create the pool object factory
        final PoolableConnectionFactory factory = new PoolableConnectionFactory(xaConnectionFactory, null);
        factory.setValidationQuery("SELECT DUMMY FROM DUAL");
        factory.setDefaultReadOnly(Boolean.TRUE);
        factory.setDefaultAutoCommit(Boolean.TRUE);

        // create the pool
        pool = new GenericObjectPool<>(factory);
        factory.setPool(pool);
        pool.setMaxTotal(10);
        pool.setMaxWait(Duration.ofMillis(100));
    }

    @AfterEach
    public void tearDown() throws SQLException {
        if (conn != null && !conn.isClosed()) {
            conn.close();
        }
        if (pool != null && !pool.isClosed()) {
            pool.close();
        }
    }

    @Test
    void testManagedConnection() throws Exception {
        assertEquals(0, pool.getNumActive());
        // create a connection
        conn = pool.borrowObject();
        assertEquals(1, pool.getNumActive());
        // create the poolable managed connection
        poolableManagedConnection = new PoolableManagedConnection(transactionRegistry, conn, pool);
        poolableManagedConnection.close();
        // closing a poolable managed connection won't close it, but simply return to the pool
        assertEquals(1, pool.getNumActive());
        // but closing the underlying connection really closes it
        conn.close();
        assertEquals(0, pool.getNumActive());
    }

    @Test
    void testPoolableConnection() throws Exception {
        // create a connection
        // pool uses LocalXAConnectionFactory, which register the connection with the TransactionRegistry
        conn = pool.borrowObject();
        assertNotNull(transactionRegistry.getXAResource(conn));
        // create the poolable managed connection
        poolableManagedConnection = new PoolableManagedConnection(transactionRegistry, conn, pool);
        poolableManagedConnection.close();
        assertNotNull(transactionRegistry.getXAResource(conn));
    }

    @Test
    void testReallyClose() throws Exception {
        assertEquals(0, pool.getNumActive());
        // create a connection
        // pool uses LocalXAConnectionFactory, which register the connection with the
        // TransactionRegistry
        conn = pool.borrowObject();
        assertEquals(1, pool.getNumActive());
        assertNotNull(transactionRegistry.getXAResource(conn));
        // create the poolable managed connection
        poolableManagedConnection = new PoolableManagedConnection(transactionRegistry, conn, pool);
        poolableManagedConnection.close();
        assertNotNull(transactionRegistry.getXAResource(conn));
        assertEquals(1, pool.getNumActive());
        // this must close the managed connection, removing it from the transaction
        // registry
        poolableManagedConnection.reallyClose();
        assertThrows(SQLException.class, () -> transactionRegistry.getXAResource(conn), "Transaction registry was supposed to be empty now");
        assertEquals(0, pool.getNumActive());
    }
}
