/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.dbcp.managed;
import java.sql.Connection;
import java.util.Collection;

import org.apache.commons.dbcp.AbandonedConfig;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.PoolingConnection;
import org.apache.commons.pool.KeyedObjectPool;
import org.apache.commons.pool.KeyedObjectPoolFactory;
import org.apache.commons.pool.ObjectPool;

/**
 * A {@link PoolableConnectionFactory} that creates {@link PoolableManagedConnection}s.
 * 
 * @version $Revision$ $Date$
 */
public class PoolableManagedConnectionFactory extends PoolableConnectionFactory {

    /** Transaction registry associated with connections created by this factory */
    private final TransactionRegistry transactionRegistry;
    
    /**
     * Create a PoolableManagedConnectionFactory and attach it to a connection pool.
     * 
     * @param connFactory XAConnectionFactory
     * @param pool connection pool 
     * @param stmtPoolFactory the {@link KeyedObjectPoolFactory} to use to create {@link KeyedObjectPool}s for pooling
     * {@link java.sql.PreparedStatement}s, or <tt>null</tt> to disable {@link java.sql.PreparedStatement} pooling
     * @param validationQuery a query to use to {@link #validateObject validate} {@link Connection}s.
     * Should return at least one row. Using <tt>null</tt> turns off validation.
     * @param defaultReadOnly the default "read only" setting for borrowed {@link Connection}s
     * @param defaultAutoCommit the default "auto commit" setting for returned {@link Connection}s
     */
    public PoolableManagedConnectionFactory(XAConnectionFactory connFactory,
            ObjectPool pool, KeyedObjectPoolFactory stmtPoolFactory,
            String validationQuery, boolean defaultReadOnly,
            boolean defaultAutoCommit) {
        super(connFactory, pool, stmtPoolFactory, validationQuery,
                defaultReadOnly, defaultAutoCommit);
        this.transactionRegistry = connFactory.getTransactionRegistry();
    }
    
    /**
     * Create a PoolableManagedConnectionFactory and attach it to a connection pool.
     * 
     * @param connFactory XAConnectionFactory
     * @param pool connection pool 
     * @param stmtPoolFactory the {@link KeyedObjectPoolFactory} to use to create {@link KeyedObjectPool}s for pooling
     * {@link java.sql.PreparedStatement}s, or <tt>null</tt> to disable {@link java.sql.PreparedStatement} pooling
     * @param validationQuery a query to use to {@link #validateObject validate} {@link Connection}s.
     * Should return at least one row. Using <tt>null</tt> turns off validation.
     * @param validationQueryTimeout the number of seconds that validation queries will wait for database response
     * before failing. Use a value less than or equal to 0 for no timeout.
     * @param connectionInitSqls a Collection of SQL statements to initialize {@link Connection}s.
     * Using <tt>null</tt> turns off initialization.
     * @param defaultReadOnly the default "read only" setting for borrowed {@link Connection}s
     * @param defaultAutoCommit the default "auto commit" setting for returned {@link Connection}s
     * @param defaultTransactionIsolation the default "Transaction Isolation" setting for returned {@link Connection}s
     * @param defaultCatalog the default "catalog" setting for returned {@link Connection}s
     * @param config the AbandonedConfig if tracing SQL objects
     */
    public PoolableManagedConnectionFactory(XAConnectionFactory connFactory,
            ObjectPool pool,
            KeyedObjectPoolFactory stmtPoolFactory,
            String validationQuery,
            int validationQueryTimeout,
            Collection connectionInitSqls,
            Boolean defaultReadOnly,
            boolean defaultAutoCommit,
            int defaultTransactionIsolation,
            String defaultCatalog,
            AbandonedConfig config) {
        super(connFactory, pool, stmtPoolFactory, validationQuery, validationQueryTimeout, connectionInitSqls,
                defaultReadOnly, defaultAutoCommit, defaultTransactionIsolation, defaultCatalog, config);
        this.transactionRegistry = connFactory.getTransactionRegistry();
    }

    /**
     * Uses the configured XAConnectionFactory to create a {@link PoolableManagedConnection}.
     * Throws <code>IllegalStateException</code> if the connection factory returns null.
     * Also initializes the connection using configured initialization sql (if provided)
     * and sets up a prepared statement pool associated with the PoolableManagedConnection
     * if statement pooling is enabled.
     */
    synchronized public Object makeObject() throws Exception {
        Connection conn = _connFactory.createConnection();
        if (conn == null) {
            throw new IllegalStateException("Connection factory returned null from createConnection");
        }
        initializeConnection(conn);
        if(null != _stmtPoolFactory) {
            KeyedObjectPool stmtpool = _stmtPoolFactory.createPool();
            conn = new PoolingConnection(conn,stmtpool);
            stmtpool.setFactory((PoolingConnection)conn);
        }
        return new PoolableManagedConnection(transactionRegistry,conn,_pool,_config);
    }

}
