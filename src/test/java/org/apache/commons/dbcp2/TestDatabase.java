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
import java.sql.SQLException;
import java.util.Locale;
import java.util.UUID;

import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.apache.commons.dbcp2.cpdsadapter.DriverAdapterCPDS;
import org.h2.jdbcx.JdbcDataSource;
import org.hsqldb.jdbc.JDBCDataSource;
import org.hsqldb.jdbc.pool.JDBCXADataSource;
import org.postgresql.ds.PGSimpleDataSource;
import org.postgresql.xa.PGXADataSource;

import com.mysql.cj.jdbc.MysqlDataSource;
import com.mysql.cj.jdbc.MysqlXADataSource;

/**
 * Database-specific configuration shared by embedded tests and Docker integration tests.
 */
public enum TestDatabase {
    /**
     * H2 in-memory databases.
     */
    H2("org.h2.Driver"),
    /**
     * HyperSQL in-memory databases.
     */
    HSQLDB("org.hsqldb.jdbc.JDBCDriver"),
    /**
     * PostgreSQL started by the Docker Maven plugin.
     */
    POSTGRESQL("org.postgresql.Driver"),
    /**
     * MySQL started by the Docker Maven plugin.
     */
    MYSQL("com.mysql.cj.jdbc.Driver");

    /** JDBC driver class name. */
    private final String driverClassName;

    /** Stores the JDBC driver class name. */
    TestDatabase(final String driverClassName) {
        this.driverClassName = driverClassName;
    }

    /**
     * Opens an isolated embedded database, or connects to the configured server database.
     *
     * @return a physical connection.
     * @throws SQLException if the connection cannot be opened.
     */
    public Connection connect() throws SQLException {
        return DriverManager.getConnection(newUrl(), getUser(), getPassword());
    }

    /**
     * Configures a DBCP data source for the supplied database URL.
     *
     * @param dataSource the data source.
     * @param url the database URL.
     */
    public void configure(final BasicDataSource dataSource, final String url) {
        dataSource.setDriverClassName(driverClassName);
        dataSource.setUrl(url);
        dataSource.setUsername(getUser());
        dataSource.setPassword(getPassword());
    }

    /**
     * Configures the connection-pool data-source adapter.
     *
     * @param dataSource the adapter.
     * @param url the database URL.
     * @throws ClassNotFoundException if the driver is unavailable.
     */
    public void configure(final DriverAdapterCPDS dataSource, final String url) throws ClassNotFoundException {
        dataSource.setDriver(driverClassName);
        dataSource.setUrl(url);
        dataSource.setUser(getUser());
        dataSource.setPassword(getPassword());
    }

    /**
     * Creates a vendor data source.
     *
     * @param url the database URL.
     * @return the configured data source.
     */
    public DataSource createDataSource(final String url) {
        switch (this) {
        case H2:
            final JdbcDataSource h2 = new JdbcDataSource();
            h2.setURL(url);
            h2.setUser(getUser());
            h2.setPassword(getPassword());
            return h2;
        case HSQLDB:
            final JDBCDataSource hsqldb = new JDBCDataSource();
            hsqldb.setUrl(url);
            hsqldb.setUser(getUser());
            hsqldb.setPassword(getPassword());
            return hsqldb;
        case POSTGRESQL:
            final PGSimpleDataSource postgres = new PGSimpleDataSource();
            postgres.setUrl(url);
            postgres.setUser(getUser());
            postgres.setPassword(getPassword());
            return postgres;
        case MYSQL:
            final MysqlDataSource mysql = new MysqlDataSource();
            mysql.setUrl(url);
            mysql.setUser(getUser());
            mysql.setPassword(getPassword());
            return mysql;
        default:
            throw new IllegalStateException(name());
        }
    }

    /**
     * Creates a vendor XA data source.
     *
     * @param url the database URL.
     * @return the configured XA data source.
     * @throws SQLException if the XA data source cannot be created.
     */
    public XADataSource createXaDataSource(final String url) throws SQLException {
        switch (this) {
        case H2:
            return (XADataSource) createDataSource(url);
        case HSQLDB:
            final JDBCXADataSource hsqldb = new JDBCXADataSource();
            hsqldb.setUrl(url);
            hsqldb.setUser(getUser());
            hsqldb.setPassword(getPassword());
            return hsqldb;
        case POSTGRESQL:
            final PGXADataSource postgres = new PGXADataSource();
            postgres.setUrl(url);
            postgres.setUser(getUser());
            postgres.setPassword(getPassword());
            return postgres;
        case MYSQL:
            final MysqlXADataSource mysql = new MysqlXADataSource();
            mysql.setUrl(url);
            mysql.setUser(getUser());
            mysql.setPassword(getPassword());
            return mysql;
        default:
            throw new IllegalStateException(name());
        }
    }

    /**
     * Gets the registered JDBC driver.
     *
     * @return the JDBC driver.
     * @throws SQLException if the driver is unavailable.
     */
    public Driver getDriver() throws SQLException {
        return DriverManager.getDriver(newUrl());
    }

    /**
     * Gets the JDBC driver class name.
     *
     * @return the class name.
     */
    public String getDriverClassName() {
        return driverClassName;
    }

    /**
     * Gets the expected concrete JDBC class for an embedded driver.
     *
     * @param jdbcType the standard JDBC interface.
     * @return the vendor implementation class.
     */
    public Class<?> getJdbcType(final Class<?> jdbcType) {
        final String prefix;
        if (this == H2) {
            prefix = "org.h2.jdbc.Jdbc";
        } else if (this == HSQLDB) {
            prefix = "org.hsqldb.jdbc.JDBC";
        } else {
            throw new IllegalStateException("Concrete class assertions are for embedded drivers: " + this);
        }
        try {
            return Class.forName(prefix + jdbcType.getSimpleName());
        } catch (final ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Gets the test password.
     *
     * @return the password.
     */
    public String getPassword() {
        return this == H2 || this == HSQLDB ? "" : property("password");
    }

    /**
     * Gets the test user.
     *
     * @return the user.
     */
    public String getUser() {
        return this == H2 || this == HSQLDB ? "SA" : property("user");
    }

    /**
     * Gets a unique embedded URL or the server URL supplied by Failsafe.
     *
     * @return the database URL.
     */
    public String newUrl() {
        switch (this) {
        case H2:
            return "jdbc:h2:mem:dbcp_" + UUID.randomUUID();
        case HSQLDB:
            // Shut down when the last physical connection closes, including after failed tests.
            return "jdbc:hsqldb:mem:dbcp_" + UUID.randomUUID() + ";shutdown=true";
        default:
            return property("url");
        }
    }

    /** Gets a required server property without silently skipping integration tests. */
    private String property(final String suffix) {
        final String name = "dbcp.test." + name().toLowerCase(Locale.ROOT) + "." + suffix;
        final String value = System.getProperty(name);
        if (value == null || value.trim().isEmpty() || value.contains("${")) {
            throw new IllegalStateException("Missing " + name + "; run mvn -Pdocker-it verify");
        }
        return value;
    }

    /**
     * Gets SQL selecting a single integer.
     *
     * @param value the integer.
     * @return the SQL.
     */
    public String select(final int value) {
        return this == HSQLDB ? "VALUES (" + value + ")" : "SELECT " + value;
    }

    /**
     * Gets SQL selecting a single integer parameter.
     *
     * @return the SQL.
     */
    public String selectParameter() {
        return this == HSQLDB ? "VALUES (CAST(? AS INTEGER))" : "SELECT ? AS ID";
    }
}
