/*
 * $Source: /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons//dbcp/src/java/org/apache/commons/dbcp/DelegatingCallableStatement.java,v $
 * $Revision: 1.16 $
 * $Date: 2003/12/26 17:03:35 $
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
 * @version $Revision: 1.16 $ $Date: 2003/12/26 17:03:35 $
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
