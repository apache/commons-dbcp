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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.util.Properties;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * TestSuite for PoolingDataSource
 */
public class TestPoolingDataSource extends TestConnectionPool {

    @Override
    protected Connection getConnection() throws Exception {
        return ds.getConnection();
    }

    protected PoolingDataSource<PoolableConnection> ds = null;
    private GenericObjectPool<PoolableConnection> pool = null;

    @Before
    public void setUp() throws Exception {
        final Properties props = new Properties();
        props.setProperty("user", "userName");
        props.setProperty("password", "password");
        final PoolableConnectionFactory factory =
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
    @After
    public void tearDown() throws Exception {
        ds.close();
        super.tearDown();
    }

    @Test
    public void testPoolGuardConnectionWrapperEqualsSameDelegate() throws Exception {
        // Get a maximal set of connections from the pool
        final Connection[] c = new Connection[getMaxTotal()];
        for (int i = 0; i < c.length; i++) {
            c[i] = newConnection();
        }
        // Close the delegate of one wrapper in the pool
        ((DelegatingConnection<?>) c[0]).getDelegate().close();

        // Grab a new connection - should get c[0]'s closed connection
        // so should be delegate-equivalent
        final Connection con = newConnection();
        Assert.assertNotEquals(c[0], con);
        Assert.assertEquals(
                ((DelegatingConnection<?>) c[0]).getInnermostDelegateInternal(),
                ((DelegatingConnection<?>) con).getInnermostDelegateInternal());
        for (final Connection element : c) {
            element.close();
        }
    }

    /*
     * JIRA: DBCP-198
     */
    @Test
    public void testPoolGuardConnectionWrapperEqualsReflexive()
        throws Exception {
        final Connection con = ds.getConnection();
        final Connection con2 = con;
        assertTrue(con2.equals(con));
        assertTrue(con.equals(con2));
        con.close();
    }

    @Test
    public void testPoolGuardConnectionWrapperEqualsFail() throws Exception {
        final Connection con1 = ds.getConnection();
        final Connection con2 = ds.getConnection();
        assertFalse(con1.equals(con2));
        con1.close();
        con2.close();
    }

    @Test
    public void testPoolGuardConnectionWrapperEqualsNull() throws Exception {
        final Connection con1 = ds.getConnection();
        final Connection con2 = null;
        assertFalse(con1.equals(con2));
        con1.close();
    }

    @Test
    public void testPoolGuardConnectionWrapperEqualsType() throws Exception {
        final Connection con1 = ds.getConnection();
        final Integer con2 = Integer.valueOf(0);
        assertFalse(con1.equals(con2));
        con1.close();
    }

    @Test
    public void testPoolGuardConnectionWrapperEqualInnermost() throws Exception {
        ds.setAccessToUnderlyingConnectionAllowed(true);
        final DelegatingConnection<?> con = (DelegatingConnection<?>) ds.getConnection();
        final Connection inner = con.getInnermostDelegate();
        ds.setAccessToUnderlyingConnectionAllowed(false);
        final DelegatingConnection<Connection> con2 = new DelegatingConnection<>(inner);
        assertFalse(con2.equals(con));
        assertTrue(con.innermostDelegateEquals(con2.getInnermostDelegate()));
        assertTrue(con2.innermostDelegateEquals(inner));
        assertFalse(con.equals(con2));
    }

    /**
     * DBCP-412
     * Verify that omitting factory.setPool(pool) when setting up PDS does not
     * result in NPE.
     */
    @Test
    public void testFixFactoryConfig() throws Exception {
        final Properties props = new Properties();
        props.setProperty("user", "userName");
        props.setProperty("password", "password");
        final PoolableConnectionFactory f =
            new PoolableConnectionFactory(
                    new DriverConnectionFactory(new TesterDriver(),
                            "jdbc:apache:commons:testdriver", props),
                    null);
        f.setValidationQuery("SELECT DUMMY FROM DUAL");
        f.setDefaultReadOnly(Boolean.TRUE);
        f.setDefaultAutoCommit(Boolean.TRUE);
        final GenericObjectPool<PoolableConnection> p = new GenericObjectPool<>(f);
        p.setMaxTotal(getMaxTotal());
        p.setMaxWaitMillis(getMaxWaitMillis());
        ds = new PoolingDataSource<>(p);
        assertTrue(f.getPool().equals(p));
        ds.getConnection();
    }

    @Test
    public void testClose() throws Exception {

        final Properties props = new Properties();
        props.setProperty("user", "userName");
        props.setProperty("password", "password");
        final PoolableConnectionFactory f =
            new PoolableConnectionFactory(
                    new DriverConnectionFactory(new TesterDriver(),
                            "jdbc:apache:commons:testdriver", props),
                    null);
        f.setValidationQuery("SELECT DUMMY FROM DUAL");
        f.setDefaultReadOnly(Boolean.TRUE);
        f.setDefaultAutoCommit(Boolean.TRUE);
        final GenericObjectPool<PoolableConnection> p = new GenericObjectPool<>(f);
        p.setMaxTotal(getMaxTotal());
        p.setMaxWaitMillis(getMaxWaitMillis());

        try ( PoolingDataSource<PoolableConnection> dataSource = new PoolingDataSource<>(p) ) {
            final Connection connection = dataSource.getConnection();
            assertNotNull(connection);
            connection.close();
        }

        assertTrue(p.isClosed());
        assertEquals(0, p.getNumIdle());
        assertEquals(0, p.getNumActive());
    }
}
