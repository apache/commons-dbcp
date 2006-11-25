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

package org.apache.commons.dbcp;

import java.util.Properties;

import javax.sql.DataSource;

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
        DataSource ds = BasicDataSourceFactory.createDataSource(properties);
        
        assertNotNull(ds);
        assertTrue(ds instanceof BasicDataSource);
    }

    public void testProperties() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("driverClassName", "org.apache.commons.dbcp.TesterDriver");
        properties.setProperty("url", "jdbc:apache:commons:testdriver");
        properties.setProperty("maxActive", "10");
        properties.setProperty("maxWait", "500");
        properties.setProperty("defaultAutoCommit", "true");
        properties.setProperty("defaultReadOnly", "false");
        properties.setProperty("defaultTransactionIsolation", "READ_COMMITTED");
        properties.setProperty("defaultCatalog", "test");
        properties.setProperty("username", "username");
        properties.setProperty("password", "password");
        properties.setProperty("validationQuery", "SELECT DUMMY FROM DUAL");

        BasicDataSource ds = (BasicDataSource) BasicDataSourceFactory.createDataSource(properties);
        
        assertEquals("jdbc:apache:commons:testdriver", ds.getUrl());
        assertEquals(10, ds.getMaxActive());
        assertEquals(true, ds.getDefaultAutoCommit());
    }
}
