/*
 * $Header: /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons//dbcp/src/test/org/apache/commons/dbcp/TestManual.java,v 1.10 2002/11/01 15:42:33 rwaldhoff Exp $
 * $Revision: 1.10 $
 * $Date: 2002/11/01 15:42:33 $
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

import junit.framework.*;
import java.sql.*;
import org.apache.commons.pool.*;
import org.apache.commons.pool.impl.*;

/**
 * 
 * @author Rodney Waldhoff
 * @author Sean C. Sullivan
 * 
 * @version $Id: TestManual.java,v 1.10 2002/11/01 15:42:33 rwaldhoff Exp $
 */
public class TestManual extends TestCase {
    public TestManual(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(TestManual.class);
    }

    public static void main(String args[]) {
        String[] testCaseName = { TestManual.class.getName() };
        junit.textui.TestRunner.main(testCaseName);
    }

    public void setUp() throws Exception {
        GenericObjectPool pool = new GenericObjectPool(null, 10, GenericObjectPool.WHEN_EXHAUSTED_BLOCK, 2000L, 10, true, true, 10000L, 5, 5000L, true);
        DriverConnectionFactory cf = new DriverConnectionFactory(new TesterDriver(),"jdbc:apache:commons:testdriver",null);
        GenericKeyedObjectPoolFactory opf = new GenericKeyedObjectPoolFactory(null, 10, GenericKeyedObjectPool.WHEN_EXHAUSTED_BLOCK, 2000L, 10, true, true, 10000L, 5, 5000L, true);
        PoolableConnectionFactory pcf = new PoolableConnectionFactory(cf, pool, opf, "SELECT COUNT(*) FROM DUAL", false, true);
        PoolingDriver driver = new PoolingDriver();
        driver.registerPool("test",pool);
        DriverManager.registerDriver(driver);
    }

    public void testIsClosed() throws Exception {
        for(int i=0;i<10;i++) {
            Connection conn = DriverManager.getConnection("jdbc:apache:commons:dbcp:test");
            assertTrue(null != conn);
            assertTrue(!conn.isClosed());
            PreparedStatement stmt = conn.prepareStatement("select * from dual");
            assertTrue(null != stmt);
            ResultSet rset = stmt.executeQuery();
            assertTrue(null != rset);
            assertTrue(rset.next());
            rset.close();
            stmt.close();
            conn.close();
            assertTrue(conn.isClosed());
        }
    }

    public void testCantCloseConnectionTwice() throws Exception {
        for(int i=0;i<2;i++) { // loop to show we *can* close again once we've borrowed it from the pool again
            Connection conn = DriverManager.getConnection("jdbc:apache:commons:dbcp:test");
            assertTrue(null != conn);
            assertTrue(!conn.isClosed());
            conn.close();
            assertTrue(conn.isClosed());
            try {
                conn.close();
                fail("Expected SQLException on second attempt to close");
            } catch(SQLException e) {
                // expected
            }
            assertTrue(conn.isClosed());
        }
    }

    public void testCantCloseStatementTwice() throws Exception {
        Connection conn = DriverManager.getConnection("jdbc:apache:commons:dbcp:test");
        assertTrue(null != conn);
        assertTrue(!conn.isClosed());
        for(int i=0;i<2;i++) { // loop to show we *can* close again once we've borrowed it from the pool again
            PreparedStatement stmt = conn.prepareStatement("select * from dual");
            assertTrue(null != stmt);
            stmt.close();
            try {
                stmt.close();
                fail("Expected SQLException on second attempt to close");
            } catch(SQLException e) {
                // expected
            }
        }
        conn.close();
    }

    public void testSimple() throws Exception {
        Connection conn = DriverManager.getConnection("jdbc:apache:commons:dbcp:test");
        assertTrue(null != conn);
        PreparedStatement stmt = conn.prepareStatement("select * from dual");
        assertTrue(null != stmt);
        ResultSet rset = stmt.executeQuery();
        assertTrue(null != rset);
        assertTrue(rset.next());
        rset.close();
        stmt.close();
        conn.close();
    }

    public void testSimple2() throws Exception {
        Connection conn = DriverManager.getConnection("jdbc:apache:commons:dbcp:test");
        assertTrue(null != conn);
        {
            PreparedStatement stmt = conn.prepareStatement("select * from dual");
            assertTrue(null != stmt);
            ResultSet rset = stmt.executeQuery();
            assertTrue(null != rset);
            assertTrue(rset.next());
            rset.close();
            stmt.close();
        }
        {
            PreparedStatement stmt = conn.prepareStatement("select * from dual");
            assertTrue(null != stmt);
            ResultSet rset = stmt.executeQuery();
            assertTrue(null != rset);
            assertTrue(rset.next());
            rset.close();
            stmt.close();
        }
        conn.close();
        try {
            conn.createStatement();
            fail("Can't use closed connections");
        } catch(SQLException e) {
            ; // expected
        }

        conn = DriverManager.getConnection("jdbc:apache:commons:dbcp:test");
        assertTrue(null != conn);
        {
            PreparedStatement stmt = conn.prepareStatement("select * from dual");
            assertTrue(null != stmt);
            ResultSet rset = stmt.executeQuery();
            assertTrue(null != rset);
            assertTrue(rset.next());
            rset.close();
            stmt.close();
        }
        {
            PreparedStatement stmt = conn.prepareStatement("select * from dual");
            assertTrue(null != stmt);
            ResultSet rset = stmt.executeQuery();
            assertTrue(null != rset);
            assertTrue(rset.next());
            rset.close();
            stmt.close();
        }
        conn.close();
        conn = null;
    }

    public void testPooling() throws Exception {
        Connection conn = DriverManager.getConnection("jdbc:apache:commons:dbcp:test");
        assertTrue(conn != null);
        Connection conn2 = DriverManager.getConnection("jdbc:apache:commons:dbcp:test");
        assertTrue(conn2 != null);
        assertTrue(conn != conn2);
        conn2.close();
        conn.close();
        Connection conn3 = DriverManager.getConnection("jdbc:apache:commons:dbcp:test");
        assertTrue( conn3 == conn || conn3 == conn2 );
    }
    
    public void testAutoCommitBehavior() throws Exception {
    	
    	final String strDriverUrl = "jdbc:apache:commons:dbcp:test";
        Connection conn = DriverManager.getConnection(strDriverUrl);
        assertTrue(conn != null);
        assertTrue(conn.getAutoCommit());
        conn.setAutoCommit(false);
        conn.close();
        
        Connection conn2 = DriverManager.getConnection(strDriverUrl);
        assertTrue( conn2.getAutoCommit() );
        
        Connection conn3 = DriverManager.getConnection(strDriverUrl);
        assertTrue( conn3.getAutoCommit() );

		conn2.close();
		
		conn3.close();
    }
    
}
