<?xml version="1.0"?>
<!--
 Licensed to the Apache Software Foundation (ASF) under one or more
 contributor license agreements.  See the NOTICE file distributed with
 this work for additional information regarding copyright ownership.
 The ASF licenses this file to You under the Apache License, Version 2.0
 (the "License"); you may not use this file except in compliance with
 the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
-->
<jsp:root xmlns="http://java.sun.com/JSP/Page" version="1.2">
<jsp:directive.page contentType="text/html"/>
<jsp:directive.page import="java.io.*"/>
<jsp:directive.page import="java.sql.*"/>
<jsp:directive.page import="javax.sql.*"/>
<jsp:directive.page import="javax.naming.*"/>

<!--
  This jsp is used to test the abandoned connection feature of BasicDataSource.
  Add the following configuration to server.xml and load the jsp page.
  The connections aren't closed and are logged to stdout/stderr.

    <Resource name="jdbc/abandoned" auth="Container" type="javax.sql.DataSource"/>
    <ResourceParams name="jdbc/abandoned">
      <parameter><name>username</name><value>sa</value></parameter>
      <parameter><name>password</name><value></value></parameter>
      <parameter><name>driverClassName</name><value>org.hsqldb.jdbcDriver</value></parameter>
      <parameter><name>url</name><value>jdbc:hsqldb:database</value></parameter>

      <parameter><name>removeAbandoned</name><value>true</value></parameter>
      <parameter><name>removeAbandonedTimeout</name><value>0</value></parameter>
      <parameter><name>logAbandoned</name><value>true</value></parameter>

      <parameter><name>maxTotal</name><value>1</value></parameter>
    </ResourceParams>
-->

<jsp:declaration>
<![CDATA[
    public DataSource getDataSource(JspWriter out) throws Exception {
        Context ctx = null;

        try {
            ctx = new InitialContext();
        } catch (NamingException e) {
            out.println("<br/>Couldn't build an initial context : " + e);
            return null;
        }

        try {
            Object value = ctx.lookup("java:/comp/env/jdbc/abandoned");
            out.println("<br/>DataSource lookup");
            out.println("<br/>jdbc value : " + value);
            out.println("<br/>jdbc class : " + value.getClass().getName());
            out.println("<hr/>");

            if (value instanceof DataSource) {
                return (DataSource) value;
            }
            else {
                return null;
            }

        } catch (NamingException e) {
            out.println("<br/>JNDI lookup failed : " + e);
            return null;
        }
    }

    private void getConnection1(DataSource ds, JspWriter out) throws Exception {
        System.err.println("BEGIN getConnection1()");
        out.println("<br/>BEGIN getConnection1()");

        Connection conn = ds.getConnection();
        System.err.println("conn: " + conn);
        out.println("<br/>conn: " + conn);

        System.err.println("END getConnection1()");
        out.println("<br/>END getConnection1()");
    }

    private void getConnection2(DataSource ds, JspWriter out) throws Exception {
        System.err.println("BEGIN getConnection2()");
        out.println("<br/>BEGIN getConnection2()");

        Connection conn = ds.getConnection();
        System.err.println("conn: " + conn);
        out.println("<br/>conn: " + conn);

        System.err.println("END getConnection2()");
        out.println("<br/>END getConnection2()");
    }

    private void getConnection3(DataSource ds, JspWriter out) throws Exception {
        System.err.println("BEGIN getConnection3()");
        out.println("<br/>BEGIN getConnection3()");

        Connection conn = ds.getConnection();
        System.err.println("conn: " + conn);
        out.println("<br/>conn: " + conn);

        System.err.println("END getConnection3()");
        out.println("<br/>END getConnection3()");
    }
]]>
</jsp:declaration>

<html>
<head>
  <title>DBCP Abandoned Connection Test</title>
</head>
<body>
<h1>DBCP Abandoned Connection Test</h1>
<hr/>
<jsp:scriptlet>
    DataSource ds = getDataSource(out);
    if (ds != null) {
        getConnection1(ds, out);
        getConnection2(ds, out);
        getConnection3(ds, out);
    }
</jsp:scriptlet>
<hr/>
<br/>
OK
</body>
</html>
</jsp:root>
