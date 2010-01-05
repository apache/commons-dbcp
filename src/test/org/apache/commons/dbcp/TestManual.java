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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.apache.commons.pool.impl.GenericKeyedObjectPoolFactory;
import org.apache.commons.pool.impl.GenericObjectPool;

/**
 * Tests for a "manually configured", {@link GenericObjectPool}
 * based {@link PoolingDriver}.
 * @author Rodney Waldhoff
 * @author Sean C. Sullivan
 * @version $Revision$ $Date$
 */
public class TestManual extends TestConnectionPool {
    public TestManual(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(TestManual.class);
    }

    protected Connection getConnection() throws Exception {
        return DriverManager.getConnection("jdbc:apache:commons:dbcp:test");
    }

    private PoolingDriver driver = null;
    
    public void setUp() throws Exception {
        super.setUp();
        GenericObjectPool pool = new GenericObjectPool(null, getMaxActive(), GenericObjectPool.WHEN_EXHAUSTED_BLOCK, getMaxWait(), 10, true, true, 10000L, 5, 5000L, true);
        DriverConnectionFactory cf = new DriverConnectionFactory(new TesterDriver(),"jdbc:apache:commons:testdriver",null);
        GenericKeyedObjectPoolFactory opf = new GenericKeyedObjectPoolFactory(null, 10, GenericKeyedObjectPool.WHEN_EXHAUSTED_BLOCK, 2000L, 10, true, true, 10000L, 5, 5000L, true);
        PoolableConnectionFactory pcf = new PoolableConnectionFactory(cf, pool, opf, "SELECT COUNT(*) FROM DUAL", false, true);
        assertNotNull(pcf);
        driver = new PoolingDriver();
        driver.registerPool("test",pool);
        PoolingDriver.setAccessToUnderlyingConnectionAllowed(true);
        DriverManager.registerDriver(driver);
    }

    public void tearDown() throws Exception {
        driver.closePool("test");
        DriverManager.deregisterDriver(driver);
        super.tearDown();
    }
    
    public void test1() {
        GenericObjectPool connectionPool = new GenericObjectPool(null);
        ConnectionFactory connectionFactory = new DriverManagerConnectionFactory("jdbc:some:connect:string","username","password");
        new PoolableConnectionFactory(connectionFactory,connectionPool,null,null,false,true);
        new PoolingDataSource(connectionPool);
    }

    public void test2() {
        GenericObjectPool connectionPool = new GenericObjectPool(null);
        ConnectionFactory connectionFactory = new DriverManagerConnectionFactory("jdbc:some:connect:string","username","password");
        new PoolableConnectionFactory(connectionFactory,connectionPool,null,null,false,true);
        PoolingDriver driver2 = new PoolingDriver();
        driver2.registerPool("example",connectionPool);
    }


    /** @see "http://issues.apache.org/bugzilla/show_bug.cgi?id=28912" */
    public void testReportedBug28912() throws Exception {
        Connection conn1 = getConnection();
        assertNotNull(conn1);
        assertFalse(conn1.isClosed());
        conn1.close();        

        Connection conn2 = getConnection();
        assertNotNull(conn2);

        assertTrue(conn1.isClosed());
        assertFalse(conn2.isClosed());

        // should be able to call close multiple times with no effect
        conn1.close();

        assertTrue(conn1.isClosed());
        assertFalse(conn2.isClosed());
    }
    
    /** @see "http://issues.apache.org/bugzilla/show_bug.cgi?id=12400" */
    public void testReportedBug12400() throws Exception {
        ObjectPool connectionPool = new GenericObjectPool(
            null,
            70,
            GenericObjectPool.WHEN_EXHAUSTED_BLOCK,
            60000,
            10);
        ConnectionFactory connectionFactory = new DriverManagerConnectionFactory(
            "jdbc:apache:commons:testdriver", 
            "username", 
            "password");
        PoolableConnectionFactory poolableConnectionFactory = 
            new PoolableConnectionFactory(
                connectionFactory,
                connectionPool,
                null,
                null,
                false,
                true);
        assertNotNull(poolableConnectionFactory);
        PoolingDriver driver2 = new PoolingDriver();
        driver2.registerPool("neusoftim",connectionPool);
        Connection[] conn = new Connection[25];
        for(int i=0;i<25;i++) {
            conn[i] = DriverManager.getConnection("jdbc:apache:commons:dbcp:neusoftim");
            for(int j=0;j<i;j++) {
                assertTrue(conn[j] != conn[i]);
                assertTrue(!conn[j].equals(conn[i]));
            }
        }
        for(int i=0;i<25;i++) {
            conn[i].close();
        }
    }
    
    public void testClosePool() throws Exception {
        Connection conn = DriverManager.getConnection("jdbc:apache:commons:dbcp:test");
        assertNotNull(conn);
        conn.close();
        
        PoolingDriver driver2 = (PoolingDriver) DriverManager.getDriver("jdbc:apache:commons:dbcp:");
        driver2.closePool("test");

        try {
            DriverManager.getConnection("jdbc:apache:commons:dbcp:test");
            fail("expected SQLException");
        }
        catch (SQLException e) {
            // OK
        }
    }
    
    public void testInvalidateConnection() throws Exception {
        Connection conn = DriverManager.getConnection("jdbc:apache:commons:dbcp:test");
        assertNotNull(conn);

        ObjectPool pool = driver.getConnectionPool("test");
        assertEquals(1, pool.getNumActive());
        assertEquals(0, pool.getNumIdle());

        PoolingDriver driver2 = (PoolingDriver) DriverManager.getDriver("jdbc:apache:commons:dbcp:");
        driver2.invalidateConnection(conn);

        assertEquals(0, pool.getNumActive());
        assertEquals(0, pool.getNumIdle());
        assertTrue(conn.isClosed());
    }

    public void testLogWriter() throws Exception {
        PrintStream ps = new PrintStream(new ByteArrayOutputStream());
        PrintWriter pw = new PrintWriter(new ByteArrayOutputStream());
        System.setErr(new PrintStream(new ByteArrayOutputStream()));
        SQLException ex;
        
        DriverManager.setLogWriter(pw);
        ex = new SQLNestedException("A", new Exception("a"));
        ex.printStackTrace();
        ex.printStackTrace(ps);
        ex.printStackTrace(pw);
        ex = new SQLNestedException("B", null);
        ex.printStackTrace();
        ex.printStackTrace(ps);
        ex.printStackTrace(pw);
        ex = new SQLNestedException(null, new Exception("c"));
        ex.printStackTrace();
        ex.printStackTrace(ps);
        ex.printStackTrace(pw);
        ex = new SQLNestedException(null, null);
        ex.printStackTrace();
        ex.printStackTrace(ps);
        ex.printStackTrace(pw);

        DriverManager.setLogWriter(null);
        ex = new SQLNestedException("A", new Exception("a"));
        ex.printStackTrace();
        ex.printStackTrace(ps);
        ex.printStackTrace(pw);
        ex = new SQLNestedException("B", null);
        ex.printStackTrace();
        ex.printStackTrace(ps);
        ex.printStackTrace(pw);
        ex = new SQLNestedException(null, new Exception("c"));
        ex.printStackTrace();
        ex.printStackTrace(ps);
        ex.printStackTrace(pw);
        ex = new SQLNestedException(null, null);
        ex.printStackTrace();
        ex.printStackTrace(ps);
        ex.printStackTrace(pw);
    }
}
