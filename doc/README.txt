===================================================================================

Before running these examples make sure you have registered the database
driver you want to use. If you don't you will get the following error:
"org.apache.commons.dbcp.DbcpException: java.sql.SQLException: No suitable driver"

The DriverManager class will attempt to load the driver classes referenced 
in the "jdbc.drivers" system property. For example you might specify

-Djdbc.drivers=foo.bah.Driver:wombat.sql.Driver:bad.taste.ourDriver

as command line argument to the java VM.

A program can also explicitly load JDBC drivers at any time. For
example, the my.sql.Driver is loaded with the following statement:

 Class.forName("my.sql.Driver");

===================================================================================

ManualPoolingDriverExample.java

 Provides a simple example of how to use the DBCP package with a
 manually configured PoolingDriver.

 Look at the comments with that file for instructions on how to
 build and run it.

ManualPoolingDataSource.java

 Provides a simple example of how to use the DBCP package with a
 manually configured PoolingDataSource.

 Look at the comments with that file for instructions on how to
 build and run it.

JOCLPoolingDriverExample.java
poolingDriverExample.jocl.sample

 Provides an example JOCL configuration and JDBC client that
 shows how to use the DBCP PoolingDriver with an external
 configuration. (JOCL will be replaced by Digester when it
 is available in jakarta-commons.)

 Look at the comments with those files for instructions on how to
 build and run it.

See also the JavaDoc documentation (use "ant doc" to generate it),
especially the package documentation for org.apache.commons.dbcp
for an overview of how it all works.

The test cases (the source files whose names start with "Test")
provide some additional examples.

