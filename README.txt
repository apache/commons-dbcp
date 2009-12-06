Apache Commons DBCP
===========================

Welcome to the DBCP component of the Apache Commons
project (http://commons.apache.org). 

DBCP uses Maven 2 (http://maven.apache.org) for build 
process management. 

To build DBCP using Maven, type "mvn package" at a command line prompt
from the top-level directory of the source distribution (the directory
that contains the file named pom.xml).

DBCP can also be built using Ant from the build.xml file. 
Locations of dependent jars for the Ant build need to be specified in 
build.properties.  There is a build.properties.sample file included in the
source distribution.

The 1.4 binary distribution was built using JDK 1.6.0_17. The Ant build includes
conditional compilation to support building on JDK 1.4 or 1.5. 

This release of JDBC compiles with and supports JDK 1.4-1.5 (JDBC 3.0)
and JDK 1.6 (JDBC 4.0).  The 1.4 binary release requires JDK 1.6 (JDBC 4.0). 
The 1.3 binary release was built from filtered versions of the same sources
using JDK 1.5.0_19.  

DBCP 1.4 binaries should be used by applications running under under JDK 1.6
and DBCP 1.3 should be used when running under JDK 1.4 or 1.5.

See http://commons.apache.org/dbcp/ for additional and 
up-to-date information on Commons DBCP.

