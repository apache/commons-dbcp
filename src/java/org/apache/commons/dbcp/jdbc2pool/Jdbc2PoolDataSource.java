package org.apache.commons.dbcp.jdbc2pool;

/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
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
 
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import java.util.Hashtable;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.PooledConnection;
import javax.naming.Name;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Referenceable;
import javax.naming.Reference;
import javax.naming.RefAddr;
import javax.naming.BinaryRefAddr;
import javax.naming.StringRefAddr;
import javax.naming.NamingException;
import javax.naming.spi.ObjectFactory;

import org.apache.commons.collections.FastHashMap;
import org.apache.commons.lang.SerializationUtils;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.commons.pool.KeyedObjectPool;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.apache.commons.pool.impl.StackObjectPool;

/**
 * <p>
 * A pooling <code>DataSource</code> appropriate for deployment within
 * J2EE environment.  There are many configuration options.  Multiple users
 * can share a common set of parameters, such as a single maximum number
 * of Connections.  The pool can also support individual pools per user, if the
 * deployment environment can support initialization of mapped properties.
 * So for example, a pool of admin or write-access Connections can be
 * guaranteed a certain number of connections, separate from a maximum
 * set for read-only connections. 
 * </p>
 *
 * <p>
 * A J2EE container will normally provide some method of initializing the
 * <code>DataSource</code> whose attributes are presented
 * as bean getters/setters and then deploying it via JNDI.  It is then
 * available to an application as a source of pooled logical connections to 
 * the database.  The pool needs a source of physical connections.  This
 * source is in the form of a <code>ConnectionPoolDataSource</code> that
 * can be specified via the {@link #setDataSourceName(String)} used to
 * lookup the source via JNDI.
 * </p>
 *
 * <p>
 * Although normally used within a JNDI environment, Jdbc2PoolDataSource
 * can be instantiated and initialized as any bean.  In this case the 
 * <code>ConnectionPoolDataSource</code> will likely be instantiated in
 * a similar manner.  The source can then be attached directly to this
 * pool using the 
 * {@link #setConnectionPoolDataSource(ConnectionPoolDataSource)} method.
 * </p>
 *
 * <p>
 * If this <code>DataSource</code> 
 * is requested via JNDI multiple times, it maintains
 * state between lookups.  Also, multiple instances can be deployed using 
 * different backend <code>ConnectionPoolDataSource</code> sources.  
 * </p>
 *
 * <p>
 * The dbcp package contains an adapter, 
 * {@link org.apache.commons.dbcp.cpdsadapter.DriverAdapterCPDS},
 * that can be used to allow the
 * use of Jdbc2PoolDataSource with jdbc driver implementations that
 * do not supply a <code>ConnectionPoolDataSource</code>, but still
 * provide a {@link java.sql.Driver} implementation.
 * </p>
 *
 * <p>
 * The <a href="package-summary.html">package documentation</a> contains an 
 * example using catalina and JNDI and it also contains a non-JNDI example. 
 * </p>
 *
 * @author <a href="mailto:jmcnally@collab.net">John D. McNally</a>
 * @version $Id: Jdbc2PoolDataSource.java,v 1.4 2002/11/07 21:28:57 rwaldhoff Exp $
 */
public class Jdbc2PoolDataSource
    implements DataSource, Referenceable, Serializable, ObjectFactory
{
    private static final String GET_CONNECTION_CALLED = 
        "A Connection was already requested from this source, " + 
        "further initialization is not allowed.";

    private static Map dsInstanceMap = new HashMap();

    private static ObjectPool userPassKeyPool = 
        new StackObjectPool(new UserPassKey.Factory(), 256);

    private static ObjectPool poolKeyPool = 
        new StackObjectPool(new PoolKey.Factory(), 256);

    private boolean getConnectionCalled = false;

    private ConnectionPoolDataSource cpds = null;
    /** DataSource Name used to find the ConnectionPoolDataSource */
    private String dataSourceName = null;
    private boolean defaultAutoCommit = false;
    private int defaultMaxActive = GenericObjectPool.DEFAULT_MAX_ACTIVE;
    private int defaultMaxIdle = GenericObjectPool.DEFAULT_MAX_IDLE;
    private int defaultMaxWait = 
        (((long)Integer.MAX_VALUE) < GenericObjectPool.DEFAULT_MAX_WAIT) ?
        (int)(GenericObjectPool.DEFAULT_MAX_WAIT) :
        Integer.MAX_VALUE;
    private boolean defaultReadOnly = false;
    /** Description */
    private String description = null;
    /** Environment that may be used to set up a jndi initial context. */
    private Properties jndiEnvironment = null;
    /** Login TimeOut in seconds */
    private int loginTimeout = 0;
    /** Log stream */
    private PrintWriter logWriter = null;
    private Map perUserDefaultAutoCommit = null;    
    private Map perUserMaxActive = null;    
    private Map perUserMaxIdle = null;    
    private Map perUserMaxWait = null;
    private Map perUserDefaultReadOnly = null;    
    private boolean _testOnBorrow = GenericObjectPool.DEFAULT_TEST_ON_BORROW;
    private boolean _testOnReturn = GenericObjectPool.DEFAULT_TEST_ON_RETURN;
    private int _timeBetweenEvictionRunsMillis = 
        (((long)Integer.MAX_VALUE) < GenericObjectPool.DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS) ?
        (int)(GenericObjectPool.DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS) :
        Integer.MAX_VALUE;;
    private int _numTestsPerEvictionRun = GenericObjectPool.DEFAULT_NUM_TESTS_PER_EVICTION_RUN;
    private int _minEvictableIdleTimeMillis =
        (((long)Integer.MAX_VALUE) < GenericObjectPool.DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS) ?
        (int)(GenericObjectPool.DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS) :
        Integer.MAX_VALUE;;    
    private boolean _testWhileIdle = GenericObjectPool.DEFAULT_TEST_WHILE_IDLE;
    private String validationQuery = null;
    private boolean testPositionSet = false;

    private boolean isNew = false;
    private Integer instanceKey = null;

    /**
     * Default no-arg constructor for Serialization
     */
    public Jdbc2PoolDataSource() 
    {
        isNew = true;
        defaultAutoCommit = true;
    }

    /**
     * Throws an IllegalStateException, if a PooledConnection has already
     * been requested.
     */
    private void assertInitializationAllowed()
        throws IllegalStateException 
    {
        if (getConnectionCalled) 
        {
            throw new IllegalStateException(GET_CONNECTION_CALLED);
        }
    }

    /**
     * Close all pools associated with this class.
     */
    public static void closeAll() 
    {
        //Get iterator to loop over all instances of this datasource.
        Iterator instanceIterator = dsInstanceMap.entrySet().iterator();
        
        while (instanceIterator.hasNext()) 
        {        
            Map.Entry nextInstance = (Map.Entry) instanceIterator.next();
            Map nextPoolMap = (Map) nextInstance.getValue();
            close(nextPoolMap);
        }
        dsInstanceMap.clear();
    }

    /**
     * Close all pools in the given Map.
     */
    private static void close(Map poolMap) 
    {
        //Get iterator to loop over all pools.
        Iterator poolIter = poolMap.entrySet().iterator();
        
        while (poolIter.hasNext()) 
        {    
            Map.Entry nextPoolEntry = (Map.Entry) poolIter.next();
            
            if (nextPoolEntry.getValue() instanceof ObjectPool) 
            {
                ObjectPool nextPool = (ObjectPool) nextPoolEntry.getValue();
                try 
                {
                    nextPool.close();
                } 
                catch (Exception closePoolException) 
                {
                    //ignore and try to close others.
                }
            } 
            else 
            {
                KeyedObjectPool nextPool = 
                    (KeyedObjectPool) nextPoolEntry.getValue();
                try {
                    nextPool.close();
                } 
                catch (Exception closePoolException) 
                {
                    //ignore and try to close others.
                }                                               
            }
        }
    }

    /**
     * Close pool(s) being maintained by this datasource.
     */
    public void close() 
    {
        close((Map)dsInstanceMap.get(instanceKey));
    }

    // -------------------------------------------------------------------
    // Properties

    /**
     * Get the value of connectionPoolDataSource.  This method will return
     * null, if the backing datasource is being accessed via jndi.
     *
     * @return value of connectionPoolDataSource.
     */
    public ConnectionPoolDataSource getConnectionPoolDataSource() 
    {
        return cpds;
    }
    
    /**
     * Set the backend ConnectionPoolDataSource.  This property should not be
     * set if using jndi to access the datasource.
     *
     * @param v  Value to assign to connectionPoolDataSource.
     */
    public void setConnectionPoolDataSource(ConnectionPoolDataSource  v) 
    {
        assertInitializationAllowed();
        if (dataSourceName != null) 
        {
            throw new IllegalStateException(
                "Cannot set the DataSource, if JNDI is used.");
        }
        this.cpds = v;
        if (isNew) 
        {
            registerInstance();
        }
    }

    /**
     * Get the name of the ConnectionPoolDataSource which backs this pool.
     * This name is used to look up the datasource from a jndi service 
     * provider.
     *
     * @return value of dataSourceName.
     */
    public String getDataSourceName() 
    {
        return dataSourceName;
    }
    
    /**
     * Set the name of the ConnectionPoolDataSource which backs this pool.
     * This name is used to look up the datasource from a jndi service 
     * provider.
     *
     * @param v  Value to assign to dataSourceName.
     */
    public void setDataSourceName(String  v) 
    {
        assertInitializationAllowed();
        if (cpds != null) 
        {
            throw new IllegalStateException(
                "Cannot set the JNDI name for the DataSource, if already " +
                "set using setConnectionPoolDataSource.");
        }
        this.dataSourceName = v;
        if (isNew) 
        {
            registerInstance();
        }
    }

    
    /** 
     * Get the value of defaultAutoCommit, which defines the state of 
     * connections handed out from this pool.  The value can be changed
     * on the Connection using Connection.setAutoCommit(boolean).
     * The default is true.
     *
     * @return value of defaultAutoCommit.
     */
    public boolean isDefaultAutoCommit() 
    {
        return defaultAutoCommit;
    }
    
    /**
     * Set the value of defaultAutoCommit, which defines the state of 
     * connections handed out from this pool.  The value can be changed
     * on the Connection using Connection.setAutoCommit(boolean).
     * The default is true.
     *
     * @param v  Value to assign to defaultAutoCommit.
     */
    public void setDefaultAutoCommit(boolean  v) 
    {
        assertInitializationAllowed();
        this.defaultAutoCommit = v;
    }


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
     * Get the value of defaultReadOnly, which defines the state of 
     * connections handed out from this pool.  The value can be changed
     * on the Connection using Connection.setReadOnly(boolean).
     * The default is false.
     *
     * @return value of defaultReadOnly.
     */
    public boolean isDefaultReadOnly() 
    {
        return defaultReadOnly;
    }
    
    /**
     * Set the value of defaultReadOnly, which defines the state of 
     * connections handed out from this pool.  The value can be changed
     * on the Connection using Connection.setReadOnly(boolean).
     * The default is false.
     *
     * @param v  Value to assign to defaultReadOnly.
     */
    public void setDefaultReadOnly(boolean  v) 
    {
        assertInitializationAllowed();
        this.defaultReadOnly = v;
    }

    
    /**
     * Get the description.  This property is defined by jdbc as for use with
     * GUI (or other) tools that might deploy the datasource.  It serves no
     * internal purpose.
     *
     * @return value of description.
     */
    public String getDescription() 
    {
        return description;
    }
    
    /**
     * Set the description.  This property is defined by jdbc as for use with
     * GUI (or other) tools that might deploy the datasource.  It serves no
     * internal purpose.
     * 
     * @param v  Value to assign to description.
     */
    public void setDescription(String  v) 
    {
        this.description = v;
    }
        

    /**
     * Get the value of jndiEnvironment which is used when instantiating
     * a jndi InitialContext.  This InitialContext is used to locate the
     * backend ConnectionPoolDataSource.
     *
     * @return value of jndiEnvironment.
     */
    public String getJndiEnvironment(String key) 
    {
        String value = null;
        if (jndiEnvironment != null) 
        {
            value = jndiEnvironment.getProperty(key);
        }
        return value;
    }
    
    /**
     * Set the value of jndiEnvironment which is used when instantiating
     * a jndi InitialContext.  This InitialContext is used to locate the
     * backend ConnectionPoolDataSource.
     *
     * @param v  Value to assign to jndiEnvironment.
     */
    public void setJndiEnvironment(String key, String value) 
    {
        if (jndiEnvironment == null) 
        {
            jndiEnvironment = new Properties();
        }
        jndiEnvironment.setProperty(key, value);
    }

    
    /**
     * Get the value of loginTimeout.
     * @return value of loginTimeout.
     */
    public int getLoginTimeout() 
    {
        return loginTimeout;
    }
    
    /**
     * Set the value of loginTimeout.
     * @param v  Value to assign to loginTimeout.
     */
    public void setLoginTimeout(int  v) 
    {
        this.loginTimeout = v;
    }
    
    
    /**
     * Get the value of logWriter.
     * @return value of logWriter.
     */
    public PrintWriter getLogWriter() 
    {
        if (logWriter == null) 
        {
            logWriter = new PrintWriter(System.out);
        }        
        return logWriter;
    }
    
    /**
     * Set the value of logWriter.
     * @param v  Value to assign to logWriter.
     */
    public void setLogWriter(PrintWriter  v) 
    {
        this.logWriter = v;
    }
    

    /**
     * The keys are usernames and the value is the --.  Any 
     * username specified here will override the value of defaultAutoCommit.
     */
    public Boolean getPerUserDefaultAutoCommit(String key) 
    {
        Boolean value = null;
        if (perUserDefaultAutoCommit != null) 
        {
            value = (Boolean)perUserDefaultAutoCommit.get(key);
        }
        return value;
    }
    
    /**
     * The keys are usernames and the value is the --.  Any 
     * username specified here will override the value of defaultAutoCommit.
     */
    public void setPerUserDefaultAutoCommit(String username, Boolean value) 
    {
        assertInitializationAllowed();
        if (perUserDefaultAutoCommit == null) 
        {
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
    public Integer getPerUserMaxActive(String username) 
    {
        Integer value = null;
        if (perUserMaxActive != null) 
        {
            value = (Integer)perUserMaxActive.get(username);
        }
        return value;
    }
    
    /**
     * The maximum number of active connections that can be allocated from
     * this pool at the same time, or zero for no limit.
     * The keys are usernames and the value is the maximum connections.  Any 
     * username specified here will override the value of defaultMaxActive.
     */
    public void setPerUserMaxActive(String username, Integer value) 
    {
        assertInitializationAllowed();
        if (perUserMaxActive == null) 
        {
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
    public Integer getPerUserMaxIdle(String username) 
    {
        Integer value = null;
        if (perUserMaxIdle != null) 
        {
            value = (Integer)perUserMaxIdle.get(username);
        }
        return value;
    }
    
    /**
     * The maximum number of active connections that can remain idle in the
     * pool, without extra ones being released, or zero for no limit.
     * The keys are usernames and the value is the maximum connections.  Any 
     * username specified here will override the value of defaultMaxIdle.
     */
    public void setPerUserMaxIdle(String username, Integer value) 
    {
        assertInitializationAllowed();
        if (perUserMaxIdle == null) 
        {
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
    public Integer getPerUserMaxWait(String username) 
    {
        Integer value = null;
        if (perUserMaxWait != null) 
        {
            value = (Integer)perUserMaxWait.get(username);
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
    public void setPerUserMaxWait(String username, Integer value) 
    {
        assertInitializationAllowed();
        if (perUserMaxWait == null) 
        {
            perUserMaxWait = new HashMap();
        }
        perUserMaxWait.put(username, value);
    }


    /**
     * The keys are usernames and the value is the --.  Any 
     * username specified here will override the value of defaultReadOnly.
     */
    public Boolean getPerUserDefaultReadOnly(String username) 
    {
        Boolean value = null;
        if (perUserDefaultReadOnly != null) 
        {
            value = (Boolean)perUserDefaultReadOnly.get(username);
        }
        return value;
    }
    
    /**
     * The keys are usernames and the value is the --.  Any 
     * username specified here will override the value of defaultReadOnly.
     */
    public void setPerUserDefaultReadOnly(String username, Boolean value) 
    {
        assertInitializationAllowed();
        if (perUserDefaultReadOnly == null) 
        {
            perUserDefaultReadOnly = new HashMap();
        }
        perUserDefaultReadOnly.put(username, value);
    }


    /**
     * @see #getTestOnBorrow
     */
    final public boolean isTestOnBorrow() {
        return getTestOnBorrow();
    }
    
    /**
     * When <tt>true</tt>, objects will be
     * {*link PoolableObjectFactory#validateObject validated}
     * before being returned by the {*link #borrowObject}
     * method.  If the object fails to validate,
     * it will be dropped from the pool, and we will attempt
     * to borrow another.
     *
     * *see #setTestOnBorrow
     */
    public boolean getTestOnBorrow() {
        return _testOnBorrow;
    }

    /**
     * When <tt>true</tt>, objects will be
     * {*link PoolableObjectFactory#validateObject validated}
     * before being returned by the {*link #borrowObject}
     * method.  If the object fails to validate,
     * it will be dropped from the pool, and we will attempt
     * to borrow another.
     *
     * *see #getTestOnBorrow
     */
    public void setTestOnBorrow(boolean testOnBorrow) {
        assertInitializationAllowed();
        _testOnBorrow = testOnBorrow;
        testPositionSet = true;
    }

    /**
     * @see #getTestOnReturn
     */
    final public boolean isTestOnReturn() {
        return getTestOnReturn();
    }
    
    /**
     * When <tt>true</tt>, objects will be
     * {*link PoolableObjectFactory#validateObject validated}
     * before being returned to the pool within the
     * {*link #returnObject}.
     *
     * *see #setTestOnReturn
     */
    public boolean getTestOnReturn() {
        return _testOnReturn;
    }

    /**
     * When <tt>true</tt>, objects will be
     * {*link PoolableObjectFactory#validateObject validated}
     * before being returned to the pool within the
     * {*link #returnObject}.
     *
     * *see #getTestOnReturn
     */
    public void setTestOnReturn(boolean testOnReturn) {
        assertInitializationAllowed();
        _testOnReturn = testOnReturn;
        testPositionSet = true;
    }

    /**
     * Returns the number of milliseconds to sleep between runs of the
     * idle object evictor thread.
     * When non-positive, no idle object evictor thread will be
     * run.
     *
     * *see #setTimeBetweenEvictionRunsMillis
     */
    public int getTimeBetweenEvictionRunsMillis() {
        return _timeBetweenEvictionRunsMillis;
    }

    /**
     * Sets the number of milliseconds to sleep between runs of the
     * idle object evictor thread.
     * When non-positive, no idle object evictor thread will be
     * run.
     *
     * *see #getTimeBetweenEvictionRunsMillis
     */
    public void 
        setTimeBetweenEvictionRunsMillis(int timeBetweenEvictionRunsMillis) {
        assertInitializationAllowed();
            _timeBetweenEvictionRunsMillis = timeBetweenEvictionRunsMillis;
    }

    /**
     * Returns the number of objects to examine during each run of the
     * idle object evictor thread (if any).
     *
     * *see #setNumTestsPerEvictionRun
     * *see #setTimeBetweenEvictionRunsMillis
     */
    public int getNumTestsPerEvictionRun() {
        return _numTestsPerEvictionRun;
    }

    /**
     * Sets the number of objects to examine during each run of the
     * idle object evictor thread (if any).
     * <p>
     * When a negative value is supplied, <tt>ceil({*link #numIdle})/abs({*link #getNumTestsPerEvictionRun})</tt>
     * tests will be run.  I.e., when the value is <i>-n</i>, roughly one <i>n</i>th of the
     * idle objects will be tested per run.
     *
     * *see #getNumTestsPerEvictionRun
     * *see #setTimeBetweenEvictionRunsMillis
     */
    public void setNumTestsPerEvictionRun(int numTestsPerEvictionRun) {
        assertInitializationAllowed();
        _numTestsPerEvictionRun = numTestsPerEvictionRun;
    }

    /**
     * Returns the minimum amount of time an object may sit idle in the pool
     * before it is eligable for eviction by the idle object evictor
     * (if any).
     *
     * *see #setMinEvictableIdleTimeMillis
     * *see #setTimeBetweenEvictionRunsMillis
     */
    public int getMinEvictableIdleTimeMillis() {
        return _minEvictableIdleTimeMillis;
    }

    /**
     * Sets the minimum amount of time an object may sit idle in the pool
     * before it is eligable for eviction by the idle object evictor
     * (if any).
     * When non-positive, no objects will be evicted from the pool
     * due to idle time alone.
     *
     * *see #getMinEvictableIdleTimeMillis
     * *see #setTimeBetweenEvictionRunsMillis
     */
    public void 
        setMinEvictableIdleTimeMillis(int minEvictableIdleTimeMillis) {
        assertInitializationAllowed();
        _minEvictableIdleTimeMillis = minEvictableIdleTimeMillis;
    }

    /**
     * @see #getTestWhileIdle
     */
    final public boolean isTestWhileIdle() {
        return getTestWhileIdle();
    }
    /**
     * When <tt>true</tt>, objects will be
     * {*link PoolableObjectFactory#validateObject validated}
     * by the idle object evictor (if any).  If an object
     * fails to validate, it will be dropped from the pool.
     *
     * *see #setTestWhileIdle
     * *see #setTimeBetweenEvictionRunsMillis
     */
    public boolean getTestWhileIdle() {
        return _testWhileIdle;
    }

    /**
     * When <tt>true</tt>, objects will be
     * {*link PoolableObjectFactory#validateObject validated}
     * by the idle object evictor (if any).  If an object
     * fails to validate, it will be dropped from the pool.
     *
     * *see #getTestWhileIdle
     * *see #setTimeBetweenEvictionRunsMillis
     */
    public void setTestWhileIdle(boolean testWhileIdle) {
        assertInitializationAllowed();
        _testWhileIdle = testWhileIdle;
        testPositionSet = true;
    }


    /**
     * The SQL query that will be used to validate connections from this pool
     * before returning them to the caller.  If specified, this query
     * <strong>MUST</strong> be an SQL SELECT statement that returns at least
     * one row.
     */
    public String getValidationQuery() {
        return (this.validationQuery);
    }

    /**
     * The SQL query that will be used to validate connections from this pool
     * before returning them to the caller.  If specified, this query
     * <strong>MUST</strong> be an SQL SELECT statement that returns at least
     * one row.  Default behavior is to test the connection when it is
     * borrowed.
     */
    public void setValidationQuery(String validationQuery) {
        assertInitializationAllowed();
        this.validationQuery = validationQuery;
        if (!testPositionSet) 
        {
            setTestOnBorrow(true);
        }
    }

    // ----------------------------------------------------------------------
    // Instrumentation Methods

    /**
     * Get the number of active connections in the default pool.
     */
    public int getNumActive()
    {
        return getNumActive( null, null );
    }

    /**
     * Get the number of active connections in the pool for a given user.
     */
    public int getNumActive( String username, String password )
    {
        PoolKey key = getPoolKey( username );

        Object pool = ( ( Map ) dsInstanceMap.get( instanceKey ) ).get( key );

        if ( pool instanceof ObjectPool )
        {
            return ( ( ObjectPool ) pool ).getNumActive();
        }
        else
        {
            return ( ( KeyedObjectPool ) pool ).getNumActive();
        }
    }

    /**
     * Get the number of idle connections in the default pool.
     */
    public int getNumIdle()
    {
        return getNumIdle( null, null );
    }

    /**
     * Get the number of idle connections in the pool for a given user.
     */
    public int getNumIdle( String username, String password )
    {
        PoolKey key = getPoolKey( username );

        Object pool = ( ( Map ) dsInstanceMap.get( instanceKey ) ).get( key );

        if ( pool instanceof ObjectPool )
        {
            return ( ( ObjectPool ) pool ).getNumIdle();
        }
        else
        {
            return ( ( KeyedObjectPool ) pool ).getNumIdle();
        }
    }

    // ----------------------------------------------------------------------
    // DataSource implementation 

    /**
     * Attempt to establish a database connection.
     */
    public Connection getConnection() 
        throws SQLException
    {
        return getConnection(null, null);
    }

    /**
     * Attempt to establish a database connection.
     */
    synchronized public Connection getConnection(String username, String password)
        throws SQLException
    {
        if (isNew) 
        {
            throw new SQLException("Must set the ConnectionPoolDataSource " + 
                "through setDataSourceName or setConnectionPoolDataSource " + 
                "before calling getConnection.");
        }
        getConnectionCalled = true;
        Map pools = (Map)dsInstanceMap.get(instanceKey);
        PoolKey key = getPoolKey(username);
        Object pool = pools.get(key);
        if ( pool == null ) 
        {
            try
            {
                registerPool(username, password);
                pool = pools.get(key);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                throw new SQLException(e.getMessage());
            }
        }
        returnPoolKey(key);

        PooledConnection pc = null;
        if (pool instanceof ObjectPool) 
        {
            try
            {
                pc = (PooledConnection)((ObjectPool)pool).borrowObject();
            }
            catch (Exception e)
            {
                if (e instanceof RuntimeException) 
                {
                    throw (RuntimeException)e;
                }
                else 
                {
                    throw new SQLException(e.getMessage());
                }
            }
        }
        else // assume KeyedObjectPool
        { 
            try
            {
                UserPassKey upkey = getPCKey(username, password);
                pc = (PooledConnection)
                    ((KeyedObjectPool)pool).borrowObject(upkey);
                returnPCKey(upkey);
            }
            catch (Exception e)
            {
                if (e instanceof RuntimeException) 
                {
                    throw (RuntimeException)e;
                }
                else 
                {
                    throw new SQLException(e.getMessage());
                }
            }
        }
        
        boolean defaultAutoCommit = isDefaultAutoCommit();
        if ( username != null ) 
        {
            Boolean userMax = 
                getPerUserDefaultAutoCommit(username);
            if ( userMax != null ) 
            {
                defaultAutoCommit = userMax.booleanValue();
            }
        }    

        boolean defaultReadOnly = isDefaultReadOnly();
        if ( username != null ) 
        {
            Boolean userMax = 
                getPerUserDefaultReadOnly(username);
            if ( userMax != null ) 
            {
                defaultReadOnly = userMax.booleanValue();
            }
        }    

        Connection con = pc.getConnection();        
        con.setAutoCommit(defaultAutoCommit);
        con.setReadOnly(defaultReadOnly);
        return con;
    }

    private UserPassKey getPCKey(String username, String password)
    {
        UserPassKey upk = null;
        try
        {
            upk = (UserPassKey)userPassKeyPool.borrowObject();
        }
        catch (Exception e)
        {
            getLogWriter().println("[WARN] Jdbc2PoolDataSource::getPCKey"
                + " could not get key from pool. Created a new instance. "
                + e.getMessage());
            upk = new UserPassKey();
        }
        upk.init(username, password);
        return upk;
    }

    private void returnPCKey(UserPassKey key)
    {
        if (key.isReusable()) 
        {
            try
            {
                userPassKeyPool.returnObject(key);
            }
            catch (Exception e)
            {
                getLogWriter().println(
                    "[WARN] Jdbc2PoolDataSource::returnPCKey could not return"
                    + " key to pool. " + e.getMessage());
            }
        }
    }

    private PoolKey getPoolKey(String username)
    {
        PoolKey key = null;
        try
        {
            key = (PoolKey)poolKeyPool.borrowObject();
        }
        catch (Exception e)
        {
            getLogWriter().println("[WARN] Jdbc2PoolDataSource::getPoolKey"
                + " could not get key from pool. Created a new instance. "
                + e.getMessage());
            key = new PoolKey();
        }
        if ( username != null && 
             (perUserMaxActive == null 
              || !perUserMaxActive.containsKey(username)) ) 
        {
            username = null;
        }
        key.init(getDataSourceName(), username);
        return key;
    }

    private void returnPoolKey(PoolKey key)
    {
        try
        {
            poolKeyPool.returnObject(key);
        }
        catch (Exception e)
        {
            getLogWriter().println(
                "[WARN] Jdbc2PoolDataSource::returnPoolKey could not return"
                + " key to pool. " + e.getMessage());
        }
    }

    synchronized private void registerInstance()
    {
        if (isNew) 
        {
            int max = 0;
            Iterator i = dsInstanceMap.keySet().iterator();
            while (i.hasNext()) 
            {
                int key = ((Integer)i.next()).intValue();
                max = Math.max(max, key);
            }
            instanceKey = new Integer(max+1);
            FastHashMap fhm = new FastHashMap();
            fhm.setFast(true);
            dsInstanceMap.put(instanceKey, fhm);
            isNew = false;
        }
    }

    synchronized private void registerPool(String username, String password)
         throws javax.naming.NamingException
    {
        Map pools = (Map)dsInstanceMap.get(instanceKey);
        PoolKey key = getPoolKey(username);
        if ( !pools.containsKey(key) ) 
        {
            int maxActive = getDefaultMaxActive();
            int maxIdle = getDefaultMaxIdle();
            int maxWait = getDefaultMaxWait();

            // The source of physical db connections
            ConnectionPoolDataSource cpds = this.cpds;
            if ( cpds == null ) 
            {            
                Context ctx = null;
                if ( jndiEnvironment == null ) 
                {
                    ctx = new InitialContext();                
                }
                else 
                {
                    ctx = new InitialContext(jndiEnvironment);
                }
                cpds = (ConnectionPoolDataSource)ctx.lookup(dataSourceName);
            }

            Object whicheverPool = null;
            if (perUserMaxActive != null 
                && perUserMaxActive.containsKey(username)) 
            {                
                Integer userMax = getPerUserMaxActive(username);
                if ( userMax != null ) 
                {
                    maxActive = userMax.intValue();
                }
                userMax = getPerUserMaxIdle(username);
                if ( userMax != null ) 
                {
                    maxIdle = userMax.intValue();
                }
                userMax = getPerUserMaxWait(username);
                if ( userMax != null ) 
                {
                    maxWait = userMax.intValue();
                }

                // Create an object pool to contain our PooledConnections
                GenericObjectPool pool = new GenericObjectPool(null);
                pool.setMaxActive(maxActive);
                pool.setMaxIdle(maxIdle);
                pool.setMaxWait(maxWait);
                pool.setWhenExhaustedAction(
                    getWhenExhausted(maxActive, maxWait));
                pool.setTestOnBorrow(getTestOnBorrow());
                pool.setTestOnReturn(getTestOnReturn());
                pool.setTimeBetweenEvictionRunsMillis(
                    getTimeBetweenEvictionRunsMillis());
                pool.setNumTestsPerEvictionRun(getNumTestsPerEvictionRun());
                pool.setMinEvictableIdleTimeMillis(
                    getMinEvictableIdleTimeMillis());
                pool.setTestWhileIdle(getTestWhileIdle());
                
                // Set up the factory we will use (passing the pool associates
                // the factory with the pool, so we do not have to do so
                // explicitly)
                new CPDSConnectionFactory(cpds, pool, validationQuery,
                                          username, password);
                whicheverPool = pool;
            }
            else // use default pool
            {
                // Create an object pool to contain our PooledConnections
                GenericKeyedObjectPool pool = new GenericKeyedObjectPool(null);
                pool.setMaxActive(maxActive);
                pool.setMaxIdle(maxIdle);
                pool.setMaxWait(maxWait);
                pool.setWhenExhaustedAction(
                    getWhenExhausted(maxActive, maxWait));
                pool.setTestOnBorrow(getTestOnBorrow());
                pool.setTestOnReturn(getTestOnReturn());
                pool.setTimeBetweenEvictionRunsMillis(
                    getTimeBetweenEvictionRunsMillis());
                pool.setNumTestsPerEvictionRun(getNumTestsPerEvictionRun());
                pool.setMinEvictableIdleTimeMillis(
                    getMinEvictableIdleTimeMillis());
                pool.setTestWhileIdle(getTestWhileIdle());
                
                // Set up the factory we will use (passing the pool associates
                // the factory with the pool, so we do not have to do so
                // explicitly)
                new KeyedCPDSConnectionFactory(cpds, pool, validationQuery);
                whicheverPool = pool;
            }
            
            // pools is a FastHashMap set to put the pool in a thread-safe way
            pools.put(key, whicheverPool);
        }        
    }

    private byte getWhenExhausted(int maxActive, int maxWait)
    {
        byte whenExhausted = GenericObjectPool.WHEN_EXHAUSTED_BLOCK;
        if (maxActive <= 0) 
        {
            whenExhausted = GenericObjectPool.WHEN_EXHAUSTED_GROW;
        }
        else if (maxWait == 0) 
        {
            whenExhausted = GenericObjectPool.WHEN_EXHAUSTED_FAIL;
        }
        return whenExhausted;
    }    

    // ----------------------------------------------------------------------
    // Referenceable implementation 

    /**
     * <CODE>Referenceable</CODE> implementation prepares object for
     * binding in jndi.
     */
    public Reference getReference() 
        throws NamingException
    {
        // this class implements its own factory
        String factory = getClass().getName();
        Reference ref = new Reference(getClass().getName(), factory, null);

        ref.add(new StringRefAddr("isNew", 
                                  String.valueOf(isNew)));
        ref.add(new StringRefAddr("instanceKey", 
            (instanceKey == null ? null : instanceKey.toString()) ));
        ref.add(new StringRefAddr("dataSourceName", getDataSourceName()));
        ref.add(new StringRefAddr("defaultAutoCommit", 
                                  String.valueOf(isDefaultAutoCommit())));
        ref.add(new StringRefAddr("defaultMaxActive", 
                                  String.valueOf(getDefaultMaxActive())));
        ref.add(new StringRefAddr("defaultMaxIdle", 
                                  String.valueOf(getDefaultMaxIdle())));
        ref.add(new StringRefAddr("defaultMaxWait", 
                                  String.valueOf(getDefaultMaxWait())));
        ref.add(new StringRefAddr("defaultReadOnly", 
                                  String.valueOf(isDefaultReadOnly())));
        ref.add(new StringRefAddr("description", getDescription()));

        byte[] ser = null;
        // BinaryRefAddr does not allow null byte[].
        if ( jndiEnvironment != null ) 
        {
            try
            {
                ser = serialize(jndiEnvironment);
                ref.add(new BinaryRefAddr("jndiEnvironment", ser));
            }
            catch (IOException ioe)
            {
                throw new NamingException("An IOException prevented " + 
                   "serializing the jndiEnvironment properties.");
            }
        }

        ref.add(new StringRefAddr("loginTimeout", 
                                  String.valueOf(getLoginTimeout())));

        if ( perUserDefaultAutoCommit != null ) 
        {
            try
            {
                ser = serialize((Serializable)perUserDefaultAutoCommit);
                ref.add(new BinaryRefAddr("perUserDefaultAutoCommit", ser));
            }
            catch (IOException ioe)
            {
                throw new NamingException("An IOException prevented " + 
                   "serializing the perUserDefaultAutoCommit properties.");
            }
        }

        if ( perUserMaxActive != null ) 
        {
            try
            {
                ser = serialize((Serializable)perUserMaxActive);
                ref.add(new BinaryRefAddr("perUserMaxActive", ser));
            }
            catch (IOException ioe)
            {
                throw new NamingException("An IOException prevented " + 
                   "serializing the perUserMaxActive properties.");
            }
        }

        if ( perUserMaxIdle != null ) 
        {
            try
            {
                ser = serialize((Serializable)perUserMaxIdle);
                ref.add(new BinaryRefAddr("perUserMaxIdle", ser));
            }
            catch (IOException ioe)
            {
                throw new NamingException("An IOException prevented " + 
                   "serializing the perUserMaxIdle properties.");
            }
        }

        if ( perUserMaxWait != null ) 
        {
            try
            {
                ser = serialize((Serializable)perUserMaxWait);
                ref.add(new BinaryRefAddr("perUserMaxWait", ser));
            }
            catch (IOException ioe)
            {
                throw new NamingException("An IOException prevented " + 
                   "serializing the perUserMaxWait properties.");
            }
        }

        if ( perUserDefaultReadOnly != null ) 
        {
            try
            {
                ser = serialize((Serializable)perUserDefaultReadOnly);
                ref.add(new BinaryRefAddr("perUserDefaultReadOnly", ser));
            }
            catch (IOException ioe)
            {
                throw new NamingException("An IOException prevented " + 
                   "serializing the perUserDefaultReadOnly properties.");
            }
        }

        ref.add(new StringRefAddr("testOnBorrow", 
                                  String.valueOf(getTestOnBorrow())));
        ref.add(new StringRefAddr("testOnReturn", 
                                  String.valueOf(getTestOnReturn())));
        ref.add(new StringRefAddr("timeBetweenEvictionRunsMillis", 
            String.valueOf(getTimeBetweenEvictionRunsMillis())));
        ref.add(new StringRefAddr("numTestsPerEvictionRun", 
            String.valueOf(getNumTestsPerEvictionRun())));
        ref.add(new StringRefAddr("minEvictableIdleTimeMillis", 
            String.valueOf(getMinEvictableIdleTimeMillis())));
        ref.add(new StringRefAddr("testWhileIdle", 
                                  String.valueOf(getTestWhileIdle())));
        ref.add(new StringRefAddr("validationQuery", getValidationQuery()));
        
        return ref;
    }

    /**
     * Converts a object to a byte array for storage/serialization.
     *
     * @param obj The Serializable to convert.
     * @return A byte[] with the converted Serializable.
     * @exception IOException, if conversion to a byte[] fails.
     */
    private static byte[] serialize(Serializable obj)
        throws IOException
    {
        byte[] byteArray = null;
        ByteArrayOutputStream baos = null;
        ObjectOutputStream out = null;
        try
        {
            // These objects are closed in the finally.
            baos = new ByteArrayOutputStream();
            out = new ObjectOutputStream(baos);

            out.writeObject(obj);
            byteArray = baos.toByteArray();
        }
        finally
        {
            if (out != null) 
            {
                out.close();
            }
        }
        return byteArray;
    }


    // ----------------------------------------------------------------------
    // ObjectFactory implementation 

    /**
     * implements ObjectFactory to create an instance of this class
     */ 
    public Object getObjectInstance(Object refObj, Name name, 
                                    Context context, Hashtable env) 
        throws Exception 
    {
        // The spec says to return null if we can't create an instance 
        // of the reference
        Jdbc2PoolDataSource ds = null;
        if (refObj instanceof Reference) 
        {
            Reference ref = (Reference)refObj;
	
            if (ref.getClassName().equals(getClass().getName())) 
            {   
                RefAddr ra = ref.get("isNew");
                if (ra != null && ra.getContent() != null) 
                {
                    isNew = Boolean.getBoolean(ra.getContent().toString());
                }

                ra = ref.get("instanceKey");
                if (ra != null && ra.getContent() != null) 
                {
                    instanceKey = new Integer(ra.getContent().toString());
                }

                ra = ref.get("dataSourceName");
                if (ra != null && ra.getContent() != null) 
                {
                    setDataSourceName(ra.getContent().toString());
                }

                ra = ref.get("defaultAutoCommit");
                if (ra != null && ra.getContent() != null) 
                {
                    setDefaultAutoCommit
                        (Boolean.getBoolean(ra.getContent().toString()));
                }

                ra = ref.get("defaultMaxActive");
                if (ra != null && ra.getContent() != null) 
                {
                    setDefaultMaxActive(
                        Integer.parseInt(ra.getContent().toString()));
                }

                ra = ref.get("defaultMaxIdle");
                if (ra != null && ra.getContent() != null) 
                {
                    setDefaultMaxIdle(
                        Integer.parseInt(ra.getContent().toString()));
                }

                ra = ref.get("defaultMaxWait");
                if (ra != null && ra.getContent() != null) 
                {
                    setDefaultMaxWait(
                        Integer.parseInt(ra.getContent().toString()));
                }

                ra = ref.get("defaultReadOnly");
                if (ra != null && ra.getContent() != null) 
                {
                    setDefaultReadOnly
                        (Boolean.getBoolean(ra.getContent().toString()));
                }

                ra = ref.get("description");
                if (ra != null && ra.getContent() != null) 
                {
                    setDescription(ra.getContent().toString());
                }

                ra = ref.get("jndiEnvironment");
                if (ra != null  && ra.getContent() != null) 
                {
                    byte[] serialized = (byte[])ra.getContent();
                    jndiEnvironment = 
                        (Properties)SerializationUtils.deserialize(serialized);
                }
                
                ra = ref.get("loginTimeout");
                if (ra != null && ra.getContent() != null) 
                {
                    setLoginTimeout(
                        Integer.parseInt(ra.getContent().toString()));
                }

                ra = ref.get("perUserDefaultAutoCommit");
                if (ra != null  && ra.getContent() != null) 
                {
                    byte[] serialized = (byte[])ra.getContent();
                    perUserDefaultAutoCommit = 
                        (Map)SerializationUtils.deserialize(serialized);
                }
                
                ra = ref.get("perUserMaxActive");
                if (ra != null  && ra.getContent() != null) 
                {
                    byte[] serialized = (byte[])ra.getContent();
                    perUserMaxActive = 
                        (Map)SerializationUtils.deserialize(serialized);
                }

                ra = ref.get("perUserMaxIdle");
                if (ra != null  && ra.getContent() != null) 
                {
                    byte[] serialized = (byte[])ra.getContent();
                    perUserMaxIdle = 
                        (Map)SerializationUtils.deserialize(serialized);
                }

                ra = ref.get("perUserMaxWait");
                if (ra != null  && ra.getContent() != null) 
                {
                    byte[] serialized = (byte[])ra.getContent();
                    perUserMaxWait = 
                        (Map)SerializationUtils.deserialize(serialized);
                }
                
                ra = ref.get("perUserDefaultReadOnly");
                if (ra != null  && ra.getContent() != null) 
                {
                    byte[] serialized = (byte[])ra.getContent();
                    perUserDefaultReadOnly = 
                        (Map)SerializationUtils.deserialize(serialized);
                }
                
                ra = ref.get("testOnBorrow");
                if (ra != null && ra.getContent() != null) 
                {
                    setTestOnBorrow
                        (Boolean.getBoolean(ra.getContent().toString()));
                }

                ra = ref.get("testOnReturn");
                if (ra != null && ra.getContent() != null) 
                {
                    setTestOnReturn
                        (Boolean.getBoolean(ra.getContent().toString()));
                }

                ra = ref.get("timeBetweenEvictionRunsMillis");
                if (ra != null && ra.getContent() != null) 
                {
                    setTimeBetweenEvictionRunsMillis(
                        Integer.parseInt(ra.getContent().toString()));
                }

                ra = ref.get("numTestsPerEvictionRun");
                if (ra != null && ra.getContent() != null) 
                {
                    setNumTestsPerEvictionRun(
                        Integer.parseInt(ra.getContent().toString()));
                }

                ra = ref.get("minEvictableIdleTimeMillis");
                if (ra != null && ra.getContent() != null) 
                {
                    setMinEvictableIdleTimeMillis(
                        Integer.parseInt(ra.getContent().toString()));
                }

                ra = ref.get("testWhileIdle");
                if (ra != null && ra.getContent() != null) 
                {
                    setTestWhileIdle
                        (Boolean.getBoolean(ra.getContent().toString()));
                }
                
                ra = ref.get("validationQuery");
                if (ra != null && ra.getContent() != null) 
                {
                    setValidationQuery(ra.getContent().toString());
                }

                ds = this;
            }            
        }
        
        return ds;
    }
}
