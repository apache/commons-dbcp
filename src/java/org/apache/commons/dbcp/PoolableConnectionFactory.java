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

import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.commons.pool.*;

/**
 * A {@link PoolableObjectFactory} that creates
 * {@link PoolableConnection}s.
 *
 * @author Rodney Waldhoff
 * @author Glenn L. Nielsen
 * @author James House
 * @author Dirk Verbeeck
 * @version $Revision: 1.22 $ $Date: 2004/05/20 13:11:56 $
 */
public class PoolableConnectionFactory implements PoolableObjectFactory {
    /**
     * Create a new <tt>PoolableConnectionFactory</tt>.
     * @param connFactory the {@link ConnectionFactory} from which to obtain base {@link Connection}s
     * @param pool the {@link ObjectPool} in which to pool those {@link Connection}s
     * @param stmtPoolFactory the {@link KeyedObjectPoolFactory} to use to create {@link KeyedObjectPool}s for pooling {@link java.sql.PreparedStatement}s, or <tt>null</tt> to disable {@link java.sql.PreparedStatement} pooling
     * @param validationQuery a query to use to {@link #validateObject validate} {@link Connection}s.  Should return at least one row. Using <tt>null</tt> turns off validation.
     * @param defaultReadOnly the default "read only" setting for borrowed {@link Connection}s
     * @param defaultAutoCommit the default "auto commit" setting for returned {@link Connection}s
     */
    public PoolableConnectionFactory(ConnectionFactory connFactory, ObjectPool pool, KeyedObjectPoolFactory stmtPoolFactory, String validationQuery, boolean defaultReadOnly, boolean defaultAutoCommit) {
        _connFactory = connFactory;
        _pool = pool;
        _pool.setFactory(this);
        _stmtPoolFactory = stmtPoolFactory;
        _validationQuery = validationQuery;
        _defaultReadOnly = Boolean.valueOf(defaultReadOnly);
        _defaultAutoCommit = defaultAutoCommit;
    }

    /**
     * Create a new <tt>PoolableConnectionFactory</tt>.
     * @param connFactory the {@link ConnectionFactory} from which to obtain base {@link Connection}s
     * @param pool the {@link ObjectPool} in which to pool those {@link Connection}s
     * @param stmtPoolFactory the {@link KeyedObjectPoolFactory} to use to create {@link KeyedObjectPool}s for pooling {@link java.sql.PreparedStatement}s, or <tt>null</tt> to disable {@link java.sql.PreparedStatement} pooling
     * @param validationQuery a query to use to {@link #validateObject validate} {@link Connection}s.  Should return at least one row. Using <tt>null</tt> turns off validation.
     * @param defaultReadOnly the default "read only" setting for borrowed {@link Connection}s
     * @param defaultAutoCommit the default "auto commit" setting for returned {@link Connection}s
     * @param defaultTransactionIsolation the default "Transaction Isolation" setting for returned {@link Connection}s
     */
    public PoolableConnectionFactory(ConnectionFactory connFactory, ObjectPool pool, KeyedObjectPoolFactory stmtPoolFactory, String validationQuery, boolean defaultReadOnly, boolean defaultAutoCommit, int defaultTransactionIsolation) {
        _connFactory = connFactory;
        _pool = pool;
        _pool.setFactory(this);
        _stmtPoolFactory = stmtPoolFactory;
        _validationQuery = validationQuery;
        _defaultReadOnly = Boolean.valueOf(defaultReadOnly);
        _defaultAutoCommit = defaultAutoCommit;
        _defaultTransactionIsolation = defaultTransactionIsolation;
    }

    /**
     * Create a new <tt>PoolableConnectionFactory</tt>.
     * @param connFactory the {@link ConnectionFactory} from which to obtain base {@link Connection}s
     * @param pool the {@link ObjectPool} in which to pool those {@link Connection}s
     * @param stmtPoolFactory the {@link KeyedObjectPoolFactory} to use to create {@link KeyedObjectPool}s for pooling {@link java.sql.PreparedStatement}s, or <tt>null</tt> to disable {@link java.sql.PreparedStatement} pooling
     * @param validationQuery a query to use to {@link #validateObject validate} {@link Connection}s.  Should return at least one row. Using <tt>null</tt> turns off validation.
     * @param defaultReadOnly the default "read only" setting for borrowed {@link Connection}s
     * @param defaultAutoCommit the default "auto commit" setting for returned {@link Connection}s
     * @param config the AbandonedConfig if tracing SQL objects
     * @deprecated AbandonedConfig is now deprecated.
     */
    public PoolableConnectionFactory(
        ConnectionFactory connFactory,
        ObjectPool pool,
        KeyedObjectPoolFactory stmtPoolFactory,
        String validationQuery,
        boolean defaultReadOnly,
        boolean defaultAutoCommit,
        AbandonedConfig config) {
            
        _connFactory = connFactory;
        _pool = pool;
        _config = config;
        _pool.setFactory(this);
        _stmtPoolFactory = stmtPoolFactory;
        _validationQuery = validationQuery;
        _defaultReadOnly = Boolean.valueOf(defaultReadOnly);
        _defaultAutoCommit = defaultAutoCommit;
    }

    /**
     * Create a new <tt>PoolableConnectionFactory</tt>.
     * @param connFactory the {@link ConnectionFactory} from which to obtain base {@link Connection}s
     * @param pool the {@link ObjectPool} in which to pool those {@link Connection}s
     * @param stmtPoolFactory the {@link KeyedObjectPoolFactory} to use to create {@link KeyedObjectPool}s for pooling {@link java.sql.PreparedStatement}s, or <tt>null</tt> to disable {@link java.sql.PreparedStatement} pooling
     * @param validationQuery a query to use to {@link #validateObject validate} {@link Connection}s.  Should return at least one row. Using <tt>null</tt> turns off validation.
     * @param defaultReadOnly the default "read only" setting for borrowed {@link Connection}s
     * @param defaultAutoCommit the default "auto commit" setting for returned {@link Connection}s
     * @param defaultTransactionIsolation the default "Transaction Isolation" setting for returned {@link Connection}s
     * @param config the AbandonedConfig if tracing SQL objects
     * @deprecated AbandonedConfig is now deprecated.
     */
    public PoolableConnectionFactory(
        ConnectionFactory connFactory,
        ObjectPool pool,
        KeyedObjectPoolFactory stmtPoolFactory,
        String validationQuery,
        boolean defaultReadOnly,
        boolean defaultAutoCommit,
        int defaultTransactionIsolation,
        AbandonedConfig config) {
            
        _connFactory = connFactory;
        _pool = pool;
        _config = config;
        _pool.setFactory(this);
        _stmtPoolFactory = stmtPoolFactory;
        _validationQuery = validationQuery;
        _defaultReadOnly = Boolean.valueOf(defaultReadOnly);
        _defaultAutoCommit = defaultAutoCommit;
        _defaultTransactionIsolation = defaultTransactionIsolation;
    }

    /**
     * Create a new <tt>PoolableConnectionFactory</tt>.
     * @param connFactory the {@link ConnectionFactory} from which to obtain base {@link Connection}s
     * @param pool the {@link ObjectPool} in which to pool those {@link Connection}s
     * @param stmtPoolFactory the {@link KeyedObjectPoolFactory} to use to create {@link KeyedObjectPool}s for pooling {@link java.sql.PreparedStatement}s, or <tt>null</tt> to disable {@link java.sql.PreparedStatement} pooling
     * @param validationQuery a query to use to {@link #validateObject validate} {@link Connection}s.  Should return at least one row. Using <tt>null</tt> turns off validation.
     * @param defaultReadOnly the default "read only" setting for borrowed {@link Connection}s
     * @param defaultAutoCommit the default "auto commit" setting for returned {@link Connection}s
     * @param defaultTransactionIsolation the default "Transaction Isolation" setting for returned {@link Connection}s
     * @param defaultCatalog the default "catalog" setting for returned {@link Connection}s
     * @param config the AbandonedConfig if tracing SQL objects
     * @deprecated AbandonedConfig is now deprecated.
     */
    public PoolableConnectionFactory(
        ConnectionFactory connFactory,
        ObjectPool pool,
        KeyedObjectPoolFactory stmtPoolFactory,
        String validationQuery,
        boolean defaultReadOnly,
        boolean defaultAutoCommit,
        int defaultTransactionIsolation,
        String defaultCatalog,
        AbandonedConfig config) {
            
        _connFactory = connFactory;
        _pool = pool;
        _config = config;
        _pool.setFactory(this);
        _stmtPoolFactory = stmtPoolFactory;
        _validationQuery = validationQuery;
        _defaultReadOnly = Boolean.valueOf(defaultReadOnly);
        _defaultAutoCommit = defaultAutoCommit;
        _defaultTransactionIsolation = defaultTransactionIsolation;
        _defaultCatalog = defaultCatalog;
    }

    /**
     * Create a new <tt>PoolableConnectionFactory</tt>.
     * @param connFactory the {@link ConnectionFactory} from which to obtain base {@link Connection}s
     * @param pool the {@link ObjectPool} in which to pool those {@link Connection}s
     * @param stmtPoolFactory the {@link KeyedObjectPoolFactory} to use to create {@link KeyedObjectPool}s for pooling {@link java.sql.PreparedStatement}s, or <tt>null</tt> to disable {@link java.sql.PreparedStatement} pooling
     * @param validationQuery a query to use to {@link #validateObject validate} {@link Connection}s.  Should return at least one row. Using <tt>null</tt> turns off validation.
     * @param defaultReadOnly the default "read only" setting for borrowed {@link Connection}s
     * @param defaultAutoCommit the default "auto commit" setting for returned {@link Connection}s
     * @param defaultTransactionIsolation the default "Transaction Isolation" setting for returned {@link Connection}s
     * @param defaultCatalog the default "catalog" setting for returned {@link Connection}s
     * @param config the AbandonedConfig if tracing SQL objects
     */
    public PoolableConnectionFactory(
        ConnectionFactory connFactory,
        ObjectPool pool,
        KeyedObjectPoolFactory stmtPoolFactory,
        String validationQuery,
        Boolean defaultReadOnly,
        boolean defaultAutoCommit,
        int defaultTransactionIsolation,
        String defaultCatalog,
        AbandonedConfig config) {
            
        _connFactory = connFactory;
        _pool = pool;
        _config = config;
        _pool.setFactory(this);
        _stmtPoolFactory = stmtPoolFactory;
        _validationQuery = validationQuery;
        _defaultReadOnly = defaultReadOnly;
        _defaultAutoCommit = defaultAutoCommit;
        _defaultTransactionIsolation = defaultTransactionIsolation;
        _defaultCatalog = defaultCatalog;
    }

    /**
     * Sets the {@link ConnectionFactory} from which to obtain base {@link Connection}s.
     * @param connFactory the {@link ConnectionFactory} from which to obtain base {@link Connection}s
     */
    synchronized public void setConnectionFactory(ConnectionFactory connFactory) {
        _connFactory = connFactory;
    }

    /**
     * Sets the query I use to {@link #validateObject validate} {@link Connection}s.
     * Should return at least one row.
     * Using <tt>null</tt> turns off validation.
     * @param validationQuery a query to use to {@link #validateObject validate} {@link Connection}s.
     */
    synchronized public void setValidationQuery(String validationQuery) {
        _validationQuery = validationQuery;
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

    public ObjectPool getPool() {
        return _pool;
    }

    /**
     * Sets the {@link KeyedObjectPoolFactory} I use to create {@link KeyedObjectPool}s
     * for pooling {@link java.sql.PreparedStatement}s.
     * Set to <tt>null</tt> to disable {@link java.sql.PreparedStatement} pooling.
     * @param stmtPoolFactory the {@link KeyedObjectPoolFactory} to use to create {@link KeyedObjectPool}s for pooling {@link java.sql.PreparedStatement}s
     */
    synchronized public void setStatementPoolFactory(KeyedObjectPoolFactory stmtPoolFactory) {
        _stmtPoolFactory = stmtPoolFactory;
    }

    /**
     * Sets the default "read only" setting for borrowed {@link Connection}s
     * @param defaultReadOnly the default "read only" setting for borrowed {@link Connection}s
     */
    public void setDefaultReadOnly(boolean defaultReadOnly) {
        _defaultReadOnly = Boolean.valueOf(defaultReadOnly);
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
    
    synchronized public Object makeObject() throws Exception {
        Connection conn = _connFactory.createConnection();
        if(null != _stmtPoolFactory) {
            KeyedObjectPool stmtpool = _stmtPoolFactory.createPool();
            conn = new PoolingConnection(conn,stmtpool);
            stmtpool.setFactory((PoolingConnection)conn);
        }
        return new PoolableConnection(conn,_pool,_config);
    }

    public void destroyObject(Object obj) throws Exception {
        if(obj instanceof PoolableConnection) {
            ((PoolableConnection)obj).reallyClose();
        }
    }

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
                rset = stmt.executeQuery(query);
                if(!rset.next()) {
                    throw new SQLException("validationQuery didn't return a row");
                }
            } finally {
                try {
                    rset.close();
                } catch(Exception t) {
                    // ignored
                }
                try {
                    stmt.close();
                } catch(Exception t) {
                    // ignored
                }

            }
        }
    }

    public void passivateObject(Object obj) throws Exception {
        if(obj instanceof Connection) {
            Connection conn = (Connection)obj;
            if(!conn.getAutoCommit() && !conn.isReadOnly()) {
                conn.rollback();
            }
            conn.clearWarnings();
            conn.setAutoCommit(true);
        }
        if(obj instanceof DelegatingConnection) {
            ((DelegatingConnection)obj).passivate();
        }
    }

    public void activateObject(Object obj) throws Exception {
        if(obj instanceof DelegatingConnection) {
            ((DelegatingConnection)obj).activate();
        }
        if(obj instanceof Connection) {
            Connection conn = (Connection)obj;
            conn.setAutoCommit(_defaultAutoCommit);
            if (_defaultTransactionIsolation != UNKNOWN_TRANSACTIONISOLATION) {
                conn.setTransactionIsolation(_defaultTransactionIsolation);
            }
            if (_defaultReadOnly != null) {
                conn.setReadOnly(_defaultReadOnly.booleanValue());
            }
            if (_defaultCatalog != null) {
                conn.setCatalog(_defaultCatalog);
            }
        }
    }

    protected ConnectionFactory _connFactory = null;
    protected String _validationQuery = null;
    protected ObjectPool _pool = null;
    protected KeyedObjectPoolFactory _stmtPoolFactory = null;
    protected Boolean _defaultReadOnly = null;
    protected boolean _defaultAutoCommit = true;
    protected int _defaultTransactionIsolation = UNKNOWN_TRANSACTIONISOLATION;
    protected String _defaultCatalog;
    
    /**
     * @deprecated AbandonedConfig is now deprecated.
     */
    protected AbandonedConfig _config = null;

    /**
     * Internal constant to indicate the level is not set. 
     */
	static final int UNKNOWN_TRANSACTIONISOLATION = -1;
}
