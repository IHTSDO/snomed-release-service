#!/bin/bash

# Stop on error
set -e;
#set -x;  #Debug on

workDir="/tmp/srs_release_comparison"

function checkFile() {
	file=$1
	if [ ! -e $file ]
	then
		echo -e "$file not found\nScript halted"
		exit -1
	fi
	
	if [[ ! $file =~ \.zip$ ]]
	then 
		echo -e "$file not an archive file.\nScript halted"
		exit -1
	fi
}

function getReleaseDate() {
	releaseDate=`echo $1 | sed 's/[^0-9]//g'`
	if [ -z $releaseDate ] 
	then
		echo "Failed to find release date in $1.\nScript halting"
		exit -1
	fi
	echo $releaseDate
} 

function createFileList {
	listName=$1
	dir=$2
	home=`pwd`
	cd $dir
	#We're comparing two different release dates so we need to strip out any numbers
	#Also strip out any leading x's.  We'll compare regardless
	find . -type f | sed -e 's/..\(.*\)\([0-9]\{8\}\).*/\1/' | sed 's/^x\(.*\)/\1/'  > ../c/${listName}_file_list.txt
	cd - > /dev/null
}

function sortIfNeeded() {
	original=$1
	sorted=$2
	if [ ! -e ${sorted} ] 
	then
		`sort ${original} > ${sorted}`
	fi
}

function ensureXinY () {
	subset=$1
	subset_sorted="${subset}_sorted"
	superset=$2
	superset_sorted="${superset}_sorted"
	#Sort the two files if a sorted version does not already exist
	sortIfNeeded ${subset} ${subset_sorted}
	echo -n "."
	sortIfNeeded ${superset} ${superset_sorted}
	echo -n "."
	
	#The number of lines common to both files should be equal to the size of the subset
	#Note that the header line will also match, so OK to use line count unmodified
	subsetLineCount=`wc -l ${subset_sorted} | awk {'print $1'}`
	commonLineCount=`comm -12 ${subset_sorted} ${superset_sorted} | wc -l | awk {'print $1'}`
	
	if (( subsetLineCount  !=  commonLineCount ))
	then
		echo -e "\nWarning - ${subset} (${subsetLineCount} lines) is not fully contained in superset ${superset} - ${commonLineCount} lines in common."
		subsetCheckMessage="FAIL: One or more subset files was not fully contained in its superset."
	fi
}

#Check we've been passed current (old) and prospective (new) releases 
currentRelease=$1
prospectiveRelease=$2
if [[ -z ${currentRelease} || -z ${prospectiveRelease} ]] 
then
	echo
	echo -e "Usage:  release-comparison.sh <current_release_location> <prospective_release_location> \nScript halted."
	exit -1
fi

#Check files exist
checkFile ${currentRelease}
checkFile ${prospectiveRelease}

#Recover their release dates
currReleaseDate=`getReleaseDate ${currentRelease}`
prosReleaseDate=`getReleaseDate ${prospectiveRelease}`

#Ensure current is before prospective
if (( currReleaseDate > prosReleaseDate ))
then
	echo -e "Expecting the prospective release date ${prosReleaseDate} to be after the current release date ${currReleaseDate}.\nScript halted"
	exit -1
fi

#Create our working directory
mkdir -p ${workDir}

#And clean it out in case we've done a previous run
rm -rf ${workDir}/*

#Extract files
echo
echo -n "Extracting files to working directory: ${workDir} and into flat structure (see a_flat, b_flat)"
unzip ${currentRelease} -d ${workDir}/a > /dev/null
echo -n "."
unzip ${prospectiveRelease} -d ${workDir}/b > /dev/null
echo -n "."
cd ${workDir}
mkdir {a_flat,b_flat,c}

#echo "Flattening structure..."
cd a
find . -type f | xargs -I {} mv {} ../a_flat
echo -n "."

cd ../b
find . -type f | xargs -I {} mv {} ../b_flat
cd ..
echo "."

createFileList "current" a_flat
createFileList "prospective" b_flat

echo -e "*** File list differences ${currReleaseDate} vs ${prosReleaseDate} ***\n"
diff c/current_file_list.txt c/prospective_file_list.txt && echo "None"
echo

#Now check that all prospective full files are larger than all current full files
message="PASS: All full files larger than previous full files."
for file in `cat c/current_file_list.txt | grep Full`; do
	currFile=`ls a_flat/*${file}* 2> /dev/null || true`
	prosFile=`ls b_flat/*${file}* 2> /dev/null || true`
	#echo "Comparing ${currFile} to ${prosFile}"
	#if either file is missing, skip - will already have been reported as a discrepancy
	if [[ ! -e $currFile  || ! -e $prosFile ]]
	then
		continue
	fi
	
	currLineCount=`wc -l ${currFile} | awk {'print $1'}`
	prosLineCount=`wc -l ${prosFile} | awk {'print $1'}`
	
	if (( ! prosLineCount > currLineCount ))
	then
		message="FAIL: One or more full files was not larger than it's previous version"
		echo -e "Warning - ${file}: ${currLineCount} lines in ${currReleaseDate} compared to ${prosLineCount} in ${prosReleaseDate}"
	fi	
done
echo ${message}

#Now check that line count of current full + prospective delta = prospective full
message="PASS: All prospective full files are the size of the previous full plus the prospective delta"
for file in `cat c/current_file_list.txt | grep Full`; do
	currFullFile=`ls a_flat/*${file}* 2> /dev/null || true`
	prosFullFile=`ls b_flat/*${file}* 2> /dev/null|| true`
	prosDeltaFile=`echo ${prosFullFile} | sed 's/Full/Delta/'`
	
	#if either file is missing, skip - will already have been reported as a discrepancy
	if [[ ! -e $currFullFile  || ! -e $prosFullFile ]]
	then
		continue
	fi
	
	#echo "Comparing ${currFullFile} plus ${prosDeltaFile} equals ${prosFullFile}"
	currFullLineCount=`wc -l ${currFullFile} | awk {'print $1'}`
	prosFullLineCount=`wc -l ${prosFullFile} | awk {'print $1'}`
	
	#It is acceptable for the delta file to be missing eg zres2_icRefset_OrderedTypeDelete_INT_20110731.txt
	if [ -e ${prosDeltaFile} ]
	then
		prosDeltaLineCount=`wc -l ${prosDeltaFile} | awk {'print $1'}`
	else 
		echo "Warning - ${prosDeltaFile} is not present.  Assuming size to be 0 + header = 1."
		prosDeltaLineCount=1
	fi
	
	#Subtract 1 from the delta line count because we don't add the header line again
	if (( currFullLineCount + prosDeltaLineCount -1 !=  prosFullLineCount))
	then
		message="FAIL: One or more prospective full files was not the sum of the previous full plus the prospective delta"
		echo -e "Warning - ${file}: ${currFullLineCount} + ${prosDeltaLineCount} -1 DOES NOT EQUAL  ${prosFullLineCount}"
	else
		echo "${file}: ${currFullLineCount} + ${prosDeltaLineCount} -1 EQUALS  ${prosFullLineCount}"
	fi	
done
echo ${message}

#Now check that all delta files are contained in the full and snapshot, and that the snapshot is contained in the full
subsetCheckMessage="PASS: All files are contained in their respective supersets"
echo
for file in `cat c/prospective_file_list.txt | grep Full`; do
	echo -n "${file} "
	prosFullFile=`ls b_flat/*${file}* 2> /dev/null || true`
	
	#if  file is missing, skip - will already have been reported as a discrepancy
	if [[ ! -e $prosFullFile ]]
	then
		continue
	fi
	prosSnapFile=`echo ${prosFullFile} | sed 's/Full/Snapshot/'`
	prosDeltaFile=`echo ${prosFullFile} | sed 's/Full/Delta/'`
	fullLineCount=`wc -l ${prosFullFile} | awk {'print $1'}`
	echo -n "Subset checking: ${file} (${fullLineCount}) "	
	ensureXinY ${prosSnapFile} ${prosFullFile}
	echo -n "."
	
	if [ -e ${prosDeltaFile} ]
	then
		ensureXinY ${prosDeltaFile} ${prosFullFile}
		echo -n "."
		ensureXinY ${prosDeltaFile} ${prosSnapFile}
		echo "."
	else 
		echo "Warning - ${prosDeltaFile} is not present.  Cannot check it exists in supersets"
	fi
done
echo ${subsetCheckMessage}

echo
echo "Comparison Complete."
