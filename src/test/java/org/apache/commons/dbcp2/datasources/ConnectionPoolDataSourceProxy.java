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

package org.apache.commons.dbcp2.datasources;

import java.io.PrintWriter;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import javax.sql.ConnectionPoolDataSource;
import javax.sql.PooledConnection;

import org.apache.commons.dbcp2.Jdbc41Bridge;

/**
 * ConnectionPoolDataSource implementation that proxies another
 * ConnectionPoolDataSource.
 */
public class ConnectionPoolDataSourceProxy implements ConnectionPoolDataSource {

    protected ConnectionPoolDataSource delegate = null;

    public ConnectionPoolDataSourceProxy(final ConnectionPoolDataSource cpds) {
        this.delegate = cpds;
    }

    public ConnectionPoolDataSource getDelegate() {
        return delegate;
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return delegate.getLoginTimeout();
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return delegate.getLogWriter();
    }

    /**
     * Return a TesterPooledConnection with notifyOnClose turned on
     */
    @Override
    public PooledConnection getPooledConnection() throws SQLException {
        return wrapPooledConnection(delegate.getPooledConnection());
    }

    /**
     * Return a TesterPooledConnection with notifyOnClose turned on
     */
    @Override
    public PooledConnection getPooledConnection(final String user, final String password)
            throws SQLException {
        return wrapPooledConnection(delegate.getPooledConnection(user, password));
    }

    @Override
    public void setLoginTimeout(final int seconds) throws SQLException {
        delegate.setLoginTimeout(seconds);
    }

    @Override
    public void setLogWriter(final PrintWriter out) throws SQLException {
        delegate.setLogWriter(out);
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return Jdbc41Bridge.getParentLogger(delegate);
    }

    /**
     * Create a TesterPooledConnection with notifyOnClose turned on
     */
    protected PooledConnection wrapPooledConnection(final PooledConnection pc) {
        final PooledConnectionProxy tpc = new PooledConnectionProxy(pc);
        tpc.setNotifyOnClose(true);
        return tpc;
    }
}
