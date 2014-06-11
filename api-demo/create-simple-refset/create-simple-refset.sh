#!/bin/bash
#
# Command line statements which use the API to create a simple refset
#

# Stop on error
set -e;

# Declare parameters
#api=http://localhost:8080/api/v1
api="http://uat-release.ihtsdotools.org/api/v1"
buildId="1_20140731_international_release_build"
packageId="snomed_release_package"

curlFlags="isS"
# i - Show response header
# s - quiet
# S - show errors

echo
echo "Target API URL is '${api}'"
echo "Target Build ID is '${buildId}'"
echo "Target Package ID is '${packageId}'"
echo

# Login
echo "Logging in and recording authorisation token."
curl -${curlFlags} -F username=manager -F password=test123 $api/login > tmp/login-response.txt
token=`cat tmp/login-response.txt | grep "Token" | sed 's/.*: "\([^"]*\)".*/\1/g'`
echo "Token is '${token}'"
echo

echo "Upload Manifset"
curl -${curlFlags} -u "${token}:" -F "file=@manifest.xml" $api/builds/${buildId}/packages/${packageId}/manifest
echo

echo "Creating Execution"
curl -${curlFlags} -X POST -u "${token}:" $api/builds/${buildId}/executions > tmp/execution-response.txt
executionId=`cat tmp/execution-response.txt | grep "id" | sed 's/.*: "\([^"]*\).*".*/\1/g'`
echo "Execution ID is '${executionId}'"
echo

echo "Trigger Execution"
curl -${curlFlags} -X POST -u "${token}:" $api/builds/${buildId}/executions/${executionId}/trigger
echo
