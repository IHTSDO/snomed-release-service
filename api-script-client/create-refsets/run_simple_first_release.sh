#!/bin/bash
#
# Sets up variables to be consumed by create-refsets

# Stop on error
set -e;

# Declare run specific parameters
executionName="simple-first" # used to find input-files and name output
manifestFileName="manifest_simple_20140131.xml"
effectiveDate="2014-01-31"
isFirstTime=true
firstTimeStr="true"

#Pass control to our busy worker bee
calling_program=`basename $0`
source create-refsets.sh