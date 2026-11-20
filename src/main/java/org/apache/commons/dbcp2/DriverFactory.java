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
 * Creates {@link Driver} instances.
 *
 * @since 2.7.0
 */
final class DriverFactory {

    /**
     * SQLState for a client unable to establish a connection.
     */
    private static final String SQL_STATE_UNABLE_TO_CONNECT = "08001";

    /**
     * Constructs a driver factory.
     */
    DriverFactory() {
    }

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
     * @throws SQLException if class loading, type validation, instantiation, or URL validation fails with an exception, or no suitable driver is found.
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
}
