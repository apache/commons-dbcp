/*
 * $Id: TestDelegatingConnection.java,v 1.1 2003/08/11 23:55:43 dirkv Exp $
 * $Revision: 1.1 $
 * $Date: 2003/08/11 23:55:43 $
 *
 * ====================================================================
 *
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
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

import java.sql.Connection;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @author Dirk Verbeeck
 * @version $Revision: 1.1 $ $Date: 2003/08/11 23:55:43 $
 */
public class TestDelegatingConnection extends TestCase {
    public TestDelegatingConnection(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(TestDelegatingConnection.class);
    }

    private DelegatingConnection conn = null;
    private Connection delegateConn = null;

    public void setUp() throws Exception {
        delegateConn = new TesterConnection();
        conn = new DelegatingConnection(delegateConn);
    }


    public void testGetDelegate() throws Exception {
        assertEquals(delegateConn,conn.getDelegate());
    }

    public void testHashCode() {
        DelegatingConnection conn = new DelegatingConnection(delegateConn);
        DelegatingConnection conn2 = new DelegatingConnection(delegateConn);
        assertEquals(conn.hashCode(), conn2.hashCode());
    }
    
    public void testEquals() {
        DelegatingConnection conn = new DelegatingConnection(delegateConn);
        DelegatingConnection conn2 = new DelegatingConnection(delegateConn);
        DelegatingConnection conn3 = new DelegatingConnection(null);
        
        assertTrue(!conn.equals(null));
        assertTrue(conn.equals(conn2));
        assertTrue(!conn.equals(conn3));
    }
}
