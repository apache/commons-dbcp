/*
 * $Source: /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons//dbcp/doc/JOCLPoolingDriverExample.java,v $
 * $Revision: 1.2 $
 * $Date: 2003/08/22 16:08:31 $
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
 * http://www.apache.org/
 *
 */

import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;


//
// Here's a simple example of how to use the PoolingDriver.
// In this example, we'll construct the PoolingDriver implictly
// using the JOCL configuration mechanism.
//
// Note that there is absolutely nothing DBCP specific about
// this code, it's just straight JDBC.  You can simply
// switch connection strings to use the "native" drivers
// directly.
//

//
// To compile this example, you'll need nothing but the JDK (1.2+)
// in your classpath.
//
// To run this example, you'll want:
//  * commons-collections.jar
//  * commons-pool.jar
//  * commons-dbcp.jar
//  * the classes for your (underlying) JDBC driver
//  * sax2.jar (the SAX 2 API)
//  * a SAX2 friendly XML parser (jaxp.jar and parser.jar,
//    for example)
//  * the JOCL configuration for your database connection pool
//    (poolingDriverExample.jocl, for example)
// in your classpath.
//
// Invoke the class using two arguments:
//  * the connect string for the JDBC driver (see below)
//  * the query you'd like to execute
// You'll also want to ensure your both your underlying JDBC
// driver and the org.apache.commons.dbcp.PoolingDriver
// are registered.  You can use the "jdbc.drivers"
// property to do this.  Note that jdbc.drivers is colon
// seperated list, on all platforms.
//
// Depending upon your XML parser, you may need to register
// the "default" SAX driver, using the "org.xml.sax.driver"
// property.
//
// For example, to invoke this class with an Oracle driver only
// (no pooling):
//
//  java -Djdbc.drivers=oracle.jdbc.driver.OracleDriver \
//       -classpath oracle-jdbc.jar:. \
//       JOCLPoolingDriverExample
//       "jdbc:oracle:thin:scott/tiger@myhost:1521:mysid"
//       "SELECT * FROM DUAL"
//
// For pooling:
//
//  java -Djdbc.drivers=oracle.jdbc.driver.OracleDriver:org.apache.commons.dbcp.PoolingDriver \
//       -classpath commons-collections.jar:commons-pool.jar:commons-dbcp.jar:oracle-jdbc.jar:jaxp.jar:parser.jar:sax2.jar:. \
//       JOCLPoolingDriverExample
//       "jdbc:apache:commons:dbcp:/poolingDriverExample"
//       "SELECT * FROM DUAL"
//
// The last token in DBCP connect string (when suffixed with ".jocl")
// is the resource the PoolingDriver reads as the JOCL configuration.
// See Class.getResource for details on resource loading.
//
public class JOCLPoolingDriverExample {

    public static void main(String[] args) {
        //
        // Just plain-old JDBC.
        //

        Connection conn = null;
        Statement stmt = null;
        ResultSet rset = null;

        try {
            System.out.println("Creating connection.");
            conn = DriverManager.getConnection(args[0]);
            System.out.println("Creating statement.");
            stmt = conn.createStatement();
            System.out.println("Executing statement.");
            rset = stmt.executeQuery(args[1]);
            System.out.println("Results:");
            int numcols = rset.getMetaData().getColumnCount();
            while(rset.next()) {
                for(int i=1;i<=numcols;i++) {
                    System.out.print("\t" + rset.getString(i));
                }
                System.out.println("");
            }
        } catch(SQLException e) {
            e.printStackTrace();
        } finally {
            try { rset.close(); } catch(Exception e) { }
            try { stmt.close(); } catch(Exception e) { }
            try { conn.close(); } catch(Exception e) { }
        }
    }
}
