/*
 * $Source: /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons//dbcp/src/java/org/apache/commons/dbcp/BasicDataSourceFactory.java,v $
 * $Revision: 1.11 $
 * $Date: 2003/09/20 14:28:54 $
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

import java.io.ByteArrayInputStream;
import java.sql.Connection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;


/**
 * <p>JNDI object factory that creates an instance of
 * <code>BasicDataSource</code> that has been configured based on the
 * <code>RefAddr</code> values of the specified <code>Reference</code>,
 * which must match the names and data types of the
 * <code>BasicDataSource</code> bean properties.</p>
 *
 * @author Craig R. McClanahan
 * @author Dirk Verbeeck
 * @version $Revision: 1.11 $ $Date: 2003/09/20 14:28:54 $
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
                (Boolean.valueOf(ra.getContent().toString()).booleanValue());
        }

        ra = ref.get("defaultReadOnly");
        if (ra != null) {
            dataSource.setDefaultReadOnly
                (Boolean.valueOf(ra.getContent().toString()).booleanValue());
        }

        ra = ref.get("defaultTransactionIsolation");
        if (ra != null) {
            String value = ra.getContent().toString();
            int level = PoolableConnectionFactory.UNKNOWN_TRANSACTIONISOLATION;
            if ("NONE".equalsIgnoreCase(value)) {
                level = Connection.TRANSACTION_NONE;
            }
            else if ("READ_COMMITTED".equalsIgnoreCase(value)) {
                level = Connection.TRANSACTION_READ_COMMITTED;
            }
            else if ("READ_UNCOMMITTED".equalsIgnoreCase(value)) {
                level = Connection.TRANSACTION_READ_UNCOMMITTED;
            }
            else if ("REPEATABLE_READ".equalsIgnoreCase(value)) {
                level = Connection.TRANSACTION_REPEATABLE_READ;
            }
            else if ("SERIALIZABLE".equalsIgnoreCase(value)) {
                level = Connection.TRANSACTION_SERIALIZABLE;
            }
            else {
                try {
                    level = Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    System.err.println("Could not parse defaultTransactionIsolation: " + value);
                    System.err.println("WARNING: defaultTransactionIsolation not set");
                    System.err.println("using default value of database driver");
                    level = PoolableConnectionFactory.UNKNOWN_TRANSACTIONISOLATION;
                }
            }
            dataSource.setDefaultTransactionIsolation(level);
        }

        ra = ref.get("defaultCatalog");
        if (ra != null) {
            dataSource.setDefaultCatalog(ra.getContent().toString());
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

        ra = ref.get("minIdle");
        if (ra != null) {
            dataSource.setMinIdle
                (Integer.parseInt(ra.getContent().toString()));
        }

        ra = ref.get("maxWait");
        if (ra != null) {
            dataSource.setMaxWait
                (Long.parseLong(ra.getContent().toString()));
        }

        ra = ref.get("testOnBorrow");
        if (ra != null) {
            dataSource.setTestOnBorrow
                (Boolean.valueOf(ra.getContent().toString()).booleanValue());
        }

        ra = ref.get("testOnReturn");
        if (ra != null) {
            dataSource.setTestOnReturn
                (Boolean.valueOf(ra.getContent().toString()).booleanValue());
        }

        ra = ref.get("timeBetweenEvictionRunsMillis");
        if (ra != null) {
            dataSource.setTimeBetweenEvictionRunsMillis
                (Long.parseLong(ra.getContent().toString()));
        }

        ra = ref.get("numTestsPerEvictionRun");
        if (ra != null) {
            dataSource.setNumTestsPerEvictionRun
                (Integer.parseInt(ra.getContent().toString()));
        }

        ra = ref.get("minEvictableIdleTimeMillis");
        if (ra != null) {
            dataSource.setMinEvictableIdleTimeMillis
                (Long.parseLong(ra.getContent().toString()));
        }

        ra = ref.get("testWhileIdle");
        if (ra != null) {
            dataSource.setTestWhileIdle
                (Boolean.valueOf(ra.getContent().toString()).booleanValue());
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

        ra = ref.get("accessToUnderlyingConnectionAllowed");
        if (ra != null) {
            dataSource.setAccessToUnderlyingConnectionAllowed
                (Boolean.valueOf(ra.getContent().toString()).booleanValue());
        }

        ra = ref.get("removeAbandoned");
        if (ra != null) {
            dataSource.setRemoveAbandoned
                (Boolean.valueOf(ra.getContent().toString()).booleanValue());
        }

        ra = ref.get("removeAbandonedTimeout");
        if (ra != null) {     
            dataSource.setRemoveAbandonedTimeout
                (Integer.parseInt(ra.getContent().toString()));
        }

        ra = ref.get("logAbandoned");
        if (ra != null) {
            dataSource.setLogAbandoned
                (Boolean.valueOf(ra.getContent().toString()).booleanValue());
        }

        ra = ref.get("poolPreparedStatements");
        if (ra != null) {
            dataSource.setPoolPreparedStatements
                (Boolean.valueOf(ra.getContent().toString()).booleanValue());
        }

        ra = ref.get("maxOpenPreparedStatements");
        if (ra != null) {
            dataSource.setMaxOpenPreparedStatements
                (Integer.parseInt(ra.getContent().toString()));
        }

        ra = ref.get("connectionProperties");
        if (ra != null) {
          Properties p = getProperties(ra.getContent().toString());
          Enumeration e = p.propertyNames();
          while (e.hasMoreElements()) {
            String propertyName = (String) e.nextElement();
            dataSource.addConnectionProperty(propertyName, p.getProperty(propertyName));
          }
        }

        // Return the configured data source instance
        return (dataSource);

    }


    /**
     * <p>Parse properties from the string. Format of the string must be [propertyName=property;]*<p>
     * @param propText
     * @return Properties
     * @throws Exception
     */
    static private Properties getProperties(String propText) throws Exception {
      Properties p = new Properties();
      if (propText != null) {
        p.load(new ByteArrayInputStream(propText.replace(';', '\n').getBytes()));
      }
      return p;
    }

}
