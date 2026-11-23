/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.dbcp2;

import java.sql.SQLException;

/**
 * Defines the methods that will be made available via
 * <a href="https://docs.oracle.com/javase/8/docs/technotes/guides/management/agent.html">JMX</a>.
 *
 * @since 2.9.0
 */
public interface DataSourceMXBean {

    /**
     * Gets whether a connection pool that implements {@link org.apache.commons.pool2.UsageTracking UsageTracking} records a stack trace every time a method is
     * called on a pooled connection and retains the most recent stack trace to aid debugging of abandoned connections. See
     * {@link BasicDataSource#getAbandonedUsageTracking()}.
     *
     * @return {@link BasicDataSource#getAbandonedUsageTracking()}
     */
    boolean getAbandonedUsageTracking();

    /**
     * Gets the state caching flag. See {@link BasicDataSource#getCacheState()}.
     *
     * @return {@link BasicDataSource#getCacheState()}.
     */
    boolean getCacheState();

    /**
     * Gets the same data as {@link BasicDataSource#getConnectionInitSqls()} but in an array so it is accessible via JMX. See
     * {@link BasicDataSource#getConnectionInitSqlsAsArray()}.
     *
     * @return {@link BasicDataSource#getConnectionInitSqlsAsArray()}.
     */
    String[] getConnectionInitSqlsAsArray();

    /**
     * Gets the default auto-commit property. See {@link BasicDataSource#getDefaultAutoCommit()}.
     *
     * @return {@link BasicDataSource#getDefaultAutoCommit()}.
     */
    Boolean getDefaultAutoCommit();

    /**
     * Gets the default catalog. See {@link BasicDataSource#getDefaultCatalog()}.
     *
     * @return {@link BasicDataSource#getDefaultCatalog()}.
     */
    String getDefaultCatalog();

    /**
     * Gets the default readOnly property. See {@link BasicDataSource#getDefaultReadOnly()}.
     *
     * @return {@link BasicDataSource#getDefaultReadOnly()}.
     */
    Boolean getDefaultReadOnly();

    /**
     * Gets the default schema. See {@link BasicDataSource#getDefaultSchema()}.
     *
     * @return {@link BasicDataSource#getDefaultSchema()}.
     * @since 2.5.0
     */
    default String getDefaultSchema() {
        return null;
    }

    /**
     * Gets the default transaction isolation state of returned connections. See {@link BasicDataSource#getDefaultTransactionIsolation()}.
     *
     * @return {@link BasicDataSource#getDefaultTransactionIsolation()}.
     */
    int getDefaultTransactionIsolation();

    /**
     * Gets the same data as {@link BasicDataSource#getDisconnectionIgnoreSqlCodes()} but in an array, so it is accessible via JMX. See
     * {@link BasicDataSource#getDisconnectionIgnoreSqlCodesAsArray()}.
     *
     * @return {@link BasicDataSource#getDisconnectionIgnoreSqlCodesAsArray()}.
     * @since 2.13.0
     */
    default String[] getDisconnectionIgnoreSqlCodesAsArray() {
        return Utils.EMPTY_STRING_ARRAY;
    }

    /**
     * Gets the same data as {@link BasicDataSource#getDisconnectionSqlCodes} but in an array so it is accessible via JMX. See
     * {@link BasicDataSource#getDisconnectionSqlCodesAsArray()}.
     *
     * @return {@link BasicDataSource#getDisconnectionSqlCodesAsArray()}.
     * @since 2.1
     */
    String[] getDisconnectionSqlCodesAsArray();

    /**
     * Gets the JDBC driver class name. See {@link BasicDataSource#getDriverClassName()}.
     *
     * @return {@link BasicDataSource#getDriverClassName()}.
     */
    String getDriverClassName();

    /**
     * Gets whether validation fails immediately for connections that have previously thrown SQLExceptions with SQL states indicating fatal disconnection
     * errors. See {@link BasicDataSource#getFastFailValidation()}.
     *
     * @return {@link BasicDataSource#getFastFailValidation()}.
     * @since 2.1
     */
    boolean getFastFailValidation();

    /**
     * Gets the initial size of the connection pool. See {@link BasicDataSource#getInitialSize()}.
     *
     * @return {@link BasicDataSource#getInitialSize()}.
     */
    int getInitialSize();

    /**
     * Gets the LIFO property. See {@link BasicDataSource#getLifo()}.
     *
     * @return {@link BasicDataSource#getLifo()}.
     */
    boolean getLifo();

    /**
     * Gets whether to log stack traces for application code that abandoned a Statement or Connection. See {@link BasicDataSource#getLogAbandoned()}.
     *
     * @return {@link BasicDataSource#getLogAbandoned()}.
     */
    boolean getLogAbandoned();

    /**
     * Gets whether to log messages when the pool closes connections that exceed the maximum lifetime configured by
     * {@link BasicDataSource#getMaxConnDuration()}. See {@link BasicDataSource#getLogExpiredConnections()}.
     *
     * @return {@link BasicDataSource#getLogExpiredConnections()}.
     * @since 2.1
     */
    boolean getLogExpiredConnections();

    /**
     * Gets the maximum permitted lifetime of a connection in milliseconds. See {@link BasicDataSource#getMaxConnLifetimeMillis()}.
     *
     * @return {@link BasicDataSource#getMaxConnLifetimeMillis()}.
     */
    long getMaxConnLifetimeMillis();

    /**
     * Gets the maximum number of connections that can remain idle in the pool. See {@link BasicDataSource#getMaxIdle()}.
     *
     * @return {@link BasicDataSource#getMaxIdle()}.
     */
    int getMaxIdle();

    /**
     * Gets the value of the {@code maxOpenPreparedStatements} property. See {@link BasicDataSource#getMaxOpenPreparedStatements()}.
     *
     * @return {@link BasicDataSource#getMaxOpenPreparedStatements()}.
     */
    int getMaxOpenPreparedStatements();

    /**
     * Gets the maximum number of active connections that can be allocated at the same time. See {@link BasicDataSource#getMaxTotal()}.
     *
     * @return {@link BasicDataSource#getMaxTotal()}.
     */
    int getMaxTotal();

    /**
     * Gets the maximum number of milliseconds that the pool will wait for a connection to be returned before throwing an exception. See
     * {@link BasicDataSource#getMaxWaitMillis()}.
     *
     * @return {@link BasicDataSource#getMaxWaitMillis()}.
     */
    long getMaxWaitMillis();

    /**
     * Gets the {@code minEvictableIdleDuration} property. See {@link BasicDataSource#getMinEvictableIdleTimeMillis()}.
     *
     * @return {@link BasicDataSource#getMinEvictableIdleTimeMillis()}.
     */
    long getMinEvictableIdleTimeMillis();

    /**
     * Gets the minimum number of idle connections in the pool. See {@link BasicDataSource#getMinIdle()}.
     *
     * @return {@link BasicDataSource#getMinIdle()}.
     */
    int getMinIdle();

    /**
     * Gets the current number of active connections that have been allocated from this data source. See {@link BasicDataSource#getNumActive()}.
     *
     * @return {@link BasicDataSource#getNumActive()}.
     */
    int getNumActive();

    /**
     * Gets the current number of idle connections that are waiting to be allocated from this data source. See {@link BasicDataSource#getNumIdle()}.
     *
     * @return {@link BasicDataSource#getNumIdle()}.
     */
    int getNumIdle();

    /**
     * Gets the value of the {@code numTestsPerEvictionRun} property. See {@link BasicDataSource#getNumTestsPerEvictionRun()}.
     *
     * @return {@link BasicDataSource#getNumTestsPerEvictionRun()}.
     */
    int getNumTestsPerEvictionRun();

    /**
     * Gets whether to remove abandoned connections that exceed the removeAbandonedTimeout when borrowObject is invoked. See
     * {@link BasicDataSource#getRemoveAbandonedOnBorrow()}.
     *
     * @return {@link BasicDataSource#getRemoveAbandonedOnBorrow()}.
     */
    boolean getRemoveAbandonedOnBorrow();

    /**
     * Gets whether to remove abandoned connections that exceed the removeAbandonedTimeout during pool maintenance. See
     * {@link BasicDataSource#getRemoveAbandonedOnMaintenance()}.
     *
     * @return {@link BasicDataSource#getRemoveAbandonedOnMaintenance()}.
     */
    boolean getRemoveAbandonedOnMaintenance();

    /**
     * Gets the timeout in seconds before an abandoned connection can be removed. See {@link BasicDataSource#getRemoveAbandonedTimeout()}.
     *
     * @return {@link BasicDataSource#getRemoveAbandonedTimeout()}.
     */
    int getRemoveAbandonedTimeout();

    /**
     * Gets the minimum amount of time a connection may sit idle in the pool before it is eligible for eviction by the idle object evictor, with the extra
     * condition that at least "minIdle" connections remain in the pool. See {@link BasicDataSource#getSoftMinEvictableIdleTimeMillis()}.
     *
     * @return {@link BasicDataSource#getSoftMinEvictableIdleTimeMillis()}.
     */
    long getSoftMinEvictableIdleTimeMillis();

    /**
     * Gets the {@code testOnBorrow} property. See {@link BasicDataSource#getTestOnBorrow()}.
     *
     * @return {@link BasicDataSource#getTestOnBorrow()}.
     */
    boolean getTestOnBorrow();

    /**
     * Gets the {@code testOnCreate} property. See {@link BasicDataSource#getTestOnCreate()}.
     *
     * @return {@link BasicDataSource#getTestOnCreate()}.
     */
    boolean getTestOnCreate();

    /**
     * Gets the value of the {@code testWhileIdle} property. See {@link BasicDataSource#getTestWhileIdle()}.
     *
     * @return {@link BasicDataSource#getTestWhileIdle()}.
     */
    boolean getTestWhileIdle();

    /**
     * Gets the value of the {@code durationBetweenEvictionRuns} property. See {@link BasicDataSource#getTimeBetweenEvictionRunsMillis()}.
     *
     * @return {@link BasicDataSource#getTimeBetweenEvictionRunsMillis()}.
     */
    long getTimeBetweenEvictionRunsMillis();

    /**
     * Gets the JDBC connection {@code connectionString} property. See {@link BasicDataSource#getUrl()}.
     *
     * @return {@link BasicDataSource#getUrl()}.
     */
    String getUrl();

    /**
     * Gets the JDBC connection {@code userName} property. See {@link BasicDataSource#getUsername()}.
     *
     * @return {@link BasicDataSource#getUsername()}.
     * @deprecated Use {@link #getUserName()}.
     */
    @Deprecated
    String getUsername();

    /**
     * Gets the JDBC connection {@code userName} property. See {@link BasicDataSource#getUsername()}.
     *
     * @return {@link BasicDataSource#getUsername()}.
     * @since 2.11.0
     */
    default String getUserName() {
        return getUsername();
    }

    /**
     * Gets the validation query used to validate connections before returning them. See {@link BasicDataSource#getValidationQuery()}.
     *
     * @return {@link BasicDataSource#getValidationQuery()}.
     */
    String getValidationQuery();

    /**
     * Gets the validation query timeout. See {@link BasicDataSource#getValidationQueryTimeout()}.
     *
     * @return {@link BasicDataSource#getValidationQueryTimeout()}.
     */
    int getValidationQueryTimeout();

    /**
     * Tests whether access to the underlying connection is allowed. See {@link BasicDataSource#isAccessToUnderlyingConnectionAllowed()}.
     *
     * @return {@link BasicDataSource#isAccessToUnderlyingConnectionAllowed()}.
     */
    boolean isAccessToUnderlyingConnectionAllowed();

    /**
     * Tests whether the statement pool is cleared when the connection is returned to its pool. See {@link BasicDataSource#isClearStatementPoolOnReturn()}.
     *
     * @return {@link BasicDataSource#isClearStatementPoolOnReturn()}.
     * @since 2.8.0
     */
    default boolean isClearStatementPoolOnReturn() {
        return false;
    }

    /**
     * Tests whether this data source is closed and no more connections can be retrieved from it. See {@link BasicDataSource#isClosed()}.
     *
     * @return {@link BasicDataSource#isClosed()}.
     */
    boolean isClosed();

    /**
     * Tests whether prepared statements are pooled. See {@link BasicDataSource#isPoolPreparedStatements()}.
     *
     * @return {@link BasicDataSource#isPoolPreparedStatements()}.
     */
    boolean isPoolPreparedStatements();

    /**
     * Restarts the datasource. See {@link BasicDataSource#restart()}
     *
     * @throws SQLException Thrown if an error occurs initializing the data source.
     * @since 2.8.0
     */
    default void restart() throws SQLException {
        // do nothing by default?
    }

    /**
     * Starts the datasource. See {@link BasicDataSource#start()}
     *
     * @throws SQLException Thrown if an error occurs initializing the data source.
     * @since 2.8.0
     */
    default void start() throws SQLException {
        // do nothing
    }
}
