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

import org.junit.jupiter.api.Test;
import org.mockito.stubbing.OngoingStubbing;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.reset;

public class TestRequestBoundaries {
    Driver driver = mock(TesterDriver.class);

    @Test
    public void testBeginRequestOneConnection() throws SQLException {
        // Verify JDK version
        assumeTrue(Double.valueOf(System.getProperty("java.class.version")) >= 53);

        // Setup
        BasicDataSource dataSource = getDataSource(driver);
        TesterConnection connection = setupPhysicalConnections(1).get(0);

        // Get connection
        dataSource.getConnection();

        // Verify number of calls
        assertCallCount(connection, 1, 0);
    }

    @Test
    public void testEndRequestOneConnection() throws SQLException {
        // Verify JDK version
        assumeTrue(Double.valueOf(System.getProperty("java.class.version")) >= 53);

        // Setup
        BasicDataSource dataSource = getDataSource(driver);
        TesterConnection connection = setupPhysicalConnections(1).get(0);

        // Get then close connection
        dataSource.getConnection().close();

        // Verify number of calls
        assertCallCount(connection, 1, 1);
    }

    @Test
    public void testBeginRequestTwoVirtualConnections() throws SQLException {
        // Verify JDK version
        assumeTrue(Double.valueOf(System.getProperty("java.class.version")) >= 53);

        // Setup
        BasicDataSource dataSource = getDataSource(driver);
        TesterConnection connection = setupPhysicalConnections(1).get(0);

        // Get connection close it then get another connection
        dataSource.getConnection().close();
        dataSource.getConnection();

        // Verify number calls
        assertCallCount(connection, 2, 1);
    }

    @Test
    public void testEndRequestTwoVirtualConnections() throws SQLException {
        // Verify JDK version
        assumeTrue(Double.valueOf(System.getProperty("java.class.version")) >= 53);

        // Setup
        BasicDataSource dataSource = getDataSource(driver);
        TesterConnection connection = setupPhysicalConnections(1).get(0);

        // Get a connection and close then get another connection and close it
        dataSource.getConnection().close();
        dataSource.getConnection().close();

        // Verify number of calls
        assertCallCount(connection, 2, 2);
    }

    @Test
    public void testRequestBoundariesTwoPhysicalConnections() throws SQLException {
        // Verify JDK version
        assumeTrue(Double.valueOf(System.getProperty("java.class.version")) >= 53);

        // Setup
        BasicDataSource dataSource = getDataSource(driver);
        List<TesterConnection> connections = setupPhysicalConnections(2);

        // Get a connection, then get another connection, then close the first connection
        Connection fetchedConnection = dataSource.getConnection();
        dataSource.getConnection();
        fetchedConnection.close();

        // Verify number of calls
        assertCallCount(connections.get(0), 1, 1);
        assertCallCount(connections.get(1), 1, 0);
    }

    @Test
    public void testConnectionWithoutRequestBoundaries() throws SQLException {
        // Verify JDK version
        assumeTrue(Double.valueOf(System.getProperty("java.class.version")) < 53);

        // Setup
        BasicDataSource dataSource = getDataSource(driver);
        TesterConnection connection = setupPhysicalConnections(1).get(0);

        // Get connection
        dataSource.getConnection().close();

        assertCallCount(connection, 0, 0);
    }

    public BasicDataSource getDataSource(Driver driver) throws SQLException {
        reset(driver);

        BasicDataSource dataSource = BasicDataSourceFactory.createDataSource(new Properties());
        dataSource.setDriver(driver);

        // Before testing the call count of beginRequest and endRequest method we'll make sure that the
        // connectionFactory has been validated which involves creating a physical connection and destroying it. If we
        // don't, it's going to be done automatically when the first connection is requested. This is going to mess the
        // call count.
        validateConnectionFactory(dataSource, driver);

        return dataSource;
    }

    public void validateConnectionFactory(BasicDataSource dataSource, Driver driver) throws SQLException {
        when(driver.connect(isNull(), any(Properties.class))).thenReturn(getTesterConnection());
        dataSource.getLogWriter();
    }

    public TesterConnection getTesterConnection() throws SQLException {
        TesterConnection connection = new TesterConnection(null, null);

        return connection;
    }

    public List<TesterConnection> setupPhysicalConnections(int numOfConnections) throws SQLException {
        List<TesterConnection> listOfConnections = new ArrayList<>();

        for (int i = 0; i < numOfConnections; i++) {
            listOfConnections.add(getTesterConnection());
        }

        OngoingStubbing<Connection> ongoingStubbing =  when(driver.connect(isNull(), any(Properties.class)));

        for (Connection connection : listOfConnections) {
            ongoingStubbing = ongoingStubbing.thenReturn(connection);
        }
        return listOfConnections;
    }

    public void assertCallCount(TesterConnection connection, int expectedBeginRequestCalls, int expectedEndRequestCalls)
            throws SQLException {
        assertEquals(expectedBeginRequestCalls, connection.beginRequestCount.get());
        assertEquals(expectedEndRequestCalls, connection.endRequestCount.get());
    }
}
