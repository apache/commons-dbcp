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

/**
 * Abstract factory interface for creating {@link java.sql.Connection}s.
 *
 * @since 2.0
 */
public interface ConnectionFactory {
    /**
     * Create a new {@link java.sql.Connection} in an implementation specific fashion.
     *
     * @return a new {@link java.sql.Connection}
     * @throws SQLException
     *             if a database error occurs creating the connection
     */
    Connection createConnection() throws SQLException;
}
