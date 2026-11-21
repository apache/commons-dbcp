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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Stream;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceAlreadyExistsException;
import javax.management.JMX;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.RuntimeMBeanException;
import javax.management.StandardMBean;

import org.apache.commons.dbcp2.managed.BasicManagedDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests JMX registration and password accessor compatibility for {@link BasicDataSource}.
 */
public class TestBasicDataSourceJmx {

    /** An MXBean that refers to the legacy interface. */
    public static class DataSourceReference implements DataSourceReferenceMXBean {
        private final BasicDataSourceMXBean dataSource;

        /**
         * Constructs a reference holder.
         *
         * @param dataSource the referenced data source.
         */
        public DataSourceReference(final BasicDataSourceMXBean dataSource) {
            this.dataSource = dataSource;
        }

        @Override
        public BasicDataSourceMXBean echo(final BasicDataSourceMXBean value) {
            return value;
        }

        @Override
        public BasicDataSourceMXBean getDataSource() {
            return dataSource;
        }

        @Override
        public BasicDataSourceMXBean[] getDataSources() {
            return new BasicDataSourceMXBean[] {dataSource};
        }
    }

    /** Management interface for a reference holder. */
    public interface DataSourceReferenceMXBean {
        /**
         * Echoes an MXBean reference.
         *
         * @param value the reference.
         * @return the reference.
         */
        BasicDataSourceMXBean echo(BasicDataSourceMXBean value);

        /**
         * Gets the referenced data source.
         *
         * @return the data source.
         */
        BasicDataSourceMXBean getDataSource();

        /**
         * Gets an array of data source references.
         *
         * @return the data sources.
         */
        BasicDataSourceMXBean[] getDataSources();
    }

    private static void assertReference(final MBeanServer server, final ObjectName referenceName, final ObjectName dataSourceName) throws Exception {
        assertEquals(dataSourceName, server.getAttribute(referenceName, "DataSource"));
        assertArrayEquals(new ObjectName[] {dataSourceName}, (ObjectName[]) server.getAttribute(referenceName, "DataSources"));
        final DataSourceReferenceMXBean proxy = JMX.newMXBeanProxy(server, referenceName, DataSourceReferenceMXBean.class);
        assertEquals("userName", proxy.getDataSource().getUserName());
        assertEquals("userName", proxy.echo(proxy.getDataSource()).getUserName());
    }

    private static void assertSafeManagement(final MBeanServer server, final ObjectName objectName, final boolean mxBean) throws Exception {
        final MBeanInfo info = server.getMBeanInfo(objectName);
        assertEquals(DataSourceMXBean.class.getName(), info.getDescriptor().getFieldValue("interfaceClassName"));
        assertEquals(Boolean.toString(mxBean), info.getDescriptor().getFieldValue("mxbean"));
        assertTrue(info.getAttributes().length > 0);
        for (final MBeanAttributeInfo attribute : info.getAttributes()) {
            assertFalse("password".equalsIgnoreCase(attribute.getName()));
        }
        assertFalse(Arrays.stream(info.getOperations()).anyMatch(operation -> "getPassword".equals(operation.getName())));
        assertThrows(AttributeNotFoundException.class, () -> server.getAttribute(objectName, "Password"));
        assertThrows(AttributeNotFoundException.class, () -> server.getAttribute(objectName, "password"));
        assertThrows(AttributeNotFoundException.class, () -> server.setAttribute(objectName, new Attribute("Password", "replacement")));
        assertThrows(AttributeNotFoundException.class, () -> server.setAttribute(objectName, new Attribute("MaxTotal", 9)));
        assertTrue(server.setAttributes(objectName,
                new AttributeList(Arrays.asList(new Attribute("Password", "replacement"), new Attribute("MaxTotal", 9)))).isEmpty());
        assertThrows(ReflectionException.class, () -> server.invoke(objectName, "getPassword", new Object[0], new String[0]));
        assertEquals(Arrays.asList(new Attribute("UserName", "userName"), new Attribute("MaxTotal", 7)),
                server.getAttributes(objectName, new String[] {"UserName", "Password", "MaxTotal"}).asList());
        final DataSourceMXBean proxy = mxBean ? JMX.newMXBeanProxy(server, objectName, DataSourceMXBean.class)
                : JMX.newMBeanProxy(server, objectName, DataSourceMXBean.class);
        assertEquals("userName", proxy.getUserName());
        assertEquals(7, proxy.getMaxTotal());
        assertEquals("schema", proxy.getDefaultSchema());
        assertEquals("jdbc:apache:commons:testdriver", proxy.getUrl());
        assertFalse(proxy.isClosed());
        final BasicDataSourceMXBean legacyProxy = mxBean ? JMX.newMXBeanProxy(server, objectName, BasicDataSourceMXBean.class)
                : JMX.newMBeanProxy(server, objectName, BasicDataSourceMXBean.class);
        assertEquals("userName", legacyProxy.getUserName());
        assertEquals(7, legacyProxy.getMaxTotal());
    }

    private static void configureDataSource(final BasicDataSource dataSource) {
        dataSource.setUsername("userName");
        dataSource.setPassword("password");
        dataSource.setUrl("jdbc:apache:commons:testdriver");
        dataSource.setMaxTotal(7);
        dataSource.setDefaultSchema("schema");
    }

    private static Stream<Arguments> directRegistrations() {
        return Stream.of(BasicDataSource.class, BasicManagedDataSource.class)
                .flatMap(type -> Stream.of(false, true).flatMap(platformServer -> Stream.of(false, true)
                        .map(configuredName -> Arguments.of(type, platformServer, configuredName))));
    }

    private static ObjectName newObjectName() throws MalformedObjectNameException {
        return new ObjectName("org.apache.commons.dbcp2:type=TestBasicDataSourceJmx,name=" + UUID.randomUUID());
    }

    @Test
    void testAutomaticMxBeanWrapperUsesLegacyInterface() throws Exception {
        final MBeanServer server = MBeanServerFactory.newMBeanServer();
        final ObjectName objectName = newObjectName();
        try (BasicDataSource dataSource = new BasicDataSource()) {
            configureDataSource(dataSource);
            server.registerMBean(new StandardMBean(dataSource, null, true), objectName);
            try {
                assertEquals("password", server.getAttribute(objectName, "Password"));
                assertEquals(BasicDataSourceMXBean.class.getName(), server.getMBeanInfo(objectName).getDescriptor().getFieldValue("interfaceClassName"));
            } finally {
                server.unregisterMBean(objectName);
            }
        }
    }

    @Test
    void testCloseAfterDeregistrationDoesNotUnregisterReplacement() throws Exception {
        final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        final ObjectName objectName = newObjectName();
        try (BasicDataSource dataSource = new BasicDataSource(); BasicDataSource replacement = new BasicDataSource()) {
            configureDataSource(replacement);
            server.registerMBean(dataSource, objectName);
            server.unregisterMBean(objectName);
            assertNull(dataSource.getRegisteredJmxName());
            server.registerMBean(replacement, objectName);
            dataSource.close();
            assertSafeManagement(server, objectName, true);
        }
        assertFalse(server.isRegistered(objectName));
    }

    @Test
    void testCloseRemovesMxBeanReference() throws Exception {
        final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        final ObjectName objectName = newObjectName();
        final ObjectName referenceName = newObjectName();
        try (BasicDataSource dataSource = new BasicDataSource()) {
            configureDataSource(dataSource);
            server.registerMBean(dataSource, objectName);
            server.registerMBean(new DataSourceReference(dataSource), referenceName);
            try {
                assertReference(server, referenceName, objectName);
                dataSource.close();
                assertFalse(server.isRegistered(objectName));
                assertThrows(MBeanException.class, () -> server.getAttribute(referenceName, "DataSource"));
            } finally {
                server.unregisterMBean(referenceName);
            }
        }
    }

    @ParameterizedTest
    @ValueSource(classes = {BasicDataSource.class, BasicManagedDataSource.class})
    void testCloseUnregistersFromOwningServer(final Class<? extends BasicDataSource> type) throws Exception {
        final MBeanServer platform = ManagementFactory.getPlatformMBeanServer();
        final MBeanServer local = MBeanServerFactory.newMBeanServer();
        final ObjectName objectName = newObjectName();
        try (BasicDataSource platformDataSource = type.getConstructor().newInstance(); BasicDataSource localDataSource = type.getConstructor().newInstance()) {
            configureDataSource(platformDataSource);
            configureDataSource(localDataSource);
            platform.registerMBean(platformDataSource, objectName);
            local.registerMBean(localDataSource, objectName);
            try {
                localDataSource.close();
                assertFalse(local.isRegistered(objectName));
                assertSafeManagement(platform, objectName, true);
            } finally {
                if (local.isRegistered(objectName)) {
                    local.unregisterMBean(objectName);
                }
            }
        }
        assertFalse(platform.isRegistered(objectName));
    }

    @ParameterizedTest
    @ValueSource(classes = {BasicDataSource.class, BasicManagedDataSource.class})
    void testConfiguredNameAllowsNullSuppliedName(final Class<? extends BasicDataSource> type) throws Exception {
        final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        final ObjectName objectName = newObjectName();
        try (BasicDataSource dataSource = type.getConstructor().newInstance()) {
            configureDataSource(dataSource);
            dataSource.setJmxName(objectName.toString());
            assertEquals(objectName, server.registerMBean(dataSource, null).getObjectName());
            assertSafeManagement(server, objectName, true);
        }
        assertFalse(server.isRegistered(objectName));
    }

    @ParameterizedTest
    @ValueSource(classes = {BasicDataSource.class, BasicManagedDataSource.class})
    void testCreateMBean(final Class<? extends BasicDataSource> type) throws Exception {
        final MBeanServer server = MBeanServerFactory.newMBeanServer();
        final ObjectName objectName = newObjectName();
        server.createMBean(type.getName(), objectName);
        try {
            assertEquals(DataSourceMXBean.class.getName(), server.getMBeanInfo(objectName).getDescriptor().getFieldValue("interfaceClassName"));
            assertThrows(AttributeNotFoundException.class, () -> server.getAttribute(objectName, "Password"));
            assertEquals(8, JMX.newMXBeanProxy(server, objectName, DataSourceMXBean.class).getMaxTotal());
        } finally {
            server.unregisterMBean(objectName);
        }
    }

    @ParameterizedTest
    @MethodSource("directRegistrations")
    void testDirectRegistration(final Class<? extends BasicDataSource> type, final boolean platformServer, final boolean configuredName) throws Exception {
        final MBeanServer server = platformServer ? ManagementFactory.getPlatformMBeanServer() : MBeanServerFactory.newMBeanServer();
        final ObjectName suppliedName = newObjectName();
        final ObjectName registeredName = configuredName ? newObjectName() : suppliedName;
        try (BasicDataSource dataSource = type.getConstructor().newInstance()) {
            configureDataSource(dataSource);
            if (configuredName) {
                dataSource.setJmxName(registeredName.toString());
            }
            assertEquals(registeredName, server.registerMBean(dataSource, suppliedName).getObjectName());
            assertEquals(registeredName, dataSource.getRegisteredJmxName());
            assertSafeManagement(server, registeredName, true);
            dataSource.setPassword("changed-password");
            assertSafeManagement(server, registeredName, true);
        }
        assertFalse(server.isRegistered(registeredName));
        assertFalse(server.isRegistered(suppliedName));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testDuplicateRegistrationPreservesOriginal(final boolean anotherServer) throws Exception {
        final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        final MBeanServer duplicateServer = anotherServer ? MBeanServerFactory.newMBeanServer() : server;
        final ObjectName objectName = newObjectName();
        final ObjectName duplicateName = newObjectName();
        final ObjectName referenceName = newObjectName();
        try (BasicDataSource dataSource = new BasicDataSource()) {
            configureDataSource(dataSource);
            server.registerMBean(dataSource, objectName);
            server.registerMBean(new DataSourceReference(dataSource), referenceName);
            try {
                assertThrows(RuntimeMBeanException.class, () -> duplicateServer.registerMBean(dataSource, duplicateName));
                assertFalse(duplicateServer.isRegistered(duplicateName));
                assertSafeManagement(server, objectName, true);
                assertReference(server, referenceName, objectName);
            } finally {
                server.unregisterMBean(referenceName);
            }
        }
        assertFalse(server.isRegistered(objectName));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testEmptyDomainUsesRegistrationServer(final boolean configuredName) throws Exception {
        final MBeanServer server = MBeanServerFactory.newMBeanServer("testDomain");
        final ObjectName shortName = new ObjectName(":" + newObjectName().getKeyPropertyListString());
        final ObjectName registeredName = new ObjectName(server.getDefaultDomain() + shortName);
        final ObjectName referenceName = newObjectName();
        try (BasicDataSource dataSource = new BasicDataSource()) {
            configureDataSource(dataSource);
            if (configuredName) {
                dataSource.setJmxName(shortName.toString());
            }
            assertEquals(registeredName, server.registerMBean(dataSource, configuredName ? newObjectName() : shortName).getObjectName());
            assertEquals(registeredName, dataSource.getRegisteredJmxName());
            server.registerMBean(new DataSourceReference(dataSource), referenceName);
            try {
                assertSafeManagement(server, registeredName, true);
                assertReference(server, referenceName, registeredName);
            } finally {
                server.unregisterMBean(referenceName);
            }
        }
        assertFalse(server.isRegistered(registeredName));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testExplicitLegacyInterfaceIsNotFiltered(final boolean mxBean) throws Exception {
        final MBeanServer server = MBeanServerFactory.newMBeanServer();
        final ObjectName objectName = newObjectName();
        try (BasicDataSource dataSource = new BasicDataSource()) {
            configureDataSource(dataSource);
            // Explicit selection bypasses automatic discovery. Callers must select DataSourceMXBean instead.
            server.registerMBean(new StandardMBean(dataSource, BasicDataSourceMXBean.class, mxBean), objectName);
            try {
                assertEquals("password", server.getAttribute(objectName, "Password"));
                assertEquals(BasicDataSourceMXBean.class.getName(), server.getMBeanInfo(objectName).getDescriptor().getFieldValue("interfaceClassName"));
            } finally {
                server.unregisterMBean(objectName);
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void testExplicitManagementInterface(final boolean mxBean) throws Exception {
        final MBeanServer server = MBeanServerFactory.newMBeanServer();
        final ObjectName objectName = newObjectName();
        try (BasicDataSource dataSource = new BasicDataSource()) {
            configureDataSource(dataSource);
            server.registerMBean(new StandardMBean(dataSource, DataSourceMXBean.class, mxBean), objectName);
            try {
                assertSafeManagement(server, objectName, mxBean);
            } finally {
                server.unregisterMBean(objectName);
            }
        }
    }

    @Test
    void testFailedDirectRegistrationPreservesExistingMxBeanWrapper() throws Exception {
        final MBeanServer server = MBeanServerFactory.newMBeanServer();
        final ObjectName wrapperName = newObjectName();
        final ObjectName directName = newObjectName();
        final ObjectName referenceName = newObjectName();
        try (BasicDataSource dataSource = new BasicDataSource()) {
            configureDataSource(dataSource);
            server.registerMBean(new StandardMBean(dataSource, DataSourceMXBean.class, true), wrapperName);
            server.registerMBean(new DataSourceReference(dataSource), referenceName);
            try {
                assertThrows(RuntimeMBeanException.class, () -> server.registerMBean(dataSource, directName));
                assertFalse(server.isRegistered(directName));
                assertSafeManagement(server, wrapperName, true);
                assertReference(server, referenceName, wrapperName);
                server.unregisterMBean(wrapperName);
                server.registerMBean(dataSource, directName);
                assertReference(server, referenceName, directName);
                assertSafeManagement(server, directName, true);
            } finally {
                server.unregisterMBean(referenceName);
                if (server.isRegistered(wrapperName)) {
                    server.unregisterMBean(wrapperName);
                }
            }
        }
        assertFalse(server.isRegistered(directName));
    }

    @Test
    void testFailedRegistrationCanBeRetried() throws Exception {
        final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        final ObjectName occupiedName = newObjectName();
        final ObjectName retryName = newObjectName();
        try (BasicDataSource incumbent = new BasicDataSource(); BasicDataSource dataSource = new BasicDataSource()) {
            configureDataSource(incumbent);
            configureDataSource(dataSource);
            server.registerMBean(incumbent, occupiedName);
            assertThrows(InstanceAlreadyExistsException.class, () -> server.registerMBean(dataSource, occupiedName));
            assertSafeManagement(server, occupiedName, true);
            server.registerMBean(dataSource, retryName);
            assertSafeManagement(server, retryName, true);
        }
        assertFalse(server.isRegistered(occupiedName));
        assertFalse(server.isRegistered(retryName));
    }

    @Test
    void testFailedRegistrationDoesNotUnregisterIncumbentOnClose() throws Exception {
        final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        final ObjectName objectName = newObjectName();
        try (BasicDataSource incumbent = new BasicDataSource()) {
            configureDataSource(incumbent);
            server.registerMBean(incumbent, objectName);
            try (BasicDataSource dataSource = new BasicDataSource()) {
                assertThrows(InstanceAlreadyExistsException.class, () -> server.registerMBean(dataSource, objectName));
            }
            assertSafeManagement(server, objectName, true);
        }
        assertFalse(server.isRegistered(objectName));
    }

    @Test
    void testInvalidJmxNameUsesSuppliedName() throws Exception {
        final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        final ObjectName objectName = newObjectName();
        try (BasicDataSource dataSource = new BasicDataSource()) {
            configureDataSource(dataSource);
            dataSource.setJmxName("invalid");
            assertEquals(objectName, server.registerMBean(dataSource, objectName).getObjectName());
            assertEquals(objectName, dataSource.getRegisteredJmxName());
            assertSafeManagement(server, objectName, true);
        }
        assertFalse(server.isRegistered(objectName));
    }

    @Test
    void testMissingNameDoesNotPreventRetry() throws Exception {
        final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        final ObjectName objectName = newObjectName();
        try (BasicDataSource dataSource = new BasicDataSource()) {
            configureDataSource(dataSource);
            assertThrows(RuntimeMBeanException.class, () -> server.registerMBean(dataSource, null));
            server.registerMBean(dataSource, objectName);
            assertSafeManagement(server, objectName, true);
        }
        assertFalse(server.isRegistered(objectName));
    }

    @Test
    void testMultipleStandardMBeanWrappers() throws Exception {
        final MBeanServer server = MBeanServerFactory.newMBeanServer();
        final ObjectName firstName = newObjectName();
        final ObjectName secondName = newObjectName();
        try (BasicDataSource dataSource = new BasicDataSource()) {
            configureDataSource(dataSource);
            server.registerMBean(new StandardMBean(dataSource, DataSourceMXBean.class), firstName);
            try {
                server.registerMBean(new StandardMBean(dataSource, DataSourceMXBean.class), secondName);
                try {
                    assertSafeManagement(server, firstName, false);
                    assertSafeManagement(server, secondName, false);
                } finally {
                    server.unregisterMBean(secondName);
                }
                assertSafeManagement(server, firstName, false);
            } finally {
                server.unregisterMBean(firstName);
            }
        }
    }

    @ParameterizedTest
    @ValueSource(classes = {BasicDataSource.class, BasicManagedDataSource.class})
    void testMxBeanReferencesAndReregistration(final Class<? extends BasicDataSource> type) throws Exception {
        final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        final ObjectName firstName = newObjectName();
        final ObjectName secondName = newObjectName();
        final ObjectName referenceName = newObjectName();
        try (BasicDataSource dataSource = type.getConstructor().newInstance()) {
            configureDataSource(dataSource);
            server.registerMBean(dataSource, firstName);
            server.registerMBean(new DataSourceReference(dataSource), referenceName);
            try {
                assertReference(server, referenceName, firstName);
                server.unregisterMBean(firstName);
                server.registerMBean(dataSource, secondName);
                assertReference(server, referenceName, secondName);
                assertSafeManagement(server, secondName, true);
            } finally {
                server.unregisterMBean(referenceName);
            }
        }
        assertFalse(server.isRegistered(firstName));
        assertFalse(server.isRegistered(secondName));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"password", "p\u00e5ss\"<&>"})
    void testPasswordAccessorCompatibility(final String password) throws Exception {
        final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        final ObjectName objectName = newObjectName();
        try (BasicDataSource dataSource = new BasicDataSource()) {
            configureDataSource(dataSource);
            dataSource.setPassword(password);
            // Check both retained Java accessors without introducing calls to deprecated methods.
            final Method interfaceGetter = BasicDataSourceMXBean.class.getMethod("getPassword");
            final Method classGetter = BasicDataSource.class.getMethod("getPassword");
            assertEquals(String.class, interfaceGetter.getReturnType());
            assertEquals(String.class, classGetter.getReturnType());
            assertEquals(password, interfaceGetter.invoke(dataSource));
            assertEquals(password, classGetter.invoke(dataSource));
            server.registerMBean(dataSource, objectName);
            assertSafeManagement(server, objectName, true);
            assertEquals(password, interfaceGetter.invoke(dataSource));
            assertEquals(password, classGetter.invoke(dataSource));
        }
        assertFalse(server.isRegistered(objectName));
    }

    @Test
    void testRegistrationCallbacksWithoutActiveRegistration() throws Exception {
        final MBeanServer server = MBeanServerFactory.newMBeanServer();
        final ObjectName objectName = newObjectName();
        try (BasicDataSource dataSource = new BasicDataSource()) {
            configureDataSource(dataSource);
            dataSource.postDeregister();
            dataSource.postRegister(null);
            dataSource.postRegister(Boolean.FALSE);
            server.registerMBean(dataSource, objectName);
            assertSafeManagement(server, objectName, true);
            server.unregisterMBean(objectName);
            dataSource.postDeregister();
            dataSource.postRegister(Boolean.FALSE);
            server.registerMBean(dataSource, objectName);
            assertSafeManagement(server, objectName, true);
        }
        assertFalse(server.isRegistered(objectName));
    }

    @Test
    void testReregistrationInAnotherServer() throws Exception {
        final MBeanServer firstServer = MBeanServerFactory.newMBeanServer();
        final MBeanServer secondServer = MBeanServerFactory.newMBeanServer();
        final ObjectName objectName = newObjectName();
        final ObjectName referenceName = newObjectName();
        try (BasicDataSource dataSource = new BasicDataSource()) {
            configureDataSource(dataSource);
            firstServer.registerMBean(dataSource, objectName);
            firstServer.registerMBean(new DataSourceReference(dataSource), referenceName);
            try {
                assertReference(firstServer, referenceName, objectName);
                firstServer.unregisterMBean(objectName);
                secondServer.registerMBean(dataSource, objectName);
                secondServer.registerMBean(new DataSourceReference(dataSource), referenceName);
                try {
                    assertThrows(MBeanException.class, () -> firstServer.getAttribute(referenceName, "DataSource"));
                    assertReference(secondServer, referenceName, objectName);
                    assertSafeManagement(secondServer, objectName, true);
                } finally {
                    secondServer.unregisterMBean(referenceName);
                }
            } finally {
                firstServer.unregisterMBean(referenceName);
            }
        }
        assertFalse(firstServer.isRegistered(objectName));
        assertFalse(secondServer.isRegistered(objectName));
    }

    @Test
    void testUrlIsNotRedacted() throws Exception {
        final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        final ObjectName objectName = newObjectName();
        final String url = "jdbc:example:database?password=password";
        try (BasicDataSource dataSource = new BasicDataSource()) {
            configureDataSource(dataSource);
            dataSource.setUrl(url);
            server.registerMBean(dataSource, objectName);
            assertThrows(AttributeNotFoundException.class, () -> server.getAttribute(objectName, "Password"));
            assertEquals(url, server.getAttribute(objectName, "Url"));
        }
        assertFalse(server.isRegistered(objectName));
    }
}
