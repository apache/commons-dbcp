/*
 * Copyright 1999-2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * Tracks db connection usage for recovering and reporting
 * abandoned db connections.
 *
 * The JDBC Connection, Statement, and ResultSet classes
 * extend this class.
 * 
 * @author Glenn L. Nielsen
 * @version $Revision: 1.12 $ $Date: 2004/04/25 10:36:24 $
 * @deprecated This will be removed in a future version of DBCP.
 */
public class AbandonedTrace {

    private static SimpleDateFormat format = new SimpleDateFormat
        ("'DBCP object created' yyyy-MM-dd HH:mm:ss " +
         "'by the following code was never closed:'");

    // DBCP AbandonedConfig
    private AbandonedConfig config = null;
    // Parent object
    private AbandonedTrace parent;
    // A stack trace of the code that created me (if in debug mode) **/
    private Exception createdBy;
    private long createdTime;
    // A list of objects created by children of this object
    private List trace = new ArrayList();
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

        if (config == null) {
            return;
        }
        if (config.getLogAbandoned()) {
            createdBy = new Exception();
            createdTime = System.currentTimeMillis();
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
           lastUsed = System.currentTimeMillis();
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
            createdBy = new Exception();
            createdTime = System.currentTimeMillis();
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
        synchronized(this) {
            this.trace.add(trace);
        }
        setLastUsed();
    }

    /**
     * Clear the list of objects being traced by this
     * object.
     */
    protected synchronized void clearTrace() {
        if (this.trace != null) {
            this.trace.clear();
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
            System.out.println(format.format(new Date(createdTime)));
            createdBy.printStackTrace(System.out);
        }
        synchronized(this) {
            Iterator it = this.trace.iterator();
            while (it.hasNext()) {
                AbandonedTrace at = (AbandonedTrace)it.next();
                at.printStackTrace();
            }
        }
    }

    /**
     * Remove a child object this object is tracing.
     *
     * @param AbandonedTrace object to remvoe
     */
    protected synchronized void removeTrace(AbandonedTrace trace) {
        if (this.trace != null) {
            this.trace.remove(trace);
        }
    }

}
