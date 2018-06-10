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

package org.apache.commons.dbcp2.datasources;

import java.io.Serializable;

/**
 * <p>Holds a user name, password pair.  Serves as a poolable object key for the KeyedObjectPool
 * backing a SharedPoolDataSource.  Two instances with the same user name are considered equal.
 * This ensures that there will be only one keyed pool for each user in the pool.  The password
 * is used (along with the user name) by the KeyedCPDSConnectionFactory when creating new connections.</p>
 *
 * <p>{@link InstanceKeyDataSource#getConnection(String, String)} validates that the password used to create
 * a connection matches the password provided by the client.</p>
 *
 * @since 2.0
 */
class UserPassKey implements Serializable {
    private static final long serialVersionUID = 5142970911626584817L;
    private final String password;
    private final String userName;

    UserPassKey(final String userName, final String password) {
        this.userName = userName;
        this.password = password;
    }

    /**
     * Gets the value of password.
     * 
     * @return value of password.
     */
    public String getPassword() {
        return password;
    }

    /**
     * Gets the value of user name.
     * 
     * @return value of user name.
     */
    public String getUsername() {
        return userName;
    }

    /**
     * @return <code>true</code> if the user name fields for both
     * objects are equal.  Two instances with the same user name
     * but different passwords are considered equal.
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj == this) {
            return true;
        }

        if (!(obj instanceof UserPassKey)) {
            return false;
        }

        final UserPassKey key = (UserPassKey) obj;

        return this.userName == null ?
                key.userName == null :
                this.userName.equals(key.userName);
    }

    /**
     * Returns the hash of the user name.
     */
    @Override
    public int hashCode() {
        return this.userName != null ?
                this.userName.hashCode() : 0;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer(50);
        sb.append("UserPassKey(");
        sb.append(userName).append(", ").append(password).append(')');
        return sb.toString();
    }
}
