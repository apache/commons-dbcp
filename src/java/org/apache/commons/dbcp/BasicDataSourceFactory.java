/*
 * $Header: /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons//dbcp/src/java/org/apache/commons/dbcp/BasicDataSourceFactory.java,v 1.1 2002/01/16 00:19:36 craigmcc Exp $
 * $Revision: 1.1 $
 * $Date: 2002/01/16 00:19:36 $
 *
 * ====================================================================
 *
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999-2002 The Apache Software Foundation.  All rights
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

import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.RefAddr;
import javax.naming.spi.ObjectFactory;


/**
 * <p>JNDI object factory that creates an instance of
 * <code>BasicDataSource</code> that has been configured based on the
 * <code>RefAddr</code> values of the specified <code>Reference</code>,
 * which must match the names and data types of the
 * <code>BasicDataSource</code> bean properties.</p>
 *
 * @author Craig R. McClanahan
 * @version $Revision: 1.1 $ $Date: 2002/01/16 00:19:36 $
 */

public class BasicDataSourceFactory implements ObjectFactory {


    // -------------------------------------------------- ObjectFactory Methods


    /**
     * <p>Create and return a new <code>BasicDataSource</code> instance.  If no
     * instance can be created, return <code>null</code> instead.</p>
     *
     * @param obj The possibly null object containing location or
     *  reference information that can be used in creating an object
     * @param name The name of this object relative to <code>nameCtx</code>
     * @param nameCts The context relative to which the <code>name</code>
     *  parameter is specified, or <code>null</code> if <code>name</code>
     *  is relative to the default initial context
     * @param environment The possibly null environment that is used in
     *  creating this object
     *
     * @exception Exception if an exception occurs creating the instance
     */
    public Object getObjectInstance(Object obj, Name name, Context nameCtx,
                                    Hashtable environment)
        throws Exception {

        // We only know how to deal with <code>javax.naming.Reference</code>s
        // that specify a class name of "javax.sql.DataSource"
        if ((obj == null) || !(obj instanceof Reference)) {
            return (null);
        }
        Reference ref = (Reference) obj;
        if (!"javax.sql.DataSource".equals(ref.getClassName())) {
            return (null);
        }

        // Create and configure a BasicDataSource instance based on the
        // RefAddr values associated with this Reference
        BasicDataSource dataSource = new BasicDataSource();
        RefAddr ra = null;

        ra = ref.get("defaultAutoCommit");
        if (ra != null) {
            dataSource.setDefaultAutoCommit
                (Boolean.getBoolean(ra.getContent().toString()));
        }

        ra = ref.get("defaultReadOnly");
        if (ra != null) {
            dataSource.setDefaultReadOnly
                (Boolean.getBoolean(ra.getContent().toString()));
        }

        ra = ref.get("driverClassName");
        if (ra != null) {
            dataSource.setDriverClassName(ra.getContent().toString());
        }

        ra = ref.get("maxActive");
        if (ra != null) {
            dataSource.setMaxActive
                (Integer.parseInt(ra.getContent().toString()));
        }

        ra = ref.get("maxIdle");
        if (ra != null) {
            dataSource.setMaxIdle
                (Integer.parseInt(ra.getContent().toString()));
        }

        ra = ref.get("maxWait");
        if (ra != null) {
            dataSource.setMaxWait
                (Long.parseLong(ra.getContent().toString()));
        }

        ra = ref.get("password");
        if (ra != null) {
            dataSource.setPassword(ra.getContent().toString());
        }

        ra = ref.get("url");
        if (ra != null) {
            dataSource.setUrl(ra.getContent().toString());
        }

        ra = ref.get("username");
        if (ra != null) {
            dataSource.setUsername(ra.getContent().toString());
        }

        ra = ref.get("validationQuery");
        if (ra != null) {
            dataSource.setValidationQuery(ra.getContent().toString());
        }

        // Return the configured data source instance
        return (dataSource);

    }




}
