/*
 * $Id: TestJdbc2PoolDataSource.java,v 1.3 2002/11/08 19:37:26 rwaldhoff Exp $
 * $Revision: 1.3 $
 * $Date: 2002/11/08 19:37:26 $
 *
 * ====================================================================
 *
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2002 The Apache Software Foundation.  All rights
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

package org.apache.commons.dbcp.jdbc2pool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.sql.DataSource;
import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.commons.dbcp.TestConnectionPool;
import org.apache.commons.dbcp.cpdsadapter.DriverAdapterCPDS;
import org.apache.commons.lang.exception.NestableRuntimeException;

/**
 * @author John McNally
 * @version $Revision: 1.3 $ $Date: 2002/11/08 19:37:26 $
 */
public class TestJdbc2PoolDataSource extends TestConnectionPool {
    public TestJdbc2PoolDataSource(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(TestJdbc2PoolDataSource.class);
    }

    protected Connection getConnection() throws Exception {
        return ds.getConnection("foo","bar");
    }

    private DataSource ds;

    public void setUp() throws Exception {
        DriverAdapterCPDS pcds = new DriverAdapterCPDS();
        pcds.setDriver("org.apache.commons.dbcp.TesterDriver");
        pcds.setUrl("jdbc:apache:commons:testdriver");
        pcds.setUser("foo");
        pcds.setPassword("bar");

        Jdbc2PoolDataSource tds = new Jdbc2PoolDataSource();
        tds.setConnectionPoolDataSource(pcds);
        tds.setDefaultMaxActive(getMaxActive());
        tds.setDefaultMaxWait((int)(getMaxWait()));
        tds.setPerUserMaxActive("foo",new Integer(getMaxActive()));
        tds.setPerUserMaxWait("foo",new Integer((int)(getMaxWait())));

        ds = tds;
    }
    
    public void testMultipleThreads() throws Exception {
        assertTrue(multipleThreads(1));
        assertTrue(!multipleThreads(2 * (int)(getMaxWait())));
    }

    private boolean multipleThreads(int holdTime) throws Exception {
        long startTime = System.currentTimeMillis();
        final boolean[] success = new boolean[1];
        success[0] = true;
        final PoolTest[] pts = new PoolTest[2 * getMaxActive()];
        ThreadGroup threadGroup = new ThreadGroup("foo") {
            public void uncaughtException(Thread t, Throwable e) {
                /*
                for (int i = 0; i < pts.length; i++)
                {
                    System.out.println(i + ": " + pts[i].reportState());
                }
                */
                for (int i = 0; i < pts.length; i++) {
                    pts[i].stop();
                }

                //e.printStackTrace();
                success[0] = false;
            }
        };

        for (int i = 0; i < pts.length; i++) {
            pts[i] = new PoolTest(threadGroup, holdTime);
        }
        Thread.currentThread().sleep(10 * holdTime);
        for (int i = 0; i < pts.length; i++) {
            pts[i].stop();
        }
        long time = System.currentTimeMillis() - startTime;
        // - (pts.length*10*holdTime);
        System.out.println("Multithread test time = " + time + " ms");

        Thread.currentThread().sleep(holdTime);
        return success[0];
    }

    private static int currentThreadCount = 0;

    private class PoolTest implements Runnable {
        /**
         * The number of milliseconds to hold onto a database connection
         */
        private int connHoldTime;

        private boolean isRun;

        private String state;

        protected PoolTest(ThreadGroup threadGroup, int connHoldTime) {
            this.connHoldTime = connHoldTime;
            Thread thread =
                new Thread(threadGroup, this, "Thread+" + currentThreadCount++);
            thread.setDaemon(false);
            thread.start();
        }

        public void run() {
            Thread thread = Thread.currentThread();
            isRun = true;
            while (isRun) {
                try {
                    Connection conn = null;
                    state = "Getting Connection";
                    conn = getConnection();
                    state = "Using Connection";
                    assertTrue(null != conn);
                    PreparedStatement stmt =
                        conn.prepareStatement("select * from dual");
                    assertTrue(null != stmt);
                    ResultSet rset = stmt.executeQuery();
                    assertTrue(null != rset);
                    assertTrue(rset.next());
                    state = "Holding Connection";
                    thread.sleep(connHoldTime);
                    state = "Returning Connection";
                    rset.close();
                    stmt.close();
                    conn.close();
                } catch (Exception e) {
                    throw new NestableRuntimeException(e);
                }
            }
        }

        public void stop() {
            isRun = false;
        }

        public String reportState() {
            return state;
        }
    }
    
}
