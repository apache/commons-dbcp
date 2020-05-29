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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.RowIdLifetime;
import java.sql.SQLException;

import org.apache.commons.dbcp2.function.SQLFunction0;
import org.apache.commons.dbcp2.function.SQLFunction2;
import org.apache.commons.dbcp2.function.SQLFunction3;
import org.apache.commons.dbcp2.function.SQLFunction4;
import org.apache.commons.dbcp2.function.SQLFunction5;
import org.apache.commons.dbcp2.function.SQLFunction6;

/**
 * <p>
 * A base delegating implementation of {@link DatabaseMetaData}.
 * </p>
 * <p>
 * Methods that create {@link ResultSet} objects are wrapped to create {@link DelegatingResultSet} objects and the
 * remaining methods simply call the corresponding method on the "delegate" provided in the constructor.
 * </p>
 *
 * @since 2.0
 */
public class DelegatingDatabaseMetaData extends ResourceFunctions implements DatabaseMetaData {

    /** My delegate {@link DatabaseMetaData} */
    private final DatabaseMetaData databaseMetaData;

    /** The connection that created me. **/
    private final DelegatingConnection<?> connection;

    /**
     * Constructs a new instance for the given delegating connection and database meta data.
     *
     * @param connection       the delegating connection
     * @param databaseMetaData the database meta data
     */
    public DelegatingDatabaseMetaData(final DelegatingConnection<?> connection,
        final DatabaseMetaData databaseMetaData) {
        super();
        this.connection = connection;
        this.databaseMetaData = databaseMetaData;
    }

    @Override
    public boolean allProceduresAreCallable() throws SQLException {
        return applyToFalse(databaseMetaData::allProceduresAreCallable);
    }

    @Override
    public boolean allTablesAreSelectable() throws SQLException {
        return applyToFalse(databaseMetaData::allTablesAreSelectable);
    }

    @Override
    public boolean autoCommitFailureClosesAllResultSets() throws SQLException {
        return applyToFalse(databaseMetaData::autoCommitFailureClosesAllResultSets);
    }

    @Override
    public boolean dataDefinitionCausesTransactionCommit() throws SQLException {
        return applyToFalse(databaseMetaData::dataDefinitionCausesTransactionCommit);
    }

    @Override
    public boolean dataDefinitionIgnoredInTransactions() throws SQLException {
        return applyToFalse(databaseMetaData::dataDefinitionIgnoredInTransactions);
    }

    @Override
    public boolean deletesAreDetected(final int type) throws SQLException {
        return applyIntTo(databaseMetaData::deletesAreDetected, type, false);
    }

    @Override
    public boolean doesMaxRowSizeIncludeBlobs() throws SQLException {
        return applyToFalse(databaseMetaData::doesMaxRowSizeIncludeBlobs);
    }

    @Override
    public boolean generatedKeyAlwaysReturned() throws SQLException {
        return applyToFalse(databaseMetaData::generatedKeyAlwaysReturned);
    }

    @Override
    public ResultSet getAttributes(final String catalog, final String schemaPattern, final String typeNamePattern,
        final String attributeNamePattern) throws SQLException {
        return toResultSet(databaseMetaData::getAttributes, catalog, schemaPattern, typeNamePattern,
            attributeNamePattern);
    }

    @Override
    public ResultSet getBestRowIdentifier(final String catalog, final String schema, final String table,
        final int scope, final boolean nullable) throws SQLException {
        return toResultSet(databaseMetaData::getBestRowIdentifier, catalog, schema, table, scope, nullable);
    }

    @Override
    public ResultSet getCatalogs() throws SQLException {
        return toResultSet(databaseMetaData::getCatalogs);
    }

    @Override
    public String getCatalogSeparator() throws SQLException {
        return apply(databaseMetaData::getCatalogSeparator);
    }

    @Override
    public String getCatalogTerm() throws SQLException {
        return apply(databaseMetaData::getCatalogTerm);
    }

    @Override
    public ResultSet getClientInfoProperties() throws SQLException {
        return toResultSet(databaseMetaData::getClientInfoProperties);
    }

    @Override
    public ResultSet getColumnPrivileges(final String catalog, final String schema, final String table,
        final String columnNamePattern) throws SQLException {
        return toResultSet(databaseMetaData::getColumnPrivileges, catalog, schema, table, columnNamePattern);
    }

    @Override
    public ResultSet getColumns(final String catalog, final String schemaPattern, final String tableNamePattern,
        final String columnNamePattern) throws SQLException {
        return toResultSet(databaseMetaData::getColumns, catalog, schemaPattern, tableNamePattern, columnNamePattern);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return connection;
    }

    @Override
    public ResultSet getCrossReference(final String parentCatalog, final String parentSchema, final String parentTable,
        final String foreignCatalog, final String foreignSchema, final String foreignTable) throws SQLException {
        return toResultSet(databaseMetaData::getCrossReference, parentCatalog, parentSchema, parentTable,
            foreignCatalog, foreignSchema, foreignTable);
    }

    @Override
    public int getDatabaseMajorVersion() throws SQLException {
        return applyTo0(databaseMetaData::getDatabaseMajorVersion);
    }

    @Override
    public int getDatabaseMinorVersion() throws SQLException {
        return applyTo0(databaseMetaData::getDatabaseMinorVersion);
    }

    @Override
    public String getDatabaseProductName() throws SQLException {
        return apply(databaseMetaData::getDatabaseProductName);
    }

    @Override
    public String getDatabaseProductVersion() throws SQLException {
        return apply(databaseMetaData::getDatabaseProductVersion);
    }

    @Override
    public int getDefaultTransactionIsolation() throws SQLException {
        return applyTo0(databaseMetaData::getDefaultTransactionIsolation);
    }

    /**
     * Gets the underlying database meta data.
     *
     * @return The underlying database meta data.
     */
    public DatabaseMetaData getDelegate() {
        return databaseMetaData;
    }

    @Override
    public int getDriverMajorVersion() {
        return databaseMetaData.getDriverMajorVersion();
    }

    @Override
    public int getDriverMinorVersion() {
        return databaseMetaData.getDriverMinorVersion();
    }

    @Override
    public String getDriverName() throws SQLException {
        return apply(databaseMetaData::getDriverName);
    }

    @Override
    public String getDriverVersion() throws SQLException {
        return apply(databaseMetaData::getDriverVersion);
    }

    @Override
    public ResultSet getExportedKeys(final String catalog, final String schema, final String table)
        throws SQLException {
        return toResultSet(databaseMetaData::getExportedKeys, catalog, schema, table);
    }

    @Override
    public String getExtraNameCharacters() throws SQLException {
        return apply(databaseMetaData::getExtraNameCharacters);
    }

    @Override
    public ResultSet getFunctionColumns(final String catalog, final String schemaPattern,
        final String functionNamePattern, final String columnNamePattern) throws SQLException {
        return toResultSet(databaseMetaData::getFunctionColumns, catalog, schemaPattern, functionNamePattern,
            columnNamePattern);
    }

    @Override
    public ResultSet getFunctions(final String catalog, final String schemaPattern, final String functionNamePattern)
        throws SQLException {
        return toResultSet(databaseMetaData::getFunctions, catalog, schemaPattern, functionNamePattern);
    }

    @Override
    public String getIdentifierQuoteString() throws SQLException {
        return apply(databaseMetaData::getIdentifierQuoteString);
    }

    @Override
    public ResultSet getImportedKeys(final String catalog, final String schema, final String table)
        throws SQLException {
        return toResultSet(databaseMetaData::getImportedKeys, catalog, schema, table);
    }

    @Override
    public ResultSet getIndexInfo(final String catalog, final String schema, final String table, final boolean unique,
        final boolean approximate) throws SQLException {
        return toResultSet(databaseMetaData::getIndexInfo, catalog, schema, table, unique, approximate);
    }

    /**
     * If my underlying {@link ResultSet} is not a {@code DelegatingResultSet}, returns it, otherwise recursively
     * invokes this method on my delegate.
     * <p>
     * Hence this method will return the first delegate that is not a {@code DelegatingResultSet}, or {@code null} when
     * no non-{@code DelegatingResultSet} delegate can be found by traversing this chain.
     * </p>
     * <p>
     * This method is useful when you may have nested {@code DelegatingResultSet}s, and you want to make sure to obtain
     * a "genuine" {@link ResultSet}.
     * </p>
     *
     * @return the innermost database meta data.
     */
    public DatabaseMetaData getInnermostDelegate() {
        DatabaseMetaData m = databaseMetaData;
        while (m != null && m instanceof DelegatingDatabaseMetaData) {
            m = ((DelegatingDatabaseMetaData) m).getDelegate();
            if (this == m) {
                return null;
            }
        }
        return m;
    }

    @Override
    public int getJDBCMajorVersion() throws SQLException {
        return applyTo0(databaseMetaData::getJDBCMajorVersion);
    }

    @Override
    public int getJDBCMinorVersion() throws SQLException {
        return applyTo0(databaseMetaData::getJDBCMinorVersion);
    }

    @Override
    public int getMaxBinaryLiteralLength() throws SQLException {
        return applyTo0(databaseMetaData::getMaxBinaryLiteralLength);
    }

    @Override
    public int getMaxCatalogNameLength() throws SQLException {
        return applyTo0(databaseMetaData::getMaxCatalogNameLength);
    }

    @Override
    public int getMaxCharLiteralLength() throws SQLException {
        return applyTo0(databaseMetaData::getMaxCharLiteralLength);
    }

    @Override
    public int getMaxColumnNameLength() throws SQLException {
        return applyTo0(databaseMetaData::getMaxColumnNameLength);
    }

    @Override
    public int getMaxColumnsInGroupBy() throws SQLException {
        return applyTo0(databaseMetaData::getMaxColumnsInGroupBy);
    }

    @Override
    public int getMaxColumnsInIndex() throws SQLException {
        return applyTo0(databaseMetaData::getMaxColumnsInIndex);
    }

    @Override
    public int getMaxColumnsInOrderBy() throws SQLException {
        return applyTo0(databaseMetaData::getMaxColumnsInOrderBy);
    }

    @Override
    public int getMaxColumnsInSelect() throws SQLException {
        return applyTo0(databaseMetaData::getMaxColumnsInSelect);
    }

    @Override
    public int getMaxColumnsInTable() throws SQLException {
        return applyTo0(databaseMetaData::getMaxColumnsInTable);
    }

    @Override
    public int getMaxConnections() throws SQLException {
        return applyTo0(databaseMetaData::getMaxConnections);
    }

    @Override
    public int getMaxCursorNameLength() throws SQLException {
        return applyTo0(databaseMetaData::getMaxCursorNameLength);
    }

    @Override
    public int getMaxIndexLength() throws SQLException {
        return applyTo0(databaseMetaData::getMaxIndexLength);
    }

    /**
     * @since 2.5.0
     */
    @Override
    public long getMaxLogicalLobSize() throws SQLException {
        return applyTo0L(databaseMetaData::getMaxLogicalLobSize);
    }

    @Override
    public int getMaxProcedureNameLength() throws SQLException {
        return applyTo0(databaseMetaData::getMaxProcedureNameLength);
    }

    @Override
    public int getMaxRowSize() throws SQLException {
        return applyTo0(databaseMetaData::getMaxRowSize);
    }

    @Override
    public int getMaxSchemaNameLength() throws SQLException {
        return applyTo0(databaseMetaData::getMaxSchemaNameLength);
    }

    @Override
    public int getMaxStatementLength() throws SQLException {
        return applyTo0(databaseMetaData::getMaxStatementLength);
    }

    @Override
    public int getMaxStatements() throws SQLException {
        return applyTo0(databaseMetaData::getMaxStatements);
    }

    @Override
    public int getMaxTableNameLength() throws SQLException {
        return applyTo0(databaseMetaData::getMaxTableNameLength);
    }

    @Override
    public int getMaxTablesInSelect() throws SQLException {
        return applyTo0(databaseMetaData::getMaxTablesInSelect);
    }

    @Override
    public int getMaxUserNameLength() throws SQLException {
        return applyTo0(databaseMetaData::getMaxUserNameLength);
    }

    @Override
    public String getNumericFunctions() throws SQLException {
        return apply(databaseMetaData::getNumericFunctions);
    }

    @Override
    public ResultSet getPrimaryKeys(final String catalog, final String schema, final String table) throws SQLException {
        return toResultSet(databaseMetaData::getPrimaryKeys, catalog, schema, table);
    }

    @Override
    public ResultSet getProcedureColumns(final String catalog, final String schemaPattern,
        final String procedureNamePattern, final String columnNamePattern) throws SQLException {
        return toResultSet(databaseMetaData::getProcedureColumns, catalog, schemaPattern, procedureNamePattern,
            columnNamePattern);
    }

    @Override
    public ResultSet getProcedures(final String catalog, final String schemaPattern, final String procedureNamePattern)
        throws SQLException {
        return toResultSet(databaseMetaData::getProcedures, catalog, schemaPattern, procedureNamePattern);
    }

    @Override
    public String getProcedureTerm() throws SQLException {
        return apply(databaseMetaData::getProcedureTerm);
    }

    @Override
    public ResultSet getPseudoColumns(final String catalog, final String schemaPattern, final String tableNamePattern,
        final String columnNamePattern) throws SQLException {
        return toResultSet(databaseMetaData::getPseudoColumns, catalog, schemaPattern, tableNamePattern,
            columnNamePattern);
    }

    @Override
    public int getResultSetHoldability() throws SQLException {
        return applyTo0(databaseMetaData::getResultSetHoldability);
    }

    @Override
    public RowIdLifetime getRowIdLifetime() throws SQLException {
        return apply(databaseMetaData::getRowIdLifetime);
    }

    @Override
    public ResultSet getSchemas() throws SQLException {
        return toResultSet(databaseMetaData::getSchemas);
    }

    @Override
    public ResultSet getSchemas(final String catalog, final String schemaPattern) throws SQLException {
        return toResultSet(databaseMetaData::getSchemas, catalog, schemaPattern);
    }

    @Override
    public String getSchemaTerm() throws SQLException {
        return apply(databaseMetaData::getSchemaTerm);
    }

    @Override
    public String getSearchStringEscape() throws SQLException {
        return apply(databaseMetaData::getSearchStringEscape);
    }

    @Override
    public String getSQLKeywords() throws SQLException {
        return apply(databaseMetaData::getSQLKeywords);
    }

    @Override
    public int getSQLStateType() throws SQLException {
        return apply(databaseMetaData::getSQLStateType);
    }

    @Override
    public String getStringFunctions() throws SQLException {
        return apply(databaseMetaData::getStringFunctions);
    }

    @Override
    public ResultSet getSuperTables(final String catalog, final String schemaPattern, final String tableNamePattern)
        throws SQLException {
        return toResultSet(databaseMetaData::getSuperTables, catalog, schemaPattern, tableNamePattern);
    }

    @Override
    public ResultSet getSuperTypes(final String catalog, final String schemaPattern, final String typeNamePattern)
        throws SQLException {
        return toResultSet(databaseMetaData::getSuperTypes, catalog, schemaPattern, typeNamePattern);
    }

    @Override
    public String getSystemFunctions() throws SQLException {
        return apply(databaseMetaData::getSystemFunctions);
    }

    @Override
    public ResultSet getTablePrivileges(final String catalog, final String schemaPattern, final String tableNamePattern)
        throws SQLException {
        return toResultSet(databaseMetaData::getTablePrivileges, catalog, schemaPattern, tableNamePattern);
    }

    @Override
    public ResultSet getTables(final String catalog, final String schemaPattern, final String tableNamePattern,
        final String[] types) throws SQLException {
        return toResultSet(databaseMetaData::getTables, catalog, schemaPattern, tableNamePattern, types);
    }

    @Override
    public ResultSet getTableTypes() throws SQLException {
        return toResultSet(databaseMetaData::getTableTypes);
    }

    @Override
    public String getTimeDateFunctions() throws SQLException {
        return apply(databaseMetaData::getTimeDateFunctions);
    }

    @Override
    public ResultSet getTypeInfo() throws SQLException {
        return toResultSet(databaseMetaData::getTypeInfo);
    }

    @Override
    public ResultSet getUDTs(final String catalog, final String schemaPattern, final String typeNamePattern,
        final int[] types) throws SQLException {
        return toResultSet(databaseMetaData::getUDTs, catalog, schemaPattern, typeNamePattern, types);
    }

    @Override
    public String getURL() throws SQLException {
        return apply(databaseMetaData::getURL);
    }

    @Override
    public String getUserName() throws SQLException {
        return apply(databaseMetaData::getUserName);
    }

    @Override
    public ResultSet getVersionColumns(final String catalog, final String schema, final String table)
        throws SQLException {
        return toResultSet(databaseMetaData::getVersionColumns, catalog, schema, table);
    }

    @Override
    protected void handleException(final SQLException e) throws SQLException {
        if (connection != null) {
            connection.handleException(e);
        } else {
            throw e;
        }
    }

    @Override
    public boolean insertsAreDetected(final int type) throws SQLException {
        return applyIntTo(databaseMetaData::insertsAreDetected, type, false);
    }

    @Override
    public boolean isCatalogAtStart() throws SQLException {
        return applyToFalse(databaseMetaData::isCatalogAtStart);
    }

    @Override
    public boolean isReadOnly() throws SQLException {
        return applyToFalse(databaseMetaData::isReadOnly);
    }

    @Override
    public boolean isWrapperFor(final Class<?> iface) throws SQLException {
        if (iface.isAssignableFrom(getClass())) {
            return true;
        } else if (iface.isAssignableFrom(databaseMetaData.getClass())) {
            return true;
        } else {
            return databaseMetaData.isWrapperFor(iface);
        }
    }

    @Override
    public boolean locatorsUpdateCopy() throws SQLException {
        return applyToFalse(databaseMetaData::locatorsUpdateCopy);
    }

    @Override
    public boolean nullPlusNonNullIsNull() throws SQLException {
        return applyToFalse(databaseMetaData::nullPlusNonNullIsNull);
    }

    @Override
    public boolean nullsAreSortedAtEnd() throws SQLException {
        return applyToFalse(databaseMetaData::nullsAreSortedAtEnd);
    }

    @Override
    public boolean nullsAreSortedAtStart() throws SQLException {
        return applyToFalse(databaseMetaData::nullsAreSortedAtStart);
    }

    @Override
    public boolean nullsAreSortedHigh() throws SQLException {
        return applyToFalse(databaseMetaData::nullsAreSortedHigh);
    }

    @Override
    public boolean nullsAreSortedLow() throws SQLException {
        return applyToFalse(databaseMetaData::nullsAreSortedLow);
    }

    @Override
    public boolean othersDeletesAreVisible(final int type) throws SQLException {
        return applyIntTo(databaseMetaData::othersDeletesAreVisible, type, false);
    }

    @Override
    public boolean othersInsertsAreVisible(final int type) throws SQLException {
        return applyIntTo(databaseMetaData::othersInsertsAreVisible, type, false);
    }

    @Override
    public boolean othersUpdatesAreVisible(final int type) throws SQLException {
        return applyIntTo(databaseMetaData::othersUpdatesAreVisible, type, false);
    }

    @Override
    public boolean ownDeletesAreVisible(final int type) throws SQLException {
        return applyIntTo(databaseMetaData::ownDeletesAreVisible, type, false);
    }

    @Override
    public boolean ownInsertsAreVisible(final int type) throws SQLException {
        return applyIntTo(databaseMetaData::ownInsertsAreVisible, type, false);
    }

    @Override
    public boolean ownUpdatesAreVisible(final int type) throws SQLException {
        return applyIntTo(databaseMetaData::ownUpdatesAreVisible, type, false);
    }

    @Override
    public boolean storesLowerCaseIdentifiers() throws SQLException {
        return applyToFalse(databaseMetaData::storesLowerCaseIdentifiers);
    }

    @Override
    public boolean storesLowerCaseQuotedIdentifiers() throws SQLException {
        return applyToFalse(databaseMetaData::storesLowerCaseQuotedIdentifiers);
    }

    @Override
    public boolean storesMixedCaseIdentifiers() throws SQLException {
        return applyToFalse(databaseMetaData::storesMixedCaseIdentifiers);
    }

    @Override
    public boolean storesMixedCaseQuotedIdentifiers() throws SQLException {
        return applyToFalse(databaseMetaData::storesMixedCaseQuotedIdentifiers);
    }

    @Override
    public boolean storesUpperCaseIdentifiers() throws SQLException {
        return applyToFalse(databaseMetaData::storesUpperCaseIdentifiers);
    }

    @Override
    public boolean storesUpperCaseQuotedIdentifiers() throws SQLException {
        return applyToFalse(databaseMetaData::storesUpperCaseQuotedIdentifiers);
    }

    @Override
    public boolean supportsAlterTableWithAddColumn() throws SQLException {
        return applyToFalse(databaseMetaData::supportsAlterTableWithAddColumn);
    }

    @Override
    public boolean supportsAlterTableWithDropColumn() throws SQLException {
        return applyToFalse(databaseMetaData::supportsAlterTableWithDropColumn);
    }

    @Override
    public boolean supportsANSI92EntryLevelSQL() throws SQLException {
        return applyToFalse(databaseMetaData::supportsANSI92EntryLevelSQL);
    }

    @Override
    public boolean supportsANSI92FullSQL() throws SQLException {
        return applyToFalse(databaseMetaData::supportsANSI92FullSQL);
    }

    @Override
    public boolean supportsANSI92IntermediateSQL() throws SQLException {
        return applyToFalse(databaseMetaData::supportsANSI92IntermediateSQL);
    }

    @Override
    public boolean supportsBatchUpdates() throws SQLException {
        return applyToFalse(databaseMetaData::supportsBatchUpdates);
    }

    @Override
    public boolean supportsCatalogsInDataManipulation() throws SQLException {
        return applyToFalse(databaseMetaData::supportsCatalogsInDataManipulation);
    }

    @Override
    public boolean supportsCatalogsInIndexDefinitions() throws SQLException {
        return applyToFalse(databaseMetaData::supportsCatalogsInIndexDefinitions);
    }

    @Override
    public boolean supportsCatalogsInPrivilegeDefinitions() throws SQLException {
        return applyToFalse(databaseMetaData::supportsCatalogsInPrivilegeDefinitions);
    }

    @Override
    public boolean supportsCatalogsInProcedureCalls() throws SQLException {
        return applyToFalse(databaseMetaData::supportsCatalogsInProcedureCalls);
    }

    @Override
    public boolean supportsCatalogsInTableDefinitions() throws SQLException {
        return applyToFalse(databaseMetaData::supportsCatalogsInTableDefinitions);
    }

    @Override
    public boolean supportsColumnAliasing() throws SQLException {
        return applyToFalse(databaseMetaData::supportsColumnAliasing);
    }

    @Override
    public boolean supportsConvert() throws SQLException {
        return applyToFalse(databaseMetaData::supportsConvert);
    }

    @Override
    public boolean supportsConvert(final int fromType, final int toType) throws SQLException {
        return applyTo(databaseMetaData::supportsConvert, fromType, toType, false);
    }

    @Override
    public boolean supportsCoreSQLGrammar() throws SQLException {
        return applyToFalse(databaseMetaData::supportsCoreSQLGrammar);
    }

    @Override
    public boolean supportsCorrelatedSubqueries() throws SQLException {
        return applyToFalse(databaseMetaData::supportsCorrelatedSubqueries);
    }

    @Override
    public boolean supportsDataDefinitionAndDataManipulationTransactions() throws SQLException {
        return applyToFalse(databaseMetaData::supportsDataDefinitionAndDataManipulationTransactions);
    }

    @Override
    public boolean supportsDataManipulationTransactionsOnly() throws SQLException {
        return applyToFalse(databaseMetaData::supportsDataManipulationTransactionsOnly);
    }

    @Override
    public boolean supportsDifferentTableCorrelationNames() throws SQLException {
        return applyToFalse(databaseMetaData::supportsDifferentTableCorrelationNames);
    }

    @Override
    public boolean supportsExpressionsInOrderBy() throws SQLException {
        return applyToFalse(databaseMetaData::supportsExpressionsInOrderBy);
    }

    @Override
    public boolean supportsExtendedSQLGrammar() throws SQLException {
        return applyToFalse(databaseMetaData::supportsExtendedSQLGrammar);
    }

    @Override
    public boolean supportsFullOuterJoins() throws SQLException {
        return applyToFalse(databaseMetaData::supportsFullOuterJoins);
    }

    @Override
    public boolean supportsGetGeneratedKeys() throws SQLException {
        return applyToFalse(databaseMetaData::supportsGetGeneratedKeys);
    }

    @Override
    public boolean supportsGroupBy() throws SQLException {
        return applyToFalse(databaseMetaData::supportsGroupBy);
    }

    @Override
    public boolean supportsGroupByBeyondSelect() throws SQLException {
        return applyToFalse(databaseMetaData::supportsGroupByBeyondSelect);
    }

    @Override
    public boolean supportsGroupByUnrelated() throws SQLException {
        return applyToFalse(databaseMetaData::supportsGroupByUnrelated);
    }

    @Override
    public boolean supportsIntegrityEnhancementFacility() throws SQLException {
        return applyToFalse(databaseMetaData::supportsIntegrityEnhancementFacility);
    }

    @Override
    public boolean supportsLikeEscapeClause() throws SQLException {
        return applyToFalse(databaseMetaData::supportsLikeEscapeClause);
    }

    @Override
    public boolean supportsLimitedOuterJoins() throws SQLException {
        return applyToFalse(databaseMetaData::supportsLimitedOuterJoins);
    }

    @Override
    public boolean supportsMinimumSQLGrammar() throws SQLException {
        return applyToFalse(databaseMetaData::supportsMinimumSQLGrammar);
    }

    @Override
    public boolean supportsMixedCaseIdentifiers() throws SQLException {
        return applyToFalse(databaseMetaData::supportsMixedCaseIdentifiers);
    }

    @Override
    public boolean supportsMixedCaseQuotedIdentifiers() throws SQLException {
        return applyToFalse(databaseMetaData::supportsMixedCaseQuotedIdentifiers);
    }

    @Override
    public boolean supportsMultipleOpenResults() throws SQLException {
        return applyToFalse(databaseMetaData::supportsMultipleOpenResults);
    }

    @Override
    public boolean supportsMultipleResultSets() throws SQLException {
        return applyToFalse(databaseMetaData::supportsMultipleResultSets);
    }

    @Override
    public boolean supportsMultipleTransactions() throws SQLException {
        return applyToFalse(databaseMetaData::supportsMultipleTransactions);
    }

    @Override
    public boolean supportsNamedParameters() throws SQLException {
        return applyToFalse(databaseMetaData::supportsNamedParameters);
    }

    @Override
    public boolean supportsNonNullableColumns() throws SQLException {
        return applyToFalse(databaseMetaData::supportsNonNullableColumns);
    }

    @Override
    public boolean supportsOpenCursorsAcrossCommit() throws SQLException {
        return applyToFalse(databaseMetaData::supportsOpenCursorsAcrossCommit);
    }

    @Override
    public boolean supportsOpenCursorsAcrossRollback() throws SQLException {
        return applyToFalse(databaseMetaData::supportsOpenCursorsAcrossRollback);
    }

    @Override
    public boolean supportsOpenStatementsAcrossCommit() throws SQLException {
        return applyToFalse(databaseMetaData::supportsOpenStatementsAcrossCommit);
    }

    @Override
    public boolean supportsOpenStatementsAcrossRollback() throws SQLException {
        return applyToFalse(databaseMetaData::supportsOpenStatementsAcrossRollback);
    }

    @Override
    public boolean supportsOrderByUnrelated() throws SQLException {
        return applyToFalse(databaseMetaData::supportsOrderByUnrelated);
    }

    @Override
    public boolean supportsOuterJoins() throws SQLException {
        return applyToFalse(databaseMetaData::supportsOuterJoins);
    }

    @Override
    public boolean supportsPositionedDelete() throws SQLException {
        return applyToFalse(databaseMetaData::supportsPositionedDelete);
    }

    @Override
    public boolean supportsPositionedUpdate() throws SQLException {
        return applyToFalse(databaseMetaData::supportsPositionedUpdate);
    }

    /**
     * @since 2.5.0
     */
    @Override
    public boolean supportsRefCursors() throws SQLException {
        return applyToFalse(databaseMetaData::supportsRefCursors);
    }

    @Override
    public boolean supportsResultSetConcurrency(final int type, final int concurrency) throws SQLException {
        return applyTo(databaseMetaData::supportsResultSetConcurrency, type, concurrency, false);
    }

    @Override
    public boolean supportsResultSetHoldability(final int holdability) throws SQLException {
        return applyTo(databaseMetaData::supportsResultSetHoldability, holdability, false);
    }

    @Override
    public boolean supportsResultSetType(final int type) throws SQLException {
        return applyTo(databaseMetaData::supportsResultSetType, type, false);
    }

    @Override
    public boolean supportsSavepoints() throws SQLException {
        return applyToFalse(databaseMetaData::supportsSavepoints);
    }

    @Override
    public boolean supportsSchemasInDataManipulation() throws SQLException {
        return applyToFalse(databaseMetaData::supportsSchemasInDataManipulation);
    }

    @Override
    public boolean supportsSchemasInIndexDefinitions() throws SQLException {
        return applyToFalse(databaseMetaData::supportsSchemasInIndexDefinitions);
    }

    @Override
    public boolean supportsSchemasInPrivilegeDefinitions() throws SQLException {
        return applyToFalse(databaseMetaData::supportsSchemasInPrivilegeDefinitions);
    }

    @Override
    public boolean supportsSchemasInProcedureCalls() throws SQLException {
        return applyToFalse(databaseMetaData::supportsSchemasInProcedureCalls);
    }

    @Override
    public boolean supportsSchemasInTableDefinitions() throws SQLException {
        return applyToFalse(databaseMetaData::supportsSchemasInTableDefinitions);
    }

    @Override
    public boolean supportsSelectForUpdate() throws SQLException {
        return applyToFalse(databaseMetaData::supportsSelectForUpdate);
    }

    @Override
    public boolean supportsStatementPooling() throws SQLException {
        return applyToFalse(databaseMetaData::supportsStatementPooling);
    }

    @Override
    public boolean supportsStoredFunctionsUsingCallSyntax() throws SQLException {
        return applyToFalse(databaseMetaData::supportsStoredFunctionsUsingCallSyntax);
    }

    @Override
    public boolean supportsStoredProcedures() throws SQLException {
        return applyToFalse(databaseMetaData::supportsStoredProcedures);
    }

    @Override
    public boolean supportsSubqueriesInComparisons() throws SQLException {
        return applyToFalse(databaseMetaData::supportsSubqueriesInComparisons);
    }

    @Override
    public boolean supportsSubqueriesInExists() throws SQLException {
        return applyToFalse(databaseMetaData::supportsSubqueriesInExists);
    }

    @Override
    public boolean supportsSubqueriesInIns() throws SQLException {
        return applyToFalse(databaseMetaData::supportsSubqueriesInIns);
    }

    @Override
    public boolean supportsSubqueriesInQuantifieds() throws SQLException {
        return applyToFalse(databaseMetaData::supportsSubqueriesInQuantifieds);
    }

    @Override
    public boolean supportsTableCorrelationNames() throws SQLException {
        return applyToFalse(databaseMetaData::supportsTableCorrelationNames);
    }

    @Override
    public boolean supportsTransactionIsolationLevel(final int level) throws SQLException {
        return applyTo(databaseMetaData::supportsTransactionIsolationLevel, level, false);
    }

    @Override
    public boolean supportsTransactions() throws SQLException {
        return applyToFalse(databaseMetaData::supportsTransactions);
    }

    /* JDBC_4_ANT_KEY_BEGIN */

    @Override
    public boolean supportsUnion() throws SQLException {
        return applyToFalse(databaseMetaData::supportsUnion);
    }

    @Override
    public boolean supportsUnionAll() throws SQLException {
        return applyToFalse(databaseMetaData::supportsUnionAll);
    }

    private ResultSet toResultSet(final SQLFunction0<ResultSet> callableResultSet) throws SQLException {
        connection.checkOpen();
        return apply(() -> DelegatingResultSet.wrapResultSet(connection, callableResultSet.apply()));
    }

    private <T, U> ResultSet toResultSet(final SQLFunction2<T, U, ResultSet> callableResultSet, final T t, final U u)
        throws SQLException {
        connection.checkOpen();
        return apply(() -> DelegatingResultSet.wrapResultSet(connection, callableResultSet.apply(t, u)));
    }

    private <T, U, V> ResultSet toResultSet(final SQLFunction3<T, U, V, ResultSet> callableResultSet, final T t,
        final U u, final V v) throws SQLException {
        connection.checkOpen();
        return apply(() -> DelegatingResultSet.wrapResultSet(connection, callableResultSet.apply(t, u, v)));
    }

    private <T, U, V, X> ResultSet toResultSet(final SQLFunction4<T, U, V, X, ResultSet> callableResultSet, final T t,
        final U u, final V v, final X x) throws SQLException {
        connection.checkOpen();
        return apply(() -> DelegatingResultSet.wrapResultSet(connection, callableResultSet.apply(t, u, v, x)));
    }

    private <T, U, V, X, Y> ResultSet toResultSet(final SQLFunction5<T, U, V, X, Y, ResultSet> callableResultSet,
        final T t, final U u, final V v, final X x, final Y y) throws SQLException {
        connection.checkOpen();
        return apply(() -> DelegatingResultSet.wrapResultSet(connection, callableResultSet.apply(t, u, v, x, y)));
    }

    private <T, U, V, X, Y, Z> ResultSet toResultSet(final SQLFunction6<T, U, V, X, Y, Z, ResultSet> callableResultSet,
        final T t, final U u, final V v, final X x, final Y y, final Z z) throws SQLException {
        connection.checkOpen();
        return apply(() -> DelegatingResultSet.wrapResultSet(connection, callableResultSet.apply(t, u, v, x, y, z)));
    }

    /* JDBC_4_ANT_KEY_END */

    @Override
    public <T> T unwrap(final Class<T> iface) throws SQLException {
        if (iface.isAssignableFrom(getClass())) {
            return iface.cast(this);
        } else if (iface.isAssignableFrom(databaseMetaData.getClass())) {
            return iface.cast(databaseMetaData);
        } else {
            return databaseMetaData.unwrap(iface);
        }
    }

    @Override
    public boolean updatesAreDetected(final int type) throws SQLException {
        return applyTo(databaseMetaData::updatesAreDetected, type, false);
    }

    @Override
    public boolean usesLocalFilePerTable() throws SQLException {
        return applyToFalse(databaseMetaData::usesLocalFilePerTable);
    }

    @Override
    public boolean usesLocalFiles() throws SQLException {
        return applyToFalse(databaseMetaData::usesLocalFiles);
    }
}
