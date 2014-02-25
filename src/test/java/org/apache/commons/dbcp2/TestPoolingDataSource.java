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
import junit.framework.TestSuite;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.junit.Assert;

/**
 * TestSuite for PoolingDataSource
 *
 * @version $Revision: 392677 $ $Date: 2006-04-08 21:42:24 -0700 (Sat, 08 Apr 2006) $
 */
public class TestPoolingDataSource extends TestConnectionPool {
    public TestPoolingDataSource(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(TestPoolingDataSource.class);
    }

    @Override
    protected Connection getConnection() throws Exception {
        return ds.getConnection();
    }

    protected PoolingDataSource<PoolableConnection> ds = null;
    private GenericObjectPool<PoolableConnection> pool = null;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        Properties props = new Properties();
        props.setProperty("user", "username");
        props.setProperty("password", "password");
        PoolableConnectionFactory factory =
            new PoolableConnectionFactory(
                    new DriverConnectionFactory(new TesterDriver(),
                            "jdbc:apache:commons:testdriver", props),
                    null);
        factory.setValidationQuery("SELECT DUMMY FROM DUAL");
        factory.setDefaultReadOnly(Boolean.TRUE);
        factory.setDefaultAutoCommit(Boolean.TRUE);
        pool = new GenericObjectPool<>(factory);
        factory.setPool(pool);
        pool.setMaxTotal(getMaxTotal());
        pool.setMaxWaitMillis(getMaxWaitMillis());
        ds = new PoolingDataSource<>(pool);
        ds.setAccessToUnderlyingConnectionAllowed(true);
    }

    @Override
    public void tearDown() throws Exception {
        pool.close();
        super.tearDown();
    }

    public void testPoolGuardConnectionWrapperEqualsSameDelegate() throws Exception {
        // Get a maximal set of connections from the pool
        Connection[] c = new Connection[getMaxTotal()];
        for (int i = 0; i < c.length; i++) {
            c[i] = newConnection();
        }
        // Close the delegate of one wrapper in the pool
        ((DelegatingConnection<?>) c[0]).getDelegate().close();

        // Grab a new connection - should get c[0]'s closed connection
        // so should be delegate-equivalent
        Connection con = newConnection();
        Assert.assertNotEquals(c[0], con);
        Assert.assertEquals(
                ((DelegatingConnection<?>) c[0]).getInnermostDelegateInternal(),
                ((DelegatingConnection<?>) con).getInnermostDelegateInternal());
        for (Connection element : c) {
            element.close();
        }
    }

    /*
     * JIRA: DBCP-198
     */
    public void testPoolGuardConnectionWrapperEqualsReflexive()
        throws Exception {
        Connection con = ds.getConnection();
        Connection con2 = con;
        assertTrue(con2.equals(con));
        assertTrue(con.equals(con2));
        con.close();
    }

    public void testPoolGuardConnectionWrapperEqualsFail() throws Exception {
        Connection con1 = ds.getConnection();
        Connection con2 = ds.getConnection();
        assertFalse(con1.equals(con2));
        con1.close();
        con2.close();
    }

    public void testPoolGuardConnectionWrapperEqualsNull() throws Exception {
        Connection con1 = ds.getConnection();
        Connection con2 = null;
        assertFalse(con1.equals(con2));
        con1.close();
    }

    public void testPoolGuardConnectionWrapperEqualsType() throws Exception {
        Connection con1 = ds.getConnection();
        Integer con2 = Integer.valueOf(0);
        assertFalse(con1.equals(con2));
        con1.close();
    }

    public void testestPoolGuardConnectionWrapperEqualInnermost() throws Exception {
        ds.setAccessToUnderlyingConnectionAllowed(true);
        DelegatingConnection<?> con = (DelegatingConnection<?>) ds.getConnection();
        Connection inner = con.getInnermostDelegate();
        ds.setAccessToUnderlyingConnectionAllowed(false);
        DelegatingConnection<Connection> con2 = new DelegatingConnection<>(inner);
        assertFalse(con2.equals(con));
        assertTrue(con.innermostDelegateEquals(con2.getInnermostDelegate()));
        assertTrue(con2.innermostDelegateEquals(inner));
        assertFalse(con.equals(con2));
    }
}
