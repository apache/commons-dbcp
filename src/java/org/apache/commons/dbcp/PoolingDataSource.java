/*
 * $Header: /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons//dbcp/src/java/org/apache/commons/dbcp/PoolingDataSource.java,v 1.2 2002/03/17 14:55:20 rwaldhoff Exp $
 * $Revision: 1.2 $
 * $Date: 2002/03/17 14:55:20 $
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

import org.apache.commons.pool.ObjectPool;
import java.util.Properties;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.io.PrintWriter;
import javax.sql.DataSource;
import java.util.Enumeration;

/**
 * A simple {@link DataSource} implementation that obtains
 * {@link Connection}s from the specified {@link ObjectPool}.
 *
 * @author Rodney Waldhoff
 * @version $Id: PoolingDataSource.java,v 1.2 2002/03/17 14:55:20 rwaldhoff Exp $
 */
public class PoolingDataSource implements DataSource {
    public PoolingDataSource() {
        this(null);
    }

    public PoolingDataSource(ObjectPool pool) {
        _pool = pool;
    }

    public void setPool(ObjectPool pool) throws IllegalStateException, NullPointerException {
        if(null != _pool) {
            throw new IllegalStateException("Pool already set");
        } else if(null == pool) {
            throw new NullPointerException("Pool must not be null.");
        } else {
            _pool = pool;
        }
    }
    //--- DataSource methods -----------------------------------------

    /**
     * Return a {@link java.sql.Connection} from my pool,
     * according to the contract specified by {@link ObjectPool#borrowObject}.
     */
    public synchronized Connection getConnection() throws SQLException {
        try {
            return (Connection)(_pool.borrowObject());
        } catch(SQLException e) {
            throw e;
        } catch(RuntimeException e) {
            throw e;
        } catch(Exception e) {
            throw new SQLException(e.toString());
        }
    }

    /**
     * Throws {@link UnsupportedOperationException}
     * @throws UnsupportedOperationException
     */
    public Connection getConnection(String uname, String passwd) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns my log writer.
     * @return my log writer
     * @see DataSource#getLogWriter
     */
    public PrintWriter getLogWriter() {
        return _logWriter;
    }

    /**
     * Throws {@link UnsupportedOperationException}.
     * Do this configuration within my {@link ObjectPool}.
     * @throws UnsupportedOperationException
     */
    public int getLoginTimeout() {
        throw new UnsupportedOperationException();
    }

    /**
     * Throws {@link UnsupportedOperationException}.
     * Do this configuration within my {@link ObjectPool}.
     * @throws UnsupportedOperationException
     */
    public void setLoginTimeout(int seconds) {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets my log writer.
     * @see DataSource#setLogWriter
     */
    public void setLogWriter(PrintWriter out) {
        _logWriter = out;
    }

    /** My log writer. */
    protected PrintWriter _logWriter = null;

    protected ObjectPool _pool = null;

}
