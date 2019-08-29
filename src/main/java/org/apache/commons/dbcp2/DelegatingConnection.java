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

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.ClientInfoStatus;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

/**
 * A base delegating implementation of {@link Connection}.
 * <p>
 * All of the methods from the {@link Connection} interface simply check to see that the {@link Connection} is active,
 * and call the corresponding method on the "delegate" provided in my constructor.
 * </p>
 * <p>
 * Extends AbandonedTrace to implement Connection tracking and logging of code which created the Connection. Tracking
 * the Connection ensures that the AbandonedObjectPool can close this connection and recycle it if its pool of
 * connections is nearing exhaustion and this connection's last usage is older than the removeAbandonedTimeout.
 * </p>
 *
 * @param <C> the Connection type
 *
 * @since 2.0
 */
public class DelegatingConnection<C extends Connection> extends AbandonedTrace implements Connection {

    private static final Map<String, ClientInfoStatus> EMPTY_FAILED_PROPERTIES = Collections
            .<String, ClientInfoStatus>emptyMap();

    /** My delegate {@link Connection}. */
    private volatile C connection;
    private volatile boolean closed;
    private boolean cacheState = true;
    private Boolean autoCommitCached;
    private Boolean readOnlyCached;
    private Integer defaultQueryTimeoutSeconds;

    /**
     * Creates a wrapper for the Connection which traces this Connection in the AbandonedObjectPool.
     *
     * @param c the {@link Connection} to delegate all calls to.
     */
    public DelegatingConnection(final C c) {
        super();
        connection = c;
    }

    @Override
    public void abort(final Executor executor) throws SQLException {
        accept(Jdbc41Bridge::abort, connection, executor);
    }

    protected void activate() {
        closed = false;
        setLastUsed();
        if (connection instanceof DelegatingConnection) {
            ((DelegatingConnection<?>) connection).activate();
        }
    }

    @Override
    protected void checkOpen() throws SQLException {
        if (closed) {
            if (null != connection) {
                String label = "";
                try {
                    label = connection.toString();
                } catch (final Exception ex) {
                    // ignore, leave label empty
                }
                throw new SQLException("Connection " + label + " is closed.");
            }
            throw new SQLException("Connection is null.");
        }
    }

    /**
     * Can be used to clear cached state when it is known that the underlying connection may have been accessed
     * directly.
     */
    public void clearCachedState() {
        autoCommitCached = null;
        readOnlyCached = null;
        if (connection instanceof DelegatingConnection) {
            ((DelegatingConnection<?>) connection).clearCachedState();
        }
    }

    @Override
    public void clearWarnings() throws SQLException {
        accept(connection::clearWarnings);
    }

    /**
     * Closes the underlying connection, and close any Statements that were not explicitly closed. Sub-classes that
     * override this method must:
     * <ol>
     * <li>Call passivate()</li>
     * <li>Call close (or the equivalent appropriate action) on the wrapped connection</li>
     * <li>Set _closed to <code>false</code></li>
     * </ol>
     */
    @Override
    public void close() throws SQLException {
        if (!closed) {
            closeInternal();
        }
    }

    protected final void closeInternal() throws SQLException {
        try {
            passivate();
        } finally {
            if (connection != null) {
                boolean connectionIsClosed;
                try {
                    connectionIsClosed = connection.isClosed();
                } catch (final SQLException e) {
                    // not sure what the state is, so assume the connection is open.
                    connectionIsClosed = false;
                }
                try {
                    // DBCP-512: Avoid exceptions when closing a connection in mutli-threaded use case.
                    // Avoid closing again, which should be a no-op, but some drivers like H2 throw an exception when
                    // closing from multiple threads.
                    if (!connectionIsClosed) {
                        connection.close();
                    }
                } finally {
                    closed = true;
                }
            } else {
                closed = true;
            }
        }
    }

    @Override
    public void commit() throws SQLException {
        accept(connection::commit);
    }

    @Override
    public Array createArrayOf(final String typeName, final Object[] elements) throws SQLException {
        return apply(connection::createArrayOf, typeName, elements);
    }

    @Override
    public Blob createBlob() throws SQLException {
        return apply(connection::createBlob);
    }

    @Override
    public Clob createClob() throws SQLException {
        return apply(connection::createClob);
    }

    @Override
    public NClob createNClob() throws SQLException {
        return apply(connection::createNClob);
    }

    @Override
    public SQLXML createSQLXML() throws SQLException {
        return apply(connection::createSQLXML);
    }

    @Override
    public Statement createStatement() throws SQLException {
        return apply(() -> init(new DelegatingStatement(this, connection.createStatement())));
    }

    @Override
    public Statement createStatement(final int resultSetType, final int resultSetConcurrency) throws SQLException {
        return apply(() -> init(
                new DelegatingStatement(this, connection.createStatement(resultSetType, resultSetConcurrency))));
    }

    @Override
    public Statement createStatement(final int resultSetType, final int resultSetConcurrency,
            final int resultSetHoldability) throws SQLException {
        return apply(() -> init(new DelegatingStatement(this,
                connection.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability))));
    }

    @Override
    public Struct createStruct(final String typeName, final Object[] attributes) throws SQLException {
        return apply(connection::createStruct, typeName, attributes);
    }

    @Override
    public boolean getAutoCommit() throws SQLException {
        checkOpen();
        if (cacheState && autoCommitCached != null) {
            return autoCommitCached.booleanValue();
        }
        try {
            autoCommitCached = Boolean.valueOf(connection.getAutoCommit());
            return autoCommitCached.booleanValue();
        } catch (final SQLException e) {
            handleException(e);
            return false;
        }
    }

    /**
     * Returns the state caching flag.
     *
     * @return the state caching flag
     */
    public boolean getCacheState() {
        return cacheState;
    }

    @Override
    public String getCatalog() throws SQLException {
        return apply(connection::getCatalog);
    }

    @Override
    public Properties getClientInfo() throws SQLException {
        return apply(connection::getClientInfo);
    }

    @Override
    public String getClientInfo(final String name) throws SQLException {
        return apply(connection::getClientInfo, name);
    }

    /**
     * Gets the default query timeout that will be used for {@link Statement}s created from this connection.
     * <code>null</code> means that the driver default will be used.
     *
     * @return query timeout limit in seconds; zero means there is no limit.
     */
    public Integer getDefaultQueryTimeout() {
        return defaultQueryTimeoutSeconds;
    }

    /**
     * Returns my underlying {@link Connection}.
     *
     * @return my underlying {@link Connection}.
     */
    public C getDelegate() {
        return getDelegateInternal();
    }

    protected final C getDelegateInternal() {
        return connection;
    }

    @Override
    public int getHoldability() throws SQLException {
        return applyTo0(connection::getHoldability);
    }

    /**
     * If my underlying {@link Connection} is not a {@code DelegatingConnection}, returns it, otherwise recursively
     * invokes this method on my delegate.
     * <p>
     * Hence this method will return the first delegate that is not a {@code DelegatingConnection}, or {@code null} when
     * no non-{@code DelegatingConnection} delegate can be found by traversing this chain.
     * </p>
     * <p>
     * This method is useful when you may have nested {@code DelegatingConnection}s, and you want to make sure to obtain
     * a "genuine" {@link Connection}.
     * </p>
     *
     * @return innermost delegate.
     */
    public Connection getInnermostDelegate() {
        return getInnermostDelegateInternal();
    }

    /**
     * Although this method is public, it is part of the internal API and should not be used by clients. The signature
     * of this method may change at any time including in ways that break backwards compatibility.
     *
     * @return innermost delegate.
     */
    @SuppressWarnings("resource")
    public final Connection getInnermostDelegateInternal() {
        Connection conn = connection;
        while (conn != null && conn instanceof DelegatingConnection) {
            conn = ((DelegatingConnection<?>) conn).getDelegateInternal();
            if (this == conn) {
                return null;
            }
        }
        return conn;
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        return apply(() -> new DelegatingDatabaseMetaData(this, connection.getMetaData()));
    }

    @Override
    public int getNetworkTimeout() throws SQLException {
        return apply(Jdbc41Bridge::getNetworkTimeout, connection);
    }

    @Override
    public String getSchema() throws SQLException {
        return apply(Jdbc41Bridge::getSchema, connection);
    }

    @Override
    public int getTransactionIsolation() throws SQLException {
        return applyTo0(connection::getTransactionIsolation);
    }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException {
        return apply(connection::getTypeMap);
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return apply(connection::getWarnings);
    }

    @Override
    protected void handleException(final SQLException e) throws SQLException {
        throw e;
    }

    /**
     * Handles the given {@code SQLException}.
     *
     * @param <T> The throwable type.
     * @param e The SQLException
     * @return the given {@code SQLException}
     * @since 2.7.0
     */
    protected <T extends Throwable> T handleExceptionNoThrow(final T e) {
        return e;
    }

    private <T extends DelegatingStatement> T init(final T ds) throws SQLException {
        if (defaultQueryTimeoutSeconds != null && defaultQueryTimeoutSeconds.intValue() != ds.getQueryTimeout()) {
            ds.setQueryTimeout(defaultQueryTimeoutSeconds.intValue());
        }
        return ds;
    }

    /**
     * Compares innermost delegate to the given connection.
     *
     * @param c connection to compare innermost delegate with
     * @return true if innermost delegate equals <code>c</code>
     */
    @SuppressWarnings("resource")
    public boolean innermostDelegateEquals(final Connection c) {
        final Connection innerCon = getInnermostDelegateInternal();
        if (innerCon == null) {
            return c == null;
        }
        return innerCon.equals(c);
    }

    @Override
    public boolean isClosed() throws SQLException {
        return closed || connection == null || connection.isClosed();
    }

    protected boolean isClosedInternal() {
        return closed;
    }

    @Override
    public boolean isReadOnly() throws SQLException {
        checkOpen();
        if (cacheState && readOnlyCached != null) {
            return readOnlyCached.booleanValue();
        }
        try {
            readOnlyCached = Boolean.valueOf(connection.isReadOnly());
            return readOnlyCached.booleanValue();
        } catch (final SQLException e) {
            handleException(e);
            return false;
        }
    }

    @Override
    public boolean isValid(final int timeoutSeconds) throws SQLException {
        if (isClosed()) {
            return false;
        }
        try {
            return connection.isValid(timeoutSeconds);
        } catch (final SQLException e) {
            handleException(e);
            return false;
        }
    }

    @Override
    public boolean isWrapperFor(final Class<?> iface) throws SQLException {
        if (iface.isAssignableFrom(getClass())) {
            return true;
        } else if (iface.isAssignableFrom(connection.getClass())) {
            return true;
        } else {
            return connection.isWrapperFor(iface);
        }
    }

    @Override
    public String nativeSQL(final String sql) throws SQLException {
        return apply(connection::nativeSQL, sql);
    }

    protected void passivate() throws SQLException {
        // The JDBC specification requires that a Connection close any open
        // Statement's when it is closed.
        // DBCP-288. Not all the traced objects will be statements
        final List<AbandonedTrace> traces = getTrace();
        if (traces != null && !traces.isEmpty()) {
            final List<Exception> thrownList = new ArrayList<>();
            final Iterator<AbandonedTrace> traceIter = traces.iterator();
            while (traceIter.hasNext()) {
                final Object trace = traceIter.next();
                if (trace instanceof Statement) {
                    try {
                        ((Statement) trace).close();
                    } catch (final Exception e) {
                        thrownList.add(e);
                    }
                } else if (trace instanceof ResultSet) {
                    // DBCP-265: Need to close the result sets that are
                    // generated via DatabaseMetaData
                    try {
                        ((ResultSet) trace).close();
                    } catch (final Exception e) {
                        thrownList.add(e);
                    }
                }
            }
            clearTrace();
            if (!thrownList.isEmpty()) {
                throw new SQLExceptionList(thrownList);
            }
        }
        setLastUsed(0);
    }

    @SuppressWarnings("resource")
    @Override
    public CallableStatement prepareCall(final String sql) throws SQLException {
        checkOpen();
        try {
            final DelegatingCallableStatement dcs = new DelegatingCallableStatement(this, connection.prepareCall(sql));
            return init(dcs);
        } catch (final SQLException e) {
            handleException(e);
            return null;
        }
    }

    @SuppressWarnings("resource")
    @Override
    public CallableStatement prepareCall(final String sql, final int resultSetType, final int resultSetConcurrency)
            throws SQLException {
        checkOpen();
        try {
            final DelegatingCallableStatement dcs = new DelegatingCallableStatement(this,
                    connection.prepareCall(sql, resultSetType, resultSetConcurrency));
            return init(dcs);
        } catch (final SQLException e) {
            handleException(e);
            return null;
        }
    }

    @Override
    public CallableStatement prepareCall(final String sql, final int resultSetType, final int resultSetConcurrency,
            final int resultSetHoldability) throws SQLException {
        return apply(() -> init(new DelegatingCallableStatement(this,
                connection.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability))));
    }

    @Override
    public PreparedStatement prepareStatement(final String sql) throws SQLException {
        return apply(() -> init(new DelegatingPreparedStatement(this, connection.prepareStatement(sql))));
    }

    @Override
    public PreparedStatement prepareStatement(final String sql, final int autoGeneratedKeys) throws SQLException {
        return apply(
                () -> init(new DelegatingPreparedStatement(this, connection.prepareStatement(sql, autoGeneratedKeys))));
    }

    @Override
    public PreparedStatement prepareStatement(final String sql, final int columnIndexes[]) throws SQLException {
        return apply(() -> init(new DelegatingPreparedStatement(this, connection.prepareStatement(sql, columnIndexes))));
    }

    @Override
    public PreparedStatement prepareStatement(final String sql, final int resultSetType, final int resultSetConcurrency)
            throws SQLException {
        return apply(() -> init(new DelegatingPreparedStatement(this,
                connection.prepareStatement(sql, resultSetType, resultSetConcurrency))));
    }

    @Override
    public PreparedStatement prepareStatement(final String sql, final int resultSetType, final int resultSetConcurrency,
            final int resultSetHoldability) throws SQLException {
        return apply(() -> init(new DelegatingPreparedStatement(this,
                connection.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability))));
    }

    @Override
    public PreparedStatement prepareStatement(final String sql, final String columnNames[]) throws SQLException {
        return apply(() -> init(new DelegatingPreparedStatement(this, connection.prepareStatement(sql, columnNames))));
    }

    @Override
    public void releaseSavepoint(final Savepoint savepoint) throws SQLException {
        accept(connection::releaseSavepoint, savepoint);
    }

    @Override
    public void rollback() throws SQLException {
        accept(connection::rollback);
    }

    @Override
    public void rollback(final Savepoint savepoint) throws SQLException {
        accept(connection::rollback, savepoint);
    }

    @Override
    public void setAutoCommit(final boolean autoCommit) throws SQLException {
        checkOpen();
        try {
            connection.setAutoCommit(autoCommit);
            if (cacheState) {
                autoCommitCached = Boolean.valueOf(autoCommit);
            }
        } catch (final SQLException e) {
            autoCommitCached = null;
            handleException(e);
        }
    }

    /**
     * Sets the state caching flag.
     *
     * @param cacheState The new value for the state caching flag
     */
    public void setCacheState(final boolean cacheState) {
        this.cacheState = cacheState;
    }

    @Override
    public void setCatalog(final String catalog) throws SQLException {
        accept(connection::setCatalog, catalog);
    }

    @Override
    public void setClientInfo(final Properties properties) throws SQLClientInfoException {
        try {
            checkOpen();
            connection.setClientInfo(properties);
        } catch (final SQLClientInfoException e) {
            throw e;
        } catch (final SQLException e) {
            throw new SQLClientInfoException("Connection is closed.", EMPTY_FAILED_PROPERTIES, e);
        }
    }

    @Override
    public void setClientInfo(final String name, final String value) throws SQLClientInfoException {
        try {
            checkOpen();
            connection.setClientInfo(name, value);
        } catch (final SQLClientInfoException e) {
            throw e;
        } catch (final SQLException e) {
            throw new SQLClientInfoException("Connection is closed.", EMPTY_FAILED_PROPERTIES, e);
        }
    }

    protected void setClosedInternal(final boolean closed) {
        this.closed = closed;
    }

    /**
     * Sets the default query timeout that will be used for {@link Statement}s created from this connection.
     * <code>null</code> means that the driver default will be used.
     *
     * @param defaultQueryTimeoutSeconds the new query timeout limit in seconds; zero means there is no limit
     */
    public void setDefaultQueryTimeout(final Integer defaultQueryTimeoutSeconds) {
        this.defaultQueryTimeoutSeconds = defaultQueryTimeoutSeconds;
    }

    /**
     * Sets my delegate.
     *
     * @param connection my delegate.
     */
    public void setDelegate(final C connection) {
        this.connection = connection;
    }

    @Override
    public void setHoldability(final int holdability) throws SQLException {
        acceptInt(connection::setHoldability, holdability);
    }

    @Override
    public void setNetworkTimeout(final Executor executor, final int milliseconds) throws SQLException {
        accept(Jdbc41Bridge::setNetworkTimeout, connection, executor, milliseconds);
    }

    @Override
    public void setReadOnly(final boolean readOnly) throws SQLException {
        checkOpen();
        try {
            connection.setReadOnly(readOnly);
            if (cacheState) {
                readOnlyCached = Boolean.valueOf(readOnly);
            }
        } catch (final SQLException e) {
            readOnlyCached = null;
            handleException(e);
        }
    }

    @Override
    public Savepoint setSavepoint() throws SQLException {
        return apply(connection::setSavepoint);
    }

    @Override
    public Savepoint setSavepoint(final String name) throws SQLException {
        return apply(connection::setSavepoint, name);
    }

    @Override
    public void setSchema(final String schema) throws SQLException {
        accept(connection::setSchema, schema);
    }

    @Override
    public void setTransactionIsolation(final int level) throws SQLException {
        acceptInt(connection::setTransactionIsolation, level);

    }

    @Override
    public void setTypeMap(final Map<String, Class<?>> map) throws SQLException {
        accept(connection::setTypeMap, map);
    }

    /**
     * Returns a string representation of the metadata associated with the innermost delegate connection.
     */
    @SuppressWarnings("resource")
    @Override
    public synchronized String toString() {
        String str = null;

        final Connection conn = this.getInnermostDelegateInternal();
        if (conn != null) {
            try {
                if (conn.isClosed()) {
                    str = "connection is closed";
                } else {
                    final StringBuffer sb = new StringBuffer();
                    sb.append(hashCode());
                    final DatabaseMetaData meta = conn.getMetaData();
                    if (meta != null) {
                        sb.append(", URL=");
                        sb.append(meta.getURL());
                        sb.append(", UserName=");
                        sb.append(meta.getUserName());
                        sb.append(", ");
                        sb.append(meta.getDriverName());
                        str = sb.toString();
                    }
                }
            } catch (final SQLException ex) {
                // Ignore
            }
        }
        return str != null ? str : super.toString();
    }

    @Override
    public <T> T unwrap(final Class<T> iface) throws SQLException {
        if (iface.isAssignableFrom(getClass())) {
            return iface.cast(this);
        } else if (iface.isAssignableFrom(connection.getClass())) {
            return iface.cast(connection);
        } else {
            return connection.unwrap(iface);
        }
    }
}
