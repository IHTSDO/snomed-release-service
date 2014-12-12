#!/bin/bash
set -e; # Stop on error

# Declare run specific parameters
productName="Core"
effectiveDate="2015-01-31"
readmeEndDate="2014"
isFirstTime=true
createInferredRelationships=true
createLegacyIds=true
# Call api_client
source ../api_client.sh
