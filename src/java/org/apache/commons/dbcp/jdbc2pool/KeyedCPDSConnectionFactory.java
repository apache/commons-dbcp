/*
 * $Header: /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons//dbcp/src/java/org/apache/commons/dbcp/jdbc2pool/Attic/KeyedCPDSConnectionFactory.java,v 1.2 2002/11/01 16:03:21 rwaldhoff Exp $
 * $Revision: 1.2 $
 * $Date: 2002/11/01 16:03:21 $
 *
 * ====================================================================
 *
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999-2001 The Apache Software Foundation.  All rights
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

package org.apache.commons.dbcp.jdbc2pool;

import java.util.Map;
import java.util.HashMap;
import java.util.WeakHashMap;
import java.sql.*;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.PooledConnection;
import javax.sql.ConnectionEvent;
import javax.sql.ConnectionEventListener;

import org.apache.commons.pool.*;
import org.apache.commons.dbcp.*;

/**
 * A {*link PoolableObjectFactory} that creates
 * {*link PoolableConnection}s.
 *
 * @author <a href="mailto:jmcnally@collab.net">John D. McNally</a>
 * @version $Id: KeyedCPDSConnectionFactory.java,v 1.2 2002/11/01 16:03:21 rwaldhoff Exp $
 */
class KeyedCPDSConnectionFactory 
    implements KeyedPoolableObjectFactory, ConnectionEventListener {
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
        throws SQLException
    {
        if(null != _pool && pool != _pool) {
            try
            {
                _pool.close();
            }
            catch (Exception e)
            {
                if (e instanceof RuntimeException) 
                {
                    throw (RuntimeException)e;
                }
                else 
                {
                    throw new SQLException(e.getMessage());
                }
            }
        }
        _pool = pool;
    }

    public KeyedObjectPool getPool() {
        return _pool;
    }

    synchronized public Object makeObject(Object key) {
        UserPassKey upkey = (UserPassKey)key;
        // since we are using the key to make a new object, we will
        // declare the key invalid for reuse.
        upkey.setReusable(false);
        String username = upkey.getUsername();
        PooledConnection pc = null;
        try
        {
            if ( username == null ) 
            {
                pc = _cpds.getPooledConnection();
            }
            else 
            {
                pc = _cpds.getPooledConnection(username, upkey.getPassword());
            }
            // should we add this object as a listener or the pool.
            // consider the validateObject method in decision
            pc.addConnectionEventListener(this);
            pcKeyMap.put(pc, key);
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e.getMessage());
        }
        return pc;
    }

    public void destroyObject(Object key, Object obj) {
        if(obj instanceof PooledConnection) {
            try {
                ((PooledConnection)obj).close();
            } catch(RuntimeException e) {
                throw e;
            } catch(SQLException e) {
                ; // ignored
            }
        }
    }

    public boolean validateObject(Object key, Object obj) {
        boolean valid = false;
        if(obj instanceof PooledConnection) {
            PooledConnection pconn = (PooledConnection)obj;
            String query = _validationQuery;
            if(null != query) {
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
                    if(rset.next()) {
                        valid = true;
                    } else {
                        valid = false;
                    }
                } catch(Exception e) {
                    valid = false;
                }
                finally 
                {
                    try {
                        rset.close();
                    } catch(Throwable t) {
                        // ignore
                    }
                    try {
                        stmt.close();
                    } catch(Throwable t) {
                        // ignore
                    }
                    try {
                        conn.close();
                    } catch(Throwable t) {
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
    public void connectionClosed(ConnectionEvent event) 
    {
        PooledConnection pc = (PooledConnection)event.getSource();
        // if this event occured becase we were validating, ignore it
        // otherwise return the connection to the pool.
        if (!validatingMap.containsKey(pc)) 
        {
            Object key = pcKeyMap.get(pc);
            if (key == null) 
            {
                throw new IllegalStateException(NO_KEY_MESSAGE);
            }            
            try
            {
                _pool.returnObject(key, pc);
            }
            catch (Exception e)
            {
                destroyObject(key, pc);
                System.err.println("CLOSING DOWN CONNECTION AS IT COULD " + 
                                   "NOT BE RETURNED TO THE POOL");
            }
        }
    }

    /**
     * If a fatal error occurs, close the underlying physical connection so as
     * not to be returned in the future
     */
    public void connectionErrorOccurred(ConnectionEvent event) 
    {
        PooledConnection pc = (PooledConnection)event.getSource();
        try 
        {
            if(null != event.getSQLException()) {
                System.err
                    .println("CLOSING DOWN CONNECTION DUE TO INTERNAL ERROR (" +
                             event.getSQLException() + ")");
            }
            //remove this from the listener list because we are no more 
            //interested in errors since we are about to close this connection
            pc.removeConnectionEventListener(this);
        }
        catch (Exception ignore) 
        {
            // ignore
        }

        Object key = pcKeyMap.get(pc);
        if (key == null) 
        {
            throw new IllegalStateException(NO_KEY_MESSAGE);
        }            
        destroyObject(key, pc);
    }

    private static final String NO_KEY_MESSAGE = 
        "close() was called on a Connection, but " + 
        "I have no record of the underlying PooledConnection.";

    protected ConnectionPoolDataSource _cpds = null;
    protected String _validationQuery = null;
    protected KeyedObjectPool _pool = null;
    private Map validatingMap = new HashMap();
    private WeakHashMap pcKeyMap = new WeakHashMap();
}
