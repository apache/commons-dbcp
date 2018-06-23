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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

import org.junit.Before;
import org.junit.Test;

/**
 * Test suite for {@link DelegatingDatabaseMetaData}.
 */
public class TestDelegatingDatabaseMetaData {

    private DelegatingConnection<Connection> conn = null;
    private Connection delegateConn = null;
    private DelegatingDatabaseMetaData meta = null;
    private DatabaseMetaData delegateMeta = null;

    @Before
    public void setUp() throws Exception {
        delegateConn = new TesterConnection("test", "test");
        delegateMeta = delegateConn.getMetaData();
        conn = new DelegatingConnection<>(delegateConn);
        meta = new DelegatingDatabaseMetaData(conn,delegateMeta);
    }

    @Test
    public void testGetDelegate() throws Exception {
        assertEquals(delegateMeta,meta.getDelegate());
    }

    @Test
    /* JDBC_4_ANT_KEY_BEGIN */
    public void testCheckOpen() throws Exception {
        final ResultSet rst = meta.getSchemas();
        assertTrue(!rst.isClosed());
        conn.close();
        assertTrue(rst.isClosed());
    }
    /* JDBC_4_ANT_KEY_END */

    @Test
    public void testAllProceduresAreCallable() throws Exception {
        assertEquals(delegateMeta.allProceduresAreCallable(), meta.allProceduresAreCallable());
    }

    @Test
    public void testAllTablesAreSelectable() throws Exception {
        assertEquals(delegateMeta.allTablesAreSelectable(), meta.allTablesAreSelectable());
    }

    @Test
    public void testAutoCommitFailureClosesAllResultSets() throws Exception {
        assertEquals(delegateMeta.autoCommitFailureClosesAllResultSets(), meta.autoCommitFailureClosesAllResultSets());
    }

    @Test
    public void testDataDefinitionCausesTransactionCommit() throws Exception {
        assertEquals(delegateMeta.dataDefinitionCausesTransactionCommit(),
                meta.dataDefinitionCausesTransactionCommit());
    }

    @Test
    public void testDataDefinitionIgnoredInTransactions() throws Exception {
        assertEquals(delegateMeta.dataDefinitionIgnoredInTransactions(), meta.dataDefinitionIgnoredInTransactions());
    }

    @Test
    public void testDeletesAreDetected() throws Exception {
        assertEquals(delegateMeta.deletesAreDetected(1), meta.deletesAreDetected(1));
    }

    @Test
    public void testDoesMaxRowSizeIncludeBlobs() throws Exception {
        assertEquals(delegateMeta.doesMaxRowSizeIncludeBlobs(), meta.doesMaxRowSizeIncludeBlobs());
    }

    @Test
    public void testGeneratedKeyAlwaysReturned() throws Exception {
        assertEquals(delegateMeta.generatedKeyAlwaysReturned(), meta.generatedKeyAlwaysReturned());
    }

    @Test
    public void testGetAttributes() throws Exception {
        assertEquals(delegateMeta.getAttributes("catalog", "schema", "type", "attribute"),
                meta.getAttributes("catalog", "schema", "type", "attribute"));
    }

    @Test
    public void testGetBestRowIdentifier() throws Exception {
        assertEquals(delegateMeta.getBestRowIdentifier("catalog", "schema", "table", 1, false),
                meta.getBestRowIdentifier("catalog", "schema", "table", 1, false));
    }

    @Test
    public void testGetCatalogSeparator() throws Exception {
        assertEquals(delegateMeta.getCatalogSeparator(), meta.getCatalogSeparator());
    }

    @Test
    public void testGetCatalogTerm() throws Exception {
        assertEquals(delegateMeta.getCatalogTerm(), meta.getCatalogTerm());
    }

    @Test
    public void testGetCatalogs() throws Exception {
        assertEquals(delegateMeta.getCatalogs(), meta.getCatalogs());
    }

    @Test
    public void testGetClientInfoProperties() throws Exception {
        assertEquals(delegateMeta.getClientInfoProperties(), meta.getClientInfoProperties());
    }

    @Test
    public void testGetColumnPrivileges() throws Exception {
        assertEquals(delegateMeta.getColumnPrivileges("catalog", "schema", "table", "column"),
                meta.getColumnPrivileges("catalog", "schema", "table", "column"));
    }

    @Test
    public void testGetColumns() throws Exception {
        assertEquals(delegateMeta.getColumns("catalog", "schema", "table", "column"),
                meta.getColumns("catalog", "schema", "table", "column"));
    }

    @Test
    public void testGetCrossReference() throws Exception {
        assertEquals(delegateMeta.getCrossReference("catalog", "schema", "table", "catalog", "schema", "table"),
                meta.getCrossReference("catalog", "schema", "table", "catalog", "schema", "table"));
    }

    @Test
    public void testGetDatabaseMajorVersion() throws Exception {
        assertEquals(delegateMeta.getDatabaseMajorVersion(), meta.getDatabaseMajorVersion());
    }

    @Test
    public void testGetDatabaseMinorVersion() throws Exception {
        assertEquals(delegateMeta.getDatabaseMinorVersion(), meta.getDatabaseMinorVersion());
    }

    @Test
    public void testGetDatabaseProductName() throws Exception {
        assertEquals(delegateMeta.getDatabaseProductName(), meta.getDatabaseProductName());
    }

    @Test
    public void testGetDatabaseProductVersion() throws Exception {
        assertEquals(delegateMeta.getDatabaseProductVersion(), meta.getDatabaseProductVersion());
    }

    @Test
    public void testGetDefaultTransactionIsolation() throws Exception {
        assertEquals(delegateMeta.getDefaultTransactionIsolation(), meta.getDefaultTransactionIsolation());
    }

    @Test
    public void testGetDriverMajorVersion() throws Exception {
        assertEquals(delegateMeta.getDriverMajorVersion(), meta.getDriverMajorVersion());
    }

    @Test
    public void testGetDriverMinorVersion() throws Exception {
        assertEquals(delegateMeta.getDriverMinorVersion(), meta.getDriverMinorVersion());
    }

    @Test
    public void testGetDriverName() throws Exception {
        assertEquals(delegateMeta.getDriverName(), meta.getDriverName());
    }

    @Test
    public void testGetDriverVersion() throws Exception {
        assertEquals(delegateMeta.getDriverVersion(), meta.getDriverVersion());
    }

    @Test
    public void testGetExportedKeys() throws Exception {
        assertEquals(delegateMeta.getExportedKeys("catalog", "schema", "table"),
                meta.getExportedKeys("catalog", "schema", "table"));
    }

    @Test
    public void testGetExtraNameCharacters() throws Exception {
        assertEquals(delegateMeta.getExtraNameCharacters(), meta.getExtraNameCharacters());
    }

    @Test
    public void testGetFunctionColumns() throws Exception {
        assertEquals(delegateMeta.getFunctionColumns("catalog", "schema", "function", "column"),
                meta.getFunctionColumns("catalog", "schema", "function", "column"));
    }

    @Test
    public void testGetFunctions() throws Exception {
        assertEquals(delegateMeta.getFunctions("catalog", "schema", "function"),
                meta.getFunctions("catalog", "schema", "function"));
    }

    @Test
    public void testGetIdentifierQuoteString() throws Exception {
        assertEquals(delegateMeta.getIdentifierQuoteString(), meta.getIdentifierQuoteString());
    }

    @Test
    public void testGetImportedKeys() throws Exception {
        assertEquals(delegateMeta.getImportedKeys("catalog", "schema", "table"),
                meta.getImportedKeys("catalog", "schema", "table"));
    }

    @Test
    public void testGetIndexInfo() throws Exception {
        assertEquals(delegateMeta.getIndexInfo("catalog", "schema", "table", false, false),
                meta.getIndexInfo("catalog", "schema", "table", false, false));
    }

    @Test
    public void testGetJDBCMajorVersion() throws Exception {
        assertEquals(delegateMeta.getJDBCMajorVersion(), meta.getJDBCMajorVersion());
    }

    @Test
    public void testGetJDBCMinorVersion() throws Exception {
        assertEquals(delegateMeta.getJDBCMinorVersion(), meta.getJDBCMinorVersion());
    }

    @Test
    public void testGetMaxBinaryLiteralLength() throws Exception {
        assertEquals(delegateMeta.getMaxBinaryLiteralLength(), meta.getMaxBinaryLiteralLength());
    }

    @Test
    public void testGetMaxCatalogNameLength() throws Exception {
        assertEquals(delegateMeta.getMaxCatalogNameLength(), meta.getMaxCatalogNameLength());
    }

    @Test
    public void testGetMaxCharLiteralLength() throws Exception {
        assertEquals(delegateMeta.getMaxCharLiteralLength(), meta.getMaxCharLiteralLength());
    }

    @Test
    public void testGetMaxColumnNameLength() throws Exception {
        assertEquals(delegateMeta.getMaxColumnNameLength(), meta.getMaxColumnNameLength());
    }

    @Test
    public void testGetMaxColumnsInGroupBy() throws Exception {
        assertEquals(delegateMeta.getMaxColumnsInGroupBy(), meta.getMaxColumnsInGroupBy());
    }

    @Test
    public void testGetMaxColumnsInIndex() throws Exception {
        assertEquals(delegateMeta.getMaxColumnsInIndex(), meta.getMaxColumnsInIndex());
    }

    @Test
    public void testGetMaxColumnsInOrderBy() throws Exception {
        assertEquals(delegateMeta.getMaxColumnsInOrderBy(), meta.getMaxColumnsInOrderBy());
    }

    @Test
    public void testGetMaxColumnsInSelect() throws Exception {
        assertEquals(delegateMeta.getMaxColumnsInSelect(), meta.getMaxColumnsInSelect());
    }

    @Test
    public void testGetMaxColumnsInTable() throws Exception {
        assertEquals(delegateMeta.getMaxColumnsInTable(), meta.getMaxColumnsInTable());
    }

    @Test
    public void testGetMaxConnections() throws Exception {
        assertEquals(delegateMeta.getMaxConnections(), meta.getMaxConnections());
    }

    @Test
    public void testGetMaxCursorNameLength() throws Exception {
        assertEquals(delegateMeta.getMaxCursorNameLength(), meta.getMaxCursorNameLength());
    }

    @Test
    public void testGetMaxIndexLength() throws Exception {
        assertEquals(delegateMeta.getMaxIndexLength(), meta.getMaxIndexLength());
    }

    @Test
    public void testGetMaxLogicalLobSize() throws Exception {
        assertEquals(delegateMeta.getMaxLogicalLobSize(), meta.getMaxLogicalLobSize());
    }

    @Test
    public void testGetMaxProcedureNameLength() throws Exception {
        assertEquals(delegateMeta.getMaxProcedureNameLength(), meta.getMaxProcedureNameLength());
    }

    @Test
    public void testGetMaxRowSize() throws Exception {
        assertEquals(delegateMeta.getMaxRowSize(), meta.getMaxRowSize());
    }

    @Test
    public void testGetMaxSchemaNameLength() throws Exception {
        assertEquals(delegateMeta.getMaxSchemaNameLength(), meta.getMaxSchemaNameLength());
    }

    @Test
    public void testGetMaxStatementLength() throws Exception {
        assertEquals(delegateMeta.getMaxStatementLength(), meta.getMaxStatementLength());
    }

    @Test
    public void testGetMaxStatements() throws Exception {
        assertEquals(delegateMeta.getMaxStatements(), meta.getMaxStatements());
    }

    @Test
    public void testGetMaxTableNameLength() throws Exception {
        assertEquals(delegateMeta.getMaxTableNameLength(), meta.getMaxTableNameLength());
    }

    @Test
    public void testGetMaxTablesInSelect() throws Exception {
        assertEquals(delegateMeta.getMaxTablesInSelect(), meta.getMaxTablesInSelect());
    }

    @Test
    public void testGetMaxUserNameLength() throws Exception {
        assertEquals(delegateMeta.getMaxUserNameLength(), meta.getMaxUserNameLength());
    }

    @Test
    public void testGetNumericFunctions() throws Exception {
        assertEquals(delegateMeta.getNumericFunctions(), meta.getNumericFunctions());
    }

    @Test
    public void testGetPrimaryKeys() throws Exception {
        assertEquals(delegateMeta.getPrimaryKeys("catalog", "schema", "table"),
                meta.getPrimaryKeys("catalog", "schema", "table"));
    }

    @Test
    public void testGetProcedureColumns() throws Exception {
        assertEquals(delegateMeta.getProcedureColumns("catalog", "schema", "table", "column"),
                meta.getProcedureColumns("catalog", "schema", "table", "column"));
    }

    @Test
    public void testGetProcedureTerm() throws Exception {
        assertEquals(delegateMeta.getProcedureTerm(), meta.getProcedureTerm());
    }

    @Test
    public void testGetProcedures() throws Exception {
        assertEquals(delegateMeta.getProcedures("catalog", "schema", "procedure"),
                meta.getProcedures("catalog", "schema", "procedure"));
    }

    @Test
    public void testGetPseudoColumns() throws Exception {
        assertEquals(delegateMeta.getPseudoColumns("catalog", "schema", "table", "column"),
                meta.getPseudoColumns("catalog", "schema", "table", "column"));
    }

    @Test
    public void testGetResultSetHoldability() throws Exception {
        assertEquals(delegateMeta.getResultSetHoldability(), meta.getResultSetHoldability());
    }

    @Test
    public void testGetRowIdLifetime() throws Exception {
        assertEquals(delegateMeta.getRowIdLifetime(), meta.getRowIdLifetime());
    }

    @Test
    public void testGetSQLKeywords() throws Exception {
        assertEquals(delegateMeta.getSQLKeywords(), meta.getSQLKeywords());
    }

    @Test
    public void testGetSQLStateType() throws Exception {
        assertEquals(delegateMeta.getSQLStateType(), meta.getSQLStateType());
    }

    @Test
    public void testGetSchemaTerm() throws Exception {
        assertEquals(delegateMeta.getSchemaTerm(), meta.getSchemaTerm());
    }

    @Test
    public void testGetSearchStringEscape() throws Exception {
        assertEquals(delegateMeta.getSearchStringEscape(), meta.getSearchStringEscape());
    }

    @Test
    public void testGetStringFunctions() throws Exception {
        assertEquals(delegateMeta.getStringFunctions(), meta.getStringFunctions());
    }

    @Test
    public void testGetSuperTables() throws Exception {
        assertEquals(delegateMeta.getSuperTables("catalog", "schema", "table"),
                meta.getSuperTables("catalog", "schema", "table"));
    }

    @Test
    public void testGetSuperTypes() throws Exception {
        assertEquals(delegateMeta.getSuperTypes("catalog", "schema", "type"),
                meta.getSuperTypes("catalog", "schema", "type"));
    }

    @Test
    public void testGetSystemFunctions() throws Exception {
        assertEquals(delegateMeta.getSystemFunctions(), meta.getSystemFunctions());
    }

    @Test
    public void testGetTablePrivileges() throws Exception {
        assertEquals(delegateMeta.getTablePrivileges("catalog", "schema", "table"),
                meta.getTablePrivileges("catalog", "schema", "table"));
    }

    @Test
    public void testGetTableTypes() throws Exception {
        assertEquals(delegateMeta.getTableTypes(), meta.getTableTypes());
    }

    @Test
    public void testGetTables() throws Exception {
        assertEquals(delegateMeta.getTables("catalog", "schema", "table", null),
                meta.getTables("catalog", "schema", "table", null));
    }

    @Test
    public void testGetTimeDateFunctions() throws Exception {
        assertEquals(delegateMeta.getTimeDateFunctions(), meta.getTimeDateFunctions());
    }

    @Test
    public void testGetTypeInfo() throws Exception {
        assertEquals(delegateMeta.getTypeInfo(), meta.getTypeInfo());
    }

    @Test
    public void testGetUDTs() throws Exception {
        assertEquals(delegateMeta.getUDTs("catalog", "schema", "type", null),
                meta.getUDTs("catalog", "schema", "type", null));
    }

    @Test
    public void testGetURL() throws Exception {
        assertEquals(delegateMeta.getURL(), meta.getURL());
    }

    @Test
    public void testGetUserName() throws Exception {
        assertEquals(delegateMeta.getUserName(), meta.getUserName());
    }

    @Test
    public void testGetVersionColumns() throws Exception {
        assertEquals(delegateMeta.getVersionColumns("catalog", "schema", "table"),
                meta.getVersionColumns("catalog", "schema", "table"));
    }

    @Test
    public void testInsertsAreDetected() throws Exception {
        assertEquals(delegateMeta.insertsAreDetected(2), meta.insertsAreDetected(2));
    }

    @Test
    public void testIsCatalogAtStart() throws Exception {
        assertEquals(delegateMeta.isCatalogAtStart(), meta.isCatalogAtStart());
    }

    @Test
    public void testIsReadOnly() throws Exception {
        assertEquals(delegateMeta.isReadOnly(), meta.isReadOnly());
    }

    @Test
    public void testLocatorsUpdateCopy() throws Exception {
        assertEquals(delegateMeta.locatorsUpdateCopy(), meta.locatorsUpdateCopy());
    }

    @Test
    public void testNullPlusNonNullIsNull() throws Exception {
        assertEquals(delegateMeta.nullPlusNonNullIsNull(), meta.nullPlusNonNullIsNull());
    }

    @Test
    public void testNullsAreSortedAtEnd() throws Exception {
        assertEquals(delegateMeta.nullsAreSortedAtEnd(), meta.nullsAreSortedAtEnd());
    }

    @Test
    public void testNullsAreSortedAtStart() throws Exception {
        assertEquals(delegateMeta.nullsAreSortedAtStart(), meta.nullsAreSortedAtStart());
    }

    @Test
    public void testNullsAreSortedHigh() throws Exception {
        assertEquals(delegateMeta.nullsAreSortedHigh(), meta.nullsAreSortedHigh());
    }

    @Test
    public void testNullsAreSortedLow() throws Exception {
        assertEquals(delegateMeta.nullsAreSortedLow(), meta.nullsAreSortedLow());
    }

    @Test
    public void testOthersDeletesAreVisible() throws Exception {
        assertEquals(delegateMeta.othersDeletesAreVisible(0), meta.othersDeletesAreVisible(0));
    }

    @Test
    public void testOthersInsertsAreVisible() throws Exception {
        assertEquals(delegateMeta.othersInsertsAreVisible(0), meta.othersInsertsAreVisible(0));
    }

    @Test
    public void testOthersUpdatesAreVisible() throws Exception {
        assertEquals(delegateMeta.othersUpdatesAreVisible(1), meta.othersUpdatesAreVisible(1));
    }

    @Test
    public void testOwnDeletesAreVisible() throws Exception {
        assertEquals(delegateMeta.ownDeletesAreVisible(1), meta.ownDeletesAreVisible(1));
    }

    @Test
    public void testOwnInsertsAreVisible() throws Exception {
        assertEquals(delegateMeta.ownInsertsAreVisible(2), meta.ownInsertsAreVisible(2));
    }

    @Test
    public void testOwnUpdatesAreVisible() throws Exception {
        assertEquals(delegateMeta.ownUpdatesAreVisible(1), meta.ownUpdatesAreVisible(1));
    }

    @Test
    public void testStoresLowerCaseIdentifiers() throws Exception {
        assertEquals(delegateMeta.storesLowerCaseIdentifiers(), meta.storesLowerCaseIdentifiers());
    }

    @Test
    public void testStoresLowerCaseQuotedIdentifiers() throws Exception {
        assertEquals(delegateMeta.storesLowerCaseQuotedIdentifiers(), meta.storesLowerCaseQuotedIdentifiers());
    }

    @Test
    public void testStoresMixedCaseIdentifiers() throws Exception {
        assertEquals(delegateMeta.storesMixedCaseIdentifiers(), meta.storesMixedCaseIdentifiers());
    }

    @Test
    public void testStoresMixedCaseQuotedIdentifiers() throws Exception {
        assertEquals(delegateMeta.storesMixedCaseQuotedIdentifiers(), meta.storesMixedCaseQuotedIdentifiers());
    }

    @Test
    public void testStoresUpperCaseIdentifiers() throws Exception {
        assertEquals(delegateMeta.storesUpperCaseIdentifiers(), meta.storesUpperCaseIdentifiers());
    }

    @Test
    public void testStoresUpperCaseQuotedIdentifiers() throws Exception {
        assertEquals(delegateMeta.storesUpperCaseQuotedIdentifiers(), meta.storesUpperCaseQuotedIdentifiers());
    }

    @Test
    public void testSupportsANSI92EntryLevelSQL() throws Exception {
        assertEquals(delegateMeta.supportsANSI92EntryLevelSQL(), meta.supportsANSI92EntryLevelSQL());
    }

    @Test
    public void testSupportsANSI92FullSQL() throws Exception {
        assertEquals(delegateMeta.supportsANSI92FullSQL(), meta.supportsANSI92FullSQL());
    }

    @Test
    public void testSupportsANSI92IntermediateSQL() throws Exception {
        assertEquals(delegateMeta.supportsANSI92IntermediateSQL(), meta.supportsANSI92IntermediateSQL());
    }

    @Test
    public void testSupportsAlterTableWithAddColumn() throws Exception {
        assertEquals(delegateMeta.supportsAlterTableWithAddColumn(), meta.supportsAlterTableWithAddColumn());
    }

    @Test
    public void testSupportsAlterTableWithDropColumn() throws Exception {
        assertEquals(delegateMeta.supportsAlterTableWithDropColumn(), meta.supportsAlterTableWithDropColumn());
    }

    @Test
    public void testSupportsBatchUpdates() throws Exception {
        assertEquals(delegateMeta.supportsBatchUpdates(), meta.supportsBatchUpdates());
    }

    @Test
    public void testSupportsCatalogsInDataManipulation() throws Exception {
        assertEquals(delegateMeta.supportsCatalogsInDataManipulation(), meta.supportsCatalogsInDataManipulation());
    }

    @Test
    public void testSupportsCatalogsInIndexDefinitions() throws Exception {
        assertEquals(delegateMeta.supportsCatalogsInIndexDefinitions(), meta.supportsCatalogsInIndexDefinitions());
    }

    @Test
    public void testSupportsCatalogsInPrivilegeDefinitions() throws Exception {
        assertEquals(delegateMeta.supportsCatalogsInPrivilegeDefinitions(),
                meta.supportsCatalogsInPrivilegeDefinitions());
    }

    @Test
    public void testSupportsCatalogsInProcedureCalls() throws Exception {
        assertEquals(delegateMeta.supportsCatalogsInProcedureCalls(), meta.supportsCatalogsInProcedureCalls());
    }

    @Test
    public void testSupportsCatalogsInTableDefinitions() throws Exception {
        assertEquals(delegateMeta.supportsCatalogsInTableDefinitions(), meta.supportsCatalogsInTableDefinitions());
    }

    @Test
    public void testSupportsColumnAliasing() throws Exception {
        assertEquals(delegateMeta.supportsColumnAliasing(), meta.supportsColumnAliasing());
    }

    @Test
    public void testSupportsConvert() throws Exception {
        assertEquals(delegateMeta.supportsConvert(), meta.supportsConvert());
    }

    @Test
    public void testSupportsCoreSQLGrammar() throws Exception {
        assertEquals(delegateMeta.supportsCoreSQLGrammar(), meta.supportsCoreSQLGrammar());
    }

    @Test
    public void testSupportsCorrelatedSubqueries() throws Exception {
        assertEquals(delegateMeta.supportsCorrelatedSubqueries(), meta.supportsCorrelatedSubqueries());
    }

    @Test
    public void testSupportsDataDefinitionAndDataManipulationTransactions() throws Exception {
        assertEquals(delegateMeta.supportsDataDefinitionAndDataManipulationTransactions(),
                meta.supportsDataDefinitionAndDataManipulationTransactions());
    }

    @Test
    public void testSupportsDataManipulationTransactionsOnly() throws Exception {
        assertEquals(delegateMeta.supportsDataManipulationTransactionsOnly(),
                meta.supportsDataManipulationTransactionsOnly());
    }

    @Test
    public void testSupportsDifferentTableCorrelationNames() throws Exception {
        assertEquals(delegateMeta.supportsDifferentTableCorrelationNames(),
                meta.supportsDifferentTableCorrelationNames());
    }

    @Test
    public void testSupportsExpressionsInOrderBy() throws Exception {
        assertEquals(delegateMeta.supportsExpressionsInOrderBy(), meta.supportsExpressionsInOrderBy());
    }

    @Test
    public void testSupportsExtendedSQLGrammar() throws Exception {
        assertEquals(delegateMeta.supportsExtendedSQLGrammar(), meta.supportsExtendedSQLGrammar());
    }

    @Test
    public void testSupportsFullOuterJoins() throws Exception {
        assertEquals(delegateMeta.supportsFullOuterJoins(), meta.supportsFullOuterJoins());
    }

    @Test
    public void testSupportsGetGeneratedKeys() throws Exception {
        assertEquals(delegateMeta.supportsGetGeneratedKeys(), meta.supportsGetGeneratedKeys());
    }

    @Test
    public void testSupportsGroupBy() throws Exception {
        assertEquals(delegateMeta.supportsGroupBy(), meta.supportsGroupBy());
    }

    @Test
    public void testSupportsGroupByBeyondSelect() throws Exception {
        assertEquals(delegateMeta.supportsGroupByBeyondSelect(), meta.supportsGroupByBeyondSelect());
    }

    @Test
    public void testSupportsGroupByUnrelated() throws Exception {
        assertEquals(delegateMeta.supportsGroupByUnrelated(), meta.supportsGroupByUnrelated());
    }

    @Test
    public void testSupportsIntegrityEnhancementFacility() throws Exception {
        assertEquals(delegateMeta.supportsIntegrityEnhancementFacility(), meta.supportsIntegrityEnhancementFacility());
    }

    @Test
    public void testSupportsLikeEscapeClause() throws Exception {
        assertEquals(delegateMeta.supportsLikeEscapeClause(), meta.supportsLikeEscapeClause());
    }

    @Test
    public void testSupportsLimitedOuterJoins() throws Exception {
        assertEquals(delegateMeta.supportsLimitedOuterJoins(), meta.supportsLimitedOuterJoins());
    }

    @Test
    public void testSupportsMinimumSQLGrammar() throws Exception {
        assertEquals(delegateMeta.supportsMinimumSQLGrammar(), meta.supportsMinimumSQLGrammar());
    }

    @Test
    public void testSupportsMixedCaseIdentifiers() throws Exception {
        assertEquals(delegateMeta.supportsMixedCaseIdentifiers(), meta.supportsMixedCaseIdentifiers());
    }

    @Test
    public void testSupportsMixedCaseQuotedIdentifiers() throws Exception {
        assertEquals(delegateMeta.supportsMixedCaseQuotedIdentifiers(), meta.supportsMixedCaseQuotedIdentifiers());
    }

    @Test
    public void testSupportsMultipleOpenResults() throws Exception {
        assertEquals(delegateMeta.supportsMultipleOpenResults(), meta.supportsMultipleOpenResults());
    }

    @Test
    public void testSupportsMultipleResultSets() throws Exception {
        assertEquals(delegateMeta.supportsMultipleResultSets(), meta.supportsMultipleResultSets());
    }

    @Test
    public void testSupportsMultipleTransactions() throws Exception {
        assertEquals(delegateMeta.supportsMultipleTransactions(), meta.supportsMultipleTransactions());
    }

    @Test
    public void testSupportsNamedParameters() throws Exception {
        assertEquals(delegateMeta.supportsNamedParameters(), meta.supportsNamedParameters());
    }

    @Test
    public void testSupportsNonNullableColumns() throws Exception {
        assertEquals(delegateMeta.supportsNonNullableColumns(), meta.supportsNonNullableColumns());
    }

    @Test
    public void testSupportsOpenCursorsAcrossCommit() throws Exception {
        assertEquals(delegateMeta.supportsOpenCursorsAcrossCommit(), meta.supportsOpenCursorsAcrossCommit());
    }

    @Test
    public void testSupportsOpenCursorsAcrossRollback() throws Exception {
        assertEquals(delegateMeta.supportsOpenCursorsAcrossRollback(), meta.supportsOpenCursorsAcrossRollback());
    }

    @Test
    public void testSupportsOpenStatementsAcrossCommit() throws Exception {
        assertEquals(delegateMeta.supportsOpenStatementsAcrossCommit(), meta.supportsOpenStatementsAcrossCommit());
    }

    @Test
    public void testSupportsOpenStatementsAcrossRollback() throws Exception {
        assertEquals(delegateMeta.supportsOpenStatementsAcrossRollback(), meta.supportsOpenStatementsAcrossRollback());
    }

    @Test
    public void testSupportsOrderByUnrelated() throws Exception {
        assertEquals(delegateMeta.supportsOrderByUnrelated(), meta.supportsOrderByUnrelated());
    }

    @Test
    public void testSupportsOuterJoins() throws Exception {
        assertEquals(delegateMeta.supportsOuterJoins(), meta.supportsOuterJoins());
    }

    @Test
    public void testSupportsPositionedDelete() throws Exception {
        assertEquals(delegateMeta.supportsPositionedDelete(), meta.supportsPositionedDelete());
    }

    @Test
    public void testSupportsPositionedUpdate() throws Exception {
        assertEquals(delegateMeta.supportsPositionedUpdate(), meta.supportsPositionedUpdate());
    }

    @Test
    public void testSupportsRefCursors() throws Exception {
        assertEquals(delegateMeta.supportsRefCursors(), meta.supportsRefCursors());
    }

    @Test
    public void testSupportsResultSetConcurrency() throws Exception {
        assertEquals(delegateMeta.supportsResultSetConcurrency(1, 2), meta.supportsResultSetConcurrency(1, 2));
    }

    @Test
    public void testSupportsResultSetHoldability() throws Exception {
        assertEquals(delegateMeta.supportsResultSetHoldability(1), meta.supportsResultSetHoldability(1));
    }

    @Test
    public void testSupportsResultSetType() throws Exception {
        assertEquals(delegateMeta.supportsResultSetType(3), meta.supportsResultSetType(3));
    }

    @Test
    public void testSupportsSavepoints() throws Exception {
        assertEquals(delegateMeta.supportsSavepoints(), meta.supportsSavepoints());
    }

    @Test
    public void testSupportsSchemasInDataManipulation() throws Exception {
        assertEquals(delegateMeta.supportsSchemasInDataManipulation(), meta.supportsSchemasInDataManipulation());
    }

    @Test
    public void testSupportsSchemasInIndexDefinitions() throws Exception {
        assertEquals(delegateMeta.supportsSchemasInIndexDefinitions(), meta.supportsSchemasInIndexDefinitions());
    }

    @Test
    public void testSupportsSchemasInPrivilegeDefinitions() throws Exception {
        assertEquals(delegateMeta.supportsSchemasInPrivilegeDefinitions(),
                meta.supportsSchemasInPrivilegeDefinitions());
    }

    @Test
    public void testSupportsSchemasInProcedureCalls() throws Exception {
        assertEquals(delegateMeta.supportsSchemasInProcedureCalls(), meta.supportsSchemasInProcedureCalls());
    }

    @Test
    public void testSupportsSchemasInTableDefinitions() throws Exception {
        assertEquals(delegateMeta.supportsSchemasInTableDefinitions(), meta.supportsSchemasInTableDefinitions());
    }

    @Test
    public void testSupportsSelectForUpdate() throws Exception {
        assertEquals(delegateMeta.supportsSelectForUpdate(), meta.supportsSelectForUpdate());
    }

    @Test
    public void testSupportsStatementPooling() throws Exception {
        assertEquals(delegateMeta.supportsStatementPooling(), meta.supportsStatementPooling());
    }

    @Test
    public void testSupportsStoredFunctionsUsingCallSyntax() throws Exception {
        assertEquals(delegateMeta.supportsStoredFunctionsUsingCallSyntax(),
                meta.supportsStoredFunctionsUsingCallSyntax());
    }

    @Test
    public void testSupportsStoredProcedures() throws Exception {
        assertEquals(delegateMeta.supportsStoredProcedures(), meta.supportsStoredProcedures());
    }

    @Test
    public void testSupportsSubqueriesInComparisons() throws Exception {
        assertEquals(delegateMeta.supportsSubqueriesInComparisons(), meta.supportsSubqueriesInComparisons());
    }

    @Test
    public void testSupportsSubqueriesInExists() throws Exception {
        assertEquals(delegateMeta.supportsSubqueriesInExists(), meta.supportsSubqueriesInExists());
    }

    @Test
    public void testSupportsSubqueriesInIns() throws Exception {
        assertEquals(delegateMeta.supportsSubqueriesInIns(), meta.supportsSubqueriesInIns());
    }

    @Test
    public void testSupportsSubqueriesInQuantifieds() throws Exception {
        assertEquals(delegateMeta.supportsSubqueriesInQuantifieds(), meta.supportsSubqueriesInQuantifieds());
    }

    @Test
    public void testSupportsTableCorrelationNames() throws Exception {
        assertEquals(delegateMeta.supportsTableCorrelationNames(), meta.supportsTableCorrelationNames());
    }

    @Test
    public void testSupportsTransactionIsolationLevel() throws Exception {
        assertEquals(delegateMeta.supportsTransactionIsolationLevel(1), meta.supportsTransactionIsolationLevel(1));
    }

    @Test
    public void testSupportsTransactions() throws Exception {
        assertEquals(delegateMeta.supportsTransactions(), meta.supportsTransactions());
    }

    @Test
    public void testSupportsUnion() throws Exception {
        assertEquals(delegateMeta.supportsUnion(), meta.supportsUnion());
    }

    @Test
    public void testSupportsUnionAll() throws Exception {
        assertEquals(delegateMeta.supportsUnionAll(), meta.supportsUnionAll());
    }

    @Test
    public void testUpdatesAreDetected() throws Exception {
        assertEquals(delegateMeta.updatesAreDetected(2), meta.updatesAreDetected(2));
    }

    @Test
    public void testUsesLocalFilePerTable() throws Exception {
        assertEquals(delegateMeta.usesLocalFilePerTable(), meta.usesLocalFilePerTable());
    }

    @Test
    public void testUsesLocalFiles() throws Exception {
        assertEquals(delegateMeta.usesLocalFiles(), meta.usesLocalFiles());
    }
}
