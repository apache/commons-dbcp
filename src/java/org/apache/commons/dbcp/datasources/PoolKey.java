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

package org.apache.commons.dbcp.datasources;

import java.io.Serializable;

/**
 * @version $Revision$ $Date$
 */
class PoolKey implements Serializable {
    private static final long serialVersionUID = 2252771047542484533L;

    private final String datasourceName;
    private final String userPwd;
    
    PoolKey(String datasourceName, String userPwd) {
        this.datasourceName = datasourceName;
        this.userPwd = userPwd;
    }
    
    public boolean equals(Object obj) {
        if (obj instanceof PoolKey) {
            PoolKey pk = (PoolKey)obj;
            return (null == datasourceName ? null == pk.datasourceName : datasourceName.equals(pk.datasourceName)) &&
                (null == userPwd ? null == pk.userPwd : userPwd.equals(pk.userPwd));
        } else {
            return false;   
        }
    }

    public int hashCode() {
        int h = 0;
        if (datasourceName != null) {
            h += datasourceName.hashCode();
        }
        if (userPwd != null) {
            h = 29 * h + userPwd.hashCode();
        }
        return h;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer(50);
        sb.append("PoolKey(");
        sb.append(userPwd).append(", ").append(datasourceName);
        sb.append(')');
        return sb.toString();
    }
}
