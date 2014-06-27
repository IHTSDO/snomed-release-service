#!/bin/bash
#
# Sets up variables to be consumed by create-simple-refset

# Stop on error
set -e;

# Declare run specific parameters
inputFilesDir="simple-subsequent"
manifestFileName="manifest_simple_20140731.xml"
effectiveDate="2014-07-31"
isFirstTime=false
firstTimeStr="false"

#Pass control to our busy worker bee
calling_program=`basename $0`
source create-refsets.sh