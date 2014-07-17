#!/bin/bash
set -e; # Stop on error

# Declare run specific parameters
effectiveDate="2014-07-31"
readmeEndDate="2014"
isFirstTime=false
extensionName="SNOMED CT International Edition"
externalDataLocation="int_refsets_subsequent"
previousPublishedPackageName="SnomedCT_Release_INT_20140131.zip"
productName="Int Refsets Release"
buildName="Int Refsets Release"

# Call api_client
source ../api_client.sh
