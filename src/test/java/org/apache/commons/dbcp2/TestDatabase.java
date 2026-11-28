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
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.Properties;
import java.util.UUID;

import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.apache.commons.dbcp2.cpdsadapter.DriverAdapterCPDS;
import org.h2.jdbcx.JdbcDataSource;
import org.hsqldb.jdbc.JDBCDataSource;
import org.hsqldb.jdbc.pool.JDBCXADataSource;
import org.mariadb.jdbc.MariaDbDataSource;
import org.postgresql.ds.PGSimpleDataSource;
import org.postgresql.xa.PGXADataSource;

import com.microsoft.sqlserver.jdbc.SQLServerDataSource;
import com.microsoft.sqlserver.jdbc.SQLServerXADataSource;
import com.mysql.cj.jdbc.MysqlDataSource;
import com.mysql.cj.jdbc.MysqlXADataSource;
import oracle.jdbc.pool.OracleDataSource;
import oracle.jdbc.xa.client.OracleXADataSource;

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
    MYSQL("com.mysql.cj.jdbc.Driver"),
    /**
     * MariaDB started by the Docker Maven plugin.
     *
     * @since 2.15.0
     */
    MARIADB("org.mariadb.jdbc.Driver"),
    /**
     * Oracle Database Express Edition started by the Docker Maven plugin.
     *
     * @since 2.15.0
     */
    ORACLE("oracle.jdbc.OracleDriver"),
    /**
     * Microsoft SQL Server started by the Docker Maven plugin.
     *
     * @since 2.15.0
     */
    SQLSERVER("com.microsoft.sqlserver.jdbc.SQLServerDriver");

    /**
     * Whether server initialization has completed in this test JVM.
     */
    private boolean initialized;

    /** JDBC driver class name. */
    private final String driverClassName;

    /** Stores the JDBC driver class name. */
    TestDatabase(final String driverClassName) {
        this.driverClassName = driverClassName;
    }

    /**
     * Opens an administrative connection for isolated test accounts.
     */
    Connection adminConnection(final String url) throws SQLException {
        if (this == ORACLE) {
            final Properties properties = new Properties();
            properties.setProperty("user", "sys");
            properties.setProperty("password", property("adminPassword"));
            properties.setProperty("internal_logon", "sysdba");
            return DriverManager.getConnection(url, properties);
        }
        if (this == SQLSERVER) {
            return DriverManager.getConnection(property("adminUrl"), "sa", getPassword());
        }
        if (this == MYSQL || this == MARIADB) {
            return DriverManager.getConnection(url, "root", property("adminPassword"));
        }
        return DriverManager.getConnection(url, getUser(), getPassword());
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
     * Opens an isolated embedded database, or connects to the configured server database.
     *
     * @return a physical connection.
     * @throws SQLException if the connection cannot be opened.
     */
    public Connection connect() throws SQLException {
        return DriverManager.getConnection(newUrl(), getUser(), getPassword());
    }

    /**
     * Creates a vendor data source.
     *
     * @param url the database URL.
     * @return the configured data source.
     * @throws SQLException if the data source cannot be configured.
     */
    public DataSource createDataSource(final String url) throws SQLException {
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
        case MARIADB:
            final MariaDbDataSource maria = new MariaDbDataSource(url);
            maria.setUser(getUser());
            maria.setPassword(getPassword());
            return maria;
        case ORACLE:
            final OracleDataSource oracle = new OracleDataSource();
            oracle.setURL(url);
            oracle.setUser(getUser());
            oracle.setPassword(getPassword());
            return oracle;
        case SQLSERVER:
            final SQLServerDataSource sqlserver = new SQLServerDataSource();
            sqlserver.setURL(url);
            sqlserver.setUser(getUser());
            sqlserver.setPassword(getPassword());
            return sqlserver;
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
        case MARIADB:
            return (XADataSource) createDataSource(url);
        case ORACLE:
            final OracleXADataSource oracle = new OracleXADataSource();
            oracle.setURL(url);
            oracle.setUser(getUser());
            oracle.setPassword(getPassword());
            return oracle;
        case SQLSERVER:
            final SQLServerXADataSource sqlserver = new SQLServerXADataSource();
            sqlserver.setURL(url);
            sqlserver.setUser(getUser());
            sqlserver.setPassword(getPassword());
            return sqlserver;
        default:
            throw new IllegalStateException(name());
        }
    }

    /**
     * Initializes XA support and the test account once per database and test JVM.
     */
    synchronized void initialize() throws SQLException {
        if (initialized) {
            return;
        }
        if (this == TestDatabase.ORACLE) {
            final Properties properties = new Properties();
            properties.setProperty("user", "sys");
            properties.setProperty("password", System.getProperty("dbcp.test.oracle.adminPassword"));
            properties.setProperty("internal_logon", "sysdba");
            try (Connection connection = DriverManager.getConnection(newUrl(), properties);
                    Statement statement = connection.createStatement()) {
                statement.execute("GRANT SELECT ON SYS.DBA_PENDING_TRANSACTIONS TO dbcp");
                statement.execute("GRANT SELECT ON SYS.PENDING_TRANS$ TO dbcp");
                statement.execute("GRANT SELECT ON SYS.DBA_2PC_PENDING TO dbcp");
                statement.execute("GRANT EXECUTE ON SYS.DBMS_XA TO dbcp");
            }
        } else if (this == TestDatabase.SQLSERVER) {
            try (Connection connection = DriverManager.getConnection(System.getProperty("dbcp.test.sqlserver.adminUrl"), "sa", getPassword());
                    Statement statement = connection.createStatement()) {
                statement.execute("EXEC sp_sqljdbc_xa_install");
                statement.execute("IF DB_ID('dbcp') IS NULL CREATE DATABASE dbcp");
                statement.execute("IF SUSER_ID('dbcp') IS NULL CREATE LOGIN dbcp WITH PASSWORD = '"
                        + getPassword().replace("'", "''") + "'");
                statement.execute("IF DATABASE_PRINCIPAL_ID('dbcp') IS NULL CREATE USER dbcp FOR LOGIN dbcp");
                statement.execute("ALTER ROLE SqlJDBCXAUser ADD MEMBER dbcp");
                statement.execute("USE dbcp");
                statement.execute("IF DATABASE_PRINCIPAL_ID('dbcp') IS NULL CREATE USER dbcp FOR LOGIN dbcp");
                statement.execute("ALTER ROLE db_owner ADD MEMBER dbcp");
            }
        }
        if (this == MYSQL || this == MARIADB) {
            try (Connection connection = connect(); Statement statement = connection.createStatement()) {
                statement.execute("CREATE PROCEDURE IF NOT EXISTS DBCP_QUERY(IN VALUE_ARG INT) SELECT VALUE_ARG AS ID");
            }
        }
        initialized = true;
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
     * Gets the identity-column definition used by the shared database contract.
     *
     * @return the SQL identity-column definition.
     */
    String getIdentityColumnDefinition() {
        switch (this) {
        case MYSQL:
        case MARIADB:
            return "INTEGER AUTO_INCREMENT";
        case SQLSERVER:
            return "INTEGER IDENTITY(1,1)";
        default:
            return "INTEGER GENERATED BY DEFAULT AS IDENTITY (START WITH 1)";
        }
    }

    /**
     * Gets the vendor JDBC type used by unwrapping assertions.
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
        } else if (this == POSTGRESQL) {
            prefix = "org.postgresql.jdbc.Pg";
        } else if (this == SQLSERVER) {
            prefix = "com.microsoft.sqlserver.jdbc.SQLServer";
        } else if (this == ORACLE) {
            prefix = jdbcType == java.sql.DatabaseMetaData.class ? "oracle.jdbc.driver.Oracle" : "oracle.jdbc.Oracle";
        } else if (this == MARIADB) {
            prefix = "org.mariadb.jdbc.";
        } else {
            prefix = "com.mysql.cj.jdbc.";
        }
        String type = jdbcType.getSimpleName();
        if (this == MYSQL) {
            if (jdbcType == java.sql.ResultSet.class) {
                type = "result.ResultSetImpl";
            } else if (jdbcType == java.sql.Connection.class || jdbcType == java.sql.Statement.class) {
                type += "Impl";
            } else if (jdbcType == java.sql.PreparedStatement.class) {
                type = "ClientPreparedStatement";
            }
        } else if (this == MARIADB && jdbcType == java.sql.ResultSet.class) {
            type = "client.result.Result";
        } else if (this == MARIADB && jdbcType == java.sql.PreparedStatement.class) {
            type = "BasePreparedStatement";
        } else if (this == MARIADB && jdbcType == java.sql.CallableStatement.class) {
            type = "BaseCallableStatement";
        }
        try {
            return Class.forName(prefix + type);
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

    /**
     * Prepares an insert that returns the identity value instead of Oracle's default ROWID.
     *
     * @param connection the connection.
     * @param sql the insert statement.
     * @return the prepared statement.
     * @throws SQLException if the statement cannot be prepared.
     */
    PreparedStatement prepareInsertWithGeneratedKeys(final Connection connection, final String sql) throws SQLException {
        return this == ORACLE ? connection.prepareStatement(sql, new String[] {"ID"}) : connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
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
     * Gets a query that can be prepared as a callable statement.
     */
    String callQuery() {
        return this == H2 ? "{? = call ABS(?)}" : this == HSQLDB ? "CALL ABS(?)"
                : this == MYSQL || this == MARIADB ? "{call DBCP_QUERY(?)}" : selectParameter();
    }

    /**
     * Gets the SQL type for a large text payload.
     *
     * @return the type.
     * @since 2.15.0
     */
    public String getLargeTextType() {
        switch (this) {
        case MYSQL:
        case MARIADB:
            return "LONGTEXT";
        case ORACLE:
            return "CLOB";
        case SQLSERVER:
            return "VARCHAR(MAX)";
        default:
            return "VARCHAR(100000)";
        }
    }

    /**
     * Gets the SQL type for a timestamp value.
     *
     * @return the type.
     * @since 2.15.0
     */
    public String getTimestampType() {
        return this == SQLSERVER ? "DATETIME2" : "TIMESTAMP";
    }

    /**
     * Gets SQL selecting a single integer.
     *
     * @param value the integer.
     * @return the SQL.
     */
    public String select(final int value) {
        return this == HSQLDB ? "VALUES (" + value + ")" : "SELECT " + value + (this == ORACLE ? " FROM DUAL" : "");
    }

    /**
     * Gets SQL selecting a single integer parameter.
     *
     * @return the SQL.
     */
    public String selectParameter() {
        return this == HSQLDB ? "VALUES (CAST(? AS INTEGER))" : "SELECT ? AS ID" + (this == ORACLE ? " FROM DUAL" : "");
    }
}
