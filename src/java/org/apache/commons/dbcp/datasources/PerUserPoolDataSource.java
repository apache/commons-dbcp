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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.sql.ConnectionPoolDataSource;

import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.commons.dbcp.SQLNestedException;

/**
 * <p>
 * A pooling <code>DataSource</code> appropriate for deployment within
 * J2EE environment.  There are many configuration options, most of which are
 * defined in the parent class.  This datasource uses individual pools per 
 * user, and some properties can be set specifically for a given user, if the 
 * deployment environment can support initialization of mapped properties.
 * So for example, a pool of admin or write-access Connections can be
 * guaranteed a certain number of connections, separate from a maximum
 * set for users with read-only connections. 
 * </p>
 *
 * @author John D. McNally
 * @version $Revision$ $Date$
 */
public class PerUserPoolDataSource
    extends InstanceKeyDataSource {

    private static final long serialVersionUID = -3104731034410444060L;

    private static final Map poolKeys = new HashMap();

    private int defaultMaxActive = GenericObjectPool.DEFAULT_MAX_ACTIVE;
    private int defaultMaxIdle = GenericObjectPool.DEFAULT_MAX_IDLE;
    private int defaultMaxWait = (int)Math.min(Integer.MAX_VALUE,
        GenericObjectPool.DEFAULT_MAX_WAIT);
    Map perUserDefaultAutoCommit = null;    
    Map perUserDefaultTransactionIsolation = null;
    Map perUserMaxActive = null;    
    Map perUserMaxIdle = null;    
    Map perUserMaxWait = null;
    Map perUserDefaultReadOnly = null;    

    private transient Map pools = new HashMap();

    /**
     * Default no-arg constructor for Serialization
     */
    public PerUserPoolDataSource() {
    }

    /**
     * Close pool(s) being maintained by this datasource.
     */
    public void close() {
        for (Iterator poolIter = pools.values().iterator();
             poolIter.hasNext();) {    
            try {
                ((ObjectPool) poolIter.next()).close();
            } catch (Exception closePoolException) {
                    //ignore and try to close others.
            }
        }
        InstanceKeyObjectFactory.removeInstance(instanceKey);
    }

    // -------------------------------------------------------------------
    // Properties

    /**
     * The maximum number of active connections that can be allocated from
     * this pool at the same time, or non-positive for no limit.
     * This value is used for any username which is not specified
     * in perUserMaxConnections.
     */
    public int getDefaultMaxActive() {
        return (this.defaultMaxActive);
    }

    /**
     * The maximum number of active connections that can be allocated from
     * this pool at the same time, or non-positive for no limit.
     * This value is used for any username which is not specified
     * in perUserMaxConnections.  The default is 8.
     */
    public void setDefaultMaxActive(int maxActive) {
        assertInitializationAllowed();
        this.defaultMaxActive = maxActive;
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
     * in perUserMaxWait.  The default is -1.
     */
    public int getDefaultMaxWait() {
        return (this.defaultMaxWait);
    }

    /**
     * The maximum number of milliseconds that the pool will wait (when there
     * are no available connections) for a connection to be returned before
     * throwing an exception, or -1 to wait indefinitely.  Will fail 
     * immediately if value is 0.
     * This value is used for any username which is not specified
     * in perUserMaxWait.  The default is -1.
     */
    public void setDefaultMaxWait(int defaultMaxWait) {
        assertInitializationAllowed();
        this.defaultMaxWait = defaultMaxWait;
    }

    /**
     * The keys are usernames and the value is the --.  Any 
     * username specified here will override the value of defaultAutoCommit.
     */
    public Boolean getPerUserDefaultAutoCommit(String key) {
        Boolean value = null;
        if (perUserDefaultAutoCommit != null) {
            value = (Boolean) perUserDefaultAutoCommit.get(key);
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
            perUserDefaultAutoCommit = new HashMap();
        }
        perUserDefaultAutoCommit.put(username, value);
    }

    /**
     * The isolation level of connections when returned from getConnection.  
     * If null, the username will use the value of defaultTransactionIsolation.
     */
    public Integer getPerUserDefaultTransactionIsolation(String username) {
        Integer value = null;
        if (perUserDefaultTransactionIsolation != null) {
            value = (Integer) perUserDefaultTransactionIsolation.get(username);
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
            perUserDefaultTransactionIsolation = new HashMap();
        }
        perUserDefaultTransactionIsolation.put(username, value);
    }

    /**
     * The maximum number of active connections that can be allocated from
     * this pool at the same time, or non-positive for no limit.
     * The keys are usernames and the value is the maximum connections.  Any 
     * username specified here will override the value of defaultMaxActive.
     */
    public Integer getPerUserMaxActive(String username) {
        Integer value = null;
        if (perUserMaxActive != null) {
            value = (Integer) perUserMaxActive.get(username);
        }
        return value;
    }
    
    /**
     * The maximum number of active connections that can be allocated from
     * this pool at the same time, or non-positive for no limit.
     * The keys are usernames and the value is the maximum connections.  Any 
     * username specified here will override the value of defaultMaxActive.
     */
    public void setPerUserMaxActive(String username, Integer value) {
        assertInitializationAllowed();
        if (perUserMaxActive == null) {
            perUserMaxActive = new HashMap();
        }
        perUserMaxActive.put(username, value);
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
            value = (Integer) perUserMaxIdle.get(username);
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
            perUserMaxIdle = new HashMap();
        }
        perUserMaxIdle.put(username, value);
    }
    
    /**
     * The maximum number of milliseconds that the pool will wait (when there
     * are no available connections) for a connection to be returned before
     * throwing an exception, or -1 to wait indefinitely.  Will fail 
     * immediately if value is 0.
     * The keys are usernames and the value is the maximum connections.  Any 
     * username specified here will override the value of defaultMaxWait.
     */
    public Integer getPerUserMaxWait(String username) {
        Integer value = null;
        if (perUserMaxWait != null) {
            value = (Integer) perUserMaxWait.get(username);
        }
        return value;
    }
    
    /**
     * The maximum number of milliseconds that the pool will wait (when there
     * are no available connections) for a connection to be returned before
     * throwing an exception, or -1 to wait indefinitely.  Will fail 
     * immediately if value is 0.
     * The keys are usernames and the value is the maximum connections.  Any 
     * username specified here will override the value of defaultMaxWait.
     */
    public void setPerUserMaxWait(String username, Integer value) {
        assertInitializationAllowed();
        if (perUserMaxWait == null) {
            perUserMaxWait = new HashMap();
        }
        perUserMaxWait.put(username, value);
    }

    /**
     * The keys are usernames and the value is the --.  Any 
     * username specified here will override the value of defaultReadOnly.
     */
    public Boolean getPerUserDefaultReadOnly(String username) {
        Boolean value = null;
        if (perUserDefaultReadOnly != null) {
            value = (Boolean) perUserDefaultReadOnly.get(username);
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
            perUserDefaultReadOnly = new HashMap();
        }
        perUserDefaultReadOnly.put(username, value);
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
        ObjectPool pool = (ObjectPool)pools.get(getPoolKey(username,password));
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
        ObjectPool pool = (ObjectPool)pools.get(getPoolKey(username,password));
        return (pool == null) ? 0 : pool.getNumIdle();
    }


    // ----------------------------------------------------------------------
    // Inherited abstract methods

    protected PooledConnectionAndInfo 
        getPooledConnectionAndInfo(String username, String password)
        throws SQLException {

        PoolKey key = getPoolKey(username,password);
        Object pool = pools.get(key);
        synchronized(this) {
            if (pool == null) {
                try {
                    registerPool(username, password);
                    pool = pools.get(key);
                } catch (NamingException e) {
                    throw new SQLNestedException("RegisterPool failed", e);
                }
            }
        }

        PooledConnectionAndInfo info = null;
        try {
            info = (PooledConnectionAndInfo)((ObjectPool) pool).borrowObject();
        }
        catch (Exception e) {
            throw new SQLNestedException(
                "Could not retrieve connection info from pool", e);
        }
        
        return info;
    }

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

    /**
     * Returns a <code>PerUserPoolDataSource</code> {@link Reference}.
     * 
     * @since 1.2.2
     */
    public Reference getReference() throws NamingException {
        Reference ref = new Reference(getClass().getName(),
                PerUserPoolDataSourceFactory.class.getName(), null);
        ref.add(new StringRefAddr("instanceKey", instanceKey));
        return ref;
    }

    private PoolKey getPoolKey(String username, String password) {
        PoolKey key = null;
        String name = username + password;
        String dsName = getDataSourceName();
        synchronized (poolKeys) {
            Map dsMap = (Map) poolKeys.get(dsName);
            if (dsMap != null) {
                key = (PoolKey) dsMap.get(name);
            }

            if (key == null) {
                key = new PoolKey(dsName, name);
                if (dsMap == null) {
                    dsMap = new HashMap();
                    poolKeys.put(dsName, dsMap);
                }
                dsMap.put(name, key);
            }
        }
        return key;
    }

    private synchronized void registerPool(
        String username, String password) 
        throws javax.naming.NamingException, SQLException {

        ConnectionPoolDataSource cpds = testCPDS(username, password);

        Integer userMax = getPerUserMaxActive(username);
        int maxActive = (userMax == null) ? 
            getDefaultMaxActive() : userMax.intValue();
        userMax = getPerUserMaxIdle(username);
        int maxIdle =  (userMax == null) ?
            getDefaultMaxIdle() : userMax.intValue();
        userMax = getPerUserMaxWait(username);
        int maxWait = (userMax == null) ?
            getDefaultMaxWait() : userMax.intValue();

        // Create an object pool to contain our PooledConnections
        GenericObjectPool pool = new GenericObjectPool(null);
        pool.setMaxActive(maxActive);
        pool.setMaxIdle(maxIdle);
        pool.setMaxWait(maxWait);
        pool.setWhenExhaustedAction(whenExhaustedAction(maxActive, maxWait));
        pool.setTestOnBorrow(getTestOnBorrow());
        pool.setTestOnReturn(getTestOnReturn());
        pool.setTimeBetweenEvictionRunsMillis(
            getTimeBetweenEvictionRunsMillis());
        pool.setNumTestsPerEvictionRun(getNumTestsPerEvictionRun());
        pool.setMinEvictableIdleTimeMillis(getMinEvictableIdleTimeMillis());
        pool.setTestWhileIdle(getTestWhileIdle());
                
        // Set up the factory we will use (passing the pool associates
        // the factory with the pool, so we do not have to do so
        // explicitly)
        new CPDSConnectionFactory(cpds, pool, getValidationQuery(),
                                  isRollbackAfterValidation(), 
                                  username, password);
           
        pools.put(getPoolKey(username,password), pool);
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
            this.pools = oldDS.pools;
        }
        catch (NamingException e)
        {
            throw new IOException("NamingException: " + e);
        }
    }
}
