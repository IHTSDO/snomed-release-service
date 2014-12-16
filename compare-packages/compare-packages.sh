#!/bin/bash
set -e

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

mkdir -p target

if [ $# -ne 2 ]; then
	echo "Usage: compare-packages.sh <SRS Zip Archive location> <Legacy Build or Download Directory>"
	exit 1
fi

srsArchive=$1
legacyData=$2

srsLocation="target/left_archive"
extractZip ${srsArchive} ${srsLocation}

#Did we in fact get passed two zip files for comparison?  Extract if so
if [[ $legacyData =~ \.zip$ ]]
then 
	legacyDir="target/right_archive" 
	echo "2nd archive detected instead of directory, resolving to ${legacyDir}"
	unzip $legacyData -d ${legacyDir}
else
	legacyDir=${legacyData}
fi

#Flatten the SRS structure into directory "a"
mkdir -p target/a
echo "Moving SRS into flat structure 'a'"
find ${srsLocation} -type f | xargs -I {} mv {} target/a

#Move the legacy structure into "b"
mkdir -p target/b
echo "Moving Legacy Build into flat structure 'b'"
find ${legacyDir} -type f | xargs -I {} mv {} target/b

createLists "srs" "target/a"
createLists "legacy" "target/b"

echo -e "_File list differences SRS vs Legacy_\n"
diff target/srs_file_list.txt target/legacy_file_list.txt && echo "None"
echo

mkdir -p target/c
echo "_File content differences_"
echo "Line count diff is SRS minus WBRP"
echo
processOrderFile="_process_order.txt"
find target/a -type f | sed "s/target\/a\///" | grep "sct2_" | sort > ${processOrderFile}
find target/a -type f | sed "s/target\/a\///" | grep "der2_" | sort >> ${processOrderFile}
#Now also compare RF1 Files
find target/a -type f | sed "s/target\/a\///" | grep "sct1_" | sort >> ${processOrderFile}
find target/a -type f | sed "s/target\/a\///" | grep "der1_" | sort >> ${processOrderFile}
find target/a -type f | sed "s/target\/a\///" | grep "res1_" | sort >> ${processOrderFile}
for file in `cat ${processOrderFile}`; do
	leftFile="target/a/${file}"
	rightFile="target/b/${file}"
	if [ -f "${rightFile}" ]
	then 
		echo "Comparing ${file}"

		leftFileCount=`wc -l ${leftFile} | awk '{print $1}'`
		echo "SRS line count: $leftFileCount"

		rightFileCount=`wc -l ${rightFile} | awk '{print $1}'`
		echo "WBRP line count: $rightFileCount"

		echo "Line count diff: $[$leftFileCount-$rightFileCount]"

		sort ${leftFile} > tmp.txt
		mv tmp.txt ${leftFile}
		sort ${rightFile} > tmp.txt 
		mv tmp.txt ${rightFile}
		echo -n "Content differences count (x2): "
		diff ${leftFile} ${rightFile} | tee target/c/diff_${file} | wc -l

		if [[ ${leftFile} == *Refset_* ]]
		then
			leftFileTrim="${leftFile}_no_first_col.txt"
			rightFileTrim="${rightFile}_no_first_col.txt"
			cut -f2- ${leftFile} | sort > ${leftFileTrim}
			cut -f2- ${rightFile} | sort > ${rightFileTrim}
			echo -n "Content without id column differences count (x2): "
			diff ${leftFileTrim} ${rightFileTrim} | tee target/c/diff_${file}_no_first_col.txt | wc -l
		fi
		echo
	else
		echo "Skipping ${file} - no counterpart in the legacy build"
		echo
	fi
done
