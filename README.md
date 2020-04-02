# Master2020

mvn install:install-file -Dfile=C:\gurobi811\win64\lib\gurobi.jar -DgroupId=gurobi -DartifactId=gurobi -Dversion=8 -Dpackaging=jar



SETUP SOLSTORM:

add java to path:

export PATH=$PATH:~:/share/apps/Java/11.0.2/bin
export JAVA_HOME=/share/apps/Java/11.0.2

*MIDLERTIDIG*
legg til maven:

Kopier apache-maven-3.6.3 mappen over i ditt directory på solstorm (storage/global/brynjag/Maven/apache-maven-3.6.3 for min del)

legg til Maven på pathen:

export PATH=$PATH:~:/storage/global/brynjag/Maven/apache-maven-3.6.3/bin
