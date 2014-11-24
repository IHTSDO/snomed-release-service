#!/bin/bash
set -e; # Stop on error

# Declare run specific parameters
effectiveDate="2014-09-26"
readmeEndDate="2014"
isFirstTime=true
isWorkbenchDataFixesRequired=false
justPackage=true
headless=true
productName="Int_LOINC"

# Call api_client
source ../api_client.sh
