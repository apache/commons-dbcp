/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2003 The Apache Software Foundation.  All rights
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
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache" and "Apache Software Foundation" and 
 *    "Apache Turbine" must not be used to endorse or promote products 
 *    derived from this software without prior written permission. For 
 *    written permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    "Apache Turbine", nor may "Apache" appear in their name, without 
 *    prior written permission of the Apache Software Foundation.
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
 */
 
package org.apache.commons.dbcp.datasources;

import java.util.Hashtable;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;

/**
 * A JNDI ObjectFactory which creates <code>SharedPoolDataSource</code>s
 */
public class InstanceKeyObjectFactory
    implements ObjectFactory
{
    private static final String SHARED_POOL_CLASSNAME =
        SharedPoolDataSource.class.getName();

    private static final String PER_USER_POOL_CLASSNAME =
        PerUserPoolDataSource.class.getName();

    private static Map instanceMap = new HashMap();

    synchronized static String registerNewInstance(InstanceKeyDataSource ds) {
        int max = 0;
        Iterator i = instanceMap.keySet().iterator();
        while (i.hasNext()) {
            int key = Integer.parseInt((String)i.next());
            max = Math.max(max, key);
        }
        String instanceKey = String.valueOf(max + 1);
        // put a placeholder here for now, so other instances will not
        // take our key.  we will replace with a pool when ready.
        instanceMap.put(instanceKey, ds);
        return instanceKey;
    }
    
    static void removeInstance(String key)
    {
        instanceMap.remove(key);
    }

    /**
     * Close all pools associated with this class.
     */
    public static void closeAll() throws Exception {
        //Get iterator to loop over all instances of this datasource.
        Iterator instanceIterator = instanceMap.entrySet().iterator();
        while (instanceIterator.hasNext()) {
            ((InstanceKeyDataSource) 
                ((Map.Entry) instanceIterator.next()).getValue()).close();
        }
        instanceMap.clear();
    }

    /**
     * implements ObjectFactory to create an instance of SharedPoolDataSource
     * or PerUserPoolDataSource
     */ 
    public Object getObjectInstance(Object refObj, Name name, 
                                    Context context, Hashtable env) 
        throws Exception {
        // The spec says to return null if we can't create an instance 
        // of the reference
        Object obj = null;
        if (refObj instanceof Reference) {
            Reference ref = (Reference) refObj;
            String classname = ref.getClassName();
            if (SHARED_POOL_CLASSNAME.equals(classname)
                || PER_USER_POOL_CLASSNAME.equals(classname)) {
                RefAddr ra = ref.get("instanceKey");
                if (ra != null && ra.getContent() != null) {
                    obj = instanceMap.get(ra.getContent());
                }
            }            
        }
        return obj;
    }
}
