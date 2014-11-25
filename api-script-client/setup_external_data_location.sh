#!/bin/bash

# Stop on error
set -e;

project="snomed-release-service-data"
command="git clone"
protocol="ssh://"
repository="@csfe.aceworkspace.net:29418/${project}"

# Check command line arguments
if [ -z "$externalDataLocation" ]
then
	echo "Please call this script from one of the 'run' scripts."
	echo
	echo "Script Halted"
	exit -1	
fi

echo "Check data directory exists"
origDir=`pwd`
cd ../../..

if [ ! -d "${project}" ]; then
  # We'll need to download from collabnet
	echo "Please enter your collabnet username: "
	read collabnetUsername
	repoUrl=${protocol}${collabnetUsername}${repository}
	echo "Cloning repository: ${repoUrl}"
	${command} ${repoUrl}
else
	# We just need an update of the directory that's already there
	echo "Updating ${project}"
	cd ${project}
	git pull
fi

echo "Returning to ${origDir}"
cd ${origDir}