#!/bin/bash
#
# Sets up variables to be consumed by api_client

# Stop on error
set -e;

# Declare run specific parameters
executionName="complex-first" # used to find input-files and name output
manifestFileName="manifest_complex_20140131.xml"
effectiveDate="2014-01-31"
isFirstTime=true
firstTimeStr="true"

#Pass control to our busy worker bee
calling_program=`basename $0`
source api_client.sh
