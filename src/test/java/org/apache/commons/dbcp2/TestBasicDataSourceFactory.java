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

import java.sql.Connection;
import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * TestSuite for BasicDataSourceFactory
 *
 * @author Dirk Verbeeck
 * @version $Revision$ $Date$
 */
public class TestBasicDataSourceFactory extends TestCase {
    public TestBasicDataSourceFactory(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(TestBasicDataSourceFactory.class);
    }

    public void testNoProperties() throws Exception {
        Properties properties = new Properties();
        BasicDataSource ds = BasicDataSourceFactory.createDataSource(properties);

        assertNotNull(ds);
    }

    public void testProperties() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("driverClassName", "org.apache.commons.dbcp2.TesterDriver");
        properties.setProperty("url", "jdbc:apache:commons:testdriver");
        properties.setProperty("maxTotal", "10");
        properties.setProperty("maxIdle", "8");
        properties.setProperty("minIdle", "0");
        properties.setProperty("maxWaitMillis", "500");
        properties.setProperty("initialSize", "5");
        properties.setProperty("defaultAutoCommit", "true");
        properties.setProperty("defaultReadOnly", "false");
        properties.setProperty("defaultTransactionIsolation", "READ_COMMITTED");
        properties.setProperty("defaultCatalog", "test");
        properties.setProperty("testOnBorrow", "true");
        properties.setProperty("testOnReturn", "false");
        properties.setProperty("username", "username");
        properties.setProperty("password", "password");
        properties.setProperty("validationQuery", "SELECT DUMMY FROM DUAL");
        properties.setProperty("validationQueryTimeout", "100");
        properties.setProperty("connectionInitSqls", "SELECT 1;SELECT 2");
        properties.setProperty("timeBetweenEvictionRunsMillis", "1000");
        properties.setProperty("minEvictableIdleTimeMillis", "2000");
        properties.setProperty("softMinEvictableIdleTimeMillis", "3000");
        properties.setProperty("numTestsPerEvictionRun", "2");
        properties.setProperty("testWhileIdle", "true");
        properties.setProperty("accessToUnderlyingConnectionAllowed", "true");
        properties.setProperty("removeAbandoned", "true");
        properties.setProperty("removeAbandonedTimeout", "3000");
        properties.setProperty("logAbandoned", "true");
        properties.setProperty("poolPreparedStatements", "true");
        properties.setProperty("maxOpenPreparedStatements", "10");
        properties.setProperty("lifo", "true");

        BasicDataSource ds = BasicDataSourceFactory.createDataSource(properties);

        assertEquals("org.apache.commons.dbcp2.TesterDriver", ds.getDriverClassName());
        assertEquals("jdbc:apache:commons:testdriver", ds.getUrl());
        assertEquals(10, ds.getMaxTotal());
        assertEquals(8, ds.getMaxIdle());
        assertEquals(0, ds.getMinIdle());
        assertEquals(500, ds.getMaxWaitMillis());
        assertEquals(5, ds.getInitialSize());
        assertEquals(5, ds.getNumIdle());
        assertEquals(Boolean.TRUE, ds.getDefaultAutoCommit());
        assertEquals(Boolean.FALSE, ds.getDefaultReadOnly());
        assertEquals(Connection.TRANSACTION_READ_COMMITTED, ds.getDefaultTransactionIsolation());
        assertEquals("test", ds.getDefaultCatalog());
        assertEquals(true, ds.getTestOnBorrow());
        assertEquals(false, ds.getTestOnReturn());
        assertEquals("username", ds.getUsername());
        assertEquals("password", ds.getPassword());
        assertEquals("SELECT DUMMY FROM DUAL", ds.getValidationQuery());
        assertEquals(100, ds.getValidationQueryTimeout());
        assertEquals(2, ds.getConnectionInitSqls().size());
        assertEquals("SELECT 1", ds.getConnectionInitSqls().get(0));
        assertEquals("SELECT 2", ds.getConnectionInitSqls().get(1));
        assertEquals(1000, ds.getTimeBetweenEvictionRunsMillis());
        assertEquals(2000, ds.getMinEvictableIdleTimeMillis());
        assertEquals(3000, ds.getSoftMinEvictableIdleTimeMillis());
        assertEquals(2, ds.getNumTestsPerEvictionRun());
        assertEquals(true, ds.getTestWhileIdle());
        assertEquals(true, ds.isAccessToUnderlyingConnectionAllowed());
        assertEquals(false, ds.getRemoveAbandonedOnBorrow());
        assertEquals(false, ds.getRemoveAbandonedOnMaintenance());
        assertEquals(3000, ds.getRemoveAbandonedTimeout());
        assertEquals(true, ds.getLogAbandoned());
        assertEquals(true, ds.isPoolPreparedStatements());
        assertEquals(10, ds.getMaxOpenPreparedStatements());
        assertEquals(true, ds.getLifo());
    }
}
