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

package org.apache.commons.dbcp2.function;

import java.sql.SQLException;
import java.util.function.Supplier;

/**
 * Represents a supplier of SQL {@code boolean}-valued results. This is the {@code boolean}-producing primitive
 * specialization of {@link Supplier}.
 *
 * <p>
 * There is no requirement that a distinct result be returned each time the supplier is invoked.
 * </p>
 *
 * <p>
 * This is a <a href="package-summary.html">functional interface</a> whose functional method is {@link #getAsBoolean()}.
 * </p>
 *
 * @see java.util.function.BooleanSupplier
 * @since 2.8.0
 */
@FunctionalInterface
public interface SQLBooleanSupplier {

    /**
     * Gets a result.
     *
     * @return a result
     * @throws SQLException
     */
    boolean getAsBoolean() throws SQLException;
}
