/*
 * $Header: /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons//dbcp/src/java/org/apache/commons/dbcp/PoolingConnection.java,v 1.2 2002/03/17 14:55:20 rwaldhoff Exp $
 * $Revision: 1.2 $
 * $Date: 2002/03/17 14:55:20 $
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

package org.apache.commons.dbcp;

import java.sql.*;
import org.apache.commons.pool.*;

/**
 * A {@link DelegatingConnection} that pools {@link PreparedStatement}s.
 * <p>
 * My {@link #prepareStatement} methods, rather than creating a new {@link PreparedStatement}
 * each time, may actually pull the {@link PreparedStatement} from a pool of unused statements.
 * The {@link PreparedStatement#close} method of the returned {@link PreparedStatement} doesn't
 * actually close the statement, but rather returns it to my pool. (See {@link PoolablePreparedStatement}.)
 *
 * @see PoolablePreparedStatement
 * @author Rodney Waldhoff (<a href="mailto:rwaldhof@us.britannica.com">rwaldhof@us.britannica.com</a>)
 */
public class PoolingConnection extends DelegatingConnection implements Connection, KeyedPoolableObjectFactory {
    /** My pool of {@link PreparedStatement}s. */
    protected KeyedObjectPool _pstmtPool = null;

    /**
     * Constructor.
     * @param c the underlying {@link Connection}.
     */
    public PoolingConnection(Connection c) {
        super(c);
    }

    /**
     * Constructor.
     * @param c the underlying {@link Connection}.
     * @param maxSleepingPerKey the maximum number of {@link PreparedStatement}s that may sit idle in my pool (per type)
     */
    public PoolingConnection(Connection c, KeyedObjectPool pool) {
        super(c);
        _pstmtPool = pool;
    }


    /**
     * Close and free all {@link PreparedStatement}s from my pool, and
     * close my underlying connection.
     */
    public synchronized void close() throws SQLException {
        if(null != _pstmtPool) {
            KeyedObjectPool oldpool = _pstmtPool;            
            _pstmtPool = null;
            try {
                oldpool.close();
            } catch(RuntimeException e) {
                throw e;
            } catch(SQLException e) {
                throw e;
            } catch(Exception e) {
                throw new SQLException(e.toString());
            }
        }
        getInnermostDelegate().close();
    }

    /**
     * Create or obtain a {@link PreparedStatement} from my pool.
     * @return a {@link PoolablePreparedStatement}
     */
    public synchronized PreparedStatement prepareStatement(String sql) throws SQLException {
        try {
            return(PreparedStatement)(_pstmtPool.borrowObject(createKey(sql)));
        } catch(RuntimeException e) {
            throw e;
        } catch(Exception e) {
            throw new SQLException(e.toString());
        }
    }

    /**
     * Create or obtain a {@link PreparedStatement} from my pool.
     * @return a {@link PoolablePreparedStatement}
     */
    public synchronized PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        try {
            return(PreparedStatement)(_pstmtPool.borrowObject(createKey(sql,resultSetType,resultSetConcurrency)));
        } catch(RuntimeException e) {
            throw e;
        } catch(Exception e) {
            throw new SQLException(e.toString());
        }
    }

    /**
     * Create a {@link PoolingConnection.PStmtKey} for the given arguments.
     */
    protected Object createKey(String sql, int resultSetType, int resultSetConcurrency) {
        return new PStmtKey(normalizeSQL(sql),resultSetType,resultSetConcurrency);
    }

    /**
     * Create a {@link PoolingConnection.PStmtKey} for the given arguments.
     */
    protected Object createKey(String sql) {
        return new PStmtKey(normalizeSQL(sql));
    }

    /**
     * Normalize the given SQL statement, producing a
     * cannonical form that is semantically equivalent to the original.
     */
    protected String normalizeSQL(String sql) {
        return sql.trim();
    }

    /**
     * My {@link KeyedPoolableObjectFactory} method for creating
     * {@link PreparedStatement}s.
     * @param obj the key for the {@link PreparedStatement} to be created
     */
    public Object makeObject(Object obj) {
        try {
            if(null == obj || !(obj instanceof PStmtKey)) {
                throw new IllegalArgumentException();
            } else {
                // _openPstmts++;
                PStmtKey key = (PStmtKey)obj;
                if(null == key._resultSetType && null == key._resultSetConcurrency) {
                    return new PoolablePreparedStatement(getDelegate().prepareStatement(key._sql),key,_pstmtPool,this);
                } else {
                    return new PoolablePreparedStatement(getDelegate().prepareStatement(key._sql,key._resultSetType.intValue(),key._resultSetConcurrency.intValue()),key,_pstmtPool,this);
                }
            }
        } catch(Exception e) {
            throw new RuntimeException(e.toString());
        }
    }

    /**
     * My {@link KeyedPoolableObjectFactory} method for destroying
     * {@link PreparedStatement}s.
     * @param key ignored
     * @param obj the {@link PreparedStatement} to be destroyed.
     */
    public void destroyObject(Object key, Object obj) {
        //_openPstmts--;
        try {
            ((DelegatingPreparedStatement)obj).getInnermostDelegate().close();
        } catch(SQLException e) {
            // ignored
        } catch(NullPointerException e) {
            // ignored
        } catch(ClassCastException e) {
            try {
                ((PreparedStatement)obj).close();
            } catch(SQLException e2) {
                // ignored
            } catch(ClassCastException e2) {
                // ignored
            }
        }
    }

    /**
     * My {@link KeyedPoolableObjectFactory} method for validating
     * {@link PreparedStatement}s.
     * @param key ignored
     * @param obj ignored
     * @return <tt>true</tt>
     */
    public boolean validateObject(Object key, Object obj) {
        return true;
    }

    /**
     * My {@link KeyedPoolableObjectFactory} method for activating
     * {@link PreparedStatement}s. (Currently a no-op.)
     * @param key ignored
     * @param obj ignored
     */
    public void activateObject(Object key, Object obj) {
        ((DelegatingPreparedStatement)obj).activate();
    }

    /**
     * My {@link KeyedPoolableObjectFactory} method for passivating
     * {@link PreparedStatement}s.  Currently invokes {@link PreparedStatement#clearParameters}.
     * @param key ignored
     * @param obj a {@link PreparedStatement}
     */
    public void passivateObject(Object key, Object obj) {
        try {
            ((PreparedStatement)obj).clearParameters();
            ((DelegatingPreparedStatement)obj).passivate();
        } catch(SQLException e) {
            // ignored
        } catch(NullPointerException e) {
            // ignored
        } catch(ClassCastException e) {
            // ignored
        }
    }

    public String toString() {
        return "PoolingConnection: " + _pstmtPool.toString();
    }

    /**
     * A key uniquely identifiying {@link PreparedStatement}s.
     */
    class PStmtKey {
        protected String _sql = null;
        protected Integer _resultSetType = null;
        protected Integer _resultSetConcurrency = null;

        PStmtKey(String sql) {
            _sql = sql;
        }

        PStmtKey(String sql, int resultSetType, int resultSetConcurrency) {
            _sql = sql;
            _resultSetType = new Integer(resultSetType);
            _resultSetConcurrency = new Integer(resultSetConcurrency);
        }

        public boolean equals(Object that) {
            try {
                PStmtKey key = (PStmtKey)that;
                return( ((null == _sql && null == key._sql) || _sql.equals(key._sql)) &&
                        ((null == _resultSetType && null == key._resultSetType) || _resultSetType.equals(key._resultSetType)) &&
                        ((null == _resultSetConcurrency && null == key._resultSetConcurrency) || _resultSetConcurrency.equals(key._resultSetConcurrency))
                      );
            } catch(ClassCastException e) {
                return false;
            } catch(NullPointerException e) {
                return false;
            }
        }

        public int hashCode() {
            return(null == _sql ? 0 : _sql.hashCode());
        }

        public String toString() {
            StringBuffer buf = new StringBuffer();
            buf.append("PStmtKey: sql=");
            buf.append(_sql);
            buf.append(", resultSetType=");
            buf.append(_resultSetType);
            buf.append(", resultSetConcurrency=");
            buf.append(_resultSetConcurrency);
            return buf.toString();
        }
    }
}
