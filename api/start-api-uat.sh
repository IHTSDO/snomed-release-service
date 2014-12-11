#!/bin/bash
set -e

environmentName="uat"
filename="${environmentName}-data-service.properties"

echo 'Building API webapp (skipping tests)..'
sleep 1
mvn clean install -Dapple.awt.UIElement='true' -DskipTests=true
echo
propertiesFile="`pwd`/$filename"
if [ -f "$propertiesFile" ]; then
	echo "Starting API webapp using $environmentName environment."
	echo
	sleep 1
	java -jar target/exec-api.jar -DdataServicePropertiesPath="file://$propertiesFile"
else
	echo "You don't have access to the $environmentName environment."
	echo
	exit 1
fi
