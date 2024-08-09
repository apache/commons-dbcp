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

import java.sql.SQLException;

/**
 * The {@code SQLExceptionOverride} interface provides a mechanism for deciding
 * whether a connection should be disconnected based on a given {@link SQLException}.
 * Implementations of this interface can provide custom logic to evaluate SQL exceptions
 * and decide if a connection should be disconnected, overriding the default handling
 * of commons-dhcp. When this interface is invoked, commons-dhcp has already decided
 * to evict the connection from the pool.
 *
 * @since 2.12.1
 */
public interface SQLExceptionOverride {

    /**
     * Determines whether a connection should be disconnected based on the provided {@link SQLException}.
     * <p>
     * The default implementation always returns {@code true}, indicating that the connection should be disconnected.
     * Override this method to provide custom logic for evaluating SQL exceptions.
     * </p>
     *
     * @param sqlException the {@link SQLException} to evaluate
     * @return {@code true} if the connection should be disconnected, {@code false} otherwise
     * @since 2.12.1
     */
    default boolean shouldDisconnectConnection(SQLException sqlException) {
        return true;
    }
}
