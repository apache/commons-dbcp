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

import org.apache.commons.dbcp2.function.SQLBooleanSupplier;
import org.apache.commons.dbcp2.function.SQLConsumer0;
import org.apache.commons.dbcp2.function.SQLConsumer1;
import org.apache.commons.dbcp2.function.SQLConsumer2;
import org.apache.commons.dbcp2.function.SQLConsumer3;
import org.apache.commons.dbcp2.function.SQLConsumer4;
import org.apache.commons.dbcp2.function.SQLConsumer5;
import org.apache.commons.dbcp2.function.SQLFunction0;
import org.apache.commons.dbcp2.function.SQLFunction1;
import org.apache.commons.dbcp2.function.SQLFunction2;
import org.apache.commons.dbcp2.function.SQLFunction3;
import org.apache.commons.dbcp2.function.SQLIntConsumer;
import org.apache.commons.dbcp2.function.SQLIntFunction;
import org.apache.commons.dbcp2.function.SQLIntSupplier;

/**
 * Functional helpers.
 *
 * @since 2.8.0
 */
public class ResourceFunctions {

    protected void accept(final SQLConsumer0 runnable) throws SQLException {
        checkOpen();
        markUse();
        try {
            runnable.accept();
        } catch (final SQLException e) {
            handleException(e);
        }
    }

    protected <T> void accept(final SQLConsumer1<T> consumer, final T value) throws SQLException {
        checkOpen();
        markUse();
        try {
            consumer.accept(value);
        } catch (final SQLException e) {
            handleException(e);
        }
    }

    protected <T, U> void accept(final SQLConsumer2<T, U> consumer, final T t, final U u) throws SQLException {
        checkOpen();
        markUse();
        try {
            consumer.accept(t, u);
        } catch (final SQLException e) {
            handleException(e);
        }
    }

    protected <T, U, V> void accept(final SQLConsumer3<T, U, V> consumer, final T t, final U u, final V v)
            throws SQLException {
        checkOpen();
        markUse();
        try {
            consumer.accept(t, u, v);
        } catch (final SQLException e) {
            handleException(e);
        }
    }

    protected <T, U, V, W> void accept(final SQLConsumer4<T, U, V, W> consumer, final T t, final U u, final V v,
            final W w) throws SQLException {
        checkOpen();
        markUse();
        try {
            consumer.accept(t, u, v, w);
        } catch (final SQLException e) {
            handleException(e);
        }
    }

    protected <T, U, V, W, X> void accept(final SQLConsumer5<T, U, V, W, X> consumer, final T t, final U u,
            final V v, final W w, final X x) throws SQLException {
        checkOpen();
        markUse();
        try {
            consumer.accept(t, u, v, w, x);
        } catch (final SQLException e) {
            handleException(e);
        }
    }

    protected void acceptInt(final SQLIntConsumer consumer, final int value) throws SQLException {
        checkOpen();
        markUse();
        try {
            consumer.accept(value);
        } catch (final SQLException e) {
            handleException(e);
        }
    }

    protected <T> T apply(final SQLFunction0<T> callable) throws SQLException {
        return applyTo(callable, null);
    }

    protected <T, R> R apply(final SQLFunction1<T, R> function, final T value) throws SQLException {
        return applyTo(function, value, null);
    }

    protected <T, U, R> R apply(final SQLFunction2<T, U, R> function, final T t, final U u) throws SQLException {
        checkOpen();
        markUse();
        try {
            return function.apply(t, u);
        } catch (final SQLException e) {
            handleException(e);
            return null;
        }
    }

    protected <T, U, V, R> R apply(final SQLFunction3<T, U, V, R> function, final T t, final U u, final V v)
            throws SQLException {
        checkOpen();
        markUse();
        try {
            return function.apply(t, u, v);
        } catch (final SQLException e) {
            handleException(e);
            return null;
        }
    }

    protected <R> R apply(final SQLIntFunction<R> function, final int value) throws SQLException {
        return applyIntTo(function, value, null);
    }

    protected <R> R applyIntTo(final SQLIntFunction<R> function, final int value, final R onException)
            throws SQLException {
        checkOpen();
        markUse();
        try {
            return function.apply(value);
        } catch (final SQLException e) {
            handleException(e);
            return onException;
        }
    }

    protected <T> T applyTo(final SQLFunction0<T> callable, final T onException) throws SQLException {
        checkOpen();
        markUse();
        try {
            return callable.apply();
        } catch (final SQLException e) {
            handleException(e);
            return onException;
        }
    }

    protected <T, R> R applyTo(final SQLFunction1<T, R> function, final T value, final R onException)
            throws SQLException {
        checkOpen();
        markUse();
        try {
            return function.apply(value);
        } catch (final SQLException e) {
            handleException(e);
            return onException;
        }
    }

    protected <T, U, R> R applyTo(final SQLFunction2<T, U, R> function, final T t, final U u,
            final R onException) throws SQLException {
        checkOpen();
        markUse();
        try {
            return function.apply(t, u);
        } catch (final SQLException e) {
            handleException(e);
            return onException;
        }
    }

    protected <T, U, V, R> R applyTo(final SQLFunction3<T, U, V, R> function, final T t, final U u,
            final V v, final R onException) throws SQLException {
        checkOpen();
        markUse();
        try {
            return function.apply(t, u, v);
        } catch (final SQLException e) {
            handleException(e);
            return onException;
        }
    }

    protected int applyTo0(final SQLFunction0<Integer> callable) throws SQLException {
        return applyTo(callable, 0);
    }

    protected long applyTo0L(final SQLFunction0<Long> callable) throws SQLException {
        return applyTo(callable, 0L);
    }

    protected boolean applyToFalse(final SQLFunction0<Boolean> callable) throws SQLException {
        return applyTo(callable, false);
    }

    @SuppressWarnings("unused")
    protected void checkOpen() throws SQLException {
        // empty
    }

    protected boolean getAsBoolean(final SQLBooleanSupplier supplier) throws SQLException {
        checkOpen();
        markUse();
        try {
            return supplier.getAsBoolean();
        } catch (final SQLException e) {
            handleException(e);
            return false;
        }
    }

    protected int getAsInt(final SQLIntSupplier supplier) throws SQLException {
        checkOpen();
        markUse();
        try {
            return supplier.getAsInt();
        } catch (final SQLException e) {
            handleException(e);
            return 0;
        }
    }

    @SuppressWarnings("unused")
    protected void handleException(final SQLException e) throws SQLException {
        // empty
    }

    protected void markUse() {
        // empty

    }

}
