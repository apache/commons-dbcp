/*
 * $Source: /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons//dbcp/src/test/org/apache/commons/dbcp/TestManual.java,v $
 * $Revision: 1.14 $
 * $Date: 2003/08/22 16:08:32 $
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
 * http://www.apache.org/
 *
 */

package org.apache.commons.dbcp;

import java.sql.Connection;
import java.sql.DriverManager;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.apache.commons.pool.impl.GenericKeyedObjectPoolFactory;
import org.apache.commons.pool.impl.GenericObjectPool;

/**
 * Tests for a "manually configured", {@link GenericObjectPool}
 * based {@link PoolingDriver}.
 * @author Rodney Waldhoff
 * @author Sean C. Sullivan
 * @version $Revision: 1.14 $ $Date: 2003/08/22 16:08:32 $
 */
public class TestManual extends TestConnectionPool {
    public TestManual(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(TestManual.class);
    }

    protected Connection getConnection() throws Exception {
        return DriverManager.getConnection("jdbc:apache:commons:dbcp:test");
    }

    private PoolingDriver driver = null;
    
    public void setUp() throws Exception {
        super.setUp();
        GenericObjectPool pool = new GenericObjectPool(null, getMaxActive(), GenericObjectPool.WHEN_EXHAUSTED_BLOCK, getMaxWait(), 10, true, true, 10000L, 5, 5000L, true);
        DriverConnectionFactory cf = new DriverConnectionFactory(new TesterDriver(),"jdbc:apache:commons:testdriver",null);
        GenericKeyedObjectPoolFactory opf = new GenericKeyedObjectPoolFactory(null, 10, GenericKeyedObjectPool.WHEN_EXHAUSTED_BLOCK, 2000L, 10, true, true, 10000L, 5, 5000L, true);
        PoolableConnectionFactory pcf = new PoolableConnectionFactory(cf, pool, opf, "SELECT COUNT(*) FROM DUAL", false, true);
        assertNotNull(pcf);
        driver = new PoolingDriver();
        driver.registerPool("test",pool);
        DriverManager.registerDriver(driver);
    }

    public void tearDown() throws Exception {
        DriverManager.deregisterDriver(driver);
    }

    /** @see http://issues.apache.org/bugzilla/show_bug.cgi?id=12400 */
    public void testReportedBug12400() throws Exception {
        ObjectPool connectionPool = new GenericObjectPool(
            null,
            70,
            GenericObjectPool.WHEN_EXHAUSTED_BLOCK,
            60000,
            10);
        ConnectionFactory connectionFactory = new DriverManagerConnectionFactory(
            "jdbc:apache:commons:testdriver", 
            "username", 
            "password");
        PoolableConnectionFactory poolableConnectionFactory = 
            new PoolableConnectionFactory(
                connectionFactory,
                connectionPool,
                null,
                null,
                false,
                true);
        assertNotNull(poolableConnectionFactory);
        PoolingDriver driver = new PoolingDriver();
        driver.registerPool("neusoftim",connectionPool);
        Connection[] conn = new Connection[25];
        for(int i=0;i<25;i++) {
            conn[i] = DriverManager.getConnection("jdbc:apache:commons:dbcp:neusoftim");
            for(int j=0;j<i;j++) {
                assertTrue(conn[j] != conn[i]);
                assertTrue(!conn[j].equals(conn[i]));
            }
        }
        for(int i=0;i<25;i++) {
            conn[i].close();
        }
    }
    
}
