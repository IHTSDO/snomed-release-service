#!/bin/bash
set -e

environmentName="uat"
filename="${environmentName}-data-service.properties"

echo "What port is nginx expecting to find the api on? "
read apiPort

echo 'Building API webapp (skipping tests)..'
sleep 1
mvn -f ../pom.xml clean install -Dapple.awt.UIElement='true' -DskipTests=true
echo
propertiesFile="`pwd`/../data-service/target/classes/${filename}"
if [ -f "$propertiesFile" ]; then
	echo "Starting API webapp using $environmentName environment."
	echo
	sleep 1
	java -Xdebug -Xnoagent -Xmx4g -DENV_NAME=LOCAL -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8000 -jar target/exec-api.jar -DdataServicePropertiesPath="file://${propertiesFile}"  -httpPort=${apiPort}
else
	echo "You don't have access to the $environmentName environment."
	echo
	exit 1
fi
