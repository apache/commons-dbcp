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
package org.apache.commons.dbcp2.cpdsadapter;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Hashtable;
import java.util.Properties;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.naming.Context;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.naming.spi.NamingManager;
import javax.sql.PooledConnection;

import org.apache.commons.dbcp2.TesterDriver;
import org.apache.commons.dbcp2.datasources.SharedPoolDataSource;
import org.apache.naming.NamingContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests password export, JNDI reconstruction, and serialization compatibility.
 */
class TestDriverAdapterCPDSReference {

    private static final String EXPORT_PASSWORD = "exportPasswordAsRefAddr";

    private static void assertCanConnect(final DriverAdapterCPDS source) throws SQLException {
        assertCanConnect(source.getPooledConnection());
    }

    private static void assertCanConnect(final PooledConnection pooled) throws SQLException {
        try (Connection connection = pooled.getConnection()) {
            assertFalse(connection.isClosed());
        } finally {
            pooled.close();
        }
    }

    private static DriverAdapterCPDS deserialize(final byte[] bytes) throws Exception {
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            return (DriverAdapterCPDS) input.readObject();
        }
    }

    private static DriverAdapterCPDS newDataSource() throws Exception {
        final DriverAdapterCPDS source = new DriverAdapterCPDS();
        source.setDriver(TesterDriver.class.getName());
        source.setUrl("jdbc:apache:commons:testdriver");
        source.setUser("u1");
        source.setPassword("p1");
        return source;
    }

    static Stream<Arguments> optionalSettingArguments() {
        return Stream.of(true, false).flatMap(export -> Stream.of(true, false)
                .map(includeNullAddress -> Arguments.of(export, includeNullAddress)));
    }

    static Stream<Arguments> passwordArguments() {
        return Stream.of(true, false).flatMap(export -> Stream.of(null, "", "p1", "s\u00ebcr\u00e8t\u0000=&\n")
                .flatMap(password -> IntStream.range(0, 4).mapToObj(mode -> Arguments.of(export, password, mode))));
    }

    private static void removeAddress(final Reference reference, final String type) {
        for (int i = 0; i < reference.size(); i++) {
            if (type.equals(reference.get(i).getType())) {
                reference.remove(i);
                return;
            }
        }
    }

    private static DriverAdapterCPDS resolve(final Reference reference) throws Exception {
        return (DriverAdapterCPDS) NamingManager.getObjectInstance(reference, null, null, null);
    }

    private static byte[] serialize(final Object source) throws Exception {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(source);
        }
        return bytes.toByteArray();
    }

    @Test
    void testDefaultReferenceReconstructsUsableDataSource() throws Exception {
        final DriverAdapterCPDS source = newDataSource();
        assertTrue(source.isExportPasswordAsRefAddr());
        final DriverAdapterCPDS resolved = resolve(source.getReference());
        assertNotSame(source, resolved);
        assertEquals("p1", resolved.getPassword());
        assertTrue(resolved.isExportPasswordAsRefAddr());
        assertCanConnect(resolved);
    }

    @Test
    void testExportCanChangeAfterConnectionUse() throws Exception {
        final DriverAdapterCPDS source = newDataSource();
        assertCanConnect(source);
        source.setExportPasswordAsRefAddr(false);
        assertNull(source.getReference().get("password"));
        source.setExportPasswordAsRefAddr(true);
        assertEquals("p1", source.getReference().get("password").getContent());
        assertCanConnect(source);
    }

    @Test
    void testExportDoesNotRedactUrl() throws Exception {
        final DriverAdapterCPDS source = newDataSource();
        final String url = "jdbc:example:database;password=embedded-secret";
        source.setUrl(url);
        source.setExportPasswordAsRefAddr(false);
        final Reference reference = source.getReference();
        assertNull(reference.get("password"));
        assertEquals(url, reference.get("url").getContent());
        assertEquals(url, resolve(reference).getUrl());
    }

    @Test
    void testExportToggleDoesNotChangeExistingReferences() throws Exception {
        final DriverAdapterCPDS source = newDataSource();
        final Reference original = source.getReference();
        source.setExportPasswordAsRefAddr(false);
        final Reference omitted = source.getReference();
        source.setExportPasswordAsRefAddr(true);
        assertEquals("p1", original.get("password").getContent());
        assertEquals("true", original.get(EXPORT_PASSWORD).getContent());
        assertNull(omitted.get("password"));
        assertEquals("false", omitted.get(EXPORT_PASSWORD).getContent());
        assertEquals("p1", source.getReference().get("password").getContent());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testExternalCredentialsWorkWithStatementPooling(final boolean poolStatements) throws Exception {
        final DriverAdapterCPDS source = newDataSource();
        source.setPoolPreparedStatements(poolStatements);
        source.setExportPasswordAsRefAddr(false);
        final DriverAdapterCPDS resolved = resolve(source.getReference());
        assertNull(resolved.getPassword());
        assertCanConnect(resolved.getPooledConnection("u1", "p1"));
        assertNull(resolved.getPassword());
        assertNull(resolved.getReference().get("password"));
    }

    @ParameterizedTest
    @CsvSource({"true, true", "true, false", "false, true", "false, false"})
    void testFactoryOverridesExistingExportSetting(final boolean initialExport, final boolean referenceExport) throws Exception {
        final DriverAdapterCPDS source = newDataSource();
        source.setExportPasswordAsRefAddr(referenceExport);
        final Reference reference = source.getReference();
        final DriverAdapterCPDS factory = newDataSource();
        factory.setExportPasswordAsRefAddr(initialExport);
        assertSame(factory, factory.getObjectInstance(reference, null, null, null));
        assertEquals(referenceExport, factory.isExportPasswordAsRefAddr());
        final Reference exported = factory.getReference();
        assertEquals(Boolean.toString(referenceExport), exported.get(EXPORT_PASSWORD).getContent());
        assertEquals(referenceExport, exported.get("password") != null);
        assertCanConnect(factory);
    }

    @ParameterizedTest
    @CsvSource({"true, true", "TRUE, true", "false, false", "FALSE, false", "invalid, false", "'', false"})
    void testFactoryReadsExportSetting(final String setting, final boolean expected) throws Exception {
        final Reference reference = newDataSource().getReference();
        removeAddress(reference, EXPORT_PASSWORD);
        reference.add(new StringRefAddr(EXPORT_PASSWORD, setting));
        final DriverAdapterCPDS resolved = resolve(reference);
        assertEquals(expected, resolved.isExportPasswordAsRefAddr());
        // An export policy does not prevent importing credentials supplied by the naming provider.
        assertEquals("p1", resolved.getPassword());
        assertCanConnect(resolved);
        assertEquals(expected, resolved.getReference().get("password") != null);
    }

    @ParameterizedTest
    @MethodSource("optionalSettingArguments")
    void testFactoryRetainsSettingWhenAddressHasNoValue(final boolean export, final boolean includeNullAddress) throws Exception {
        final DriverAdapterCPDS factory = new DriverAdapterCPDS();
        factory.setExportPasswordAsRefAddr(export);
        final Reference reference = newDataSource().getReference();
        removeAddress(reference, EXPORT_PASSWORD);
        if (includeNullAddress) {
            reference.add(new StringRefAddr(EXPORT_PASSWORD, null));
        }
        assertSame(factory, factory.getObjectInstance(reference, null, null, null));
        assertEquals(export, factory.isExportPasswordAsRefAddr());
        assertCanConnect(factory);
    }

    @Test
    void testLegacyReferenceRetainsPasswordExport() throws Exception {
        final Reference reference = newDataSource().getReference();
        removeAddress(reference, EXPORT_PASSWORD);
        final DriverAdapterCPDS resolved = resolve(reference);
        assertTrue(resolved.isExportPasswordAsRefAddr());
        assertEquals("p1", resolved.getReference().get("password").getContent());
        assertCanConnect(resolved);
    }

    @Test
    void testLegacySerializedDataSourceRetainsPasswordExport() throws Exception {
        final Properties fixture = new Properties();
        try (InputStream input = getClass().getResourceAsStream("DriverAdapterCPDS-2.14.0.properties")) {
            assertNotNull(input);
            fixture.load(input);
        }
        final DriverAdapterCPDS restored = deserialize(Base64.getDecoder().decode(fixture.getProperty("serializedData")));
        assertTrue(restored.isExportPasswordAsRefAddr());
        assertEquals("u1", restored.getUser());
        assertEquals("p1", restored.getReference().get("password").getContent());
        restored.setDriver(TesterDriver.class.getName());
        assertCanConnect(restored);
    }

    @Test
    void testMissingPasswordFailsWithoutChangingSourceCredentials() throws Exception {
        final DriverAdapterCPDS source = newDataSource();
        source.setExportPasswordAsRefAddr(false);
        final DriverAdapterCPDS resolved = resolve(source.getReference());
        assertThrows(SQLException.class, resolved::getPooledConnection);
        // Even an unsuccessful attempt ends the existing initialization phase.
        assertThrows(IllegalStateException.class, () -> resolved.setPassword("p1"));
        assertCanConnect(source);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testPasswordProvisioningBeforePoolInitialization(final boolean useCharArray) throws Exception {
        final DriverAdapterCPDS source = newDataSource();
        source.setExportPasswordAsRefAddr(false);
        final DriverAdapterCPDS resolved = resolve(source.getReference());
        assertNull(resolved.getPassword());
        if (useCharArray) {
            resolved.setPassword(new char[] {'p', '1'});
        } else {
            resolved.setPassword("p1");
        }
        try (SharedPoolDataSource pool = new SharedPoolDataSource()) {
            pool.setConnectionPoolDataSource(resolved);
            try (Connection connection = pool.getConnection()) {
                assertFalse(connection.isClosed());
            }
        }
        assertNull(resolved.getReference().get("password"));
    }

    @ParameterizedTest
    @MethodSource("passwordArguments")
    void testPasswordReferenceContents(final boolean export, final String password, final int mode) throws Exception {
        final DriverAdapterCPDS source = newDataSource();
        source.setPassword((String) null);
        switch (mode) {
        case 0:
            source.setPassword(password);
            break;
        case 1:
            source.setPassword(password == null ? null : password.toCharArray());
            break;
        default:
            final Properties properties = new Properties();
            if (password != null) {
                properties.setProperty("password", password);
            }
            source.setConnectionProperties(mode == 2 ? properties : new Properties(properties));
            break;
        }
        source.setDescription("reference-test");
        source.setExportPasswordAsRefAddr(export);
        final Properties before = source.getConnectionProperties() == null ? null : (Properties) source.getConnectionProperties().clone();
        final Reference reference = source.getReference();
        assertEquals(Boolean.toString(export), reference.get(EXPORT_PASSWORD).getContent());
        if (export) {
            assertNotNull(reference.get("password"));
            assertEquals(password, reference.get("password").getContent());
        } else {
            assertNull(reference.get("password"));
        }
        assertEquals(password, source.getPassword());
        assertArrayEquals(password == null ? null : password.toCharArray(), source.getPasswordCharArray());
        assertEquals(before, source.getConnectionProperties());
        assertEquals(source.getUser(), reference.get("user").getContent());
        assertEquals(source.getUrl(), reference.get("url").getContent());
        assertEquals(source.getDriver(), reference.get("driver").getContent());
        assertEquals(source.getDescription(), reference.get("description").getContent());
        final DriverAdapterCPDS resolved = resolve(reference);
        assertEquals(export ? password : null, resolved.getPassword());
        assertEquals(export, resolved.isExportPasswordAsRefAddr());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testRebindingPreservesExportSetting(final boolean export) throws Exception {
        DriverAdapterCPDS source = newDataSource();
        source.setExportPasswordAsRefAddr(export);
        final Context context = new NamingContext(new Hashtable<>(), "referenceTest");
        try {
            context.bind("source", source);
            for (int i = 0; i < 3; i++) {
                final DriverAdapterCPDS resolved = (DriverAdapterCPDS) context.lookup("source");
                assertNotSame(source, resolved);
                assertEquals(export, resolved.isExportPasswordAsRefAddr());
                assertEquals(export ? "p1" : null, resolved.getPassword());
                resolved.setPassword("p1");
                assertCanConnect(resolved);
                assertEquals(export, resolved.getReference().get("password") != null);
                context.rebind("source", resolved);
                source = resolved;
            }
        } finally {
            context.close();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testSerializationRetainsPolicyAndCredentials(final boolean export) throws Exception {
        final DriverAdapterCPDS source = newDataSource();
        source.setExportPasswordAsRefAddr(export);
        final DriverAdapterCPDS restored = deserialize(serialize(source));
        assertNotSame(source, restored);
        assertEquals(export, restored.isExportPasswordAsRefAddr());
        assertEquals("p1", restored.getPassword());
        assertEquals(export, restored.getReference().get("password") != null);
        assertCanConnect(restored);
    }
}
