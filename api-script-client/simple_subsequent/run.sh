#!/bin/bash
set -e; # Stop on error

# Declare run specific parameters
productName="Simple Refset"
effectiveDate="2014-07-31"
readmeEndDate="2014"
isFirstTime=false
previousPublishedPackageName="ExampleSimpleRefset_Release_INT_20140131.zip"

# Call api_client
source ../api_client.sh
