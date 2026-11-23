/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.dbcp2;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.function.Supplier;
import java.util.logging.Logger;

import org.apache.commons.pool2.ObjectPool;

/**
 * A {@link Driver} implementation that obtains {@link Connection}s from a registered {@link ObjectPool}.
 *
 * @since 2.0
 */
public class PoolingDriver implements Driver {

    /**
     * Builds {@link PoolingDriver} instances.
     *
     * <p>
     * Access to underlying connections is initialized from {@link #ACCESS_TO_UNDERLYING_CONNECTION_ALLOWED_SYSTEM_PROPERTY}
     * when the builder is constructed and is enabled by default, matching {@link PoolingDriver#PoolingDriver()}.
     * </p>
     *
     * @since 2.15.0
     */
    public static class Builder implements Supplier<PoolingDriver> {

        private static boolean getDefaultAccessToUnderlyingConnectionAllowed() {
            final boolean defaultValue = true;
            try {
                return Boolean.parseBoolean(System.getProperty(ACCESS_TO_UNDERLYING_CONNECTION_ALLOWED_SYSTEM_PROPERTY, Boolean.toString(defaultValue)));
            } catch (final SecurityException ignored) {
                return defaultValue;
            }
        }

        private boolean accessToUnderlyingConnectionAllowed = getDefaultAccessToUnderlyingConnectionAllowed();

        /**
         * Constructs a new builder using the current system property value.
         */
        public Builder() {
            // empty
        }

        /**
         * Gets a new driver with this builder's current configuration.
         *
         * <p>
         * This method does not register the returned driver with {@link DriverManager} or replace the driver registered by
         * {@link PoolingDriver}'s static initializer. Changes to this builder do not affect drivers already created by it.
         * </p>
         *
         * @return a new driver.
         */
        @Override
        public PoolingDriver get() {
            return new PoolingDriver(this);
        }

        /**
         * Sets whether connections and their statements, result sets, and database metadata permit access to delegates.
         *
         * <p>
         * This setting overrides the value read from {@link #ACCESS_TO_UNDERLYING_CONNECTION_ALLOWED_SYSTEM_PROPERTY} when this builder was
         * constructed. When {@code false}, delegate getters return {@code null} and JDBC unwrapping is limited to interfaces implemented by
         * the wrapper itself.
         * </p>
         *
         * @param accessToUnderlyingConnectionAllowed whether to allow access to delegates.
         * @return this builder.
         */
        public Builder setAccessToUnderlyingConnectionAllowed(final boolean accessToUnderlyingConnectionAllowed) {
            this.accessToUnderlyingConnectionAllowed = accessToUnderlyingConnectionAllowed;
            return this;
        }
    }

    /**
     * PoolGuardConnectionWrapper is a Connection wrapper that makes sure a closed connection cannot be used anymore.
     *
     * @since 2.0
     */
    private final class PoolGuardConnectionWrapper extends DelegatingConnection<Connection> {

        private final ObjectPool<? extends Connection> pool;

        PoolGuardConnectionWrapper(final ObjectPool<? extends Connection> pool, final Connection delegate) {
            super(delegate);
            this.pool = pool;
        }

        /**
         * Gets the delegate connection if access is allowed.
         *
         * @see org.apache.commons.dbcp2.DelegatingConnection#getDelegate()
         */
        @Override
        public Connection getDelegate() {
            if (isAccessToUnderlyingConnectionAllowed()) {
                return super.getDelegate();
            }
            return null;
        }

        /**
         * Gets the innermost delegate connection if access is allowed.
         *
         * @see org.apache.commons.dbcp2.DelegatingConnection#getInnermostDelegate()
         */
        @Override
        public Connection getInnermostDelegate() {
            if (isAccessToUnderlyingConnectionAllowed()) {
                return super.getInnermostDelegate();
            }
            return null;
        }

        @Override
        protected boolean isAccessToUnderlyingConnectionAllowed() {
            return PoolingDriver.this.isAccessToUnderlyingConnectionAllowed();
        }
    }

    /**
     * The system property used to initialize access to underlying connections: {@value}.
     *
     * <p>
     * Each new {@link Builder} reads this property. If it is absent or cannot be read because of a {@link SecurityException}, access is enabled. Otherwise,
     * access is enabled only when the value equals {@code "true"}, ignoring case. Existing builders and drivers are unaffected by later property changes. An
     * explicit call to {@link Builder#setAccessToUnderlyingConnectionAllowed(boolean)} overrides the property value.
     * </p>
     *
     * <p>
     * The no-argument constructor uses a new builder. To configure the driver registered by this class's static initializer, set the property before this class
     * is initialized, for example on the JVM command line.
     * </p>
     *
     * @since 2.15.0
     */
    public static final String ACCESS_TO_UNDERLYING_CONNECTION_ALLOWED_SYSTEM_PROPERTY = "org.apache.commons.dbcp2.PoolingDriver.accessToUnderlyingConnectionAllowed";

    private static final DriverPropertyInfo[] EMPTY_DRIVER_PROPERTY_INFO_ARRAY = {};

    /* Register myself with the {@link DriverManager}. */
    static {
        try {
            DriverManager.registerDriver(new PoolingDriver());
        } catch (final Exception ignored) {
            // Ignored
        }
    }

    /** The map of registered pools. */
    protected static final HashMap<String, ObjectPool<? extends Connection>> pools = new HashMap<>();

    /**
     * The Apache Commons connection string prefix {@value}.
     */
    public static final String URL_PREFIX = "jdbc:apache:commons:dbcp:";

    /**
     * The String length of {@link #URL_PREFIX}.
     */
    protected static final int URL_PREFIX_LEN = URL_PREFIX.length();

    /**
     * Major version number.
     */
    protected static final int MAJOR_VERSION = 1;

    /**
     * Minor version number.
     */
    protected static final int MINOR_VERSION = 0;

    /**
     * Creates a new builder.
     *
     * @return a new builder.
     * @since 2.15.0
     */
    public static Builder builder() {
        return new Builder();
    }

    /** Controls access to the underlying connection */
    private final boolean accessToUnderlyingConnectionAllowed;

    /**
     * Constructs a new driver using a new builder's configuration.
     *
     * <p>
     * Access to underlying connections is initialized from {@link #ACCESS_TO_UNDERLYING_CONNECTION_ALLOWED_SYSTEM_PROPERTY} and is enabled by
     * default. When enabled, this permits delegate getters and JDBC unwrapping on connections, statements, result sets, and database metadata.
     * </p>
     */
    public PoolingDriver() {
        this(builder());
    }

    /**
     * For unit testing purposes.
     *
     * @param accessToUnderlyingConnectionAllowed
     *            Whether connections and their statements, result sets, and database metadata permit access to delegates. When false,
     *            delegate getters return {@code null} and JDBC unwrapping is limited to interfaces implemented by the wrapper itself.
     */
    protected PoolingDriver(final boolean accessToUnderlyingConnectionAllowed) {
        this.accessToUnderlyingConnectionAllowed = accessToUnderlyingConnectionAllowed;
    }

    /**
     * Constructs a new driver from a builder.
     *
     * @param builder the builder.
     */
    private PoolingDriver(final Builder builder) {
        this(builder.accessToUnderlyingConnectionAllowed);
    }

    @Override
    public boolean acceptsURL(final String url) throws SQLException {
        return url != null && url.startsWith(URL_PREFIX);
    }

    /**
     * Closes a named pool.
     *
     * @param name
     *            The pool name.
     * @throws SQLException Thrown when a problem is caught closing the pool.
     */
    public synchronized void closePool(final String name) throws SQLException {
        @SuppressWarnings("resource")
        final ObjectPool<? extends Connection> pool = pools.get(name);
        if (pool != null) {
            pools.remove(name);
            try {
                pool.close();
            } catch (final Exception e) {
                throw new SQLException("Error closing pool " + name, e);
            }
        }
    }

    @Override
    public Connection connect(final String url, final Properties info) throws SQLException {
        if (acceptsURL(url)) {
            final ObjectPool<? extends Connection> pool = getConnectionPool(url.substring(URL_PREFIX_LEN));
            try {
                final Connection conn = pool.borrowObject();
                if (conn == null) {
                    return null;
                }
                return new PoolGuardConnectionWrapper(pool, conn);
            } catch (final NoSuchElementException e) {
                throw new SQLException("Cannot get a connection, pool error: " + e.getMessage(), e);
            } catch (final SQLException | RuntimeException e) {
                throw e;
            } catch (final Exception e) {
                throw new SQLException("Cannot get a connection, general error: " + e.getMessage(), e);
            }
        }
        return null;
    }

    /**
     * Gets the connection pool for the given name.
     *
     * @param name
     *            The pool name
     * @return The pool
     * @throws SQLException Thrown when the named pool is not registered.
     */
    public synchronized ObjectPool<? extends Connection> getConnectionPool(final String name) throws SQLException {
        final ObjectPool<? extends Connection> pool = pools.get(name);
        if (pool == null) {
            throw new SQLException("Pool not registered: " + name);
        }
        return pool;
    }

    @Override
    public int getMajorVersion() {
        return MAJOR_VERSION;
    }

    @Override
    public int getMinorVersion() {
        return MINOR_VERSION;
    }

    /**
     * Always throws {@link SQLFeatureNotSupportedException} because this operation is not supported.
     *
     * @throws SQLFeatureNotSupportedException Thrown always.
     */
    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * Gets the pool names.
     *
     * @return The pool names.
     */
    public synchronized String[] getPoolNames() {
        return pools.keySet().toArray(Utils.EMPTY_STRING_ARRAY);
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(final String url, final Properties info) {
        return EMPTY_DRIVER_PROPERTY_INFO_ARRAY;
    }

    /**
     * Invalidates the given connection.
     *
     * @param conn
     *            connection to invalidate
     * @throws SQLException Thrown if the connection is not a {@code PoolGuardConnectionWrapper} or an error occurs invalidating the connection.
     */
    public void invalidateConnection(final Connection conn) throws SQLException {
        if (!(conn instanceof PoolGuardConnectionWrapper)) {
            throw new SQLException("Invalid connection class");
        }
        final PoolGuardConnectionWrapper pgconn = (PoolGuardConnectionWrapper) conn;
        @SuppressWarnings("unchecked")
        final ObjectPool<Connection> pool = (ObjectPool<Connection>) pgconn.pool;
        try {
            pool.invalidateObject(pgconn.getDelegateInternal());
        } catch (final Exception ignored) {
            // Ignored.
        }
    }

    /**
     * Tests whether access to the underlying connection is allowed.
     *
     * @return true if access to the underlying is allowed, false otherwise.
     */
    protected boolean isAccessToUnderlyingConnectionAllowed() {
        return accessToUnderlyingConnectionAllowed;
    }

    @Override
    public boolean jdbcCompliant() {
        return true;
    }

    /**
     * Registers a named pool.
     *
     * @param name
     *            The pool name.
     * @param pool
     *            The pool.
     */
    public synchronized void registerPool(final String name, final ObjectPool<? extends Connection> pool) {
        pools.put(name, pool);
    }
}
