/*
 * $Source: /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons//dbcp/src/java/org/apache/commons/dbcp/DelegatingCallableStatement.java,v $
 * $Revision: 1.15 $
 * $Date: 2003/12/26 16:56:44 $
 *
 * ====================================================================
 *
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999-2003 The Apache Software Foundation.  All rights
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
 *    any, must include the following acknowledgement:
 *       "This product includes software developed by the
 *        Apache Software Foundation - http://www.apache.org/"
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowledgements normally appear.
 *
 * 4. The names "The Jakarta Project", "Commons", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Software Foundation.
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
 * http://www.apache.org/
 *
 */

package org.apache.commons.dbcp;

import java.net.URL;
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
import java.sql.SQLException;

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
 * @author Dirk Verbeeck
 * @version $Revision: 1.15 $ $Date: 2003/12/26 16:56:44 $
 */
public class DelegatingCallableStatement extends DelegatingPreparedStatement
        implements CallableStatement {

    /** My delegate. */
    protected CallableStatement _stmt = null;

    /**
     * Create a wrapper for the Statement which traces this
     * Statement to the Connection which created it and the
     * code which created it.
     *
     * @param cs the {@link CallableStatement} to delegate all calls to.
     */
    public DelegatingCallableStatement(DelegatingConnection c,
                                       CallableStatement s) {
        super(c, s);
        _stmt = s;
    }

    public boolean equals(Object obj) {
        CallableStatement delegate = (CallableStatement) getInnermostDelegate();
        if (delegate == null) {
            return false;
        }
        if (obj instanceof DelegatingCallableStatement) {
            DelegatingCallableStatement s = (DelegatingCallableStatement) obj;
            return delegate.equals(s.getInnermostDelegate());
        }
        else {
            return delegate.equals(obj);
        }
    }

    /** Sets my delegate. */
    public void setDelegate(CallableStatement s) {
        super.setDelegate(s);
        _stmt = s;
    }

    public ResultSet executeQuery(String sql) throws SQLException {
        checkOpen();
        return DelegatingResultSet.wrapResultSet(this,_stmt.executeQuery(sql));
    }

    public ResultSet getResultSet() throws SQLException {
        checkOpen();
        return DelegatingResultSet.wrapResultSet(this,_stmt.getResultSet());
    }

    public ResultSet executeQuery() throws SQLException {
        checkOpen();
        return DelegatingResultSet.wrapResultSet(this,_stmt.executeQuery());
    }

    public int executeUpdate(String sql) throws SQLException { checkOpen(); return _stmt.executeUpdate(sql);}
    public int getMaxFieldSize() throws SQLException { checkOpen(); return _stmt.getMaxFieldSize();}
    public void setMaxFieldSize(int max) throws SQLException { checkOpen();_stmt.setMaxFieldSize(max);}
    public int getMaxRows() throws SQLException { checkOpen();return _stmt.getMaxRows();}
    public void setMaxRows(int max) throws SQLException { checkOpen(); _stmt.setMaxRows(max);}
    public void setEscapeProcessing(boolean enable) throws SQLException { checkOpen();_stmt.setEscapeProcessing(enable);}
    public int getQueryTimeout() throws SQLException { checkOpen(); return _stmt.getQueryTimeout();}
    public void setQueryTimeout(int seconds) throws SQLException { checkOpen(); _stmt.setQueryTimeout(seconds);}
    public void cancel() throws SQLException { checkOpen(); _stmt.cancel();}
    public SQLWarning getWarnings() throws SQLException { checkOpen(); return _stmt.getWarnings();}
    public void clearWarnings() throws SQLException { checkOpen(); _stmt.clearWarnings();}
    public void setCursorName(String name) throws SQLException { checkOpen(); _stmt.setCursorName(name);}
    public boolean execute(String sql) throws SQLException { checkOpen(); return _stmt.execute(sql);}

    public int getUpdateCount() throws SQLException { checkOpen(); return _stmt.getUpdateCount();}
    public boolean getMoreResults() throws SQLException { checkOpen(); return _stmt.getMoreResults();}
    public void setFetchDirection(int direction) throws SQLException { checkOpen(); _stmt.setFetchDirection(direction);}
    public int getFetchDirection() throws SQLException { checkOpen(); return _stmt.getFetchDirection();}
    public void setFetchSize(int rows) throws SQLException { checkOpen(); _stmt.setFetchSize(rows);}
    public int getFetchSize() throws SQLException { checkOpen(); return _stmt.getFetchSize();}
    public int getResultSetConcurrency() throws SQLException { checkOpen(); return _stmt.getResultSetConcurrency();}
    public int getResultSetType() throws SQLException { checkOpen(); return _stmt.getResultSetType();}
    public void addBatch(String sql) throws SQLException { checkOpen(); _stmt.addBatch(sql);}
    public void clearBatch() throws SQLException { checkOpen(); _stmt.clearBatch();}
    public int[] executeBatch() throws SQLException { checkOpen(); return _stmt.executeBatch();}

    public int executeUpdate() throws SQLException { checkOpen(); return _stmt.executeUpdate();}
    public void setNull(int parameterIndex, int sqlType) throws SQLException { checkOpen(); _stmt.setNull(parameterIndex,sqlType);}
    public void setBoolean(int parameterIndex, boolean x) throws SQLException { checkOpen(); _stmt.setBoolean(parameterIndex,x);}
    public void setByte(int parameterIndex, byte x) throws SQLException { checkOpen(); _stmt.setByte(parameterIndex,x);}
    public void setShort(int parameterIndex, short x) throws SQLException { checkOpen(); _stmt.setShort(parameterIndex,x);}
    public void setInt(int parameterIndex, int x) throws SQLException { checkOpen(); _stmt.setInt(parameterIndex,x);}
    public void setLong(int parameterIndex, long x) throws SQLException { checkOpen(); _stmt.setLong(parameterIndex,x);}
    public void setFloat(int parameterIndex, float x) throws SQLException { checkOpen(); _stmt.setFloat(parameterIndex,x);}
    public void setDouble(int parameterIndex, double x) throws SQLException { checkOpen(); _stmt.setDouble(parameterIndex,x);}
    public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException { checkOpen(); _stmt.setBigDecimal(parameterIndex,x);}
    public void setString(int parameterIndex, String x) throws SQLException { checkOpen(); _stmt.setString(parameterIndex,x);}
    public void setBytes(int parameterIndex, byte[] x) throws SQLException { checkOpen(); _stmt.setBytes(parameterIndex,x);}
    public void setDate(int parameterIndex, java.sql.Date x) throws SQLException { checkOpen(); _stmt.setDate(parameterIndex,x);}
    public void setTime(int parameterIndex, java.sql.Time x) throws SQLException { checkOpen(); _stmt.setTime(parameterIndex,x);}
    public void setTimestamp(int parameterIndex, java.sql.Timestamp x) throws SQLException { checkOpen(); _stmt.setTimestamp(parameterIndex,x);}
    public void setAsciiStream(int parameterIndex, java.io.InputStream x, int length) throws SQLException { checkOpen(); _stmt.setAsciiStream(parameterIndex,x,length);}
    /** @deprecated */
    public void setUnicodeStream(int parameterIndex, java.io.InputStream x, int length) throws SQLException { checkOpen(); _stmt.setUnicodeStream(parameterIndex,x,length);}
    public void setBinaryStream(int parameterIndex, java.io.InputStream x, int length) throws SQLException{ checkOpen(); _stmt.setBinaryStream(parameterIndex,x,length);}
    public void clearParameters() throws SQLException { checkOpen(); _stmt.clearParameters();}
    public void setObject(int parameterIndex, Object x, int targetSqlType, int scale) throws SQLException { checkOpen(); _stmt.setObject(parameterIndex, x, targetSqlType, scale);}
    public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException { checkOpen(); _stmt.setObject(parameterIndex, x, targetSqlType);}
    public void setObject(int parameterIndex, Object x) throws SQLException { checkOpen(); _stmt.setObject(parameterIndex, x);}
    public boolean execute() throws SQLException { checkOpen(); return _stmt.execute();}
    public void addBatch() throws SQLException { checkOpen(); _stmt.addBatch();}
    public void setCharacterStream(int parameterIndex, java.io.Reader reader, int length) throws SQLException { checkOpen(); _stmt.setCharacterStream(parameterIndex,reader,length);}
    public void setRef(int i, Ref x) throws SQLException { checkOpen(); _stmt.setRef(i,x);}
    public void setBlob(int i, Blob x) throws SQLException { checkOpen(); _stmt.setBlob(i,x);}
    public void setClob(int i, Clob x) throws SQLException { checkOpen(); _stmt.setClob(i,x);}
    public void setArray(int i, Array x) throws SQLException { checkOpen(); _stmt.setArray(i,x);}
    public ResultSetMetaData getMetaData() throws SQLException { checkOpen(); return _stmt.getMetaData();}
    public void setDate(int parameterIndex, java.sql.Date x, Calendar cal) throws SQLException { checkOpen(); _stmt.setDate(parameterIndex,x,cal);}
    public void setTime(int parameterIndex, java.sql.Time x, Calendar cal) throws SQLException { checkOpen(); _stmt.setTime(parameterIndex,x,cal);}
    public void setTimestamp(int parameterIndex, java.sql.Timestamp x, Calendar cal) throws SQLException { checkOpen(); _stmt.setTimestamp(parameterIndex,x,cal);}
    public void setNull(int paramIndex, int sqlType, String typeName) throws SQLException { checkOpen(); _stmt.setNull(paramIndex,sqlType,typeName);}

    public void registerOutParameter(int parameterIndex, int sqlType) throws SQLException { checkOpen(); _stmt.registerOutParameter( parameterIndex,  sqlType);  }
    public void registerOutParameter(int parameterIndex, int sqlType, int scale) throws SQLException { checkOpen(); _stmt.registerOutParameter( parameterIndex,  sqlType,  scale);  }
    public boolean wasNull() throws SQLException { checkOpen(); return _stmt.wasNull();  }
    public String getString(int parameterIndex) throws SQLException { checkOpen(); return _stmt.getString( parameterIndex);  }
    public boolean getBoolean(int parameterIndex) throws SQLException { checkOpen(); return _stmt.getBoolean( parameterIndex);  }
    public byte getByte(int parameterIndex) throws SQLException { checkOpen(); return _stmt.getByte( parameterIndex);  }
    public short getShort(int parameterIndex) throws SQLException { checkOpen(); return _stmt.getShort( parameterIndex);  }
    public int getInt(int parameterIndex) throws SQLException { checkOpen(); return _stmt.getInt( parameterIndex);  }
    public long getLong(int parameterIndex) throws SQLException { checkOpen(); return _stmt.getLong( parameterIndex);  }
    public float getFloat(int parameterIndex) throws SQLException { checkOpen(); return _stmt.getFloat( parameterIndex);  }
    public double getDouble(int parameterIndex) throws SQLException { checkOpen(); return _stmt.getDouble( parameterIndex);  }
    /** @deprecated */
    public BigDecimal getBigDecimal(int parameterIndex, int scale) throws SQLException { checkOpen(); return _stmt.getBigDecimal( parameterIndex,  scale);  }
    public byte[] getBytes(int parameterIndex) throws SQLException { checkOpen(); return _stmt.getBytes( parameterIndex);  }
    public Date getDate(int parameterIndex) throws SQLException { checkOpen(); return _stmt.getDate( parameterIndex);  }
    public Time getTime(int parameterIndex) throws SQLException { checkOpen(); return _stmt.getTime( parameterIndex);  }
    public Timestamp getTimestamp(int parameterIndex) throws SQLException { checkOpen(); return _stmt.getTimestamp( parameterIndex);  }
    public Object getObject(int parameterIndex) throws SQLException { checkOpen(); return _stmt.getObject( parameterIndex);  }
    public BigDecimal getBigDecimal(int parameterIndex) throws SQLException { checkOpen(); return _stmt.getBigDecimal( parameterIndex);  }
    public Object getObject(int i, Map map) throws SQLException { checkOpen(); return _stmt.getObject( i, map);  }
    public Ref getRef(int i) throws SQLException { checkOpen(); return _stmt.getRef( i);  }
    public Blob getBlob(int i) throws SQLException { checkOpen(); return _stmt.getBlob( i);  }
    public Clob getClob(int i) throws SQLException { checkOpen(); return _stmt.getClob( i);  }
    public Array getArray(int i) throws SQLException { checkOpen(); return _stmt.getArray( i);  }
    public Date getDate(int parameterIndex, Calendar cal) throws SQLException { checkOpen(); return _stmt.getDate( parameterIndex,  cal);  }
    public Time getTime(int parameterIndex, Calendar cal) throws SQLException { checkOpen(); return _stmt.getTime( parameterIndex,  cal);  }
    public Timestamp getTimestamp(int parameterIndex, Calendar cal) throws SQLException { checkOpen(); return _stmt.getTimestamp( parameterIndex,  cal);  }
    public void registerOutParameter(int paramIndex, int sqlType, String typeName) throws SQLException { checkOpen(); _stmt.registerOutParameter( paramIndex,  sqlType,  typeName);  }

    // ------------------- JDBC 3.0 -----------------------------------------
    // Will be commented by the build process on a JDBC 2.0 system

/* JDBC_3_ANT_KEY_BEGIN */

    public boolean getMoreResults(int current) throws SQLException {
        checkOpen();
        return _stmt.getMoreResults(current);
    }

    public ResultSet getGeneratedKeys() throws SQLException {
        checkOpen();
        return _stmt.getGeneratedKeys();
    }

    public int executeUpdate(String sql, int autoGeneratedKeys)
        throws SQLException {
        checkOpen();
        return _stmt.executeUpdate(sql, autoGeneratedKeys);
    }

    public int executeUpdate(String sql, int columnIndexes[])
        throws SQLException {
        checkOpen();
        return _stmt.executeUpdate(sql, columnIndexes);
    }

    public int executeUpdate(String sql, String columnNames[])
        throws SQLException {
        checkOpen();
        return _stmt.executeUpdate(sql, columnNames);
    }

    public boolean execute(String sql, int autoGeneratedKeys)
        throws SQLException {
        checkOpen();
        return _stmt.execute(sql, autoGeneratedKeys);
    }

    public boolean execute(String sql, int columnIndexes[])
        throws SQLException {
        checkOpen();
        return _stmt.execute(sql, columnIndexes);
    }

    public boolean execute(String sql, String columnNames[])
        throws SQLException {
        checkOpen();
        return _stmt.execute(sql, columnNames);
    }

    public int getResultSetHoldability() throws SQLException {
        checkOpen();
        return _stmt.getResultSetHoldability();
    }

    public void setURL(int parameterIndex, java.net.URL x)
        throws SQLException {
        checkOpen();
        _stmt.setURL(parameterIndex, x);
    }

    public java.sql.ParameterMetaData getParameterMetaData() throws SQLException {
        checkOpen();
        return _stmt.getParameterMetaData();
    }

    public void registerOutParameter(String parameterName, int sqlType)
        throws SQLException {
        checkOpen();
        _stmt.registerOutParameter(parameterName, sqlType);
    }

    public void registerOutParameter(String parameterName,
        int sqlType, int scale) throws SQLException {
        checkOpen();
        _stmt.registerOutParameter(parameterName, sqlType, scale);
    }

    public void registerOutParameter(String parameterName,
        int sqlType, String typeName) throws SQLException {
        checkOpen();
        _stmt.registerOutParameter(parameterName, sqlType, typeName);
    }

    public URL getURL(int parameterIndex) throws SQLException {
        checkOpen();
        return _stmt.getURL(parameterIndex);
    }

    public void setURL(String parameterName, URL val) throws SQLException {
        checkOpen();
        _stmt.setURL(parameterName, val);
    }

    public void setNull(String parameterName, int sqlType)
        throws SQLException {
        checkOpen();
        _stmt.setNull(parameterName, sqlType);
    }

    public void setBoolean(String parameterName, boolean x)
        throws SQLException {
        checkOpen();
        _stmt.setBoolean(parameterName, x);
    }

    public void setByte(String parameterName, byte x)
        throws SQLException {
        checkOpen();
        _stmt.setByte(parameterName, x);
    }

    public void setShort(String parameterName, short x)
        throws SQLException {
        checkOpen();
        _stmt.setShort(parameterName, x);
    }

    public void setInt(String parameterName, int x)
        throws SQLException {
        checkOpen();
        _stmt.setInt(parameterName, x);
    }

    public void setLong(String parameterName, long x)
        throws SQLException {
        checkOpen();
        _stmt.setLong(parameterName, x);
    }

    public void setFloat(String parameterName, float x)
        throws SQLException {
        checkOpen();
        _stmt.setFloat(parameterName, x);
    }

    public void setDouble(String parameterName, double x)
        throws SQLException {
        checkOpen();
        _stmt.setDouble(parameterName, x);
    }

    public void setBigDecimal(String parameterName, BigDecimal x)
        throws SQLException {
        checkOpen();
        _stmt.setBigDecimal(parameterName, x);
    }

    public void setString(String parameterName, String x)
        throws SQLException {
        checkOpen();
        _stmt.setString(parameterName, x);
    }

    public void setBytes(String parameterName, byte [] x)
        throws SQLException {
        checkOpen();
        _stmt.setBytes(parameterName, x);
    }

    public void setDate(String parameterName, Date x)
        throws SQLException {
        checkOpen();
        _stmt.setDate(parameterName, x);
    }

    public void setTime(String parameterName, Time x)
        throws SQLException {
        checkOpen();
        _stmt.setTime(parameterName, x);
    }

    public void setTimestamp(String parameterName, Timestamp x)
        throws SQLException {
        checkOpen();
        _stmt.setTimestamp(parameterName, x);
    }

    public void setAsciiStream(String parameterName,
        InputStream x, int length)
        throws SQLException {
        checkOpen();
        _stmt.setAsciiStream(parameterName, x, length);
    }

    public void setBinaryStream(String parameterName,
        InputStream x, int length)
        throws SQLException {
        checkOpen();
        _stmt.setBinaryStream(parameterName, x, length);
    }

    public void setObject(String parameterName,
        Object x, int targetSqlType, int scale)
        throws SQLException {
        checkOpen();
        _stmt.setObject(parameterName, x, targetSqlType, scale);
    }

    public void setObject(String parameterName,
        Object x, int targetSqlType)
        throws SQLException {
        checkOpen();
        _stmt.setObject(parameterName, x, targetSqlType);
    }

    public void setObject(String parameterName, Object x)
        throws SQLException {
        checkOpen();
        _stmt.setObject(parameterName, x);
    }

    public void setCharacterStream(String parameterName,
        Reader reader, int length) throws SQLException {
        checkOpen();
        _stmt.setCharacterStream(parameterName, reader, length);
    }

    public void setDate(String parameterName,
        Date x, Calendar cal) throws SQLException {
        checkOpen();
        _stmt.setDate(parameterName, x, cal);
    }

    public void setTime(String parameterName,
        Time x, Calendar cal) throws SQLException {
        checkOpen();
        _stmt.setTime(parameterName, x, cal);
    }

    public void setTimestamp(String parameterName,
        Timestamp x, Calendar cal) throws SQLException {
        checkOpen();
        _stmt.setTimestamp(parameterName, x, cal);
    }

    public void setNull(String parameterName,
        int sqlType, String typeName) throws SQLException {
        checkOpen();
        _stmt.setNull(parameterName, sqlType, typeName);
    }

    public String getString(String parameterName) throws SQLException {
        checkOpen();
        return _stmt.getString(parameterName);
    }

    public boolean getBoolean(String parameterName) throws SQLException {
        checkOpen();
        return _stmt.getBoolean(parameterName);
    }

    public byte getByte(String parameterName) throws SQLException {
        checkOpen();
        return _stmt.getByte(parameterName);
    }

    public short getShort(String parameterName) throws SQLException {
        checkOpen();
        return _stmt.getShort(parameterName);
    }

    public int getInt(String parameterName) throws SQLException {
        checkOpen();
        return _stmt.getInt(parameterName);
    }

    public long getLong(String parameterName) throws SQLException {
        checkOpen();
        return _stmt.getLong(parameterName);
    }

    public float getFloat(String parameterName) throws SQLException {
        checkOpen();
        return _stmt.getFloat(parameterName);
    }

    public double getDouble(String parameterName) throws SQLException {
        checkOpen();
        return _stmt.getDouble(parameterName);
    }

    public byte[] getBytes(String parameterName) throws SQLException {
        checkOpen();
        return _stmt.getBytes(parameterName);
    }

    public Date getDate(String parameterName) throws SQLException {
        checkOpen();
        return _stmt.getDate(parameterName);
    }

    public Time getTime(String parameterName) throws SQLException {
        checkOpen();
        return _stmt.getTime(parameterName);
    }

    public Timestamp getTimestamp(String parameterName) throws SQLException {
        checkOpen();
        return _stmt.getTimestamp(parameterName);
    }

    public Object getObject(String parameterName) throws SQLException {
        checkOpen();
        return _stmt.getObject(parameterName);
    }

    public BigDecimal getBigDecimal(String parameterName) throws SQLException {
        checkOpen();
        return _stmt.getBigDecimal(parameterName);
    }

    public Object getObject(String parameterName, Map map)
        throws SQLException {
        checkOpen();
        return _stmt.getObject(parameterName, map);
    }

    public Ref getRef(String parameterName) throws SQLException {
        checkOpen();
        return _stmt.getRef(parameterName);
    }

    public Blob getBlob(String parameterName) throws SQLException {
        checkOpen();
        return _stmt.getBlob(parameterName);
    }

    public Clob getClob(String parameterName) throws SQLException {
        checkOpen();
        return _stmt.getClob(parameterName);
    }

    public Array getArray(String parameterName) throws SQLException {
        checkOpen();
        return _stmt.getArray(parameterName);
    }

    public Date getDate(String parameterName, Calendar cal)
        throws SQLException {
        checkOpen();
        return _stmt.getDate(parameterName, cal);
    }

    public Time getTime(String parameterName, Calendar cal)
        throws SQLException {
        checkOpen();
        return _stmt.getTime(parameterName, cal);
    }

    public Timestamp getTimestamp(String parameterName, Calendar cal)
        throws SQLException {
        checkOpen();
        return _stmt.getTimestamp(parameterName, cal);
    }

    public URL getURL(String parameterName) throws SQLException {
        checkOpen();
        return _stmt.getURL(parameterName);
    }

/* JDBC_3_ANT_KEY_END */
}
