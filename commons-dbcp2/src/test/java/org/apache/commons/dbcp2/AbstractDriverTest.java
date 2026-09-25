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

import java.sql.DriverManager;
import java.sql.SQLException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

/**
 * Abstracts testing a JDBC driver.
 */
public abstract class AbstractDriverTest {

    //private static final String KEY_JDBC_DRIVERS = "jdbc.drivers";

    @AfterAll
    public static void afterClass() throws SQLException {
        //System.clearProperty(KEY_JDBC_DRIVERS);
        DriverManager.deregisterDriver(TesterDriver.INSTANCE);
    }

    @BeforeAll
    public static void beforeClass() throws SQLException {
        //System.setProperty(KEY_JDBC_DRIVERS, "org.apache.commons.dbcp2.TesterDriver");
        DriverManager.registerDriver(TesterDriver.INSTANCE);
    }

}
