/*
 * $Header: /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons//dbcp/src/java/org/apache/commons/dbcp/datasources/KeyedCPDSConnectionFactory.java,v 1.1 2003/08/12 06:09:20 jmcnally Exp $
 * $Revision: 1.1 $
 * $Date: 2003/08/12 06:09:20 $
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

package org.apache.commons.dbcp.datasources;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import javax.sql.ConnectionEvent;
import javax.sql.ConnectionEventListener;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.PooledConnection;

import org.apache.commons.dbcp.SQLNestedException;
import org.apache.commons.pool.KeyedObjectPool;
import org.apache.commons.pool.KeyedPoolableObjectFactory;

/**
 * A {*link PoolableObjectFactory} that creates
 * {*link PoolableConnection}s.
 *
 * @author <a href="mailto:jmcnally@collab.net">John D. McNally</a>
 * @version $Id: KeyedCPDSConnectionFactory.java,v 1.1 2003/08/12 06:09:20 jmcnally Exp $
 */
class KeyedCPDSConnectionFactory 
    implements KeyedPoolableObjectFactory, ConnectionEventListener {

    private static final String NO_KEY_MESSAGE 
            = "close() was called on a Connection, but " 
            + "I have no record of the underlying PooledConnection.";

    protected ConnectionPoolDataSource _cpds = null;
    protected String _validationQuery = null;
    protected KeyedObjectPool _pool = null;
    private Map validatingMap = new HashMap();
    private WeakHashMap pcMap = new WeakHashMap();

    /**
     * Create a new <tt>KeyedPoolableConnectionFactory</tt>.
     * @param cpds the ConnectionPoolDataSource from which to obtain PooledConnection's
     * @param pool the {*link ObjectPool} in which to pool those {*link Connection}s
     * @param validationQuery a query to use to {*link #validateObject validate} {*link Connection}s.  Should return at least one row. May be <tt>null</tt>
     */
    public KeyedCPDSConnectionFactory(ConnectionPoolDataSource cpds, 
                                      KeyedObjectPool pool, 
                                      String validationQuery) {
        _cpds = cpds;
        _pool = pool;
        _pool.setFactory(this);
        _validationQuery = validationQuery;
    }

    /**
     * Sets the {*link ConnectionFactory} from which to obtain base {*link Connection}s.
     * @param connFactory the {*link ConnectionFactory} from which to obtain base {*link Connection}s
     */
    synchronized public void setCPDS(ConnectionPoolDataSource cpds) {
        _cpds = cpds;
    }

    /**
     * Sets the query I use to {*link #validateObject validate} {*link Connection}s.
     * Should return at least one row.
     * May be <tt>null</tt>
     * @param validationQuery a query to use to {*link #validateObject validate} {*link Connection}s.
     */
    synchronized public void setValidationQuery(String validationQuery) {
        _validationQuery = validationQuery;
    }

    /**
     * Sets the {*link ObjectPool} in which to pool {*link Connection}s.
     * @param pool the {*link ObjectPool} in which to pool those {*link Connection}s
     */
    synchronized public void setPool(KeyedObjectPool pool) 
        throws SQLException {
        if (null != _pool && pool != _pool) {
            try {
                _pool.close();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new SQLNestedException("Cannot set the pool on this factory", e);
            }
        }
        _pool = pool;
    }

    public KeyedObjectPool getPool() {
        return _pool;
    }

    /**
     * @param key
     * @throws SQLException if the connection could not be created.
     * @see org.apache.commons.pool.KeyedPoolableObjectFactory#makeObject(java.lang.Object)
     */
    public synchronized Object makeObject(Object key) throws Exception {
        Object obj = null;
        UserPassKey upkey = (UserPassKey)key;

        PooledConnection pc = null;
        String username = upkey.getUsername();
        String password = upkey.getPassword();
        if (username == null) {
            pc = _cpds.getPooledConnection();
        } else {
            pc = _cpds.getPooledConnection(username, password);
        }
        // should we add this object as a listener or the pool.
        // consider the validateObject method in decision
        pc.addConnectionEventListener(this);
        obj = new PooledConnectionAndInfo(pc, username, password);
        pcMap.put(pc, obj);
 
        return obj;
    }

    public void destroyObject(Object key, Object obj) throws Exception {
        if (obj instanceof PooledConnectionAndInfo) {
            ((PooledConnectionAndInfo)obj).getPooledConnection().close();
        }
    }

    public boolean validateObject(Object key, Object obj) {
        boolean valid = false;
        if (obj instanceof PooledConnectionAndInfo) {
            PooledConnection pconn = 
                ((PooledConnectionAndInfo)obj).getPooledConnection();
            String query = _validationQuery;
            if (null != query) {
                Connection conn = null;
                Statement stmt = null;
                ResultSet rset = null;
                // logical Connection from the PooledConnection must be closed
                // before another one can be requested and closing it will 
                // generate an event. Keep track so we know not to return
                // the PooledConnection 
                validatingMap.put(pconn, null);
                try {
                    conn = pconn.getConnection();
                    stmt = conn.createStatement();
                    rset = stmt.executeQuery(query);
                    if (rset.next()) {
                        valid = true;
                    } else {
                        valid = false;
                    }
                } catch(Exception e) {
                    valid = false;
                } finally {
                    try {
                        rset.close();
                    } catch (Throwable t) {
                        // ignore
                    }
                    try {
                        stmt.close();
                    } catch (Throwable t) {
                        // ignore
                    }
                    try {
                        conn.close();
                    } catch (Throwable t) {
                        // ignore
                    }
                    validatingMap.remove(pconn);
                }
            } else {
                valid = true;
            }
        } else {
            valid = false;
        }
        return valid;
    }

    public void passivateObject(Object key, Object obj) {
    }

    public void activateObject(Object key, Object obj) {
    }

    // ***********************************************************************
    // java.sql.ConnectionEventListener implementation
    // ***********************************************************************

    /**
     * This will be called if the Connection returned by the getConnection
     * method came from a PooledConnection, and the user calls the close()
     * method of this connection object. What we need to do here is to
     * release this PooledConnection from our pool...
     */
    public void connectionClosed(ConnectionEvent event) {
        PooledConnection pc = (PooledConnection)event.getSource();
        // if this event occured becase we were validating, ignore it
        // otherwise return the connection to the pool.
        if (!validatingMap.containsKey(pc)) {
            PooledConnectionAndInfo info = 
                (PooledConnectionAndInfo) pcMap.get(pc);
            if (info == null) {
                throw new IllegalStateException(NO_KEY_MESSAGE);
            }            
            try {
                _pool.returnObject(info.getUserPassKey(), info);
            } catch (Exception e) {
                System.err.println("CLOSING DOWN CONNECTION AS IT COULD " + 
                                   "NOT BE RETURNED TO THE POOL");
                try {
                    destroyObject(info.getUserPassKey(), info);
                } catch (Exception e2) {
                    System.err.println("EXCEPTION WHILE DESTROYING OBJECT " + 
                                       info);
                    e2.printStackTrace();
                }
            }
        }
    }

    /**
     * If a fatal error occurs, close the underlying physical connection so as
     * not to be returned in the future
     */
    public void connectionErrorOccurred(ConnectionEvent event) {
        PooledConnection pc = (PooledConnection)event.getSource();
        try {
            if (null != event.getSQLException()) {
                System.err
                    .println("CLOSING DOWN CONNECTION DUE TO INTERNAL ERROR (" +
                             event.getSQLException() + ")");
            }
            //remove this from the listener list because we are no more 
            //interested in errors since we are about to close this connection
            pc.removeConnectionEventListener(this);
        } catch (Exception ignore) {
            // ignore
        }

        PooledConnectionAndInfo info = (PooledConnectionAndInfo) pcMap.get(pc);
        if (info == null) {
            throw new IllegalStateException(NO_KEY_MESSAGE);
        }            
        try {
            destroyObject(info.getUserPassKey(), info);
        } catch (Exception e) {
            System.err.println("EXCEPTION WHILE DESTROYING OBJECT " + info);
            e.printStackTrace();
        }
    }
}
