/*
 * $Header: /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons//dbcp/src/java/org/apache/commons/dbcp/AbandonedObjectPool.java,v 1.4 2003/03/06 19:25:34 rwaldhoff Exp $
 * $Revision: 1.4 $
 * $Date: 2003/03/06 19:25:34 $ 
 *
 * ====================================================================
 *
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999-2001 The Apache Software Foundation.  All rights
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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;

/**
 * <p>An implementation of a Jakarta-Commons ObjectPool which
 * tracks JDBC connections and can recover abandoned db connections.
 * If logAbandoned=true, a stack trace will be printed for any
 * abandoned db connections recovered.
 *                                                                        
 * @author Glenn L. Nielsen
 * @version $Revision: 1.4 $ $Date: 2003/03/06 19:25:34 $
 */
public class AbandonedObjectPool extends GenericObjectPool {

    // DBCP AbandonedConfig
    private AbandonedConfig config = null;
    // A list of connections in use
    private List trace = new ArrayList();

    /**
     * Create an ObjectPool which tracks db connections.
     *
     * @param PoolableObjectFactory factory used to create this
     * @param AbandonedConfig configuration for abandoned db connections
     */
    public AbandonedObjectPool(PoolableObjectFactory factory,
                               AbandonedConfig config) {
        super(factory);
        this.config = config;
    }

    /**
     * Get a db connection from the pool.
     *
     * If removeAbandoned=true, recovers db connections which
     * have been idle > removeAbandonedTimeout.
     * 
     * @return Object jdbc Connection
     */
    public synchronized Object borrowObject() throws Exception {
        try {
            if (config != null
                    && config.getRemoveAbandoned()
                    && (getNumIdle() < 2)
                    && (getNumActive() > getMaxActive() - 3) ) {
                removeAbandoned();
            }
            Object obj = super.borrowObject();
            if(obj instanceof AbandonedTrace) {
                ((AbandonedTrace)obj).setStackTrace();
            }
            if (obj != null && config != null && config.getRemoveAbandoned()) {
                trace.add(obj);
            }
            return obj;
        } catch(NoSuchElementException ne) {
            throw new SQLException(
                "DBCP could not obtain an idle db connection, pool exhausted");
        } catch(Exception e) {
            System.out.println("DBCP borrowObject failed: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Return a db connection to the pool.
     *
     * @param Object db Connection to return
     */
    public synchronized void returnObject(Object obj) throws Exception {
        if (config != null && config.getRemoveAbandoned()) {
            trace.remove(obj);
        }
        super.returnObject(obj);
    }

    public synchronized void invalidateObject(Object obj) throws Exception {
        if (config != null && config.getRemoveAbandoned()) {
            trace.remove(obj);
        }
        super.invalidateObject(obj);        
    }
    
    /**
     * Recover abandoned db connections which have been idle
     * greater than the removeAbandonedTimeout.
     */
    private synchronized void removeAbandoned() {
        // Generate a list of abandoned connections to remove
        long now = new Date().getTime();
        long timeout = now - (config.getRemoveAbandonedTimeout() * 1000);
        ArrayList remove = new ArrayList();
        Iterator it = trace.iterator();
        while (it.hasNext()) {
            PoolableConnection pc = (PoolableConnection)it.next();
            if (pc.getLastUsed() > timeout) {
                continue;
            }
            if (pc.getLastUsed() > 0) {
                remove.add(pc);
            }
        }
        // Now remove the abandoned connections
        it = remove.iterator();
        while (it.hasNext()) {
            PoolableConnection pc = (PoolableConnection)it.next();
            if (config.getLogAbandoned()) {
                pc.printStackTrace();
            }             
            try {
                pc.close();
            } catch(SQLException e) {
                e.printStackTrace();
            }             
        }
    }

}
