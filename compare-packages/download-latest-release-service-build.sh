#!/bin/bash

set -e

if [ -z "${1}" ]
then
	echo -e "Usage download-latest-release-service-build.sh <output directory>\nScript Halted."
	exit -1
fi

outputDir=$1
api="https://uat-release.ihtsdotools.org/api/v1"
timeoutMins=3


function getBuiltExecutionId {
	curl -s $api/builds/5_int_daily_build/executions 2>/dev/null | grep -A2 "\"id\" : \"$today" | grep -B2 '"status" : "BUILT"' | head -n1 | awk -F \" '{print $4}'
}

today=$(date +%Y-%m-%d)
attempt=1
executionsURL="${api}/builds/5_int_daily_build/executions"
echo "Looking for executions files at ${executionsURL} with date ${today}"
builtExecutionId=$(getBuiltExecutionId)
while [ "$builtExecutionId" == "" ] && [ $attempt -le $timeoutMins ]; do
	echo "Built execution not available, sleeping a minute."
	sleep 120
	let "attempt += 1"
	builtExecutionId=$(getBuiltExecutionId)
done

outputFilesURL="${api}/builds/5_int_daily_build/executions/${builtExecutionId}/packages/snomed_release_package/outputfiles"
echo "Looking for output files at ${outputFilesURL}"

if [ "$builtExecutionId" != "" ]; then
	releaseFile=$(curl -s ${outputFilesURL} | grep '"url"' | grep '.zip"' | awk -F \" '{ print $4 }')
	echo "Downloading $releaseFile"
	mkdir -p $outputDir > /dev/null
	cd $outputDir
	curl -sO $releaseFile
	cd -  > /dev/null
	echo "Done"
else
	echo "Built execution not available. Reached timeout."
	exit 1
fi
