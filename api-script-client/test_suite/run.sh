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


while getopts ":t:" opt
do
	case $opt in
		t)
			testIndex=$OPTARG
		;;
	esac
done

if [ -n "${testIndex}" ] && [[ $testIndex =~ ^-?[0-9]+$ ]]
then
	echo "Running test ${testIndex}."
	externalDataLocation="${externalDataLocation}/test_${testIndex}"
else
	echo "Warn - No parameter -t <integer> to indicate test to run"
	echo
	# Reset getopts
	OPTIND=1
fi

source ../api_client.sh
