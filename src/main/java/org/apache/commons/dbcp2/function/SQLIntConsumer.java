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
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Represents an operation that accepts a single {@code int}-valued argument and no result. This is the primitive type
 * specialization of {@link Consumer} for {@code int}. Unlike most other functional interfaces, {@code IntConsumer} is
 * expected to operate via side-effects.
 *
 * <p>
 * This is a <a href="package-summary.html">functional interface</a> whose functional method is {@link #accept(int)}.
 * <p>
 *
 * @see Consumer
 * @since 2.8.0
 */
@FunctionalInterface
public interface SQLIntConsumer {

    /**
     * Performs this operation on the given argument.
     *
     * @param value the input argument
     * @throws SQLException
     */
    void accept(int value) throws SQLException;

    /**
     * As {@link Consumer#andThen(Consumer)} but with a SQL Exception.
     *
     * @param after the operation to perform after this operation
     * @return a composed {@code Consumer} that performs in sequence this operation followed by the {@code after}
     *         operation
     * @throws NullPointerException if {@code after} is null
     */
    default SQLIntConsumer andThen(SQLIntConsumer after) {
        Objects.requireNonNull(after);
        return (int t) -> {
            accept(t);
            after.accept(t);
        };
    }
}
