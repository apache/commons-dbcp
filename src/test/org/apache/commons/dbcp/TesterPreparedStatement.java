/*
 * $Id: TesterPreparedStatement.java,v 1.6 2003/03/06 19:25:36 rwaldhoff Exp $
 * $Revision: 1.6 $
 * $Date: 2003/03/06 19:25:36 $
 *
 * ====================================================================
 *
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2002 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Commons", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package org.apache.commons.dbcp;

import java.math.BigDecimal;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Calendar;

public class TesterPreparedStatement extends TesterStatement implements PreparedStatement {
    private ResultSetMetaData _resultSetMetaData = null;
    private String _sql = null;

    public TesterPreparedStatement(Connection conn) {
        super(conn);
    }

    public TesterPreparedStatement(Connection conn, String sql) {
        super(conn);
        _sql = sql;
    }

    public ResultSet executeQuery(String sql) throws SQLException {
        checkOpen();
        if("null".equals(sql)) {
            return null;
        } else {
            return new TesterResultSet(this);
        }
    }

    public int executeUpdate(String sql) throws SQLException {
        checkOpen();
        return _rowsUpdated;
    }

    public ResultSet executeQuery() throws SQLException {
        checkOpen();
        if("null".equals(_sql)) {
            return null;
        } else {
            return new TesterResultSet(this);
        }
    }

    public int executeUpdate() throws SQLException {
        checkOpen();
        return _rowsUpdated;
    }

    public void setNull(int parameterIndex, int sqlType) throws SQLException {
        checkOpen();
    }

    public void setBoolean(int parameterIndex, boolean x) throws SQLException {
        checkOpen();
    }

    public void setByte(int parameterIndex, byte x) throws SQLException {
        checkOpen();
    }

    public void setShort(int parameterIndex, short x) throws SQLException {
        checkOpen();
    }

    public void setInt(int parameterIndex, int x) throws SQLException {
        checkOpen();
    }

    public void setLong(int parameterIndex, long x) throws SQLException {
        checkOpen();
    }

    public void setFloat(int parameterIndex, float x) throws SQLException {
        checkOpen();
    }

    public void setDouble(int parameterIndex, double x) throws SQLException {
        checkOpen();
    }

    public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {
        checkOpen();
    }

    public void setString(int parameterIndex, String x) throws SQLException {
        checkOpen();
    }

    public void setBytes(int parameterIndex, byte x[]) throws SQLException {
        checkOpen();
    }

    public void setDate(int parameterIndex, java.sql.Date x) throws SQLException {
        checkOpen();
    }

    public void setTime(int parameterIndex, java.sql.Time x) throws SQLException {
        checkOpen();
    }

    public void setTimestamp(int parameterIndex, java.sql.Timestamp x) throws SQLException {
        checkOpen();
    }

    public void setAsciiStream(int parameterIndex, java.io.InputStream x, int length) throws SQLException {
        checkOpen();
    }

    /** @deprecated */
    public void setUnicodeStream(int parameterIndex, java.io.InputStream x, int length) throws SQLException {
        checkOpen();
    }

    public void setBinaryStream(int parameterIndex, java.io.InputStream x, int length) throws SQLException {
        checkOpen();
    }

    public void clearParameters() throws SQLException {
        checkOpen();
    }

    public void setObject(int parameterIndex, Object x, int targetSqlType, int scale) throws SQLException {
        checkOpen();
    }

    public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {
        checkOpen();
    }

    public void setObject(int parameterIndex, Object x) throws SQLException {
        checkOpen();
    }


    public boolean execute() throws SQLException {
        checkOpen(); return true;
    }

    public void addBatch() throws SQLException {
        checkOpen();
    }

    public void setCharacterStream(int parameterIndex, java.io.Reader reader, int length) throws SQLException {
        checkOpen();
    }

    public void setRef (int i, Ref x) throws SQLException {
        checkOpen();
    }

    public void setBlob (int i, Blob x) throws SQLException {
        checkOpen();
    }

    public void setClob (int i, Clob x) throws SQLException {
        checkOpen();
    }

    public void setArray (int i, Array x) throws SQLException {
        checkOpen();
    }

    public ResultSetMetaData getMetaData() throws SQLException {
        checkOpen();
        return _resultSetMetaData;
    }

    public void setDate(int parameterIndex, java.sql.Date x, Calendar cal) throws SQLException {
        checkOpen();
    }

    public void setTime(int parameterIndex, java.sql.Time x, Calendar cal) throws SQLException {
        checkOpen();
    }

    public void setTimestamp(int parameterIndex, java.sql.Timestamp x, Calendar cal) throws SQLException {
        checkOpen();
    }

    public void setNull (int paramIndex, int sqlType, String typeName) throws SQLException {
        checkOpen();
    }


    // ------------------- JDBC 3.0 -----------------------------------------
    // Will be commented by the build process on a JDBC 2.0 system

/* JDBC_3_ANT_KEY_BEGIN */

    public boolean getMoreResults(int current) throws SQLException {
        throw new SQLException("Not implemented.");
    }

    public ResultSet getGeneratedKeys() throws SQLException {
        throw new SQLException("Not implemented.");
    }

    public int executeUpdate(String sql, int autoGeneratedKeys)
        throws SQLException {
        throw new SQLException("Not implemented.");
    }

    public int executeUpdate(String sql, int columnIndexes[])
        throws SQLException {
        throw new SQLException("Not implemented.");
    }

    public int executeUpdate(String sql, String columnNames[])
        throws SQLException {
        throw new SQLException("Not implemented.");
    }

    public boolean execute(String sql, int autoGeneratedKeys)
        throws SQLException {
        throw new SQLException("Not implemented.");
    }

    public boolean execute(String sl, int columnIndexes[])
        throws SQLException {
        throw new SQLException("Not implemented.");
    }

    public boolean execute(String sql, String columnNames[])
        throws SQLException {
        throw new SQLException("Not implemented.");
    }

    public int getResultSetHoldability() throws SQLException {
        throw new SQLException("Not implemented.");
    }

    public void setURL(int parameterIndex, java.net.URL x)
        throws SQLException {
        throw new SQLException("Not implemented.");
    }

    public java.sql.ParameterMetaData getParameterMetaData() throws SQLException {
        throw new SQLException("Not implemented.");
    }

/* JDBC_3_ANT_KEY_END */

}
