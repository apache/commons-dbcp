/*
 * $Source: /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons//dbcp/src/java/org/apache/commons/dbcp/datasources/InstanceKeyObjectFactory.java,v $
 * $Revision: 1.6 $
 * $Date: 2003/10/13 05:06:00 $
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

package org.apache.commons.dbcp.datasources;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

import java.util.Hashtable;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;

/**
 * A JNDI ObjectFactory which creates <code>SharedPoolDataSource</code>s
 * or <code>PerUserPoolDataSource</code>s
 */
abstract class InstanceKeyObjectFactory
    implements ObjectFactory
{
    private static Map instanceMap = new HashMap();

    synchronized static String registerNewInstance(InstanceKeyDataSource ds) {
        int max = 0;
        Iterator i = instanceMap.keySet().iterator();
        while (i.hasNext()) {
            Object obj = i.next();
            if (obj instanceof String) 
            {
                max = Math.max(max, Integer.parseInt((String)obj));
            }
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
        throws IOException, ClassNotFoundException {
        // The spec says to return null if we can't create an instance 
        // of the reference
        Object obj = null;
        if (refObj instanceof Reference) {
            Reference ref = (Reference) refObj;
            if (isCorrectClass(ref.getClassName())) { 
                RefAddr ra = ref.get("instanceKey");
                if (ra != null && ra.getContent() != null) {
                    // object was bound to jndi via Referenceable api.
                    obj = instanceMap.get(ra.getContent());
                }
                else 
                {
                    // tomcat jndi creates a Reference out of server.xml 
                    // <ResourceParam> configuration and passes it to an
                    // instance of the factory given in server.xml.
                    String key = null;
                    if (name != null) 
                    {
                        key = name.toString();
                        obj = instanceMap.get(key); 
                    }                    
                    if (obj == null)
                    {
                        InstanceKeyDataSource ds = getNewInstance(ref);
                        setCommonProperties(ref, ds);
                        obj = ds;
                        if (key != null) 
                        {
                            instanceMap.put(key, ds);
                        }                        
                    }
                }
            }
        }
        return obj;
    }

    private void setCommonProperties(Reference ref, 
                                     InstanceKeyDataSource ikds) 
        throws IOException, ClassNotFoundException {
                    
        RefAddr ra = ref.get("dataSourceName");
        if (ra != null && ra.getContent() != null) {
            ikds.setDataSourceName(ra.getContent().toString());
        }
                    
        ra = ref.get("defaultAutoCommit");
        if (ra != null && ra.getContent() != null) {
            ikds.setDefaultAutoCommit(Boolean.valueOf(
                ra.getContent().toString()).booleanValue());
        }
        
        ra = ref.get("defaultReadOnly");
        if (ra != null && ra.getContent() != null) {
            ikds.setDefaultReadOnly(Boolean.valueOf(
                ra.getContent().toString()).booleanValue());
        }
        
        ra = ref.get("description");
        if (ra != null && ra.getContent() != null) {
            ikds.setDescription(ra.getContent().toString());
        }
        
        ra = ref.get("jndiEnvironment");
        if (ra != null  && ra.getContent() != null) {
            byte[] serialized = (byte[]) ra.getContent();
            ikds.jndiEnvironment = 
                (Properties) deserialize(serialized);
        }
        
        ra = ref.get("loginTimeout");
        if (ra != null && ra.getContent() != null) {
            ikds.setLoginTimeout(
                Integer.parseInt(ra.getContent().toString()));
        }
        
        ra = ref.get("testOnBorrow");
        if (ra != null && ra.getContent() != null) {
            ikds.setTestOnBorrow(
                Boolean.getBoolean(ra.getContent().toString()));
        }
        
        ra = ref.get("testOnReturn");
        if (ra != null && ra.getContent() != null) {
            ikds.setTestOnReturn(Boolean.valueOf(
                ra.getContent().toString()).booleanValue());
        }
        
        ra = ref.get("timeBetweenEvictionRunsMillis");
        if (ra != null && ra.getContent() != null) {
            ikds.setTimeBetweenEvictionRunsMillis(
                Integer.parseInt(ra.getContent().toString()));
        }
        
        ra = ref.get("numTestsPerEvictionRun");
        if (ra != null && ra.getContent() != null) {
            ikds.setNumTestsPerEvictionRun(
                Integer.parseInt(ra.getContent().toString()));
        }
        
        ra = ref.get("minEvictableIdleTimeMillis");
        if (ra != null && ra.getContent() != null) {
            ikds.setMinEvictableIdleTimeMillis(
                Integer.parseInt(ra.getContent().toString()));
        }
        
        ra = ref.get("testWhileIdle");
        if (ra != null && ra.getContent() != null) {
            ikds.setTestWhileIdle(Boolean.valueOf(
                ra.getContent().toString()).booleanValue());
        }
                
        ra = ref.get("validationQuery");
        if (ra != null && ra.getContent() != null) {
            ikds.setValidationQuery(ra.getContent().toString());
        }
    }


    /**
     * @return true if and only if className is the value returned
     * from getClass().getName().toString()
     */
    protected abstract boolean isCorrectClass(String className);

    /**
     * Creates an instance of the subclass and sets any properties
     * contained in the Reference.
     */
    protected abstract InstanceKeyDataSource getNewInstance(Reference ref)
        throws IOException, ClassNotFoundException;

    /**
     * used to set some properties saved within a Reference
     */
    protected static final Object deserialize(byte[] data) 
        throws IOException, ClassNotFoundException {
        ObjectInputStream in = null;
        try {
            in = new ObjectInputStream(new ByteArrayInputStream(data));
            return in.readObject();
        } finally {
            try { 
                in.close(); 
            } catch (IOException ex) {
            }
        }
    }
}

