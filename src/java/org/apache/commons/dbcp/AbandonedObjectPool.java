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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;

/**
 * <p>An implementation of a Jakarta-Commons ObjectPool which
 * tracks JDBC connections and can recover abandoned db connections.
 * If logAbandoned=true, a stack trace will be printed for any
 * abandoned db connections recovered.
 *                                                                        
 * @author Glenn L. Nielsen
 * @version $Revision: 1.16 $ $Date: 2004/05/01 12:42:19 $
 * @deprecated This will be removed in a future version of DBCP.
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
        System.out.println("AbandonedObjectPool is used (" + this + ")");
        System.out.println("   LogAbandoned: " + config.getLogAbandoned());
        System.out.println("   RemoveAbandoned: " + config.getRemoveAbandoned());
        System.out.println("   RemoveAbandonedTimeout: " + config.getRemoveAbandonedTimeout());
    }

    /**
     * Get a db connection from the pool.
     *
     * If removeAbandoned=true, recovers db connections which
     * have been idle > removeAbandonedTimeout.
     * 
     * @return Object jdbc Connection
     */
    public Object borrowObject() throws Exception {
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
            synchronized(trace) {
                trace.add(obj);
            }
        }
        return obj;
    }

    /**
     * Return a db connection to the pool.
     *
     * @param Object db Connection to return
     */
    public void returnObject(Object obj) throws Exception {
        if (config != null && config.getRemoveAbandoned()) {
            synchronized(trace) {
                boolean foundObject = trace.remove(obj);
                if (!foundObject) {
                    return; // This connection has already been invalidated.  Stop now.
                }
            }
        }
        super.returnObject(obj);
    }

    public void invalidateObject(Object obj) throws Exception {
        if (config != null && config.getRemoveAbandoned()) {
            synchronized(trace) {
                boolean foundObject = trace.remove(obj);
                if (!foundObject) {
                    return; // This connection has already been invalidated.  Stop now.
                }
            }
        }
        super.invalidateObject(obj);        
    }

    /**
     * Recover abandoned db connections which have been idle
     * greater than the removeAbandonedTimeout.
     */
    private void removeAbandoned() {
        // Generate a list of abandoned connections to remove
        long now = System.currentTimeMillis();
        long timeout = now - (config.getRemoveAbandonedTimeout() * 1000);
        ArrayList remove = new ArrayList();
        synchronized(trace) {
            Iterator it = trace.iterator();
            while (it.hasNext()) {
                AbandonedTrace pc = (AbandonedTrace)it.next();
                if (pc.getLastUsed() > timeout) {
                    continue;
                }
                if (pc.getLastUsed() > 0) {
                    remove.add(pc);
                }
            }
        }

        // Now remove the abandoned connections
        Iterator it = remove.iterator();
        while (it.hasNext()) {
            AbandonedTrace pc = (AbandonedTrace)it.next();
            if (config.getLogAbandoned()) {
                pc.printStackTrace();
            }             
            try {
                invalidateObject(pc);
            } catch(Exception e) {
                e.printStackTrace();
            }             
        }
    }
}
