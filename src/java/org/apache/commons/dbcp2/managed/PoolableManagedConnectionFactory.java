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

package org.apache.commons.dbcp2.managed;
import java.sql.Connection;

import org.apache.commons.dbcp2.DelegatingPreparedStatement;
import org.apache.commons.dbcp2.PStmtKey;
import org.apache.commons.dbcp2.PoolableConnection;
import org.apache.commons.dbcp2.PoolableConnectionFactory;
import org.apache.commons.dbcp2.PoolingConnection;
import org.apache.commons.pool2.KeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;
import org.apache.commons.pool2.impl.WhenExhaustedAction;

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
     */
    public PoolableManagedConnectionFactory(XAConnectionFactory connFactory) {
        super(connFactory);
        this.transactionRegistry = connFactory.getTransactionRegistry();
    }
    
    /**
     * Uses the configured XAConnectionFactory to create a {@link PoolableManagedConnection}.
     * Throws <code>IllegalStateException</code> if the connection factory returns null.
     * Also initializes the connection using configured initialization sql (if provided)
     * and sets up a prepared statement pool associated with the PoolableManagedConnection
     * if statement pooling is enabled.
     */
    @Override
    synchronized public PoolableConnection makeObject() throws Exception {
        Connection conn = _connFactory.createConnection();
        if (conn == null) {
            throw new IllegalStateException("Connection factory returned null from createConnection");
        }
        initializeConnection(conn);
        if(poolStatements) {
            conn = new PoolingConnection(conn);
            GenericKeyedObjectPoolConfig<PStmtKey,DelegatingPreparedStatement> config =
                new GenericKeyedObjectPoolConfig<PStmtKey,DelegatingPreparedStatement>();
            config.setMaxTotalPerKey(-1);
            config.setWhenExhaustedAction(WhenExhaustedAction.FAIL);
            config.setMaxWait(0);
            config.setMaxIdlePerKey(1);
            config.setMaxTotal(maxOpenPreparedStatements);
            KeyedObjectPool<PStmtKey,DelegatingPreparedStatement> stmtPool =
                new GenericKeyedObjectPool<PStmtKey,DelegatingPreparedStatement>(
                        (PoolingConnection)conn, config);
            ((PoolingConnection)conn).setStatementPool(stmtPool);
            ((PoolingConnection) conn).setCacheState(_cacheState);
        }
        return new PoolableManagedConnection(transactionRegistry, conn, _pool,
                _config);
    }

}
