/*
 * $Id: DelegatingPreparedStatement.java,v 1.12 2003/04/09 00:47:01 dgraham Exp $
 * $Revision: 1.12 $
 * $Date: 2003/04/09 00:47:01 $
 *
 * ====================================================================
 *
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2003 The Apache Software Foundation.  All rights
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
import java.sql.SQLWarning;
import java.util.Calendar;
import java.util.List;

/**
 * A base delegating implementation of {@link PreparedStatement}.
 * <p>
 * All of the methods from the {@link PreparedStatement} interface
 * simply check to see that the {@link PreparedStatement} is active,
 * and call the corresponding method on the "delegate"
 * provided in my constructor.
 * <p>
 * Extends AbandonedTrace to implement Statement tracking and
 * logging of code which created the Statement. Tracking the 
 * Statement ensures that the Connection which created it can
 * close any open Statement's on Connection close.
 *
 * @author Rodney Waldhoff
 * @author Glenn L. Nielsen
 * @author James House (<a href="mailto:james@interobjective.com">james@interobjective.com</a>)
 * @version $Revision: 1.12 $ $Date: 2003/04/09 00:47:01 $
 */
public class DelegatingPreparedStatement extends AbandonedTrace
        implements PreparedStatement {
    /** My delegate. */
    protected PreparedStatement _stmt = null;
    /** The connection that created me. **/
    protected DelegatingConnection _conn = null;

    /**
     * Create a wrapper for the Statement which traces this
     * Statement to the Connection which created it and the
     * code which created it.
     *
     * @param s the {@link PreparedStatement} to delegate all calls to.
     * @param c the {@link DelegatingConnection} that created this statement.
     */
    public DelegatingPreparedStatement(DelegatingConnection c,
                                       PreparedStatement s) {
        super(c);
        _stmt = s;
        _conn = c;
    }

    /**
     * Returns my underlying {@link PreparedStatement}.
     * @return my underlying {@link PreparedStatement}.
     */
    public PreparedStatement getDelegate() {
        return _stmt;
    }

    /**
     * If my underlying {@link PreparedStatement} is not a
     * <tt>DelegatingPreparedStatement</tt>, returns it,
     * otherwise recursively invokes this method on
     * my delegate.
     * <p>
     * Hence this method will return the first
     * delegate that is not a <tt>DelegatingPreparedStatement</tt>,
     * or <tt>null</tt> when no non-<tt>DelegatingPreparedStatement</tt>
     * delegate can be found by transversing this chain.
     * <p>
     * This method is useful when you may have nested
     * <tt>DelegatingPreparedStatement</tt>s, and you want to make
     * sure to obtain a "genuine" {@link PreparedStatement}.
     */
    public PreparedStatement getInnermostDelegate() {
        PreparedStatement s = _stmt;
        while(s != null && s instanceof DelegatingPreparedStatement) {
            s = ((DelegatingPreparedStatement)s).getDelegate();
            if(this == s) {
                return null;
            }
        }
        return s;
    }

    /** Sets my delegate. */
    public void setDelegate(PreparedStatement s) {
        _stmt = s;
    }

    /**
     * Close this DelegatingPreparedStatement, and close
     * any ResultSets that were not explicitly closed.
     */
    public void close() throws SQLException {
        _stmt.close();
        passivate();
    }

    public Connection getConnection() throws SQLException {
        checkOpen();
        return _conn; // return the delegating connection that created this
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
    public void setBytes(int parameterIndex, byte x[]) throws SQLException { checkOpen(); _stmt.setBytes(parameterIndex,x);}
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
    public void setRef (int i, Ref x) throws SQLException { checkOpen(); _stmt.setRef(i,x);}
    public void setBlob (int i, Blob x) throws SQLException { checkOpen(); _stmt.setBlob(i,x);}
    public void setClob (int i, Clob x) throws SQLException { checkOpen(); _stmt.setClob(i,x);}
    public void setArray (int i, Array x) throws SQLException { checkOpen(); _stmt.setArray(i,x);}
    public ResultSetMetaData getMetaData() throws SQLException { checkOpen(); return _stmt.getMetaData();}
    public void setDate(int parameterIndex, java.sql.Date x, Calendar cal) throws SQLException { checkOpen(); _stmt.setDate(parameterIndex,x,cal);}
    public void setTime(int parameterIndex, java.sql.Time x, Calendar cal) throws SQLException { checkOpen(); _stmt.setTime(parameterIndex,x,cal);}
    public void setTimestamp(int parameterIndex, java.sql.Timestamp x, Calendar cal) throws SQLException { checkOpen(); _stmt.setTimestamp(parameterIndex,x,cal);}
    public void setNull (int paramIndex, int sqlType, String typeName) throws SQLException { checkOpen(); _stmt.setNull(paramIndex,sqlType,typeName);}

    protected void checkOpen() throws SQLException {
        if (_closed) {
            throw new SQLException("PreparedStatement is closed.");
        }
    }

    protected void activate() {
        _closed = false;
        if(_stmt instanceof DelegatingPreparedStatement) {
            ((DelegatingPreparedStatement)_stmt).activate();
        }
    }

    protected void passivate() throws SQLException {
        _closed = true;
        if(_conn != null) {
            _conn.removeTrace(this);
           _conn = null;
        }
         
        // The JDBC spec requires that a statment close any open
        // ResultSet's when it is closed.
        // FIXME The PreparedStatement we're wrapping should handle this for us.
        // See bug 17301 for what could happen when ResultSets are closed twice.
        List resultSets = getTrace();
        if( resultSets != null) {
            ResultSet[] set = new ResultSet[resultSets.size()];
            resultSets.toArray(set);
            for (int i = 0; i < set.length; i++) {
                set[i].close();
            }
            clearTrace();
        }
        if(_stmt instanceof DelegatingPreparedStatement) {
            ((DelegatingPreparedStatement)_stmt).passivate();
        }
    }

    protected boolean isClosed() {
        return _closed;
    }

    protected boolean _closed = false;

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

/* JDBC_3_ANT_KEY_END */

}
