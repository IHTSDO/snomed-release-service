#!/bin/bash
set -e; # Stop on error

# Declare run specific parameters
extensionName="Examples"
productName="Complex Refset"
effectiveDate="2014-01-31"
readmeEndDate="2014"
isFirstTime=true

# Call api_client
source ../api_client.sh
