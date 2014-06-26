#!/bin/bash
#
# Sets up variables to be consumed by create-simple-refset

# Stop on error
set -e;

# Declare run specific parameters
inputFilesDir="first-input-files"
unwantedInputFilesDir="subsequent-input-files"
manifestFileName="manifest_20140131.xml"
effectiveDate="2014-01-31"
isFirstTime=true
firstTimeStr="true"

#Pass control to our busy worker bee
calling_program=`basename $0`
source create-simple-refset.sh