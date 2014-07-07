#!/bin/bash
set -e; # Stop on error

# Declare run specific parameters
effectiveDate="2014-07-31"
isFirstTime=false

# Call api_client
source ../api_client.sh
