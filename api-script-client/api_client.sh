#!/bin/bash

# Stop on error
set -e;

#
# Command line statements which use the API to create a simple refset
# Expects to be called from one of the run_*.sh scripts.
#

# Declare common parameters
api=http://localhost:8080/api/v1
#api="http://local.ihtsdotools.org/api/v1"
#api="https://uat-release.ihtsdotools.org/api/v1"
#api="http://dev-release.ihtsdotools.org/api/v1"
#api="http://release.ihtsdotools.org/api/v1"

rcId=international
extId=snomed_ct_international_edition
prodId=snomed_ct_release
buildId="1_20140731_international_release_build"
packageId="snomed_release_package"
readmeHeader="readme-header.txt"

# Set curl verbocity
curlFlags="isS"
# i - Show response header
# s - quiet
# S - show errors

ensureCorrectResponse() {
	while read response 
	do
		httpResponseCode=`echo $response | grep "HTTP" | awk '{print $2}'`
		echo " Response received: $response "
		if [ "${httpResponseCode:0:1}" != "2" ] && [ "${httpResponseCode:0:1}" != "1" ]
		then
			echo "Failure detected with non-2xx HTTP response code received.  Script halted"
			exit -1
		fi
	done
	echo
}

getElapsedTime() {
	seconds=${SECONDS}
	hours=$((seconds / 3600))
	seconds=$((seconds % 3600))
	minutes=$((seconds / 60))
	seconds=$((seconds % 60))
	
	echo "$hours hour(s) $minutes minute(s) $seconds second(s)"
}

# Check command line arguments
if [ -z "$effectiveDate" ]
then
	echo "Please call this script from one of the 'run' scripts."
	echo
	echo "Script Halted"
	exit -1	
fi

skipLoad=false
listOnly=false
autoPublish=false
completePublish=false

while getopts ":slcar:p:" opt
do
	case $opt in
		s) 
			skipLoad=true
			echo "Option set to skip input file load."
		;;
		l) 
			listOnly=true
			echo "Option set to list input files only."
		;;
		a) 
			autoPublish=true
			echo "Option set to automatically publish packages on successful execution."
		;;
		p) 
			publishFile=$OPTARG
			echo "Option set to upload ${publishFile} for publishing only."
		;;
		c) 
			completePublish=true
			if [ -n "${publishFile}" ] || ${autoPublish} 
			then
				echo "Mutually exclusive command line options set"
				exit -1
			fi
			echo "Option set to complete.  Last execution will be published."
		;;
		r) 
			replaceInputFile=$OPTARG
			if  ${skipLoad} 
			then
				echo "Mutually exclusive command line options set"
				exit -1
			fi
			echo "Option set to replace input file ${replaceInputFile} and execute a build."
		;;
		help|\?) 
			echo -e "Usage: [-s] [-l] [-a] [-c] [-r <filename>] [-p <filename>]"
			echo -e "\t s - skip.  Skips the upload of input files (say if you've already run the process and they don't need to change)."
			echo -e "\t l - list.  Just lists the current input files and does no further processing." 
			echo -e "\t r <filename> - replace.  Uploads just the file specified and then runs the execution."
			echo -e "\t c - complete.  Completes the execution by publishing the last generated zip file."
			echo -e "\t a - automatically publish packages on successful execution." 
			echo -e "\t p <filename> - publish. Uploads the specified zip file for publishing independent of any execution (eg for priming the system with a previous release)."
			exit 0
		;;
	esac
done

echo
echo "Target API URL is '${api}'"
echo "Target Build ID is '${buildId}'"
echo "Target Package ID is '${packageId}'"
echo

mkdir -p tmp

# Login
echo "Login and record authorisation token."
curl -${curlFlags} -F username=manager -F password=test123 ${api}/login | tee tmp/login-response.txt | grep HTTP | ensureCorrectResponse
token=`cat tmp/login-response.txt | grep "Token" | sed 's/.*: "\([^"]*\)".*/\1/g'`
echo "Authorisation Token is '${token}'"
# Ensure we have a valid token before proceeding
if [ -z "${token}" ]
then
	echo "Cannot proceed further if we haven't logged in!"
	exit -1
fi
commonParamsSilent="-s --retry 0 -u ${token}:"
commonParams="-${curlFlags} --retry 0 -u ${token}:"

echo
# Are we just listing the input files and stopping there?
if ${listOnly}
then
	echo "Recover input file list"
	curl ${commonParams} ${api}/builds/${buildId}/packages/${packageId}/inputfiles | tee tmp/listing-response.txt | grep HTTP | ensureCorrectResponse 
	echo "Input delta files currently held:"
	cat tmp/listing-response.txt | grep "id" | sed 's/.*: "\([^"]*\).*".*/\1/g'
	exit 0
fi

# Are we just uploading a file for publishing and stopping there?
if [ -n "${publishFile}" ]
then
	echo "Upload file to be published: ${publishFile}"
	curl ${commonParams} -X POST -F "file=@${publishFile}" ${api}/centers/${rcId}/extensions/${extId}/products/${prodId}/published  | grep HTTP | ensureCorrectResponse 
	echo "File successfully published in $(getElapsedTime)"
	exit 0
fi

# Are we just publishing the last execution and stopping there?
if ${completePublish}
then 
	# Recover the last known execution ID
	executionId=`cat tmp/execution-response.txt | grep "\"id\"" | sed 's/.*: "\([^"]*\).*".*/\1/g'`
	echo "Execution ID is '${executionId}'"
	echo "Publish the package"
	curl ${commonParams} ${api}/builds/${buildId}/executions/${executionId}/output/publish  | grep HTTP | ensureCorrectResponse
	echo "Process Complete in $(getElapsedTime)"
	exit 0
fi

echo "Set Readme Header and readmeEndDate"
readmeHeaderContents=`cat ${readmeHeader} | python -c 'import json,sys; print json.dumps(sys.stdin.read())' | sed -e 's/^.\(.*\).$/\1/'`
curl ${commonParams} -X PATCH -H 'Content-Type:application/json' --data-binary "{ \"readmeHeader\" : \"${readmeHeaderContents}\", \"readmeEndDate\" : \"${readmeEndDate}\" }" ${api}/builds/${buildId}/packages/${packageId}  | grep HTTP | ensureCorrectResponse

echo "Upload Manifest"
curl ${commonParams} --write-out \\n%{http_code} -F "file=@manifest.xml" ${api}/builds/${buildId}/packages/${packageId}/manifest  | grep HTTP | ensureCorrectResponse


if ! ${skipLoad}
then
	# Are we just replacing one file, or uploading the whole lot?
	if [ -n "${replaceInputFile}" ]
	then
			echo "Replacing Input File ${replaceInputFile}"
			curl ${commonParams} -F "file=@${replaceInputFile}" ${api}/builds/${buildId}/packages/${packageId}/inputfiles | grep HTTP | ensureCorrectResponse
				
	else
		# If we've done a different release before, then we need to delete the input files from the last run!
		# Not checking the return code from this call, doesn't matter if the files aren't there
		echo "Delete previous delta Input Files "
		curl ${commonParams} -X DELETE ${api}/builds/${buildId}/packages/${packageId}/inputfiles/*.txt | grep HTTP | ensureCorrectResponse
		
		
		inputFilesPath="input-files/${executionName}"
		echo "Upload Input Files:"
		for file in `ls input_files`;
		do
			echo "Upload Input File ${file}"
			curl ${commonParams} -F "file=@input_files/${file}" ${api}/builds/${buildId}/packages/${packageId}/inputfiles | grep HTTP | ensureCorrectResponse
		done
	fi
fi

echo "Set effectiveTime to ${effectiveDate}"
curl ${commonParams} -X PATCH -H 'Content-Type:application/json' --data-binary "{ \"effectiveTime\" : \"${effectiveDate}\" }"  ${api}/builds/${buildId}  | grep HTTP | ensureCorrectResponse

if [ "${justPackage}" = "true" ]
then
	echo "Set justPackage flag to true"
	curl ${commonParams} -X PATCH -H 'Content-Type:application/json' --data-binary "{ \"justPackage\" : \"true\"  }" ${api}/builds/${buildId}/packages/${packageId}  | grep HTTP | ensureCorrectResponse
else

	echo "Set justPackage flag to false"
	curl ${commonParams} -X PATCH -H 'Content-Type:application/json' --data-binary "{ \"justPackage\" : \"false\"  }" ${api}/builds/${buildId}/packages/${packageId}  | grep HTTP | ensureCorrectResponse

	# Set the first time release flag, and if a subsequent release, recover the previously published package and set that
	firstTimeStr="${isFirstTime}"
	if ${isFirstTime}
	then
		echo "Set first time flag to ${firstTimeStr}"
		curl ${commonParams} -X PATCH -H 'Content-Type:application/json' --data-binary "{ \"firstTimeRelease\" : \"${firstTimeStr}\"  }" ${api}/builds/${buildId}/packages/${packageId}  | grep HTTP | ensureCorrectResponse
	else
		echo "Set first time flag to ${firstTimeStr} and previous published package to ${previousPublishedPackageName}"
		updateJSON="{ \"firstTimeRelease\" : \"${firstTimeStr}\", \"previousPublishedPackage\" : \"${previousPublishedPackageName}\" }"
		curl ${commonParams} -X PATCH -H 'Content-Type:application/json' --data-binary "$updateJSON" ${api}/builds/${buildId}/packages/${packageId}  | grep HTTP | ensureCorrectResponse
	fi

fi

echo "Create Execution"
curl ${commonParams} -X POST ${api}/builds/${buildId}/executions | tee tmp/execution-response.txt | grep HTTP | ensureCorrectResponse
executionId=`cat tmp/execution-response.txt | grep "\"id\"" | sed 's/.*: "\([^"]*\).*".*/\1/g'`
echo "Execution ID is '${executionId}'"

echo "Preparation complete.  Time taken so far: $(getElapsedTime)"

echo "Trigger Execution"
curl ${commonParams} -X POST ${api}/builds/${buildId}/executions/${executionId}/trigger  | tee tmp/trigger-response.txt | grep HTTP | ensureCorrectResponse
triggerSuccess=`cat tmp/trigger-response.txt | grep pass` || true # Do not fail on exit here, some reporting first
if [ -z "${triggerSuccess}" ]
then
	echo "Failed to successfully process any packages.  Received response: "
	echo
	cat tmp/trigger-response.txt
	echo
	echo "Script Halted after $(getElapsedTime) "
	exit -1
fi

if ${autoPublish}
then
	echo "Publish the package"
	curl ${commonParams} ${api}/builds/${buildId}/executions/${executionId}/output/publish  | grep HTTP | ensureCorrectResponse
fi

echo "List the output files"
downloadUrlRoot=${api}/builds/${buildId}/executions/${executionId}/packages/${packageId}/outputfiles
localDownloadDirectory=output
curl ${commonParams} ${downloadUrlRoot} | tee tmp/output-file-listing.txt | grep HTTP | ensureCorrectResponse


mkdir -p ${localDownloadDirectory}
downloadFile() {
	read fileName
	echo "Downloading file to: ${localDownloadDirectory}/${fileName}"
	# Using curl as the MAC doesn't have wget loaded by default
	curl ${commonParamsSilent} ${downloadUrlRoot}/${fileName} -o "${localDownloadDirectory}/${fileName}"
}
cat tmp/output-file-listing.txt | grep id | while read line ; do echo  $line | sed 's/.*: "\([^"]*\).*".*/\1/g' | downloadFile; done

echo
echo "Process Complete in $(getElapsedTime)"
if ! ${autoPublish} 
then
	echo "Run again with the -c flag to just publish the packages, or -a to re-run the whole execution and automatically publish the results."
fi
echo
