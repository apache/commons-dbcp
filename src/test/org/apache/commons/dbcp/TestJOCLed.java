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

import java.sql.Connection;
import java.sql.DriverManager;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * @author Rodney Waldhoff
 * @version $Revision$ $Date$
 */
public class TestJOCLed extends TestConnectionPool {
    public TestJOCLed(String testName) {
        super(testName);
        if(null == System.getProperty("org.xml.sax.driver")) {
           System.setProperty("org.xml.sax.driver","org.apache.xerces.parsers.SAXParser");
        }
    }

    public static Test suite() {
        return new TestSuite(TestJOCLed.class);
    }
    
    protected Connection getConnection() throws Exception {
        return DriverManager.getConnection("jdbc:apache:commons:dbcp:/testpool");
    }

    private PoolingDriver driver = null;
    
    public void setUp() throws Exception {
        super.setUp();
        driver = new PoolingDriver();
        PoolingDriver.setAccessToUnderlyingConnectionAllowed(true);
    }

    public void tearDown() throws Exception {
        driver.closePool("testpool");
        DriverManager.deregisterDriver(driver);
        super.tearDown();
    }

}
