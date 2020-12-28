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

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.sql.ConnectionPoolDataSource;

import org.apache.commons.pool2.KeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;

/**
 * <p>
 * A pooling <code>DataSource</code> appropriate for deployment within J2EE environment. There are many configuration
 * options, most of which are defined in the parent class. All users (based on user name) share a single maximum number
 * of Connections in this data source.
 * </p>
 *
 * <p>
 * User passwords can be changed without re-initializing the data source. When a
 * <code>getConnection(user name, password)</code> request is processed with a password that is different from those
 * used to create connections in the pool associated with <code>user name</code>, an attempt is made to create a new
 * connection using the supplied password and if this succeeds, idle connections created using the old password are
 * destroyed and new connections are created using the new password.
 * </p>
 *
 * @since 2.0
 */
public class SharedPoolDataSource extends InstanceKeyDataSource {

    private static final long serialVersionUID = -1458539734480586454L;

    // Pool properties
    private int maxTotal = GenericKeyedObjectPoolConfig.DEFAULT_MAX_TOTAL;

    private transient KeyedObjectPool<UserPassKey, PooledConnectionAndInfo> pool;
    private transient KeyedCPDSConnectionFactory factory;

    /**
     * Default no-argument constructor for Serialization
     */
    public SharedPoolDataSource() {
        // empty.
    }

    /**
     * Closes pool being maintained by this data source.
     */
    @Override
    public void close() throws Exception {
        if (pool != null) {
            pool.close();
        }
        InstanceKeyDataSourceFactory.removeInstance(getInstanceKey());
    }

    // -------------------------------------------------------------------
    // Properties

    /**
     * Gets {@link GenericKeyedObjectPool#getMaxTotal()} for this pool.
     *
     * @return {@link GenericKeyedObjectPool#getMaxTotal()} for this pool.
     */
    public int getMaxTotal() {
        return this.maxTotal;
    }

    /**
     * Sets {@link GenericKeyedObjectPool#getMaxTotal()} for this pool.
     *
     * @param maxTotal
     *            {@link GenericKeyedObjectPool#getMaxTotal()} for this pool.
     */
    public void setMaxTotal(final int maxTotal) {
        assertInitializationAllowed();
        this.maxTotal = maxTotal;
    }

    // ----------------------------------------------------------------------
    // Instrumentation Methods

    /**
     * Gets the number of active connections in the pool.
     *
     * @return The number of active connections in the pool.
     */
    public int getNumActive() {
        return pool == null ? 0 : pool.getNumActive();
    }

    /**
     * Gets the number of idle connections in the pool.
     *
     * @return The number of idle connections in the pool.
     */
    public int getNumIdle() {
        return pool == null ? 0 : pool.getNumIdle();
    }

    // ----------------------------------------------------------------------
    // Inherited abstract methods

    @Override
    protected PooledConnectionAndInfo getPooledConnectionAndInfo(final String userName, final String userPassword)
            throws SQLException {

        synchronized (this) {
            if (pool == null) {
                try {
                    registerPool(userName, userPassword);
                } catch (final NamingException e) {
                    throw new SQLException("RegisterPool failed", e);
                }
            }
        }

        PooledConnectionAndInfo info = null;

        final UserPassKey key = new UserPassKey(userName, userPassword);

        try {
            info = pool.borrowObject(key);
        } catch (final Exception e) {
            throw new SQLException("Could not retrieve connection info from pool", e);
        }
        return info;
    }

    @Override
    protected PooledConnectionManager getConnectionManager(final UserPassKey upkey) {
        return factory;
    }

    /**
     * Returns a <code>SharedPoolDataSource</code> {@link Reference}.
     */
    @Override
    public Reference getReference() throws NamingException {
        final Reference ref = new Reference(getClass().getName(), SharedPoolDataSourceFactory.class.getName(), null);
        ref.add(new StringRefAddr("instanceKey", getInstanceKey()));
        return ref;
    }

    private void registerPool(final String userName, final String password) throws NamingException, SQLException {

        final ConnectionPoolDataSource cpds = testCPDS(userName, password);

        // Create an object pool to contain our PooledConnections
        factory = new KeyedCPDSConnectionFactory(cpds, getValidationQuery(), getValidationQueryTimeout(),
                isRollbackAfterValidation());
        factory.setMaxConnLifetimeMillis(getMaxConnLifetimeMillis());

        final GenericKeyedObjectPoolConfig<PooledConnectionAndInfo> config = new GenericKeyedObjectPoolConfig<>();
        config.setBlockWhenExhausted(getDefaultBlockWhenExhausted());
        config.setEvictionPolicyClassName(getDefaultEvictionPolicyClassName());
        config.setLifo(getDefaultLifo());
        config.setMaxIdlePerKey(getDefaultMaxIdle());
        config.setMaxTotal(getMaxTotal());
        config.setMaxTotalPerKey(getDefaultMaxTotal());
        config.setMaxWaitMillis(getDefaultMaxWaitMillis());
        config.setMinEvictableIdleTimeMillis(getDefaultMinEvictableIdleTimeMillis());
        config.setMinIdlePerKey(getDefaultMinIdle());
        config.setNumTestsPerEvictionRun(getDefaultNumTestsPerEvictionRun());
        config.setSoftMinEvictableIdleTimeMillis(getDefaultSoftMinEvictableIdleTimeMillis());
        config.setTestOnCreate(getDefaultTestOnCreate());
        config.setTestOnBorrow(getDefaultTestOnBorrow());
        config.setTestOnReturn(getDefaultTestOnReturn());
        config.setTestWhileIdle(getDefaultTestWhileIdle());
        config.setTimeBetweenEvictionRunsMillis(getDefaultTimeBetweenEvictionRunsMillis());

        final KeyedObjectPool<UserPassKey, PooledConnectionAndInfo> tmpPool = new GenericKeyedObjectPool<>(factory,
                config);
        factory.setPool(tmpPool);
        pool = tmpPool;
    }

    @Override
    protected void setupDefaults(final Connection connection, final String userName) throws SQLException {
        final Boolean defaultAutoCommit = isDefaultAutoCommit();
        if (defaultAutoCommit != null && connection.getAutoCommit() != defaultAutoCommit) {
            connection.setAutoCommit(defaultAutoCommit);
        }

        final int defaultTransactionIsolation = getDefaultTransactionIsolation();
        if (defaultTransactionIsolation != UNKNOWN_TRANSACTIONISOLATION) {
            connection.setTransactionIsolation(defaultTransactionIsolation);
        }

        final Boolean defaultReadOnly = isDefaultReadOnly();
        if (defaultReadOnly != null && connection.isReadOnly() != defaultReadOnly) {
            connection.setReadOnly(defaultReadOnly);
        }
    }

    /**
     * Supports Serialization interface.
     *
     * @param in
     *            a <code>java.io.ObjectInputStream</code> value
     * @throws IOException
     *             if an error occurs
     * @throws ClassNotFoundException
     *             if an error occurs
     */
    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        try {
            in.defaultReadObject();
            final SharedPoolDataSource oldDS = (SharedPoolDataSource) new SharedPoolDataSourceFactory()
                    .getObjectInstance(getReference(), null, null, null);
            this.pool = oldDS.pool;
        } catch (final NamingException e) {
            throw new IOException("NamingException: " + e);
        }
    }

    @Override
    protected void toStringFields(final StringBuilder builder) {
        super.toStringFields(builder);
        builder.append(", maxTotal=");
        builder.append(maxTotal);
    }
}
