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

/**
 * Configuration settings for handling abandoned db connections.
 *                                                            
 * @author Glenn L. Nielsen           
 * @version $Revision: 1.5 $ $Date: 2004/02/28 11:48:04 $
 * @deprecated This will be removed in a future version of DBCP.
 */
public class AbandonedConfig {

    private boolean removeAbandoned = false;

    /**
     * Flag to remove abandoned connections if they exceed the
     * removeAbandonedTimeout.
     *
     * Set to true or false, default false.
     * If set to true a connection is considered abandoned and eligible
     * for removal if it has been idle longer than the removeAbandonedTimeout.
     * Setting this to true can recover db connections from poorly written    
     * applications which fail to close a connection.
     *
     * @return boolean
     */
    public boolean getRemoveAbandoned() {
        return (this.removeAbandoned);
    }

    /**
     * Flag to remove abandoned connections if they exceed the
     * removeAbandonedTimeout.
     *
     * Set to true or false, default false.
     * If set to true a connection is considered abandoned and eligible   
     * for removal if it has been idle longer than the removeAbandonedTimeout.
     * Setting this to true can recover db connections from poorly written
     * applications which fail to close a connection.
     *
     * @param boolean
     */
    public void setRemoveAbandoned(boolean removeAbandoned) {
        this.removeAbandoned = removeAbandoned;
    }

    private int removeAbandonedTimeout = 300;

    /**
     * Timeout in seconds before an abandoned connection can be removed.
     *
     * Defaults to 300 seconds.
     *
     * @return int remove abandoned timeout in seconds
     */
    public int getRemoveAbandonedTimeout() {
        return (this.removeAbandonedTimeout);
    }

    /**
     * Timeout in seconds before an abandoned connection can be removed.
     *
     * Defaults to 300 seconds.
     *
     * @param int remove abandoned timeout in seconds
     */
    public void setRemoveAbandonedTimeout(int removeAbandonedTimeout) {
        this.removeAbandonedTimeout = removeAbandonedTimeout;
    }

    private boolean logAbandoned = false;

    /**
     * Flag to log stack traces for application code which abandoned
     * a Statement or Connection.
     *
     * Defaults to false.
     * Logging of abandoned Statements and Connections adds overhead
     * for every Connection open or new Statement because a stack
     * trace has to be generated.
     *
     * @return boolean
     */
    public boolean getLogAbandoned() {
        return (this.logAbandoned);
    }

    /**
     * Flag to log stack traces for application code which abandoned
     * a Statement or Connection.
     *
     * Defaults to false.
     * Logging of abandoned Statements and Connections adds overhead
     * for every Connection open or new Statement because a stack
     * trace has to be generated.
     *
     * @param boolean
     */
    public void setLogAbandoned(boolean logAbandoned) {
        this.logAbandoned = logAbandoned;
    }

}
