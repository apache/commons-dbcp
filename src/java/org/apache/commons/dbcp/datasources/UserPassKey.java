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

package org.apache.commons.dbcp.datasources;

import java.io.Serializable;

/**
 * Holds a username, password pair.
 * @version $Revision: 1.5 $ $Date: 2004/02/28 12:18:17 $
 */
class UserPassKey implements Serializable {
    private String password;
    private String username;
    
    UserPassKey(String username, String password) {
        this.username = username;
        this.password = password;
    }
        
    /**
     * Get the value of password.
     * @return value of password.
     */
    public String getPassword() {
        return password;
    }
    
    /**
     * Get the value of username.
     * @return value of username.
     */
    public String getUsername() {
        return username;
    }
    
    /**
     * @return <code>true</code> if the username and password fields for both 
     * objects are equal.
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj == this) {
            return true;
        }
        
        if (!(obj instanceof UserPassKey)) {
            return false;
        }
        
        UserPassKey key = (UserPassKey) obj;
        
        boolean usersEqual =
            (this.username == null
                ? key.username == null
                : this.username.equals(key.username));
                
        boolean passwordsEqual =
            (this.password == null
                ? key.password == null
                : this.password.equals(key.password));

        return (usersEqual && passwordsEqual);
    }

    public int hashCode() {
        return (this.username != null ? this.username.hashCode() : 0);
    }

    public String toString() {
        StringBuffer sb = new StringBuffer(50);
        sb.append("UserPassKey(");
        sb.append(username).append(", ").append(password).append(')');
        return sb.toString();
    }
}
