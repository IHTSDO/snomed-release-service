#!/bin/bash
set -e; # Stop on error

# Declare run specific parameters
effectiveDate="2014-10-31"
readmeEndDate="2014"
isFirstTime=true
isWorkbenchDataFixesRequired=false
headless=true
extensionName="SNOMED CT International Edition"
productName="SNOMED CT Release"
buildName="Int KP Donation Technology Preview"
packageName="KP Donation Technology Preview Release Package"

# Call api_client
source ../api_client.sh
