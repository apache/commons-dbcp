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
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * A {@link Driver}-based implementation of {@link ConnectionFactory}.
 *
 * @since 2.0
 */
public class DriverConnectionFactory implements ConnectionFactory {

    private final String connectionString;
    private final Driver driver;
    private final Properties properties;

    /**
     * Creates a JDBC connection factory for the given data source. The JDBC driver is loaded using the following
     * algorithm:
     * <ol>
     * <li>If a Driver instance has been specified via {@link #setDriver(Driver)} use it</li>
     * <li>If no Driver instance was specified and {@link #driverClassName} is specified that class is loaded using the
     * {@link ClassLoader} of this class or, if {@link #driverClassLoader} is set, {@link #driverClassName} is loaded
     * with the specified {@link ClassLoader}.</li>
     * <li>If {@link #driverClassName} is specified and the previous attempt fails, the class is loaded using the
     * context class loader of the current thread.</li>
     * <li>If a driver still isn't loaded one is loaded via the {@link DriverManager} using the specified {@link #url}.
     * </ol>
     * <p>
     * This method exists so subclasses can replace the implementation class.
     * </p>
     *
     * @return A new connection factory.
     *
     * @throws SQLException If the connection factory cannot be created
     */
    static DriverConnectionFactory createConnectionFactory(final BasicDataSource basicDataSource) throws SQLException {
        // Load the JDBC driver class
        Driver driverToUse = basicDataSource.getDriver();
        final String driverClassName = basicDataSource.getDriverClassName();
        final ClassLoader driverClassLoader = basicDataSource.getDriverClassLoader();
        final String url = basicDataSource.getUrl();

        if (driverToUse == null) {
            Class<?> driverFromCCL = null;
            if (driverClassName != null) {
                try {
                    try {
                        if (driverClassLoader == null) {
                            driverFromCCL = Class.forName(driverClassName);
                        } else {
                            driverFromCCL = Class.forName(driverClassName, true, driverClassLoader);
                        }
                    } catch (final ClassNotFoundException cnfe) {
                        driverFromCCL = Thread.currentThread().getContextClassLoader().loadClass(driverClassName);
                    }
                } catch (final Exception t) {
                    final String message = "Cannot load JDBC driver class '" + driverClassName + "'";
                    basicDataSource.log(message);
                    basicDataSource.log(t);
                    throw new SQLException(message, t);
                }
            }

            try {
                if (driverFromCCL == null) {
                    driverToUse = DriverManager.getDriver(url);
                } else {
                    // Usage of DriverManager is not possible, as it does not
                    // respect the ContextClassLoader
                    // N.B. This cast may cause ClassCastException which is handled below
                    driverToUse = (Driver) driverFromCCL.getConstructor().newInstance();
                    if (!driverToUse.acceptsURL(url)) {
                        throw new SQLException("No suitable driver", "08001");
                    }
                }
            } catch (final Exception t) {
                final String message = "Cannot create JDBC driver of class '"
                        + (driverClassName != null ? driverClassName : "") + "' for connect URL '" + url + "'";
                basicDataSource.log(message);
                basicDataSource.log(t);
                throw new SQLException(message, t);
            }
        }

        // Set up the driver connection factory we will use
        final String user = basicDataSource.getUsername();
        if (user != null) {
            basicDataSource.addConnectionProperty("user", user);
        } else {
            basicDataSource.log("DBCP DataSource configured without a 'username'");
        }

        final String pwd = basicDataSource.getPassword();
        if (pwd != null) {
            basicDataSource.addConnectionProperty("password", pwd);
        } else {
            basicDataSource.log("DBCP DataSource configured without a 'password'");
        }

        return new DriverConnectionFactory(driverToUse, url, basicDataSource.getConnectionProperties());
    }

    /**
     * Constructs a connection factory for a given Driver.
     *
     * @param driver
     *            The Driver.
     * @param connectString
     *            The connection string.
     * @param properties
     *            The connection properties.
     */
    public DriverConnectionFactory(final Driver driver, final String connectString, final Properties properties) {
        this.driver = driver;
        this.connectionString = connectString;
        this.properties = properties;
    }

    @Override
    public Connection createConnection() throws SQLException {
        return driver.connect(connectionString, properties);
    }

    /**
     * @return The connection String.
     * @since 2.6.0
     */
    public String getConnectionString() {
        return connectionString;
    }

    /**
     * @return The Driver.
     * @since 2.6.0
     */
    public Driver getDriver() {
        return driver;
    }

    /**
     * @return The Properties.
     * @since 2.6.0
     */
    public Properties getProperties() {
        return properties;
    }

    @Override
    public String toString() {
        return this.getClass().getName() + " [" + String.valueOf(driver) + ";" + String.valueOf(connectionString) + ";"
                + String.valueOf(properties) + "]";
    }
}
