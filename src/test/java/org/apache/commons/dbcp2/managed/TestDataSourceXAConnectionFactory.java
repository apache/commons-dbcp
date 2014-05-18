/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.commons.dbcp2.managed;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.XAConnection;
import javax.sql.XADataSource;

import org.apache.commons.dbcp2.TestBasicDataSource;
import org.apache.geronimo.transaction.manager.TransactionManagerImpl;

/**
 * TestSuite for BasicManagedDataSource when using a
 * DataSourceXAConnectionFactory (configured from a XADataSource)
 */
public class TestDataSourceXAConnectionFactory extends TestBasicDataSource {

    public TestDataSourceXAConnectionFactory(String testName) {
        super(testName);
    }

    protected BasicManagedDataSource bmds;

    public AtomicInteger closeCounter = new AtomicInteger();

    @Override
    public void setUp() throws Exception {
        super.setUp();
        bmds = new BasicManagedDataSource();
        bmds.setTransactionManager(new TransactionManagerImpl());
        bmds.setXADataSource("notnull");
        XADataSourceHandle handle = new XADataSourceHandle();
        XADataSource xads = (XADataSource) Proxy.newProxyInstance(
                XADataSourceHandle.class.getClassLoader(),
                new Class[] { XADataSource.class }, handle);
        bmds.setXaDataSourceInstance(xads);
    }

    /**
     * Delegates everything to the BasicDataSource (ds field), except for
     * getXAConnection which creates a BasicXAConnection.
     */
    public class XADataSourceHandle implements InvocationHandler {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args)
                throws Throwable {
            String methodName = method.getName();
            if (methodName.equals("hashCode")) {
                return Integer.valueOf(System.identityHashCode(proxy));
            }
            if (methodName.equals("equals")) {
                return Boolean.valueOf(proxy == args[0]);
            }
            if (methodName.equals("getXAConnection")) {
                // both zero and 2-arg signatures
                return getXAConnection();
            }
            try {
                return method.invoke(ds, args);
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            }
        }

        protected XAConnection getXAConnection() throws SQLException {
            return new TesterBasicXAConnection(ds.getConnection(), closeCounter);
        }
    }

    /**
     * JIRA: DBCP-355
     */
    public void testPhysicalClose() throws Exception {
        bmds.setMaxIdle(1);
        Connection conn1 = bmds.getConnection();
        Connection conn2 = bmds.getConnection();
        closeCounter.set(0);
        conn1.close();
        assertEquals(0, closeCounter.get()); // stays idle in the pool
        conn2.close();
        assertEquals(1, closeCounter.get()); // can't have 2 idle ones
        bmds.close();
        assertEquals(2, closeCounter.get());
    }

}

