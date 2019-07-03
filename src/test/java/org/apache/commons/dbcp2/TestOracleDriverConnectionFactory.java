package org.apache.commons.dbcp2;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;

/**
 *	Simple ConnectionFactory impl class that use BasicDataSource.createConnectionFactory()
 */

public class TestOracleDriverConnectionFactory implements ConnectionFactory {
	private final String connectionString;
    private final Driver driver;
    private final Properties properties;

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
    public TestOracleDriverConnectionFactory(final Driver driver, final String connectString, final Properties properties) {
        this.driver = driver;
        this.connectionString = connectString;
        this.properties = properties;
    }

    @Override
    public Connection createConnection() throws SQLException {
        Connection conn = driver.connect(connectionString, properties);
        callOracleSetModule(conn);
        return conn;
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

    private void callOracleSetModule(Connection conn) {
    	// do something
    }
}
