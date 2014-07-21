#!/bin/bash
set -e; # Stop on error

# Declare run specific parameters
effectiveDate="2014-07-31"
readmeEndDate="2014"
isFirstTime=false
extensionName="SNOMED CT International Edition"
externalDataLocation="test_suite"
previousPublishedPackageName="SnomedCT_Release_INT_20140131.zip"
productName="Snomed CT Release"
buildName="TestSuite"


while getopts "t:" opt
do
	case $opt in
		t) 
			testIndex=$OPTARG
			echo "Running test ${testIndex}."
		;;
	esac
done

if [ -n "${testIndex}" ] && [[ $testIndex =~ ^-?[0-9]+$ ]] 
then
	externalDataLocation="${externalDataLocation}/test_${testIndex}"
	# Call api_client
	source ../api_client.sh
else 
	echo "Please call using parameter -t <integer> to indicate test to run"
	echo "Script halted."
fi
