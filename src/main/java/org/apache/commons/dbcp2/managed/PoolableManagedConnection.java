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
import java.sql.SQLException;

import org.apache.commons.dbcp2.PoolableConnection;
import org.apache.commons.pool2.ObjectPool;

/**
 * PoolableConnection that unregisters from TransactionRegistry on Connection real destroy.
 *
 * @see PoolableConnection
 * @since 2.0
 */
public class PoolableManagedConnection extends PoolableConnection {
    private final TransactionRegistry transactionRegistry;


    /**
     * Create a PoolableManagedConnection.
     *
     * @param transactionRegistry transaction registry
     * @param conn underlying connection
     * @param pool connection pool
     */
    public PoolableManagedConnection(TransactionRegistry transactionRegistry,
            Connection conn, ObjectPool<PoolableConnection> pool) {
        super(conn, pool, null);
        this.transactionRegistry = transactionRegistry;
    }


    /**
     * Actually close the underlying connection.
     */
    @Override
    public void reallyClose() throws SQLException {
        try {
            super.reallyClose();
        } finally {
            transactionRegistry.unregisterConnection(this);
        }
    }
}
