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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.commons.dbcp.cpdsadapter.TestDriverAdapterCPDS;
import org.apache.commons.dbcp.datasources.TestCPDSConnectionFactory;
import org.apache.commons.dbcp.datasources.TestFactory;
import org.apache.commons.dbcp.datasources.TestInstanceKeyDataSource;
import org.apache.commons.dbcp.datasources.TestKeyedCPDSConnectionFactory;
import org.apache.commons.dbcp.datasources.TestPerUserPoolDataSource;
import org.apache.commons.dbcp.datasources.TestSharedPoolDataSource;
import org.apache.commons.dbcp.managed.TestBasicManagedDataSource;
import org.apache.commons.dbcp.managed.TestManagedDataSource;
import org.apache.commons.dbcp.managed.TestManagedDataSourceInTx;
import org.apache.commons.jocl.TestJOCLContentHandler;

/**
 * @author Rodney Waldhoff
 * @version $Revision$ $Date$
 */
public class TestAll extends TestCase {
    public TestAll(String testName) {
        super(testName);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite();
        // The test *must* execute first since it tests the initialisation
        // of DriverManager
        suite.addTest(TestDriverManagerConnectionFactory.suite());
        // o.a.c.dbcp
        suite.addTest(TestAbandonedBasicDataSource.suite());
        suite.addTest(TestAbandonedObjectPool.suite());
        suite.addTest(TestBasicDataSource.suite());
        suite.addTest(TestBasicDataSourceFactory.suite());
        // TestConnectionPool is abstract
        suite.addTest(TestDelegatingConnection.suite());
        suite.addTest(TestDelegatingDatabaseMetaData.suite());
        suite.addTest(TestDelegatingPreparedStatement.suite());
        suite.addTest(TestDelegatingStatement.suite());
        suite.addTest(TestJndi.suite());
        suite.addTest(TestJOCLed.suite());
        suite.addTest(TestManual.suite());
        suite.addTest(TestPoolableConnection.suite());
        suite.addTest(TestPoolingDataSource.suite());
        suite.addTest(TestPStmtPooling.suite());
        suite.addTest(TestPStmtPoolingBasicDataSource.suite());
        // o.a.c.dbcp.cpdsadapter
        suite.addTest(TestDriverAdapterCPDS.suite());
        // o.a.c.dbcp.datasources
        suite.addTest(TestFactory.suite());
        suite.addTest(TestInstanceKeyDataSource.suite());
        suite.addTest(TestCPDSConnectionFactory.suite());
        suite.addTest(TestKeyedCPDSConnectionFactory.suite());
        suite.addTest(TestPerUserPoolDataSource.suite());
        suite.addTest(TestSharedPoolDataSource.suite());
        // o.a.c.dbcp.managed
        suite.addTest(TestBasicManagedDataSource.suite());
        suite.addTest(TestManagedDataSource.suite());
        suite.addTest(TestManagedDataSourceInTx.suite());
        // o.a.c.jocl
        suite.addTest(TestJOCLContentHandler.suite());
        return suite;
    }

    public static void main(String args[]) {
        String[] testCaseName = { TestAll.class.getName() };
        junit.textui.TestRunner.main(testCaseName);
    }
}
