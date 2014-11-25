#!/bin/bash
set -e; # Stop on error

# Declare run specific parameters
effectiveDate="2014-10-31"
readmeEndDate="2014"
isFirstTime=false
isWorkbenchDataFixesRequired=true
externalDataLocation="int_spanish_process"
justPackage=false
headless=true
productName="Int Spanish Release"
previousPublishedPackageName="SnomedCT_Release-es_INT_20140430.zip"

# Call api_client
source ../api_client.sh
