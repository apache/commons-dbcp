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

/**
 * Represents an operation that accepts four input arguments and returns no result. This is the four-arity
 * specialization of {@link SQLConsumer1}. Unlike most other functional interfaces, this is expected to operate via
 * side-effects.
 *
 * <p>
 * This is a <a href="package-summary.html">functional interface</a> whose functional method is
 * {@link #accept(Object, Object, Object, Object, Object)}.
 * </p>
 * 
 * @param <T> the type of the first argument to the operation
 * @param <U> the type of the second argument to the operation
 * @param <V> the type of the third argument to the operation
 * @param <W> the type of the fourth argument to the operation
 * @param <X> the type of the fifth argument to the operation
 *
 * @see java.util.function.Consumer
 * @see java.util.function.BiConsumer
 * @since 2.8.0
 */
@FunctionalInterface
public interface SQLConsumer5<T, U, V, W, X> {

    /**
     * Performs this operation on the given arguments.
     *
     * @param t the first input argument
     * @param u the second input argument
     * @param v the third input argument
     * @param w the fourth input argument
     * @param x the fifth input argument
     * @throws SQLException
     */
    void accept(T t, U u, V v, W w, X x) throws SQLException;

}