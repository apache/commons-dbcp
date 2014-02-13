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

import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.pool2.KeyedObjectPool;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;
import org.apache.commons.pool2.impl.DefaultPooledObject;

/**
 * A {@link PooledObjectFactory} that creates
 * {@link PoolableConnection}s.
 *
 * @author Rodney Waldhoff
 * @author Glenn L. Nielsen
 * @author James House
 * @author Dirk Verbeeck
 * @version $Revision$ $Date$
 */
public class PoolableConnectionFactory
        implements PooledObjectFactory<PoolableConnection> {

    private static final Log log =
            LogFactory.getLog(PoolableConnectionFactory.class);

    private static MBeanServer MBEAN_SERVER = ManagementFactory.getPlatformMBeanServer();

    /**
     * Create a new <tt>PoolableConnectionFactory</tt>.
     * @param connFactory the {@link ConnectionFactory} from which to obtain
     * base {@link Connection}s
     */
    public PoolableConnectionFactory(ConnectionFactory connFactory,
            ObjectName dataSourceJmxName) {
        _connFactory = connFactory;
        this.dataSourceJmxName = dataSourceJmxName;
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
    public void setConnectionInitSql(Collection<String> connectionInitSqls) {
        _connectionInitSqls = connectionInitSqls;
    }

    /**
     * Sets the {@link ObjectPool} in which to pool {@link Connection}s.
     * @param pool the {@link ObjectPool} in which to pool those {@link Connection}s
     */
    synchronized public void setPool(ObjectPool<PoolableConnection> pool) {
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
    public synchronized ObjectPool<PoolableConnection> getPool() {
        return _pool;
    }

    /**
     * Sets the default "read only" setting for borrowed {@link Connection}s
     * @param defaultReadOnly the default "read only" setting for borrowed {@link Connection}s
     */
    public void setDefaultReadOnly(Boolean defaultReadOnly) {
        _defaultReadOnly = defaultReadOnly;
    }

    /**
     * Sets the default "auto commit" setting for borrowed {@link Connection}s
     * @param defaultAutoCommit the default "auto commit" setting for borrowed {@link Connection}s
     */
    public void setDefaultAutoCommit(Boolean defaultAutoCommit) {
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

    public void setCacheState(boolean cacheState) {
        this._cacheState = cacheState;
    }

    public void setPoolStatements(boolean poolStatements) {
        this.poolStatements = poolStatements;
    }

    public void setMaxOpenPrepatedStatements(int maxOpenPreparedStatements) {
        this.maxOpenPreparedStatements = maxOpenPreparedStatements;
    }

    /**
     * Sets the maximum lifetime in milliseconds of a connection after which the
     * connection will always fail activation, passivation and validation. A
     * value of zero or less indicates an infinite lifetime. The default value
     * is -1.
     */
    public void setMaxConnLifetimeMillis(long maxConnLifetimeMillis) {
        this.maxConnLifetimeMillis = maxConnLifetimeMillis;
    }

    @Override
    public PooledObject<PoolableConnection> makeObject() throws Exception {
        Connection conn = _connFactory.createConnection();
        if (conn == null) {
            throw new IllegalStateException("Connection factory returned null from createConnection");
        }
        try {
            initializeConnection(conn);
        } catch (SQLException sqle) {
            // Make sure the connection is closed
            try {
                conn.close();
            } catch (SQLException ignore) {
                // ignore
            }
            // Rethrow original exception so it is visible to caller
            throw sqle;
        }

        long connIndex = connectionIndex.getAndIncrement();

        if(poolStatements) {
            conn = new PoolingConnection(conn);
            GenericKeyedObjectPoolConfig config = new GenericKeyedObjectPoolConfig();
            config.setMaxTotalPerKey(-1);
            config.setBlockWhenExhausted(false);
            config.setMaxWaitMillis(0);
            config.setMaxIdlePerKey(1);
            config.setMaxTotal(maxOpenPreparedStatements);
            if (dataSourceJmxName != null) {
                StringBuilder base = new StringBuilder(dataSourceJmxName.toString());
                base.append(",connection=");
                config.setJmxNameBase(base.toString());
                config.setJmxNamePrefix(Long.toString(connIndex) + ",pool=statements");
            }
            KeyedObjectPool<PStmtKey,DelegatingPreparedStatement> stmtPool =
                    new GenericKeyedObjectPool<>((PoolingConnection)conn, config);
            ((PoolingConnection)conn).setStatementPool(stmtPool);
            ((PoolingConnection) conn).setCacheState(_cacheState);
        }

        PoolableConnection pc = new PoolableConnection(conn,_pool);

        // Register this connection with JMX
        if (dataSourceJmxName != null) {
            StringBuilder connectionJmxName = new StringBuilder(dataSourceJmxName.toString());
            connectionJmxName.append(",connection=");
            connectionJmxName.append(connIndex);
            jmxRegister(pc, connectionJmxName.toString());
        }

        return new DefaultPooledObject<>(pc);
    }

    private void jmxRegister(PoolableConnection pc, String jmxName) {
        try {
            ObjectName oName = new ObjectName(jmxName);
            MBEAN_SERVER.registerMBean(pc, oName);
        } catch (MalformedObjectNameException | InstanceAlreadyExistsException |
                MBeanRegistrationException | NotCompliantMBeanException e) {
            // For now, simply skip registration
        }
    }

    protected void initializeConnection(Connection conn) throws SQLException {
        Collection<String> sqls = _connectionInitSqls;
        if(conn.isClosed()) {
            throw new SQLException("initializeConnection: connection closed");
        }
        if(null != sqls) {
            try (Statement stmt = conn.createStatement();) {
                for (String sql : sqls) {
                    if (sql == null) {
                        throw new NullPointerException(
                                "null connectionInitSqls element");
                    }
                    stmt.execute(sql);
                }
            }
        }
    }

    @Override
    public void destroyObject(PooledObject<PoolableConnection> p)
            throws Exception {
        p.getObject().reallyClose();
    }

    @Override
    public boolean validateObject(PooledObject<PoolableConnection> p) {
        try {
            validateLifetime(p);

            validateConnection(p.getObject());
            return true;
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug(Utils.getMessage(
                        "poolableConnectionFactory.validateObject.fail"), e);
            }
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
    public void passivateObject(PooledObject<PoolableConnection> p)
            throws Exception {

        validateLifetime(p);

        PoolableConnection conn = p.getObject();
        boolean connAutoCommit = conn.getAutoCommit();
        if(!connAutoCommit && !conn.isReadOnly()) {
            conn.rollback();
        }
        conn.clearWarnings();
        // DBCP-97 Idle connections in the pool should have autoCommit enabled
        if(!connAutoCommit) {
            conn.setAutoCommit(true);
        }

        conn.passivate();
    }

    @Override
    public void activateObject(PooledObject<PoolableConnection> p)
            throws Exception {

        validateLifetime(p);

        PoolableConnection conn = p.getObject();
        conn.activate();

        if (_defaultAutoCommit != null &&
                conn.getAutoCommit() != _defaultAutoCommit.booleanValue()) {
            conn.setAutoCommit(_defaultAutoCommit.booleanValue());
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

    private void validateLifetime(PooledObject<PoolableConnection> p)
            throws Exception {
        if (maxConnLifetimeMillis > 0) {
            long lifetime = System.currentTimeMillis() - p.getCreateTime();
            if (lifetime > maxConnLifetimeMillis) {
                throw new Exception(Utils.getMessage(
                        "connectionFactory.lifetimeExceeded",
                        Long.valueOf(lifetime),
                        Long.valueOf(maxConnLifetimeMillis)));
            }
        }
    }

    protected ConnectionFactory getConnectionFactory() {
        return _connFactory;
    }

    protected boolean getPoolStatements() {
        return poolStatements;
    }

    protected int getMaxOpenPreparedStatements() {
        return maxOpenPreparedStatements;
    }

    protected boolean getCacheState() {
        return _cacheState;
    }

    private final ConnectionFactory _connFactory;
    private final ObjectName dataSourceJmxName;
    private volatile String _validationQuery = null;
    private volatile int _validationQueryTimeout = -1;
    private Collection<String> _connectionInitSqls = null;
    private volatile ObjectPool<PoolableConnection> _pool = null;
    private Boolean _defaultReadOnly = null;
    private Boolean _defaultAutoCommit = null;
    private int _defaultTransactionIsolation = UNKNOWN_TRANSACTIONISOLATION;
    private String _defaultCatalog;
    private boolean _cacheState;
    private boolean poolStatements = false;
    private int maxOpenPreparedStatements =
        GenericKeyedObjectPoolConfig.DEFAULT_MAX_TOTAL_PER_KEY;
    private long maxConnLifetimeMillis = -1;
    private AtomicLong connectionIndex = new AtomicLong(0);

    /**
     * Internal constant to indicate the level is not set.
     */
    static final int UNKNOWN_TRANSACTIONISOLATION = -1;
}
