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
//  * commons-pool-1.5.4.jar
//  * commons-dbcp-1.2.2.jar
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
//       JOCLPoolingDriverExample \
//       "jdbc:oracle:thin:scott/tiger@myhost:1521:mysid" \
//       "SELECT * FROM DUAL"
//
// For pooling:
//
//  java -Djdbc.drivers=oracle.jdbc.driver.OracleDriver:org.apache.commons.dbcp.PoolingDriver \
//       -classpath commons-pool-1.5.4.jar:commons-dbcp-1.2.2.jar:oracle-jdbc.jar:jaxp.jar:parser.jar:sax2.jar:. \
//       JOCLPoolingDriverExample \
//       "jdbc:apache:commons:dbcp:/poolingDriverExample" \
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
            try { if (rset != null) rset.close(); } catch(Exception e) { }
            try { if (stmt != null) stmt.close(); } catch(Exception e) { }
            try { if (conn != null) conn.close(); } catch(Exception e) { }
        }
    }
}
