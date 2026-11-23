/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.dbcp2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for {@link DriverFactory}.
 */
public class TestDriverFactory {

    /**
     * A driver that cannot be instantiated.
     */
    public abstract static class AbstractDriver extends TesterDriver {
    }

    /**
     * A non-driver initialized before it is passed to the factory.
     */
    public static final class AlreadyInitializedNonDriver {
        static {
            INITIALIZED_CLASSES.add(AlreadyInitializedNonDriver.class);
        }

        /**
         * Constructs a non-driver and records the side effect.
         */
        public AlreadyInitializedNonDriver() {
            CONSTRUCTED_CLASSES.add(AlreadyInitializedNonDriver.class);
        }
    }

    /**
     * A driver whose constructor fails.
     */
    public static final class ConstructorFailureDriver extends TesterDriver {
        /**
         * Always throws {@link IllegalStateException}.
         */
        public ConstructorFailureDriver() {
            throw new IllegalStateException("Constructor failed");
        }
    }

    /**
     * A driver whose constructor fails with SQL diagnostics.
     */
    public static final class ConstructorSqlExceptionDriver extends TesterDriver {
        /**
         * Always throws {@link SQLException}.
         *
         * @throws SQLException Thrown always, with a SQLState and vendor code.
         */
        public ConstructorSqlExceptionDriver() throws SQLException {
            throw new SQLException("Constructor failed", "08006", -7);
        }
    }

    /**
     * A driver whose constructor fails without a SQLState.
     */
    public static final class ConstructorSqlExceptionWithoutStateDriver extends TesterDriver {
        /**
         * Always throws {@link SQLException}.
         *
         * @throws SQLException Thrown always, with a vendor code but no SQLState.
         */
        public ConstructorSqlExceptionWithoutStateDriver() throws SQLException {
            throw new SQLException("Constructor failed", null, -7);
        }
    }

    /**
     * Records valid driver initialization and construction through the context class loader.
     */
    public static final class ContextLoaderDriver extends TesterDriver {
        static {
            INITIALIZED_CLASSES.add(ContextLoaderDriver.class);
        }

        /**
         * Constructs a driver and records the side effect.
         */
        public ContextLoaderDriver() {
            CONSTRUCTED_CLASSES.add(ContextLoaderDriver.class);
        }
    }

    /**
     * A driver whose initialization fails through the context class loader.
     */
    public static final class ContextLoaderInitializerFailureDriver extends TesterDriver {
        static {
            failInitialization();
        }
    }

    /**
     * Records initialization and construction through the context class loader.
     */
    public static final class ContextLoaderNonDriver {
        static {
            INITIALIZED_CLASSES.add(ContextLoaderNonDriver.class);
        }

        /**
         * Constructs a non-driver and records the side effect.
         */
        public ContextLoaderNonDriver() {
            CONSTRUCTED_CLASSES.add(ContextLoaderNonDriver.class);
        }
    }

    /**
     * Records valid driver initialization and construction through the default class loader.
     */
    public static final class DefaultLoaderDriver extends TesterDriver {
        static {
            INITIALIZED_CLASSES.add(DefaultLoaderDriver.class);
        }

        /**
         * Constructs a driver and records the side effect.
         */
        public DefaultLoaderDriver() {
            CONSTRUCTED_CLASSES.add(DefaultLoaderDriver.class);
        }
    }

    /**
     * A driver whose initialization fails through the default class loader.
     */
    public static final class DefaultLoaderInitializerFailureDriver extends TesterDriver {
        static {
            failInitialization();
        }
    }

    /**
     * Records initialization and construction through the default class loader.
     */
    public static final class DefaultLoaderNonDriver {
        static {
            INITIALIZED_CLASSES.add(DefaultLoaderNonDriver.class);
        }

        /**
         * Constructs a non-driver and records the side effect.
         */
        public DefaultLoaderNonDriver() {
            CONSTRUCTED_CLASSES.add(DefaultLoaderNonDriver.class);
        }
    }

    /**
     * Records valid driver initialization and construction through an explicit class loader.
     */
    public static final class ExplicitLoaderDriver extends TesterDriver {
        static {
            INITIALIZED_CLASSES.add(ExplicitLoaderDriver.class);
        }

        /**
         * Constructs a driver and records the side effect.
         */
        public ExplicitLoaderDriver() {
            CONSTRUCTED_CLASSES.add(ExplicitLoaderDriver.class);
        }
    }

    /**
     * A driver whose initialization fails through an explicit class loader.
     */
    public static final class ExplicitLoaderInitializerFailureDriver extends TesterDriver {
        static {
            failInitialization();
        }
    }

    /**
     * Records initialization and construction through an explicit class loader.
     */
    public static final class ExplicitLoaderNonDriver {
        static {
            INITIALIZED_CLASSES.add(ExplicitLoaderNonDriver.class);
        }

        /**
         * Constructs a non-driver and records the side effect.
         */
        public ExplicitLoaderNonDriver() {
            CONSTRUCTED_CLASSES.add(ExplicitLoaderNonDriver.class);
        }
    }

    /**
     * Loads selected classes independently of the parent loader.
     */
    private static final class IsolatedClassLoader extends URLClassLoader {
        private final Predicate<String> isolatedClasses;
        private final List<String> requests = new ArrayList<>();

        private IsolatedClassLoader(final Class<?> sourceClass, final Predicate<String> isolatedClasses) {
            super(new URL[] {sourceClass.getProtectionDomain().getCodeSource().getLocation()}, TestDriverFactory.class.getClassLoader());
            this.isolatedClasses = isolatedClasses;
        }

        @Override
        protected Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
            if (!isolatedClasses.test(name)) {
                return super.loadClass(name, resolve);
            }
            synchronized (getClassLoadingLock(name)) {
                requests.add(name);
                Class<?> clazz = findLoadedClass(name);
                if (clazz == null) {
                    clazz = findClass(name);
                }
                if (resolve) {
                    resolveClass(clazz);
                }
                return clazz;
            }
        }
    }

    /**
     * A driver whose definition can be isolated from the application loader.
     */
    public static final class IsolatedDriver extends TesterDriver {
        static {
            TestDriverFactory.recordInitialization(IsolatedDriver.class);
        }

        /**
         * Constructs a driver and records the side effect.
         */
        public IsolatedDriver() {
            TestDriverFactory.recordConstruction(IsolatedDriver.class);
        }
    }

    /**
     * A non-driver whose definition can be isolated from the application loader.
     */
    public static final class IsolatedNonDriver {
        static {
            TestDriverFactory.recordInitialization(IsolatedNonDriver.class);
        }

        /**
         * Constructs a non-driver and records the side effect.
         */
        public IsolatedNonDriver() {
            TestDriverFactory.recordConstruction(IsolatedNonDriver.class);
        }
    }

    private enum LoaderMode {
        DEFAULT(DefaultLoaderDriver.class, DefaultLoaderNonDriver.class, DefaultLoaderInitializerFailureDriver.class),
        EXPLICIT(ExplicitLoaderDriver.class, ExplicitLoaderNonDriver.class, ExplicitLoaderInitializerFailureDriver.class),
        CONTEXT(ContextLoaderDriver.class, ContextLoaderNonDriver.class, ContextLoaderInitializerFailureDriver.class);

        private final Class<? extends Driver> driverClass;
        private final Class<?> nonDriverClass;
        private final Class<? extends Driver> initializerFailureClass;

        LoaderMode(final Class<? extends Driver> driverClass, final Class<?> nonDriverClass, final Class<? extends Driver> initializerFailureClass) {
            this.driverClass = driverClass;
            this.nonDriverClass = nonDriverClass;
            this.initializerFailureClass = initializerFailureClass;
        }
    }

    private static final class LoaderScope implements AutoCloseable {
        private final Thread thread = Thread.currentThread();
        private final ClassLoader original = thread.getContextClassLoader();
        private final LoaderMode mode;
        private final RecordingClassLoader primary;
        private final RecordingClassLoader context;

        private LoaderScope(final BasicDataSource dataSource, final LoaderMode mode) {
            this.mode = mode;
            primary = new RecordingClassLoader(mode == LoaderMode.CONTEXT);
            context = new RecordingClassLoader(mode != LoaderMode.CONTEXT);
            if (mode != LoaderMode.DEFAULT) {
                dataSource.setDriverClassLoader(primary);
            }
            thread.setContextClassLoader(context);
        }

        private void assertUsed(final String className) {
            assertEquals(mode == LoaderMode.DEFAULT ? Collections.emptyList() : Collections.singletonList(className), primary.requests);
            assertEquals(mode == LoaderMode.CONTEXT ? Collections.singletonList(className) : Collections.emptyList(), context.requests);
        }

        @Override
        public void close() {
            thread.setContextClassLoader(original);
        }
    }

    /**
     * Records initialization even though the class has no public constructor.
     */
    public static final class NonDriverWithoutPublicConstructor {
        static {
            INITIALIZED_CLASSES.add(NonDriverWithoutPublicConstructor.class);
        }

        private NonDriverWithoutPublicConstructor() {
        }
    }

    /**
     * A valid driver type without a public constructor.
     */
    public static final class PrivateConstructorDriver extends TesterDriver {
        private PrivateConstructorDriver() {
        }
    }

    private static final class RecordingClassLoader extends ClassLoader {
        private final boolean reject;
        private final List<String> requests = new ArrayList<>();

        private RecordingClassLoader(final boolean reject) {
            super(TestDriverFactory.class.getClassLoader());
            this.reject = reject;
        }

        @Override
        protected Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
            requests.add(name);
            if (reject) {
                throw new ClassNotFoundException(name);
            }
            return super.loadClass(name, resolve);
        }
    }

    /**
     * A driver that fails while inspecting the URL, using the URL as the exception's SQLState.
     */
    public static final class UrlFailureDriver extends TesterDriver {
        /**
         * Always throws {@link SQLException}.
         *
         * @throws SQLException Thrown always.
         */
        @Override
        public boolean acceptsURL(final String url) throws SQLException {
            throw new SQLException("URL validation failed", url, 7);
        }
    }

    // Keep the side effects outside the fixtures and never reset them, since class initialization runs only once.
    private static final Set<Class<?>> INITIALIZED_CLASSES = ConcurrentHashMap.newKeySet();
    private static final Set<Class<?>> CONSTRUCTED_CLASSES = ConcurrentHashMap.newKeySet();
    private static final String URL = "jdbc:apache:commons:testdriver";

    private static void assertConnectionFailure(final SQLException exception) {
        assertEquals("08001", exception.getSQLState());
        assertEquals(0, exception.getErrorCode());
    }

    private static void assertNoSideEffects(final Class<?> clazz) {
        assertFalse(INITIALIZED_CLASSES.contains(clazz), "Unexpected initialization of " + clazz.getName());
        assertFalse(CONSTRUCTED_CLASSES.contains(clazz), "Unexpected construction of " + clazz.getName());
    }

    private static Stream<RuntimeException> classLoadingFailures() {
        return Stream.of(new SecurityException("Access denied"), new IllegalStateException("Class loader failed"));
    }

    private static Stream<Arguments> constructorSqlExceptions() {
        return Arrays.stream(LoaderMode.values()).flatMap(mode -> Stream.of(
                Arguments.of(mode, ConstructorSqlExceptionDriver.class, "08006", "08006"),
                Arguments.of(mode, ConstructorSqlExceptionWithoutStateDriver.class, null, "08001")));
    }

    private static Stream<Arguments> definingLoaderCases() {
        return Stream.of(LoaderMode.EXPLICIT, LoaderMode.CONTEXT)
                .flatMap(mode -> Stream.of(Arguments.of(mode, false), Arguments.of(mode, true)));
    }

    /**
     * Always throws {@link IllegalStateException}.
     *
     * @throws IllegalStateException Thrown always.
     */
    private static void failInitialization() {
        throw new IllegalStateException("Initialization failed");
    }

    private static Stream<Arguments> instantiationFailures() {
        return Arrays.stream(LoaderMode.values()).flatMap(mode -> Stream.of(
                Arguments.of(mode, Driver.class, NoSuchMethodException.class),
                Arguments.of(mode, PrivateConstructorDriver.class, NoSuchMethodException.class),
                Arguments.of(mode, AbstractDriver.class, InstantiationException.class),
                Arguments.of(mode, ConstructorFailureDriver.class, InvocationTargetException.class)));
    }

    private static Stream<Arguments> nonDriverTypes() {
        return Arrays.stream(LoaderMode.values()).flatMap(mode -> Stream.of(String.class, Runnable.class)
                .map(type -> Arguments.of(mode, type)));
    }

    /**
     * Records construction from a fixture defined by another class loader.
     *
     * @param clazz the fixture class that was constructed.
     */
    public static void recordConstruction(final Class<?> clazz) {
        CONSTRUCTED_CLASSES.add(clazz);
    }

    /**
     * Records initialization from a fixture defined by another class loader.
     *
     * @param clazz the fixture class that was initialized.
     */
    public static void recordInitialization(final Class<?> clazz) {
        INITIALIZED_CLASSES.add(clazz);
    }

    private static Stream<Arguments> unsupportedUrls() {
        return Arrays.stream(LoaderMode.values())
                .flatMap(mode -> Stream.of(Arguments.of(mode, "jdbc:unsupported"), Arguments.of(mode, (String) null)));
    }

    private static Stream<Arguments> urlValidationFailures() {
        return Arrays.stream(LoaderMode.values()).flatMap(mode -> Stream.of(
                Arguments.of(mode, null, "08001"),
                Arguments.of(mode, "", ""),
                Arguments.of(mode, "08006", "08006"),
                Arguments.of(mode, "42000", "42000")));
    }

    @ParameterizedTest
    @MethodSource("classLoadingFailures")
    void testClassLoadingFailureDoesNotFallBack(final RuntimeException failure) throws SQLException {
        final Thread thread = Thread.currentThread();
        final ClassLoader original = thread.getContextClassLoader();
        final RecordingClassLoader context = new RecordingClassLoader(false);
        try (BasicDataSource dataSource = new BasicDataSource()) {
            thread.setContextClassLoader(context);
            dataSource.setDriverClassName(TesterDriver.class.getName());
            dataSource.setDriverClassLoader(new ClassLoader(null) {
                /**
                 * Always throws {@link SQLException}.
                 *
                 * @throws SQLException Thrown always.
                 */
                @Override
                protected Class<?> loadClass(final String name, final boolean resolve) {
                    throw failure;
                }
            });
            final SQLException exception = assertThrows(SQLException.class, () -> DriverFactory.createDriver(dataSource));
            assertConnectionFailure(exception);
            assertEquals("Cannot load JDBC driver class '" + TesterDriver.class.getName() + "'", exception.getMessage());
            assertSame(failure, exception.getCause());
            assertTrue(context.requests.isEmpty());
        } finally {
            thread.setContextClassLoader(original);
        }
    }

    @ParameterizedTest
    @MethodSource("constructorSqlExceptions")
    void testConstructorSqlException(final LoaderMode loaderMode, final Class<? extends Driver> driverClass, final String sqlState,
            final String expectedSqlState) throws SQLException {
        try (BasicDataSource dataSource = new BasicDataSource(); LoaderScope loaders = new LoaderScope(dataSource, loaderMode)) {
            dataSource.setDriverClassName(driverClass.getName());
            dataSource.setUrl(URL);
            final SQLException exception = assertThrows(SQLException.class, () -> DriverFactory.createDriver(dataSource));
            assertEquals(expectedSqlState, exception.getSQLState());
            assertEquals(-7, exception.getErrorCode());
            assertEquals("Cannot create JDBC driver of class '" + driverClass.getName() + "' for connect URL '" + URL + "'", exception.getMessage());
            final InvocationTargetException invocationException = assertInstanceOf(InvocationTargetException.class, exception.getCause());
            final SQLException cause = assertInstanceOf(SQLException.class, invocationException.getCause());
            assertEquals("Constructor failed", cause.getMessage());
            assertEquals(sqlState, cause.getSQLState());
            assertEquals(-7, cause.getErrorCode());
            loaders.assertUsed(driverClass.getName());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testContextLoaderWhenDefaultLoaderCannotFindClass(final boolean nonDriver) throws Exception {
        final Thread thread = Thread.currentThread();
        final ClassLoader original = thread.getContextClassLoader();
        final Class<?> driverClass = nonDriver ? DefaultLoaderNonDriver.class : TesterDriver.class;
        final String className = driverClass.getName();
        try (IsolatedClassLoader libraryLoader = new IsolatedClassLoader(BasicDataSource.class, name -> name.startsWith("org.apache.commons.dbcp2."))) {
            assertThrows(ClassNotFoundException.class, () -> libraryLoader.loadClass(className));
            final Class<?> dataSourceClass = libraryLoader.loadClass(BasicDataSource.class.getName());
            assertSame(libraryLoader, dataSourceClass.getClassLoader());
            try (AutoCloseable resource = (AutoCloseable) dataSourceClass.getConstructor().newInstance()) {
                final DataSource dataSource = (DataSource) resource;
                dataSourceClass.getMethod("setDriverClassName", String.class).invoke(resource, className);
                dataSourceClass.getMethod("setUrl", String.class).invoke(resource, URL);
                dataSourceClass.getMethod("setUsername", String.class).invoke(resource, "userName");
                dataSourceClass.getMethod("setPassword", String.class).invoke(resource, "password");
                libraryLoader.requests.clear();
                final RecordingClassLoader context = new RecordingClassLoader(false);
                thread.setContextClassLoader(context);
                if (nonDriver) {
                    assertNoSideEffects(driverClass);
                    final SQLException exception = assertThrows(SQLException.class, dataSource::getConnection);
                    assertConnectionFailure(exception);
                    assertInstanceOf(ClassCastException.class, exception.getCause());
                    assertNoSideEffects(driverClass);
                } else {
                    try (Connection connection = dataSource.getConnection()) {
                        assertTrue(connection.isValid(0));
                    }
                }
                assertTrue(libraryLoader.requests.contains(className), "The default loader must try the configured class first");
                assertTrue(context.requests.contains(className), "The context loader must resolve the otherwise inaccessible class");
            }
        } finally {
            thread.setContextClassLoader(original);
        }
    }

    @ParameterizedTest
    @EnumSource(LoaderMode.class)
    void testCreateDriver(final LoaderMode loaderMode) throws SQLException {
        final Class<? extends Driver> driverClass = loaderMode.driverClass;
        assertFalse(INITIALIZED_CLASSES.contains(driverClass));
        assertFalse(CONSTRUCTED_CLASSES.contains(driverClass));
        try (BasicDataSource dataSource = new BasicDataSource(); LoaderScope loaders = new LoaderScope(dataSource, loaderMode)) {
            dataSource.setDriverClassName(driverClass.getName());
            dataSource.setUrl(URL);
            final Driver driver = DriverFactory.createDriver(dataSource);
            assertSame(driverClass, driver.getClass());
            assertTrue(INITIALIZED_CLASSES.contains(driverClass));
            assertTrue(CONSTRUCTED_CLASSES.contains(driverClass));
            assertTrue(driver.acceptsURL(URL));
            loaders.assertUsed(driverClass.getName());
        }
    }

    @ParameterizedTest(name = "{0}, nonDriver={1}")
    @MethodSource("definingLoaderCases")
    void testDefiningClassLoader(final LoaderMode loaderMode, final boolean nonDriver) throws Exception {
        final Thread thread = Thread.currentThread();
        final ClassLoader original = thread.getContextClassLoader();
        final Class<?> parentClass = nonDriver ? IsolatedNonDriver.class : IsolatedDriver.class;
        final String className = parentClass.getName();
        assertNoSideEffects(parentClass);
        try (IsolatedClassLoader definingLoader = new IsolatedClassLoader(parentClass, className::equals);
                BasicDataSource dataSource = new BasicDataSource()) {
            final Class<?> isolatedClass = Class.forName(className, false, definingLoader);
            assertSame(definingLoader, isolatedClass.getClassLoader());
            assertNotSame(parentClass, isolatedClass);
            assertNoSideEffects(isolatedClass);
            final RecordingClassLoader otherLoader = new RecordingClassLoader(true);
            final boolean contextFallback = loaderMode == LoaderMode.CONTEXT;
            dataSource.setDriverClassLoader(contextFallback ? otherLoader : definingLoader);
            thread.setContextClassLoader(contextFallback ? definingLoader : otherLoader);
            dataSource.setDriverClassName(className);
            dataSource.setUrl(URL);
            if (nonDriver) {
                final SQLException exception = assertThrows(SQLException.class, () -> DriverFactory.createDriver(dataSource));
                assertConnectionFailure(exception);
                assertInstanceOf(ClassCastException.class, exception.getCause());
                assertNoSideEffects(isolatedClass);
            } else {
                final Driver driver = DriverFactory.createDriver(dataSource);
                assertSame(isolatedClass, driver.getClass());
                assertTrue(INITIALIZED_CLASSES.contains(isolatedClass));
                assertTrue(CONSTRUCTED_CLASSES.contains(isolatedClass));
                assertTrue(driver.acceptsURL(URL));
            }
            assertNoSideEffects(parentClass);
            assertEquals(contextFallback ? Collections.singletonList(className) : Collections.emptyList(), otherLoader.requests);
        } finally {
            thread.setContextClassLoader(original);
        }
    }

    @Test
    void testDriverInstanceTakesPrecedence() throws SQLException {
        try (BasicDataSource dataSource = new BasicDataSource()) {
            final Driver driver = new TesterDriver() {
                /**
                 * Always throws {@link AssertionError}.
                 *
                 * @throws AssertionError Thrown always.
                 */
                @Override
                public boolean acceptsURL(final String url) {
                    throw new AssertionError("An explicit driver must bypass URL validation");
                }
            };
            dataSource.setDriver(driver);
            dataSource.setDriverClassName(DefaultLoaderNonDriver.class.getName());
            dataSource.setUrl("jdbc:unsupported");
            dataSource.setDriverClassLoader(new ClassLoader(null) {
                /**
                 * Always throws {@link AssertionError}.
                 *
                 * @throws AssertionError Thrown always.
                 */
                @Override
                protected Class<?> loadClass(final String name, final boolean resolve) {
                    throw new AssertionError("An explicit driver must bypass class loading");
                }
            });
            assertSame(driver, DriverFactory.createDriver(dataSource));
            assertNoSideEffects(DefaultLoaderNonDriver.class);
        }
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = "jdbc:apache:commons:missing-driver")
    void testDriverManagerFailure(final String url) throws SQLException {
        try (BasicDataSource dataSource = new BasicDataSource()) {
            dataSource.setUrl(url);
            final SQLException exception = assertThrows(SQLException.class, () -> DriverFactory.createDriver(dataSource));
            assertConnectionFailure(exception);
            assertEquals("Cannot create JDBC driver of class '' for connect URL '" + url + "'", exception.getMessage());
            final SQLException cause = assertInstanceOf(SQLException.class, exception.getCause());
            assertEquals("08001", cause.getSQLState());
        }
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testDriverManagerLookup(final String className) throws SQLException {
        final String url = "jdbc:apache:commons:driverfactory:" + (className == null ? "null" : "empty");
        final Driver driver = new TesterDriver() {
            @Override
            public boolean acceptsURL(final String candidate) {
                return url.equals(candidate);
            }
        };
        DriverManager.registerDriver(driver);
        try (BasicDataSource dataSource = new BasicDataSource()) {
            dataSource.setDriverClassName(className);
            dataSource.setUrl(url);
            assertSame(driver, DriverFactory.createDriver(dataSource));
        } finally {
            DriverManager.deregisterDriver(driver);
        }
    }

    @ParameterizedTest
    @EnumSource(LoaderMode.class)
    void testInitializerFailure(final LoaderMode loaderMode) throws SQLException {
        try (BasicDataSource dataSource = new BasicDataSource(); LoaderScope loaders = new LoaderScope(dataSource, loaderMode)) {
            dataSource.setDriverClassName(loaderMode.initializerFailureClass.getName());
            dataSource.setUrl(URL);
            final ExceptionInInitializerError error = assertThrows(ExceptionInInitializerError.class, () -> DriverFactory.createDriver(dataSource));
            assertInstanceOf(IllegalStateException.class, error.getCause());
            assertEquals("Initialization failed", error.getCause().getMessage());
            loaders.assertUsed(dataSource.getDriverClassName());
        }
    }

    @ParameterizedTest
    @MethodSource("instantiationFailures")
    void testInstantiationFailure(final LoaderMode loaderMode, final Class<?> driverClass, final Class<? extends Exception> causeType) throws SQLException {
        try (BasicDataSource dataSource = new BasicDataSource(); LoaderScope loaders = new LoaderScope(dataSource, loaderMode)) {
            dataSource.setDriverClassName(driverClass.getName());
            dataSource.setUrl(URL);
            final SQLException exception = assertThrows(SQLException.class, () -> DriverFactory.createDriver(dataSource));
            assertConnectionFailure(exception);
            assertInstanceOf(causeType, exception.getCause());
            if (causeType == InvocationTargetException.class) {
                assertInstanceOf(IllegalStateException.class, exception.getCause().getCause());
                assertEquals("Constructor failed", exception.getCause().getCause().getMessage());
            }
            loaders.assertUsed(driverClass.getName());
        }
    }

    @Test
    void testLinkageErrorDoesNotFallBack() throws SQLException {
        final Thread thread = Thread.currentThread();
        final ClassLoader original = thread.getContextClassLoader();
        final RecordingClassLoader context = new RecordingClassLoader(false);
        final LinkageError failure = new NoClassDefFoundError("Missing dependency");
        try (BasicDataSource dataSource = new BasicDataSource()) {
            thread.setContextClassLoader(context);
            dataSource.setDriverClassName(TesterDriver.class.getName());
            dataSource.setDriverClassLoader(new ClassLoader(null) {
                /**
                 * Always throws {@link SQLException}.
                 *
                 * @throws SQLException Thrown always.
                 */
                @Override
                protected Class<?> loadClass(final String name, final boolean resolve) {
                    throw failure;
                }
            });
            assertSame(failure, assertThrows(LinkageError.class, () -> DriverFactory.createDriver(dataSource)));
            assertTrue(context.requests.isEmpty());
        } finally {
            thread.setContextClassLoader(original);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testMissingDriverClass(final boolean explicitLoader) throws SQLException {
        final Thread thread = Thread.currentThread();
        final ClassLoader original = thread.getContextClassLoader();
        final ClassNotFoundException missing = new ClassNotFoundException("Missing from context loader");
        try (BasicDataSource dataSource = new BasicDataSource()) {
            final String className = "org.apache.commons.dbcp2.MissingDriver";
            final RecordingClassLoader primary = new RecordingClassLoader(true);
            if (explicitLoader) {
                dataSource.setDriverClassLoader(primary);
            }
            thread.setContextClassLoader(new ClassLoader(null) {
                @Override
                protected Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
                    assertEquals(className, name);
                    throw missing;
                }
            });
            dataSource.setDriverClassName(className);
            dataSource.setUrl(URL);
            final SQLException exception = assertThrows(SQLException.class, () -> DriverFactory.createDriver(dataSource));
            assertConnectionFailure(exception);
            assertEquals("Cannot load JDBC driver class '" + className + "'", exception.getMessage());
            assertSame(missing, exception.getCause());
            assertEquals(explicitLoader ? Collections.singletonList(className) : Collections.emptyList(), primary.requests);
        } finally {
            thread.setContextClassLoader(original);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testNullContextClassLoader(final boolean missingClass) throws SQLException {
        final Thread thread = Thread.currentThread();
        final ClassLoader original = thread.getContextClassLoader();
        try (BasicDataSource dataSource = new BasicDataSource()) {
            thread.setContextClassLoader(null);
            dataSource.setDriverClassName(missingClass ? "org.apache.commons.dbcp2.MissingDriver" : TesterDriver.class.getName());
            dataSource.setUrl(URL);
            if (missingClass) {
                assertConnectionFailure(assertThrows(SQLException.class, () -> DriverFactory.createDriver(dataSource)));
            } else {
                assertInstanceOf(TesterDriver.class, DriverFactory.createDriver(dataSource));
            }
        } finally {
            thread.setContextClassLoader(original);
        }
    }

    @ParameterizedTest
    @EnumSource(LoaderMode.class)
    void testRejectAlreadyInitializedNonDriver(final LoaderMode loaderMode) throws Exception {
        final Class<?> nonDriverClass = AlreadyInitializedNonDriver.class;
        Class.forName(nonDriverClass.getName(), true, getClass().getClassLoader());
        assertTrue(INITIALIZED_CLASSES.contains(nonDriverClass));
        assertFalse(CONSTRUCTED_CLASSES.contains(nonDriverClass));
        try (BasicDataSource dataSource = new BasicDataSource(); LoaderScope loaders = new LoaderScope(dataSource, loaderMode)) {
            dataSource.setDriverClassName(nonDriverClass.getName());
            dataSource.setUrl(URL);
            final SQLException exception = assertThrows(SQLException.class, () -> DriverFactory.createDriver(dataSource));
            assertConnectionFailure(exception);
            assertInstanceOf(ClassCastException.class, exception.getCause());
            assertFalse(CONSTRUCTED_CLASSES.contains(nonDriverClass), "An initialized non-driver must not be constructed");
            loaders.assertUsed(nonDriverClass.getName());
        }
    }

    @ParameterizedTest
    @EnumSource(value = LoaderMode.class, names = {"DEFAULT", "EXPLICIT"})
    void testRejectArrayClass(final LoaderMode loaderMode) throws SQLException {
        try (BasicDataSource dataSource = new BasicDataSource(); LoaderScope loaders = new LoaderScope(dataSource, loaderMode)) {
            dataSource.setDriverClassName(String[].class.getName());
            dataSource.setUrl(URL);
            final SQLException exception = assertThrows(SQLException.class, () -> DriverFactory.createDriver(dataSource));
            assertConnectionFailure(exception);
            assertInstanceOf(ClassCastException.class, exception.getCause());
            // Class.forName asks the loader for the component type when resolving an array class.
            loaders.assertUsed(String.class.getName());
        }
    }

    @ParameterizedTest
    @EnumSource(LoaderMode.class)
    void testRejectNonDriverThroughDataSource(final LoaderMode loaderMode) throws SQLException {
        final Class<?> nonDriverClass = loaderMode.nonDriverClass;
        assertNoSideEffects(nonDriverClass);
        try (BasicDataSource dataSource = new BasicDataSource(); LoaderScope loaders = new LoaderScope(dataSource, loaderMode)) {
            dataSource.setDriverClassName(nonDriverClass.getName());
            dataSource.setUrl(URL);
            final SQLException exception = assertThrows(SQLException.class, dataSource::getConnection);
            assertConnectionFailure(exception);
            assertInstanceOf(ClassCastException.class, exception.getCause());
            assertNoSideEffects(nonDriverClass);
            loaders.assertUsed(nonDriverClass.getName());
        }
    }

    @ParameterizedTest
    @MethodSource("nonDriverTypes")
    void testRejectNonDriverTypes(final LoaderMode loaderMode, final Class<?> type) throws SQLException {
        try (BasicDataSource dataSource = new BasicDataSource(); LoaderScope loaders = new LoaderScope(dataSource, loaderMode)) {
            dataSource.setDriverClassName(type.getName());
            dataSource.setUrl(URL);
            final SQLException exception = assertThrows(SQLException.class, () -> DriverFactory.createDriver(dataSource));
            assertConnectionFailure(exception);
            assertInstanceOf(ClassCastException.class, exception.getCause());
            loaders.assertUsed(type.getName());
        }
    }

    @ParameterizedTest
    @EnumSource(LoaderMode.class)
    void testRejectNonDriverWithoutPublicConstructor(final LoaderMode loaderMode) throws SQLException {
        final Class<?> nonDriverClass = NonDriverWithoutPublicConstructor.class;
        assertFalse(INITIALIZED_CLASSES.contains(nonDriverClass));
        try (BasicDataSource dataSource = new BasicDataSource(); LoaderScope loaders = new LoaderScope(dataSource, loaderMode)) {
            dataSource.setDriverClassName(nonDriverClass.getName());
            dataSource.setUrl(URL);
            final SQLException exception = assertThrows(SQLException.class, () -> DriverFactory.createDriver(dataSource));
            assertConnectionFailure(exception);
            assertInstanceOf(ClassCastException.class, exception.getCause());
            assertFalse(INITIALIZED_CLASSES.contains(nonDriverClass), "A non-driver must not be initialized");
            loaders.assertUsed(nonDriverClass.getName());
        }
    }

    @ParameterizedTest
    @EnumSource(LoaderMode.class)
    void testRejectNonDriverWithoutSideEffects(final LoaderMode loaderMode) throws SQLException {
        // Class literals and getName() do not initialize these fixtures.
        final Class<?> nonDriverClass = loaderMode.nonDriverClass;
        assertNoSideEffects(nonDriverClass);
        try (BasicDataSource dataSource = new BasicDataSource(); LoaderScope loaders = new LoaderScope(dataSource, loaderMode)) {
            dataSource.setDriverClassName(nonDriverClass.getName());
            dataSource.setUrl(URL);
            final SQLException exception = assertThrows(SQLException.class, () -> DriverFactory.createDriver(dataSource));
            assertConnectionFailure(exception);
            assertEquals("Cannot create JDBC driver of class '" + nonDriverClass.getName() + "' for connect URL '" + URL + "'", exception.getMessage());
            assertInstanceOf(ClassCastException.class, exception.getCause());
            assertNoSideEffects(nonDriverClass);
            loaders.assertUsed(nonDriverClass.getName());
        }
    }

    @ParameterizedTest
    @MethodSource("unsupportedUrls")
    void testRejectUnsupportedUrl(final LoaderMode loaderMode, final String url) throws SQLException {
        try (BasicDataSource dataSource = new BasicDataSource(); LoaderScope loaders = new LoaderScope(dataSource, loaderMode)) {
            dataSource.setDriverClassName(TesterDriver.class.getName());
            dataSource.setUrl(url);
            final SQLException exception = assertThrows(SQLException.class, () -> DriverFactory.createDriver(dataSource));
            assertConnectionFailure(exception);
            final SQLException cause = assertInstanceOf(SQLException.class, exception.getCause());
            assertEquals("No suitable driver", cause.getMessage());
            assertEquals("08001", cause.getSQLState());
            loaders.assertUsed(dataSource.getDriverClassName());
        }
    }

    @ParameterizedTest
    @MethodSource("urlValidationFailures")
    void testUrlValidationFailure(final LoaderMode loaderMode, final String sqlState, final String expectedSqlState) throws SQLException {
        try (BasicDataSource dataSource = new BasicDataSource(); LoaderScope loaders = new LoaderScope(dataSource, loaderMode)) {
            dataSource.setDriverClassName(UrlFailureDriver.class.getName());
            dataSource.setUrl(sqlState);
            final SQLException exception = assertThrows(SQLException.class, () -> DriverFactory.createDriver(dataSource));
            assertEquals(expectedSqlState, exception.getSQLState());
            assertEquals(7, exception.getErrorCode());
            assertEquals("Cannot create JDBC driver of class '" + UrlFailureDriver.class.getName() + "' for connect URL '" + sqlState + "'",
                    exception.getMessage());
            final SQLException cause = assertInstanceOf(SQLException.class, exception.getCause());
            assertEquals("URL validation failed", cause.getMessage());
            assertEquals(sqlState, cause.getSQLState());
            assertEquals(7, cause.getErrorCode());
            loaders.assertUsed(dataSource.getDriverClassName());
        }
    }
}
