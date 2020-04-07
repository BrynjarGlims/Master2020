# Master2020

SETUP FOR SOLSTORM:

1. Login via Putty (Solstorm-login.iot.ntnu.no)

2. Add java to path and create JAVA_HOME environment variable:

export PATH=$PATH:~:/share/apps/Java/11.0.2/bin export JAVA_HOME=/share/apps/Java/11.0.2

3. Add Maven to path:

export PATH=$PATH:~:/share/apps/Maven/3.6.3/bin

4. clone master project to storage:

cd /storage/global

mkdir \<your-username\>

cd \<your-username\>

git clone \<master-project-link\>

cd Master2020

5. Install gurobi to maven:

mvn install:install-file -Dfile=/share/apps/gurobi/8.1.1/lib/gurobi.jar -DgroupId=gurobi -DartifactId=gurobi -Dversion=8 -Dpackaging=jar

6. Add gurobi environment variables:

export PATH=$PATH:~:/share/apps/gurobi/8.1.1/bin

export LD_LIBRARY_PATH=/share/apps/gurobi/8.1.1/lib

export GRB_LICENSE_FILE="/share/apps/gurobi/gurobi.lic"

7. Build maven project:

mvn clean package

8. Run project!

java -jar target/test-1.0-SNAPSHOT.jar \<model\> \<num_customers\>

example:

java -jar target/test-1.0-SNAPSHOT.jar JBM 25
