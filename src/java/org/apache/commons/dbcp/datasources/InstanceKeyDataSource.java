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

import java.io.Serializable;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.NoSuchElementException;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.naming.Referenceable;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.DataSource;
import javax.sql.PooledConnection;

import org.apache.commons.dbcp.SQLNestedException;
import org.apache.commons.pool.impl.GenericObjectPool;

/**
 * <p>The base class for <code>SharedPoolDataSource</code> and 
 * <code>PerUserPoolDataSource</code>.  Many of the configuration properties
 * are shared and defined here.  This class is declared public in order
 * to allow particular usage with commons-beanutils; do not make direct
 * use of it outside of commons-dbcp.
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
 * Although normally used within a JNDI environment, A DataSource
 * can be instantiated and initialized as any bean.  In this case the 
 * <code>ConnectionPoolDataSource</code> will likely be instantiated in
 * a similar manner.  This class allows the physical source of connections
 * to be attached directly to this pool using the 
 * {@link #setConnectionPoolDataSource(ConnectionPoolDataSource)} method.
 * </p>
 *
 * <p>
 * The dbcp package contains an adapter, 
 * {@link org.apache.commons.dbcp.cpdsadapter.DriverAdapterCPDS},
 * that can be used to allow the use of <code>DataSource</code>'s based on this
 * class with jdbc driver implementations that do not supply a 
 * <code>ConnectionPoolDataSource</code>, but still
 * provide a {@link java.sql.Driver} implementation.
 * </p>
 *
 * <p>
 * The <a href="package-summary.html">package documentation</a> contains an 
 * example using catalina and JNDI and it also contains a non-JNDI example. 
 * </p>
 *
 * @author John D. McNally
 * @version $Revision: 1.11 $ $Date: 2004/02/28 12:18:17 $
 */
public abstract class InstanceKeyDataSource
        implements DataSource, Referenceable, Serializable {
    private static final String GET_CONNECTION_CALLED 
            = "A Connection was already requested from this source, " 
            + "further initialization is not allowed.";
    private static final String BAD_TRANSACTION_ISOLATION
        = "The requested TransactionIsolation level is invalid.";
    /**
    * Internal constant to indicate the level is not set. 
    */
    protected static final int UNKNOWN_TRANSACTIONISOLATION = -1;

    private boolean getConnectionCalled = false;

    private ConnectionPoolDataSource cpds = null;
    /** DataSource Name used to find the ConnectionPoolDataSource */
    private String dataSourceName = null;
    private boolean defaultAutoCommit = false;
    private int defaultTransactionIsolation = UNKNOWN_TRANSACTIONISOLATION;
    private int maxActive = GenericObjectPool.DEFAULT_MAX_ACTIVE;
    private int maxIdle = GenericObjectPool.DEFAULT_MAX_IDLE;
    private int maxWait = (int)Math.min((long)Integer.MAX_VALUE,
        GenericObjectPool.DEFAULT_MAX_WAIT);
    private boolean defaultReadOnly = false;
    /** Description */
    private String description = null;
    /** Environment that may be used to set up a jndi initial context. */
    Properties jndiEnvironment = null;
    /** Login TimeOut in seconds */
    private int loginTimeout = 0;
    /** Log stream */
    private PrintWriter logWriter = null;
    private boolean _testOnBorrow = GenericObjectPool.DEFAULT_TEST_ON_BORROW;
    private boolean _testOnReturn = GenericObjectPool.DEFAULT_TEST_ON_RETURN;
    private int _timeBetweenEvictionRunsMillis = (int)
        Math.min((long)Integer.MAX_VALUE,
                 GenericObjectPool.DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS);
    private int _numTestsPerEvictionRun = 
        GenericObjectPool.DEFAULT_NUM_TESTS_PER_EVICTION_RUN;
    private int _minEvictableIdleTimeMillis = (int)
    Math.min((long)Integer.MAX_VALUE,
             GenericObjectPool.DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS);
    private boolean _testWhileIdle = GenericObjectPool.DEFAULT_TEST_WHILE_IDLE;
    private String validationQuery = null;
    private boolean testPositionSet = false;

    protected String instanceKey = null;

    /**
     * Default no-arg constructor for Serialization
     */
    public InstanceKeyDataSource() {
        defaultAutoCommit = true;
    }

    /**
     * Throws an IllegalStateException, if a PooledConnection has already
     * been requested.
     */
    protected void assertInitializationAllowed()
        throws IllegalStateException {
        if (getConnectionCalled) {
            throw new IllegalStateException(GET_CONNECTION_CALLED);
        }
    }

    /**
     * Close pool being maintained by this datasource.
     */
    public abstract void close() throws Exception;

    // -------------------------------------------------------------------
    // Properties

    /**
     * Get the value of connectionPoolDataSource.  This method will return
     * null, if the backing datasource is being accessed via jndi.
     *
     * @return value of connectionPoolDataSource.
     */
    public ConnectionPoolDataSource getConnectionPoolDataSource() {
        return cpds;
    }
    
    /**
     * Set the backend ConnectionPoolDataSource.  This property should not be
     * set if using jndi to access the datasource.
     *
     * @param v  Value to assign to connectionPoolDataSource.
     */
    public void setConnectionPoolDataSource(ConnectionPoolDataSource v) {
        assertInitializationAllowed();
        if (dataSourceName != null) {
            throw new IllegalStateException(
                "Cannot set the DataSource, if JNDI is used.");
        }
        if (cpds != null) 
        {
            throw new IllegalStateException(
                "The CPDS has already been set. It cannot be altered.");
        }
        cpds = v;
        instanceKey = InstanceKeyObjectFactory.registerNewInstance(this);
    }

    /**
     * Get the name of the ConnectionPoolDataSource which backs this pool.
     * This name is used to look up the datasource from a jndi service 
     * provider.
     *
     * @return value of dataSourceName.
     */
    public String getDataSourceName() {
        return dataSourceName;
    }
    
    /**
     * Set the name of the ConnectionPoolDataSource which backs this pool.
     * This name is used to look up the datasource from a jndi service 
     * provider.
     *
     * @param v  Value to assign to dataSourceName.
     */
    public void setDataSourceName(String v) {
        assertInitializationAllowed();
        if (cpds != null) {
            throw new IllegalStateException(
                "Cannot set the JNDI name for the DataSource, if already " +
                "set using setConnectionPoolDataSource.");
        }
        if (dataSourceName != null) 
        {
            throw new IllegalStateException(
                "The DataSourceName has already been set. " + 
                "It cannot be altered.");
        }
        this.dataSourceName = v;
        instanceKey = InstanceKeyObjectFactory.registerNewInstance(this);
    }

    /** 
     * Get the value of defaultAutoCommit, which defines the state of 
     * connections handed out from this pool.  The value can be changed
     * on the Connection using Connection.setAutoCommit(boolean).
     * The default is true.
     *
     * @return value of defaultAutoCommit.
     */
    public boolean isDefaultAutoCommit() {
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
    public void setDefaultAutoCommit(boolean v) {
        assertInitializationAllowed();
        this.defaultAutoCommit = v;
    }

    /**
     * Get the value of defaultReadOnly, which defines the state of 
     * connections handed out from this pool.  The value can be changed
     * on the Connection using Connection.setReadOnly(boolean).
     * The default is false.
     *
     * @return value of defaultReadOnly.
     */
    public boolean isDefaultReadOnly() {
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
    public void setDefaultReadOnly(boolean v) {
        assertInitializationAllowed();
        this.defaultReadOnly = v;
    }

    /**
     * Get the value of defaultTransactionIsolation, which defines the state of
     * connections handed out from this pool.  The value can be changed
     * on the Connection using Connection.setTransactionIsolation(int).
     * If this method returns -1, the default is JDBC driver dependent.
     * 
     * @return value of defaultTransactionIsolation.
     */
    public int getDefaultTransactionIsolation() {
            return defaultTransactionIsolation;
    }

    /**
     * Set the value of defaultTransactionIsolation, which defines the state of
     * connections handed out from this pool.  The value can be changed
     * on the Connection using Connection.setTransactionIsolation(int).
     * The default is JDBC driver dependent.
     * 
     * @param v  Value to assign to defaultTransactionIsolation
     */
    public void setDefaultTransactionIsolation(int v) {
        assertInitializationAllowed();
        switch (v) {
        case Connection.TRANSACTION_NONE:
        case Connection.TRANSACTION_READ_COMMITTED:
        case Connection.TRANSACTION_READ_UNCOMMITTED:
        case Connection.TRANSACTION_REPEATABLE_READ:
        case Connection.TRANSACTION_SERIALIZABLE:
            break;
        default:
            throw new IllegalArgumentException(BAD_TRANSACTION_ISOLATION);
        }
        this.defaultTransactionIsolation = v;
    }
    
    /**
     * Get the description.  This property is defined by jdbc as for use with
     * GUI (or other) tools that might deploy the datasource.  It serves no
     * internal purpose.
     *
     * @return value of description.
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Set the description.  This property is defined by jdbc as for use with
     * GUI (or other) tools that might deploy the datasource.  It serves no
     * internal purpose.
     * 
     * @param v  Value to assign to description.
     */
    public void setDescription(String v) {
        this.description = v;
    }
        
    /**
     * Get the value of jndiEnvironment which is used when instantiating
     * a jndi InitialContext.  This InitialContext is used to locate the
     * backend ConnectionPoolDataSource.
     *
     * @return value of jndiEnvironment.
     */
    public String getJndiEnvironment(String key) {
        String value = null;
        if (jndiEnvironment != null) {
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
    public void setJndiEnvironment(String key, String value) {
        if (jndiEnvironment == null) {
            jndiEnvironment = new Properties();
        }
        jndiEnvironment.setProperty(key, value);
    }
    
    /**
     * Get the value of loginTimeout.
     * @return value of loginTimeout.
     */
    public int getLoginTimeout() {
        return loginTimeout;
    }
    
    /**
     * Set the value of loginTimeout.
     * @param v  Value to assign to loginTimeout.
     */
    public void setLoginTimeout(int v) {
        this.loginTimeout = v;
    }
        
    /**
     * Get the value of logWriter.
     * @return value of logWriter.
     */
    public PrintWriter getLogWriter() {
        if (logWriter == null) {
            logWriter = new PrintWriter(System.out);
        }        
        return logWriter;
    }
    
    /**
     * Set the value of logWriter.
     * @param v  Value to assign to logWriter.
     */
    public void setLogWriter(PrintWriter v) {
        this.logWriter = v;
    }
    
    /**
     * @see #getTestOnBorrow
     */
    public final boolean isTestOnBorrow() {
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
     * @see #setTestOnBorrow
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
     * @see #getTestOnBorrow
     */
    public void setTestOnBorrow(boolean testOnBorrow) {
        assertInitializationAllowed();
        _testOnBorrow = testOnBorrow;
        testPositionSet = true;
    }

    /**
     * @see #getTestOnReturn
     */
    public final boolean isTestOnReturn() {
        return getTestOnReturn();
    }
    
    /**
     * When <tt>true</tt>, objects will be
     * {*link PoolableObjectFactory#validateObject validated}
     * before being returned to the pool within the
     * {*link #returnObject}.
     *
     * @see #setTestOnReturn
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
     * @see #getTestOnReturn
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
     * @see #setTimeBetweenEvictionRunsMillis
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
     * @see #getTimeBetweenEvictionRunsMillis
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
     * @see #setNumTestsPerEvictionRun
     * @see #setTimeBetweenEvictionRunsMillis
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
     * @see #getNumTestsPerEvictionRun
     * @see #setTimeBetweenEvictionRunsMillis
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
     * @see #setMinEvictableIdleTimeMillis
     * @see #setTimeBetweenEvictionRunsMillis
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
     * @see #getMinEvictableIdleTimeMillis
     * @see #setTimeBetweenEvictionRunsMillis
     */
    public void setMinEvictableIdleTimeMillis(int minEvictableIdleTimeMillis) {
        assertInitializationAllowed();
        _minEvictableIdleTimeMillis = minEvictableIdleTimeMillis;
    }

    /**
     * @see #getTestWhileIdle
     */
    public final boolean isTestWhileIdle() {
        return getTestWhileIdle();
    }
    
    /**
     * When <tt>true</tt>, objects will be
     * {*link PoolableObjectFactory#validateObject validated}
     * by the idle object evictor (if any).  If an object
     * fails to validate, it will be dropped from the pool.
     *
     * @see #setTestWhileIdle
     * @see #setTimeBetweenEvictionRunsMillis
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
     * @see #getTestWhileIdle
     * @see #setTimeBetweenEvictionRunsMillis
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
        if (!testPositionSet) {
            setTestOnBorrow(true);
        }
    }

    // ----------------------------------------------------------------------
    // Instrumentation Methods

    // ----------------------------------------------------------------------
    // DataSource implementation 

    /**
     * Attempt to establish a database connection.
     */
    public Connection getConnection() throws SQLException {
        return getConnection(null, null);
    }

    /**
     * Attempt to establish a database connection.
     */
    public Connection getConnection(String username, String password)
            throws SQLException {        
        if (instanceKey == null) {
            throw new SQLException("Must set the ConnectionPoolDataSource " 
                    + "through setDataSourceName or setConnectionPoolDataSource"
                    + " before calling getConnection.");
        }
        getConnectionCalled = true;
        PooledConnectionAndInfo info = null;
        try {
            info = getPooledConnectionAndInfo(username, password);
        } catch (NoSuchElementException e) {
            closeDueToException(info);
            throw new SQLNestedException("Cannot borrow connection from pool", e);
        } catch (RuntimeException e) {
            closeDueToException(info);
            throw e;
        } catch (SQLException e) {            
            closeDueToException(info);
            throw e;
        } catch (Exception e) {
            closeDueToException(info);
            throw new SQLNestedException("Cannot borrow connection from pool", e);
        }
        
        if (!(null == password ? null == info.getPassword() 
                : password.equals(info.getPassword()))) {
            closeDueToException(info);
            throw new SQLException("Given password did not match password used"
                                   + " to create the PooledConnection.");
        }

        Connection con = info.getPooledConnection().getConnection();        
        setupDefaults(con, username);
        con.clearWarnings();
        return con;
    }

    protected abstract PooledConnectionAndInfo 
        getPooledConnectionAndInfo(String username, String password)
        throws SQLException;

    protected abstract void setupDefaults(Connection con, String username) 
        throws SQLException;

        
    private void closeDueToException(PooledConnectionAndInfo info) {
        if (info != null) {
            try {
                info.getPooledConnection().getConnection().close();
            } catch (Exception e) {
                // do not throw this exception because we are in the middle
                // of handling another exception.  But record it because
                // it potentially leaks connections from the pool.
                getLogWriter().println("[ERROR] Could not return connection to "
                    + "pool during exception handling. " + e.getMessage());   
            }
        }
    }

    protected ConnectionPoolDataSource 
        testCPDS(String username, String password)
        throws javax.naming.NamingException, SQLException {
        // The source of physical db connections
        ConnectionPoolDataSource cpds = this.cpds;
        if (cpds == null) {            
            Context ctx = null;
            if (jndiEnvironment == null) {
                ctx = new InitialContext();                
            } else {
                ctx = new InitialContext(jndiEnvironment);
            }
            Object ds = ctx.lookup(dataSourceName);
            if (ds instanceof ConnectionPoolDataSource) {
                cpds = (ConnectionPoolDataSource) ds;
            } else {
                throw new SQLException("Illegal configuration: "
                    + "DataSource " + dataSourceName
                    + " (" + ds.getClass().getName() + ")"
                    + " doesn't implement javax.sql.ConnectionPoolDataSource");
            }
        }
        
        // try to get a connection with the supplied username/password
        PooledConnection conn = null;
        try {
            if (username != null) {
                conn = cpds.getPooledConnection(username, password);
            }
            else {
                conn = cpds.getPooledConnection();
            }
            if (conn == null) {
                throw new SQLException(
                    "Cannot connect using the supplied username/password");
            }
        }
        finally {
            if (conn != null) {
                try {
                    conn.close();
                }
                catch (SQLException e) {
                    // at least we could connect
                }
            }
        }
        return cpds;
    }

    protected byte whenExhaustedAction(int maxActive, int maxWait) {
        byte whenExhausted = GenericObjectPool.WHEN_EXHAUSTED_BLOCK;
        if (maxActive <= 0) {
            whenExhausted = GenericObjectPool.WHEN_EXHAUSTED_GROW;
        } else if (maxWait == 0) {
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
    public Reference getReference() throws NamingException {
        Reference ref = new Reference(getClass().getName(), 
            InstanceKeyObjectFactory.class.getName(), null);
        ref.add(new StringRefAddr("instanceKey", instanceKey));
        return ref;
    }
}
