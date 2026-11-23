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

import java.lang.management.ManagementFactory;
import java.util.Objects;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Associates a JMX object name with the MBean server used for registration and unregistration.
 *
 * <p>
 * Factories that accept only a name use the platform MBean server, which is obtained once when this class is initialized. If that lookup fails,
 * these factories use a {@code null} server; they do not retry the lookup. A caller can instead supply another MBean server.
 * </p>
 *
 * <p>
 * Wrapping a name does not register an MBean. A {@code null} wrapped object name or server disables registration and unregistration through the wrapper.
 * </p>
 */
final class ObjectNameWrapper {

    /**
     * The logger for server lookup, registration, and unregistration failures.
     */
    private static final Log log = LogFactory.getLog(ObjectNameWrapper.class);

    /**
     * The platform MBean server, or {@code null} if it could not be obtained.
     */
    private static final MBeanServer MBEAN_SERVER = getPlatformMBeanServer();

    /**
     * Gets the platform MBean server if available.
     *
     * <p>
     * Exceptions and linkage errors from the lookup are logged at debug level and result in {@code null}.
     * </p>
     *
     * @return The platform MBean server, or {@code null} if the lookup fails.
     */
    private static MBeanServer getPlatformMBeanServer() {
        try {
            return ManagementFactory.getPlatformMBeanServer();
        } catch (final LinkageError | Exception e) {
            // ignore - JMX not available
            log.debug("Failed to get platform MBeanServer", e);
            return null;
        }
    }

    /**
     * Gets the object name from a wrapper, accepting a {@code null} wrapper.
     *
     * @param wrapper The wrapper, may be {@code null}.
     * @return The wrapped object name, or {@code null} if the wrapper or its name is {@code null}.
     */
    static ObjectName unwrap(final ObjectNameWrapper wrapper) {
        return wrapper == null ? null : wrapper.unwrap();
    }

    /**
     * Wraps an object name using the supplied MBean server.
     *
     * @param server The server to use, may be {@code null} to disable registration and unregistration; no platform server fallback is performed.
     * @param objectName The object name, may be {@code null} to disable registration and unregistration.
     * @return A new wrapper using the supplied server and name.
     */
    static ObjectNameWrapper wrap(final MBeanServer server, final ObjectName objectName) {
        return new ObjectNameWrapper(server, objectName);
    }

    /**
     * Wraps an object name using the platform MBean server.
     *
     * @param objectName The object name, may be {@code null} to disable registration and unregistration.
     * @return A new wrapper for the name, associated with the platform MBean server or a {@code null} server if unavailable.
     */
    static ObjectNameWrapper wrap(final ObjectName objectName) {
        return new ObjectNameWrapper(MBEAN_SERVER, objectName);
    }

    /**
     * Parses and wraps a JMX object name using the platform MBean server.
     *
     * @param name The object name to parse, must not be {@code null}.
     * @return A new wrapper for the parsed name, associated with the platform MBean server or a {@code null} server if unavailable.
     * @throws MalformedObjectNameException If the name is not a valid JMX object name.
     * @throws NullPointerException If {@code name} is {@code null}.
     */
    static ObjectNameWrapper wrap(final String name) throws MalformedObjectNameException {
        return wrap(new ObjectName(name));
    }

    /**
     * The object name, or {@code null} to disable registration and unregistration.
     */
    private final ObjectName objectName;

    /**
     * The server to use, or {@code null} to disable registration and unregistration.
     */
    private final MBeanServer server;

    /**
     * Constructs a wrapper without registering an MBean.
     *
     * @param server The server to use, may be {@code null} to disable registration and unregistration.
     * @param objectName The object name, may be {@code null} to disable registration and unregistration.
     */
    private ObjectNameWrapper(final MBeanServer server, final ObjectName objectName) {
        this.server = server;
        this.objectName = objectName;
    }

    /**
     * Attempts to register an object with the associated server using the wrapped name.
     *
     * <p>
     * Does nothing if the server or name is {@code null}. Exceptions and linkage errors thrown by registration are logged at warning level and are
     * not propagated.
     * </p>
     *
     * @param object The object to register as an MBean.
     */
    void registerMBean(final Object object) {
        if (server == null || objectName == null) {
            return;
        }
        try {
            server.registerMBean(object, objectName);
        } catch (final LinkageError | Exception e) {
            log.warn("Failed to complete JMX registration for " + objectName, e);
        }
    }

    /**
     * Converts the wrapped object name to a string.
     *
     * @return The object name as a string, or {@code "null"} if no name is wrapped.
     * @since 2.7.0
     */
    @Override
    public String toString() {
        return Objects.toString(objectName);
    }

    /**
     * Attempts to unregister the wrapped name from the associated server if it is registered.
     *
     * <p>
     * Does nothing if the server or name is {@code null}, or the name is not registered. Exceptions and linkage errors thrown by
     * {@link MBeanServer#unregisterMBean(ObjectName)} are logged at warning level and are not propagated. Failures from the preceding
     * {@link MBeanServer#isRegistered(ObjectName)} check are propagated.
     * </p>
     */
    void unregisterMBean() {
        if (server == null || objectName == null) {
            return;
        }
        if (server.isRegistered(objectName)) {
            try {
                server.unregisterMBean(objectName);
            } catch (final LinkageError | Exception e) {
                log.warn("Failed to complete JMX unregistration for " + objectName, e);
            }
        }
    }

    /**
     * Gets the wrapped object name.
     *
     * @return The wrapped object name, which may be {@code null}.
     */
    ObjectName unwrap() {
        return objectName;
    }

}
