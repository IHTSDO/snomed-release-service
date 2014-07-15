#!/bin/bash

# Stop on error
set -e;

# Check we have what we need to run
if [ -z "$buildName" ]
then
	echo "Please call this script from one of the 'run' scripts."
	echo
	echo "Script Halted"
	exit -1	
fi

# Recover a list of builds and see if we can find the ID of the target name
echo "Recover build list"
curl ${commonParams} ${api}/builds | tee tmp/builds-response.txt | grep HTTP | ensureCorrectResponse 
echo "Recover id of build: ${buildName}"
buildId=`cat tmp/builds-response.txt | grep -B1 "\"name\" : \"${buildName}\"" | head -1 | sed 's/.*: "\([^"]*\).*".*/\1/g'` || true


# Did we find it, or do we need to create it?
if [ -n "$buildId" ]
then
	echo "Build ID recovered: ${buildId}"
else
	# Create Build with the required name
	curl ${commonParams} -X POST -H 'Content-Type:application/json' --data-binary "{ \"name\" : \"${buildName}\" }"  ${api}/centers/${rcId}/extensions/${extId}/products/${prodId}/builds | tee tmp/create-build-response.txt | grep HTTP | ensureCorrectResponse
	echo "Recover id of build: ${buildName}"
	buildId=`cat tmp/create-build-response.txt | grep -B1 "\"name\" : \"${buildName}\"" | head -1 | sed 's/.*: "\([^"]*\).*".*/\1/g'` || true
	if [ -n "$buildId" ]
	then
		echo "Newly created build ID recovered: ${buildId}"
	else
		echo "Failed to create build, script halting"
		exit -1
	fi
fi
	