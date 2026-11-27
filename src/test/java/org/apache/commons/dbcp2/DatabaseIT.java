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
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Locale;
import java.util.Properties;
import java.util.stream.Stream;

import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.BeforeParameterizedClassInvocation;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Runs the common database contract against the Maven-managed Docker databases.
 */
@ParameterizedClass(name = "{0}")
@MethodSource("databases")
class DatabaseIT extends AbstractDatabaseConnectionTest {

    /**
     * Selects the same comma-separated database aliases used by the Docker Maven plugin.
     *
     * @return the selected server databases.
     */
    static Stream<TestDatabase> databases() {
        return Arrays.stream(System.getProperty("dbcp.test.databases", "postgresql,mysql,mariadb,oracle,sqlserver").split(",", -1))
                .map(String::trim).map(name -> TestDatabase.valueOf(name.toUpperCase(Locale.ROOT))).peek(database -> {
                    if (database == TestDatabase.H2 || database == TestDatabase.HSQLDB) {
                        throw new IllegalArgumentException("Use EmbeddedDatabaseTest for " + database);
                    }
                }).distinct();
    }

    /**
     * Configures server-side XA support using JDBC, independently of the host operating system.
     *
     * @param database the selected database.
     * @throws SQLException if database initialization fails.
     */
    @BeforeParameterizedClassInvocation
    @Timeout(60)
    static void initializeDatabase(final TestDatabase database) throws SQLException {
        if (database == TestDatabase.ORACLE) {
            final Properties properties = new Properties();
            properties.setProperty("user", "sys");
            properties.setProperty("password", System.getProperty("dbcp.test.oracle.adminPassword"));
            properties.setProperty("internal_logon", "sysdba");
            try (Connection connection = DriverManager.getConnection(database.newUrl(), properties);
                    Statement statement = connection.createStatement()) {
                statement.execute("GRANT SELECT ON SYS.DBA_PENDING_TRANSACTIONS TO dbcp");
                statement.execute("GRANT SELECT ON SYS.PENDING_TRANS$ TO dbcp");
                statement.execute("GRANT SELECT ON SYS.DBA_2PC_PENDING TO dbcp");
                statement.execute("GRANT EXECUTE ON SYS.DBMS_XA TO dbcp");
            }
        } else if (database == TestDatabase.SQLSERVER) {
            try (Connection connection = DriverManager.getConnection(System.getProperty("dbcp.test.sqlserver.adminUrl"), "sa", database.getPassword());
                    Statement statement = connection.createStatement()) {
                statement.execute("EXEC sp_sqljdbc_xa_install");
                statement.execute("IF DB_ID('dbcp') IS NULL CREATE DATABASE dbcp");
                statement.execute("IF SUSER_ID('dbcp') IS NULL CREATE LOGIN dbcp WITH PASSWORD = '"
                        + database.getPassword().replace("'", "''") + "'");
                statement.execute("IF DATABASE_PRINCIPAL_ID('dbcp') IS NULL CREATE USER dbcp FOR LOGIN dbcp");
                statement.execute("ALTER ROLE SqlJDBCXAUser ADD MEMBER dbcp");
                statement.execute("USE dbcp");
                statement.execute("IF DATABASE_PRINCIPAL_ID('dbcp') IS NULL CREATE USER dbcp FOR LOGIN dbcp");
                statement.execute("ALTER ROLE db_owner ADD MEMBER dbcp");
            }
        }
    }
}
