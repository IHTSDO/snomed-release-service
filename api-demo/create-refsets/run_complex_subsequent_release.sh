#!/bin/bash
#
# Sets up variables to be consumed by create-complex-refset

# Stop on error
set -e;

# Declare run specific parameters
inputFilesDir="complex-subsequent"
manifestFileName="manifest_complex_20140731.xml"
effectiveDate="2014-07-31"
isFirstTime=false
firstTimeStr="false"

#Pass control to our busy worker bee
calling_program=`basename $0`
source create-refsets.sh