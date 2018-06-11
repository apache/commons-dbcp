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

package org.apache.commons.dbcp2.datasources;

import static org.junit.Assert.assertNotNull;

import java.util.Hashtable;

import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.naming.spi.ObjectFactory;

import org.junit.Test;

/**
 */
public class TestFactory {

    // Bugzilla Bug 24082: bug in InstanceKeyDataSourceFactory
    // There's a fatal bug in InstanceKeyDataSourceFactory that means you can't
    // instantiate more than one factory.
    // http://issues.apache.org/bugzilla/show_bug.cgi?id=24082
    @Test
    public void testJNDI2Pools() throws Exception {
        final Reference refObj = new Reference(SharedPoolDataSource.class.getName());
        refObj.add(new StringRefAddr("dataSourceName","java:comp/env/jdbc/bookstoreCPDS"));
        final Context context = new InitialContext();
        final Hashtable<?, ?> env = new Hashtable<>();

        final ObjectFactory factory = new SharedPoolDataSourceFactory();

        final Name name = new CompositeName("myDB");
        final Object obj = factory.getObjectInstance(refObj, name, context, env);
        assertNotNull(obj);

        final Name name2 = new CompositeName("myDB2");
        final Object obj2 = factory.getObjectInstance(refObj, name2, context, env);
        assertNotNull(obj2);
    }
}
