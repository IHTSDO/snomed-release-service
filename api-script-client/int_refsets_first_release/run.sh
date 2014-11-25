#!/bin/bash
set -e; # Stop on error

# Declare run specific parameters
extensionName="SNOMED CT International Edition"
effectiveDate="2014-01-31"
readmeEndDate="2014"
isFirstTime=true
externalDataLocation="int_refsets_first"
productName="Int Refsets Release"

# Call api_client
source ../api_client.sh
