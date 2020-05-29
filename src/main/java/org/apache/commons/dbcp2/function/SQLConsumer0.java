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
 * A SQL task that may throw a SQLException. Implementors define a method without arguments called {@code call()}.
 *
 * <p>
 * The {@code Callable} interface is similar to {@link java.lang.Runnable} but is specialized for SQL Exceptions.
 * </p>
 *
 * @since 2.8.0
 */
@FunctionalInterface
public interface SQLConsumer0 {

    /**
     * Computes a result, or throws an exception if unable to do so.
     *
     * @throws SQLException if unable to compute a result
     */
    void accept() throws SQLException;
}