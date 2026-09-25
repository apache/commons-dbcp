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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Test suite for {@link DelegatingDatabaseMetaData}.
 */
public class TestDelegatingDatabaseMetaData {

    private TesterConnection testConn;
    private DelegatingConnection<?> conn;
    private DelegatingDatabaseMetaData delegate;
    private DelegatingDatabaseMetaData delegateSpy;
    private DatabaseMetaData obj;

    @BeforeEach
    public void setUp() throws Exception {
        obj = mock(DatabaseMetaData.class);
        testConn = new TesterConnection("test", "test");
        conn = new DelegatingConnection<>(testConn);
        delegate = new DelegatingDatabaseMetaData(conn, obj);
        delegateSpy = Mockito.spy(delegate);
    }

    @Test
    void testAllProceduresAreCallable() throws Exception {
        try {
            delegate.allProceduresAreCallable();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).allProceduresAreCallable();
        // SQLException
        Mockito.when(obj.allProceduresAreCallable()).thenThrow(SQLException.class);
        // The default handler rethrows
        assertThrows(SQLException.class, delegate::allProceduresAreCallable);
    }

    @Test
    void testAllTablesAreSelectable() throws Exception {
        try {
            delegate.allTablesAreSelectable();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).allTablesAreSelectable();
        // SQLException
        Mockito.when(obj.allTablesAreSelectable()).thenThrow(SQLException.class);
        // The default handler rethrows
        assertThrows(SQLException.class, delegate::allTablesAreSelectable);
    }

    @Test
    void testAutoCommitFailureClosesAllResultSets() throws Exception {
        try {
            delegate.autoCommitFailureClosesAllResultSets();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).autoCommitFailureClosesAllResultSets();
        // SQLException
        Mockito.when(obj.autoCommitFailureClosesAllResultSets()).thenThrow(SQLException.class);
        // The default handler rethrows
        assertThrows(SQLException.class, delegate::autoCommitFailureClosesAllResultSets);
    }

    @Test
    void testCheckOpen() throws Exception {
        delegate = new DelegatingDatabaseMetaData(conn, conn.getMetaData());
        final ResultSet rst = delegate.getSchemas();
        assertFalse(rst.isClosed());
        conn.close();
        assertTrue(rst.isClosed());
    }

    @Test
    void testDataDefinitionCausesTransactionCommit() throws Exception {
        try {
            delegate.dataDefinitionCausesTransactionCommit();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).dataDefinitionCausesTransactionCommit();
        // SQLException
        Mockito.when(obj.dataDefinitionCausesTransactionCommit()).thenThrow(SQLException.class);
        // The default handler rethrows
        assertThrows(SQLException.class, delegate::dataDefinitionCausesTransactionCommit);
    }

    @Test
    void testDataDefinitionIgnoredInTransactions() throws Exception {
        try {
            delegate.dataDefinitionIgnoredInTransactions();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).dataDefinitionIgnoredInTransactions();
        // SQLException
        Mockito.when(obj.dataDefinitionIgnoredInTransactions()).thenThrow(SQLException.class);
        // The default handler rethrows
        assertThrows(SQLException.class, delegate::dataDefinitionIgnoredInTransactions);
    }

    @Test
    void testDeletesAreDetectedInteger() throws Exception {
        try {
            delegate.deletesAreDetected(1);
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).deletesAreDetected(1);
        // SQLException
        Mockito.when(obj.deletesAreDetected(1)).thenThrow(SQLException.class);
        // The default handler rethrows
        assertThrows(SQLException.class, () -> delegate.deletesAreDetected(1));
    }

    @Test
    void testDoesMaxRowSizeIncludeBlobs() throws Exception {
        try {
            delegate.doesMaxRowSizeIncludeBlobs();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).doesMaxRowSizeIncludeBlobs();
        // SQLException
        Mockito.when(obj.doesMaxRowSizeIncludeBlobs()).thenThrow(SQLException.class);
        // The default handler rethrows
        assertThrows(SQLException.class, delegate::doesMaxRowSizeIncludeBlobs);
    }

    @Test
    void testGeneratedKeyAlwaysReturned() throws Exception {
        try {
            delegate.generatedKeyAlwaysReturned();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).generatedKeyAlwaysReturned();
        // SQLException
        Mockito.when(obj.generatedKeyAlwaysReturned()).thenThrow(SQLException.class);
        // The default handler rethrows
        assertThrows(SQLException.class, delegate::generatedKeyAlwaysReturned);
    }

    @Test
    void testGetAttributesStringStringStringString() throws Exception {
        try {
            delegate.getAttributes("foo", "foo", "foo", "foo");
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).getAttributes("foo", "foo", "foo", "foo");
    }

    @Test
    void testGetBestRowIdentifierStringStringStringIntegerBoolean() throws Exception {
        try {
            delegate.getBestRowIdentifier("foo", "foo", "foo", 1, Boolean.TRUE);
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).getBestRowIdentifier("foo", "foo", "foo", 1, Boolean.TRUE);
    }

    @Test
    void testGetCatalogs() throws Exception {
        try {
            delegate.getCatalogs();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).getCatalogs();
        // SQLException
        Mockito.when(obj.getCatalogs()).thenThrow(SQLException.class);
        // The default handler rethrows
        assertThrows(SQLException.class, delegate::getCatalogs);
    }

    @Test
    void testGetCatalogSeparator() throws Exception {
        try {
            delegate.getCatalogSeparator();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).getCatalogSeparator();
        // SQLException
        Mockito.when(obj.getCatalogSeparator()).thenThrow(SQLException.class);
        // The default handler rethrows
        assertThrows(SQLException.class, delegate::getCatalogSeparator);
    }

    @Test
    void testGetCatalogTerm() throws Exception {
        try {
            delegate.getCatalogTerm();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).getCatalogTerm();
        // SQLException
        Mockito.when(obj.getCatalogTerm()).thenThrow(SQLException.class);
        // The default handler rethrows
        assertThrows(SQLException.class, delegate::getCatalogTerm);
    }

    @Test
    void testGetClientInfoProperties() throws Exception {
        try {
            delegate.getClientInfoProperties();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).getClientInfoProperties();
        // SQLException
        Mockito.when(obj.getClientInfoProperties()).thenThrow(SQLException.class);
        // The default handler rethrows
        assertThrows(SQLException.class, delegate::getClientInfoProperties);
    }

    @Test
    void testGetColumnPrivilegesStringStringStringString() throws Exception {
        try {
            delegate.getColumnPrivileges("foo", "foo", "foo", "foo");
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).getColumnPrivileges("foo", "foo", "foo", "foo");
    }

    @Test
    void testGetColumnsStringStringStringString() throws Exception {
        try {
            delegate.getColumns("foo", "foo", "foo", "foo");
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).getColumns("foo", "foo", "foo", "foo");
    }

    /**
     * This method is a bit special, and doesn't call the method on the wrapped object, instead returning the connection from the delegate object itself.
     *
     * @throws Exception
     */
    @Test
    void testGetConnection() throws Exception {
        try {
            delegate.getConnection();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(0)).getConnection();
    }

    @Test
    void testGetCrossReferenceStringStringStringStringStringString() throws Exception {
        try {
            delegate.getCrossReference("foo", "foo", "foo", "foo", "foo", "foo");
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).getCrossReference("foo", "foo", "foo", "foo", "foo", "foo");
    }

    @Test
    void testGetDatabaseMajorVersion() throws Exception {
        try {
            delegate.getDatabaseMajorVersion();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).getDatabaseMajorVersion();
        // SQLException
        Mockito.when(obj.getDatabaseMajorVersion()).thenThrow(SQLException.class);
        // The default handler rethrows
        assertThrows(SQLException.class, delegate::getDatabaseMajorVersion);
    }

    @Test
    void testGetDatabaseMinorVersion() throws Exception {
        try {
            delegate.getDatabaseMinorVersion();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).getDatabaseMinorVersion();
        // SQLException
        Mockito.when(obj.getDatabaseMinorVersion()).thenThrow(SQLException.class);
        // The default handler rethrows
        assertThrows(SQLException.class, delegate::getDatabaseMinorVersion);
    }

    @Test
    void testGetDatabaseProductName() throws Exception {
        try {
            delegate.getDatabaseProductName();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).getDatabaseProductName();
        // SQLException
        Mockito.when(obj.getDatabaseProductName()).thenThrow(SQLException.class);
        // The default handler rethrows
        assertThrows(SQLException.class, delegate::getDatabaseProductName);
    }

    @Test
    void testGetDatabaseProductVersion() throws Exception {
        try {
            delegate.getDatabaseProductVersion();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).getDatabaseProductVersion();
    }

    @Test
    void testGetDefaultTransactionIsolation() throws Exception {
        try {
            delegate.getDefaultTransactionIsolation();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).getDefaultTransactionIsolation();
    }

    @Test
    void testGetDelegate() throws Exception {
        assertEquals(obj, delegate.getDelegate());
    }

    @Test
    void testGetDriverMajorVersion() throws Exception {
        delegate.getDriverMajorVersion();
        verify(obj, times(1)).getDriverMajorVersion();
    }

    @Test
    void testGetDriverMinorVersion() throws Exception {
        delegate.getDriverMinorVersion();
        verify(obj, times(1)).getDriverMinorVersion();
    }

    @Test
    void testGetDriverName() throws Exception {
        try {
            delegate.getDriverName();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).getDriverName();
    }

    @Test
    void testGetDriverVersion() throws Exception {
        try {
            delegate.getDriverVersion();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).getDriverVersion();
    }

    @Test
    void testGetExportedKeysStringStringString() throws Exception {
        try {
            delegate.getExportedKeys("foo", "foo", "foo");
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).getExportedKeys("foo", "foo", "foo");
    }

    @Test
    void testGetExtraNameCharacters() throws Exception {
        try {
            delegate.getExtraNameCharacters();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).getExtraNameCharacters();
    }

    @Test
    void testGetFunctionColumnsStringStringStringString() throws Exception {
        try {
            delegate.getFunctionColumns("foo", "foo", "foo", "foo");
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).getFunctionColumns("foo", "foo", "foo", "foo");
    }

    @Test
    void testGetFunctionsStringStringString() throws Exception {
        try {
            delegate.getFunctions("foo", "foo", "foo");
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).getFunctions("foo", "foo", "foo");
    }

    @Test
    void testGetIdentifierQuoteString() throws Exception {
        try {
            delegate.getIdentifierQuoteString();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).getIdentifierQuoteString();
    }

    @Test
    void testGetImportedKeysStringStringString() throws Exception {
        try {
            delegate.getImportedKeys("foo", "foo", "foo");
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).getImportedKeys("foo", "foo", "foo");
    }

    @Test
    void testGetIndexInfoStringStringStringBooleanBoolean() throws Exception {
        try {
            delegate.getIndexInfo("foo", "foo", "foo", Boolean.TRUE, Boolean.TRUE);
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).getIndexInfo("foo", "foo", "foo", Boolean.TRUE, Boolean.TRUE);
    }

    @Test
    void testGetInnermostDelegate() {
        assertNotNull(delegate.getInnermostDelegate());
    }

    @Test
    void testGetJDBCMajorVersion() throws Exception {
        try {
            delegate.getJDBCMajorVersion();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).getJDBCMajorVersion();
    }

    @Test
    void testGetJDBCMinorVersion() throws Exception {
        try {
            delegate.getJDBCMinorVersion();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).getJDBCMinorVersion();
    }

    @Test
    void testGetMaxBinaryLiteralLength() throws Exception {
        try {
            delegate.getMaxBinaryLiteralLength();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).getMaxBinaryLiteralLength();
    }

    @Test
    void testGetMaxCatalogNameLength() throws Exception {
        try {
            delegate.getMaxCatalogNameLength();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).getMaxCatalogNameLength();
    }

    @Test
    void testGetMaxCharLiteralLength() throws Exception {
        try {
            delegate.getMaxCharLiteralLength();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).getMaxCharLiteralLength();
    }

    @Test
    void testGetMaxColumnNameLength() throws Exception {
        try {
            delegate.getMaxColumnNameLength();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).getMaxColumnNameLength();
    }

    @Test
    void testGetMaxColumnsInGroupBy() throws Exception {
        try {
            delegate.getMaxColumnsInGroupBy();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).getMaxColumnsInGroupBy();
    }

    @Test
    void testGetMaxColumnsInIndex() throws Exception {
        try {
            delegate.getMaxColumnsInIndex();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).getMaxColumnsInIndex();
    }

    @Test
    void testGetMaxColumnsInOrderBy() throws Exception {
        try {
            delegate.getMaxColumnsInOrderBy();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).getMaxColumnsInOrderBy();
    }

    @Test
    void testGetMaxColumnsInSelect() throws Exception {
        try {
            delegate.getMaxColumnsInSelect();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).getMaxColumnsInSelect();
    }

    @Test
    void testGetMaxColumnsInTable() throws Exception {
        try {
            delegate.getMaxColumnsInTable();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).getMaxColumnsInTable();
    }

    @Test
    void testGetMaxConnections() throws Exception {
        try {
            delegate.getMaxConnections();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).getMaxConnections();
    }

    @Test
    void testGetMaxCursorNameLength() throws Exception {
        try {
            delegate.getMaxCursorNameLength();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).getMaxCursorNameLength();
    }

    @Test
    void testGetMaxIndexLength() throws Exception {
        try {
            delegate.getMaxIndexLength();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).getMaxIndexLength();
    }

    @Test
    void testGetMaxLogicalLobSize() throws Exception {
        try {
            delegate.getMaxLogicalLobSize();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).getMaxLogicalLobSize();
    }

    @Test
    void testGetMaxProcedureNameLength() throws Exception {
        try {
            delegate.getMaxProcedureNameLength();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).getMaxProcedureNameLength();
    }

    @Test
    void testGetMaxRowSize() throws Exception {
        try {
            delegate.getMaxRowSize();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).getMaxRowSize();
    }

    @Test
    void testGetMaxSchemaNameLength() throws Exception {
        try {
            delegate.getMaxSchemaNameLength();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).getMaxSchemaNameLength();
    }

    @Test
    void testGetMaxStatementLength() throws Exception {
        try {
            delegate.getMaxStatementLength();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).getMaxStatementLength();
    }

    @Test
    void testGetMaxStatements() throws Exception {
        try {
            delegate.getMaxStatements();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).getMaxStatements();
    }

    @Test
    void testGetMaxTableNameLength() throws Exception {
        try {
            delegate.getMaxTableNameLength();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).getMaxTableNameLength();
    }

    @Test
    void testGetMaxTablesInSelect() throws Exception {
        try {
            delegate.getMaxTablesInSelect();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).getMaxTablesInSelect();
    }

    @Test
    void testGetMaxUserNameLength() throws Exception {
        try {
            delegate.getMaxUserNameLength();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).getMaxUserNameLength();
    }

    @Test
    void testGetNumericFunctions() throws Exception {
        try {
            delegate.getNumericFunctions();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).getNumericFunctions();
    }

    @Test
    void testGetPrimaryKeysStringStringString() throws Exception {
        try {
            delegate.getPrimaryKeys("foo", "foo", "foo");
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).getPrimaryKeys("foo", "foo", "foo");
    }

    @Test
    void testGetProcedureColumnsStringStringStringString() throws Exception {
        try {
            delegate.getProcedureColumns("foo", "foo", "foo", "foo");
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).getProcedureColumns("foo", "foo", "foo", "foo");
    }

    @Test
    void testGetProceduresStringStringString() throws Exception {
        try {
            delegate.getProcedures("foo", "foo", "foo");
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).getProcedures("foo", "foo", "foo");
    }

    @Test
    void testGetProcedureTerm() throws Exception {
        try {
            delegate.getProcedureTerm();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).getProcedureTerm();
    }

    @Test
    void testGetPseudoColumnsStringStringStringString() throws Exception {
        try {
            delegate.getPseudoColumns("foo", "foo", "foo", "foo");
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).getPseudoColumns("foo", "foo", "foo", "foo");
    }

    @Test
    void testGetResultSetHoldability() throws Exception {
        try {
            delegate.getResultSetHoldability();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).getResultSetHoldability();
    }

    @Test
    void testGetRowIdLifetime() throws Exception {
        try {
            delegate.getRowIdLifetime();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).getRowIdLifetime();
    }

    @Test
    void testGetSchemas() throws Exception {
        try {
            delegate.getSchemas();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).getSchemas();
    }

    @Test
    void testGetSchemasStringString() throws Exception {
        try {
            delegate.getSchemas("foo", "foo");
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).getSchemas("foo", "foo");
    }

    @Test
    void testGetSchemaTerm() throws Exception {
        try {
            delegate.getSchemaTerm();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).getSchemaTerm();
    }

    @Test
    void testGetSearchStringEscape() throws Exception {
        try {
            delegate.getSearchStringEscape();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).getSearchStringEscape();
    }

    @Test
    void testGetSQLKeywords() throws Exception {
        try {
            delegate.getSQLKeywords();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).getSQLKeywords();
    }

    @Test
    void testGetSQLStateType() throws Exception {
        try {
            delegate.getSQLStateType();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).getSQLStateType();
    }

    @Test
    void testGetStringFunctions() throws Exception {
        try {
            delegate.getStringFunctions();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).getStringFunctions();
    }

    @Test
    void testGetSuperTablesStringStringString() throws Exception {
        try {
            delegate.getSuperTables("foo", "foo", "foo");
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).getSuperTables("foo", "foo", "foo");
    }

    @Test
    void testGetSuperTypesStringStringString() throws Exception {
        try {
            delegate.getSuperTypes("foo", "foo", "foo");
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).getSuperTypes("foo", "foo", "foo");
    }

    @Test
    void testGetSystemFunctions() throws Exception {
        try {
            delegate.getSystemFunctions();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).getSystemFunctions();
    }

    @Test
    void testGetTablePrivilegesStringStringString() throws Exception {
        try {
            delegate.getTablePrivileges("foo", "foo", "foo");
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).getTablePrivileges("foo", "foo", "foo");
    }

    @Test
    void testGetTablesStringStringStringStringArray() throws Exception {
        try {
            delegate.getTables("foo", "foo", "foo", (String[]) null);
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).getTables("foo", "foo", "foo", (String[]) null);
    }

    @Test
    void testGetTableTypes() throws Exception {
        try {
            delegate.getTableTypes();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).getTableTypes();
    }

    @Test
    void testGetTimeDateFunctions() throws Exception {
        try {
            delegate.getTimeDateFunctions();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).getTimeDateFunctions();
    }

    @Test
    void testGetTypeInfo() throws Exception {
        try {
            delegate.getTypeInfo();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).getTypeInfo();
    }

    @Test
    void testGetUDTsStringStringStringIntegerArray() throws Exception {
        try {
            delegate.getUDTs("foo", "foo", "foo", (int[]) null);
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).getUDTs("foo", "foo", "foo", (int[]) null);
    }

    @Test
    void testGetURL() throws Exception {
        try {
            delegate.getURL();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).getURL();
    }

    @Test
    void testGetUserName() throws Exception {
        try {
            delegate.getUserName();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).getUserName();
    }

    @Test
    void testGetVersionColumnsStringStringString() throws Exception {
        try {
            delegate.getVersionColumns("foo", "foo", "foo");
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).getVersionColumns("foo", "foo", "foo");
    }

    @Test
    void testInsertsAreDetectedInteger() throws Exception {
        try {
            delegate.insertsAreDetected(1);
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).insertsAreDetected(1);
    }

    @Test
    void testIsCatalogAtStart() throws Exception {
        try {
            delegate.isCatalogAtStart();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).isCatalogAtStart();
    }

    @Test
    void testIsReadOnly() throws Exception {
        try {
            delegate.isReadOnly();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).isReadOnly();
    }

    @Test
    void testLocatorsUpdateCopy() throws Exception {
        try {
            delegate.locatorsUpdateCopy();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).locatorsUpdateCopy();
    }

    @Test
    void testNullArguments() throws Exception {
        assertThrows(NullPointerException.class, () -> new DelegatingDatabaseMetaData(null, null));
        assertThrows(NullPointerException.class, () -> new DelegatingDatabaseMetaData(new DelegatingConnection(null), null));
    }

    @Test
    void testNullPlusNonNullIsNull() throws Exception {
        try {
            delegate.nullPlusNonNullIsNull();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).nullPlusNonNullIsNull();
    }

    @Test
    void testNullsAreSortedAtEnd() throws Exception {
        try {
            delegate.nullsAreSortedAtEnd();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).nullsAreSortedAtEnd();
    }

    @Test
    void testNullsAreSortedAtStart() throws Exception {
        try {
            delegate.nullsAreSortedAtStart();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).nullsAreSortedAtStart();
    }

    @Test
    void testNullsAreSortedHigh() throws Exception {
        try {
            delegate.nullsAreSortedHigh();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).nullsAreSortedHigh();
    }

    @Test
    void testNullsAreSortedLow() throws Exception {
        try {
            delegate.nullsAreSortedLow();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).nullsAreSortedLow();
    }

    @Test
    void testOthersDeletesAreVisibleInteger() throws Exception {
        try {
            delegate.othersDeletesAreVisible(1);
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).othersDeletesAreVisible(1);
    }

    @Test
    void testOthersInsertsAreVisibleInteger() throws Exception {
        try {
            delegate.othersInsertsAreVisible(1);
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).othersInsertsAreVisible(1);
    }

    @Test
    void testOthersUpdatesAreVisibleInteger() throws Exception {
        try {
            delegate.othersUpdatesAreVisible(1);
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).othersUpdatesAreVisible(1);
    }

    @Test
    void testOwnDeletesAreVisibleInteger() throws Exception {
        try {
            delegate.ownDeletesAreVisible(1);
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).ownDeletesAreVisible(1);
    }

    @Test
    void testOwnInsertsAreVisibleInteger() throws Exception {
        try {
            delegate.ownInsertsAreVisible(1);
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).ownInsertsAreVisible(1);
    }

    @Test
    void testOwnUpdatesAreVisibleInteger() throws Exception {
        try {
            delegate.ownUpdatesAreVisible(1);
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).ownUpdatesAreVisible(1);
    }

    @Test
    void testStoresLowerCaseIdentifiers() throws Exception {
        try {
            delegate.storesLowerCaseIdentifiers();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).storesLowerCaseIdentifiers();
    }

    @Test
    void testStoresLowerCaseQuotedIdentifiers() throws Exception {
        try {
            delegate.storesLowerCaseQuotedIdentifiers();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).storesLowerCaseQuotedIdentifiers();
    }

    @Test
    void testStoresMixedCaseIdentifiers() throws Exception {
        try {
            delegate.storesMixedCaseIdentifiers();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).storesMixedCaseIdentifiers();
    }

    @Test
    void testStoresMixedCaseQuotedIdentifiers() throws Exception {
        try {
            delegate.storesMixedCaseQuotedIdentifiers();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).storesMixedCaseQuotedIdentifiers();
    }

    @Test
    void testStoresUpperCaseIdentifiers() throws Exception {
        try {
            delegate.storesUpperCaseIdentifiers();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).storesUpperCaseIdentifiers();
    }

    @Test
    void testStoresUpperCaseQuotedIdentifiers() throws Exception {
        try {
            delegate.storesUpperCaseQuotedIdentifiers();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).storesUpperCaseQuotedIdentifiers();
    }

    @Test
    void testSupportsAlterTableWithAddColumn() throws Exception {
        try {
            delegate.supportsAlterTableWithAddColumn();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).supportsAlterTableWithAddColumn();
    }

    @Test
    void testSupportsAlterTableWithDropColumn() throws Exception {
        try {
            delegate.supportsAlterTableWithDropColumn();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).supportsAlterTableWithDropColumn();
    }

    @Test
    void testSupportsANSI92EntryLevelSQL() throws Exception {
        try {
            delegate.supportsANSI92EntryLevelSQL();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).supportsANSI92EntryLevelSQL();
    }

    @Test
    void testSupportsANSI92FullSQL() throws Exception {
        try {
            delegate.supportsANSI92FullSQL();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).supportsANSI92FullSQL();
    }

    @Test
    void testSupportsANSI92IntermediateSQL() throws Exception {
        try {
            delegate.supportsANSI92IntermediateSQL();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).supportsANSI92IntermediateSQL();
    }

    @Test
    void testSupportsBatchUpdates() throws Exception {
        try {
            delegate.supportsBatchUpdates();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).supportsBatchUpdates();
    }

    @Test
    void testSupportsCatalogsInDataManipulation() throws Exception {
        try {
            delegate.supportsCatalogsInDataManipulation();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).supportsCatalogsInDataManipulation();
    }

    @Test
    void testSupportsCatalogsInIndexDefinitions() throws Exception {
        try {
            delegate.supportsCatalogsInIndexDefinitions();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).supportsCatalogsInIndexDefinitions();
    }

    @Test
    void testSupportsCatalogsInPrivilegeDefinitions() throws Exception {
        try {
            delegate.supportsCatalogsInPrivilegeDefinitions();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).supportsCatalogsInPrivilegeDefinitions();
    }

    @Test
    void testSupportsCatalogsInProcedureCalls() throws Exception {
        try {
            delegate.supportsCatalogsInProcedureCalls();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).supportsCatalogsInProcedureCalls();
    }

    @Test
    void testSupportsCatalogsInTableDefinitions() throws Exception {
        try {
            delegate.supportsCatalogsInTableDefinitions();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).supportsCatalogsInTableDefinitions();
    }

    @Test
    void testSupportsColumnAliasing() throws Exception {
        try {
            delegate.supportsColumnAliasing();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).supportsColumnAliasing();
    }

    @Test
    void testSupportsConvert() throws Exception {
        try {
            delegate.supportsConvert();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).supportsConvert();
    }

    @Test
    void testSupportsConvertIntegerInteger() throws Exception {
        try {
            delegate.supportsConvert(1, 1);
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).supportsConvert(1, 1);
    }

    @Test
    void testSupportsCoreSQLGrammar() throws Exception {
        try {
            delegate.supportsCoreSQLGrammar();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).supportsCoreSQLGrammar();
    }

    @Test
    void testSupportsCorrelatedSubqueries() throws Exception {
        try {
            delegate.supportsCorrelatedSubqueries();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).supportsCorrelatedSubqueries();
    }

    @Test
    void testSupportsDataDefinitionAndDataManipulationTransactions() throws Exception {
        try {
            delegate.supportsDataDefinitionAndDataManipulationTransactions();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).supportsDataDefinitionAndDataManipulationTransactions();
    }

    @Test
    void testSupportsDataManipulationTransactionsOnly() throws Exception {
        try {
            delegate.supportsDataManipulationTransactionsOnly();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).supportsDataManipulationTransactionsOnly();
    }

    @Test
    void testSupportsDifferentTableCorrelationNames() throws Exception {
        try {
            delegate.supportsDifferentTableCorrelationNames();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).supportsDifferentTableCorrelationNames();
    }

    @Test
    void testSupportsExpressionsInOrderBy() throws Exception {
        try {
            delegate.supportsExpressionsInOrderBy();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).supportsExpressionsInOrderBy();
    }

    @Test
    void testSupportsExtendedSQLGrammar() throws Exception {
        try {
            delegate.supportsExtendedSQLGrammar();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).supportsExtendedSQLGrammar();
    }

    @Test
    void testSupportsFullOuterJoins() throws Exception {
        try {
            delegate.supportsFullOuterJoins();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).supportsFullOuterJoins();
    }

    @Test
    void testSupportsGetGeneratedKeys() throws Exception {
        try {
            delegate.supportsGetGeneratedKeys();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).supportsGetGeneratedKeys();
    }

    @Test
    void testSupportsGroupBy() throws Exception {
        try {
            delegate.supportsGroupBy();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).supportsGroupBy();
    }

    @Test
    void testSupportsGroupByBeyondSelect() throws Exception {
        try {
            delegate.supportsGroupByBeyondSelect();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).supportsGroupByBeyondSelect();
    }

    @Test
    void testSupportsGroupByUnrelated() throws Exception {
        try {
            delegate.supportsGroupByUnrelated();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).supportsGroupByUnrelated();
    }

    @Test
    void testSupportsIntegrityEnhancementFacility() throws Exception {
        try {
            delegate.supportsIntegrityEnhancementFacility();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).supportsIntegrityEnhancementFacility();
    }

    @Test
    void testSupportsLikeEscapeClause() throws Exception {
        try {
            delegate.supportsLikeEscapeClause();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).supportsLikeEscapeClause();
    }

    @Test
    void testSupportsLimitedOuterJoins() throws Exception {
        try {
            delegate.supportsLimitedOuterJoins();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).supportsLimitedOuterJoins();
    }

    @Test
    void testSupportsMinimumSQLGrammar() throws Exception {
        try {
            delegate.supportsMinimumSQLGrammar();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).supportsMinimumSQLGrammar();
    }

    @Test
    void testSupportsMixedCaseIdentifiers() throws Exception {
        try {
            delegate.supportsMixedCaseIdentifiers();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).supportsMixedCaseIdentifiers();
    }

    @Test
    void testSupportsMixedCaseQuotedIdentifiers() throws Exception {
        try {
            delegate.supportsMixedCaseQuotedIdentifiers();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).supportsMixedCaseQuotedIdentifiers();
    }

    @Test
    void testSupportsMultipleOpenResults() throws Exception {
        try {
            delegate.supportsMultipleOpenResults();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).supportsMultipleOpenResults();
    }

    @Test
    void testSupportsMultipleResultSets() throws Exception {
        try {
            delegate.supportsMultipleResultSets();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).supportsMultipleResultSets();
    }

    @Test
    void testSupportsMultipleTransactions() throws Exception {
        try {
            delegate.supportsMultipleTransactions();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).supportsMultipleTransactions();
    }

    @Test
    void testSupportsNamedParameters() throws Exception {
        try {
            delegate.supportsNamedParameters();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).supportsNamedParameters();
    }

    @Test
    void testSupportsNonNullableColumns() throws Exception {
        try {
            delegate.supportsNonNullableColumns();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).supportsNonNullableColumns();
    }

    @Test
    void testSupportsOpenCursorsAcrossCommit() throws Exception {
        try {
            delegate.supportsOpenCursorsAcrossCommit();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).supportsOpenCursorsAcrossCommit();
    }

    @Test
    void testSupportsOpenCursorsAcrossRollback() throws Exception {
        try {
            delegate.supportsOpenCursorsAcrossRollback();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).supportsOpenCursorsAcrossRollback();
    }

    @Test
    void testSupportsOpenStatementsAcrossCommit() throws Exception {
        try {
            delegate.supportsOpenStatementsAcrossCommit();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).supportsOpenStatementsAcrossCommit();
    }

    @Test
    void testSupportsOpenStatementsAcrossRollback() throws Exception {
        try {
            delegate.supportsOpenStatementsAcrossRollback();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).supportsOpenStatementsAcrossRollback();
    }

    @Test
    void testSupportsOrderByUnrelated() throws Exception {
        try {
            delegate.supportsOrderByUnrelated();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).supportsOrderByUnrelated();
    }

    @Test
    void testSupportsOuterJoins() throws Exception {
        try {
            delegate.supportsOuterJoins();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).supportsOuterJoins();
    }

    @Test
    void testSupportsPositionedDelete() throws Exception {
        try {
            delegate.supportsPositionedDelete();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).supportsPositionedDelete();
    }

    @Test
    void testSupportsPositionedUpdate() throws Exception {
        try {
            delegate.supportsPositionedUpdate();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).supportsPositionedUpdate();
    }

    @Test
    void testSupportsRefCursors() throws Exception {
        try {
            delegate.supportsRefCursors();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).supportsRefCursors();
    }

    @Test
    void testSupportsResultSetConcurrencyIntegerInteger() throws Exception {
        try {
            delegate.supportsResultSetConcurrency(1, 1);
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).supportsResultSetConcurrency(1, 1);
    }

    @Test
    void testSupportsResultSetHoldabilityInteger() throws Exception {
        try {
            delegate.supportsResultSetHoldability(1);
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).supportsResultSetHoldability(1);
    }

    @Test
    void testSupportsResultSetTypeInteger() throws Exception {
        try {
            delegate.supportsResultSetType(1);
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).supportsResultSetType(1);
    }

    @Test
    void testSupportsSavepoints() throws Exception {
        try {
            delegate.supportsSavepoints();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).supportsSavepoints();
    }

    @Test
    void testSupportsSchemasInDataManipulation() throws Exception {
        try {
            delegate.supportsSchemasInDataManipulation();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).supportsSchemasInDataManipulation();
    }

    @Test
    void testSupportsSchemasInIndexDefinitions() throws Exception {
        try {
            delegate.supportsSchemasInIndexDefinitions();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).supportsSchemasInIndexDefinitions();
    }

    @Test
    void testSupportsSchemasInPrivilegeDefinitions() throws Exception {
        try {
            delegate.supportsSchemasInPrivilegeDefinitions();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).supportsSchemasInPrivilegeDefinitions();
    }

    @Test
    void testSupportsSchemasInProcedureCalls() throws Exception {
        try {
            delegate.supportsSchemasInProcedureCalls();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).supportsSchemasInProcedureCalls();
    }

    @Test
    void testSupportsSchemasInTableDefinitions() throws Exception {
        try {
            delegate.supportsSchemasInTableDefinitions();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).supportsSchemasInTableDefinitions();
    }

    @Test
    void testSupportsSelectForUpdate() throws Exception {
        try {
            delegate.supportsSelectForUpdate();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).supportsSelectForUpdate();
    }

    @Test
    void testSupportsStatementPooling() throws Exception {
        try {
            delegate.supportsStatementPooling();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).supportsStatementPooling();
    }

    @Test
    void testSupportsStoredFunctionsUsingCallSyntax() throws Exception {
        try {
            delegate.supportsStoredFunctionsUsingCallSyntax();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).supportsStoredFunctionsUsingCallSyntax();
    }

    @Test
    void testSupportsStoredProcedures() throws Exception {
        try {
            delegate.supportsStoredProcedures();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).supportsStoredProcedures();
    }

    @Test
    void testSupportsSubqueriesInComparisons() throws Exception {
        try {
            delegate.supportsSubqueriesInComparisons();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).supportsSubqueriesInComparisons();
    }

    @Test
    void testSupportsSubqueriesInExists() throws Exception {
        try {
            delegate.supportsSubqueriesInExists();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).supportsSubqueriesInExists();
    }

    @Test
    void testSupportsSubqueriesInIns() throws Exception {
        try {
            delegate.supportsSubqueriesInIns();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).supportsSubqueriesInIns();
    }

    @Test
    void testSupportsSubqueriesInQuantifieds() throws Exception {
        try {
            delegate.supportsSubqueriesInQuantifieds();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).supportsSubqueriesInQuantifieds();
    }

    @Test
    void testSupportsTableCorrelationNames() throws Exception {
        try {
            delegate.supportsTableCorrelationNames();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).supportsTableCorrelationNames();
    }

    @Test
    void testSupportsTransactionIsolationLevelInteger() throws Exception {
        try {
            delegate.supportsTransactionIsolationLevel(1);
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).supportsTransactionIsolationLevel(1);
    }

    @Test
    void testSupportsTransactions() throws Exception {
        try {
            delegate.supportsTransactions();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).supportsTransactions();
    }

    @Test
    void testSupportsUnion() throws Exception {
        try {
            delegate.supportsUnion();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).supportsUnion();
    }

    @Test
    void testSupportsUnionAll() throws Exception {
        try {
            delegate.supportsUnionAll();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).supportsUnionAll();
    }

    @Test
    void testUpdatesAreDetectedInteger() throws Exception {
        try {
            delegate.updatesAreDetected(1);
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).updatesAreDetected(1);
    }

    @Test
    void testUsesLocalFilePerTable() throws Exception {
        try {
            delegate.usesLocalFilePerTable();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).usesLocalFilePerTable();
    }

    @Test
    void testUsesLocalFiles() throws Exception {
        try {
            delegate.usesLocalFiles();
        } catch (final SQLException e) {
            // ignore
        }
        verify(obj, times(1)).usesLocalFiles();
    }

    @Test
    void testWrap() throws SQLException {
        assertEquals(delegate, delegate.unwrap(DatabaseMetaData.class));
        assertEquals(delegate, delegate.unwrap(DelegatingDatabaseMetaData.class));
        assertEquals(obj, delegate.unwrap(obj.getClass()));
        assertNull(delegate.unwrap(String.class));
        assertTrue(delegate.isWrapperFor(DatabaseMetaData.class));
        assertTrue(delegate.isWrapperFor(DelegatingDatabaseMetaData.class));
        assertTrue(delegate.isWrapperFor(obj.getClass()));
        assertFalse(delegate.isWrapperFor(String.class));
    }
}
