/*
 * $Header: /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons//dbcp/src/java/org/apache/commons/dbcp/AbandonedTrace.java,v 1.1 2002/05/16 21:25:37 glenn Exp $
 * $Revision: 1.1 $
 * $Date: 2002/05/16 21:25:37 $ 
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

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections.FastArrayList;

/**
 * Tracks db connection usage for recovering and reporting
 * abandoned db connections.
 *
 * The JDBC Connection, Statement, and ResultSet classes
 * extend this class.
 * 
 * @author Glenn L. Nielsen
 * @version $Revision: 1.1 $ $Date: 2002/05/16 21:25:37 $
 */
public class AbandonedTrace {

    private static String MESSAGE =
        "DBCP object was created, but never closed by the following code: ";

    // DBCP AbandonedConfig
    private AbandonedConfig config = null;
    // Parent object
    private AbandonedTrace parent;
    // A stack trace of the code that created me (if in debug mode) **/
    private Exception createdBy;
    // A list of objects created by children of this object
    private List trace = new FastArrayList();
    // Last time this connection was used
    private long lastUsed = 0;

    /**
     * Create a new AbandonedTrace without config and
     * without doing abandoned tracing.
     */
    public AbandonedTrace() {
        init(parent);
    }

    /**
     * Construct a new AbandonedTrace with no parent object.
     *
     * @param AbandonedConfig
     */
    public AbandonedTrace(AbandonedConfig config) {
        this.config = config;
        init(parent);
    }

    /**
     * Construct a new AbandonedTrace with a parent object.
     *
     * @param AbandonedTrace parent object
     */
    public AbandonedTrace(AbandonedTrace parent) {
        this.config = parent.getConfig();
        init(parent);
    }

    /**
     * Initialize abandoned tracing for this object.
     *
     * @param AbandonedTrace parent object
     */
    private void init(AbandonedTrace parent) {
        if (parent != null) {                  
            parent.addTrace(this);
        }
        ((FastArrayList)trace).setFast(true);

        if (config == null) {
            return;
        }
        if (config.getLogAbandoned()) {
            createdBy = new Exception(MESSAGE);
        }
    }

    /**
     * Get the abandoned config for this object.
     *
     * @return AbandonedConfig for this object
     */
    protected AbandonedConfig getConfig() {
        return config;
    }

    /**
     * Get the last time this object was used in ms.
     *
     * @return long time in ms
     */
    protected long getLastUsed() {
        if (parent != null) {     
           return parent.getLastUsed();  
        }
        return lastUsed;
    }

    /**
     * Set the time this object was last used to the
     * current time in ms.
     */
    protected void setLastUsed() {
        if (parent != null) {
           parent.setLastUsed();
        } else {
           lastUsed = new Date().getTime();
        }
    }

    /**
     * Set the time in ms this object was last used.
     *
     * @param long time in ms
     */
    protected void setLastUsed(long time) {
        if (parent != null) {
           parent.setLastUsed(time);
        } else {   
           lastUsed = time;
        }
    }

    /**
     * If logAbandoned=true generate a stack trace
     * for this object then add this object to the parent
     * object trace list.
     */
    protected void setStackTrace() {
        if (config == null) {                 
            return;                           
        }                    
        if (config.getLogAbandoned()) {
            createdBy = new Exception(MESSAGE);
        }                                      
        if (parent != null) {                  
            parent.addTrace(this);
        }
    }

    /**
     * Add an object to the list of objects being
     * traced.
     *
     * @param AbandonedTrace object to add
     */
    protected void addTrace(AbandonedTrace trace) {
        this.trace.add(trace);
        setLastUsed();
    }

    /**
     * Clear the list of objects being traced by this
     * object.
     */
    protected void clearTrace() {
        if (trace != null) {
            trace.clear();
        }
    }

    /**
     * Get a list of objects being traced by this object.
     *
     * @return List of objects
     */
    protected List getTrace() {
        return trace;
    }

    /**
     * If logAbandoned=true, print a stack trace of the code that
     * created this object.
     */
    public void printStackTrace() {
        if (createdBy != null) {
            createdBy.printStackTrace();
        }
        Iterator it = trace.iterator();
        while (it.hasNext()) {
            AbandonedTrace at = (AbandonedTrace)it.next();
            at.printStackTrace();
        }
    }

    /**
     * Remove a child object this object is tracing.
     *
     * @param AbandonedTrace object to remvoe
     */
    protected void removeTrace(AbandonedTrace trace) {
        if (this.trace != null) {
            this.trace.remove(trace);
        }
    }

}

