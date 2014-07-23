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

# Should come from caller script:
# 	extensionName
# 	productName
#	buildName
#	packageName


releaseCentreId="international"
readmeHeader="readme-header.txt"
externalDataRoot="../../../snomed-release-service-data/api-script-client-data/"

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
			echo -e "Failure detected with non-2xx HTTP response code received.\nScript halted."
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

findEntity() {
	# Gather params
	entityType=$1
	entityResponseFile="tmp/entity-${entityType}-response.txt"
	curl ${entityUrl} 2>/dev/null | tee ${entityResponseFile} | grep HTTP | ensureCorrectResponse >/dev/null
	cat ${entityResponseFile} | grep -i -B1 "\"name\" : \"${entityName}\"" | head -n1 | sed 's/.*: "\(.*\)",/\1/'
}
findOrCreateEntity() {
	# Gather params
	entityType=$1
	entityUrl=$2
	entityName=$3

	if [ -z "${entityName}" ]
	then
		echo -e "${entityType} name not specified.\nScript halted."
		exit -1
	fi

	# Find existing entity
	entityId="`findEntity ${entityType}`"

	# Test if entity found
	if [ -z "${entityId}" ]
	then
		# Entity doesn't exist, create
		echo "Creating ${entityType}: ${entityName} using URL ${entityUrl}"
		jsonData="{ \"name\" : \"${entityName}\" }"
		curl ${commonParams} -X POST -H 'Content-Type:application/json' --data-binary "$jsonData" ${entityUrl} | grep HTTP | ensureCorrectResponse
		# Retrieve new entity id
		entityId="`findEntity ${entityType}-create`"
	fi

	echo "${entityType} Name '${entityName}', ID '${entityId}'."
	echo
}

# Check command line arguments
if [ -z "$effectiveDate" ]
then
	echo -e "Please call this script from one of the 'run' scripts.\nScript halted."
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

# This be printed first - before we start using it!
echo "Target API URL is '${api}'"
echo

findOrCreateEntity "Extension" "${api}/centers/${releaseCentreId}/extensions" "${extensionName}"
extensionId=${entityId}

findOrCreateEntity "Product" "${api}/centers/${releaseCentreId}/extensions/${extensionId}/products" "${productName}"
productId=${entityId}

# Default build name to product name
if [ -z "${buildName}" ]
then
	buildName="${productName}"
fi
findOrCreateEntity "Build" "${api}/centers/${releaseCentreId}/extensions/${extensionId}/products/${productId}/builds" "${buildName}"
buildId=${entityId}

# Default package name to build name
if [ -z "${packageName}" ]
then
	packageName="${buildName}"
fi
findOrCreateEntity "Package" "${api}/builds/${buildId}/packages" "${packageName}"
packageId=${entityId}

# Are we just listing the input and published files and stopping there?
if ${listOnly}
then
	echo "Recover input file list"
	curl ${commonParams} ${api}/builds/${buildId}/packages/${packageId}/inputfiles | tee tmp/listing-response.txt | grep HTTP | ensureCorrectResponse 
	echo "Input delta files currently held:"
	cat tmp/listing-response.txt | grep "id" | sed 's/.*: "\([^"]*\).*".*/\1/g'
	
	echo "Recover list of published files"
	curl ${commonParams} ${api}/centers/${releaseCentreId}/products/${productId}/published | tee tmp/published-listing-response.txt | grep HTTP | ensureCorrectResponse 
	echo "Published files for product ${productName}:"
	cat tmp/published-listing-response.txt 
	exit 0
fi

# Are we just uploading a file for publishing and stopping there?
if [ -n "${publishFile}" ]
then
	echo "Upload file to be published: ${publishFile}"
	curl ${commonParams} -X POST -F "file=@${publishFile}" ${api}/centers/${releaseCentreId}/extensions/${extensionId}/products/${productId}/published  | grep HTTP | ensureCorrectResponse
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
		
		if [ -n "$externalDataLocation" ]
		then
			source ../setup_external_data_location.sh
			inputFilesPath=${externalDataRoot}${externalDataLocation}
		else
			inputFilesPath="input_files"
		fi
		
		filesUploaded=0
		echo "Upload Input Files from ${inputFilesPath}:"
		for file in `ls ${inputFilesPath}`;
		do
			echo "Upload Input File ${file}"
			curl ${commonParams} -F "file=@${inputFilesPath}/${file}" ${api}/builds/${buildId}/packages/${packageId}/inputfiles | grep HTTP | ensureCorrectResponse
			((filesUploaded++))
		done
		
		if [ ${filesUploaded} -lt 1 ] 
		then
			echo -e "Failed to find files to upload.\nScript halted."
			exit -1
		fi
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
echo "Execution URL is '${api}/builds/${buildId}/executions/${executionId}'"
echo "Preparation complete.  Time taken so far: $(getElapsedTime)"
echo

#Check whether any pre-condition checks failed and get users confirmation to continue or not if failures occcured
preConditionFailures=`cat tmp/execution-response.txt | grep -C1 FAIL` || true
if [ -n "${preConditionFailures}" ]
then
    echo "Failures detected in Pre-Condition Check: "
    echo ${preConditionFailures}
    echo
    while read -p "Please confirm whether you still want to continue (y/n):" choice
    do
        case "$choice" in
        y|Y)
            break
            ;;
        n|N)
            echo "Script is stopped by user."
        exit 0
        ;;
        esac
    done
fi


# Has there been a fatal pre-condition failure?  We'll stop the script if so.
preConditionFatalFailures=`cat tmp/execution-response.txt | grep -C1 FATAL` || true
if [ -n "${preConditionFatalFailures}" ]
then
	echo "Fatal failure detected in Pre-Condition Check: "
	echo ${preConditionFailures}
	echo "Script Halted"
	exit 0
fi


echo "Trigger Execution"
curl ${commonParams} -X POST ${api}/builds/${buildId}/executions/${executionId}/trigger  | tee tmp/trigger-response.txt | grep HTTP | ensureCorrectResponse
triggerSuccess=`cat tmp/trigger-response.txt | grep pass` || true # Do not fail on exit here, some reporting first
if [ -z "${triggerSuccess}" ]
then
	echo "Failed to successfully process any packages.  Received response: "
	echo
	cat tmp/trigger-response.txt
	echo
	echo "Script halted after $(getElapsedTime) "
	exit -1
fi

if ${autoPublish}
then
	echo "Publish the package"
	curl ${commonParams} ${api}/builds/${buildId}/executions/${executionId}/output/publish  | grep HTTP | ensureCorrectResponse
fi

downloadFile() {
	read fileName
	echo "Downloading file to: ${localDownloadDirectory}/${fileName}"
	mkdir -p ${localDownloadDirectory}
	# Using curl as the MAC doesn't have wget loaded by default
	curl ${commonParamsSilent} ${downloadUrlRoot}/${fileName} -o "${localDownloadDirectory}/${fileName}"
}

echo "List the output files"
downloadUrlRoot=${api}/builds/${buildId}/executions/${executionId}/packages/${packageId}/outputfiles
localDownloadDirectory=output
curl ${commonParams} ${downloadUrlRoot} | tee tmp/output-file-listing.txt | grep HTTP | ensureCorrectResponse
# Download files
cat tmp/output-file-listing.txt | grep id | while read line ; do echo  $line | sed 's/.*: "\([^"]*\).*".*/\1/g' | downloadFile; done
echo

echo "List the logs"
downloadUrlRoot=${api}/builds/${buildId}/executions/${executionId}/packages/${packageId}/logs
localDownloadDirectory=logs
curl ${commonParams} ${downloadUrlRoot} | tee tmp/log-file-listing.txt | grep HTTP | ensureCorrectResponse
# Download files
cat tmp/log-file-listing.txt | grep id | while read line ; do echo  $line | sed 's/.*: "\([^"]*\).*".*/\1/g' | downloadFile; done

echo
echo "Process Complete in $(getElapsedTime)"
if ! ${autoPublish} 
then
	echo "Run again with the -c flag to just publish the packages, or -a to re-run the whole execution and automatically publish the results."
fi
echo
