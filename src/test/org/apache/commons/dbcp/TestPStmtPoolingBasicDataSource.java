/*
 * $Source: /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons//dbcp/src/test/org/apache/commons/dbcp/TestPStmtPoolingBasicDataSource.java,v $
 * $Revision: 1.2 $
 * $Date: 2003/12/22 14:41:17 $
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
import java.sql.SQLException;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * TestSuite for BasicDataSource with prepared statement pooling enabled
 * 
 * @author Dirk Verbeeck
 * @version $Revision: 1.2 $ $Date: 2003/12/22 14:41:17 $
 */
public class TestPStmtPoolingBasicDataSource extends TestBasicDataSource {
    public TestPStmtPoolingBasicDataSource(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(TestPStmtPoolingBasicDataSource.class);
    }

    public void setUp() throws Exception {
        super.setUp();

        // PoolPreparedStatements enabled, should not affect the basic tests
        ds.setPoolPreparedStatements(true);
        ds.setMaxOpenPreparedStatements(2);
    }

    public void tearDown() throws Exception {
        super.tearDown();
        // nothing to do here
    }

    public void testPreparedStatementPooling() throws Exception {
        Connection conn = getConnection();
        assertNotNull(conn);
        
        PreparedStatement stmt1 = conn.prepareStatement("select 'a' from dual");
        assertNotNull(stmt1);
        
        PreparedStatement stmt2 = conn.prepareStatement("select 'b' from dual");
        assertNotNull(stmt2);
        
        assertTrue(stmt1 != stmt2);
        
        // go over the maxOpen limit
        PreparedStatement stmt3 = null;
        try {
            stmt3 = conn.prepareStatement("select 'c' from dual");
            fail("expected SQLException");
        } 
        catch (SQLException e) {}
        
        // make idle
        stmt2.close();

        // test cleanup the 'b' statement
        stmt3 = conn.prepareStatement("select 'c' from dual");
        assertNotNull(stmt3);
        assertTrue(stmt3 != stmt1);
        assertTrue(stmt3 != stmt2);
        
        // normal reuse of statement
        stmt1.close();
        PreparedStatement stmt4 = conn.prepareStatement("select 'a' from dual");
        assertNotNull(stmt4);
    }
}
