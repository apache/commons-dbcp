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
import java.util.function.Function;

/**
 * Represents a SQL function that accepts one argument and produces a result.
 *
 * <p>
 * This is a <a href="package-summary.html">functional interface</a> whose functional method is {@link #apply(Object)}.
 * </p>
 *
 * @param <T> the type of the input to the function
 * @param <R> the type of the result of the function
 *
 * @since 2.8.0
 */
@FunctionalInterface
public interface SQLFunction1<T, R> {

    /**
     * Applies this function to the given argument.
     *
     * @param t the function argument
     * @return the function result
     * @throws SQLException 
     */
    R apply(T t) throws SQLException;

    /**
     * See {@link Function#compose(Function)}.
     *
     * @param <V> the type of input to the {@code before} function, and to the composed function
     * @param before the function to apply before this function is applied
     * @return a composed function that first applies the {@code before} function and then applies this function
     * @throws NullPointerException if before is null
     *
     * @see #andThen(SQLFunction1)
     */
    default <V> SQLFunction1<V, R> compose(SQLFunction1<? super V, ? extends T> before) {
        Objects.requireNonNull(before);
        return (V v) -> apply(before.apply(v));
    }

    /**
     * See {@link Function#andThen(Function)}.
     *
     * @param <V> the type of output of the {@code after} function, and of the composed function
     * @param after the function to apply after this function is applied
     * @return a composed function that first applies this function and then applies the {@code after} function
     * @throws NullPointerException if after is null
     *
     * @see #compose(SQLFunction1)
     */
    default <V> SQLFunction1<T, V> andThen(SQLFunction1<? super R, ? extends V> after) {
        Objects.requireNonNull(after);
        return (T t) -> after.apply(apply(t));
    }

    /**
     * Returns a function that always returns its input argument.
     *
     * @param <T> the type of the input and output objects to the function
     * @return a function that always returns its input argument
     */
    static <T> SQLFunction1<T, T> identity() {
        return t -> t;
    }
}
