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
 * @version $Revision: 1.37 $ $Date: 2004/06/09 18:21:23 $
 */
public class BasicDataSource implements DataSource {

    // ------------------------------------------------------------- Properties

    /**
     * The default auto-commit state of connections created by this pool.
     */
    protected boolean defaultAutoCommit = true;

    public synchronized boolean getDefaultAutoCommit() {
        return this.defaultAutoCommit;
    }

    public synchronized void setDefaultAutoCommit(boolean defaultAutoCommit) {
        this.defaultAutoCommit = defaultAutoCommit;
        this.restartNeeded = true;
    }


    /**
     * The default read-only state of connections created by this pool.
     */
    protected Boolean defaultReadOnly = null;

    public synchronized boolean getDefaultReadOnly() {
        if (this.defaultReadOnly != null) {
            return this.defaultReadOnly.booleanValue();
        }
        return false;
    }

    public synchronized void setDefaultReadOnly(boolean defaultReadOnly) {
        this.defaultReadOnly = defaultReadOnly ? Boolean.TRUE : Boolean.FALSE;
        this.restartNeeded = true;
    }

    /**
     * The default TransactionIsolation state of connections created by this pool.
     */
    protected int defaultTransactionIsolation = PoolableConnectionFactory.UNKNOWN_TRANSACTIONISOLATION;

    public synchronized int getDefaultTransactionIsolation() {
        return this.defaultTransactionIsolation;
    }

    public synchronized void setDefaultTransactionIsolation(int defaultTransactionIsolation) {
        this.defaultTransactionIsolation = defaultTransactionIsolation;
        this.restartNeeded = true;
    }


    /**
     * The default "catalog" of connections created by this pool.
     */
    protected String defaultCatalog = null;

    public synchronized String getDefaultCatalog() {
        return this.defaultCatalog;
    }

    public synchronized void setDefaultCatalog(String defaultCatalog) {
        if ((defaultCatalog != null) && (defaultCatalog.trim().length() > 0)) {
            this.defaultCatalog = defaultCatalog;
        }
        else {
            this.defaultCatalog = null;
        }
        this.restartNeeded = true;
    }

  
    /**
     * The fully qualified Java class name of the JDBC driver to be used.
     */
    protected String driverClassName = null;

    public synchronized String getDriverClassName() {
        return this.driverClassName;
    }

    public synchronized void setDriverClassName(String driverClassName) {
        if ((driverClassName != null) && (driverClassName.trim().length() > 0)) {
            this.driverClassName = driverClassName;
        }
        else {
            this.driverClassName = null;
        }
        this.restartNeeded = true;
    }


    /**
     * The maximum number of active connections that can be allocated from
     * this pool at the same time, or zero for no limit.
     */
    protected int maxActive = GenericObjectPool.DEFAULT_MAX_ACTIVE;

    public synchronized int getMaxActive() {
        return this.maxActive;
    }

    public synchronized void setMaxActive(int maxActive) {
        this.maxActive = maxActive;
        if (connectionPool != null) {
            connectionPool.setMaxActive(maxActive);
        }
    }


    /**
     * The maximum number of active connections that can remain idle in the
     * pool, without extra ones being released, or zero for no limit.
     */
    protected int maxIdle = GenericObjectPool.DEFAULT_MAX_IDLE;;

    public synchronized int getMaxIdle() {
        return this.maxIdle;
    }

    public synchronized void setMaxIdle(int maxIdle) {
        this.maxIdle = maxIdle;
        if (connectionPool != null) {
            connectionPool.setMaxIdle(maxIdle);
        }
    }

    /**
     * The minimum number of active connections that can remain idle in the
     * pool, without extra ones being created, or 0 to create none.
     */
    protected int minIdle = GenericObjectPool.DEFAULT_MIN_IDLE;;

    public synchronized int getMinIdle() {
        return this.minIdle;
    }

    public synchronized void setMinIdle(int minIdle) {
       this.minIdle = minIdle;
       if (connectionPool != null) {
           connectionPool.setMinIdle(minIdle);
       }
    }

    /**
     * The initial number of connections that are created when the pool
     * is started.
     * @since 1.2
     */
    protected int initialSize = 0;
    
    public synchronized int getInitialSize() {
        return this.initialSize;
    }
    
    public synchronized void setInitialSize(int initialSize) {
        this.initialSize = initialSize;
        this.restartNeeded = true;
    }

    /**
     * The maximum number of milliseconds that the pool will wait (when there
     * are no available connections) for a connection to be returned before
     * throwing an exception, or -1 to wait indefinitely.
     */
    protected long maxWait = GenericObjectPool.DEFAULT_MAX_WAIT;

    public synchronized long getMaxWait() {
        return this.maxWait;
    }

    public synchronized void setMaxWait(long maxWait) {
        this.maxWait = maxWait;
        if (connectionPool != null) {
            connectionPool.setMaxWait(maxWait);
        }
    }

    /**
     * Prepared statement pooling for this pool.
     */
    protected boolean poolPreparedStatements = false;
    
    /**
     * Returns true if we are pooling statements.
     * @return boolean
     */
    public synchronized boolean isPoolPreparedStatements() {
        return this.poolPreparedStatements;
    }

    /**
     * Sets whether to pool statements or not.
     * @param poolPreparedStatements pooling on or off
     */
    public synchronized void setPoolPreparedStatements(boolean poolingStatements) {
        this.poolPreparedStatements = poolingStatements;
        this.restartNeeded = true;
    }

    /**
     * The maximum number of open statements that can be allocated from
     * the statement pool at the same time, or zero for no limit.  Since 
     * a connection usually only uses one or two statements at a time, this is
     * mostly used to help detect resource leaks.
     */
    protected int maxOpenPreparedStatements = GenericKeyedObjectPool.DEFAULT_MAX_TOTAL;

    public synchronized int getMaxOpenPreparedStatements() {
        return this.maxOpenPreparedStatements;
    }

    public synchronized void setMaxOpenPreparedStatements(int maxOpenStatements) {
        this.maxOpenPreparedStatements = maxOpenStatements;
        this.restartNeeded = true;
    }

    /**
     * The indication of whether objects will be validated before being
     * borrowed from the pool.  If the object fails to validate, it will be
     * dropped from the pool, and we will attempt to borrow another.
     */
    protected boolean testOnBorrow = true;

    public synchronized boolean getTestOnBorrow() {
        return this.testOnBorrow;
    }

    public synchronized void setTestOnBorrow(boolean testOnBorrow) {
        this.testOnBorrow = testOnBorrow;
        if (connectionPool != null) {
            connectionPool.setTestOnBorrow(testOnBorrow);
        }
    }

    /**
     * The indication of whether objects will be validated before being
     * returned to the pool.
     */
    protected boolean testOnReturn = false;

    public synchronized boolean getTestOnReturn() {
        return this.testOnReturn;
    }

    public synchronized void setTestOnReturn(boolean testOnReturn) {
        this.testOnReturn = testOnReturn;
        if (connectionPool != null) {
            connectionPool.setTestOnReturn(testOnReturn);
        }
    }


    /**
     * The number of milliseconds to sleep between runs of the idle object
     * evictor thread.  When non-positive, no idle object evictor thread will
     * be run.
     */
    protected long timeBetweenEvictionRunsMillis =
        GenericObjectPool.DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS;
        
    public synchronized long getTimeBetweenEvictionRunsMillis() {
        return this.timeBetweenEvictionRunsMillis;
    }

    public synchronized void setTimeBetweenEvictionRunsMillis(long timeBetweenEvictionRunsMillis) {
        this.timeBetweenEvictionRunsMillis = timeBetweenEvictionRunsMillis;
        if (connectionPool != null) {
            connectionPool.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);
        }
    }


    /**
     * The number of objects to examine during each run of the idle object
     * evictor thread (if any).
     */
    protected int numTestsPerEvictionRun =
        GenericObjectPool.DEFAULT_NUM_TESTS_PER_EVICTION_RUN;

    public synchronized int getNumTestsPerEvictionRun() {
        return this.numTestsPerEvictionRun;
    }

    public synchronized void setNumTestsPerEvictionRun(int numTestsPerEvictionRun) {
        this.numTestsPerEvictionRun = numTestsPerEvictionRun;
        if (connectionPool != null) {
            connectionPool.setNumTestsPerEvictionRun(numTestsPerEvictionRun);
        }
    }


    /**
     * The minimum amount of time an object may sit idle in the pool before it
     * is eligable for eviction by the idle object evictor (if any).
     */
    protected long minEvictableIdleTimeMillis =
        GenericObjectPool.DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS;

    public synchronized long getMinEvictableIdleTimeMillis() {
        return this.minEvictableIdleTimeMillis;
    }

    public synchronized void setMinEvictableIdleTimeMillis(long minEvictableIdleTimeMillis) {
        this.minEvictableIdleTimeMillis = minEvictableIdleTimeMillis;
        if (connectionPool != null) {
            connectionPool.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);
        }
    }

    /**
     * The indication of whether objects will be validated by the idle object
     * evictor (if any).  If an object fails to validate, it will be dropped
     * from the pool.
     */
    protected boolean testWhileIdle = false;

    public synchronized boolean getTestWhileIdle() {
        return this.testWhileIdle;
    }

    public synchronized void setTestWhileIdle(boolean testWhileIdle) {
        this.testWhileIdle = testWhileIdle;
        if (connectionPool != null) {
            connectionPool.setTestWhileIdle(testWhileIdle);
        }
    }

    /**
     * [Read Only] The current number of active connections that have been
     * allocated from this data source.
     */
    public synchronized int getNumActive() {
        if (connectionPool != null) {
            return connectionPool.getNumActive();
        } else {
            return 0;
        }
    }


    /**
     * [Read Only] The current number of idle connections that are waiting
     * to be allocated from this data source.
     */
    public synchronized int getNumIdle() {
        if (connectionPool != null) {
            return connectionPool.getNumIdle();
        } else {
            return 0;
        }
    }


    /**
     * The connection password to be passed to our JDBC driver to establish
     * a connection.
     */
    protected String password = null;

    public synchronized String getPassword() {
        return this.password;
    }

    public synchronized void setPassword(String password) {
        this.password = password;
        this.restartNeeded = true;
    }


    /**
     * The connection URL to be passed to our JDBC driver to establish
     * a connection.
     */
    protected String url = null;

    public synchronized String getUrl() {
        return this.url;
    }

    public synchronized void setUrl(String url) {
        this.url = url;
        this.restartNeeded = true;
    }


    /**
     * The connection username to be passed to our JDBC driver to
     * establish a connection.
     */
    protected String username = null;

    public synchronized String getUsername() {
        return this.username;
    }

    public synchronized void setUsername(String username) {
        this.username = username;
        this.restartNeeded = true;
    }


    /**
     * The SQL query that will be used to validate connections from this pool
     * before returning them to the caller.  If specified, this query
     * <strong>MUST</strong> be an SQL SELECT statement that returns at least
     * one row.
     */
    protected String validationQuery = null;

    public synchronized String getValidationQuery() {
        return this.validationQuery;
    }

    public synchronized void setValidationQuery(String validationQuery) {
        if ((validationQuery != null) && (validationQuery.trim().length() > 0)) {
            this.validationQuery = validationQuery;
        } else {
            this.validationQuery = null;
        }
        this.restartNeeded = true;
    }

    /** 
     * Controls access to the underlying connection 
     */
    private boolean accessToUnderlyingConnectionAllowed = false; 

    /**
     * Returns the value of the accessToUnderlyingConnectionAllowed property.
     * 
     * @return true if access to the underlying is allowed, false otherwise.
     */
    public synchronized boolean isAccessToUnderlyingConnectionAllowed() {
        return this.accessToUnderlyingConnectionAllowed;
    }

    /**
     * Sets the value of the accessToUnderlyingConnectionAllowed property.
     * It controls if the PoolGuard allows access to the underlying connection.
     * (Default: false)
     * 
     * @param allow Access to the underlying connection is granted when true.
     */
    public synchronized void setAccessToUnderlyingConnectionAllowed(boolean allow) {
        this.accessToUnderlyingConnectionAllowed = allow;
        this.restartNeeded = true;
    }

    // ----------------------------------------------------- Instance Variables

    // TODO: review & make isRestartNeeded() public, restartNeeded protected

    private boolean restartNeeded = false;
    
    /**
     * Returns whether or not a restart is needed.
     * @return true if a restart is needed
     */
    private synchronized boolean isRestartNeeded() {
        return restartNeeded;
    }

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
        return createDataSource().getConnection();
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
    public Connection getConnection(String username, String password) throws SQLException {
        return createDataSource().getConnection(username, password);
    }


    /**
     * Return the login timeout (in seconds) for connecting to the database.
     *
     * @exception SQLException if a database access error occurs
     */
    public int getLoginTimeout() throws SQLException {
        return createDataSource().getLoginTimeout();
    }


    /**
     * Return the log writer being used by this data source.
     *
     * @exception SQLException if a database access error occurs
     */
    public PrintWriter getLogWriter() throws SQLException {
        return createDataSource().getLogWriter();
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
        this.restartNeeded = true;
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
        this.restartNeeded = true;
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
        this.restartNeeded = true;
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
        this.restartNeeded = true;
    }

    public void removeConnectionProperty(String name) {
        connectionProperties.remove(name);
        this.restartNeeded = true;
    }

    /**
     * Close and release all connections that are currently stored in the
     * connection pool associated with our data source.
     *
     * @exception SQLException if a database error occurs
     */
    public synchronized void close() throws SQLException {
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
        if (driverClassName != null) {
            try {
                Class.forName(driverClassName);
            } catch (Throwable t) {
                String message = "Cannot load JDBC driver class '" +
                    driverClassName + "'";
                logWriter.println(message);
                t.printStackTrace(logWriter);
                throw new SQLNestedException(message, t);
            }
        }

        // Create a JDBC driver instance
        Driver driver = null;
        try {
            driver = DriverManager.getDriver(url);
        } catch (Throwable t) {
            String message = "Cannot create JDBC driver of class '" +
                (driverClassName != null ? driverClassName : "") + 
                "' for connect URL '" + url + "'";
            logWriter.println(message);
            t.printStackTrace(logWriter);
            throw new SQLNestedException(message, t);
        }

        // Can't test without a validationQuery
        if (validationQuery == null) {
            setTestOnBorrow(false);
            setTestOnReturn(false);
            setTestWhileIdle(false);
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
            log("DBCP DataSource configured without a 'username'");
        }
        
        if (password != null) {
            connectionProperties.put("password", password);
        } else {
            log("DBCP DataSource configured without a 'password'");
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
                                              defaultCatalog,
                                              abandonedConfig);
            if (connectionFactory == null) {
                throw new SQLException("Cannot create PoolableConnectionFactory");
            }
            validateConnectionFactory(connectionFactory);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new SQLNestedException("Cannot create PoolableConnectionFactory (" + e.getMessage() + ")", e);
        }

        // Create and return the pooling data source to manage the connections
        dataSource = new PoolingDataSource(connectionPool);
        ((PoolingDataSource) dataSource).setAccessToUnderlyingConnectionAllowed(isAccessToUnderlyingConnectionAllowed());
        dataSource.setLogWriter(logWriter);
        
        try {
            for (int i = 0 ; i < initialSize ; i++) {
                connectionPool.addObject();
            }
        } catch (Exception e) {
            throw new SQLNestedException("Error preloading the connection pool", e);
        }
        
        return dataSource;
    }

    private static void validateConnectionFactory(PoolableConnectionFactory connectionFactory) throws Exception {
        Connection conn = null;
        try {
            conn = (Connection) connectionFactory.makeObject();
            connectionFactory.activateObject(conn);
            connectionFactory.validateConnection(conn);
            connectionFactory.passivateObject(conn);
        }
        finally {
            connectionFactory.destroyObject(conn);
        }
    }

    private void restart() {
        try {
            close();
        } catch (SQLException e) {
            log("Could not restart DataSource, cause: " + e.getMessage());
        }
    }

    private void log(String message) {
        if (logWriter != null) {
            logWriter.println(message);
        }
    }
}
