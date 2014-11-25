#!/bin/bash
set -e; # Stop on error

# Declare run specific parameters
effectiveDate="2014-07-31"
readmeEndDate="2014"
isFirstTime=true
isWorkbenchDataFixesRequired=true
headless=true
productName="Int GMDN"

# Call api_client
source ../api_client.sh
