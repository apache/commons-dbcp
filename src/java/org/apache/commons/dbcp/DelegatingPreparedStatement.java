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
 * @author James House
 * @author Dirk Verbeeck
 * @version $Revision: 1.22 $ $Date: 2004/03/06 13:35:31 $
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
        try {
            return DelegatingResultSet.wrapResultSet(this,_stmt.executeQuery());
        }
        catch (SQLException e) {
            handleException(e);
            return null;
        }
    }

    public int executeUpdate() throws SQLException
    { checkOpen(); try { return _stmt.executeUpdate(); } catch (SQLException e) { handleException(e); return 0; } }

    public void setNull(int parameterIndex, int sqlType) throws SQLException
    { checkOpen(); try { _stmt.setNull(parameterIndex,sqlType); } catch (SQLException e) { handleException(e); } }

    public void setBoolean(int parameterIndex, boolean x) throws SQLException
    { checkOpen(); try { _stmt.setBoolean(parameterIndex,x); } catch (SQLException e) { handleException(e); } }

    public void setByte(int parameterIndex, byte x) throws SQLException
    { checkOpen(); try { _stmt.setByte(parameterIndex,x); } catch (SQLException e) { handleException(e); } }

    public void setShort(int parameterIndex, short x) throws SQLException
    { checkOpen(); try { _stmt.setShort(parameterIndex,x); } catch (SQLException e) { handleException(e); } }

    public void setInt(int parameterIndex, int x) throws SQLException
    { checkOpen(); try { _stmt.setInt(parameterIndex,x); } catch (SQLException e) { handleException(e); } }

    public void setLong(int parameterIndex, long x) throws SQLException
    { checkOpen(); try { _stmt.setLong(parameterIndex,x); } catch (SQLException e) { handleException(e); } }

    public void setFloat(int parameterIndex, float x) throws SQLException
    { checkOpen(); try { _stmt.setFloat(parameterIndex,x); } catch (SQLException e) { handleException(e); } }

    public void setDouble(int parameterIndex, double x) throws SQLException
    { checkOpen(); try { _stmt.setDouble(parameterIndex,x); } catch (SQLException e) { handleException(e); } }

    public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException
    { checkOpen(); try { _stmt.setBigDecimal(parameterIndex,x); } catch (SQLException e) { handleException(e); } }

    public void setString(int parameterIndex, String x) throws SQLException
    { checkOpen(); try { _stmt.setString(parameterIndex,x); } catch (SQLException e) { handleException(e); } }

    public void setBytes(int parameterIndex, byte[] x) throws SQLException
    { checkOpen(); try { _stmt.setBytes(parameterIndex,x); } catch (SQLException e) { handleException(e); } }

    public void setDate(int parameterIndex, java.sql.Date x) throws SQLException
    { checkOpen(); try { _stmt.setDate(parameterIndex,x); } catch (SQLException e) { handleException(e); } }

    public void setTime(int parameterIndex, java.sql.Time x) throws SQLException
    { checkOpen(); try { _stmt.setTime(parameterIndex,x); } catch (SQLException e) { handleException(e); } }

    public void setTimestamp(int parameterIndex, java.sql.Timestamp x) throws SQLException
    { checkOpen(); try { _stmt.setTimestamp(parameterIndex,x); } catch (SQLException e) { handleException(e); } }

    public void setAsciiStream(int parameterIndex, java.io.InputStream x, int length) throws SQLException
    { checkOpen(); try { _stmt.setAsciiStream(parameterIndex,x,length); } catch (SQLException e) { handleException(e); } }

    /** @deprecated */
    public void setUnicodeStream(int parameterIndex, java.io.InputStream x, int length) throws SQLException
    { checkOpen(); try { _stmt.setUnicodeStream(parameterIndex,x,length); } catch (SQLException e) { handleException(e); } }
    
    public void setBinaryStream(int parameterIndex, java.io.InputStream x, int length) throws SQLException
    { checkOpen(); try { _stmt.setBinaryStream(parameterIndex,x,length); } catch (SQLException e) { handleException(e); } }

    public void clearParameters() throws SQLException
    { checkOpen(); try { _stmt.clearParameters(); } catch (SQLException e) { handleException(e); } }

    public void setObject(int parameterIndex, Object x, int targetSqlType, int scale) throws SQLException
    { checkOpen(); try { _stmt.setObject(parameterIndex, x, targetSqlType, scale); } catch (SQLException e) { handleException(e); } }

    public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException
    { checkOpen(); try { _stmt.setObject(parameterIndex, x, targetSqlType); } catch (SQLException e) { handleException(e); } }

    public void setObject(int parameterIndex, Object x) throws SQLException
    { checkOpen(); try { _stmt.setObject(parameterIndex, x); } catch (SQLException e) { handleException(e); } }

    public boolean execute() throws SQLException
    { checkOpen(); try { return _stmt.execute(); } catch (SQLException e) { handleException(e); return false; } }

    public void addBatch() throws SQLException
    { checkOpen(); try { _stmt.addBatch(); } catch (SQLException e) { handleException(e); } }

    public void setCharacterStream(int parameterIndex, java.io.Reader reader, int length) throws SQLException
    { checkOpen(); try { _stmt.setCharacterStream(parameterIndex,reader,length); } catch (SQLException e) { handleException(e); } }

    public void setRef(int i, Ref x) throws SQLException
    { checkOpen(); try { _stmt.setRef(i,x); } catch (SQLException e) { handleException(e); } }

    public void setBlob(int i, Blob x) throws SQLException
    { checkOpen(); try { _stmt.setBlob(i,x); } catch (SQLException e) { handleException(e); } }

    public void setClob(int i, Clob x) throws SQLException
    { checkOpen(); try { _stmt.setClob(i,x); } catch (SQLException e) { handleException(e); } }

    public void setArray(int i, Array x) throws SQLException
    { checkOpen(); try { _stmt.setArray(i,x); } catch (SQLException e) { handleException(e); } }

    public ResultSetMetaData getMetaData() throws SQLException
    { checkOpen(); try { return _stmt.getMetaData(); } catch (SQLException e) { handleException(e); return null; } }

    public void setDate(int parameterIndex, java.sql.Date x, Calendar cal) throws SQLException
    { checkOpen(); try { _stmt.setDate(parameterIndex,x,cal); } catch (SQLException e) { handleException(e); } }

    public void setTime(int parameterIndex, java.sql.Time x, Calendar cal) throws SQLException
    { checkOpen(); try { _stmt.setTime(parameterIndex,x,cal); } catch (SQLException e) { handleException(e); } }

    public void setTimestamp(int parameterIndex, java.sql.Timestamp x, Calendar cal) throws SQLException
    { checkOpen(); try { _stmt.setTimestamp(parameterIndex,x,cal); } catch (SQLException e) { handleException(e); } }

    public void setNull(int paramIndex, int sqlType, String typeName) throws SQLException
    { checkOpen(); try { _stmt.setNull(paramIndex,sqlType,typeName); } catch (SQLException e) { handleException(e); } }

    // ------------------- JDBC 3.0 -----------------------------------------
    // Will be commented by the build process on a JDBC 2.0 system

/* JDBC_3_ANT_KEY_BEGIN */

    public void setURL(int parameterIndex, java.net.URL x) throws SQLException
    { checkOpen(); try { _stmt.setURL(parameterIndex, x); } catch (SQLException e) { handleException(e); } }

    public java.sql.ParameterMetaData getParameterMetaData() throws SQLException
    { checkOpen(); try { return _stmt.getParameterMetaData(); } catch (SQLException e) { handleException(e); return null; } }

/* JDBC_3_ANT_KEY_END */
}
