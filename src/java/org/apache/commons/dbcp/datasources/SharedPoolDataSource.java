/*
 * $Source: /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons//dbcp/src/java/org/apache/commons/dbcp/datasources/SharedPoolDataSource.java,v $
 * $Revision: 1.3 $
 * $Date: 2003/08/22 16:08:32 $
 *
 * ====================================================================
 *
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999-2003 The Apache Software Foundation.  All rights
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
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowledgement:
 *       "This product includes software developed by the
 *        Apache Software Foundation - http://www.apache.org/"
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Commons", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
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
 * http://www.apache.org/
 *
 */

package org.apache.commons.dbcp.datasources;

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
 * <p>
 * A pooling <code>DataSource</code> appropriate for deployment within
 * J2EE environment.  There are many configuration options, most of which are
 * defined in the parent class. All users (based on username) share a single 
 * maximum number of Connections in this datasource.
 * </p>
 *
 * @author <a href="mailto:jmcnally@collab.net">John D. McNally</a>
 * @version $Id: SharedPoolDataSource.java,v 1.3 2003/08/22 16:08:32 dirkv Exp $
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

    private synchronized void registerPool(
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
    }
}

