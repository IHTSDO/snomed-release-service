#!/bin/bash
set -e; # Stop on error

# Declare run specific parameters
effectiveDate="2014-09-30"
readmeEndDate="2014"
isFirstTime=true
isWorkbenchDataFixesRequired=true
headless=true
productName="Int GPFP"

# Call api_client
source ../api_client.sh
