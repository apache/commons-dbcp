/* 
 * $Header: /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons//dbcp/src/java/org/apache/commons/dbcp/jdbc2pool/Attic/UserPassKey.java,v 1.5 2003/06/29 12:42:16 mpoeschl Exp $
 * $Revision: 1.5 $
 * $Date: 2003/06/29 12:42:16 $
 * 
 * ====================================================================
 * 
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2003 The Apache Software Foundation.  All rights
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
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache" and "Apache Software Foundation" and 
 *    "Apache Turbine" must not be used to endorse or promote products 
 *    derived from this software without prior written permission. For 
 *    written permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    "Apache Turbine", nor may "Apache" appear in their name, without 
 *    prior written permission of the Apache Software Foundation.
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
 */

package org.apache.commons.dbcp.jdbc2pool;

import java.io.Serializable;

/**
 * Holds a username, password pair.
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
