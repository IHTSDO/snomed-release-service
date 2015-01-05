#!/bin/bash
set -e

environmentName="prod"
apiPort=8080
filename="${environmentName}-data-service.properties"

function getProperty() {
	property=$1	
	cat ${propertiesFile} | grep ${property} | awk '{print $2}'
}

while getopts ":dp:" opt
do
	case $opt in
		d) 
			debugMode=true
			echo "Option set to start API in debug mode."
			debugFlags="-Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8001 -Djava.compiler=NONE" 
		;;
		p)
			apiPort=$OPTARG
			echo "Option set run API on port ${apiPort}"
		;;
		help|\?)
			echo -e "Usage: [-d]  [-p <port>]"
			echo -e "\t d - debug. Starts the API in debug mode, which an IDE can attach to on port 8001"
			echo -e "\t p <port> - Starts the API on a specific port (default 8080)"
			exit 0
		;;
	esac
done

echo "Please enter a username under which to set up the id-gen tunnel: "
read tunnelUserName

propertiesFile="`pwd`/../data-service/target/classes/${filename}"
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
	mvn -f ../pom.xml clean install -Dapple.awt.UIElement='true' -DskipTests=true
	echo

	echo "Starting API webapp using $environmentName environment on port ${apiPort}."
	echo
	sleep 1
	java ${debugFlags} -Xmx4g -DENV_NAME=$(whoami) -jar target/exec-api.jar -DdataServicePropertiesPath="file://${propertiesFile}"  -httpPort=${apiPort}
else
	echo "You don't have access to the $environmentName environment."
	echo
	exit 1
fi
