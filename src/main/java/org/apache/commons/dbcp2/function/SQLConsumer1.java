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
 * Represents a SQL operation that accepts a single input argument and no result. Unlike most other functional
 * interfaces, this is expected to operate via side-effects.
 *
 * <p>
 * This is a <a href="package-summary.html">functional interface</a> whose functional method is {@link #accept(Object)}.
 * </p>
 *
 * @param <T> the type of the input to the operation
 *
 * @since 2.8.0
 */
@FunctionalInterface
public interface SQLConsumer1<T> {

    /**
     * Performs this operation on the given argument.
     *
     * @param t the input argument
     * @throws SQLException
     */
    void accept(T t) throws SQLException;

    /**
     * As {@link Consumer#andThen(Consumer)} but with a SQL Exception.
     *
     * @param after the operation to perform after this operation
     * @return a composed {@code Consumer} that performs in sequence this operation followed by the {@code after}
     *         operation
     * @throws NullPointerException if {@code after} is null
     */
    default SQLConsumer1<T> andThen(SQLConsumer1<? super T> after) {
        Objects.requireNonNull(after);
        return (T t) -> {
            accept(t);
            after.accept(t);
        };
    }
}