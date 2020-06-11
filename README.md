# Heuristic Methods for a Periodic Multi-Trip Vehicle Routing Problem in the Food Distribution Industry

#Description
The project describes multiple heuristics to solve the periodic multi-trip vehicle routing problem with time windows (PMTVRPTW), incompatible commodities, and a heterogeneous fleet. The project is coded as part of a master thesis in optimization from NTNU 2020.

NOTE THAT THE DATA USED IN THIS  CODE MUST NOT BE DISTRIBUTED AS ITS PROPERTY OF ASKO AS.

##Dependencies
In order to run the project the user has to have installed Apache Maven v3.1.6 or newer. In addition, Gurobi optimizer v8.1.  or newer is required. The following environment variables must be set properly:
 * GUROBI_HOME: point to the Gurobi folder
 * JAVA_HOME: point to the SDK version folder
 * MAVEN_HOME: to Apache Maven folder
 
 Java, Gurobi, and Maven must also be added to path.
 
 After these properties has been set, run following command in order to install Gurobi to Maven
 
 mvn install:install-file -Dfile=["PATH TO GUROBI.JAR"] -DgroupId=gurobi -DartifactId=gurobi -Dversion=8 -Dpackaging=jar

where the path has been set correctly.

In order to run the project, build it using command "mvn package".

Run the algorithm by running the VRP-1.0.jar file in the /target folder

Input arguments are [GA, ABC, PGA, HYBRID] [base, full]

in order to reproduce results reported in the master thesis


##Contributors
Fride Elise Bakken \
Brynjar Glimsdal \
Lars Kåre Tvinnereim