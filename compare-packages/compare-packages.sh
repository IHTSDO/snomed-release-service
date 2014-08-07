#!/bin/bash
set -e

if [ $# -ne 2 ]; then
	echo "Usage: compare-packages.sh <SRS Zip Archive location> <Legacy Build Download Directory>"
	exit 1
fi

srsArchive=$1
legacyDir=$2

function extractZip {
	zipName=$1
	dirName=$2
	echo "Extracting $zipName to $dirName"
	mkdir -p $dirName
	unzip $zipName -d $dirName
}

function createLists {
	listName=$1
	dir=$2
	find $dir -type d > target/${listName}_dir_list.txt
	find $dir -type f > target/${listName}_file_list.txt
}

function normaliseLegacyStructure {
	targetDir=$1
	mkdir -p $targetDir
	
	# Copy files from their download location to the appropriate location
	moveLegacyFiles "Delta" $targetDir
	moveLegacyFiles "Snapshot"	$targetDir
	moveLegacyFiles "Full"	$targetDir
}

function moveLegacyFiles {
	fileType=$1
	targetDir=$2	
	
	echo "Moving ${fileType} legacy files from ${legacyDir} to ${targetDir}"
	# First move the content files
	thisTarget="${targetDir}/RF2Release/${fileType}/Terminology/"
	mkdir -p $thisTarget
	mv -v ${legacyDir}/${fileType}/sct2*  ${thisTarget}

	#Next the Map, Language and the content Refsets
	thisTarget="${targetDir}/RF2Release/${fileType}/Refset/Language/"
	mkdir -p $thisTarget
	mv -v ${legacyDir}/${fileType}/der2*Language* $thisTarget
	
	thisTarget="${targetDir}/RF2Release/${fileType}/Refset/Map/"
	mkdir -p $thisTarget
	mv -v ${legacyDir}/${fileType}/der2*SimpleMap* $thisTarget
	
	thisTarget="${targetDir}/RF2Release/${fileType}/Refset/Content/"
	mkdir -p $thisTarget
	mv -v ${legacyDir}/${fileType}/der2* $thisTarget
}

extractZip $srsArchive "target/a"
normaliseLegacyStructure "target/b"
echo

createLists "srs" "target/a/SRS_Daily_Build_20150131/RF2Release/"
createLists "legacy" "target/b/RF2Release/"

echo -e "_Directory list differences_\n"
diff target/srs_dir_list.txt target/legacy_dir_list.txt || echo
echo

echo -e "_File list differences_\n"
diff target/srs_file_list.txt target/legacy_file_list.txt
echo

#echo -e "_File content differences_\n"
#for file in `find a/extract -type f | sed 's/a\///'`; do
#	echo "File" $file
#	sort a/$file > tmp.txt && mv tmp.txt a/$file
#	sort b/$file > tmp.txt && mv tmp.txt b/$file
#	diff a/$file b/$file && echo "None"
#	echo
#done
