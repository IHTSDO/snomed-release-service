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
	home=`pwd`
	cd $dir
	find . -type f > ${home}/target/${listName}_file_list.txt
	cd - > /dev/null
}


extractZip $srsArchive "target"

srsLocation="target/SRS_Daily_Build_20150131"
legacyLocation="${legacyDir}/destination"

#Flatten the SRS structure into directory "a"
mkdir -p target/a
echo "Moving SRS into flat structure 'a'"
find ${srsLocation} -type f | xargs -I {} mv {} target/a

#Move the legacy structure into "b"
mkdir -p target/b
echo "Moving Legacy Build into flat structure 'b'"
find ${legacyLocation} -type f | xargs -I {} mv {} target/b

createLists "srs" "target/a"
createLists "legacy" "target/b"

echo -e "_File list differences SRS vs Legacy_\n"
diff target/srs_file_list.txt target/legacy_file_list.txt && echo "None"
echo

mkdir -p target/c
echo "_File content differences_"
echo "Line count diff is SRS minus WBRP"
echo
for file in `find target/a -type f | sed "s/target\/a\///"`; do
	leftFile="target/a/${file}"
	rightFile="target/b/${file}"
	if [ -f "${rightFile}" ]
	then 
		echo "Comparing ${file}"

		leftFileCount=`wc -l ${leftFile}`
		echo "SRS line count: $leftFileCount"

		rightFileCount=`wc -l ${rightFile}`
		echo "WBRP line count: $rightFileCount"

		echo "Line count diff: $[$leftFileCount-$rightFileCount]"

		sort ${leftFile} > tmp.txt
		mv tmp.txt ${leftFile}
		sort ${rightFile} > tmp.txt 
		mv tmp.txt ${rightFile}
		echo -n "Content differences count (x2): "
		diff ${leftFile} ${rightFile} | tee target/c/diff_${file} | wc -l

		echo
	else
		echo "Skipping ${file} - no counterpart in the legacy build"
		echo
	fi
done
