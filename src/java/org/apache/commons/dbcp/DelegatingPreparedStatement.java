/*
 * Copyright 1999-2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.dbcp;

import java.math.BigDecimal;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Calendar;

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
 * @author Dirk Verbeeck
 * @version $Revision: 1.20 $ $Date: 2004/02/28 11:48:04 $
 */
public class DelegatingPreparedStatement extends DelegatingStatement
        implements PreparedStatement {

    /** My delegate. */
    protected PreparedStatement _stmt = null;

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
        super(c, s);
        _stmt = s;
    }

    public boolean equals(Object obj) {
        PreparedStatement delegate = (PreparedStatement) getInnermostDelegate();
        if (delegate == null) {
            return false;
        }
        if (obj instanceof DelegatingPreparedStatement) {
            DelegatingPreparedStatement s = (DelegatingPreparedStatement) obj;
            return delegate.equals(s.getInnermostDelegate());
        }
        else {
            return delegate.equals(obj);
        }
    }

    /** Sets my delegate. */
    public void setDelegate(PreparedStatement s) {
        super.setDelegate(s);
        _stmt = s;
    }

    public ResultSet executeQuery() throws SQLException {
        checkOpen();
        return DelegatingResultSet.wrapResultSet(this,_stmt.executeQuery());
    }

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

    // ------------------- JDBC 3.0 -----------------------------------------
    // Will be commented by the build process on a JDBC 2.0 system

/* JDBC_3_ANT_KEY_BEGIN */

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
