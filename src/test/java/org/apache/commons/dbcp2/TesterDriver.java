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
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Mock object implementing the {@link Driver} interface.
 * Returns {@code TestConnection}'s from getConnection methods.
 * Valid user name, password combinations are:
 *
 * <table summary="valid credentials">
 * <tr><th>user</th><th>password</th></tr>
 * <tr><td>foo</td><td>bar</td></tr>
 * <tr><td>u1</td><td>p1</td></tr>
 * <tr><td>u2</td><td>p2</td></tr>
 * <tr><td>username</td><td>password</td></tr>
 * </table>
 */
public class TesterDriver implements Driver {
    private static final Properties validUserPasswords = new Properties();
    static {
        try {
            DriverManager.registerDriver(new TesterDriver());
        } catch (final Exception e) {
            // ignore
        }
        validUserPasswords.put("foo", "bar");
        validUserPasswords.put("u1", "p1");
        validUserPasswords.put("u2", "p2");
        validUserPasswords.put("userName", "password");
    }

    private static final String CONNECT_STRING = "jdbc:apache:commons:testdriver";

    // version numbers
    private static final int MAJOR_VERSION = 1;

    private static final int MINOR_VERSION = 0;

    /**
     * TesterDriver specific method to add users to the list of valid users
     */
    public static void addUser(final String userName, final String password) {
        synchronized (validUserPasswords) {
            validUserPasswords.put(userName, password);
        }
    }

    @Override
    public boolean acceptsURL(final String url) throws SQLException {
        return url != null && url.startsWith(CONNECT_STRING);
    }

    private void assertValidUserPassword(final String userName, final String password)
        throws SQLException {
        if (userName == null){
            throw new SQLException("user name cannot be null.");
        }
        synchronized (validUserPasswords) {
            final String realPassword = validUserPasswords.getProperty(userName);
            if (realPassword == null) {
                throw new SQLException(userName + " is not a valid user name.");
            }
            if (!realPassword.equals(password)) {
                throw new SQLException(password + " is not the correct password for " + userName
                        + ".  The correct password is " + realPassword);
            }
        }
    }

    @Override
    public Connection connect(final String url, final Properties info) throws SQLException {
        //return (acceptsURL(url) ? new TesterConnection() : null);
        Connection conn = null;
        if (acceptsURL(url))
        {
            String userName = "test";
            String password = "test";
            if (info != null)
            {
                userName = info.getProperty(Constants.KEY_USER);
                password = info.getProperty(Constants.KEY_PASSWORD);
                if (userName == null) {
                    final String[] parts = url.split(";");
                    userName = parts[1];
                    userName = userName.split("=")[1];
                    password = parts[2];
                    password = password.split("=")[1];
                }
                assertValidUserPassword(userName, password);
            }
            conn = new TesterConnection(userName, password);
        }

        return conn;
    }

    @Override
    public int getMajorVersion() {
        return MAJOR_VERSION;
    }

    @Override
    public int getMinorVersion() {
        return MINOR_VERSION;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(final String url, final Properties info) {
        return new DriverPropertyInfo[0];
    }
    @Override
    public boolean jdbcCompliant() {
        return true;
    }

}
