/*
 * $Source: /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons//dbcp/src/java/org/apache/commons/dbcp/PoolingDriver.java,v $
 * $Revision: 1.6 $
 * $Date: 2003/08/22 16:08:31 $
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
 *    any, must include the following acknowledgement:
 *       "This product includes software developed by the
 *        Apache Software Foundation - http://www.apache.org/"
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
 * http://www.apache.org/
 *
 */

package org.apache.commons.dbcp;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Properties;

import org.apache.commons.jocl.JOCLContentHandler;
import org.apache.commons.pool.ObjectPool;
import org.xml.sax.SAXException;


/**
 * A {@link Driver} implementation that obtains
 * {@link Connection}s from a registered
 * {@link ObjectPool}.
 *
 * @author Rodney Waldhoff
 * @author Dirk Verbeeck
 * @version $Id: PoolingDriver.java,v 1.6 2003/08/22 16:08:31 dirkv Exp $
 */
public class PoolingDriver implements Driver {
    /** Register an myself with the {@link DriverManager}. */
    static {
        try {
            DriverManager.registerDriver(new PoolingDriver());
        } catch(Exception e) {
        }
    }

    /** The map of registered pools. */
    protected static HashMap _pools = new HashMap();

    public PoolingDriver() {
    }

    /**
     * WARNING: This method throws DbcpExceptions (RuntimeExceptions)
     * and will be replaced by the protected getConnectionPool method.
     * 
     * @deprecated This will be removed in a future version of DBCP.
     */
    synchronized public ObjectPool getPool(String name) {
        try {
            return getConnectionPool(name);
        }
        catch (Exception e) {
            throw new DbcpException(e);
        }
    }
    
    synchronized protected ObjectPool getConnectionPool(String name) throws SQLException {
        ObjectPool pool = (ObjectPool)(_pools.get(name));
        if(null == pool) {
            InputStream in = this.getClass().getResourceAsStream(String.valueOf(name) + ".jocl");
            if(null != in) {
                JOCLContentHandler jocl = null;
                try {
                    jocl = JOCLContentHandler.parse(in);
                }
                catch (SAXException e) {
                    throw new SQLNestedException("Could not parse configuration file", e);
                }
                catch (IOException e) {
                    throw new SQLNestedException("Could not load configuration file", e);
                }
                if(jocl.getType(0).equals(String.class)) {
                    pool = getPool((String)(jocl.getValue(0)));
                    if(null != pool) {
                        registerPool(name,pool);
                    }
                } else {
                    pool = ((PoolableConnectionFactory)(jocl.getValue(0))).getPool();
                    if(null != pool) {
                        registerPool(name,pool);
                    }
                }
            }
            else {
                throw new SQLException("Configuration file not found");
            }
        }
        return pool;
    }

    synchronized public void registerPool(String name, ObjectPool pool) {
        _pools.put(name,pool);
    }

    public boolean acceptsURL(String url) throws SQLException {
        try {
            return url.startsWith(URL_PREFIX);
        } catch(NullPointerException e) {
            return false;
        }
    }

    public Connection connect(String url, Properties info) throws SQLException {
        if(acceptsURL(url)) {
            ObjectPool pool = getConnectionPool(url.substring(URL_PREFIX_LEN));
            if(null == pool) {
                throw new SQLException("No pool found for " + url + ".");
            } else {
                try {
                    return (Connection)(pool.borrowObject());
                } catch(SQLException e) {
                    throw e;
                } catch(Throwable e) {
                    throw new SQLNestedException("Connect failed", e);
                }
            }
        } else {
            return null;
        }
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

    /** My URL prefix */
    protected static String URL_PREFIX = "jdbc:apache:commons:dbcp:";
    protected static int URL_PREFIX_LEN = URL_PREFIX.length();

    // version numbers
    protected static int MAJOR_VERSION = 1;
    protected static int MINOR_VERSION = 0;

}
