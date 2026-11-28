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
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

/**
 * An isolated database account, removed after its pools have closed.
 */
final class DatabaseTestUser implements AutoCloseable {
    private final TestDatabase database;
    private final String url;
    private final String name = "DBCP_U_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase(java.util.Locale.ROOT);
    private String password;

    /**
     * Creates a disposable login with permission to connect to the test database.
     */
    DatabaseTestUser(final TestDatabase database, final String url, final String password) throws SQLException {
        this.database = database;
        this.url = url;
        this.password = password;
        try (Connection connection = database.adminConnection(url); Statement statement = connection.createStatement()) {
            switch (database) {
            case MYSQL:
            case MARIADB:
                statement.execute("CREATE USER '" + name + "'@'%' IDENTIFIED BY '" + password + "'");
                statement.execute("GRANT ALL ON dbcp.* TO '" + name + "'@'%'");
                break;
            case ORACLE:
                statement.execute("CREATE USER " + name + " IDENTIFIED BY \"" + password + "\"");
                statement.execute("GRANT CREATE SESSION TO " + name);
                break;
            case SQLSERVER:
                statement.execute("CREATE LOGIN " + name + " WITH PASSWORD = '" + password + "'");
                statement.execute("USE dbcp");
                statement.execute("CREATE USER " + name + " FOR LOGIN " + name);
                break;
            default:
                statement.execute("CREATE USER \"" + name + "\" " + (database == TestDatabase.POSTGRESQL ? "WITH " : "")
                        + "PASSWORD '" + password + "'");
            }
        }
    }

    /**
     * Changes this account's password without modifying the common database account.
     */
    void changePassword(final String value) throws SQLException {
        try (Connection connection = database.adminConnection(url); Statement statement = connection.createStatement()) {
            switch (database) {
            case MYSQL:
            case MARIADB:
                statement.execute("ALTER USER '" + name + "'@'%' IDENTIFIED BY '" + value + "'");
                break;
            case ORACLE:
                statement.execute("ALTER USER " + name + " IDENTIFIED BY \"" + value + "\"");
                break;
            case SQLSERVER:
                statement.execute("ALTER LOGIN " + name + " WITH PASSWORD = '" + value + "'");
                break;
            default:
                statement.execute("ALTER USER \"" + name + "\" " + (database == TestDatabase.POSTGRESQL ? "WITH " : "SET ")
                        + "PASSWORD '" + value + "'");
            }
            password = value;
        }
    }

    @Override
    public void close() throws SQLException {
        try (Connection connection = database.adminConnection(url); Statement statement = connection.createStatement()) {
            switch (database) {
            case MYSQL:
            case MARIADB:
                statement.execute("DROP USER '" + name + "'@'%'");
                break;
            case SQLSERVER:
                statement.execute("USE dbcp");
                statement.execute("DROP USER " + name);
                statement.execute("DROP LOGIN " + name);
                break;
            default:
                statement.execute("DROP USER \"" + name + "\"");
            }
        }
    }

    /**
     * Gets the login name.
     */
    String getName() {
        return name;
    }

    /**
     * Gets the current password.
     */
    String getPassword() {
        return password;
    }
}
