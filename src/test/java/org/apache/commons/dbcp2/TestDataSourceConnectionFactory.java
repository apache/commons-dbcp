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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for DataSourceConnectionFactory.
 */
public class TestDataSourceConnectionFactory {

    private static class TestDataSource implements DataSource {

        @Override
        public Connection getConnection() throws SQLException {
            return new TesterConnection(null, null);
        }

        @Override
        public Connection getConnection(final String username, final String password) throws SQLException {
            return new TesterConnection(username, password);
        }

        @Override
        public int getLoginTimeout() throws SQLException {
            return 0;
        }

        @Override
        public PrintWriter getLogWriter() throws SQLException {
            return null;
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            return null;
        }

        @Override
        public boolean isWrapperFor(final Class<?> iface) throws SQLException {
            return false;
        }

        @Override
        public void setLoginTimeout(final int seconds) throws SQLException {
            // noop
        }

        @Override
        public void setLogWriter(final PrintWriter out) throws SQLException {
            // noop
        }

        @Override
        public <T> T unwrap(final Class<T> iface) throws SQLException {
            return null;
        }
    }
    private DataSource datasource;

    private DataSourceConnectionFactory factory;

    @BeforeEach
    public void setUp() {
        datasource = new TestDataSource();
        factory = new DataSourceConnectionFactory(datasource);
    }

    @Test
    public void testCredentials() throws SQLException {
        final DataSourceConnectionFactory factory = new DataSourceConnectionFactory(datasource, "foo", "bar");
        try (Connection conn = factory.createConnection()) {
            assertEquals("foo", ((TesterConnection) conn).getUserName());
        }
    }

    @Test
    public void testDefaultValues() throws SQLException {
        try (Connection conn = factory.createConnection()) {
            assertNull(((TesterConnection) conn).getUserName());
        }
    }

    @Test
    public void testEmptyPassword() throws SQLException {
        final DataSourceConnectionFactory factory = new DataSourceConnectionFactory(datasource, "foo", (char[]) null);
        try (Connection conn = factory.createConnection()) {
            assertEquals("foo", ((TesterConnection) conn).getUserName());
        }
    }

    @Test
    public void testEmptyUser() throws SQLException {
        final DataSourceConnectionFactory factory = new DataSourceConnectionFactory(datasource, null, new char[] { 'a' });
        try (Connection conn = factory.createConnection()) {
            assertNull(((TesterConnection) conn).getUserName());
        }
    }
}
