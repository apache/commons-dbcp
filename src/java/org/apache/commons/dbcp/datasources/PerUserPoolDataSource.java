/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache" and "Apache Software Foundation" and 
 *    "Apache Turbine" must not be used to endorse or promote products 
 *    derived from this software without prior written permission. For 
 *    written permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    "Apache Turbine", nor may "Apache" appear in their name, without 
 *    prior written permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */
 
package org.apache.commons.dbcp.datasources;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.naming.NamingException;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.DataSource;

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
 * @author <a href="mailto:jmcnally@collab.net">John D. McNally</a>
 * @version $Id: PerUserPoolDataSource.java,v 1.1 2003/08/12 06:09:20 jmcnally Exp $
 */
public class PerUserPoolDataSource
    extends InstanceKeyDataSource {
    private static final Map poolKeys = new HashMap();

    private int defaultMaxActive = GenericObjectPool.DEFAULT_MAX_ACTIVE;
    private int defaultMaxIdle = GenericObjectPool.DEFAULT_MAX_IDLE;
    private int defaultMaxWait = (int)Math.min((long)Integer.MAX_VALUE,
        GenericObjectPool.DEFAULT_MAX_WAIT);
    private Map perUserDefaultAutoCommit = null;    
    private Map perUserMaxActive = null;    
    private Map perUserMaxIdle = null;    
    private Map perUserMaxWait = null;
    private Map perUserDefaultReadOnly = null;    

    private final Map pools = new HashMap();

    /**
     * Default no-arg constructor for Serialization
     */
    public PerUserPoolDataSource() {
    }

    /**
     * Close all pools in the given Map.
     */
    private static void close(Map poolMap) {
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
     * this pool at the same time, or zero for no limit.
     * This value is used for any username which is not specified
     * in perUserMaxConnections.  The default is 0.
     */
    public int getDefaultMaxActive() {
        return (this.defaultMaxActive);
    }

    /**
     * The maximum number of active connections that can be allocated from
     * this pool at the same time, or zero for no limit.
     * This value is used for any username which is not specified
     * in perUserMaxConnections.  The default is 0.
     */
    public void setDefaultMaxActive(int maxActive) {
        assertInitializationAllowed();
        this.defaultMaxActive = maxActive;
    }

    /**
     * The maximum number of active connections that can remain idle in the
     * pool, without extra ones being released, or zero for no limit.
     * This value is used for any username which is not specified
     * in perUserMaxIdle.  The default is 0.
     */
    public int getDefaultMaxIdle() {
        return (this.defaultMaxIdle);
    }

    /**
     * The maximum number of active connections that can remain idle in the
     * pool, without extra ones being released, or zero for no limit.
     * This value is used for any username which is not specified
     * in perUserMaxIdle.  The default is 0.
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
     * The maximum number of active connections that can be allocated from
     * this pool at the same time, or zero for no limit.
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
     * this pool at the same time, or zero for no limit.
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
     * pool, without extra ones being released, or zero for no limit.
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
     * pool, without extra ones being released, or zero for no limit.
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
        ObjectPool pool = (ObjectPool)pools.get(getPoolKey(username));
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
        ObjectPool pool = (ObjectPool)pools.get(getPoolKey(username));
        return (pool == null) ? 0 : pool.getNumIdle();
    }


    // ----------------------------------------------------------------------
    // Inherited abstract methods

    protected synchronized PooledConnectionAndInfo 
        getPooledConnectionAndInfo(String username, String password)
        throws SQLException {

        PoolKey key = getPoolKey(username);
        Object pool = pools.get(key);
        if (pool == null) {
            try {
                registerPool(username, password);
                pool = pools.get(key);
            } catch (NamingException e) {
                throw new SQLNestedException("RegisterPool failed", e);
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

        con.setAutoCommit(defaultAutoCommit);
        con.setReadOnly(defaultReadOnly);
    }

    private PoolKey getPoolKey(String username) {
        PoolKey key = null;

        if (username != null && (perUserMaxActive == null 
                || !perUserMaxActive.containsKey(username))) {
            username = null;
        }

        String dsName = getDataSourceName();
        Map dsMap = (Map) poolKeys.get(dsName);
        if (dsMap != null) {
            key = (PoolKey) dsMap.get(username);
        }
        
        if (key == null) {
            key = new PoolKey(dsName, username);
            if (dsMap == null) {
                dsMap = new HashMap();
                poolKeys.put(dsName, dsMap);
            }
            dsMap.put(username, key);
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
                                  username, password);
           
        pools.put(getPoolKey(username), pool);
    }
}
