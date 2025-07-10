/**
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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import jakarta.transaction.TransactionManager;
import javax.transaction.xa.XAException;

import org.apache.commons.dbcp2.ConnectionFactory;
import org.apache.commons.dbcp2.Constants;
import org.apache.commons.dbcp2.DriverConnectionFactory;
import org.apache.commons.dbcp2.PoolableConnection;
import org.apache.commons.dbcp2.PoolableConnectionFactory;
import org.apache.commons.dbcp2.PoolingDataSource;
import org.apache.commons.dbcp2.TesterDriver;
import org.apache.commons.pool2.SwallowedExceptionListener;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.geronimo.transaction.manager.TransactionManagerImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for ManagedConnection cached state.
 */
public class TestManagedConnectionCachedState {

    private static final class SwallowedExceptionRecorder implements SwallowedExceptionListener {

        private final List<Exception> exceptions = new ArrayList<>();

        public List<Exception> getExceptions() {
            return exceptions;
        }

        @Override
        public void onSwallowException(final Exception e) {
            exceptions.add(e);
        }
    }

    private PoolingDataSource<PoolableConnection> ds;

    private GenericObjectPool<PoolableConnection> pool;

    private TransactionManager transactionManager;

    private SwallowedExceptionRecorder swallowedExceptionRecorder;

    public Connection getConnection() throws SQLException {
        return ds.getConnection();
    }

    @BeforeEach
    public void setUp() throws XAException {
        // create a GeronimoTransactionManager for testing
        transactionManager = new TransactionManagerImpl();

        // create a driver connection factory
        final Properties properties = new Properties();
        properties.setProperty(Constants.KEY_USER, "userName");
        properties.setProperty(Constants.KEY_PASSWORD, "password");
        final ConnectionFactory connectionFactory = new DriverConnectionFactory(new TesterDriver(), "jdbc:apache:commons:testdriver", properties);

        // wrap it with a LocalXAConnectionFactory
        final XAConnectionFactory xaConnectionFactory = new LocalXAConnectionFactory(transactionManager, connectionFactory);

        // create the pool object factory
        // make sure we ask for state caching
        final PoolableConnectionFactory factory = new PoolableConnectionFactory(xaConnectionFactory, null);
        factory.setValidationQuery("SELECT DUMMY FROM DUAL");
        factory.setCacheState(true);

        // create the pool
        pool = new GenericObjectPool<>(factory);
        factory.setPool(pool);
        // record swallowed exceptions
        swallowedExceptionRecorder = new SwallowedExceptionRecorder();
        pool.setSwallowedExceptionListener(swallowedExceptionRecorder);

        // finally create the datasource
        ds = new ManagedDataSource<>(pool, xaConnectionFactory.getTransactionRegistry());
        ds.setAccessToUnderlyingConnectionAllowed(true);
    }

    @AfterEach
    public void tearDown() {
        pool.close();
    }

    @Test
    void testConnectionCachedState() throws Exception {
        // see DBCP-568

        // begin a transaction
        transactionManager.begin();
        // acquire a connection enlisted in the transaction
        try (final Connection conn = getConnection()) {
            // check the autocommit status to trigger internal caching
            conn.getAutoCommit();
            // ask the transaction manager to rollback
            transactionManager.rollback();
        }
        // check that no exceptions about failed rollback during close were logged
        assertEquals(0, swallowedExceptionRecorder.getExceptions().size());
    }

}
