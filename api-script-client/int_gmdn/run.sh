#!/bin/bash
set -e; # Stop on error

# Declare run specific parameters
effectiveDate="2014-07-31"
readmeEndDate="2014"
isFirstTime=true
isWorkbenchDataFixesRequired=true
headless=true
extensionName="SNOMED CT International Edition"
productName="SNOMED CT Release"
buildName="Int GMDN Build"
packageName="GMDN Release Package"

# Call api_client
source ../api_client.sh
