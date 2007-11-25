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

package org.apache.commons.dbcp.datasources;

import java.io.PrintWriter;
import java.sql.SQLException;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.PooledConnection;

/**
 * ConnectionPoolDataSource implementation that proxies another 
 * ConnectionPoolDataSource.
 * 
 * @version $Revision$ $Date$
 */
public class ConnectionPoolDataSourceProxy implements ConnectionPoolDataSource {

    protected ConnectionPoolDataSource delegate = null;
    
    public ConnectionPoolDataSourceProxy(ConnectionPoolDataSource cpds) {
        this.delegate = cpds;
    }
    
    public ConnectionPoolDataSource getDelegate() {
        return delegate;
    }
    
    public int getLoginTimeout() throws SQLException {
        return delegate.getLoginTimeout();
    }
   
    public PrintWriter getLogWriter() throws SQLException {
        return delegate.getLogWriter();
    }

    /**
     * Return a TesterPooledConnection with notifyOnClose turned on
     */
    public PooledConnection getPooledConnection() throws SQLException {
        return wrapPooledConnection(delegate.getPooledConnection());
    }

    /**
     * Return a TesterPooledConnection with notifyOnClose turned on
     */
    public PooledConnection getPooledConnection(String user, String password)
            throws SQLException {
        return wrapPooledConnection(delegate.getPooledConnection(user, password));
    }

    public void setLoginTimeout(int seconds) throws SQLException {
        delegate.setLoginTimeout(seconds);     
    }

    public void setLogWriter(PrintWriter out) throws SQLException {
        delegate.setLogWriter(out);     
    }
    
    /**
     * Create a TesterPooledConnection with notifyOnClose turned on
     */
    protected PooledConnection wrapPooledConnection(PooledConnection pc) {
        PooledConnectionProxy tpc = new PooledConnectionProxy(pc);
        tpc.setNotifyOnClose(true);
        return tpc; 
    }

}
