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

import java.sql.ResultSet;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.io.InputStream;
import java.sql.SQLWarning;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.io.Reader;
import java.sql.Statement;
import java.util.Map;
import java.sql.Ref;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Array;
import java.util.Calendar;

/**
 * A base delegating implementation of {@link ResultSet}.
 * <p>
 * All of the methods from the {@link ResultSet} interface
 * simply call the corresponding method on the "delegate"
 * provided in my constructor.
 * <p>
 * Extends AbandonedTrace to implement result set tracking and
 * logging of code which created the ResultSet. Tracking the
 * ResultSet ensures that the Statment which created it can
 * close any open ResultSet's on Statement close.
 *
 * @author Glenn L. Nielsen
 * @author James House (<a href="mailto:james@interobjective.com">james@interobjective.com</a>)
 */
public class DelegatingResultSet extends AbandonedTrace implements ResultSet {

    /** My delegate. **/
    private ResultSet _res;

    /** The Statement that created me, if any. **/
    private Statement _stmt;

    /**
     * Create a wrapper for the ResultSet which traces this
     * ResultSet to the Statement which created it and the
     * code which created it.
     *
     * @param Statement stmt which create this ResultSet
     * @param ResultSet to wrap
     */
    public DelegatingResultSet(Statement stmt, ResultSet res) {
        super((AbandonedTrace)stmt);
        this._stmt = stmt;
        this._res = res;
    }
    
    public static ResultSet wrapResultSet(Statement stmt, ResultSet rset) {
        if(null == rset) {
            return null;
        } else {
            return new DelegatingResultSet(stmt,rset);
        }
    }

    public ResultSet getDelegate() {
        return _res;
    }

    public boolean equals(Object obj) {
        ResultSet delegate = getInnermostDelegate();
        if (delegate == null) {
            return false;
        }
        if (obj instanceof DelegatingResultSet) {
            DelegatingResultSet s = (DelegatingResultSet) obj;
            return delegate.equals(s.getInnermostDelegate());
        }
        else {
            return delegate.equals(obj);
        }
    }

    public int hashCode() {
        Object obj = getInnermostDelegate();
        if (obj == null) {
            return 0;
        }
        return obj.hashCode();
    }

    /**
     * If my underlying {@link ResultSet} is not a
     * <tt>DelegatingResultSet</tt>, returns it,
     * otherwise recursively invokes this method on
     * my delegate.
     * <p>
     * Hence this method will return the first
     * delegate that is not a <tt>DelegatingResultSet</tt>,
     * or <tt>null</tt> when no non-<tt>DelegatingResultSet</tt>
     * delegate can be found by transversing this chain.
     * <p>
     * This method is useful when you may have nested
     * <tt>DelegatingResultSet</tt>s, and you want to make
     * sure to obtain a "genuine" {@link ResultSet}.
     */
    public ResultSet getInnermostDelegate() {
        ResultSet r = _res;
        while(r != null && r instanceof DelegatingResultSet) {
            r = ((DelegatingResultSet)r).getDelegate();
            if(this == r) {
                return null;
            }
        }
        return r;
    }
    
    public Statement getStatement() throws SQLException {
        return _stmt;
    }

    /**
     * Wrapper for close of ResultSet which removes this
     * result set from being traced then calls close on
     * the original ResultSet.
     */
    public void close() throws SQLException {
        if(_stmt != null) {
            ((AbandonedTrace)_stmt).removeTrace(this);
            _stmt = null;
        }
        _res.close();
    }

    public boolean next() throws SQLException { return _res.next();  }
    public boolean wasNull() throws SQLException { return _res.wasNull();  }
    public String getString(int columnIndex) throws SQLException { return _res.getString(columnIndex);  }
    public boolean getBoolean(int columnIndex) throws SQLException { return _res.getBoolean(columnIndex);  }
    public byte getByte(int columnIndex) throws SQLException { return _res.getByte(columnIndex); }
    public short getShort(int columnIndex) throws SQLException { return _res.getShort(columnIndex); }
    public int getInt(int columnIndex) throws SQLException { return _res.getInt(columnIndex); }
    public long getLong(int columnIndex) throws SQLException { return _res.getLong(columnIndex); }
    public float getFloat(int columnIndex) throws SQLException { return _res.getFloat(columnIndex); }
    public double getDouble(int columnIndex) throws SQLException { return _res.getDouble(columnIndex); }
    /** @deprecated */
    public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException { return _res.getBigDecimal(columnIndex); }
    public byte[] getBytes(int columnIndex) throws SQLException { return _res.getBytes(columnIndex); }
    public Date getDate(int columnIndex) throws SQLException { return _res.getDate(columnIndex); }
    public Time getTime(int columnIndex) throws SQLException { return _res.getTime(columnIndex); }
    public Timestamp getTimestamp(int columnIndex) throws SQLException { return _res.getTimestamp(columnIndex); }
    public InputStream getAsciiStream(int columnIndex) throws SQLException { return _res.getAsciiStream(columnIndex); }
    /** @deprecated */
    public InputStream getUnicodeStream(int columnIndex) throws SQLException { return _res.getUnicodeStream(columnIndex); }
    public InputStream getBinaryStream(int columnIndex) throws SQLException { return _res.getBinaryStream(columnIndex); }
    public String getString(String columnName) throws SQLException { return _res.getString(columnName); }
    public boolean getBoolean(String columnName) throws SQLException { return _res.getBoolean(columnName); }
    public byte getByte(String columnName) throws SQLException { return _res.getByte(columnName); }
    public short getShort(String columnName) throws SQLException { return _res.getShort(columnName); }
    public int getInt(String columnName) throws SQLException { return _res.getInt(columnName); }
    public long getLong(String columnName) throws SQLException { return _res.getLong(columnName); }
    public float getFloat(String columnName) throws SQLException { return _res.getFloat(columnName); }
    public double getDouble(String columnName) throws SQLException { return _res.getDouble(columnName); }
    /** @deprecated */
    public BigDecimal getBigDecimal(String columnName, int scale) throws SQLException { return _res.getBigDecimal(columnName); }
    public byte[] getBytes(String columnName) throws SQLException { return _res.getBytes(columnName); }
    public Date getDate(String columnName) throws SQLException { return _res.getDate(columnName); }
    public Time getTime(String columnName) throws SQLException { return _res.getTime(columnName); }
    public Timestamp getTimestamp(String columnName) throws SQLException { return _res.getTimestamp(columnName); }
    public InputStream getAsciiStream(String columnName) throws SQLException { return _res.getAsciiStream(columnName); }
    /** @deprecated */
    public InputStream getUnicodeStream(String columnName) throws SQLException { return _res.getUnicodeStream(columnName); }
    public InputStream getBinaryStream(String columnName) throws SQLException { return _res.getBinaryStream(columnName); }
    public SQLWarning getWarnings() throws SQLException { return _res.getWarnings();  }
    public void clearWarnings() throws SQLException { _res.clearWarnings();  }
    public String getCursorName() throws SQLException { return _res.getCursorName();  }
    public ResultSetMetaData getMetaData() throws SQLException { return _res.getMetaData();  }
    public Object getObject(int columnIndex) throws SQLException { return _res.getObject(columnIndex);  }
    public Object getObject(String columnName) throws SQLException { return _res.getObject(columnName);  }
    public int findColumn(String columnName) throws SQLException { return _res.findColumn(columnName);  }
    public Reader getCharacterStream(int columnIndex) throws SQLException { return _res.getCharacterStream(columnIndex);  }
    public Reader getCharacterStream(String columnName) throws SQLException { return _res.getCharacterStream(columnName);  }
    public BigDecimal getBigDecimal(int columnIndex) throws SQLException { return _res.getBigDecimal(columnIndex);  }
    public BigDecimal getBigDecimal(String columnName) throws SQLException { return _res.getBigDecimal(columnName);  }
    public boolean isBeforeFirst() throws SQLException { return _res.isBeforeFirst();  }
    public boolean isAfterLast() throws SQLException { return _res.isAfterLast();  }
    public boolean isFirst() throws SQLException { return _res.isFirst();  }
    public boolean isLast() throws SQLException { return _res.isLast();  }
    public void beforeFirst() throws SQLException { _res.beforeFirst();  }
    public void afterLast() throws SQLException { _res.afterLast();  }
    public boolean first() throws SQLException { return _res.first();  }
    public boolean last() throws SQLException { return _res.last();  }
    public int getRow() throws SQLException { return _res.getRow();  }
    public boolean absolute(int row) throws SQLException { return _res.absolute(row);  }
    public boolean relative(int rows) throws SQLException { return _res.relative(rows);  }
    public boolean previous() throws SQLException { return _res.previous();  }
    public void setFetchDirection(int direction) throws SQLException { _res.setFetchDirection(direction);  }
    public int getFetchDirection() throws SQLException { return _res.getFetchDirection();  }
    public void setFetchSize(int rows) throws SQLException { _res.setFetchSize(rows); }
    public int getFetchSize() throws SQLException { return _res.getFetchSize();  }
    public int getType() throws SQLException { return _res.getType();  }
    public int getConcurrency() throws SQLException { return _res.getConcurrency();  }
    public boolean rowUpdated() throws SQLException { return _res.rowUpdated();  }
    public boolean rowInserted() throws SQLException { return _res.rowInserted();  }
    public boolean rowDeleted() throws SQLException { return _res.rowDeleted();  }
    public void updateNull(int columnIndex) throws SQLException {  _res.updateNull(columnIndex);  }
    public void updateBoolean(int columnIndex, boolean x) throws SQLException {  _res.updateBoolean(columnIndex, x);  }
    public void updateByte(int columnIndex, byte x) throws SQLException {  _res.updateByte(columnIndex, x);  }
    public void updateShort(int columnIndex, short x) throws SQLException {  _res.updateShort(columnIndex, x);  }
    public void updateInt(int columnIndex, int x) throws SQLException {  _res.updateInt(columnIndex, x);  }
    public void updateLong(int columnIndex, long x) throws SQLException {  _res.updateLong(columnIndex, x); }
    public void updateFloat(int columnIndex, float x) throws SQLException {  _res.updateFloat(columnIndex, x);  }
    public void updateDouble(int columnIndex, double x) throws SQLException {  _res.updateDouble(columnIndex, x);  }
    public void updateBigDecimal(int columnIndex, BigDecimal x) throws SQLException {  _res.updateBigDecimal(columnIndex, x);  }
    public void updateString(int columnIndex, String x) throws SQLException {  _res.updateString(columnIndex, x);  }
    public void updateBytes(int columnIndex, byte[] x) throws SQLException {  _res.updateBytes(columnIndex, x); }
    public void updateDate(int columnIndex, Date x) throws SQLException {  _res.updateDate(columnIndex, x);  }
    public void updateTime(int columnIndex, Time x) throws SQLException {  _res.updateTime(columnIndex, x); }
    public void updateTimestamp(int columnIndex, Timestamp x) throws SQLException {  _res.updateTimestamp(columnIndex, x);  }
    public void updateAsciiStream(int columnIndex, InputStream x, int length) throws SQLException {  _res.updateAsciiStream(columnIndex, x, length);  }
    public void updateBinaryStream(int columnIndex, InputStream x, int length) throws SQLException {  _res.updateBinaryStream(columnIndex, x, length); }
    public void updateCharacterStream(int columnIndex, Reader x, int length) throws SQLException {  _res.updateCharacterStream(columnIndex, x, length); }
    public void updateObject(int columnIndex, Object x, int scale) throws SQLException {  _res.updateObject(columnIndex, x);  }
    public void updateObject(int columnIndex, Object x) throws SQLException {  _res.updateObject(columnIndex, x);  }
    public void updateNull(String columnName) throws SQLException {  _res.updateNull(columnName);  }
    public void updateBoolean(String columnName, boolean x) throws SQLException {  _res.updateBoolean(columnName, x);  }
    public void updateByte(String columnName, byte x) throws SQLException {  _res.updateByte(columnName, x);  }
    public void updateShort(String columnName, short x) throws SQLException {  _res.updateShort(columnName, x);  }
    public void updateInt(String columnName, int x) throws SQLException {  _res.updateInt(columnName, x);  }
    public void updateLong(String columnName, long x) throws SQLException {  _res.updateLong(columnName, x);  }
    public void updateFloat(String columnName, float x) throws SQLException {  _res.updateFloat(columnName, x);  }
    public void updateDouble(String columnName, double x) throws SQLException { _res.updateDouble(columnName, x);  }
    public void updateBigDecimal(String columnName, BigDecimal x) throws SQLException {  _res.updateBigDecimal(columnName, x);  }
    public void updateString(String columnName, String x) throws SQLException {  _res.updateString(columnName, x);  }
    public void updateBytes(String columnName, byte[] x) throws SQLException {  _res.updateBytes(columnName, x);  }
    public void updateDate(String columnName, Date x) throws SQLException {  _res.updateDate(columnName, x);  }
    public void updateTime(String columnName, Time x) throws SQLException {  _res.updateTime(columnName, x);  }
    public void updateTimestamp(String columnName, Timestamp x) throws SQLException {  _res.updateTimestamp(columnName, x);  }
    public void updateAsciiStream(String columnName, InputStream x, int length) throws SQLException {  _res.updateAsciiStream(columnName, x, length);  }
    public void updateBinaryStream(String columnName, InputStream x, int length) throws SQLException {  _res.updateBinaryStream(columnName, x, length);  }
    public void updateCharacterStream(String columnName, Reader reader, int length) throws SQLException {  _res.updateCharacterStream(columnName, reader, length);  }
    public void updateObject(String columnName, Object x, int scale) throws SQLException {  _res.updateObject(columnName, x);  }
    public void updateObject(String columnName, Object x) throws SQLException {  _res.updateObject(columnName, x);  }
    public void insertRow() throws SQLException {  _res.insertRow();  }
    public void updateRow() throws SQLException {  _res.updateRow();  }
    public void deleteRow() throws SQLException {  _res.deleteRow();  }
    public void refreshRow() throws SQLException {  _res.refreshRow();  }
    public void cancelRowUpdates() throws SQLException {  _res.cancelRowUpdates();  }
    public void moveToInsertRow() throws SQLException {  _res.moveToInsertRow();  }
    public void moveToCurrentRow() throws SQLException {  _res.moveToCurrentRow();  }
    public Object getObject(int i, Map map) throws SQLException { return _res.getObject(i, map);  }
    public Ref getRef(int i) throws SQLException { return _res.getRef(i);  }
    public Blob getBlob(int i) throws SQLException { return _res.getBlob(i);  }
    public Clob getClob(int i) throws SQLException { return _res.getClob(i);  }
    public Array getArray(int i) throws SQLException { return _res.getArray(i);  }
    public Object getObject(String colName, Map map) throws SQLException { return _res.getObject(colName, map);  }
    public Ref getRef(String colName) throws SQLException { return _res.getRef(colName);  }
    public Blob getBlob(String colName) throws SQLException { return _res.getBlob(colName);  }
    public Clob getClob(String colName) throws SQLException { return _res.getClob(colName);  }
    public Array getArray(String colName) throws SQLException { return _res.getArray(colName);  }
    public Date getDate(int columnIndex, Calendar cal) throws SQLException { return _res.getDate(columnIndex, cal);  }
    public Date getDate(String columnName, Calendar cal) throws SQLException { return _res.getDate(columnName, cal);  }
    public Time getTime(int columnIndex, Calendar cal) throws SQLException { return _res.getTime(columnIndex, cal);  }
    public Time getTime(String columnName, Calendar cal) throws SQLException { return _res.getTime(columnName, cal);  }
    public Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException { return _res.getTimestamp(columnIndex, cal);  }
    public Timestamp getTimestamp(String columnName, Calendar cal) throws SQLException { return _res.getTimestamp(columnName, cal);  }

    // ------------------- JDBC 3.0 -----------------------------------------
    // Will be commented by the build process on a JDBC 2.0 system

/* JDBC_3_ANT_KEY_BEGIN */

    public java.net.URL getURL(int columnIndex) throws SQLException {
        return _res.getURL(columnIndex);
    }

    public java.net.URL getURL(String columnName) throws SQLException {
        return _res.getURL(columnName);
    }

    public void updateRef(int columnIndex, java.sql.Ref x)
        throws SQLException {
        _res.updateRef(columnIndex, x);
    }

    public void updateRef(String columnName, java.sql.Ref x)
        throws SQLException {
        _res.updateRef(columnName, x);
    }

    public void updateBlob(int columnIndex, java.sql.Blob x)
        throws SQLException {
        _res.updateBlob(columnIndex, x);
    }

    public void updateBlob(String columnName, java.sql.Blob x)
        throws SQLException {
        _res.updateBlob(columnName, x);
    }

    public void updateClob(int columnIndex, java.sql.Clob x)
        throws SQLException {
        _res.updateClob(columnIndex, x);
    }

    public void updateClob(String columnName, java.sql.Clob x)
        throws SQLException {
        _res.updateClob(columnName, x);
    }

    public void updateArray(int columnIndex, java.sql.Array x)
        throws SQLException {
        _res.updateArray(columnIndex, x);
    }

    public void updateArray(String columnName, java.sql.Array x)
        throws SQLException {
        _res.updateArray(columnName, x);
    }

/* JDBC_3_ANT_KEY_END */
}
