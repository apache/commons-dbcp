===================================================================================

Before running these examples make sure you have registered the database
driver you want to use. If you don't you will get the following error:
"org.apache.commons.dbcp2.DbcpException: java.sql.SQLException: No suitable driver"

The DriverManager class will attempt to load the driver classes referenced 
in the "jdbc.drivers" system property. For example you might specify

-Djdbc.drivers=foo.bah.Driver:wombat.sql.Driver:bad.taste.ourDriver

as command line argument to the java VM.

A program can also explicitly load JDBC drivers at any time. For
example, the my.sql.Driver is loaded with the following statement:

 Class.forName("my.sql.Driver");

===================================================================================

PoolingDriverExample.java

 Provides a simple example of how to use the DBCP package with a
 PoolingDriver.

 Look at the comments with that file for instructions on how to
 build and run it.

PoolingDataSource.java

 Provides a simple example of how to use the DBCP package with a
 PoolingDataSource.

 Look at the comments with that file for instructions on how to
 build and run it.

See also the JavaDoc documentation (use "ant doc" to generate it),
especially the package documentation for org.apache.commons.dbcp
for an overview of how it all works.

The test cases (the source files whose names start with "Test")
provide some additional examples.

