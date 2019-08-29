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
import java.util.function.Function;

/**
 * Represents a SQL function that accepts five arguments and produces a result. This is the five-arity specialization of
 * {@link SQLFunction1}.
 *
 * <p>
 * This is a <a href="package-summary.html">functional interface</a> whose functional method is
 * {@link #apply(Object, Object, Object, Object, Object)}.
 * </p>
 *
 * @param <T> the type of the first argument to the function
 * @param <U> the type of the second argument to the function
 * @param <V> the type of the third argument to the function
 * @param <X> the type of the fourth argument to the function
 * @param <Y> the type of the fourth argument to the function
 * @param <R> the type of the result of the function
 *
 * @see Function
 * @since 2.8.0
 */
@FunctionalInterface
public interface SQLFunction5<T, U, V, X, Y, R> {

    /**
     * Applies this function to the given arguments.
     *
     * @param t the first function argument
     * @param u the second function argument
     * @param v the third function argument
     * @param x the fourth function argument
     * @param y the fourth function argument
     * @return the function result
     * @throws SQLException
     */
    R apply(T t, U u, V v, X x, Y y) throws SQLException;

}