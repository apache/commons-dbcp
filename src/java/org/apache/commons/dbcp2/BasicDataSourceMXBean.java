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
package org.apache.commons.dbcp2;

/**
 * Defines the methods that will be made available via JMX.
 *
 * @since 2.0
 */
public interface BasicDataSourceMXBean {
    
    /**
     * @see {@link BasicDataSource#getAbandonedUsageTracking()}
     * @return {@link BasicDataSource#getAbandonedUsageTracking()}
     */
    boolean getAbandonedUsageTracking();
 
    /**
     * @see {@link BasicDataSource#getDefaultAutoCommit()}
     * @return {@link BasicDataSource#getDefaultAutoCommit()}
     */
    boolean getDefaultAutoCommit();
   
    /**
     * @see {@link BasicDataSource#getDefaultReadOnly()}
     * @return {@link BasicDataSource#getDefaultReadOnly()}
     */
    boolean getDefaultReadOnly();
   
    /**
     * @see {@link BasicDataSource#getDefaultTransactionIsolation()}
     * @return {@link BasicDataSource#getDefaultTransactionIsolation()}
     */
    int getDefaultTransactionIsolation();
   
    /**
     * @see {@link BasicDataSource#getDefaultCatalog()}
     * @return {@link BasicDataSource#getDefaultCatalog()}
     */
    String getDefaultCatalog();
   
    /**
     * @see {@link BasicDataSource#getCacheState()}
     * @return {@link BasicDataSource#getCacheState()}
     */
    boolean getCacheState();
   
    /**
     * @see {@link BasicDataSource#getDriverClassName()}
     * @return {@link BasicDataSource#getDriverClassName()}
     */
    String getDriverClassName();
   
    /**
     * @see {@link BasicDataSource#getLifo()}
     * @return {@link BasicDataSource#getLifo()}
     */
    boolean getLifo();
   
    /**
     * @see {@link BasicDataSource#getMaxTotal()}
     * @return {@link BasicDataSource#getMaxTotal()}
     */
    int getMaxTotal();
   
    /**
     * @see {@link BasicDataSource#getMaxIdle()}
     * @return {@link BasicDataSource#getMaxIdle()}
     */
    int getMaxIdle();

    /**
     * @see {@link BasicDataSource#getMinIdle()}
     * @return {@link BasicDataSource#getMinIdle()}
     */
    int getMinIdle();
   
    /**
     * @see {@link BasicDataSource#getInitialSize()}
     * @return {@link BasicDataSource#getInitialSize()}
     */
    int getInitialSize();
   
    /**
     * @see {@link BasicDataSource#getMaxWaitMillis()}
     * @return {@link BasicDataSource#getMaxWaitMillis()}
     */
    long getMaxWaitMillis();
   
    /**
     * @see {@link BasicDataSource#isPoolPreparedStatements()}
     * @return {@link BasicDataSource#isPoolPreparedStatements()}
     */
    boolean isPoolPreparedStatements();
   
    /**
     * @see {@link BasicDataSource#getMaxOpenPreparedStatements()}
     * @return {@link BasicDataSource#getMaxOpenPreparedStatements()}
     */
    int getMaxOpenPreparedStatements();
   
    /**
     * @see {@link BasicDataSource#getTestOnBorrow()}
     * @return {@link BasicDataSource#getTestOnBorrow()}
     */
    boolean getTestOnBorrow();
      
    /**
     * @see {@link BasicDataSource#getTimeBetweenEvictionRunsMillis()}
     * @return {@link BasicDataSource#getTimeBetweenEvictionRunsMillis()}
     */
    long getTimeBetweenEvictionRunsMillis();
   
    /**
     * @see {@link BasicDataSource#getNumTestsPerEvictionRun()}
     * @return {@link BasicDataSource#getNumTestsPerEvictionRun()}
     */
    int getNumTestsPerEvictionRun();
   
    /**
     * @see {@link BasicDataSource#getMinEvictableIdleTimeMillis()}
     * @return {@link BasicDataSource#getMinEvictableIdleTimeMillis()}
     */
    long getMinEvictableIdleTimeMillis();
   
    /**
     * @see {@link BasicDataSource#getSoftMinEvictableIdleTimeMillis()}
     * @return {@link BasicDataSource#getSoftMinEvictableIdleTimeMillis()}
     */
    long getSoftMinEvictableIdleTimeMillis();
   
    /**
     * @see {@link BasicDataSource#getTestWhileIdle()}
     * @return {@link BasicDataSource#getTestWhileIdle()}
     */
    boolean getTestWhileIdle();
   
    /**
     * @see {@link BasicDataSource#getNumActive()}
     * @return {@link BasicDataSource#getNumActive()}
     */
    int getNumActive();
   
    /**
     * @see {@link BasicDataSource#getNumIdle()}
     * @return {@link BasicDataSource#getNumIdle()}
     */
    int getNumIdle();
   
    /**
     * @see {@link BasicDataSource#getPassword()}
     * @return {@link BasicDataSource#getPassword()}
     */
    String getPassword();
   
    /**
     * @see {@link BasicDataSource#getUrl()}
     * @return {@link BasicDataSource#getUrl()}
     */
    String getUrl();
   
    /**
     * @see {@link BasicDataSource#getUsername()}
     * @return {@link BasicDataSource#getUsername()}
     */
    String getUsername();
   
    /**
     * @see {@link BasicDataSource#getValidationQuery()}
     * @return {@link BasicDataSource#getValidationQuery()}
     */
    String getValidationQuery();
   
    /**
     * @see {@link BasicDataSource#getValidationQueryTimeout()}
     * @return {@link BasicDataSource#getValidationQueryTimeout()}
     */
    int getValidationQueryTimeout();
   
    /**
     * @see {@link BasicDataSource#getConnectionInitSqlsAsArray()}
     * @return {@link BasicDataSource#getConnectionInitSqlsAsArray()}
     */
    String[] getConnectionInitSqlsAsArray();
   
    /**
     * @see {@link BasicDataSource#isAccessToUnderlyingConnectionAllowed()}
     * @return {@link BasicDataSource#isAccessToUnderlyingConnectionAllowed()}
     */
    boolean isAccessToUnderlyingConnectionAllowed();
   
    /**
     * @see {@link BasicDataSource#getMaxConnLifetimeMillis()}
     * @return {@link BasicDataSource#getMaxConnLifetimeMillis()}
     */
    long getMaxConnLifetimeMillis();
   
    /**
     * @see {@link BasicDataSource#getRemoveAbandonedOnBorrow()}
     * @return {@link BasicDataSource#getRemoveAbandonedOnBorrow()}
     */
    boolean getRemoveAbandonedOnBorrow();
   
    /**
     * @see {@link BasicDataSource#getRemoveAbandonedOnMaintenance()}
     * @return {@link BasicDataSource#getRemoveAbandonedOnMaintenance()}
     */
    boolean getRemoveAbandonedOnMaintenance();
   
    /**
     * @see {@link BasicDataSource#getRemoveAbandonedTimeout()}
     * @return {@link BasicDataSource#getRemoveAbandonedTimeout()}
     */
    int getRemoveAbandonedTimeout();
   
    /**
     * @see {@link BasicDataSource#getLogAbandoned()}
     * @return {@link BasicDataSource#getLogAbandoned()}
     */
    boolean getLogAbandoned();
   
    /**
     * @see {@link BasicDataSource#isClosed()}
     * @return {@link BasicDataSource#isClosed()}
     */
    boolean isClosed();
}
