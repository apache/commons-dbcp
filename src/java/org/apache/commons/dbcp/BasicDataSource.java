/*
 * $Source: /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons//dbcp/src/java/org/apache/commons/dbcp/BasicDataSource.java,v $
 * $Revision: 1.23 $
 * $Date: 2003/08/27 15:43:55 $
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

package org.apache.commons.dbcp;

import java.io.PrintWriter;
import java.util.Properties;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import javax.sql.DataSource;

import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.apache.commons.pool.impl.GenericKeyedObjectPoolFactory;
import org.apache.commons.pool.impl.GenericObjectPool;


/**
 * <p>Basic implementation of <code>javax.sql.DataSource</code> that is
 * configured via JavaBeans properties.  This is not the only way to
 * combine the <em>commons-dbcp</em> and <em>commons-pool</em> packages,
 * but provides a "one stop shopping" solution for basic requirements.</p>
 *
 * @author Glenn L. Nielsen
 * @author Craig R. McClanahan
 * @author Dirk Verbeeck
 * @version $Revision: 1.23 $ $Date: 2003/08/27 15:43:55 $
 */

public class BasicDataSource implements DataSource {


    // ------------------------------------------------------------- Properties


    /**
     * The default auto-commit state of connections created by this pool.
     */
    protected boolean defaultAutoCommit = true;

    public boolean getDefaultAutoCommit() {
        return (this.defaultAutoCommit);
    }

    public void setDefaultAutoCommit(boolean defaultAutoCommit) {
        this.defaultAutoCommit = defaultAutoCommit;
    }


    /**
     * The default read-only state of connections created by this pool.
     */
    protected boolean defaultReadOnly = false;

    public boolean getDefaultReadOnly() {
        return (this.defaultReadOnly);
    }

    public void setDefaultReadOnly(boolean defaultReadOnly) {
        this.defaultReadOnly = defaultReadOnly;
    }

    /**
     * The default TransactionIsolation state of connections created by this pool.
     */
    protected int defaultTransactionIsolation = PoolableConnectionFactory.UNKNOWN_TRANSACTIONISOLATION;

    public int getDefaultTransactionIsolation() {
        return this.defaultTransactionIsolation;
    }

    public void setDefaultTransactionIsolation(int defaultTransactionIsolation) {
        this.defaultTransactionIsolation = defaultTransactionIsolation;
    }

    /**
     * The fully qualified Java class name of the JDBC driver to be used.
     */
    protected String driverClassName = null;

    public String getDriverClassName() {
        return (this.driverClassName);
    }

    public void setDriverClassName(String driverClassName) {
        this.driverClassName = driverClassName;
    }


    /**
     * The maximum number of active connections that can be allocated from
     * this pool at the same time, or zero for no limit.
     */
    protected int maxActive = GenericObjectPool.DEFAULT_MAX_ACTIVE;

    public int getMaxActive() {
        return (this.maxActive);
    }

    public void setMaxActive(int maxActive) {
        this.maxActive = maxActive;
    }


    /**
     * The maximum number of active connections that can remain idle in the
     * pool, without extra ones being released, or zero for no limit.
     */
    protected int maxIdle = GenericObjectPool.DEFAULT_MAX_IDLE;;

    public int getMaxIdle() {
        return (this.maxIdle);
    }

    public void setMaxIdle(int maxIdle) {
        this.maxIdle = maxIdle;
    }

    /**
     * The minimum number of active connections that can remain idle in the
     * pool, without extra ones being created, or 0 to create none.
     */
    protected int minIdle = GenericObjectPool.DEFAULT_MIN_IDLE;;

    public int getMinIdle() {
        return (this.minIdle);
    }

    public void setMinIdle(int minIdle) {
       this.minIdle = minIdle;
    }

    /**
     * The maximum number of milliseconds that the pool will wait (when there
     * are no available connections) for a connection to be returned before
     * throwing an exception, or -1 to wait indefinitely.
     */
    protected long maxWait = GenericObjectPool.DEFAULT_MAX_WAIT;

    public long getMaxWait() {
        return (this.maxWait);
    }

    public void setMaxWait(long maxWait) {
        this.maxWait = maxWait;
    }

    /**
     * Prepared statement pooling for this pool.
     */
    protected boolean poolPreparedStatements = false;
    
    /**
     * Returns true if we are pooling statements.
     * @return boolean
     */
    public boolean isPoolPreparedStatements()
    {
        return poolPreparedStatements;
    }

    /**
     * Sets whether to pool statements or not.
     * @param poolPreparedStatements pooling on or off
     */
    public void setPoolPreparedStatements(boolean poolingStatements)
    {
         this.poolPreparedStatements = poolingStatements;
    }

    /**
     * The maximum number of open statements that can be allocated from
     * the statement pool at the same time, or zero for no limit.  Since 
     * a connection usually only uses one or two statements at a time, this is
     * mostly used to help detect resource leaks.
     */
    protected int maxOpenPreparedStatements = GenericKeyedObjectPool.DEFAULT_MAX_TOTAL;

    public int getMaxOpenPreparedStatements() {
        return maxOpenPreparedStatements;
    }

    public void setMaxOpenPreparedStatements(int maxOpenStatements) {
        this.maxOpenPreparedStatements = maxOpenStatements;
    }

    /**
     * The indication of whether objects will be validated before being
     * borrowed from the pool.  If the object fails to validate, it will be
     * dropped from the pool, and we will attempt to borrow another.
     */
    protected boolean testOnBorrow = GenericObjectPool.DEFAULT_TEST_ON_BORROW;

    public boolean getTestOnBorrow() {
        return (this.testOnBorrow);
    }

    public void setTestOnBorrow(boolean testOnBorrow) {
        this.testOnBorrow = testOnBorrow;
    }

    /**
     * The indication of whether objects will be validated before being
     * returned to the pool.
     */
    protected boolean testOnReturn =  GenericObjectPool.DEFAULT_TEST_ON_RETURN;

    public boolean getTestOnReturn() {
        return (this.testOnReturn);
    }

    public void setTestOnReturn(boolean testOnReturn) {
        this.testOnReturn = testOnReturn;
    }


    /**
     * The number of milliseconds to sleep between runs of the idle object
     * evictor thread.  When non-positive, no idle object evictor thread will
     * be run.
     */
    protected long timeBetweenEvictionRunsMillis =
        GenericObjectPool.DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS;
        
    public long getTimeBetweenEvictionRunsMillis() {
        return (this.timeBetweenEvictionRunsMillis);
    }

    public void setTimeBetweenEvictionRunsMillis(long timeBetweenEvictionRunsMillis) {
        this.timeBetweenEvictionRunsMillis = timeBetweenEvictionRunsMillis;
    }


    /**
     * The number of objects to examine during each run of the idle object
     * evictor thread (if any).
     */
    protected int numTestsPerEvictionRun =
        GenericObjectPool.DEFAULT_NUM_TESTS_PER_EVICTION_RUN;

    public int getNumTestsPerEvictionRun() {
        return (this.numTestsPerEvictionRun);
    }

    public void setNumTestsPerEvictionRun(int numTestsPerEvictionRun) {
        this.numTestsPerEvictionRun = numTestsPerEvictionRun;
    }


    /**
     * The minimum amount of time an object may sit idle in the pool before it
     * is eligable for eviction by the idle object evictor (if any).
     */
    protected long minEvictableIdleTimeMillis =
        GenericObjectPool.DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS;

    public long getMinEvictableIdleTimeMillis() {
        return (this.minEvictableIdleTimeMillis);
    }

    public void setMinEvictableIdleTimeMillis(long minEvictableIdleTimeMillis) {
        this.minEvictableIdleTimeMillis = minEvictableIdleTimeMillis;
    }

    /**
     * The indication of whether objects will be validated by the idle object
     * evictor (if any).  If an object fails to validate, it will be dropped
     * from the pool.
     */
    protected boolean testWhileIdle = GenericObjectPool.DEFAULT_TEST_WHILE_IDLE;

    public boolean getTestWhileIdle() {
        return (this.testWhileIdle);
    }

    public void setTestWhileIdle(boolean testWhileIdle) {
        this.testWhileIdle = testWhileIdle;
    }

    /**
     * [Read Only] The current number of active connections that have been
     * allocated from this data source.
     */
    public int getNumActive() {
        if (connectionPool != null) {
            return (connectionPool.getNumActive());
        } else {
            return (0);
        }
    }


    /**
     * [Read Only] The current number of idle connections that are waiting
     * to be allocated from this data source.
     */
    public int getNumIdle() {
        if (connectionPool != null) {
            return (connectionPool.getNumIdle());
        } else {
            return (0);
        }
    }


    /**
     * The connection password to be passed to our JDBC driver to establish
     * a connection.
     */
    protected String password = null;

    public String getPassword() {
        return (this.password);
    }

    public void setPassword(String password) {
        this.password = password;
    }


    /**
     * The connection URL to be passed to our JDBC driver to establish
     * a connection.
     */
    protected String url = null;

    public String getUrl() {
        return (this.url);
    }

    public void setUrl(String url) {
        this.url = url;
    }


    /**
     * The connection username to be passed to our JDBC driver to
     * establish a connection.
     */
    protected String username = null;

    public String getUsername() {
        return (this.username);
    }

    public void setUsername(String username) {
        this.username = username;
    }


    /**
     * The SQL query that will be used to validate connections from this pool
     * before returning them to the caller.  If specified, this query
     * <strong>MUST</strong> be an SQL SELECT statement that returns at least
     * one row.
     */
    protected String validationQuery = null;

    public String getValidationQuery() {
        return (this.validationQuery);
    }

    public void setValidationQuery(String validationQuery) {
        this.validationQuery = validationQuery;
    }


    // ----------------------------------------------------- Instance Variables


    /**
     * The object pool that internally manages our connections.
     */
    protected GenericObjectPool connectionPool = null;


    /**
     * The connection properties that will be sent to our JDBC driver when
     * establishing new connections.  <strong>NOTE</strong> - The "user" and
     * "password" properties will be passed explicitly, so they do not need
     * to be included here.
     */
    protected Properties connectionProperties = new Properties();


    /**
     * The data source we will use to manage connections.  This object should
     * be acquired <strong>ONLY</strong> by calls to the
     * <code>createDataSource()</code> method.
     */
    protected DataSource dataSource = null;


    /**
     * The PrintWriter to which log messages should be directed.
     */
    protected PrintWriter logWriter = new PrintWriter(System.out);


    // ----------------------------------------------------- DataSource Methods


    /**
     * Create (if necessary) and return a connection to the database.
     *
     * @exception SQLException if a database access error occurs
     */
    public Connection getConnection() throws SQLException {

        return (createDataSource().getConnection());

    }


    /**
     * Create (if necessary) and return a connection to the database.
     *
     * @param username Database user on whose behalf the Connection
     *   is being made
     * @param password The database user's password
     *
     * @exception SQLException if a database access error occurs
     */
    public Connection getConnection(String username, String password)
        throws SQLException {

        return (createDataSource().getConnection(username, password));

    }


    /**
     * Return the login timeout (in seconds) for connecting to the database.
     *
     * @exception SQLException if a database access error occurs
     */
    public int getLoginTimeout() throws SQLException {

        return (createDataSource().getLoginTimeout());

    }


    /**
     * Return the log writer being used by this data source.
     *
     * @exception SQLException if a database access error occurs
     */
    public PrintWriter getLogWriter() throws SQLException {

        return (createDataSource().getLogWriter());

    }


    /**
     * Set the login timeout (in seconds) for connecting to the database.
     *
     * @param loginTimeout The new login timeout, or zero for no timeout
     *
     * @exception SQLException if a database access error occurs
     */
    public void setLoginTimeout(int loginTimeout) throws SQLException {

        createDataSource().setLoginTimeout(loginTimeout);

    }


    /**
     * Set the log writer being used by this data source.
     *
     * @param logWriter The new log writer
     *
     * @exception SQLException if a database access error occurs
     */
    public void setLogWriter(PrintWriter logWriter) throws SQLException {

        createDataSource().setLogWriter(logWriter);
        this.logWriter = logWriter;

    }

    private AbandonedConfig abandonedConfig;

    /**                       
     * Flag to remove abandoned connections if they exceed the
     * removeAbandonedTimout.
     *
     * Set to true or false, default false.
     * If set to true a connection is considered abandoned and eligible
     * for removal if it has been idle longer than the removeAbandonedTimeout.
     * Setting this to true can recover db connections from poorly written    
     * applications which fail to close a connection.      
     * @deprecated                   
     */                                                                   
    public boolean getRemoveAbandoned() {   
        if (abandonedConfig != null) {
            return abandonedConfig.getRemoveAbandoned();
        }
        return false;
    }                                    
                                 
    /**
     * @deprecated
     * @param removeAbandoned
     */
    public void setRemoveAbandoned(boolean removeAbandoned) {
        if (abandonedConfig == null) {
            abandonedConfig = new AbandonedConfig();
        }
        abandonedConfig.setRemoveAbandoned(removeAbandoned);
    }                                                        
                                               
    /**
     * Timeout in seconds before an abandoned connection can be removed.
     *
     * Defaults to 300 seconds.         
     * @deprecated                                
     */                                                                 
    public int getRemoveAbandonedTimeout() { 
        if (abandonedConfig != null) {
            return abandonedConfig.getRemoveAbandonedTimeout();
        }
        return 300;
    }                                        

    /**
     * @deprecated
     * @param removeAbandonedTimeout
     */               
    public void setRemoveAbandonedTimeout(int removeAbandonedTimeout) {
        if (abandonedConfig == null) {
            abandonedConfig = new AbandonedConfig();
        }
        abandonedConfig.setRemoveAbandonedTimeout(removeAbandonedTimeout);
    }                                                                  
                                                             
    /**
     * Flag to log stack traces for application code which abandoned
     * a Statement or Connection.
     *
     * Defaults to false.                
     *                                                              
     * Logging of abandoned Statements and Connections adds overhead
     * for every Connection open or new Statement because a stack   
     * trace has to be generated.             
     * @deprecated                      
     */                                                          
    public boolean getLogAbandoned() {   
        if (abandonedConfig != null) {
            return abandonedConfig.getLogAbandoned();
        }
        return false;
    }                                 

    /**
     * @deprecated
     * @param logAbandoned
     */
    public void setLogAbandoned(boolean logAbandoned) {
        if (abandonedConfig == null) {
            abandonedConfig = new AbandonedConfig();
        }
        abandonedConfig.setLogAbandoned(logAbandoned);
    }


    // --------------------------------------------------------- Public Methods


    /**
     * Add a custom connection property to the set that will be passed to our
     * JDBC driver.    This <strong>MUST</strong> be called before the first
     * connection is retrieved (along with all the other configuration
     * property setters).
     *
     * @param name Name of the custom connection property
     * @param value Value of the custom connection property
     */
    public void addConnectionProperty(String name, String value) {

        connectionProperties.put(name, value);

    }


    /**
     * Close and release all connections that are currently stored in the
     * connection pool associated with our data source.
     *
     * @exception SQLException if a database error occurs
     */
    public void close() throws SQLException {
        GenericObjectPool oldpool = connectionPool;
        connectionPool = null;
        dataSource = null;
        try {
            if (oldpool != null) {
                oldpool.close();
            }
        } catch(SQLException e) {
            throw e;
        } catch(RuntimeException e) {
            throw e;
        } catch(Exception e) {
            throw new SQLNestedException("Cannot close connection pool", e);
        }
    }


    // ------------------------------------------------------ Protected Methods


    /**
     * <p>Create (if necessary) and return the internal data source we are
     * using to manage our connections.</p>
     *
     * <p><strong>IMPLEMENTATION NOTE</strong> - It is tempting to use the
     * "double checked locking" idiom in an attempt to avoid synchronizing
     * on every single call to this method.  However, this idiom fails to
     * work correctly in the face of some optimizations that are legal for
     * a JVM to perform.</p>
     *
     * @exception SQLException if the object pool cannot be created.
     */
    protected synchronized DataSource createDataSource()
        throws SQLException {

        // Return the pool if we have already created it
        if (dataSource != null) {
            return (dataSource);
        }

        // Load the JDBC driver class
        try {
            Class.forName(driverClassName);
        } catch (Throwable t) {
            String message = "Cannot load JDBC driver class '" +
                driverClassName + "'";
            logWriter.println(message);
            t.printStackTrace(logWriter);
            throw new SQLNestedException(message, t);
        }

        // Create a JDBC driver instance
        Driver driver = null;
        try {
            driver = DriverManager.getDriver(url);
        } catch (Throwable t) {
            String message = "Cannot create JDBC driver of class '" +
                driverClassName + "' for connect URL '" + url + "'";
            logWriter.println(message);
            t.printStackTrace(logWriter);
            throw new SQLNestedException(message, t);
        }

        // Create an object pool to contain our active connections
        if ((abandonedConfig != null) && (abandonedConfig.getRemoveAbandoned() == true)) {
            connectionPool = new AbandonedObjectPool(null,abandonedConfig);
        }
        else {
            connectionPool = new GenericObjectPool();
        }
        connectionPool.setMaxActive(maxActive);
        connectionPool.setMaxIdle(maxIdle);
        connectionPool.setMinIdle(minIdle);
        connectionPool.setMaxWait(maxWait);
        connectionPool.setTestOnBorrow(testOnBorrow);
        connectionPool.setTestOnReturn(testOnReturn);
        connectionPool.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);
        connectionPool.setNumTestsPerEvictionRun(numTestsPerEvictionRun);
        connectionPool.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);
        connectionPool.setTestWhileIdle(testWhileIdle);
        
        if (validationQuery != null && !testOnReturn && !testWhileIdle) {
            connectionPool.setTestOnBorrow(true);
        }

        // Set up statement pool, if desired
        GenericKeyedObjectPoolFactory statementPoolFactory = null;
        if (isPoolPreparedStatements()) {
            statementPoolFactory = new GenericKeyedObjectPoolFactory(null, 
                        -1, // unlimited maxActive (per key)
                        GenericKeyedObjectPool.WHEN_EXHAUSTED_FAIL, 
                        0, // maxWait
                        1, // maxIdle (per key) 
                        maxOpenPreparedStatements); 
        }

        // Set up the driver connection factory we will use
        if (username != null) {
            connectionProperties.put("user", username);
        } else {
            this.logWriter.println("DBCP DataSource configured without a 'username'");
        }
        
        if (password != null) {
            connectionProperties.put("password", password);
        } else {
            this.logWriter.println("DBCP DataSource configured without a 'password'");
        }
        
        DriverConnectionFactory driverConnectionFactory =
            new DriverConnectionFactory(driver, url, connectionProperties);

        // Set up the poolable connection factory we will use
        PoolableConnectionFactory connectionFactory = null;
        try {
            connectionFactory =
                new PoolableConnectionFactory(driverConnectionFactory,
                                              connectionPool,
                                              statementPoolFactory,
                                              validationQuery,
                                              defaultReadOnly,
                                              defaultAutoCommit,
                                              defaultTransactionIsolation,
                                              abandonedConfig);
            if (connectionFactory == null) {
                throw new SQLException("Cannot create PoolableConnectionFactory");
            }
        } catch(RuntimeException e) {
            throw e;
        } catch(Exception e) {
            throw new SQLNestedException("Cannot create PoolableConnectionFactory", e);
        }

        // Create and return the pooling data source to manage the connections
        dataSource = new PoolingDataSource(connectionPool);
        dataSource.setLogWriter(logWriter);
        return (dataSource);

    }


}
