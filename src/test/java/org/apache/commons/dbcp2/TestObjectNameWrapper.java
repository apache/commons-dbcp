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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.StandardMBean;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests object name handling, server selection, and failure handling for {@link ObjectNameWrapper}.
 */
public class TestObjectNameWrapper {

    /**
     * Initializes a separate wrapper class with unavailable management support, without changing the platform server or system properties.
     */
    private static final class UnavailableManagementClassLoader extends URLClassLoader {
        private final boolean linkageError;
        private int lookups;

        private UnavailableManagementClassLoader(final boolean linkageError) {
            super(new URL[] {ObjectNameWrapper.class.getProtectionDomain().getCodeSource().getLocation()}, TestObjectNameWrapper.class.getClassLoader());
            this.linkageError = linkageError;
        }

        @Override
        protected Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
            if (ManagementFactory.class.getName().equals(name)) {
                lookups++;
                if (linkageError) {
                    throw new NoClassDefFoundError("Management support is unavailable");
                }
                throw new SecurityException("Management access is denied");
            }
            if (!ObjectNameWrapper.class.getName().equals(name)) {
                return super.loadClass(name, resolve);
            }
            synchronized (getClassLoadingLock(name)) {
                Class<?> type = findLoadedClass(name);
                if (type == null) {
                    type = findClass(name);
                }
                if (resolve) {
                    resolveClass(type);
                }
                return type;
            }
        }
    }

    /**
     * Management interface for distinguishing registrations on different servers.
     */
    public interface ValueMBean {
        /**
         * Gets the value identifying this registration.
         *
         * @return The value.
         */
        int getValue();
    }

    private static Method accessibleMethod(final Class<?> type, final String name, final Class<?>... parameterTypes) throws NoSuchMethodException {
        final Method method = type.getDeclaredMethod(name, parameterTypes);
        method.setAccessible(true);
        return method;
    }

    private static String assertLoggedFailure(final String message, final Executable action) {
        StackMessageLog.lock();
        try {
            StackMessageLog.clear();
            assertDoesNotThrow(action);
            final List<String> messages = StackMessageLog.getAll();
            assertEquals(1, messages.size());
            assertTrue(messages.get(0).startsWith(message), messages.toString());
            return messages.get(0);
        } finally {
            StackMessageLog.clear();
            StackMessageLog.unLock();
        }
    }

    private static void assertLoggedFailure(final String message, final Throwable failure, final Executable action) {
        final String logged = assertLoggedFailure(message, action);
        assertTrue(logged.contains(failure.toString()), logged);
    }

    private static ObjectName newObjectName() throws MalformedObjectNameException {
        return new ObjectName("org.apache.commons.dbcp2:type=TestObjectNameWrapper,name=" + UUID.randomUUID());
    }

    private static MBeanServer newServer(final InvocationHandler handler) {
        return (MBeanServer) Proxy.newProxyInstance(MBeanServer.class.getClassLoader(), new Class<?>[] {MBeanServer.class}, handler);
    }

    private static StandardMBean newValueMBean(final int value) throws NotCompliantMBeanException {
        return new StandardMBean((ValueMBean) () -> value, ValueMBean.class);
    }

    private static Stream<Throwable> registrationFailures() {
        return Stream.of(new InstanceAlreadyExistsException("Duplicate name"), new MBeanRegistrationException(new Exception("Registration callback failed")),
                new NotCompliantMBeanException("Invalid MBean"), new SecurityException("Registration denied"), new NoClassDefFoundError("Missing dependency"));
    }

    private static Stream<Throwable> uncheckedFailures() {
        return Stream.of(new SecurityException("Access denied"), new NoClassDefFoundError("Missing dependency"), new AssertionError("Unexpected failure"));
    }

    private static void unregisterIfPresent(final MBeanServer server, final ObjectName name) throws Exception {
        if (server.isRegistered(name)) {
            server.unregisterMBean(name);
        }
    }

    private static Stream<Throwable> unregistrationFailures() {
        return Stream.of(new InstanceNotFoundException("Registration was removed"),
                new MBeanRegistrationException(new Exception("Unregistration callback failed")),
                new SecurityException("Unregistration denied"), new NoClassDefFoundError("Missing dependency"));
    }

    @Test
    void testDuplicateRegistrationPreservesExistingMBean() throws Exception {
        final MBeanServer server = MBeanServerFactory.newMBeanServer();
        final ObjectName name = newObjectName();
        final ObjectNameWrapper wrapper = ObjectNameWrapper.wrap(server, name);
        try {
            wrapper.registerMBean(newValueMBean(1));
            assertTrue(server.isRegistered(name));
            assertLoggedFailure("Failed to complete JMX registration for " + name, new InstanceAlreadyExistsException(),
                    () -> wrapper.registerMBean(newValueMBean(2)));
            assertEquals(1, server.getAttribute(name, "Value"));
            wrapper.unregisterMBean();
            assertFalse(server.isRegistered(name));
            wrapper.registerMBean(newValueMBean(2));
            assertEquals(2, server.getAttribute(name, "Value"));
        } finally {
            unregisterIfPresent(server, name);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testNullNameDisablesServerAccess(final boolean explicitServer) {
        final MBeanServer server = newServer((proxy, method, args) -> {
            throw new AssertionError("A null name must not access the server: " + method.getName());
        });
        final ObjectNameWrapper wrapper = explicitServer ? ObjectNameWrapper.wrap(server, null) : ObjectNameWrapper.wrap((ObjectName) null);
        assertNull(wrapper.unwrap());
        assertNull(ObjectNameWrapper.unwrap(wrapper));
        assertEquals("null", wrapper.toString());
        assertDoesNotThrow(() -> wrapper.registerMBean(new Object()));
        assertDoesNotThrow(wrapper::unregisterMBean);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testNullServerDoesNotFallBackToPlatform(final boolean nullName) throws Exception {
        final MBeanServer platform = ManagementFactory.getPlatformMBeanServer();
        final ObjectName name = newObjectName();
        final ObjectNameWrapper wrapper = ObjectNameWrapper.wrap(null, nullName ? null : name);
        assertSame(nullName ? null : name, wrapper.unwrap());
        assertEquals(nullName ? "null" : name.toString(), wrapper.toString());
        try {
            wrapper.registerMBean(newValueMBean(1));
            assertFalse(platform.isRegistered(name));
            platform.registerMBean(newValueMBean(2), name);
            wrapper.unregisterMBean();
            assertEquals(2, platform.getAttribute(name, "Value"));
        } finally {
            unregisterIfPresent(platform, name);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testPlatformFactories(final boolean stringName) throws Exception {
        final MBeanServer platform = ManagementFactory.getPlatformMBeanServer();
        final ObjectName name = newObjectName();
        final ObjectNameWrapper wrapper = stringName ? ObjectNameWrapper.wrap(name.toString()) : ObjectNameWrapper.wrap(name);
        final ObjectNameWrapper another = stringName ? ObjectNameWrapper.wrap(name.toString()) : ObjectNameWrapper.wrap(name);
        assertNotSame(wrapper, another);
        assertEquals(name, ObjectNameWrapper.unwrap(wrapper));
        if (!stringName) {
            assertSame(name, wrapper.unwrap());
        }
        assertEquals(name.toString(), wrapper.toString());
        assertFalse(platform.isRegistered(name), "Wrapping a name must not register an MBean");
        try {
            wrapper.registerMBean(newValueMBean(42));
            assertEquals(42, platform.getAttribute(name, "Value"));
            another.unregisterMBean();
            assertFalse(platform.isRegistered(name));
            assertDoesNotThrow(wrapper::unregisterMBean);
        } finally {
            unregisterIfPresent(platform, name);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testPlatformLookupFailureIsCached(final boolean linkageError) throws Exception {
        try (UnavailableManagementClassLoader loader = new UnavailableManagementClassLoader(linkageError)) {
            final Class<?> type = Class.forName(ObjectNameWrapper.class.getName(), true, loader);
            assertSame(loader, type.getClassLoader());
            assertEquals(1, loader.lookups);
            final Method wrapObject = accessibleMethod(type, "wrap", ObjectName.class);
            final Method wrapString = accessibleMethod(type, "wrap", String.class);
            final Method wrapServer = accessibleMethod(type, "wrap", MBeanServer.class, ObjectName.class);
            final Method register = accessibleMethod(type, "registerMBean", Object.class);
            final Method unregister = accessibleMethod(type, "unregisterMBean");
            final Method unwrap = accessibleMethod(type, "unwrap");
            final ObjectName name = newObjectName();
            final MBeanServer platform = ManagementFactory.getPlatformMBeanServer();
            final MBeanServer local = MBeanServerFactory.newMBeanServer();
            try {
                final Object first = wrapObject.invoke(null, name);
                final Object second = wrapString.invoke(null, name.toString());
                assertSame(name, unwrap.invoke(first));
                assertEquals(name, unwrap.invoke(second));
                register.invoke(first, newValueMBean(1));
                register.invoke(second, newValueMBean(2));
                assertFalse(platform.isRegistered(name));
                platform.registerMBean(newValueMBean(3), name);
                unregister.invoke(first);
                unregister.invoke(second);
                assertEquals(3, platform.getAttribute(name, "Value"));
                assertEquals(1, loader.lookups, "Factories must reuse the failed platform lookup");

                // Explicit servers remain usable even when the platform lookup failed.
                final Object explicit = wrapServer.invoke(null, local, name);
                register.invoke(explicit, newValueMBean(4));
                assertEquals(4, local.getAttribute(name, "Value"));
                unregister.invoke(explicit);
                assertFalse(local.isRegistered(name));
                assertEquals(3, platform.getAttribute(name, "Value"));

                // Parsing still validates the input when the platform server is unavailable.
                assertEquals(MalformedObjectNameException.class,
                        assertThrows(InvocationTargetException.class, () -> wrapString.invoke(null, "malformed")).getCause().getClass());
                assertEquals(NullPointerException.class,
                        assertThrows(InvocationTargetException.class, () -> wrapString.invoke(null, new Object[] {null})).getCause().getClass());
            } finally {
                unregisterIfPresent(local, name);
                unregisterIfPresent(platform, name);
            }
        }
    }

    @Test
    void testRegisterDoesNotSuppressOtherErrors() throws Exception {
        final AssertionError failure = new AssertionError("Registration failed");
        final MBeanServer server = newServer((proxy, method, args) -> {
            assertEquals("registerMBean", method.getName());
            throw failure;
        });
        final ObjectNameWrapper wrapper = ObjectNameWrapper.wrap(server, newObjectName());
        assertSame(failure, assertThrows(AssertionError.class, () -> wrapper.registerMBean(new Object())));
    }

    @ParameterizedTest
    @MethodSource("registrationFailures")
    void testRegisterLogsAndSuppressesFailures(final Throwable failure) throws Exception {
        final Object bean = new Object();
        final ObjectName name = newObjectName();
        final List<String> calls = new ArrayList<>();
        final MBeanServer server = newServer((proxy, method, args) -> {
            calls.add(method.getName());
            assertEquals("registerMBean", method.getName());
            assertSame(bean, args[0]);
            assertSame(name, args[1]);
            throw failure;
        });
        assertLoggedFailure("Failed to complete JMX registration for " + name, failure, () -> ObjectNameWrapper.wrap(server, name).registerMBean(bean));
        assertEquals(Collections.singletonList("registerMBean"), calls);
    }

    @Test
    void testRegisterNullObjectIsLoggedAndSuppressed() throws Exception {
        final MBeanServer server = MBeanServerFactory.newMBeanServer();
        final ObjectName name = newObjectName();
        assertLoggedFailure("Failed to complete JMX registration for " + name,
                () -> ObjectNameWrapper.wrap(server, name).registerMBean(null));
        assertFalse(server.isRegistered(name));
    }

    @Test
    void testSuppliedServerOwnsRegistrationAndUnregistration() throws Exception {
        final MBeanServer platform = ManagementFactory.getPlatformMBeanServer();
        final MBeanServer local = MBeanServerFactory.newMBeanServer();
        final ObjectName name = newObjectName();
        final ObjectNameWrapper wrapper = ObjectNameWrapper.wrap(local, name);
        assertNotSame(wrapper, ObjectNameWrapper.wrap(local, name));
        assertSame(name, wrapper.unwrap());
        assertSame(name, ObjectNameWrapper.unwrap(wrapper));
        assertEquals(name.toString(), wrapper.toString());
        assertFalse(local.isRegistered(name), "Wrapping a name must not register an MBean");
        try {
            platform.registerMBean(newValueMBean(1), name);
            wrapper.registerMBean(newValueMBean(2));
            assertEquals(1, platform.getAttribute(name, "Value"));
            assertEquals(2, local.getAttribute(name, "Value"));
            wrapper.unregisterMBean();
            assertFalse(local.isRegistered(name));
            assertEquals(1, platform.getAttribute(name, "Value"));
        } finally {
            unregisterIfPresent(local, name);
            unregisterIfPresent(platform, name);
        }
    }

    @Test
    void testToStringPreservesPropertyOrder() throws Exception {
        final String name = "org.apache.commons.dbcp2:z=last,a=first";
        assertEquals(name, ObjectNameWrapper.wrap(name).toString());
    }

    @Test
    void testUnregisterDoesNotSuppressOtherErrors() throws Exception {
        final AssertionError failure = new AssertionError("Unregistration failed");
        final List<String> calls = new ArrayList<>();
        final MBeanServer server = newServer((proxy, method, args) -> {
            calls.add(method.getName());
            if ("isRegistered".equals(method.getName())) {
                return true;
            }
            assertEquals("unregisterMBean", method.getName());
            throw failure;
        });
        final ObjectNameWrapper wrapper = ObjectNameWrapper.wrap(server, newObjectName());
        assertSame(failure, assertThrows(AssertionError.class, wrapper::unregisterMBean));
        assertEquals(Arrays.asList("isRegistered", "unregisterMBean"), calls);
    }

    @ParameterizedTest
    @MethodSource("uncheckedFailures")
    void testUnregisterDoesNotSuppressRegistrationCheckFailures(final Throwable failure) throws Exception {
        final ObjectName name = newObjectName();
        final List<String> calls = new ArrayList<>();
        final MBeanServer server = newServer((proxy, method, args) -> {
            calls.add(method.getName());
            assertEquals("isRegistered", method.getName());
            assertSame(name, args[0]);
            throw failure;
        });
        final ObjectNameWrapper wrapper = ObjectNameWrapper.wrap(server, name);
        assertSame(failure, assertThrows(failure.getClass(), wrapper::unregisterMBean));
        assertEquals(Collections.singletonList("isRegistered"), calls);
    }

    @ParameterizedTest
    @MethodSource("unregistrationFailures")
    void testUnregisterLogsAndSuppressesFailures(final Throwable failure) throws Exception {
        final ObjectName name = newObjectName();
        final List<String> calls = new ArrayList<>();
        final MBeanServer server = newServer((proxy, method, args) -> {
            calls.add(method.getName());
            assertSame(name, args[0]);
            if ("isRegistered".equals(method.getName())) {
                return true;
            }
            assertEquals("unregisterMBean", method.getName());
            throw failure;
        });
        assertLoggedFailure("Failed to complete JMX unregistration for " + name, failure, ObjectNameWrapper.wrap(server, name)::unregisterMBean);
        assertEquals(Arrays.asList("isRegistered", "unregisterMBean"), calls);
    }

    @Test
    void testUnregisterSkipsAbsentName() throws Exception {
        final ObjectName name = newObjectName();
        final List<String> calls = new ArrayList<>();
        final MBeanServer server = newServer((proxy, method, args) -> {
            calls.add(method.getName());
            assertEquals("isRegistered", method.getName());
            assertSame(name, args[0]);
            return false;
        });
        assertDoesNotThrow(ObjectNameWrapper.wrap(server, name)::unregisterMBean);
        assertEquals(Collections.singletonList("isRegistered"), calls);
    }

    @Test
    void testUnwrapNullWrapper() {
        assertNull(ObjectNameWrapper.unwrap(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"malformed", "domain:key", "domain:key=one,key=two", "domain:key=\"unterminated"})
    void testWrapMalformedName(final String name) {
        assertThrows(MalformedObjectNameException.class, () -> ObjectNameWrapper.wrap(name));
    }

    @Test
    void testWrapNullString() {
        assertThrows(NullPointerException.class, () -> ObjectNameWrapper.wrap((String) null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "*:*", "domain:type=*"})
    void testWrapPatternName(final String name) throws Exception {
        final ObjectNameWrapper wrapper = ObjectNameWrapper.wrap(name);
        assertTrue(wrapper.unwrap().isPattern());
        assertEquals(new ObjectName(name), wrapper.unwrap());
    }
}
