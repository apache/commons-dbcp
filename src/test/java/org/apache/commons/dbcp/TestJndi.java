/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.dbcp;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.dbcp.datasources.SharedPoolDataSource;
import org.apache.commons.dbcp.datasources.PerUserPoolDataSource;

/**
 * Tests JNID bind and lookup for DataSource implementations.
 * Demonstrates problem indicated in BZ #38073.
 *
 */

public class TestJndi extends TestCase {
    /**
     * The subcontext where the data source is bound.
     */
    protected static final String JNDI_SUBCONTEXT = "jdbc";

    /**
     * the full jndi path to the data source.
     */
    protected static final String JNDI_PATH = JNDI_SUBCONTEXT + "/"
            + "jndiTestDataSource";
    
    /** jndi context to use in tests **/
    protected Context context = null;

    /**
     * Creates a new instance.
     */
    public TestJndi(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(TestJndi.class);
    }

    /**
     * Test BasicDatsource bind and lookup
     * 
     * @throws Exception
     */
    public void testBasicDataSourceBind() throws Exception {
        BasicDataSource dataSource = new BasicDataSource();
        checkBind(dataSource);      
    }
    
    /**
     * Test SharedPoolDataSource bind and lookup
     * 
     * @throws Exception
     */
    public void testSharedPoolDataSourceBind() throws Exception {
        SharedPoolDataSource dataSource = new SharedPoolDataSource();
        checkBind(dataSource);      
    }
    
    /**
     * Test PerUserPoolDataSource bind and lookup
     * 
     * @throws Exception
     */
    public void testPerUserPoolDataSourceBind() throws Exception {
        PerUserPoolDataSource dataSource = new PerUserPoolDataSource();
        checkBind(dataSource);      
    }
    
    public void setUp() throws Exception {
        context = getInitialContext();
        context.createSubcontext(JNDI_SUBCONTEXT);  
    }
    
    public void tearDown() throws Exception {
        context.unbind(JNDI_PATH);
        context.destroySubcontext(JNDI_SUBCONTEXT);    
    }
    
    /**
     * Binds a DataSource to the jndi and checks that we have successfully 
     * bound it by looking it up again.
     * 
     * @throws Exception if the bind, lookup or connect fails
     */
    protected void checkBind(DataSource dataSource) throws Exception {
        bindDataSource(dataSource);
        retrieveDataSource();
    }

    /**
     * Binds a DataSource into jndi.
     * 
     * @throws Exception if creation or binding fails.
     */
    protected void bindDataSource(DataSource dataSource) throws Exception {
        context.bind(JNDI_PATH, dataSource);
    }

    /**
     * Retrieves a DataSource from jndi.
     * 
     * @throws Exception if the jndi lookup fails or no DataSource is bound.
     */
    protected DataSource retrieveDataSource() throws Exception {
        Context ctx = getInitialContext();
        DataSource dataSource = (DataSource) ctx.lookup(JNDI_PATH);

        if (dataSource == null) {
            fail("DataSource should not be null");
        }
        return dataSource;
    }

    /**
     * Retrieves (or creates if it does not exist) an InitialContext.
     * 
     * @return the InitialContext.
     * @throws NamingException if the InitialContext cannot be retrieved 
     *         or created.
     */
    protected InitialContext getInitialContext() throws NamingException {
        Hashtable environment = new Hashtable();
        environment.put(Context.INITIAL_CONTEXT_FACTORY,
                org.apache.naming.java.javaURLContextFactory.class.getName());
        InitialContext ctx = new InitialContext(environment);
        return ctx;
    }
}
