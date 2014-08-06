#!/bin/sh

set -e

outputDir=$1
api="https://uat-release.ihtsdotools.org/api/v1"
timeoutMins=3

function getBuiltExecutionId {
	curl -s $api/builds/5_int_daily_build/executions 2>/dev/null | grep -A2 "\"id\" : \"$today" | grep -B2 '"status" : "BUILT"' | head -n1 | awk -F \" '{print $4}'
}

today="2014-06-26T20:45:00"
#today=$(date +%Y-%m-%d)
attempt=1
builtExecutionId=$(getBuiltExecutionId)
while [ "$builtExecutionId" == "" ] && [ $attempt -le $timeoutMins ]; do
	echo "Built execution not available, sleeping a minute."
	sleep 120
	let "attempt += 1"
	builtExecutionId=$(getBuiltExecutionId)
done

if [ "$builtExecutionId" != "" ]; then
	echo "builtExecutionId = $builtExecutionId"
	releaseFile=$(curl -s ${api}/builds/5_int_daily_build/executions/${builtExecutionId}/packages/snomed_release_package/outputfiles | grep '"url"' | grep '.zip"' | awk -F \" '{ print $4 }')
	echo "Downloading $releaseFile"
	mkdir -p $outputDir
	cd $outputDir
	curl -sO $releaseFile
	cd -
	echo "Done"
else
	echo "Built execution not available. Reached timeout."
	exit 1
fi
