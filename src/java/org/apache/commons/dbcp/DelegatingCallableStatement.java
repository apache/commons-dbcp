/*
 * $Header: /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons//dbcp/src/java/org/apache/commons/dbcp/DelegatingCallableStatement.java,v 1.1 2002/05/16 21:25:37 glenn Exp $
 * $Revision: 1.1 $
 * $Date: 2002/05/16 21:25:37 $
 *
 * ====================================================================
 *
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999-2001 The Apache Software Foundation.  All rights
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

import java.sql.CallableStatement;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Map;
import java.sql.Ref;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Array;
import java.util.Calendar;
import java.sql.ResultSet;
import java.io.InputStream;
import java.io.Reader;
import java.sql.ResultSetMetaData;
import java.sql.SQLWarning;
import java.sql.Connection;
import java.sql.SQLException;

import java.util.List;
import java.util.Iterator;

/**
 * A base delegating implementation of {@link CallableStatement}.
 * <p>
 * All of the methods from the {@link CallableStatement} interface
 * simply call the corresponding method on the "delegate"
 * provided in my constructor.
 * <p>
 * Extends AbandonedTrace to implement Statement tracking and
 * logging of code which created the Statement. Tracking the
 * Statement ensures that the Connection which created it can
 * close any open Statement's on Connection close.
 *
 * @author Glenn L. Nielsen
 * @author James House (<a href="mailto:james@interobjective.com">james@interobjective.com</a>)
 */
public class DelegatingCallableStatement extends AbandonedTrace
        implements CallableStatement {

    /** My delegate. */
    protected CallableStatement _stmt = null;
    /** The connection that created me. **/
    protected DelegatingConnection _conn = null;

    /**
     * Create a wrapper for the Statement which traces this
     * Statement to the Connection which created it and the
     * code which created it.
     *
     * @param cs the {@link CallableStatement} to delegate all calls to.
     */
    public DelegatingCallableStatement(DelegatingConnection c,
                                       CallableStatement s) {
        super(c);
        _conn = c;
        _stmt = s;
    }

    /**
     * Close this DelegatingCallableStatement, and close
     * any ResultSets that were not explicitly closed.
     */
    public void close() throws SQLException {
        if(_conn != null) {
            _conn.removeTrace(this);
           _conn = null;
        }

        // The JDBC spec requires that a statment close any open
        // ResultSet's when it is closed.
        List resultSets = getTrace();
        if( resultSets != null) {
            Iterator it = resultSets.iterator();
            while(it.hasNext()) {
                ((ResultSet)it.next()).close();
            }
            clearTrace();
        }

        _stmt.close();
    }

    public Connection getConnection() throws SQLException {
      return _conn;
    }

    public ResultSet executeQuery() throws SQLException {
        return new DelegatingResultSet(this, _stmt.executeQuery());
    }

    public ResultSet getResultSet() throws SQLException {
        return new DelegatingResultSet(this, _stmt.getResultSet());
    }

    public ResultSet executeQuery(String sql) throws SQLException {
        return new DelegatingResultSet(this, _stmt.executeQuery(sql));
    }

    public void registerOutParameter(int parameterIndex, int sqlType) throws SQLException { _stmt.registerOutParameter( parameterIndex,  sqlType);  }
    public void registerOutParameter(int parameterIndex, int sqlType, int scale) throws SQLException { _stmt.registerOutParameter( parameterIndex,  sqlType,  scale);  }
    public boolean wasNull() throws SQLException { return _stmt.wasNull();  }
    public String getString(int parameterIndex) throws SQLException { return _stmt.getString( parameterIndex);  }
    public boolean getBoolean(int parameterIndex) throws SQLException { return _stmt.getBoolean( parameterIndex);  }
    public byte getByte(int parameterIndex) throws SQLException { return _stmt.getByte( parameterIndex);  }
    public short getShort(int parameterIndex) throws SQLException { return _stmt.getShort( parameterIndex);  }
    public int getInt(int parameterIndex) throws SQLException { return _stmt.getInt( parameterIndex);  }
    public long getLong(int parameterIndex) throws SQLException { return _stmt.getLong( parameterIndex);  }
    public float getFloat(int parameterIndex) throws SQLException { return _stmt.getFloat( parameterIndex);  }
    public double getDouble(int parameterIndex) throws SQLException { return _stmt.getDouble( parameterIndex);  }
    public BigDecimal getBigDecimal(int parameterIndex, int scale) throws SQLException { return _stmt.getBigDecimal( parameterIndex,  scale);  }
    public byte[] getBytes(int parameterIndex) throws SQLException { return _stmt.getBytes( parameterIndex);  }
    public Date getDate(int parameterIndex) throws SQLException { return _stmt.getDate( parameterIndex);  }
    public Time getTime(int parameterIndex) throws SQLException { return _stmt.getTime( parameterIndex);  }
    public Timestamp getTimestamp(int parameterIndex) throws SQLException { return _stmt.getTimestamp( parameterIndex);  }
    public Object getObject(int parameterIndex) throws SQLException { return _stmt.getObject( parameterIndex);  }
    public BigDecimal getBigDecimal(int parameterIndex) throws SQLException { return _stmt.getBigDecimal( parameterIndex);  }
    public Object getObject(int i, Map map) throws SQLException { return _stmt.getObject( i, map);  }
    public Ref getRef(int i) throws SQLException { return _stmt.getRef( i);  }
    public Blob getBlob(int i) throws SQLException { return _stmt.getBlob( i);  }
    public Clob getClob(int i) throws SQLException { return _stmt.getClob( i);  }
    public Array getArray(int i) throws SQLException { return _stmt.getArray( i);  }
    public Date getDate(int parameterIndex, Calendar cal) throws SQLException { return _stmt.getDate( parameterIndex,  cal);  }
    public Time getTime(int parameterIndex, Calendar cal) throws SQLException { return _stmt.getTime( parameterIndex,  cal);  }
    public Timestamp getTimestamp(int parameterIndex, Calendar cal) throws SQLException { return _stmt.getTimestamp( parameterIndex,  cal);  }
    public void registerOutParameter(int paramIndex, int sqlType, String typeName) throws SQLException { _stmt.registerOutParameter( paramIndex,  sqlType,  typeName);  }
    public int executeUpdate() throws SQLException { return _stmt.executeUpdate();  }
    public void setNull(int parameterIndex, int sqlType) throws SQLException { _stmt.setNull( parameterIndex,  sqlType);  }
    public void setBoolean(int parameterIndex, boolean x) throws SQLException { _stmt.setBoolean( parameterIndex,  x);  }
    public void setByte(int parameterIndex, byte x) throws SQLException { _stmt.setByte( parameterIndex,  x);  }
    public void setShort(int parameterIndex, short x) throws SQLException { _stmt.setShort( parameterIndex,  x);  }
    public void setInt(int parameterIndex, int x) throws SQLException { _stmt.setInt( parameterIndex,  x);  }
    public void setLong(int parameterIndex, long x) throws SQLException { _stmt.setLong( parameterIndex,  x);  }
    public void setFloat(int parameterIndex, float x) throws SQLException { _stmt.setFloat( parameterIndex,  x);  }
    public void setDouble(int parameterIndex, double x) throws SQLException { _stmt.setDouble( parameterIndex,  x);  }
    public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException { _stmt.setBigDecimal( parameterIndex,  x);  }
    public void setString(int parameterIndex, String x) throws SQLException { _stmt.setString( parameterIndex,  x);  }
    public void setBytes(int parameterIndex, byte[] x) throws SQLException { _stmt.setBytes( parameterIndex,  x);  }
    public void setDate(int parameterIndex, Date x) throws SQLException { _stmt.setDate( parameterIndex,  x);  }
    public void setTime(int parameterIndex, Time x) throws SQLException { _stmt.setTime( parameterIndex,  x);  }
    public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException { _stmt.setTimestamp( parameterIndex,  x);  }
    public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException { _stmt.setAsciiStream( parameterIndex,  x,  length);  }
    public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException { _stmt.setUnicodeStream( parameterIndex,  x,  length);  }
    public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException { _stmt.setBinaryStream( parameterIndex,  x,  length);  }
    public void clearParameters() throws SQLException { _stmt.clearParameters();  }
    public void setObject(int parameterIndex, Object x, int targetSqlType, int scale) throws SQLException { _stmt.setObject( parameterIndex,  x,  targetSqlType,  scale);  }
    public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException { _stmt.setObject( parameterIndex,  x,  targetSqlType);  }
    public void setObject(int parameterIndex, Object x) throws SQLException { _stmt.setObject( parameterIndex,  x);  }
    public boolean execute() throws SQLException { return _stmt.execute();  }
    public void addBatch() throws SQLException { _stmt.addBatch();  }
    public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException { _stmt.setCharacterStream( parameterIndex,  reader,  length);  }
    public void setRef(int i, Ref x) throws SQLException { _stmt.setRef( i,  x);  }
    public void setBlob(int i, Blob x) throws SQLException { _stmt.setBlob( i,  x);  }
    public void setClob(int i, Clob x) throws SQLException { _stmt.setClob( i,  x);  }
    public void setArray(int i, Array x) throws SQLException { _stmt.setArray( i,  x);  }
    public ResultSetMetaData getMetaData() throws SQLException { return _stmt.getMetaData();  }
    public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException { _stmt.setDate( parameterIndex,  x,  cal);  }
    public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException { _stmt.setTime( parameterIndex,  x,  cal);  }
    public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException { _stmt.setTimestamp( parameterIndex,  x,  cal);  }
    public void setNull(int paramIndex, int sqlType, String typeName) throws SQLException { _stmt.setNull( paramIndex,  sqlType,  typeName);  }

    public int executeUpdate(String sql) throws SQLException { return _stmt.executeUpdate( sql);  }
    public int getMaxFieldSize() throws SQLException { return _stmt.getMaxFieldSize();  }
    public void setMaxFieldSize(int max) throws SQLException { _stmt.setMaxFieldSize( max);  }
    public int getMaxRows() throws SQLException { return _stmt.getMaxRows();  }
    public void setMaxRows(int max) throws SQLException { _stmt.setMaxRows( max);  }
    public void setEscapeProcessing(boolean enable) throws SQLException { _stmt.setEscapeProcessing( enable);  }
    public int getQueryTimeout() throws SQLException { return _stmt.getQueryTimeout();  }
    public void setQueryTimeout(int seconds) throws SQLException { _stmt.setQueryTimeout( seconds);  }
    public void cancel() throws SQLException { _stmt.cancel();  }
    public SQLWarning getWarnings() throws SQLException { return _stmt.getWarnings();  }
    public void clearWarnings() throws SQLException { _stmt.clearWarnings();  }
    public void setCursorName(String name) throws SQLException { _stmt.setCursorName( name);  }
    public boolean execute(String sql) throws SQLException { return _stmt.execute( sql);  }


    public int getUpdateCount() throws SQLException { return _stmt.getUpdateCount();  }
    public boolean getMoreResults() throws SQLException { return _stmt.getMoreResults();  }
    public void setFetchDirection(int direction) throws SQLException { _stmt.setFetchDirection( direction);  }
    public int getFetchDirection() throws SQLException { return _stmt.getFetchDirection();  }
    public void setFetchSize(int rows) throws SQLException { _stmt.setFetchSize( rows);  }
    public int getFetchSize() throws SQLException { return _stmt.getFetchSize();  }
    public int getResultSetConcurrency() throws SQLException { return _stmt.getResultSetConcurrency();  }
    public int getResultSetType() throws SQLException { return _stmt.getResultSetType();  }
    public void addBatch(String sql) throws SQLException { _stmt.addBatch( sql);  }
    public void clearBatch() throws SQLException { _stmt.clearBatch();  }
    public int[] executeBatch() throws SQLException { return _stmt.executeBatch();  }
}
