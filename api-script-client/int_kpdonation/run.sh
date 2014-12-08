#!/bin/bash
set -e; # Stop on error

# Declare run specific parameters
effectiveDate="2014-10-31"
readmeEndDate="2014"
isFirstTime=true
isWorkbenchDataFixesRequired=false
headless=true
productName="Int KP Donation Technology Preview"

# Call api_client
source ../api_client.sh
