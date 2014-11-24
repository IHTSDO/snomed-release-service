#!/bin/bash
set -e; # Stop on error

# Declare run specific parameters
effectiveDate="2014-07-31"
readmeEndDate="2014"
isFirstTime=false
externalDataLocation="test_suite"
previousPublishedPackageName="SnomedCT_Release_INT_20140131.zip"
productName="Test Suite"

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
	buildName="${buildName}${testIndex}"
	
	if [ "${testIndex}" -eq "8" ]
	then
		manifestFile="manifest_20240731.xml"
		effectiveDate="2024-07-31"
	fi
	
	if [[ "${testIndex}" -gt 8 && "${testIndex}" -lt 15 ]] 
	then
		manifestFile="manifest_20250131.xml"
		effectiveDate="2025-01-31"		
		previousPublishedPackageName="Test_Suite_20240731.zip"
		echo "Running Test Tranche Beta"
	fi
	
	if [[ "${testIndex}" -gt 14 && "${testIndex}" -lt 20 ]] 
	then
		manifestFile="manifest_20250731.xml"
		effectiveDate="2025-07-31"		
		previousPublishedPackageName="Test_Suite_20250131.zip"
		echo "Running Test Tranche Gamma"
	fi
	
	# Test 20 using default manifest, previous and effective date
else
	echo "Warn - No parameter -t <integer> to indicate test to run"
fi

source ../api_client.sh
