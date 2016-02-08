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
 * Mock object implementing the <code>java.sql.Driver</code> interface.
 * Returns <code>TestConnection</code>'s from getConnection methods.  
 * Valid username, password combinations are:
 *
 * <table summary="valid credentials">
 * <tr><th>user</th><th>password</th></tr>
 * <tr><td>foo</td><td>bar</td></tr>
 * <tr><td>u1</td><td>p1</td></tr>
 * <tr><td>u2</td><td>p2</td></tr>
 * <tr><td>username</td><td>password</td></tr>
 * </table>
 * 
 * @author Rodney Waldhoff
 * @author Dirk Verbeeck
 * @version $Revision$ $Date: 2014-02-05 18:13:01 +0100 (Wed, 05 Feb 2014) $
 */
public class TesterDriver implements Driver {
    private static final Properties validUserPasswords = new Properties();
    static {
        try {
            DriverManager.registerDriver(new TesterDriver());
        } catch(final Exception e) {
        }
        validUserPasswords.put("foo", "bar");
        validUserPasswords.put("u1", "p1");
        validUserPasswords.put("u2", "p2");
        validUserPasswords.put("username", "password");
    }

    /** 
     * TesterDriver specific method to add users to the list of valid users 
     */
    public static void addUser(final String username, final String password) {
        synchronized (validUserPasswords) {
            validUserPasswords.put(username, password);
        }
    }

    @Override
    public boolean acceptsURL(final String url) throws SQLException {
        return url != null && url.startsWith(CONNECT_STRING);
    }

    private void assertValidUserPassword(final String user, final String password) 
        throws SQLException {
        if (user == null){
            throw new SQLException("username cannot be null.");            
        }
        synchronized (validUserPasswords) {
            final String realPassword = validUserPasswords.getProperty(user);
            if (realPassword == null) {
                throw new SQLException(user + " is not a valid username.");
            }
            if (!realPassword.equals(password)) {
                throw new SQLException(password + " is not the correct password for " + user
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
            String username = "test";
            String password = "test";
            if (info != null) 
            {
                username = info.getProperty("user");
                password = info.getProperty("password");
                if (username == null) {
                    final String[] parts = url.split(";");
                    username = parts[1];
                    username = username.split("=")[1];
                    password = parts[2];
                    password = password.split("=")[1];
                }
                assertValidUserPassword(username, password);
            }
            conn = new TesterConnection(username, password);
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
    public boolean jdbcCompliant() {
        return true;
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(final String url, final Properties info) {
        return new DriverPropertyInfo[0];
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
    }

    private static final String CONNECT_STRING = "jdbc:apache:commons:testdriver";

    // version numbers
    private static final int MAJOR_VERSION = 1;
    private static final int MINOR_VERSION = 0;

}
