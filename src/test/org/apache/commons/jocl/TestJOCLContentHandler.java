/*
 * $Header: /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons//dbcp/src/test/org/apache/commons/jocl/TestJOCLContentHandler.java,v 1.1 2001/04/14 17:16:18 rwaldhoff Exp $
 * $Revision: 1.1 $
 * $Date: 2001/04/14 17:16:18 $
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

package org.apache.commons.jocl;

import junit.framework.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

public class TestJOCLContentHandler extends TestCase {
    public TestJOCLContentHandler(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(TestJOCLContentHandler.class);
    }

    public static void main(String args[]) {
        String[] testCaseName = { TestJOCLContentHandler.class.getName() };
        junit.textui.TestRunner.main(testCaseName);
    }

    private JOCLContentHandler jocl = null;

    public void setUp() {
        jocl = new JOCLContentHandler();
    }

    public void testPrimatives() throws Exception {
        jocl.startDocument();
        jocl.startElement("http://apache.org/xml/xmlns/jakarta/commons/jocl","jocl","jocl",new AttributesImpl());
        {
            AttributesImpl attr = new AttributesImpl();
            attr.addAttribute("http://apache.org/xml/xmlns/jakarta/commons/jocl","value","value","CDATA","true");
            jocl.startElement("http://apache.org/xml/xmlns/jakarta/commons/jocl","boolean","boolean",attr);
            jocl.endElement("http://apache.org/xml/xmlns/jakarta/commons/jocl","boolean","boolean");
        }
        {
            AttributesImpl attr = new AttributesImpl();
            attr.addAttribute("http://apache.org/xml/xmlns/jakarta/commons/jocl","value","value","CDATA","1");
            jocl.startElement("http://apache.org/xml/xmlns/jakarta/commons/jocl","byte","byte",attr);
            jocl.endElement("http://apache.org/xml/xmlns/jakarta/commons/jocl","byte","byte");
        }
        {
            AttributesImpl attr = new AttributesImpl();
            attr.addAttribute("http://apache.org/xml/xmlns/jakarta/commons/jocl","value","value","CDATA","c");
            jocl.startElement("http://apache.org/xml/xmlns/jakarta/commons/jocl","char","char",attr);
            jocl.endElement("http://apache.org/xml/xmlns/jakarta/commons/jocl","char","char");
        }
        {
            AttributesImpl attr = new AttributesImpl();
            attr.addAttribute("http://apache.org/xml/xmlns/jakarta/commons/jocl","value","value","CDATA","2.0");
            jocl.startElement("http://apache.org/xml/xmlns/jakarta/commons/jocl","double","double",attr);
            jocl.endElement("http://apache.org/xml/xmlns/jakarta/commons/jocl","double","double");
        }
        {
            AttributesImpl attr = new AttributesImpl();
            attr.addAttribute("http://apache.org/xml/xmlns/jakarta/commons/jocl","value","value","CDATA","3.0");
            jocl.startElement("http://apache.org/xml/xmlns/jakarta/commons/jocl","float","float",attr);
            jocl.endElement("http://apache.org/xml/xmlns/jakarta/commons/jocl","float","float");
        }
        {
            AttributesImpl attr = new AttributesImpl();
            attr.addAttribute("http://apache.org/xml/xmlns/jakarta/commons/jocl","value","value","CDATA","5");
            jocl.startElement("http://apache.org/xml/xmlns/jakarta/commons/jocl","int","int",attr);
            jocl.endElement("http://apache.org/xml/xmlns/jakarta/commons/jocl","int","int");
        }
        {
            AttributesImpl attr = new AttributesImpl();
            attr.addAttribute("http://apache.org/xml/xmlns/jakarta/commons/jocl","value","value","CDATA","7");
            jocl.startElement("http://apache.org/xml/xmlns/jakarta/commons/jocl","long","long",attr);
            jocl.endElement("http://apache.org/xml/xmlns/jakarta/commons/jocl","long","long");
        }
        {
            AttributesImpl attr = new AttributesImpl();
            attr.addAttribute("http://apache.org/xml/xmlns/jakarta/commons/jocl","value","value","CDATA","11");
            jocl.startElement("http://apache.org/xml/xmlns/jakarta/commons/jocl","short","short",attr);
            jocl.endElement("http://apache.org/xml/xmlns/jakarta/commons/jocl","short","short");
        }
        {
            AttributesImpl attr = new AttributesImpl();
            attr.addAttribute("http://apache.org/xml/xmlns/jakarta/commons/jocl","value","value","CDATA","All your base are belong to us.");
            jocl.startElement("http://apache.org/xml/xmlns/jakarta/commons/jocl","string","string",attr);
            jocl.endElement("http://apache.org/xml/xmlns/jakarta/commons/jocl","string","string");
        }
        jocl.endElement("http://apache.org/xml/xmlns/jakarta/commons/jocl","jocl","jocl");
        jocl.endDocument();

        assertEquals(Boolean.TYPE,jocl.getType(0));
        assertEquals(Byte.TYPE,jocl.getType(1));
        assertEquals(Character.TYPE,jocl.getType(2));
        assertEquals(Double.TYPE,jocl.getType(3));
        assertEquals(Float.TYPE,jocl.getType(4));
        assertEquals(Integer.TYPE,jocl.getType(5));
        assertEquals(Long.TYPE,jocl.getType(6));
        assertEquals(Short.TYPE,jocl.getType(7));
        assertEquals(String.class,jocl.getType(8));

        assertEquals(Boolean.TRUE,jocl.getValue(0));
        assertEquals(new Byte("1"),jocl.getValue(1));
        assertEquals(new Character('c'),jocl.getValue(2));
        assertEquals(new Double("2.0"),jocl.getValue(3));
        assertEquals(new Float("3.0"),jocl.getValue(4));
        assertEquals(new Integer("5"),jocl.getValue(5));
        assertEquals(new Long("7"),jocl.getValue(6));
        assertEquals(new Short("11"),jocl.getValue(7));
        assertEquals("All your base are belong to us.",jocl.getValue(8));
    }

    public void testObject() throws Exception {
        jocl.startDocument();
        jocl.startElement("http://apache.org/xml/xmlns/jakarta/commons/jocl","jocl","jocl",new AttributesImpl());
        {
            AttributesImpl attr = new AttributesImpl();
            attr.addAttribute("http://apache.org/xml/xmlns/jakarta/commons/jocl","null","null","CDATA","true");
            attr.addAttribute("http://apache.org/xml/xmlns/jakarta/commons/jocl","class","class","CDATA","java.lang.String");
            jocl.startElement("http://apache.org/xml/xmlns/jakarta/commons/jocl","object","object",attr);
            jocl.endElement("http://apache.org/xml/xmlns/jakarta/commons/jocl","object","object");
        }
        {
            AttributesImpl attr = new AttributesImpl();
            attr.addAttribute("http://apache.org/xml/xmlns/jakarta/commons/jocl","class","class","CDATA","java.util.Date");
            jocl.startElement("http://apache.org/xml/xmlns/jakarta/commons/jocl","object","object",attr);
            jocl.endElement("http://apache.org/xml/xmlns/jakarta/commons/jocl","object","object");
        }
        {
            AttributesImpl attr = new AttributesImpl();
            attr.addAttribute("http://apache.org/xml/xmlns/jakarta/commons/jocl","class","class","CDATA","java.util.Date");
            jocl.startElement("http://apache.org/xml/xmlns/jakarta/commons/jocl","object","object",attr);
        }
        {
            AttributesImpl attr = new AttributesImpl();
            attr.addAttribute("http://apache.org/xml/xmlns/jakarta/commons/jocl","value","value","CDATA","3");
            jocl.startElement("http://apache.org/xml/xmlns/jakarta/commons/jocl","int","int",attr);
            jocl.endElement("http://apache.org/xml/xmlns/jakarta/commons/jocl","int","int");
        }
        {
            AttributesImpl attr = new AttributesImpl();
            attr.addAttribute("http://apache.org/xml/xmlns/jakarta/commons/jocl","value","value","CDATA","4");
            jocl.startElement("http://apache.org/xml/xmlns/jakarta/commons/jocl","int","int",attr);
            jocl.endElement("http://apache.org/xml/xmlns/jakarta/commons/jocl","int","int");
        }
        {
            AttributesImpl attr = new AttributesImpl();
            attr.addAttribute("http://apache.org/xml/xmlns/jakarta/commons/jocl","value","value","CDATA","5");
            jocl.startElement("http://apache.org/xml/xmlns/jakarta/commons/jocl","int","int",attr);
            jocl.endElement("http://apache.org/xml/xmlns/jakarta/commons/jocl","int","int");
        }
        jocl.endElement("http://apache.org/xml/xmlns/jakarta/commons/jocl","object","object");
        jocl.endElement("http://apache.org/xml/xmlns/jakarta/commons/jocl","jocl","jocl");
        jocl.endDocument();

        assertEquals(String.class,jocl.getType(0));
        assertEquals(java.util.Date.class,jocl.getType(1));
        assertEquals(java.util.Date.class,jocl.getType(2));

        assert(null == jocl.getValue(0));
        assert(null != jocl.getValue(1));
        assertEquals(new java.util.Date(3,4,5),jocl.getValue(2));
    }
}
