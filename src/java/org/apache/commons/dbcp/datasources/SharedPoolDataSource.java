/*
 * Copyright 1999-2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import javax.naming.NamingException;
import javax.sql.ConnectionPoolDataSource;

import org.apache.commons.collections.LRUMap;
import org.apache.commons.pool.KeyedObjectPool;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.commons.dbcp.SQLNestedException;

/**
 * A pooling <code>DataSource</code> appropriate for deployment within
 * J2EE environment.  There are many configuration options, most of which are
 * defined in the parent class. All users (based on username) share a single 
 * maximum number of Connections in this datasource.
 *
 * @author John D. McNally
 * @version $Revision: 1.9 $ $Date: 2004/02/28 12:18:17 $
 */
public class SharedPoolDataSource
    extends InstanceKeyDataSource {

    private static final Map userKeys = new LRUMap(10);

    private int maxActive = GenericObjectPool.DEFAULT_MAX_ACTIVE;
    private int maxIdle = GenericObjectPool.DEFAULT_MAX_IDLE;
    private int maxWait = (int)Math.min((long)Integer.MAX_VALUE,
        GenericObjectPool.DEFAULT_MAX_WAIT);
    private KeyedObjectPool pool = null;

    /**
     * Default no-arg constructor for Serialization
     */
    public SharedPoolDataSource() {
    }

    /**
     * Close pool being maintained by this datasource.
     */
    public void close() throws Exception {
        pool.close();
        InstanceKeyObjectFactory.removeInstance(instanceKey);
    }

    // -------------------------------------------------------------------
    // Properties

    /**
     * The maximum number of active connections that can be allocated from
     * this pool at the same time, or zero for no limit.
     * The default is 0.
     */
    public int getMaxActive() {
        return (this.maxActive);
    }

    /**
     * The maximum number of active connections that can be allocated from
     * this pool at the same time, or zero for no limit.
     * The default is 0.
     */
    public void setMaxActive(int maxActive) {
        assertInitializationAllowed();
        this.maxActive = maxActive;
    }

    /**
     * The maximum number of active connections that can remain idle in the
     * pool, without extra ones being released, or zero for no limit.
     * The default is 0.
     */
    public int getMaxIdle() {
        return (this.maxIdle);
    }

    /**
     * The maximum number of active connections that can remain idle in the
     * pool, without extra ones being released, or zero for no limit.
     * The default is 0.
     */
    public void setMaxIdle(int maxIdle) {
        assertInitializationAllowed();
        this.maxIdle = maxIdle;
    }

    /**
     * The maximum number of milliseconds that the pool will wait (when there
     * are no available connections) for a connection to be returned before
     * throwing an exception, or -1 to wait indefinitely.  Will fail 
     * immediately if value is 0.
     * The default is -1.
     */
    public int getMaxWait() {
        return (this.maxWait);
    }

    /**
     * The maximum number of milliseconds that the pool will wait (when there
     * are no available connections) for a connection to be returned before
     * throwing an exception, or -1 to wait indefinitely.  Will fail 
     * immediately if value is 0.
     * The default is -1.
     */
    public void setMaxWait(int maxWait) {
        assertInitializationAllowed();
        this.maxWait = maxWait;
    }

    // ----------------------------------------------------------------------
    // Instrumentation Methods

    /**
     * Get the number of active connections in the pool.
     */
    public int getNumActive() {
        return (pool == null) ? 0 : pool.getNumActive();
    }

    /**
     * Get the number of idle connections in the pool.
     */
    public int getNumIdle() {
        return (pool == null) ? 0 : pool.getNumIdle();
    }

    // ----------------------------------------------------------------------
    // Inherited abstract methods

    protected synchronized PooledConnectionAndInfo 
        getPooledConnectionAndInfo(String username, String password)
        throws SQLException {
        if (pool == null) {
            try {
                registerPool(username, password);
            } catch (NamingException e) {
                throw new SQLNestedException("RegisterPool failed", e);
            }
        }

        PooledConnectionAndInfo info = null;
        try {
            info = (PooledConnectionAndInfo) pool
                .borrowObject(getUserPassKey(username, password));
        }
        catch (Exception e) {
            throw new SQLNestedException(
                "Could not retrieve connection info from pool", e);
        }
        return info;
    }

    private UserPassKey getUserPassKey(String username, String password) {
        UserPassKey key = (UserPassKey) userKeys.get(username);
        if (key == null) {
            key = new UserPassKey(username, password);
            userKeys.put(username, key);
        }
        return key;
    }

    private void registerPool(
        String username, String password) 
        throws javax.naming.NamingException, SQLException {

        ConnectionPoolDataSource cpds = testCPDS(username, password);

        // Create an object pool to contain our PooledConnections
        GenericKeyedObjectPool tmpPool = new GenericKeyedObjectPool(null);
        tmpPool.setMaxActive(getMaxActive());
        tmpPool.setMaxIdle(getMaxIdle());
        tmpPool.setMaxWait(getMaxWait());
        tmpPool.setWhenExhaustedAction(whenExhaustedAction(maxActive, maxWait));
        tmpPool.setTestOnBorrow(getTestOnBorrow());
        tmpPool.setTestOnReturn(getTestOnReturn());
        tmpPool.setTimeBetweenEvictionRunsMillis(
            getTimeBetweenEvictionRunsMillis());
        tmpPool.setNumTestsPerEvictionRun(getNumTestsPerEvictionRun());
        tmpPool.setMinEvictableIdleTimeMillis(getMinEvictableIdleTimeMillis());
        tmpPool.setTestWhileIdle(getTestWhileIdle());
        pool = tmpPool;
        // Set up the factory we will use (passing the pool associates
        // the factory with the pool, so we do not have to do so
        // explicitly)
        new KeyedCPDSConnectionFactory(cpds, pool, getValidationQuery());
    }

    protected void setupDefaults(Connection con, String username)
        throws SQLException {
        con.setAutoCommit(isDefaultAutoCommit());
        con.setReadOnly(isDefaultReadOnly());
        int defaultTransactionIsolation = getDefaultTransactionIsolation();
        if (defaultTransactionIsolation != UNKNOWN_TRANSACTIONISOLATION) {
            con.setTransactionIsolation(defaultTransactionIsolation);
        }
    }

    /**
     * Supports Serialization interface.
     *
     * @param in a <code>java.io.ObjectInputStream</code> value
     * @exception IOException if an error occurs
     * @exception ClassNotFoundException if an error occurs
     */
    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException {
        try 
        {
            in.defaultReadObject();
            SharedPoolDataSource oldDS = (SharedPoolDataSource)
                new SharedPoolDataSourceFactory()
                    .getObjectInstance(getReference(), null, null, null);
            this.pool = oldDS.pool;
        }
        catch (NamingException e)
        {
            throw new IOException("NamingException: " + e);
        }
    }
}

