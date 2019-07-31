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

package org.apache.commons.dbcp2;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.commons.dbcp2.datasources.SharedPoolDataSource;
import org.apache.commons.dbcp2.datasources.PerUserPoolDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests JNID bind and lookup for DataSource implementations.
 * Demonstrates problem indicated in BZ #38073.
 */
public class TestJndi {
    /**
     * The subcontext where the data source is bound.
     */
    protected static final String JNDI_SUBCONTEXT = "jdbc";

    /**
     * the full JNDI path to the data source.
     */
    protected static final String JNDI_PATH = JNDI_SUBCONTEXT + "/"
            + "jndiTestDataSource";

    /** JNDI context to use in tests **/
    protected Context context = null;

    /**
     * Test BasicDatasource bind and lookup
     *
     * @throws Exception
     */
    @Test
    public void testBasicDataSourceBind() throws Exception {
        final BasicDataSource dataSource = new BasicDataSource();
        checkBind(dataSource);
    }

    /**
     * Test SharedPoolDataSource bind and lookup
     *
     * @throws Exception
     */
    @Test
    public void testSharedPoolDataSourceBind() throws Exception {
        final SharedPoolDataSource dataSource = new SharedPoolDataSource();
        checkBind(dataSource);
    }

    /**
     * Test PerUserPoolDataSource bind and lookup
     *
     * @throws Exception
     */
    @Test
    public void testPerUserPoolDataSourceBind() throws Exception {
        final PerUserPoolDataSource dataSource = new PerUserPoolDataSource();
        checkBind(dataSource);
    }

    @BeforeEach
    public void setUp() throws Exception {
        context = getInitialContext();
        context.createSubcontext(JNDI_SUBCONTEXT);
    }

    @AfterEach
    public void tearDown() throws Exception {
        context.unbind(JNDI_PATH);
        context.destroySubcontext(JNDI_SUBCONTEXT);
    }

    /**
     * Binds a DataSource to the JNDI and checks that we have successfully
     * bound it by looking it up again.
     *
     * @throws Exception if the bind, lookup or connect fails
     */
    protected void checkBind(final DataSource dataSource) throws Exception {
        bindDataSource(dataSource);
        retrieveDataSource();
    }

    /**
     * Binds a DataSource into JNDI.
     *
     * @throws Exception if creation or binding fails.
     */
    protected void bindDataSource(final DataSource dataSource) throws Exception {
        context.bind(JNDI_PATH, dataSource);
    }

    /**
     * Retrieves a DataSource from JNDI.
     *
     * @throws Exception if the JNDI lookup fails or no DataSource is bound.
     */
    protected DataSource retrieveDataSource() throws Exception {
        final Context ctx = getInitialContext();
        final DataSource dataSource = (DataSource) ctx.lookup(JNDI_PATH);

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
        final Hashtable<String, String> environment = new Hashtable<>();
        environment.put(Context.INITIAL_CONTEXT_FACTORY,
                org.apache.naming.java.javaURLContextFactory.class.getName());
        final InitialContext ctx = new InitialContext(environment);
        return ctx;
    }
}
