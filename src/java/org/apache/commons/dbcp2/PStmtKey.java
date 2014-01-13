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

import java.sql.PreparedStatement;

/**
 * A key uniquely identifying {@link PreparedStatement}s.
 */
public class PStmtKey {

    /** SQL defining Prepared or Callable Statement */
    private final String _sql;

    /** Result set type */
    private final Integer _resultSetType;

    /** Result set concurrency */
    private final Integer _resultSetConcurrency;

    /** Database catalog */
    private final String _catalog;

    /**
     *  Statement type. Either STATEMENT_PREPAREDSTMT (PreparedStatement)
     *  or STATEMENT_CALLABLESTMT (CallableStatement)
     */
    private final byte _stmtType;

    public PStmtKey(String sql) {
        this(sql, null, PoolingConnection.STATEMENT_PREPAREDSTMT);
    }

    public PStmtKey(String sql, String catalog) {
        this(sql, catalog, PoolingConnection.STATEMENT_PREPAREDSTMT);
    }

    public PStmtKey(String sql, String catalog, byte stmtType) {
        _sql = sql;
        _catalog = catalog;
        _stmtType = stmtType;
        _resultSetType = null;
        _resultSetConcurrency = null;
    }

    public  PStmtKey(String sql, int resultSetType, int resultSetConcurrency) {
        this(sql, null, resultSetType, resultSetConcurrency, PoolingConnection.STATEMENT_PREPAREDSTMT);
    }

    public PStmtKey(String sql, String catalog, int resultSetType, int resultSetConcurrency) {
        this(sql, catalog, resultSetType, resultSetConcurrency, PoolingConnection.STATEMENT_PREPAREDSTMT);
    }

    public PStmtKey(String sql, String catalog, int resultSetType, int resultSetConcurrency, byte stmtType) {
        _sql = sql;
        _catalog = catalog;
        _resultSetType = Integer.valueOf(resultSetType);
        _resultSetConcurrency = Integer.valueOf(resultSetConcurrency);
        _stmtType = stmtType;
    }


    public String getSql() {
        return _sql;
    }

    public Integer getResultSetType() {
        return _resultSetType;
    }

    public Integer getResultSetConcurrency() {
        return _resultSetConcurrency;
    }

    public String getCatalog() {
        return _catalog;
    }

    public byte getStmtType() {
        return _stmtType;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        PStmtKey key = (PStmtKey) obj;
        return( ((null == _sql && null == key._sql) || _sql.equals(key._sql)) &&
                ((null == _catalog && null == key._catalog) || _catalog.equals(key._catalog)) &&
                ((null == _resultSetType && null == key._resultSetType) || _resultSetType.equals(key._resultSetType)) &&
                ((null == _resultSetConcurrency && null == key._resultSetConcurrency) || _resultSetConcurrency.equals(key._resultSetConcurrency)) &&
                (_stmtType == key._stmtType)
              );
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((_catalog == null) ? 0 : _catalog.hashCode());
        result = prime * result + ((_resultSetConcurrency == null) ? 0 : _resultSetConcurrency.hashCode());
        result = prime * result + ((_resultSetType == null) ? 0 : _resultSetType.hashCode());
        result = prime * result + ((_sql == null) ? 0 : _sql.hashCode());
        result = prime * result + _stmtType;
        return result;
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("PStmtKey: sql=");
        buf.append(_sql);
        buf.append(", catalog=");
        buf.append(_catalog);
        buf.append(", resultSetType=");
        buf.append(_resultSetType);
        buf.append(", resultSetConcurrency=");
        buf.append(_resultSetConcurrency);
        buf.append(", statmentType=");
        buf.append(_stmtType);
        return buf.toString();
    }
}