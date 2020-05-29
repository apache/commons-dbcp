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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * A base delegating implementation of {@link Statement}.
 * <p>
 * All of the methods from the {@link Statement} interface simply check to see that the {@link Statement} is active, and
 * call the corresponding method on the "delegate" provided in my constructor.
 * <p>
 * Extends AbandonedTrace to implement Statement tracking and logging of code which created the Statement. Tracking the
 * Statement ensures that the Connection which created it can close any open Statement's on Connection close.
 *
 * @since 2.0
 */
public class DelegatingStatement extends AbandonedTrace implements Statement {

    /** My delegate. */
    private Statement statement;

    /** The connection that created me. **/
    private DelegatingConnection<?> connection;

    private boolean closed = false;

    /**
     * Create a wrapper for the Statement which traces this Statement to the Connection which created it and the code
     * which created it.
     *
     * @param statement the {@link Statement} to delegate all calls to.
     * @param connection the {@link DelegatingConnection} that created this statement.
     */
    public DelegatingStatement(final DelegatingConnection<?> connection, final Statement statement) {
        super(connection);
        this.statement = statement;
        this.connection = connection;
    }

    /**
     *
     * @throws SQLException thrown by the delegating statement.
     * @since 2.4.0 made public, was protected in 2.3.0.
     */
    public void activate() throws SQLException {
        if (statement instanceof DelegatingStatement) {
            ((DelegatingStatement) statement).activate();
        }
    }

    @Override
    public void addBatch(final String sql) throws SQLException {
        accept(Statement::addBatch, statement, sql);
    }

    @Override
    public void cancel() throws SQLException {
        accept(Statement::cancel, statement);
    }

    @Override
    protected void checkOpen() throws SQLException {
        if (isClosed()) {
            throw new SQLException(this.getClass().getName() + " with address: \"" + this.toString() + "\" is closed.");
        }
    }

    @Override
    public void clearBatch() throws SQLException {
        accept(Statement::clearBatch, statement);
    }

    @Override
    public void clearWarnings() throws SQLException {
        accept(Statement::clearWarnings, statement);
    }

    /**
     * Close this DelegatingStatement, and close any ResultSets that were not explicitly closed.
     */
    @Override
    public void close() throws SQLException {
        if (isClosed()) {
            return;
        }
        final List<Exception> thrownList = new ArrayList<>();
        try {
            if (connection != null) {
                connection.removeTrace(this);
                connection = null;
            }

            // The JDBC spec requires that a statement close any open
            // ResultSet's when it is closed.
            // FIXME The PreparedStatement we're wrapping should handle this for us.
            // See bug 17301 for what could happen when ResultSets are closed twice.
            final List<AbandonedTrace> resultSetList = getTrace();
            if (resultSetList != null) {
                final int size = resultSetList.size();
                final ResultSet[] resultSets = resultSetList.toArray(new ResultSet[size]);
                for (final ResultSet resultSet : resultSets) {
                    if (resultSet != null) {
                        try {
                            resultSet.close();
                        } catch (final Exception e) {
                            if (connection != null) {
                                // Does not rethrow e.
                                connection.handleExceptionNoThrow(e);
                            }
                            thrownList.add(e);
                        }
                    }
                }
                clearTrace();
            }
            if (statement != null) {
                try {
                    statement.close();
                } catch (final Exception e) {
                    if (connection != null) {
                        // Does not rethrow e.
                        connection.handleExceptionNoThrow(e);
                    }
                    thrownList.add(e);
                }
            }
        } finally {
            closed = true;
            statement = null;
            if (!thrownList.isEmpty()) {
                throw new SQLExceptionList(thrownList);
            }
        }
    }

    @Override
    public void closeOnCompletion() throws SQLException {
        accept(Jdbc41Bridge::closeOnCompletion, statement);
    }

    @Override
    public boolean execute(final String sql) throws SQLException {
        return applyTo(Statement::execute, statement, sql, false);
    }

    @Override
    public boolean execute(final String sql, final int autoGeneratedKeys) throws SQLException {
        return applyTo(Statement::execute, statement, sql, autoGeneratedKeys, false);
    }

    @Override
    public boolean execute(final String sql, final int columnIndexes[]) throws SQLException {
        return applyTo(Statement::execute, statement, sql, columnIndexes, false);
    }

    @Override
    public boolean execute(final String sql, final String columnNames[]) throws SQLException {
        return applyTo(Statement::execute, statement, sql, columnNames, false);
    }

    @Override
    public int[] executeBatch() throws SQLException {
        return apply(Statement::executeBatch, statement);
    }

    /**
     * @since 2.5.0
     */
    @Override
    public long[] executeLargeBatch() throws SQLException {
        return apply(Statement::executeLargeBatch, statement);
    }

    /**
     * @since 2.5.0
     */
    @Override
    public long executeLargeUpdate(final String sql) throws SQLException {
        return applyTo(Statement::executeLargeUpdate, statement,  sql, 0L);
    }

    /**
     * @since 2.5.0
     */
    @Override
    public long executeLargeUpdate(final String sql, final int autoGeneratedKeys) throws SQLException {
        return applyTo(Statement::executeLargeUpdate, statement, sql, autoGeneratedKeys, 0L);
    }

    /**
     * @since 2.5.0
     */
    @Override
    public long executeLargeUpdate(final String sql, final int[] columnIndexes) throws SQLException {
        return applyTo(Statement::executeLargeUpdate, statement, sql, columnIndexes, 0L);
    }

    /**
     * @since 2.5.0
     */
    @Override
    public long executeLargeUpdate(final String sql, final String[] columnNames) throws SQLException {
        return applyTo(Statement::executeLargeUpdate, statement, sql, columnNames, 0L);
    }

    @Override
    public ResultSet executeQuery(final String sql) throws SQLException {
        return apply(() -> DelegatingResultSet.wrapResultSet(this, statement.executeQuery(sql)));
    }

    @Override
    public int executeUpdate(final String sql) throws SQLException {
        return applyTo(Statement::executeUpdate, statement, sql, 0);
    }

    @Override
    public int executeUpdate(final String sql, final int autoGeneratedKeys) throws SQLException {
        return applyTo(Statement::executeUpdate, statement, sql, autoGeneratedKeys, 0);
    }

    @Override
    public int executeUpdate(final String sql, final int columnIndexes[]) throws SQLException {
        return applyTo(Statement::executeUpdate, statement, sql, columnIndexes, 0);
    }

    @Override
    public int executeUpdate(final String sql, final String columnNames[]) throws SQLException {
        return applyTo(Statement::executeUpdate, statement, sql, columnNames, 0);
    }

    @Override
    protected void finalize() throws Throwable {
        // This is required because of statement pooling. The poolable
        // statements will always be strongly held by the statement pool. If the
        // delegating statements that wrap the poolable statement are not
        // strongly held they will be garbage collected but at that point the
        // poolable statements need to be returned to the pool else there will
        // be a leak of statements from the pool. Closing this statement will
        // close all the wrapped statements and return any poolable statements
        // to the pool.
        close();
        super.finalize();
    }

    @Override
    public Connection getConnection() throws SQLException {
        checkOpen();
        return getConnectionInternal(); // return the delegating connection that created this
    }

    protected DelegatingConnection<?> getConnectionInternal() {
        return connection;
    }

    /**
     * Returns my underlying {@link Statement}.
     *
     * @return my underlying {@link Statement}.
     * @see #getInnermostDelegate
     */
    public Statement getDelegate() {
        return statement;
    }

    @Override
    public int getFetchDirection() throws SQLException {
        return applyTo(Statement::getFetchDirection, statement, 0);
    }

    @Override
    public int getFetchSize() throws SQLException {
        return applyTo(Statement::getFetchSize, statement, 0);
    }

    @Override
    public ResultSet getGeneratedKeys() throws SQLException {
        return apply(() -> DelegatingResultSet.wrapResultSet(this, statement.getGeneratedKeys()));
    }

    /**
     * If my underlying {@link Statement} is not a {@code DelegatingStatement}, returns it, otherwise recursively
     * invokes this method on my delegate.
     * <p>
     * Hence this method will return the first delegate that is not a {@code DelegatingStatement} or {@code null} when
     * no non-{@code DelegatingStatement} delegate can be found by traversing this chain.
     * </p>
     * <p>
     * This method is useful when you may have nested {@code DelegatingStatement}s, and you want to make sure to obtain
     * a "genuine" {@link Statement}.
     * </p>
     *
     * @return The innermost delegate.
     *
     * @see #getDelegate
     */
    @SuppressWarnings("resource")
    public Statement getInnermostDelegate() {
        Statement s = statement;
        while (s != null && s instanceof DelegatingStatement) {
            s = ((DelegatingStatement) s).getDelegate();
            if (this == s) {
                return null;
            }
        }
        return s;
    }

    /**
     * @since 2.5.0
     */
    @Override
    public long getLargeMaxRows() throws SQLException {
        return applyTo(Statement::getLargeMaxRows, statement, 0L);
    }

    /**
     * @since 2.5.0
     */
    @Override
    public long getLargeUpdateCount() throws SQLException {
        return applyTo(Statement::getLargeUpdateCount, statement, 0L);
    }

    @Override
    public int getMaxFieldSize() throws SQLException {
        return applyTo(Statement::getMaxFieldSize, statement, 0);
    }

    @Override
    public int getMaxRows() throws SQLException {
        return applyTo(Statement::getMaxRows, statement, 0);
    }

    @Override
    public boolean getMoreResults() throws SQLException {
        return applyTo(Statement::getMoreResults, statement, false);
    }

    @Override
    public boolean getMoreResults(final int current) throws SQLException {
        return applyTo(Statement::getMoreResults, statement, current, false);
    }

    @Override
    public int getQueryTimeout() throws SQLException {
        return applyTo(Statement::getQueryTimeout, statement, 0);
    }

    @Override
    public ResultSet getResultSet() throws SQLException {
        return apply(() -> DelegatingResultSet.wrapResultSet(this, statement.getResultSet()));
    }

    @Override
    public int getResultSetConcurrency() throws SQLException {
        return applyTo(Statement::getResultSetConcurrency, statement, 0);
    }

    @Override
    public int getResultSetHoldability() throws SQLException {
        return applyTo(Statement::getResultSetHoldability, statement, 0);
    }

    @Override
    public int getResultSetType() throws SQLException {
        return applyTo(Statement::getResultSetType, statement, 0);
    }

    @Override
    public int getUpdateCount() throws SQLException {
        return applyTo(Statement::getUpdateCount, statement, 0);
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return apply(Statement::getWarnings, statement);
    }

    @Override
    protected void handleException(final SQLException e) throws SQLException {
        if (connection != null) {
            connection.handleException(e);
        } else {
            throw e;
        }
    }

    /*
     * Note: This method was protected prior to JDBC 4.
     */
    @Override
    public boolean isClosed() throws SQLException {
        return closed;
    }

    protected boolean isClosedInternal() {
        return closed;
    }

    @Override
    public boolean isCloseOnCompletion() throws SQLException {
        return apply(Jdbc41Bridge::isCloseOnCompletion, statement);
    }

    @Override
    public boolean isPoolable() throws SQLException {
        return applyTo(Statement::isPoolable, statement, false);
    }

    @Override
    public boolean isWrapperFor(final Class<?> iface) throws SQLException {
        if (iface.isAssignableFrom(getClass())) {
            return true;
        } else if (iface.isAssignableFrom(statement.getClass())) {
            return true;
        } else {
            return statement.isWrapperFor(iface);
        }
    }

    /**
     *
     * @throws SQLException thrown by the delegating statement.
     * @since 2.4.0 made public, was protected in 2.3.0.
     */
    public void passivate() throws SQLException {
        if (statement instanceof DelegatingStatement) {
            ((DelegatingStatement) statement).passivate();
        }
    }

    protected void setClosedInternal(final boolean closed) {
        this.closed = closed;
    }

    @Override
    public void setCursorName(final String name) throws SQLException {
        accept(Statement::setCursorName, statement, name);
    }

    /**
     * Sets my delegate.
     *
     * @param statement my delegate.
     */
    public void setDelegate(final Statement statement) {
        this.statement = statement;
    }

    @Override
    public void setEscapeProcessing(final boolean enable) throws SQLException {
        accept(Statement::setEscapeProcessing, statement, enable);
    }

    @Override
    public void setFetchDirection(final int direction) throws SQLException {
        accept(Statement::setFetchDirection, statement, direction);
    }

    @Override
    public void setFetchSize(final int rows) throws SQLException {
        accept(Statement::setFetchSize, statement, rows);
    }

    /**
     * @since 2.5.0
     */
    @Override
    public void setLargeMaxRows(final long max) throws SQLException {
        accept(Statement::setLargeMaxRows, statement, max);
    }

    @Override
    protected void markUse() {
        if (connection != null) {
            connection.setLastUsed();
        }
    }

    @Override
    public void setMaxFieldSize(final int max) throws SQLException {
        accept(Statement::setMaxFieldSize, statement, max);
    }

    @Override
    public void setMaxRows(final int max) throws SQLException {
        accept(Statement::setMaxRows, statement, max);
    }

    @Override
    public void setPoolable(final boolean poolable) throws SQLException {
        accept(Statement::setPoolable, statement, poolable);
    }

    @Override
    public void setQueryTimeout(final int seconds) throws SQLException {
        accept(Statement::setQueryTimeout, statement, seconds);
    }

    /**
     * Returns a String representation of this object.
     *
     * @return String
     */
    @Override
    public synchronized String toString() {
        return statement == null ? "NULL" : statement.toString();
    }

    @Override
    public <T> T unwrap(final Class<T> iface) throws SQLException {
        if (iface.isAssignableFrom(getClass())) {
            return iface.cast(this);
        } else if (iface.isAssignableFrom(statement.getClass())) {
            return iface.cast(statement);
        } else {
            return statement.unwrap(iface);
        }
    }
}
