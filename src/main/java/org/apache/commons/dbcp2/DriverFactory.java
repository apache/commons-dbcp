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

import java.lang.reflect.InvocationTargetException;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;

/**
 * Selects JDBC drivers and creates instances for explicitly configured driver classes.
 *
 * <p>
 * Driver discovery and loading depend on the JDBC version:
 * </p>
 * <ul>
 * <li>Before JDBC 4.0, applications using {@link DriverManager} generally arranged for drivers to be loaded explicitly, commonly through
 * {@link Class#forName(String)} or the {@code jdbc.drivers} system property. Drivers then registered themselves with {@link DriverManager}. This was the usual
 * model for JDBC 1.x, 2.x, and 3.0. JDBC 3.0 was introduced in J2SE 1.4 and remained the JDBC version in Java 5.</li>
 * <li>JDBC 4.0, introduced in Java SE 6, added automatic driver discovery through Java's service-provider mechanism. A driver JAR on the classpath declares its
 * driver implementations in {@code META-INF/services/java.sql.Driver}. Provided the service metadata and driver classes are visible to the relevant class
 * loaders, an application using {@link DriverManager} normally supplies the JDBC URL and any required credentials or connection properties without explicitly
 * naming or loading the driver class. Explicit loading remains supported for compatibility.</li>
 * <li>JDBC 4.1 (Java 7), 4.2 (Java 8), and 4.3 (Java 9) retained automatic discovery while adding other JDBC capabilities.</li>
 * </ul>
 *
 * <p>
 * URL-based driver selection predates JDBC 4.0. Automatic discovery changes how drivers become available to {@link DriverManager}; selection uses drivers that
 * are registered and accessible to the caller. A driver JAR can contain multiple implementations for compatibility or specialized use cases. If several drivers
 * accept the same JDBC URL, that URL alone does not uniquely identify the intended implementation.
 * </p>
 *
 * <p>
 * Tools and deployment environments may still require an explicit driver class name, including when using modern JDBC drivers. For {@link BasicDataSource}, a
 * driver supplied through {@link BasicDataSource#setDriver(Driver)} takes precedence over a class name supplied through
 * {@link BasicDataSource#setDriverClassName(String)}. If neither is configured, this factory uses {@link DriverManager#getDriver(String)} to select an
 * available driver for the configured URL.
 * </p>
 *
 * <p>
 * Whether a driver JAR is supplied through a user interface, obtained as a Maven dependency, or made available through another build or deployment process, it
 * contains executable code chosen for the application to load. Automatic discovery neither downloads the driver nor establishes its trustworthiness. Developers
 * and administrators are responsible for selecting trusted dependencies and controlling who can change driver configuration.
 * </p>
 *
 * <p>
 * When a driver class name is configured, this factory checks that the class implements {@link Driver} before initializing it or invoking its constructor. This
 * hardening prevents unrelated classes from being initialized or instantiated through this driver-creation path. A malicious implementation of {@link Driver}
 * can still pass the type check and execute code with the privileges available to it. Type validation does not establish that a driver is safe, and DBCP does
 * not provide a sandbox for driver implementations. The driver JAR must therefore be trusted.
 * </p>
 *
 * @since 2.7.0
 * @see DriverManager
 * @see <a href="https://docs.oracle.com/javase/9/docs/api/java/sql/package-summary.html">JDBC version history and features</a>
 */
final class DriverFactory {

    /**
     * SQLState for a client unable to establish a connection.
     */
    private static final String SQL_STATE_UNABLE_TO_CONNECT = "08001";

    /**
     * Selects the JDBC driver for a data source, creating an instance when a driver class name is configured.
     *
     * <p>
     * An explicitly configured driver takes precedence. Otherwise, a named class is loaded without initialization using the configured class loader, or DBCP's
     * defining class loader if none is configured. If the class is not found, the thread context class loader is tried. The class must implement {@link Driver}
     * before DBCP initializes and instantiates it, and its instance must accept the configured URL. If neither a driver instance nor a class name is
     * configured, {@link DriverManager#getDriver(String)} selects a driver for that URL.
     * </p>
     *
     * <p>
     * Failures use SQLState {@code 08001} (client unable to establish a connection). If driver instantiation or URL validation throws a {@link SQLException},
     * its vendor code and any non-null SQLState are preserved in the wrapping exception. The original exception chain is retained.
     * </p>
     *
     * @param basicDataSource the data source providing the driver configuration.
     * @return the configured, instantiated, or discovered JDBC driver.
     * @throws SQLException Thrown if class loading, type validation, instantiation, or URL validation fails with an exception, or no suitable driver is found.
     */
    static Driver createDriver(final BasicDataSource basicDataSource) throws SQLException {
        // Load the JDBC driver class
        Driver driver = basicDataSource.getDriver();
        final String driverClassName = basicDataSource.getDriverClassName();
        final ClassLoader driverClassLoader = basicDataSource.getDriverClassLoader();
        final String url = basicDataSource.getUrl();
        if (driver == null) {
            Class<?> driverFromCCL = null;
            if (driverClassName != null) {
                try {
                    try {
                        // Defer initialization until the class has been checked to implement Driver.
                        driverFromCCL = Class.forName(driverClassName, false,
                                driverClassLoader == null ? DriverFactory.class.getClassLoader() : driverClassLoader);
                    } catch (final ClassNotFoundException cnfe) {
                        driverFromCCL = Thread.currentThread().getContextClassLoader().loadClass(driverClassName);
                    }
                } catch (final Exception t) {
                    final String message = "Cannot load JDBC driver class '" + driverClassName + "'";
                    basicDataSource.log(message, t);
                    throw new SQLException(message, SQL_STATE_UNABLE_TO_CONNECT, t);
                }
            }
            try {
                if (driverFromCCL == null) {
                    driver = DriverManager.getDriver(url);
                } else {
                    // Usage of DriverManager is not possible, as it does not respect the ContextClassLoader.
                    // Check the type before initializing the class or invoking its constructor.
                    driver = driverFromCCL.asSubclass(Driver.class).getConstructor().newInstance();
                    if (!driver.acceptsURL(url)) {
                        throw new SQLException("No suitable driver", SQL_STATE_UNABLE_TO_CONNECT);
                    }
                }
            } catch (final Exception t) {
                final String message = "Cannot create JDBC driver of class '" + Objects.toString(driverClassName, "") + "' for connect URL '" + url + "'";
                basicDataSource.log(message, t);
                final Throwable cause = t instanceof InvocationTargetException ? t.getCause() : t;
                if (cause instanceof SQLException) {
                    final SQLException sqlException = (SQLException) cause;
                    throw new SQLException(message, Objects.toString(sqlException.getSQLState(), SQL_STATE_UNABLE_TO_CONNECT), sqlException.getErrorCode(), t);
                }
                throw new SQLException(message, SQL_STATE_UNABLE_TO_CONNECT, t);
            }
        }
        return driver;
    }

    /**
     * Constructs a driver factory.
     */
    DriverFactory() {
    }
}
