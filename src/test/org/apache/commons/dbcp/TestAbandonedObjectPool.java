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

package org.apache.commons.dbcp;

import java.util.Vector;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.pool.PoolableObjectFactory;

/**
 * TestCase for AbandonedObjectPool
 * 
 * @author Wayne Woodfield
 * @version $Revision$ $Date$
 */
public class TestAbandonedObjectPool extends TestCase {
    private AbandonedObjectPool pool = null;
    private AbandonedConfig config = null;
    
    public TestAbandonedObjectPool(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(TestAbandonedObjectPool.class);
    }

    public void setUp() throws Exception {
        super.setUp();
        config = new AbandonedConfig();
        
        // -- Uncomment the following line to enable logging -- 
        // config.setLogAbandoned(true);
        
        config.setRemoveAbandoned(true);
        config.setRemoveAbandonedTimeout(1);
        pool = new AbandonedObjectPool(new SimpleFactory(), config);
    }

    public void tearDown() throws Exception {
        super.tearDown();
        pool.close();
        pool = null;
    }

    /**
    * Tests fix for Bug 28579, a bug in AbandonedObjectPool that causes numActive to go negative
    * in GenericObjectPool
    */
    public void testConcurrentInvalidation() throws Exception {
        final int POOL_SIZE = 30;
        pool.setMaxActive(POOL_SIZE);
        pool.setMaxIdle(POOL_SIZE);
        pool.setWhenExhaustedAction(AbandonedObjectPool.WHEN_EXHAUSTED_FAIL);

        // Exhaust the connection pool
        Vector vec = new Vector();
        for (int i = 0; i < POOL_SIZE; i++) {
            vec.add(pool.borrowObject());
        }
        
        // Abandon all borrowed objects
        for (int i = 0; i < vec.size(); i++) {
            ((PooledTestObject)vec.elementAt(i)).setAbandoned(true);
        }

        // Try launching a bunch of borrows concurrently.  Abandoned sweep will be triggered for each.
        final int CONCURRENT_BORROWS = 5;
        Thread[] threads = new Thread[CONCURRENT_BORROWS];
        for (int i = 0; i < CONCURRENT_BORROWS; i++) {
            threads[i] = new ConcurrentBorrower(vec);
            threads[i].start();
        }

        // Wait for all the threads to finish
        for (int i = 0; i < CONCURRENT_BORROWS; i++) {
            threads[i].join();
        }
        
        // Return all objects that have not been destroyed
        for (int i = 0; i < vec.size(); i++) {
            PooledTestObject pto = (PooledTestObject)vec.elementAt(i);
            if (pto.isActive()) {
                pool.returnObject(pto);
            }
        }
        
        // Now, the number of open connections should be 0
        assertTrue("numActive should have been 0, was " + pool.getNumActive(), pool.getNumActive() == 0);
    }
    
    class ConcurrentBorrower extends Thread {
        private Vector _borrowed;
        
        public ConcurrentBorrower(Vector borrowed) {
            _borrowed = borrowed;
        }
        
        public void run() {
            try {
                _borrowed.add(pool.borrowObject());
            } catch (Exception e) {
                // expected in most cases
            }
        }
    }
    
    class SimpleFactory implements PoolableObjectFactory {

        public Object makeObject() {
            return new PooledTestObject(config);
        }
        
        public boolean validateObject(Object obj) { return true; }
        
        public void activateObject(Object obj) {
            ((PooledTestObject)obj).setActive(true);
        }
        
        public void passivateObject(Object obj) {
            ((PooledTestObject)obj).setActive(false);
        }

        public void destroyObject(Object obj) {
            ((PooledTestObject)obj).setActive(false);
            // while destroying connections, yield control to other threads
            // helps simulate threading errors
            Thread.yield();
        }
    }
}

class PooledTestObject extends AbandonedTrace {
    private boolean active = false;
    private int _hash = 0;
    private boolean _abandoned = false;

    private static int hash = 1;
    
    public PooledTestObject(AbandonedConfig config) {
        super(config);
        _hash = hash++;
    }
    
    public synchronized void setActive(boolean b) {
        active = b;
    }

    public synchronized boolean isActive() {
        return active;
    }
    
    public int hashCode() {
        return _hash;
    }
    
    public void setAbandoned(boolean b) {
        _abandoned = b;
    }
    
    protected long getLastUsed() {
        if (_abandoned) {
            // Abandoned object sweep will occur no matter what the value of removeAbandonedTimeout,
            // because this indicates that this object was last used decades ago
            return 1;
        } else {
            // Abandoned object sweep won't clean up this object
            return 0;
        }
    }
    
    public boolean equals(Object obj) {
        if (!(obj instanceof PooledTestObject)) return false;
        return obj.hashCode() == hashCode();
    }
}

