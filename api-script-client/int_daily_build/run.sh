#!/bin/bash
set -e; # Stop on error

# Command line args
dataLocation=$1
host=$2
if [ -n "$host" ]; then
	clientOpts="-h $host"
else
	clientOpts=""
fi

# Declare run specific parameters
effectiveDate="2015-01-31"
readmeEndDate="2014"
isFirstTime=false
isWorkbenchDataFixesRequired=true
headless=true
#createInferredRelationships=true
previousPublishedPackageName="SnomedCT_Release_INT_20140731.zip"
productName="SNOMED CT Release"

echo "Configuration set to pull export files from $1"

# Call api_client
source ../api_client.sh $clientOpts
