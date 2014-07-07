#!/bin/bash
#
# Sets up variables to be consumed by api_client

# Stop on error
set -e;

# Declare run specific parameters
executionName="complex-subsequent" # used to find input-files and name output
manifestFileName="manifest_complex_20140731.xml"
effectiveDate="2014-07-31"
isFirstTime=false
firstTimeStr="false"

#Pass control to our busy worker bee
calling_program=`basename $0`
source api_client.sh
