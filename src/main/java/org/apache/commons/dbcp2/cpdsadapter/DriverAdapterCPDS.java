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
package org.apache.commons.dbcp2.cpdsadapter;

import java.io.PrintWriter;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.time.Duration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.naming.StringRefAddr;
import javax.naming.spi.ObjectFactory;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.DataSource;
import javax.sql.PooledConnection;

import org.apache.commons.dbcp2.Constants;
import org.apache.commons.dbcp2.DelegatingPreparedStatement;
import org.apache.commons.dbcp2.PStmtKey;
import org.apache.commons.dbcp2.Utils;
import org.apache.commons.pool2.KeyedObjectPool;
import org.apache.commons.pool2.impl.BaseObjectPoolConfig;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;

/**
 * <p>
 * An adapter for JDBC drivers that do not include an implementation of {@link javax.sql.ConnectionPoolDataSource}, but
 * still include a {@link java.sql.DriverManager} implementation. {@link ConnectionPoolDataSource}s are not used
 * within general applications. They are used by {@link DataSource} implementations that pool
 * {@link Connection}s, such as {@link org.apache.commons.dbcp2.datasources.SharedPoolDataSource}. A J2EE container
 * will normally provide some method of initializing the {@link ConnectionPoolDataSource} whose attributes are
 * presented as bean getters/setters and then deploying it via JNDI. It is then available as a source of physical
 * connections to the database, when the pooling {@link DataSource} needs to create a new physical connection.
 * </p>
 * <p>
 * Although normally used within a JNDI environment, the DriverAdapterCPDS can be instantiated and initialized as any
 * bean and then attached directly to a pooling {@link DataSource}. {@code Jdbc2PoolDataSource} can use the
 * {@link ConnectionPoolDataSource} with or without the use of JNDI.
 * </p>
 * <p>
 * The DriverAdapterCPDS also provides {@link PreparedStatement} pooling which is not generally available in jdbc2
 * {@link ConnectionPoolDataSource} implementation, but is addressed within the JDBC 3 specification. The
 * {@link PreparedStatement} pool in DriverAdapterCPDS has been in the DBCP package for some time, but it has not
 * undergone extensive testing in the configuration used here. It should be considered experimental and can be toggled
 * with the poolPreparedStatements attribute.
 * </p>
 * <p>
 * The <a href="package-summary.html">package documentation</a> contains an example using Apache Catalina and JNDI. The
 * <a href="../datasources/package-summary.html">datasources package documentation</a> shows how to use
 * {@code DriverAdapterCPDS} as a source for {@code Jdbc2PoolDataSource} without the use of JNDI.
 * </p>
 *
 * @since 2.0
 */
public class DriverAdapterCPDS implements ConnectionPoolDataSource, Referenceable, Serializable, ObjectFactory {

    private static final Pattern NEGATIVE_ZERO_SECONDS_PATTERN = Pattern.compile("-0+([.,][0-9]{0,9})S$", Pattern.CASE_INSENSITIVE);

    private static final String KEY_MIN_EVICTABLE_IDLE_DURATION = "minEvictableIdleDuration";

    private static final String KEY_DURATION_BETWEEN_EVICTION_RUNS = "durationBetweenEvictionRuns";

    private static final String KEY_LOGIN_TIMEOUT = "loginTimeout";

    private static final String KEY_URL = "url";

    private static final String KEY_DRIVER = "driver";

    private static final String KEY_DESCRIPTION = "description";

    private static final String KEY_EXPORT_PASSWORD_AS_REF_ADDR = "exportPasswordAsRefAddr";

    private static final String KEY_ACCESS_TO_UNDERLYING_CONNECTION_ALLOWED = "accessToUnderlyingConnectionAllowed";

    private static final String KEY_MAX_PREPARED_STATEMENTS = "maxPreparedStatements";

    private static final String KEY_MIN_EVICTABLE_IDLE_TIME_MILLIS = "minEvictableIdleTimeMillis";

    private static final String KEY_NUM_TESTS_PER_EVICTION_RUN = "numTestsPerEvictionRun";

    private static final String KEY_TIME_BETWEEN_EVICTION_RUNS_MILLIS = "timeBetweenEvictionRunsMillis";

    private static final String KEY_MAX_IDLE = "maxIdle";

    private static final String KEY_POOL_PREPARED_STATEMENTS = "poolPreparedStatements";

    private static final long serialVersionUID = -4820523787212147844L;

    private static final String GET_CONNECTION_CALLED = "A PooledConnection was already requested from this source, further initialization is not allowed.";

    static {
        // Attempt to prevent deadlocks - see DBCP-272
        DriverManager.getDrivers(); // NOPMD
    }

    /** Description */
    private String description;

    /** Connection string */
    private String connectionString;

    /** User name */
    private String userName;

    /** User password */
    private char[] userPassword;

    /** Driver class name */
    private String driver;

    /** Login TimeOut in seconds */
    private volatile int loginTimeout;

    /** Log stream. NOT USED */
    private transient PrintWriter logWriter;

    /** PreparedStatement pool property defaults to false. */
    private volatile boolean poolPreparedStatements;

    /** PreparedStatement pool property defaults to 10. */
    private volatile int maxIdle = 10;

    /** PreparedStatement pool property defaults to {@link BaseObjectPoolConfig#DEFAULT_DURATION_BETWEEN_EVICTION_RUNS}. */
    private Duration durationBetweenEvictionRuns = BaseObjectPoolConfig.DEFAULT_DURATION_BETWEEN_EVICTION_RUNS;

    /** PreparedStatement pool property defaults to -1. */
    private volatile int numTestsPerEvictionRun = -1;

    /** PreparedStatement pool property defaults to {@link BaseObjectPoolConfig#DEFAULT_MIN_EVICTABLE_IDLE_DURATION}. */
    private Duration minEvictableIdleDuration = BaseObjectPoolConfig.DEFAULT_MIN_EVICTABLE_IDLE_DURATION;

    /** Maximum number of prepared statements, defaults to -1, meaning no limit. */
    private volatile int maxPreparedStatements = -1;

    /** Whether or not getConnection has been called */
    private volatile boolean getConnectionCalled;

    /** Connection properties passed to JDBC Driver */
    private Properties connectionProperties;

    /**
     * Controls access to the underlying connection
     */
    private boolean accessToUnderlyingConnectionAllowed;

    /**
     * Omits the password from JNDI References when true. The negative form preserves password export when reading serialized instances
     * written before this field existed, for which the field defaults to false.
     */
    private volatile boolean omitPasswordFromReference;

    /**
     * Default no-argument constructor for Serialization
     */
    public DriverAdapterCPDS() {
    }

    /**
     * Adds a duration property and, when representable, its legacy millisecond property to a JNDI Reference.
     *
     * <p>
     * The legacy property is omitted when converting the duration to milliseconds would overflow a {@code long}.
     * </p>
     *
     * @param reference the Reference to update.
     * @param durationKey the address type for the ISO-8601 duration.
     * @param millisKey the address type for the legacy millisecond value.
     * @param duration the duration to export, not null.
     * @throws NullPointerException if reference or duration is null.
     */
    private void addDurationReference(final Reference reference, final String durationKey, final String millisKey, final Duration duration) {
        reference.add(new StringRefAddr(durationKey, duration.toString()));
        try {
            reference.add(new StringRefAddr(millisKey, Long.toString(duration.toMillis())));
        } catch (final ArithmeticException ignored) {
            // Only the Duration representation can preserve values outside the range of long milliseconds.
        }
    }

    /**
     * Throws an IllegalStateException, if a PooledConnection has already been requested.
     */
    private void assertInitializationAllowed() throws IllegalStateException {
        if (getConnectionCalled) {
            throw new IllegalStateException(GET_CONNECTION_CALLED);
        }
    }

    private boolean getBooleanContentString(final RefAddr ra) {
        return Boolean.parseBoolean(getStringContent(ra));
    }

    /**
     * Gets the configured connection properties.
     *
     * <p>
     * Connection requests apply their credentials to a shallow copy of these properties without changing the configured properties.
     * </p>
     *
     * @return The configured JDBC connection properties, or {@code null} if none are configured.
     */
    public Properties getConnectionProperties() {
        return connectionProperties;
    }

    /**
     * Gets the value of description. This property is here for use by the code which will deploy this data source. It
     * is not used internally.
     *
     * @return value of description, may be null.
     * @see #setDescription(String)
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the driver class name.
     *
     * @return value of driver.
     */
    public String getDriver() {
        return driver;
    }

    /**
     * Gets a duration from a JNDI Reference, preferring the ISO-8601 property over the legacy millisecond property.
     *
     * @param reference the Reference to read.
     * @param durationKey the address type for the ISO-8601 duration.
     * @param millisKey the address type for the legacy millisecond value.
     * @return the configured duration, or {@code null} if both properties are absent or empty.
     * @throws java.time.format.DateTimeParseException if the nonempty ISO-8601 property is invalid.
     * @throws NumberFormatException if the selected legacy property is not a valid long integer.
     */
    private Duration getDuration(final Reference reference, final String durationKey, final String millisKey) {
        final RefAddr duration = reference.get(durationKey);
        if (isNotEmpty(duration)) {
            return parseDuration(getStringContent(duration));
        }
        final RefAddr millis = reference.get(millisKey);
        return isNotEmpty(millis) ? Duration.ofMillis(Long.parseLong(getStringContent(millis))) : null;
    }

    /**
     * Gets the duration to sleep between runs of the idle object evictor thread. When non-positive, no
     * idle object evictor thread will be run.
     *
     * @return The value of the evictor thread timer
     * @see #setDurationBetweenEvictionRuns(Duration)
     * @since 2.9.0
     */
    public Duration getDurationBetweenEvictionRuns() {
        return durationBetweenEvictionRuns;
    }

    private int getIntegerStringContent(final RefAddr ra) {
        return Integer.parseInt(getStringContent(ra));
    }

    /**
     * Gets the maximum time in seconds that this data source can wait while attempting to connect to a database. NOT
     * USED.
     */
    @Override
    public int getLoginTimeout() {
        return loginTimeout;
    }

    /**
     * Gets the log writer for this data source. NOT USED.
     */
    @Override
    public PrintWriter getLogWriter() {
        return logWriter;
    }

    /**
     * Gets the maximum number of statements that can remain idle in the pool, without extra ones being released, or
     * negative for no limit.
     *
     * @return The value of maxIdle
     */
    public int getMaxIdle() {
        return maxIdle;
    }

    /**
     * Gets the maximum number of prepared statements.
     *
     * @return maxPrepartedStatements, defaults to -1, meaning no limit.
     */
    public int getMaxPreparedStatements() {
        return maxPreparedStatements;
    }

    /**
     * Gets the minimum amount of time a statement may sit idle in the pool before it is eligible for eviction by the
     * idle object evictor (if any).
     *
     * @see #setMinEvictableIdleDuration
     * @see #setDurationBetweenEvictionRuns
     * @return The minimum amount of time a statement may sit idle in the pool.
     * @since 2.9.0
     */
    public Duration getMinEvictableIdleDuration() {
        return minEvictableIdleDuration;
    }

    /**
     * Gets the minimum amount of time a statement may sit idle in the pool before it is eligible for eviction by the
     * idle object evictor (if any).
     *
     * @see #setMinEvictableIdleTimeMillis
     * @see #setTimeBetweenEvictionRunsMillis
     * @return The minimum amount of time a statement may sit idle in the pool.
     * @deprecated USe {@link #getMinEvictableIdleDuration()}.
     */
    @Deprecated
    public int getMinEvictableIdleTimeMillis() {
        return (int) minEvictableIdleDuration.toMillis();
    }

    /**
     * Gets the number of statements to examine during each run of the idle object evictor thread (if any.)
     *
     * @see #setNumTestsPerEvictionRun
     * @see #setTimeBetweenEvictionRunsMillis
     * @return The number of statements to examine during each run of the idle object evictor thread (if any.)
     */
    public int getNumTestsPerEvictionRun() {
        return numTestsPerEvictionRun;
    }

    /**
     * Implements {@link ObjectFactory} to create an instance of this class
     */
    @Override
    public Object getObjectInstance(final Object refObj, final Name name, final Context context, final Hashtable<?, ?> env) throws ClassNotFoundException {
        // The spec says to return null if we can't create an instance
        // of the reference
        DriverAdapterCPDS cpds = null;
        if (refObj instanceof Reference) {
            final Reference ref = (Reference) refObj;
            if (ref.getClassName().equals(getClass().getName())) {
                RefAddr ra = ref.get(KEY_DESCRIPTION);
                if (isNotEmpty(ra)) {
                    setDescription(getStringContent(ra));
                }

                ra = ref.get(KEY_DRIVER);
                if (isNotEmpty(ra)) {
                    setDriver(getStringContent(ra));
                }
                ra = ref.get(KEY_URL);
                if (isNotEmpty(ra)) {
                    setUrl(getStringContent(ra));
                }
                ra = ref.get(Constants.KEY_USER);
                if (isNotEmpty(ra)) {
                    setUser(getStringContent(ra));
                }
                ra = ref.get(Constants.KEY_PASSWORD);
                if (isNotEmpty(ra)) {
                    setPassword(getStringContent(ra));
                }

                ra = ref.get(KEY_EXPORT_PASSWORD_AS_REF_ADDR);
                if (isNotEmpty(ra)) {
                    setExportPasswordAsRefAddr(getBooleanContentString(ra));
                }

                ra = ref.get(KEY_POOL_PREPARED_STATEMENTS);
                if (isNotEmpty(ra)) {
                    setPoolPreparedStatements(getBooleanContentString(ra));
                }
                ra = ref.get(KEY_MAX_IDLE);
                if (isNotEmpty(ra)) {
                    setMaxIdle(getIntegerStringContent(ra));
                }

                final Duration evictionInterval = getDuration(ref, KEY_DURATION_BETWEEN_EVICTION_RUNS, KEY_TIME_BETWEEN_EVICTION_RUNS_MILLIS);
                if (evictionInterval != null) {
                    setDurationBetweenEvictionRuns(evictionInterval);
                }

                ra = ref.get(KEY_NUM_TESTS_PER_EVICTION_RUN);
                if (isNotEmpty(ra)) {
                    setNumTestsPerEvictionRun(getIntegerStringContent(ra));
                }

                final Duration idleDuration = getDuration(ref, KEY_MIN_EVICTABLE_IDLE_DURATION, KEY_MIN_EVICTABLE_IDLE_TIME_MILLIS);
                if (idleDuration != null) {
                    setMinEvictableIdleDuration(idleDuration);
                }

                ra = ref.get(KEY_MAX_PREPARED_STATEMENTS);
                if (isNotEmpty(ra)) {
                    setMaxPreparedStatements(getIntegerStringContent(ra));
                }

                ra = ref.get(KEY_ACCESS_TO_UNDERLYING_CONNECTION_ALLOWED);
                if (isNotEmpty(ra)) {
                    setAccessToUnderlyingConnectionAllowed(getBooleanContentString(ra));
                }

                cpds = this;
            }
        }
        return cpds;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * Gets the value of password for the default user.
     *
     * @return value of password.
     */
    public String getPassword() {
        return Utils.toString(userPassword);
    }

    /**
     * Gets the value of password for the default user.
     *
     * @return value of password.
     * @since 2.4.0
     */
    public char[] getPasswordCharArray() {
        return Utils.clone(userPassword);
    }

    /**
     * Attempts to establish a database connection using the default user and password.
     */
    @Override
    public PooledConnection getPooledConnection() throws SQLException {
        return getPooledConnection(getUser(), getPassword());
    }

    /**
     * Attempts to establish a database connection.
     *
     * @param pooledUserName name to be used for the connection
     * @param pooledUserPassword password to be used for the connection
     */
    @Override
    public PooledConnection getPooledConnection(final String pooledUserName, final String pooledUserPassword) throws SQLException {
        getConnectionCalled = true;
        final Properties configuredProperties = connectionProperties;
        final Properties properties = configuredProperties == null ? null : (Properties) configuredProperties.clone();
        if (properties != null) {
            update(properties, Constants.KEY_USER, pooledUserName);
            update(properties, Constants.KEY_PASSWORD, pooledUserPassword);
        }
        // Workaround for buggy WebLogic 5.1 class loader - ignore ClassCircularityError upon first invocation.
        PooledConnectionImpl pooledConnection = null;
        try {
            pooledConnection = getPooledConnectionImpl(pooledUserName, pooledUserPassword, properties);
        } catch (final ClassCircularityError e) {
            pooledConnection = getPooledConnectionImpl(pooledUserName, pooledUserPassword, properties);
        }
        if (isPoolPreparedStatements()) {
            final GenericKeyedObjectPoolConfig<DelegatingPreparedStatement> config = new GenericKeyedObjectPoolConfig<>();
            config.setMaxTotalPerKey(Integer.MAX_VALUE);
            config.setBlockWhenExhausted(false);
            config.setMaxWait(Duration.ZERO);
            config.setMaxIdlePerKey(getMaxIdle());
            if (getMaxPreparedStatements() <= 0) {
                // Since there is no limit, create a prepared statement pool with an eviction thread;
                // evictor settings are the same as the connection pool settings.
                config.setTimeBetweenEvictionRuns(getDurationBetweenEvictionRuns());
                config.setNumTestsPerEvictionRun(getNumTestsPerEvictionRun());
                config.setMinEvictableIdleDuration(getMinEvictableIdleDuration());
            } else {
                // Since there is a limit, create a prepared statement pool without an eviction thread;
                // pool has LRU functionality so when the limit is reached, 15% of the pool is cleared.
                // see org.apache.commons.pool2.impl.GenericKeyedObjectPool.clearOldest method
                config.setMaxTotal(getMaxPreparedStatements());
                config.setTimeBetweenEvictionRuns(Duration.ofMillis(-1));
                config.setNumTestsPerEvictionRun(0);
                config.setMinEvictableIdleDuration(Duration.ZERO);
            }
            @SuppressWarnings("resource") // PooledConnectionImpl closes
            final KeyedObjectPool<PStmtKey, DelegatingPreparedStatement> stmtPool = new GenericKeyedObjectPool<>(pooledConnection, config);
            pooledConnection.setStatementPool(stmtPool);
        }
        return pooledConnection;
    }

    @SuppressWarnings("resource") // Caller closes
    private PooledConnectionImpl getPooledConnectionImpl(final String pooledUserName, final String pooledUserPassword, final Properties properties)
            throws SQLException {
        PooledConnectionImpl pooledConnection;
        if (properties != null) {
            pooledConnection = new PooledConnectionImpl(DriverManager.getConnection(getUrl(), properties));
        } else {
            pooledConnection = new PooledConnectionImpl(DriverManager.getConnection(getUrl(), pooledUserName, pooledUserPassword));
        }
        pooledConnection.setAccessToUnderlyingConnectionAllowed(isAccessToUnderlyingConnectionAllowed());
        return pooledConnection;
    }

    /**
     * Gets a JNDI Reference containing this data source's connection configuration.
     *
     * <p>
     * By default, the Reference includes the default user's password as a cleartext {@link StringRefAddr}, preserving authentication
     * when a naming provider reconstructs this data source. Protect the Reference and any directory entries, logs, backups, or replication
     * streams that contain it as credentials. To omit the password address, call {@link #setExportPasswordAsRefAddr(boolean)} with
     * {@code false} before creating or binding the Reference. The export setting is itself included in the Reference and restored on lookup.
     * </p>
     *
     * <p>
     * This option only controls the dedicated {@code password} address. Other values, including credentials embedded in the JDBC URL,
     * are exported unchanged. It does not omit the configured password from Java serialization or remove passwords from previously created References.
     * </p>
     *
     * <p>
     * Duration properties include their ISO-8601 representations and, when representable as a long, legacy millisecond
     * representations. The ISO-8601 properties take precedence when this factory reconstructs the data source.
     * </p>
     *
     * @return a Reference describing this data source.
     * @throws NamingException if the Reference cannot be created.
     * @see #setExportPasswordAsRefAddr(boolean)
     */
    @Override
    public Reference getReference() throws NamingException {
        // this class implements its own factory
        final String factory = getClass().getName();

        final Reference ref = new Reference(getClass().getName(), factory, null);
        final boolean exportPassword = isExportPasswordAsRefAddr();

        ref.add(new StringRefAddr(KEY_DESCRIPTION, getDescription()));
        ref.add(new StringRefAddr(KEY_DRIVER, getDriver()));
        ref.add(new StringRefAddr(KEY_LOGIN_TIMEOUT, String.valueOf(getLoginTimeout())));
        if (exportPassword) {
            ref.add(new StringRefAddr(Constants.KEY_PASSWORD, getPassword()));
        }
        ref.add(new StringRefAddr(KEY_EXPORT_PASSWORD_AS_REF_ADDR, Boolean.toString(exportPassword)));
        ref.add(new StringRefAddr(Constants.KEY_USER, getUser()));
        ref.add(new StringRefAddr(KEY_URL, getUrl()));

        ref.add(new StringRefAddr(KEY_POOL_PREPARED_STATEMENTS, String.valueOf(isPoolPreparedStatements())));
        ref.add(new StringRefAddr(KEY_MAX_IDLE, String.valueOf(getMaxIdle())));
        ref.add(new StringRefAddr(KEY_NUM_TESTS_PER_EVICTION_RUN, String.valueOf(getNumTestsPerEvictionRun())));
        ref.add(new StringRefAddr(KEY_MAX_PREPARED_STATEMENTS, String.valueOf(getMaxPreparedStatements())));
        addDurationReference(ref, KEY_DURATION_BETWEEN_EVICTION_RUNS, KEY_TIME_BETWEEN_EVICTION_RUNS_MILLIS, getDurationBetweenEvictionRuns());
        addDurationReference(ref, KEY_MIN_EVICTABLE_IDLE_DURATION, KEY_MIN_EVICTABLE_IDLE_TIME_MILLIS, getMinEvictableIdleDuration());

        return ref;
    }

    private String getStringContent(final RefAddr ra) {
        return ra.getContent().toString();
    }

    /**
     * Gets the number of milliseconds to sleep between runs of the idle object evictor thread. When non-positive, no
     * idle object evictor thread will be run.
     *
     * @return The value of the evictor thread timer
     * @see #setDurationBetweenEvictionRuns(Duration)
     * @deprecated Use {@link #getDurationBetweenEvictionRuns()}.
     */
    @Deprecated
    public long getTimeBetweenEvictionRunsMillis() {
        return durationBetweenEvictionRuns.toMillis();
    }

    /**
     * Gets the value of connection string used to locate the database for this data source.
     *
     * @return value of connection string.
     */
    public String getUrl() {
        return connectionString;
    }

    /**
     * Gets the value of default user (login or user name).
     *
     * @return value of user.
     */
    public String getUser() {
        return userName;
    }

    /**
     * Returns the value of the accessToUnderlyingConnectionAllowed property.
     *
     * @return true if access to the underlying is allowed, false otherwise.
     */
    public synchronized boolean isAccessToUnderlyingConnectionAllowed() {
        return this.accessToUnderlyingConnectionAllowed;
    }

    /**
     * Gets whether {@link #getReference()} includes the default user's password address.
     *
     * @return true (the default) to export the password address, or false to omit it.
     * @since 2.15.0
     * @see #setExportPasswordAsRefAddr(boolean)
     */
    public boolean isExportPasswordAsRefAddr() {
        return !omitPasswordFromReference;
    }

    private boolean isNotEmpty(final RefAddr ra) {
        return ra != null && ra.getContent() != null;
    }

    /**
     * Tests whether to toggle the pooling of {@link PreparedStatement}s
     *
     * @return value of poolPreparedStatements.
     */
    public boolean isPoolPreparedStatements() {
        return poolPreparedStatements;
    }

    /**
     * Parses a duration without losing the sign of negative fractional seconds on Java 8.
     *
     * <p>
     * A negative-zero seconds component is temporarily changed to negative one before parsing. The result is adjusted by one second, respecting any leading
     * sign for the whole duration.
     * </p>
     *
     * @param text the duration text, not null.
     * @return the parsed duration.
     * @throws java.time.format.DateTimeParseException if the text is invalid or the duration exceeds the supported range.
     * @throws NullPointerException if text is null.
     */
    private Duration parseDuration(final String text) {
        final Matcher matcher = NEGATIVE_ZERO_SECONDS_PATTERN.matcher(text);
        if (matcher.find()) {
            // Java 8 loses the sign of a "-0" seconds component. Parse "-1" instead, then compensate
            // after applying any leading sign to the whole duration. This also handles mixed-sign components.
            final Duration duration = Duration.parse(matcher.replaceFirst("-1$1S"));
            return text.startsWith("-") ? duration.minusSeconds(1) : duration.plusSeconds(1);
        }
        return Duration.parse(text);
    }

    /**
     * Sets the value of the accessToUnderlyingConnectionAllowed property. It controls if the PoolGuard allows access to
     * the underlying connection. (Default: false)
     *
     * <p>
     * Since 2.15.0, this also controls delegate getters and JDBC unwrapping on connections, statements, result sets, and database metadata.
     * When disabled, delegate getters return {@code null}, {@code isWrapperFor} returns {@code false}, and {@code unwrap} throws
     * {@link SQLException} for types requiring access to the delegate. Interfaces implemented by the wrapper itself remain accessible.
     * Enable this property before creating connections if the application needs driver-specific extensions. This is a safeguard against
     * accidental misuse, not a security boundary against code running in the same JVM.
     * </p>
     *
     * @param allow Access to the underlying connection is granted when true.
     */
    public synchronized void setAccessToUnderlyingConnectionAllowed(final boolean allow) {
        this.accessToUnderlyingConnectionAllowed = allow;
    }

    /**
     * Sets the connection properties passed to the JDBC driver.
     *
     * <p>
     * If {@code props} contains "user" and/or "password" properties, including inherited defaults, the corresponding instance properties are set.
     * Calls to {@link #setUser(String)} or {@link #setPassword(String)} overwrite these entries in {@code props} if it is not {@code null}.
     * </p>
     *
     * <p>
     * Each connection request uses a shallow copy of the configured properties. The copy's "user" and "password" entries are overwritten using
     * {@link #getUser()} and {@link #getPassword()} for {@link #getPooledConnection()}, or the supplied arguments for
     * {@link #getPooledConnection(String, String)}. A {@code null} credential removes the corresponding explicit entry from the copy; inherited
     * {@link Properties} defaults remain in effect. Connection requests do not modify {@code props}.
     * </p>
     *
     * @param props Connection properties to use when creating new connections.
     * @throws IllegalStateException if {@link #getPooledConnection()} has been called
     */
    public void setConnectionProperties(final Properties props) {
        assertInitializationAllowed();
        connectionProperties = props;
        if (connectionProperties != null) {
            final String user = connectionProperties.getProperty(Constants.KEY_USER);
            if (user != null) {
                setUser(user);
            }
            final String password = connectionProperties.getProperty(Constants.KEY_PASSWORD);
            if (password != null) {
                setPassword(password);
            }
        }
    }

    /**
     * Sets the value of description. This property is here for use by the code which will deploy this datasource. It is
     * not used internally.
     *
     * @param description Value to assign to description.
     */
    public void setDescription(final String description) {
        this.description = description;
    }

    /**
     * Sets the driver class name. Setting the driver class name cause the driver to be registered with the
     * DriverManager.
     *
     * @param driver Value to assign to driver.
     * @throws IllegalStateException if {@link #getPooledConnection()} has been called
     * @throws ClassNotFoundException if the class cannot be located
     */
    public void setDriver(final String driver) throws ClassNotFoundException {
        assertInitializationAllowed();
        this.driver = driver;
        // make sure driver is registered
        Class.forName(driver);
    }

    /**
     * Sets the duration to sleep between runs of the idle object evictor thread. When non-positive, no
     * idle object evictor thread will be run.
     *
     * @param durationBetweenEvictionRuns The duration to sleep between runs of the idle object evictor
     *        thread. When non-positive, no idle object evictor thread will be run.
     * @see #getDurationBetweenEvictionRuns()
     * @throws IllegalStateException if {@link #getPooledConnection()} has been called
     * @since 2.9.0
     */
    public void setDurationBetweenEvictionRuns(final Duration durationBetweenEvictionRuns) {
        assertInitializationAllowed();
        this.durationBetweenEvictionRuns = durationBetweenEvictionRuns;
    }

    /**
     * Sets whether {@link #getReference()} includes the default user's password address.
     *
     * <p>
     * The default is {@code true} for compatibility with existing JNDI deployments. When {@code false}, a newly reconstructed data source
     * has no default password unless it is supplied separately. Before its first connection attempt, including an unsuccessful attempt,
     * provision the password with {@link #setPassword(String)} or {@link #setPassword(char[])}. Alternatively, supply credentials to
     * {@link #getPooledConnection(String, String)}. Containers that look up and immediately use the data source must provide credentials
     * through their own configuration or retain password export.
     * </p>
     *
     * <p>
     * This setting can be changed after connections have been requested and affects subsequent References only. It is retained through
     * JNDI lookup and Java serialization. It does not clear the configured password or redact other Reference addresses, such as the URL.
     * </p>
     *
     * @param exportPasswordAsRefAddr true to export the password address, or false to omit it.
     * @since 2.15.0
     */
    public void setExportPasswordAsRefAddr(final boolean exportPasswordAsRefAddr) {
        omitPasswordFromReference = !exportPasswordAsRefAddr;
    }

    /**
     * Sets the maximum time in seconds that this data source will wait while attempting to connect to a database. NOT
     * USED.
     */
    @Override
    public void setLoginTimeout(final int seconds) {
        this.loginTimeout = seconds;
    }

    /**
     * Sets the log writer for this data source. NOT USED.
     */
    @Override
    public void setLogWriter(final PrintWriter logWriter) {
        this.logWriter = logWriter;
    }

    /**
     * Gets the maximum number of statements that can remain idle in the pool, without extra ones being released, or
     * negative for no limit.
     *
     * @param maxIdle The maximum number of statements that can remain idle
     * @throws IllegalStateException if {@link #getPooledConnection()} has been called
     */
    public void setMaxIdle(final int maxIdle) {
        assertInitializationAllowed();
        this.maxIdle = maxIdle;
    }

    /**
     * Sets the maximum number of prepared statements.
     *
     * @param maxPreparedStatements The new maximum number of prepared statements, &lt;= 0 means no limit.
     */
    public void setMaxPreparedStatements(final int maxPreparedStatements) {
        this.maxPreparedStatements = maxPreparedStatements;
    }

    /**
     * Sets the minimum amount of time a statement may sit idle in the pool before it is eligible for eviction by the
     * idle object evictor (if any). When non-positive, no objects will be evicted from the pool due to idle time alone.
     *
     * @param minEvictableIdleDuration minimum time to set in milliseconds.
     * @see #getMinEvictableIdleDuration()
     * @see #setDurationBetweenEvictionRuns(Duration)
     * @throws IllegalStateException if {@link #getPooledConnection()} has been called.
     * @since 2.9.0
     */
    public void setMinEvictableIdleDuration(final Duration minEvictableIdleDuration) {
        assertInitializationAllowed();
        this.minEvictableIdleDuration = minEvictableIdleDuration;
    }

    /**
     * Sets the minimum amount of time a statement may sit idle in the pool before it is eligible for eviction by the
     * idle object evictor (if any). When non-positive, no objects will be evicted from the pool due to idle time alone.
     *
     * @param minEvictableIdleTimeMillis minimum time to set in milliseconds.
     * @see #getMinEvictableIdleDuration()
     * @see #setDurationBetweenEvictionRuns(Duration)
     * @throws IllegalStateException if {@link #getPooledConnection()} has been called
     * @deprecated Use {@link #setMinEvictableIdleDuration(Duration)}.
     */
    @Deprecated
    public void setMinEvictableIdleTimeMillis(final int minEvictableIdleTimeMillis) {
        assertInitializationAllowed();
        this.minEvictableIdleDuration = Duration.ofMillis(minEvictableIdleTimeMillis);
    }

    /**
     * Sets the number of statements to examine during each run of the idle object evictor thread (if any).
     * <p>
     * When a negative value is supplied,
     * {@code ceil({@link BasicDataSource#getNumIdle})/abs({@link #getNumTestsPerEvictionRun})} tests will be run.
     * I.e., when the value is <em>-n</em>, roughly one <em>n</em>th of the idle objects will be tested per run.
     * </p>
     *
     * @param numTestsPerEvictionRun number of statements to examine per run
     * @see #getNumTestsPerEvictionRun()
     * @see #setDurationBetweenEvictionRuns(Duration)
     * @throws IllegalStateException if {@link #getPooledConnection()} has been called
     */
    public void setNumTestsPerEvictionRun(final int numTestsPerEvictionRun) {
        assertInitializationAllowed();
        this.numTestsPerEvictionRun = numTestsPerEvictionRun;
    }

    /**
     * Sets the value of password for the default user.
     *
     * @param userPassword Value to assign to password.
     * @throws IllegalStateException if {@link #getPooledConnection()} has been called
     */
    public void setPassword(final char[] userPassword) {
        assertInitializationAllowed();
        this.userPassword = Utils.clone(userPassword);
        update(connectionProperties, Constants.KEY_PASSWORD, Utils.toString(this.userPassword));
    }

    /**
     * Sets the value of password for the default user.
     *
     * @param userPassword Value to assign to password.
     * @throws IllegalStateException if {@link #getPooledConnection()} has been called
     */
    public void setPassword(final String userPassword) {
        assertInitializationAllowed();
        this.userPassword = Utils.toCharArray(userPassword);
        update(connectionProperties, Constants.KEY_PASSWORD, userPassword);
    }

    /**
     * Sets whether to toggle the pooling of {@link PreparedStatement}s
     *
     * @param poolPreparedStatements true to pool statements.
     * @throws IllegalStateException if {@link #getPooledConnection()} has been called
     */
    public void setPoolPreparedStatements(final boolean poolPreparedStatements) {
        assertInitializationAllowed();
        this.poolPreparedStatements = poolPreparedStatements;
    }

    /**
     * Sets the number of milliseconds to sleep between runs of the idle object evictor thread. When non-positive, no
     * idle object evictor thread will be run.
     *
     * @param timeBetweenEvictionRunsMillis The number of milliseconds to sleep between runs of the idle object evictor
     *        thread. When non-positive, no idle object evictor thread will be run.
     * @see #getDurationBetweenEvictionRuns()
     * @throws IllegalStateException if {@link #getPooledConnection()} has been called
     * @deprecated Use {@link #setDurationBetweenEvictionRuns(Duration)}.
     */
    @Deprecated
    public void setTimeBetweenEvictionRunsMillis(final long timeBetweenEvictionRunsMillis) {
        assertInitializationAllowed();
        this.durationBetweenEvictionRuns = Duration.ofMillis(timeBetweenEvictionRunsMillis);
    }

    /**
     * Sets the value of URL string used to locate the database for this data source.
     *
     * @param connectionString Value to assign to connection string.
     * @throws IllegalStateException if {@link #getPooledConnection()} has been called
     */
    public void setUrl(final String connectionString) {
        assertInitializationAllowed();
        this.connectionString = connectionString;
    }

    /**
     * Sets the value of default user (login or user name).
     *
     * @param userName Value to assign to user.
     * @throws IllegalStateException if {@link #getPooledConnection()} has been called
     */
    public void setUser(final String userName) {
        assertInitializationAllowed();
        this.userName = userName;
        update(connectionProperties, Constants.KEY_USER, userName);
    }

    /**
     * Does not print the userName and userPassword field nor the 'user' or 'password' in the connectionProperties.
     *
     * @since 2.6.0
     */
    @Override
    public synchronized String toString() {
        final StringBuilder builder = new StringBuilder(super.toString());
        builder.append("[description=");
        builder.append(description);
        builder.append(", connectionString=");
        // TODO What if the connection string contains a 'user' or 'password' query parameter but that connection string
        // is not in a legal URL format?
        builder.append(connectionString);
        builder.append(", driver=");
        builder.append(driver);
        builder.append(", loginTimeout=");
        builder.append(loginTimeout);
        builder.append(", poolPreparedStatements=");
        builder.append(poolPreparedStatements);
        builder.append(", maxIdle=");
        builder.append(maxIdle);
        builder.append(", timeBetweenEvictionRunsMillis=");
        builder.append(durationBetweenEvictionRuns);
        builder.append(", numTestsPerEvictionRun=");
        builder.append(numTestsPerEvictionRun);
        builder.append(", minEvictableIdleTimeMillis=");
        builder.append(minEvictableIdleDuration);
        builder.append(", maxPreparedStatements=");
        builder.append(maxPreparedStatements);
        builder.append(", getConnectionCalled=");
        builder.append(getConnectionCalled);
        builder.append(", connectionProperties=");
        builder.append(Utils.cloneWithoutCredentials(connectionProperties));
        builder.append(", accessToUnderlyingConnectionAllowed=");
        builder.append(accessToUnderlyingConnectionAllowed);
        builder.append("]");
        return builder.toString();
    }

    private void update(final Properties properties, final String key, final String value) {
        if (properties != null && key != null) {
            if (value == null) {
                properties.remove(key);
            } else {
                properties.setProperty(key, value);
            }
        }
    }
}
