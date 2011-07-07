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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolFactory;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.commons.pool2.impl.WhenExhaustedAction;

/**
 * Tests for a  {@link GenericObjectPool} based {@link PoolingDriver}.
 * @author Rodney Waldhoff
 * @author Sean C. Sullivan
 * @version $Revision$ $Date$
 */
public class TestPoolingDriver extends TestConnectionPool {
    public TestPoolingDriver(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(TestPoolingDriver.class);
    }

    @Override
    protected Connection getConnection() throws Exception {
        return DriverManager.getConnection("jdbc:apache:commons:dbcp:test");
    }

    private PoolingDriver driver = null;
    
    @Override
    public void setUp() throws Exception {
        super.setUp();
        DriverConnectionFactory cf = new DriverConnectionFactory(new TesterDriver(),"jdbc:apache:commons:testdriver",null);

        GenericKeyedObjectPoolConfig keyedPoolConfig =
            new GenericKeyedObjectPoolConfig();
        keyedPoolConfig.setMaxTotalPerKey(10);
        keyedPoolConfig.setMaxWait(2000);
        keyedPoolConfig.setMaxIdlePerKey(10);
        keyedPoolConfig.setTestOnBorrow(true);
        keyedPoolConfig.setTestOnReturn(true);
        keyedPoolConfig.setTestWhileIdle(true);
        keyedPoolConfig.setTimeBetweenEvictionRunsMillis(10000);
        keyedPoolConfig.setNumTestsPerEvictionRun(5);
        keyedPoolConfig.setMinEvictableIdleTimeMillis(5000);
        GenericKeyedObjectPoolFactory opf =
            new GenericKeyedObjectPoolFactory(keyedPoolConfig);

        PoolableConnectionFactory pcf = new PoolableConnectionFactory(cf);
        pcf.setStatementPoolFactory(opf);
        pcf.setValidationQuery("SELECT COUNT(*) FROM DUAL");
        pcf.setDefaultReadOnly(false);
        pcf.setDefaultAutoCommit(true);

        GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
        poolConfig.setMaxTotal(getMaxTotal());
        poolConfig.setMaxWait(getMaxWait());
        poolConfig.setMinIdle(10);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setTimeBetweenEvictionRunsMillis(10000L);
        poolConfig.setNumTestsPerEvictionRun(5);
        poolConfig.setMinEvictableIdleTimeMillis(5000L);
        
        GenericObjectPool pool = new GenericObjectPool(pcf, poolConfig);
        pcf.setPool(pool);

        assertNotNull(pcf);
        driver = new PoolingDriver();
        driver.registerPool("test",pool);
        PoolingDriver.setAccessToUnderlyingConnectionAllowed(true);
    }

    @Override
    public void tearDown() throws Exception {
        driver.closePool("test");
        super.tearDown();
    }
    
    public void test1() {
        ConnectionFactory connectionFactory = new DriverManagerConnectionFactory("jdbc:some:connect:string","username","password");
        PoolableConnectionFactory pcf =
            new PoolableConnectionFactory(connectionFactory);
        pcf.setDefaultReadOnly(false);
        pcf.setDefaultAutoCommit(true);
        GenericObjectPool connectionPool = new GenericObjectPool(pcf);
        new PoolingDataSource(connectionPool);
    }

    public void test2() {
        ConnectionFactory connectionFactory = new DriverManagerConnectionFactory("jdbc:some:connect:string","username","password");
        PoolableConnectionFactory pcf =
            new PoolableConnectionFactory(connectionFactory);
        pcf.setDefaultReadOnly(false);
        pcf.setDefaultAutoCommit(true);
        GenericObjectPool connectionPool = new GenericObjectPool(pcf);
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
        GenericObjectPoolConfig config = new GenericObjectPoolConfig();
        config.setMaxTotal(70);
        config.setMaxWait(60000);
        config.setMaxIdle(10);
        ConnectionFactory connectionFactory = new DriverManagerConnectionFactory(
            "jdbc:apache:commons:testdriver", 
            "username", 
            "password");
        PoolableConnectionFactory poolableConnectionFactory = 
            new PoolableConnectionFactory(connectionFactory);
        poolableConnectionFactory.setDefaultReadOnly(false);
        poolableConnectionFactory.setDefaultAutoCommit(true);
        ObjectPool connectionPool =
            new GenericObjectPool(poolableConnectionFactory,config);
        poolableConnectionFactory.setPool(connectionPool);
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
        ex = new SQLException("A", new Exception("a"));
        ex.printStackTrace();
        ex.printStackTrace(ps);
        ex.printStackTrace(pw);
        ex = new SQLException("B");
        ex.printStackTrace();
        ex.printStackTrace(ps);
        ex.printStackTrace(pw);
        ex = new SQLException(null, new Exception("c"));
        ex.printStackTrace();
        ex.printStackTrace(ps);
        ex.printStackTrace(pw);
        ex = new SQLException((String)null);
        ex.printStackTrace();
        ex.printStackTrace(ps);
        ex.printStackTrace(pw);

        DriverManager.setLogWriter(null);
        ex = new SQLException("A", new Exception("a"));
        ex.printStackTrace();
        ex.printStackTrace(ps);
        ex.printStackTrace(pw);
        ex = new SQLException("B");
        ex.printStackTrace();
        ex.printStackTrace(ps);
        ex.printStackTrace(pw);
        ex = new SQLException(null, new Exception("c"));
        ex.printStackTrace();
        ex.printStackTrace(ps);
        ex.printStackTrace(pw);
        ex = new SQLException((String)null);
        ex.printStackTrace();
        ex.printStackTrace(ps);
        ex.printStackTrace(pw);
    }
}
