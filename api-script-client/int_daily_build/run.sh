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
headless=true
extensionName="SNOMED CT International Edition"
previousPublishedPackageName="SnomedCT_Release_INT_20140731.zip"
productName="SNOMED CT Release"
buildName="Int Daily Build"
packageName="Snomed Release Package"

echo "Configuration set to pull export files from $1"

# Call api_client
source ../api_client.sh $clientOpts
