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
import static org.junit.Assert.assertNull;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;

/**
 * Test for DataSourceConnectionFactory.
 */
public class TestDataSourceConnectionFactory {

    private DataSource datasource;
    private DataSourceConnectionFactory factory;

    @Before
    public void setUp() {
        datasource = new TestDataSource();
        factory = new DataSourceConnectionFactory(datasource);
    }

    @Test
    public void testDefaultValues() throws SQLException {
        Connection conn = factory.createConnection();
        assertNull(((TesterConnection) conn).getUserName());
    }

    @Test
    public void testCredentials() throws SQLException {
        DataSourceConnectionFactory factory = new DataSourceConnectionFactory(datasource, "foo", "bar");
        Connection conn = factory.createConnection();
        assertEquals("foo", ((TesterConnection) conn).getUserName());
    }

    @Test
    public void testEmptyPassword() throws SQLException {
        DataSourceConnectionFactory factory = new DataSourceConnectionFactory(datasource, "foo", (char[]) null);
        Connection conn = factory.createConnection();
        assertEquals("foo", ((TesterConnection) conn).getUserName());
    }

    @Test
    public void testEmptyUser() throws SQLException {
        DataSourceConnectionFactory factory = new DataSourceConnectionFactory(datasource, null, new char[] {'a'});
        Connection conn = factory.createConnection();
        assertNull(((TesterConnection) conn).getUserName());
    }

    private static class TestDataSource implements DataSource {

        @Override
        public PrintWriter getLogWriter() throws SQLException {
            return null;
        }

        @Override
        public void setLogWriter(PrintWriter out) throws SQLException {
        }

        @Override
        public void setLoginTimeout(int seconds) throws SQLException {
        }

        @Override
        public int getLoginTimeout() throws SQLException {
            return 0;
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            return null;
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            return null;
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) throws SQLException {
            return false;
        }

        @Override
        public Connection getConnection() throws SQLException {
            return new TesterConnection(null, null);
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            return new TesterConnection(username, password);
        }
    }
}
