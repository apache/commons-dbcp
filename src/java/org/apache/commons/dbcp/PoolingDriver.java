/*
 * Copyright 1999-2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Properties;
import java.util.Set;

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
 * @version $Revision: 1.11 $ $Date: 2004/02/28 12:18:17 $
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
    public synchronized ObjectPool getPool(String name) {
        try {
            return getConnectionPool(name);
        }
        catch (Exception e) {
            throw new DbcpException(e);
        }
    }
    
    public synchronized ObjectPool getConnectionPool(String name) throws SQLException {
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

    public synchronized void registerPool(String name, ObjectPool pool) {
        _pools.put(name,pool);
    }

    public synchronized void closePool(String name) throws SQLException {
        ObjectPool pool = (ObjectPool) _pools.get(name);
        if (pool != null) {
            _pools.remove(name);
            try {
                pool.close();
            }
            catch (Exception e) {
                throw new SQLNestedException("Error closing pool " + name, e);
            }
        }
    }
    
    public synchronized String[] getPoolNames() throws SQLException{
        Set names = _pools.keySet();
        return (String[]) names.toArray(new String[names.size()]);
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
