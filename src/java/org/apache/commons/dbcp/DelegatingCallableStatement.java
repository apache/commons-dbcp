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
import java.io.InputStream;
import java.io.Reader;
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
 * @version $Revision: 1.17 $ $Date: 2004/02/28 11:48:04 $
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
