/*
 * $Header: /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons//dbcp/src/java/org/apache/commons/dbcp/DelegatingStatement.java,v 1.5 2002/06/29 17:36:37 glenn Exp $
 * $Revision: 1.5 $
 * $Date: 2002/06/29 17:36:37 $
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

import java.sql.*;

import java.util.List;
import java.util.Iterator;

/**
 * A base delegating implementation of {@link Statement}.
 * <p>
 * All of the methods from the {@link Statement} interface
 * simply check to see that the {@link Statement} is active,
 * and call the corresponding method on the "delegate"
 * provided in my constructor.
 * <p>
 * Extends AbandonedTrace to implement Statement tracking and
 * logging of code which created the Statement. Tracking the
 * Statement ensures that the Connection which created it can
 * close any open Statement's on Connection close.
 *
 * @author Rodney Waldhoff (<a href="mailto:rwaldhof@us.britannica.com">rwaldhof@us.britannica.com</a>)
 * @author Glenn L. Nielsen
 * @author James House (<a href="mailto:james@interobjective.com">james@interobjective.com</a>)
 */
public class DelegatingStatement extends AbandonedTrace implements Statement {
    /** My delegate. */
    protected Statement _stmt = null;
    /** The connection that created me. **/
    protected DelegatingConnection _conn = null;

    /**
     * Create a wrapper for the Statement which traces this
     * Statement to the Connection which created it and the
     * code which created it.
     *
     * @param s the {@link Statement} to delegate all calls to.
     * @param c the {@link DelegatingConnection} that created this statement.
     */
    public DelegatingStatement(DelegatingConnection c, Statement s) {
        super(c);
        _stmt = s;
        _conn = c;
    }

    /**
     * Returns my underlying {@link Statement}.
     * @return my underlying {@link Statement}.
     */
    public Statement getDelegate() {
        return _stmt;
    }

    /**
     * If my underlying {@link Statement} is not a
     * <tt>DelegatingStatement</tt>, returns it,
     * otherwise recursively invokes this method on
     * my delegate.
     * <p>
     * Hence this method will return the first
     * delegate that is not a <tt>DelegatingStatement</tt>
     * or <tt>null</tt> when no non-<tt>DelegatingStatement</tt>
     * delegate can be found by transversing this chain.
     * <p>
     * This method is useful when you may have nested
     * <tt>DelegatingStatement</tt>s, and you want to make
     * sure to obtain a "genuine" {@link Statement}.
     */
    public Statement getInnermostDelegate() {
        Statement s = _stmt;
        while(s != null && s instanceof DelegatingStatement) {
            s = ((DelegatingStatement)s).getDelegate();
            if(this == s) {
                return null;
            }
        }
        return s;
    }

    /** Sets my delegate. */
    public void setDelegate(Statement s) {
        _stmt = s;
    }

    /**
     * Close this DelegatingStatement, and close
     * any ResultSets that were not explicitly closed.
     */
    public void close() throws SQLException {
        passivate();
        _stmt.close();
    }

    public Connection getConnection() throws SQLException {
        checkOpen();
        return _conn; // return the delegating connection that created this
    }

    public ResultSet executeQuery(String sql) throws SQLException {
        checkOpen();

        return new DelegatingResultSet(this, _stmt.executeQuery(sql));
    }

    public ResultSet getResultSet() throws SQLException {
        checkOpen();

        return new DelegatingResultSet(this, _stmt.getResultSet());
    }

    public int executeUpdate(String sql) throws SQLException { checkOpen(); return _stmt.executeUpdate(sql);}
    public int getMaxFieldSize() throws SQLException { checkOpen(); return _stmt.getMaxFieldSize();}
    public void setMaxFieldSize(int max) throws SQLException { checkOpen(); _stmt.setMaxFieldSize(max);}
    public int getMaxRows() throws SQLException { checkOpen(); return _stmt.getMaxRows();}
    public void setMaxRows(int max) throws SQLException { checkOpen(); _stmt.setMaxRows(max);}
    public void setEscapeProcessing(boolean enable) throws SQLException { checkOpen(); _stmt.setEscapeProcessing(enable);}
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

    protected void checkOpen() throws SQLException {
        if(_closed) {
            throw new SQLException("Connection is closed.");
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
        if (_conn != null) {
            _conn.removeTrace(this);
           _conn = null;
        }

        // The JDBC spec requires that a statment close any open
        // ResultSet's when it is closed.
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

    protected boolean _closed = false;

    // ------------------- JDBC 3.0 -----------------------------------------
    // Will be uncommented by the build process on a JDBC 3.0 system

/* JDBC_3_ANT_KEY

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

JDBC_3_ANT_KEY */

}
