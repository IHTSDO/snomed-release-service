#!/bin/bash
set -e; # Stop on error

# Declare run specific parameters
effectiveDate="2015-01-31"
readmeEndDate="2014"
isFirstTime=false
headless=true
extensionName="SNOMED CT International Edition"
dataLocation=$1
previousPublishedPackageName="SnomedCT_Release_INT_20140731.zip"
productName="SNOMED CT Release"
buildName="Int Daily Build"

echo "Configuration set to pull export files from $1"

# Call api_client
source ../api_client.sh
