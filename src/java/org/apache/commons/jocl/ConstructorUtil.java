/*
 * $Source: /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons//dbcp/src/java/org/apache/commons/jocl/ConstructorUtil.java,v $
 * $Revision: 1.4 $
 * $Date: 2003/10/09 21:05:29 $
 *
 * ====================================================================
 *
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999-2003 The Apache Software Foundation.  All rights
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
 *    any, must include the following acknowledgement:
 *       "This product includes software developed by the
 *        Apache Software Foundation - http://www.apache.org/"
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowledgements normally appear.
 *
 * 4. The names "The Jakarta Project", "Commons", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Software Foundation.
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
 * http://www.apache.org/
 *
 */

package org.apache.commons.jocl;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Miscellaneous {@link Constructor} related utility functions.
 *
 * @version $Id: ConstructorUtil.java,v 1.4 2003/10/09 21:05:29 rdonkin Exp $
 * @author Rodney Waldhoff
 */
public class ConstructorUtil {
    /**
     * Returns a {@link Constructor} for the given method signature, or <tt>null</tt>
     * if no such <tt>Constructor</tt> can be found.
     *
     * @param type     the (non-<tt>null</tt>) type of {@link Object} the returned {@link Constructor} should create
     * @param argTypes a non-<tt>null</tt> array of types describing the parameters to the {@link Constructor}.
     * @return a {@link Constructor} for the given method signature, or <tt>null</tt>
     *         if no such <tt>Constructor</tt> can be found.
     * @see #invokeConstructor
     */
    public static Constructor getConstructor(Class type, Class[] argTypes) {
        if(null == type || null == argTypes) {
            throw new NullPointerException();
        }
        Constructor ctor = null;
        try {
            ctor = type.getConstructor(argTypes);
        } catch(Exception e) {
            ctor = null;
        }
        if(null == ctor) {
            // no directly declared matching constructor,
            // look for something that will work
            // XXX this should really be more careful to
            //     adhere to the jls mechanism for late binding
            Constructor[] ctors = type.getConstructors();
            for(int i=0;i<ctors.length;i++) {
                Class[] paramtypes = ctors[i].getParameterTypes();
                if(paramtypes.length == argTypes.length) {
                    boolean canuse = true;
                    for(int j=0;j<paramtypes.length;j++) {
                        if(paramtypes[j].isAssignableFrom(argTypes[j])) {
                            continue;
                        } else {
                            canuse = false;
                            break;
                        }
                    }
                    if(canuse == true) {
                        ctor = ctors[i];
                        break;
                    }
                }
            }
        }
        return ctor;
    }

    /**
     * Creates a new instance of the specified <tt><i>type</i></tt>
     * using a {@link Constructor} described by the given parameter types
     * and values.
     *
     * @param type      the type of {@link Object} to be created
     * @param argTypes  a non-<tt>null</tt> array of types describing the parameters to the {@link Constructor}.
     * @param argValues a non-<tt>null</tt> array containing the values of the parameters to the {@link Constructor}.
     * @return a new instance of the specified <tt><i>type</i></tt>
     *         using a {@link Constructor} described by the given parameter types
     *         and values.
     * @exception InstantiationException
     * @exception IllegalAccessException
     * @exception InvocationTargetException
     */
    public static Object invokeConstructor(Class type, Class[] argTypes, Object[] argValues) throws InstantiationException, IllegalAccessException, InvocationTargetException {
        return ConstructorUtil.getConstructor(type,argTypes).newInstance(argValues);
    }
}


