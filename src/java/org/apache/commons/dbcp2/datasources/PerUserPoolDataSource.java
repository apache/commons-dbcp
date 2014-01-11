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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.sql.ConnectionPoolDataSource;

import org.apache.commons.dbcp2.SwallowedExceptionLogger;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

/**
 * <p>A pooling <code>DataSource</code> appropriate for deployment within
 * J2EE environment.  There are many configuration options, most of which are
 * defined in the parent class.  This datasource uses individual pools per
 * user, and some properties can be set specifically for a given user, if the
 * deployment environment can support initialization of mapped properties.
 * So for example, a pool of admin or write-access Connections can be
 * guaranteed a certain number of connections, separate from a maximum
 * set for users with read-only connections.</p>
 *
 * <p>User passwords can be changed without re-initializing the datasource.
 * When a <code>getConnection(username, password)</code> request is processed
 * with a password that is different from those used to create connections in the
 * pool associated with <code>username</code>, an attempt is made to create a
 * new connection using the supplied password and if this succeeds, the existing
 * pool is cleared and a new pool is created for connections using the new password.</p>
 *
 *
 * @author John D. McNally
 * @version $Revision$ $Date$
 */
public class PerUserPoolDataSource extends InstanceKeyDataSource {

    private static final long serialVersionUID = -3104731034410444060L;

    private static final Log log =
            LogFactory.getLog(PerUserPoolDataSource.class);

    private int defaultMaxTotal = GenericObjectPoolConfig.DEFAULT_MAX_TOTAL;
    private int defaultMaxIdle = GenericObjectPoolConfig.DEFAULT_MAX_IDLE;
    private int defaultMaxWaitMillis = (int)Math.min(Integer.MAX_VALUE,
            GenericObjectPoolConfig.DEFAULT_MAX_WAIT_MILLIS);
    private Map<String,Boolean> perUserDefaultAutoCommit = null;
    private Map<String,Integer> perUserDefaultTransactionIsolation = null;
    private Map<String,Integer> perUserMaxTotal = null;
    private Map<String,Integer> perUserMaxIdle = null;
    private Map<String,Integer> perUserMaxWaitMillis = null;
    private Map<String,Boolean> perUserDefaultReadOnly = null;

    /**
     * Map to keep track of Pools for a given user
     */
    private transient Map<PoolKey, PooledConnectionManager> managers =
            new HashMap<>();

    /**
     * Default no-arg constructor for Serialization
     */
    public PerUserPoolDataSource() {
    }

    /**
     * Close pool(s) being maintained by this datasource.
     */
    @Override
    public void close() {
        for (PooledConnectionManager manager : managers.values()) {
            try {
              ((CPDSConnectionFactory) manager).getPool().close();
            } catch (Exception closePoolException) {
                    //ignore and try to close others.
            }
        }
        InstanceKeyObjectFactory.removeInstance(getInstanceKey());
    }

    // -------------------------------------------------------------------
    // Properties

    /**
     * The maximum number of active connections that can be allocated from
     * this pool at the same time, or negative for no limit.
     * This value is used for any username which is not specified
     * in perUserMaxConnections.
     */
    public int getDefaultMaxTotal() {
        return (this.defaultMaxTotal);
    }

    /**
     * The maximum number of active connections that can be allocated from
     * this pool at the same time, or negative for no limit.
     * This value is used for any username which is not specified
     * in perUserMaxConnections.  The default is 8.
     */
    public void setDefaultMaxTotal(int maxActive) {
        assertInitializationAllowed();
        this.defaultMaxTotal = maxActive;
    }

    /**
     * The maximum number of active connections that can remain idle in the
     * pool, without extra ones being released, or negative for no limit.
     * This value is used for any username which is not specified
     * in perUserMaxIdle.
     */
    public int getDefaultMaxIdle() {
        return (this.defaultMaxIdle);
    }

    /**
     * The maximum number of active connections that can remain idle in the
     * pool, without extra ones being released, or negative for no limit.
     * This value is used for any username which is not specified
     * in perUserMaxIdle.  The default is 8.
     */
    public void setDefaultMaxIdle(int defaultMaxIdle) {
        assertInitializationAllowed();
        this.defaultMaxIdle = defaultMaxIdle;
    }

    /**
     * The maximum number of milliseconds that the pool will wait (when there
     * are no available connections) for a connection to be returned before
     * throwing an exception, or -1 to wait indefinitely.  Will fail
     * immediately if value is 0.
     * This value is used for any username which is not specified
     * in perUserMaxWaitMillis.  The default is -1.
     */
    public int getDefaultMaxWaitMillis() {
        return (this.defaultMaxWaitMillis);
    }

    /**
     * The maximum number of milliseconds that the pool will wait (when there
     * are no available connections) for a connection to be returned before
     * throwing an exception, or -1 to wait indefinitely.  Will fail
     * immediately if value is 0.
     * This value is used for any username which is not specified
     * in perUserMaxWaitMillis.  The default is -1.
     */
    public void setDefaultMaxWaitMillis(int defaultMaxWaitMillis
            ) {
        assertInitializationAllowed();
        this.defaultMaxWaitMillis = defaultMaxWaitMillis;
    }

    /**
     * The keys are usernames and the value is the --.  Any
     * username specified here will override the value of defaultAutoCommit.
     */
    public Boolean getPerUserDefaultAutoCommit(String key) {
        Boolean value = null;
        if (perUserDefaultAutoCommit != null) {
            value = perUserDefaultAutoCommit.get(key);
        }
        return value;
    }

    /**
     * The keys are usernames and the value is the --.  Any
     * username specified here will override the value of defaultAutoCommit.
     */
    public void setPerUserDefaultAutoCommit(String username, Boolean value) {
        assertInitializationAllowed();
        if (perUserDefaultAutoCommit == null) {
            perUserDefaultAutoCommit = new HashMap<>();
        }
        perUserDefaultAutoCommit.put(username, value);
    }

    void setPerUserDefaultAutoCommit(Map<String,Boolean> userDefaultAutoCommit) {
        assertInitializationAllowed();
        if (perUserDefaultAutoCommit == null) {
            perUserDefaultAutoCommit = new HashMap<>();
        } else {
            perUserDefaultAutoCommit.clear();
        }
        perUserDefaultAutoCommit.putAll(userDefaultAutoCommit);
    }

    /**
     * The isolation level of connections when returned from getConnection.
     * If null, the username will use the value of defaultTransactionIsolation.
     */
    public Integer getPerUserDefaultTransactionIsolation(String username) {
        Integer value = null;
        if (perUserDefaultTransactionIsolation != null) {
            value = perUserDefaultTransactionIsolation.get(username);
        }
        return value;
    }

    /**
     * The isolation level of connections when returned from getConnection.
     * Valid values are the constants defined in Connection.
     */
    public void setPerUserDefaultTransactionIsolation(String username,
                                                      Integer value) {
        assertInitializationAllowed();
        if (perUserDefaultTransactionIsolation == null) {
            perUserDefaultTransactionIsolation = new HashMap<>();
        }
        perUserDefaultTransactionIsolation.put(username, value);
    }

    void setPerUserDefaultTransactionIsolation(
            Map<String,Integer> userDefaultTransationIsolation) {
        assertInitializationAllowed();
        if (perUserDefaultTransactionIsolation == null) {
            perUserDefaultTransactionIsolation = new HashMap<>();
        } else {
            perUserDefaultTransactionIsolation.clear();
        }
        perUserDefaultTransactionIsolation.putAll(userDefaultTransationIsolation);
    }

    /**
     * The maximum number of active connections that can be allocated from
     * this pool at the same time, or negative for no limit.
     * The keys are usernames and the value is the maximum connections.  Any
     * username specified here will override the value of defaultMaxTotal.
     */
    public Integer getPerUserMaxTotal(String username) {
        Integer value = null;
        if (perUserMaxTotal != null) {
            value = perUserMaxTotal.get(username);
        }
        return value;
    }

    /**
     * The maximum number of active connections that can be allocated from
     * this pool at the same time, or negative for no limit.
     * The keys are usernames and the value is the maximum connections.  Any
     * username specified here will override the value of defaultMaxTotal.
     */
    public void setPerUserMaxTotal(String username, Integer value) {
        assertInitializationAllowed();
        if (perUserMaxTotal == null) {
            perUserMaxTotal = new HashMap<>();
        }
        perUserMaxTotal.put(username, value);
    }

    void setPerUserMaxTotal(Map<String,Integer> userMaxTotal) {
        assertInitializationAllowed();
        if (perUserMaxTotal == null) {
            perUserMaxTotal = new HashMap<>();
        } else {
            perUserMaxTotal.clear();
        }
        perUserMaxTotal.putAll(userMaxTotal);
    }

    /**
     * The maximum number of active connections that can remain idle in the
     * pool, without extra ones being released, or negative for no limit.
     * The keys are usernames and the value is the maximum connections.  Any
     * username specified here will override the value of defaultMaxIdle.
     */
    public Integer getPerUserMaxIdle(String username) {
        Integer value = null;
        if (perUserMaxIdle != null) {
            value = perUserMaxIdle.get(username);
        }
        return value;
    }

    /**
     * The maximum number of active connections that can remain idle in the
     * pool, without extra ones being released, or negative for no limit.
     * The keys are usernames and the value is the maximum connections.  Any
     * username specified here will override the value of defaultMaxIdle.
     */
    public void setPerUserMaxIdle(String username, Integer value) {
        assertInitializationAllowed();
        if (perUserMaxIdle == null) {
            perUserMaxIdle = new HashMap<>();
        }
        perUserMaxIdle.put(username, value);
    }

    void setPerUserMaxIdle(Map<String,Integer> userMaxIdle) {
        assertInitializationAllowed();
        if (perUserMaxIdle == null) {
            perUserMaxIdle = new HashMap<>();
        } else {
            perUserMaxIdle.clear();
        }
        perUserMaxIdle.putAll(userMaxIdle);
    }

    /**
     * The maximum number of milliseconds that the pool will wait (when there
     * are no available connections) for a connection to be returned before
     * throwing an exception, or -1 to wait indefinitely.  Will fail
     * immediately if value is 0.
     * The keys are usernames and the value is the maximum connections.  Any
     * username specified here will override the value of defaultMaxWaitMillis.
     */
    public Integer getPerUserMaxWaitMillis(String username) {
        Integer value = null;
        if (perUserMaxWaitMillis != null) {
            value = perUserMaxWaitMillis.get(username);
        }
        return value;
    }

    /**
     * The maximum number of milliseconds that the pool will wait (when there
     * are no available connections) for a connection to be returned before
     * throwing an exception, or -1 to wait indefinitely.  Will fail
     * immediately if value is 0.
     * The keys are usernames and the value is the maximum connections.  Any
     * username specified here will override the value of defaultMaxWaitMillis.
     */
    public void setPerUserMaxWaitMillis(String username, Integer value) {
        assertInitializationAllowed();
        if (perUserMaxWaitMillis == null) {
            perUserMaxWaitMillis = new HashMap<>();
        }
        perUserMaxWaitMillis.put(username, value);
    }

    void setPerUserMaxWaitMillis(Map<String,Integer> userMaxWaitMillis) {
        assertInitializationAllowed();
        if (perUserMaxWaitMillis == null) {
            perUserMaxWaitMillis = new HashMap<>();
        } else {
            perUserMaxWaitMillis.clear();
        }
        perUserMaxWaitMillis.putAll(userMaxWaitMillis);
    }

    /**
     * The keys are usernames and the value is the --.  Any
     * username specified here will override the value of defaultReadOnly.
     */
    public Boolean getPerUserDefaultReadOnly(String username) {
        Boolean value = null;
        if (perUserDefaultReadOnly != null) {
            value = perUserDefaultReadOnly.get(username);
        }
        return value;
    }

    /**
     * The keys are usernames and the value is the --.  Any
     * username specified here will override the value of defaultReadOnly.
     */
    public void setPerUserDefaultReadOnly(String username, Boolean value) {
        assertInitializationAllowed();
        if (perUserDefaultReadOnly == null) {
            perUserDefaultReadOnly = new HashMap<>();
        }
        perUserDefaultReadOnly.put(username, value);
    }

    void setPerUserDefaultReadOnly(Map<String,Boolean> userDefaultReadOnly) {
        assertInitializationAllowed();
        if (perUserDefaultReadOnly == null) {
            perUserDefaultReadOnly = new HashMap<>();
        } else {
            perUserDefaultReadOnly.clear();
        }
        perUserDefaultReadOnly.putAll(userDefaultReadOnly);
    }

    // ----------------------------------------------------------------------
    // Instrumentation Methods

    /**
     * Get the number of active connections in the default pool.
     */
    public int getNumActive() {
        return getNumActive(null, null);
    }

    /**
     * Get the number of active connections in the pool for a given user.
     */
    public int getNumActive(String username, String password) {
        ObjectPool<PooledConnectionAndInfo> pool =
            getPool(getPoolKey(username,password));
        return (pool == null) ? 0 : pool.getNumActive();
    }

    /**
     * Get the number of idle connections in the default pool.
     */
    public int getNumIdle() {
        return getNumIdle(null, null);
    }

    /**
     * Get the number of idle connections in the pool for a given user.
     */
    public int getNumIdle(String username, String password) {
        ObjectPool<PooledConnectionAndInfo> pool =
            getPool(getPoolKey(username,password));
        return (pool == null) ? 0 : pool.getNumIdle();
    }


    // ----------------------------------------------------------------------
    // Inherited abstract methods

    @Override
    protected PooledConnectionAndInfo
        getPooledConnectionAndInfo(String username, String password)
        throws SQLException {

        final PoolKey key = getPoolKey(username,password);
        ObjectPool<PooledConnectionAndInfo> pool;
        PooledConnectionManager manager;
        synchronized(this) {
            manager = managers.get(key);
            if (manager == null) {
                try {
                    registerPool(username, password);
                    manager = managers.get(key);
                } catch (NamingException e) {
                    throw new SQLException("RegisterPool failed", e);
                }
            }
            pool = ((CPDSConnectionFactory) manager).getPool();
        }

        PooledConnectionAndInfo info = null;
        try {
            info = pool.borrowObject();
        }
        catch (NoSuchElementException ex) {
            throw new SQLException(
                    "Could not retrieve connection info from pool", ex);
        }
        catch (Exception e) {
            // See if failure is due to CPDSConnectionFactory authentication failure
            try {
                testCPDS(username, password);
            } catch (Exception ex) {
                throw (SQLException) new SQLException(
                        "Could not retrieve connection info from pool").initCause(ex);
            }
            // New password works, so kill the old pool, create a new one, and borrow
            manager.closePool(username);
            synchronized (this) {
                managers.remove(key);
            }
            try {
                registerPool(username, password);
                pool = getPool(key);
            } catch (NamingException ne) {
                throw new SQLException("RegisterPool failed", ne);
            }
            try {
                info = pool.borrowObject();
            } catch (Exception ex) {
                throw (SQLException) new SQLException(
                "Could not retrieve connection info from pool").initCause(ex);
            }
        }
        return info;
    }

    @Override
    protected void setupDefaults(Connection con, String username)
        throws SQLException {
        boolean defaultAutoCommit = isDefaultAutoCommit();
        if (username != null) {
            Boolean userMax = getPerUserDefaultAutoCommit(username);
            if (userMax != null) {
                defaultAutoCommit = userMax.booleanValue();
            }
        }

        boolean defaultReadOnly = isDefaultReadOnly();
        if (username != null) {
            Boolean userMax = getPerUserDefaultReadOnly(username);
            if (userMax != null) {
                defaultReadOnly = userMax.booleanValue();
            }
        }

        int defaultTransactionIsolation = getDefaultTransactionIsolation();
        if (username != null) {
            Integer userMax = getPerUserDefaultTransactionIsolation(username);
            if (userMax != null) {
                defaultTransactionIsolation = userMax.intValue();
            }
        }

        if (con.getAutoCommit() != defaultAutoCommit) {
            con.setAutoCommit(defaultAutoCommit);
        }

        if (defaultTransactionIsolation != UNKNOWN_TRANSACTIONISOLATION) {
            con.setTransactionIsolation(defaultTransactionIsolation);
        }

        if (con.isReadOnly() != defaultReadOnly) {
            con.setReadOnly(defaultReadOnly);
        }
    }

    @Override
    protected PooledConnectionManager getConnectionManager(UserPassKey upkey) {
        return managers.get(
                getPoolKey(upkey.getUsername(), upkey.getPassword()));
    }

    /**
     * Returns a <code>PerUserPoolDataSource</code> {@link Reference}.
     *
     * @since 1.2.2
     */
    @Override
    public Reference getReference() throws NamingException {
        Reference ref = new Reference(getClass().getName(),
                PerUserPoolDataSourceFactory.class.getName(), null);
        ref.add(new StringRefAddr("instanceKey", getInstanceKey()));
        return ref;
    }

    /**
     * Create a pool key from the provided parameters.
     *
     * @param username  User name
     * @param password  Not currently used
     * @return  The pool key
     */
    private PoolKey getPoolKey(String username, String password) {
        return new PoolKey(getDataSourceName(), username);
    }

    private synchronized void registerPool(
        String username, String password)
        throws javax.naming.NamingException, SQLException {

        ConnectionPoolDataSource cpds = testCPDS(username, password);

        Integer userMax = getPerUserMaxTotal(username);
        int maxTotal = (userMax == null) ?
            getDefaultMaxTotal() : userMax.intValue();
        userMax = getPerUserMaxIdle(username);
        int maxIdle =  (userMax == null) ?
            getDefaultMaxIdle() : userMax.intValue();
        userMax = getPerUserMaxWaitMillis(username);
        int maxWaitMillis = (userMax == null) ?
            getDefaultMaxWaitMillis() : userMax.intValue();

        // Set up the factory we will use (passing the pool associates
        // the factory with the pool, so we do not have to do so
        // explicitly)
        CPDSConnectionFactory factory = new CPDSConnectionFactory(cpds,
                getValidationQuery(), isRollbackAfterValidation(), username,
                password);
        factory.setMaxConnLifetimeMillis(getMaxConnLifetimeMillis());

        // Create an object pool to contain our PooledConnections
        GenericObjectPool<PooledConnectionAndInfo> pool =
                new GenericObjectPool<>(factory);
        factory.setPool(pool);
        pool.setMaxTotal(maxTotal);
        pool.setMaxIdle(maxIdle);
        pool.setMaxWaitMillis(maxWaitMillis);
        if (maxTotal <= 0) {
            pool.setBlockWhenExhausted(false);
            pool.setMaxTotal(Integer.MAX_VALUE);
        }
        if (maxWaitMillis == 0) {
            pool.setBlockWhenExhausted(false);
        }

        pool.setTestOnBorrow(getTestOnBorrow());
        pool.setTestOnReturn(getTestOnReturn());
        pool.setTimeBetweenEvictionRunsMillis(
            getTimeBetweenEvictionRunsMillis());
        pool.setNumTestsPerEvictionRun(getNumTestsPerEvictionRun());
        pool.setMinEvictableIdleTimeMillis(getMinEvictableIdleTimeMillis());
        pool.setTestWhileIdle(getTestWhileIdle());
        pool.setSwallowedExceptionListener(new SwallowedExceptionLogger(log));

        Object old = managers.put(getPoolKey(username,password), factory);
        if (old != null) {
            throw new IllegalStateException("Pool already contains an entry for this user/password: "+username);
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
            PerUserPoolDataSource oldDS = (PerUserPoolDataSource)
                new PerUserPoolDataSourceFactory()
                    .getObjectInstance(getReference(), null, null, null);
            this.managers = oldDS.managers;
        }
        catch (NamingException e)
        {
            throw new IOException("NamingException: " + e);
        }
    }

    /**
     * Returns the object pool associated with the given PoolKey.
     *
     * @param key PoolKey identifying the pool
     * @return the GenericObjectPool pooling connections for the username and datasource
     * specified by the PoolKey
     */
    private ObjectPool<PooledConnectionAndInfo> getPool(PoolKey key) {
        CPDSConnectionFactory mgr = (CPDSConnectionFactory) managers.get(key);
        return mgr == null ? null : mgr.getPool();
    }
}
