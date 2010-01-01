Apache Commons DBCP
===========================

Welcome to the DBCP component of the Apache Commons
project (http://commons.apache.org).

DBCP now comes in two different versions, one to support JDBC 3
and one to support JDBC 4.  Here is how it works:
 
DBCP 1.4 compiles and runs under JDK 1.6 only (JDBC 4)
DBCP 1.3 compiles and runs under JDK 1.4-1.5 only (JDBC 3)

DBCP 1.4 binaries should be used by applications running under JDK 1.6
DBCP 1.3 should be used when running under JDK 1.4 or 1.5.

There is no difference in the codebase supporting these two
versions, other than that the code implementing methods added
to support JDBC 4 has been filtered out of the DBCP 1.3 sources.
 
Both versions can be built using either Ant or Maven (version 2).
To build DBCP using Maven, type "mvn package" at a command line prompt
from the top-level directory of the source distribution (the directory
that contains the file named pom.xml).   

DBCP can also be built using Ant from the build.xml file. 
Locations of dependent jars for the Ant build need to be specified in 
build.properties. There is a build.properties.sample file included in the
source distribution.

See http://commons.apache.org/dbcp/ for additional and 
up-to-date information on Commons DBCP.

