/*
 * $Header: /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons//dbcp/src/test/org/apache/commons/dbcp/TesterDriver.java,v 1.3 2003/06/02 04:54:11 jmcnally Exp $
 * $Revision: 1.3 $
 * $Date: 2003/06/02 04:54:11 $
 *
 * ====================================================================
 *
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999-2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Commons", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package org.apache.commons.dbcp;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Mock object implementing the <code>java.sql.Driver</code> interface.
 * Returns <code>TestConnection</code>'s from getConnection methods.  
 * Valid username, password combinations are:
 *
 * <table>
 * <tr><th>user</th><th>password</th></tr>
 * <tr><td>foo</td><td>bar</td></tr>
 * <tr><td>u1</td><td>p1</td></tr>
 * <tr><td>u2</td><td>p2</td></tr>
 * <tr><td>username</td><td>password</td></tr>
 * </table>
 */
public class TesterDriver implements Driver {
    private static Properties validUserPasswords = new Properties();
    static {
        try {
            DriverManager.registerDriver(new TesterDriver());
        } catch(Exception e) {
        }
        validUserPasswords.put("foo", "bar");
        validUserPasswords.put("u1", "p1");
        validUserPasswords.put("u2", "p2");
        validUserPasswords.put("username", "password");
    }

    public boolean acceptsURL(String url) throws SQLException {
        return CONNECT_STRING.startsWith(url);
    }

    private void assertValidUserPassword(String user, String password) 
        throws SQLException {
        String realPassword = validUserPasswords.getProperty(user);
        if (realPassword == null) 
        {
            throw new SQLException(user + " is not a valid username.");
        }
        if (!realPassword.equals(password)) 
        {
            throw new SQLException(password + 
                                   " is not the correct password for " +
                                   user + ".  The correct password is " +
                                   realPassword);
        }
    }

    public Connection connect(String url, Properties info) throws SQLException {
        //return (acceptsURL(url) ? new TesterConnection() : null);
        Connection conn = null;
        if (acceptsURL(url)) 
        {
            if (info != null) 
            {
                assertValidUserPassword(info.getProperty("user"), 
                                        info.getProperty("password"));
            }
            
            conn = new TesterConnection();
        }
        
        return conn;
    }

    public int getMajorVersion() {
        return MAJOR_VERSION;
    }

    public int getMinorVersion() {
        return MINOR_VERSION;
    }

    public boolean jdbcCompliant() {
        return true;
    }

    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) {
        return new DriverPropertyInfo[0];
    }

    protected static String CONNECT_STRING = "jdbc:apache:commons:testdriver";

    // version numbers
    protected static int MAJOR_VERSION = 1;
    protected static int MINOR_VERSION = 0;

}
