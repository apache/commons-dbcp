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
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;
import org.apache.commons.pool2.KeyedObjectPool;
import org.apache.commons.pool2.PoolableObjectFactory;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;
import org.apache.commons.pool2.impl.WhenExhaustedAction;

/**
 * A {@link PoolableObjectFactory} that creates
 * {@link PoolableConnection}s.
 *
 * @author Rodney Waldhoff
 * @author Glenn L. Nielsen
 * @author James House
 * @author Dirk Verbeeck
 * @version $Revision$ $Date$
 */
public class PoolableConnectionFactory implements PoolableObjectFactory {
    /**
     * Create a new <tt>PoolableConnectionFactory</tt>.
     * @param connFactory the {@link ConnectionFactory} from which to obtain
     * base {@link Connection}s
     */
    public PoolableConnectionFactory(ConnectionFactory connFactory) {
        _connFactory = connFactory;
    }

    /**
     * Sets the {@link ConnectionFactory} from which to obtain base {@link Connection}s.
     * @param connFactory the {@link ConnectionFactory} from which to obtain base {@link Connection}s
     */
    public void setConnectionFactory(ConnectionFactory connFactory) {
        _connFactory = connFactory;
    }

    /**
     * Sets the query I use to {@link #validateObject validate} {@link Connection}s.
     * Should return at least one row.
     * Using <tt>null</tt> turns off validation.
     * @param validationQuery a query to use to {@link #validateObject validate} {@link Connection}s.
     */
    public void setValidationQuery(String validationQuery) {
        _validationQuery = validationQuery;
    }
    
    /**
     * Sets the validation query timeout, the amount of time, in seconds, that
     * connection validation will wait for a response from the database when
     * executing a validation query.  Use a value less than or equal to 0 for
     * no timeout.
     *
     * @param timeout new validation query timeout value in seconds
     * @since 1.3
     */
    public void setValidationQueryTimeout(int timeout) {
        _validationQueryTimeout = timeout;
    }

    /**
     * Sets the SQL statements I use to initialize newly created {@link Connection}s.
     * Using <tt>null</tt> turns off connection initialization.
     * @param connectionInitSqls SQL statement to initialize {@link Connection}s.
     * @since 1.3
     */
    synchronized public void setConnectionInitSql(Collection connectionInitSqls) {
        _connectionInitSqls = connectionInitSqls;
    }

    /**
     * Sets the {@link ObjectPool} in which to pool {@link Connection}s.
     * @param pool the {@link ObjectPool} in which to pool those {@link Connection}s
     */
    synchronized public void setPool(ObjectPool pool) {
        if(null != _pool && pool != _pool) {
            try {
                _pool.close();
            } catch(Exception e) {
                // ignored !?!
            }
        }
        _pool = pool;
    }

    /**
     * Returns the {@link ObjectPool} in which {@link Connection}s are pooled.
     * @return the connection pool
     */
    public synchronized ObjectPool getPool() {
        return _pool;
    }

    /**
     * Sets the default "read only" setting for borrowed {@link Connection}s
     * @param defaultReadOnly the default "read only" setting for borrowed {@link Connection}s
     */
    public void setDefaultReadOnly(boolean defaultReadOnly) {
        _defaultReadOnly = defaultReadOnly ? Boolean.TRUE : Boolean.FALSE;
    }

    /**
     * Sets the default "auto commit" setting for borrowed {@link Connection}s
     * @param defaultAutoCommit the default "auto commit" setting for borrowed {@link Connection}s
     */
    public void setDefaultAutoCommit(boolean defaultAutoCommit) {
        _defaultAutoCommit = defaultAutoCommit;
    }

    /**
     * Sets the default "Transaction Isolation" setting for borrowed {@link Connection}s
     * @param defaultTransactionIsolation the default "Transaction Isolation" setting for returned {@link Connection}s
     */
    public void setDefaultTransactionIsolation(int defaultTransactionIsolation) {
        _defaultTransactionIsolation = defaultTransactionIsolation;
    }

    /**
     * Sets the default "catalog" setting for borrowed {@link Connection}s
     * @param defaultCatalog the default "catalog" setting for borrowed {@link Connection}s
     */
    public void setDefaultCatalog(String defaultCatalog) {
        _defaultCatalog = defaultCatalog;
    }

    public void setAbandonedConfig(AbandonedConfig abandonedConfig) {
        this._config = abandonedConfig;
    }

    public void setCacheState(boolean cacheState) {
        this._cacheState = cacheState;
    }

    public void setPoolStatements(boolean poolStatements) {
        this.poolStatements = poolStatements;
    }

    public void setMaxOpenPrepatedStatements(int maxOpenPreparedStatements) {
        this.maxOpenPreparedStatements = maxOpenPreparedStatements;
    }

    @Override
    public Object makeObject() throws Exception {
        Connection conn = _connFactory.createConnection();
        if (conn == null) {
            throw new IllegalStateException("Connection factory returned null from createConnection");
        }
        initializeConnection(conn);
        if(poolStatements) {
            conn = new PoolingConnection(conn);
            GenericKeyedObjectPoolConfig config =
                new GenericKeyedObjectPoolConfig();
            config.setMaxTotalPerKey(-1);
            config.setWhenExhaustedAction(WhenExhaustedAction.FAIL);
            config.setMaxWait(0);
            config.setMaxIdlePerKey(1);
            config.setMaxTotal(maxOpenPreparedStatements);
            KeyedObjectPool stmtPool =
                new GenericKeyedObjectPool((PoolingConnection)conn, config);
            ((PoolingConnection)conn).setStatementPool(stmtPool);
            ((PoolingConnection) conn).setCacheState(_cacheState);
        }
        return new PoolableConnection(conn,_pool,_config);
    }

    protected void initializeConnection(Connection conn) throws SQLException {
        Collection sqls = _connectionInitSqls;
        if(conn.isClosed()) {
            throw new SQLException("initializeConnection: connection closed");
        }
        if(null != sqls) {
            Statement stmt = null;
            try {
                stmt = conn.createStatement();
                for (Iterator iterator = sqls.iterator(); iterator.hasNext();)
                {
                    Object o = iterator.next();
                    if (o == null) {
                        throw new NullPointerException("null connectionInitSqls element");
                    }
                    // o might not be a String instance
                    String sql = o.toString();
                    stmt.execute(sql);
                }
            } finally {
                if (stmt != null) {
                    try {
                        stmt.close();
                    } catch(Exception t) {
                        // ignored
                    }
                }
            }
        }
    }

    @Override
    public void destroyObject(Object obj) throws Exception {
        if(obj instanceof PoolableConnection) {
            ((PoolableConnection)obj).reallyClose();
        }
    }

    @Override
    public boolean validateObject(Object obj) {
        if(obj instanceof Connection) {
            try {
                validateConnection((Connection) obj);
                return true;
            } catch(Exception e) {
                return false;
            }
        } else {
            return false;
        }
    }

    public void validateConnection(Connection conn) throws SQLException {
        String query = _validationQuery;
        if(conn.isClosed()) {
            throw new SQLException("validateConnection: connection closed");
        }
        if(null != query) {
            Statement stmt = null;
            ResultSet rset = null;
            try {
                stmt = conn.createStatement();
                if (_validationQueryTimeout > 0) {
                    stmt.setQueryTimeout(_validationQueryTimeout);
                }
                rset = stmt.executeQuery(query);
                if(!rset.next()) {
                    throw new SQLException("validationQuery didn't return a row");
                }
            } finally {
                if (rset != null) {
                    try {
                        rset.close();
                    } catch(Exception t) {
                        // ignored
                    }
                }
                if (stmt != null) {
                    try {
                        stmt.close();
                    } catch(Exception t) {
                        // ignored
                    }
                }
            }
        }
    }

    @Override
    public void passivateObject(Object obj) throws Exception {
        if(obj instanceof Connection) {
            Connection conn = (Connection)obj;
            if(!conn.getAutoCommit() && !conn.isReadOnly()) {
                conn.rollback();
            }
            conn.clearWarnings();
            if(!conn.getAutoCommit()) {
                conn.setAutoCommit(true);
            }
        }
        if(obj instanceof DelegatingConnection) {
            ((DelegatingConnection)obj).passivate();
        }
    }

    @Override
    public void activateObject(Object obj) throws Exception {
        if(obj instanceof DelegatingConnection) {
            ((DelegatingConnection)obj).activate();
        }
        if(obj instanceof Connection) {
            Connection conn = (Connection)obj;
            if (conn.getAutoCommit() != _defaultAutoCommit) {
                conn.setAutoCommit(_defaultAutoCommit);
            }
            if ((_defaultTransactionIsolation != UNKNOWN_TRANSACTIONISOLATION) 
                    && (conn.getTransactionIsolation() != 
                    _defaultTransactionIsolation)) {
                conn.setTransactionIsolation(_defaultTransactionIsolation);
            }
            if ((_defaultReadOnly != null) && 
                    (conn.isReadOnly() != _defaultReadOnly.booleanValue())) {
                conn.setReadOnly(_defaultReadOnly.booleanValue());
            }
            if ((_defaultCatalog != null) &&
                    (!_defaultCatalog.equals(conn.getCatalog()))) {
                conn.setCatalog(_defaultCatalog);
            }
        }
    }

    protected volatile ConnectionFactory _connFactory = null;
    protected volatile String _validationQuery = null;
    protected volatile int _validationQueryTimeout = -1;
    protected Collection _connectionInitSqls = null;
    protected volatile ObjectPool _pool = null;
    protected Boolean _defaultReadOnly = null;
    protected boolean _defaultAutoCommit = true;
    protected int _defaultTransactionIsolation = UNKNOWN_TRANSACTIONISOLATION;
    protected String _defaultCatalog;
    protected boolean _cacheState;
    protected boolean poolStatements = false;
    protected int maxOpenPreparedStatements =
        GenericKeyedObjectPoolConfig.DEFAULT_MAX_TOTAL_PER_KEY;

    /**
     * Configuration for removing abandoned connections.
     */
    protected AbandonedConfig _config = null;

    /**
     * Internal constant to indicate the level is not set.
     */
    static final int UNKNOWN_TRANSACTIONISOLATION = -1;
}
