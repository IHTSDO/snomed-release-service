#!/bin/bash
set -e; # Stop on error

# Declare run specific parameters
effectiveDate="2014-09-26"
readmeEndDate="2014"
isFirstTime=true
isWorkbenchDataFixesRequired=false
justPackage=true
headless=true
extensionName="SNOMED CT International Edition"
productName="SNOMED CT Release"
buildName="Int_LOINC"
packageName="LOINC Technology Preview Release Package"

# Call api_client
source ../api_client.sh
