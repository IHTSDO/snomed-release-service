#!/bin/bash
set -e

environmentName="prod"
filename="${environmentName}-data-service.properties"

function getProperty() {
	property=$1	
	cat ${propertiesFile} | grep ${property} | awk '{print $2}'
}

echo "Please enter a username under which to set up the id-gen tunnel: "
read tunnelUserName

echo "What port is nginx expecting to find the api on? "
read apiPort

propertiesFile="`pwd`/data-service/target/classes/${filename}"
if [ -f "$propertiesFile" ]; then

	# Check if tunnel is already established before running
	echo "Checking for existing tunnel and killing if found"
	ps -ef | grep ssh | grep $(getProperty target_hostname) | awk '{print $2}' | xargs kill
	
	command="ssh -L $(getProperty local_port):$(getProperty target_hostname):$(getProperty target_port) $(getProperty tunnel_proxy) -l ${tunnelUserName}"
	echo "Running tunnelling command: ${command}"
	${command} &
	
	# Give the user time to enter their password for this connection
	sleep 20
	
	echo 'Building API webapp (skipping tests)..'
	sleep 1
	mvn clean install -Dapple.awt.UIElement='true' -DskipTests=true
	echo

	echo "Starting API webapp using $environmentName environment."
	echo
	sleep 1
	java -Xdebug -Xnoagent -Xmx4g -DENV_NAME=PGW -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8000 -jar target/exec-api.jar -DdataServicePropertiesPath="file://${propertiesFile}"  -httpPort=${apiPort}
else
	echo "You don't have access to the $environmentName environment."
	echo
	exit 1
fi
