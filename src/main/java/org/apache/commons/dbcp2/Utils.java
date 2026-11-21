/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.dbcp2;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.MessageFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.apache.commons.pool2.PooledObject;

/**
 * Utility methods.
 *
 * @since 2.0
 */
public final class Utils {

    /** A function whose evaluation may throw a checked exception. */
    @FunctionalInterface
    interface CheckedFunction<T, R, E extends Exception> {
        R apply(T value) throws E;
    }

    /** A predicate whose evaluation may throw a checked exception. */
    @FunctionalInterface
    interface CheckedPredicate<T, E extends Exception> {
        boolean test(T value) throws E;
    }

    private static final ResourceBundle messages = ResourceBundle
            .getBundle(Utils.class.getPackage().getName() + ".LocalStrings");

    /**
     * Whether the security manager is enabled.
     *
     * @deprecated No replacement.
     */
    @Deprecated
    public static final boolean IS_SECURITY_ENABLED = isSecurityEnabled();

    /**
     * Any SQL State starting with this value is considered a fatal disconnect.
     */
    public static final String DISCONNECTION_SQL_CODE_PREFIX = "08";

    /**
     * SQL codes of fatal connection errors.
     * <ul>
     * <li>57P01 (Admin shutdown)</li>
     * <li>57P02 (Crash shutdown)</li>
     * <li>57P03 (Cannot connect now)</li>
     * <li>01002 (SQL92 disconnect error)</li>
     * <li>JZ0C0 (Sybase disconnect error)</li>
     * <li>JZ0C1 (Sybase disconnect error)</li>
     * </ul>
     *
     * @deprecated Use {@link #getDisconnectionSqlCodes()}.
     */
    @Deprecated
    public static final Set<String> DISCONNECTION_SQL_CODES;

    static final ResultSet[] EMPTY_RESULT_SET_ARRAY = {};

    static final String[] EMPTY_STRING_ARRAY = {};
    static {
        DISCONNECTION_SQL_CODES = new HashSet<>();
        DISCONNECTION_SQL_CODES.add("57P01"); // Admin shutdown
        DISCONNECTION_SQL_CODES.add("57P02"); // Crash shutdown
        DISCONNECTION_SQL_CODES.add("57P03"); // Cannot connect now
        DISCONNECTION_SQL_CODES.add("01002"); // SQL92 disconnect error
        DISCONNECTION_SQL_CODES.add("JZ0C0"); // Sybase disconnect error
        DISCONNECTION_SQL_CODES.add("JZ0C1"); // Sybase disconnect error
    }

    /**
     * Checks for conflicts between two collections.
     * <p>
     * If any overlap is found between the two provided collections, an {@link IllegalArgumentException} is thrown.
     * </p>
     *
     * @param codes1 The first collection of SQL state codes.
     * @param codes2 The second collection of SQL state codes.
     * @throws IllegalArgumentException if any codes overlap between the two collections.
     * @since 2.13.0
     */
    static void checkSqlCodes(final Collection<String> codes1, final Collection<String> codes2) {
        if (codes1 != null && codes2 != null) {
            final Set<String> test = new HashSet<>(codes1);
            test.retainAll(codes2);
            if (!test.isEmpty()) {
                throw new IllegalArgumentException(test + " cannot be in both disconnectionSqlCodes and disconnectionIgnoreSqlCodes.");
            }
        }
    }

    /**
     * Clones the given char[] if not null.
     *
     * @param value may be null.
     * @return A cloned char[] or null.
     */
    public static char[] clone(final char[] value) {
        return value == null ? null : value.clone();
    }

    /**
     * Clones the given {@link Properties} without the standard "user" or "password" entries.
     *
     * @param properties may be null
     * @return A clone of the input without the standard "user" or "password" entries.
     * @since 2.8.0
     */
    public static Properties cloneWithoutCredentials(final Properties properties) {
        if (properties != null) {
            final Properties temp = (Properties) properties.clone();
            temp.remove(Constants.KEY_USER);
            temp.remove(Constants.KEY_PASSWORD);
            return temp;
        }
        return properties;
    }

    /**
     * Closes the given {@link AutoCloseable} and if an exception is caught, then calls {@code exceptionHandler}.
     *
     * @param autoCloseable The resource to close.
     * @param exceptionHandler Consumes exception thrown closing this resource.
     * @since 2.10.0
     */
    public static void close(final AutoCloseable autoCloseable, final Consumer<Exception> exceptionHandler) {
        if (autoCloseable != null) {
            try {
                autoCloseable.close();
            } catch (final Exception e) {
                if (exceptionHandler != null) {
                    exceptionHandler.accept(e);
                }
            }
        }
    }

    /**
     * Closes the AutoCloseable (which may be null).
     *
     * @param autoCloseable An AutoCloseable, may be {@code null}
     * @since 2.6.0
     */
    public static void closeQuietly(final AutoCloseable autoCloseable) {
        close(autoCloseable, null);
    }

    /**
     * Closes the Connection (which may be null).
     *
     * @param connection A Connection, may be {@code null}
     * @deprecated Use {@link #closeQuietly(AutoCloseable)}.
     */
    @Deprecated
    public static void closeQuietly(final Connection connection) {
        closeQuietly((AutoCloseable) connection);
    }

    /**
     * Closes the ResultSet (which may be null).
     *
     * @param resultSet A ResultSet, may be {@code null}
     * @deprecated Use {@link #closeQuietly(AutoCloseable)}.
     */
    @Deprecated
    public static void closeQuietly(final ResultSet resultSet) {
        closeQuietly((AutoCloseable) resultSet);
    }

    /**
     * Closes the Statement (which may be null).
     *
     * @param statement A Statement, may be {@code null}.
     * @deprecated Use {@link #closeQuietly(AutoCloseable)}.
     */
    @Deprecated
    public static void closeQuietly(final Statement statement) {
        closeQuietly((AutoCloseable) statement);
    }

    /**
     * Finds the first matching object in a chain, visiting each object by identity at most once.
     *
     * @param <T> the chain element type.
     * @param start the first object to inspect, which may be null.
     * @param matches tests whether the current object is the desired result.
     * @param next gets the next object in the chain.
     * @return the first match, or null if the chain ends or cycles before a match is found.
     */
    static <T> T findInChain(final T start, final Predicate<? super T> matches, final Function<? super T, ? extends T> next) {
        return Utils.<T, RuntimeException>findInChain(start, matches::test, next::apply, current -> null);
    }

    /**
     * Finds the first matching object in a chain, allowing checked exceptions and caller-specific cycle handling.
     *
     * @param <T> the chain element type.
     * @param <E> the exception type.
     * @param start the first object to inspect, which may be null.
     * @param matches tests whether the current object is the desired result.
     * @param next gets the next object in the chain.
     * @param onCycle handles the first repeated object, returning a result or throwing an exception.
     * @return the first match, null if the chain ends, or the result of the cycle handler.
     * @throws E if the predicate, next function, or cycle handler throws an exception.
     */
    static <T, E extends Exception> T findInChain(final T start, final CheckedPredicate<? super T, E> matches,
            final CheckedFunction<? super T, ? extends T, E> next, final CheckedFunction<? super T, ? extends T, E> onCycle) throws E {
        final Set<T> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        T current = start;
        while (current != null) {
            if (!visited.add(current)) {
                return onCycle.apply(current);
            }
            if (matches.test(current)) {
                return current;
            }
            current = next.apply(current);
        }
        return null;
    }

    /**
     * Gets a copy of SQL codes of fatal connection errors.
     * <ul>
     * <li>57P01 (Admin shutdown)</li>
     * <li>57P02 (Crash shutdown)</li>
     * <li>57P03 (Cannot connect now)</li>
     * <li>01002 (SQL92 disconnect error)</li>
     * <li>JZ0C0 (Sybase disconnect error)</li>
     * <li>JZ0C1 (Sybase disconnect error)</li>
     * </ul>
     *
     * @return A copy SQL codes of fatal connection errors.
     * @since 2.10.0
     */
    public static Set<String> getDisconnectionSqlCodes() {
        return new HashSet<>(DISCONNECTION_SQL_CODES);
    }

    /**
     * Follows a delegate chain, visiting each wrapper by identity at most once.
     *
     * @param <T> the JDBC type.
     * @param <W> the wrapper type.
     * @param start the first object to inspect, which may be null.
     * @param wrapperType the wrappers to traverse.
     * @param getDelegate gets the next delegate, applying the caller's chosen access policy.
     * @return the first non-wrapper delegate, or null if the chain ends in null or contains a cycle.
     */
    static <T, W extends T> T getInnermostDelegate(final T start, final Class<W> wrapperType,
            final Function<? super W, ? extends T> getDelegate) {
        return findInChain(start, current -> !wrapperType.isInstance(current), current -> getDelegate.apply(wrapperType.cast(current)));
    }

    /**
     * Gets the correct i18n message for the given key.
     *
     * @param key The key to look up an i18n message.
     * @return The i18n message.
     */
    public static String getMessage(final String key) {
        return getMessage(key, (Object[]) null);
    }

    /**
     * Gets the correct i18n message for the given key with placeholders replaced by the supplied arguments.
     *
     * @param key A message key.
     * @param args The message arguments.
     * @return An i18n message.
     */
    public static String getMessage(final String key, final Object... args) {
        final String msg = messages.getString(key);
        if (args == null || args.length == 0) {
            return msg;
        }
        final MessageFormat mf = new MessageFormat(msg);
        return mf.format(args, new StringBuffer(), null).toString();
    }

    /**
     * Checks if the given SQL state corresponds to a fatal connection error.
     *
     * @param sqlState The SQL state to check.
     * @return true if the SQL state is a fatal connection error, false otherwise.
     * @since 2.13.0
     */
    static boolean isDisconnectionSqlCode(final String sqlState) {
        return DISCONNECTION_SQL_CODES.contains(sqlState);
    }

    static boolean isEmpty(final Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    static boolean isSecurityEnabled() {
        return System.getSecurityManager() != null;
    }

    /**
     * Converts the given String to a char[].
     *
     * @param value may be null.
     * @return A char[] or null.
     */
    public static char[] toCharArray(final String value) {
        return value != null ? value.toCharArray() : null;
    }

    /**
     * Converts the given char[] to a String.
     *
     * @param value may be null.
     * @return A String or null.
     */
    public static String toString(final char[] value) {
        return value == null ? null : String.valueOf(value);
    }

    /**
     * Throws a LifetimeExceededException if the given pooled object's lifetime has exceeded a maximum duration.
     *
     * @param p           The pooled object to test.
     * @param maxDuration The maximum lifetime.
     * @throws LifetimeExceededException Thrown if the given pooled object's lifetime has exceeded a maximum duration.
     */
    public static void validateLifetime(final PooledObject<?> p, final Duration maxDuration) throws LifetimeExceededException {
        if (maxDuration.compareTo(Duration.ZERO) > 0) {
            final Duration lifetimeDuration = Duration.between(p.getCreateInstant(), Instant.now());
            if (lifetimeDuration.compareTo(maxDuration) > 0) {
                throw new LifetimeExceededException(getMessage("connectionFactory.lifetimeExceeded", lifetimeDuration, maxDuration));
            }
        }
    }

    private Utils() {
        // not instantiable
    }

}
