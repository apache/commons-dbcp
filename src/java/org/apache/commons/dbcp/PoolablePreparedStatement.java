/*
 * $Source: /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons//dbcp/src/java/org/apache/commons/dbcp/PoolablePreparedStatement.java,v $
 * $Revision: 1.8 $
 * $Date: 2003/12/26 15:43:55 $
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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.pool.KeyedObjectPool;

/**
 * A {@link DelegatingPreparedStatement} that cooperates with
 * {@link PoolingConnection} to implement a pool of {@link PreparedStatement}s.
 * <p>
 * My {@link #close} method returns me to my containing pool. (See {@link PoolingConnection}.)
 *
 * @see PoolingConnection
 * @author Rodney Waldhoff
 * @author Glenn L. Nielsen
 * @author James House (<a href="mailto:james@interobjective.com">james@interobjective.com</a>)
 * @author Dirk Verbeeck
 * @version $Revision: 1.8 $ $Date: 2003/12/26 15:43:55 $
 */
public class PoolablePreparedStatement extends DelegatingPreparedStatement implements PreparedStatement {
    /**
     * The {@link KeyedObjectPool} from which I was obtained.
     */
    protected KeyedObjectPool _pool = null;

    /**
     * My "key" as used by {@link KeyedObjectPool}.
     */
    protected Object _key = null;

    /**
     * Constructor
     * @param stmt my underlying {@link PreparedStatement}
     * @param key my key" as used by {@link KeyedObjectPool}
     * @param pool the {@link KeyedObjectPool} from which I was obtained.
     * @param conn the {@link Connection} from which I was created
     */
    public PoolablePreparedStatement(PreparedStatement stmt, Object key, KeyedObjectPool pool, Connection conn) {
        super((DelegatingConnection) conn, stmt);
        _pool = pool;
        _key = key;
    }

    /**
     * Return me to my pool.
     */
    public void close() throws SQLException {
        if(isClosed()) {
            throw new SQLException("Already closed");
        } else {
            try {
                _pool.returnObject(_key,this);
            } catch(SQLException e) {
                throw e;
            } catch(RuntimeException e) {
                throw e;
            } catch(Exception e) {
                throw new SQLNestedException("Cannot close preparedstatement (return to pool failed)", e);
            }
        }
    }
    
    protected void activate() throws SQLException{
        _closed = false;
        if(_conn != null) {
            _conn.addTrace(this);
        }
        super.passivate();
    }
  
    protected void passivate() throws SQLException {
        _closed = true;
        if(_conn != null) {
            _conn.removeTrace(this);
        }
           
        // The JDBC spec requires that a statment close any open
        // ResultSet's when it is closed.
        // FIXME The PreparedStatement we're wrapping should handle this for us.
        // See bug 17301 for what could happen when ResultSets are closed twice.
        List resultSets = getTrace();
        if( resultSets != null) {
            ResultSet[] set = (ResultSet[]) resultSets.toArray(new ResultSet[resultSets.size()]);
            for (int i = 0; i < set.length; i++) {
                set[i].close();
            }
            clearTrace();
        }
        
        super.passivate();
    }

}
