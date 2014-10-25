#!/bin/bash
set -e; # Stop on error

# Declare run specific parameters
effectiveDate="2014-10-31"
readmeEndDate="2014"
isFirstTime=false
isWorkbenchDataFixesRequired=true
justPackage=false
headless=true
extensionName="SNOMED CT International Edition"
productName="SNOMED CT Release"
buildName="Int Spanish"
packageName="Spanish Release Package"

# Call api_client
source ../api_client.sh
