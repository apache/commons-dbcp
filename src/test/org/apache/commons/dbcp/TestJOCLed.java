/*
 * $Header: /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons//dbcp/src/test/org/apache/commons/dbcp/TestJOCLed.java,v 1.1 2001/04/14 17:16:22 rwaldhoff Exp $
 * $Revision: 1.1 $
 * $Date: 2001/04/14 17:16:22 $
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

// this suite requires a JOCL configuration
// file named testpool.jocl to be in your classpath
// see the doc directory for an example

// note that depending upon the configuration of the testpool,
// testThreaded1 might fail

/**
 * @author Rodney Waldhoff
 * @version $Id: TestJOCLed.java,v 1.1 2001/04/14 17:16:22 rwaldhoff Exp $
 */
public class TestJOCLed extends TestCase {
    public TestJOCLed(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(TestJOCLed.class);
    }

    public static void main(String args[]) {
        String[] testCaseName = { TestJOCLed.class.getName() };
        junit.textui.TestRunner.main(testCaseName);
    }

    public void setUp() throws Exception {
        Class.forName("org.apache.commons.dbcp.PoolingDriver");
    }

    public void testSimple() throws Exception {
        Connection conn = DriverManager.getConnection("jdbc:apache:commons:dbcp:/testpool");
        assert(null != conn);
        PreparedStatement stmt = conn.prepareStatement("select * from dual");
        assert(null != stmt);
        ResultSet rset = stmt.executeQuery();
        assert(null != rset);
        assert(rset.next());
        rset.close();
        stmt.close();
        conn.close();
    }

    public void testSimple2() throws Exception {
        Connection conn = DriverManager.getConnection("jdbc:apache:commons:dbcp:/testpool");
        assert(null != conn);
        {
            PreparedStatement stmt = conn.prepareStatement("select * from dual");
            assert(null != stmt);
            ResultSet rset = stmt.executeQuery();
            assert(null != rset);
            assert(rset.next());
            rset.close();
            stmt.close();
        }
        {
            PreparedStatement stmt = conn.prepareStatement("select * from dual");
            assert(null != stmt);
            ResultSet rset = stmt.executeQuery();
            assert(null != rset);
            assert(rset.next());
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

        conn = DriverManager.getConnection("jdbc:apache:commons:dbcp:/testpool");
        assert(null != conn);
        {
            PreparedStatement stmt = conn.prepareStatement("select * from dual");
            assert(null != stmt);
            ResultSet rset = stmt.executeQuery();
            assert(null != rset);
            assert(rset.next());
            rset.close();
            stmt.close();
        }
        {
            PreparedStatement stmt = conn.prepareStatement("select * from dual");
            assert(null != stmt);
            ResultSet rset = stmt.executeQuery();
            assert(null != rset);
            assert(rset.next());
            rset.close();
            stmt.close();
        }
        conn.close();
        conn = null;
    }

    public void testPooling() throws Exception {
        Connection conn = DriverManager.getConnection("jdbc:apache:commons:dbcp:/testpool");
        assert(conn != null);
        Connection conn2 = DriverManager.getConnection("jdbc:apache:commons:dbcp:/testpool");
        assert(conn2 != null);
        assert(conn != conn2);
        conn2.close();
        conn.close();
        conn2 = DriverManager.getConnection("jdbc:apache:commons:dbcp:/testpool");
        assertSame(conn,conn2);
    }

    public void testThreaded1() {
        TestThread[] threads = new TestThread[20];
        for(int i=0;i<20;i++) {
            threads[i] = new TestThread(100,50);
            Thread t = new Thread(threads[i]);
            t.start();
        }
        for(int i=0;i<20;i++) {
            while(!(threads[i]).complete()) {
                try {
                    Thread.currentThread().sleep(500L);
                } catch(Exception e) {
                    // ignored
                }
            }
            if(threads[i].failed()) {
                fail();
            }
        }
    }

    class TestThread implements Runnable {
        java.util.Random _random = new java.util.Random();
        boolean _complete = false;
        boolean _failed = false;
        int _iter = 100;
        int _delay = 50;

        public TestThread() {
        }

        public TestThread(int iter) {
            _iter = iter;
        }

        public TestThread(int iter, int delay) {
            _iter = iter;
            _delay = delay;
        }

        public boolean complete() {
            return _complete;
        }

        public boolean failed() {
            return _failed;
        }

        public void run() {
            for(int i=0;i<_iter;i++) {
                try {
                    Thread.currentThread().sleep((long)_random.nextInt(_delay));
                } catch(Exception e) {
                    // ignored
                }
                Connection conn = null;
                PreparedStatement stmt = null;
                ResultSet rset = null;
                try {
                    conn = DriverManager.getConnection("jdbc:apache:commons:dbcp:/testpool");
                    stmt = conn.prepareStatement("select 'literal', SYSDATE from dual");
                    rset = stmt.executeQuery();
                    try {
                        Thread.currentThread().sleep((long)_random.nextInt(_delay));
                    } catch(Exception e) {
                        // ignored
                    }
                } catch(Exception e) {
                    e.printStackTrace();
                    _failed = true;
                    _complete = true;
                    break;
                } finally {
                    try { rset.close(); } catch(Exception e) { }
                    try { stmt.close(); } catch(Exception e) { }
                    try { conn.close(); } catch(Exception e) { }
                }
            }
            _complete = true;
        }
    }

}
