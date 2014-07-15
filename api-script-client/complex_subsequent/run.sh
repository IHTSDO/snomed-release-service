#!/bin/bash
set -e; # Stop on error

# Declare run specific parameters
effectiveDate="2014-07-31"
readmeEndDate="2014"
isFirstTime=false
previousPublishedPackageName="ExampleComplexRefset_Release_INT_20140131.zip"
buildName="ComplexRelease"

# Call api_client
source ../api_client.sh
