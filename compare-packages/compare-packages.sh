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
rm -rf target || true
mkdir -p target

if [ $# -lt 4 ]; then
	echo "Usage: compare-packages.sh <Left Name eg SRS> <Left Archive location> <Right Name eg Legacy> <Right Archive or Download Directory> [-normaliseDates]"
	exit 1
fi

leftName=$1
leftArchive=$2
rightName=$3
rightArchive=$4
flags=$5

if [ "${flags}" == "-normaliseDates" ]; then
	normaliseDates=true
	echo "Option set to make effective dates in filenames the same"
fi

leftLocation="target/left_archive"
extractZip ${leftArchive} ${leftLocation}

#Did we in fact get passed two zip files for comparison?  Extract if so
if [[ $rightArchive =~ \.zip$ ]]
then 
	rightDir="target/right_archive" 
	echo "2nd archive detected instead of directory, resolving to ${rightDir}"
	unzip $rightArchive -d ${rightDir}
else
	rightDir=${rightArchive}
fi

#Flatten the left structure into directory "a"
mkdir -p target/a
echo "Moving ${leftName} into flat structure 'a'"
find ${leftLocation} -type f | xargs -I {} mv {} target/a

#Move the right structure into "b"
mkdir -p target/b
echo "Moving ${rightName} into flat structure 'b'"
find ${rightDir} -type f | xargs -I {} mv {} target/b

#If we're normalising the dates, find the date from a file in a and set all the 
#files in b to have the same date
if [ "${normaliseDates}" == true ]; then
	effectiveDate=`ls -1 target/a | head -1 | sed  's/.*\([0-9]\{8\}\).*/\1/'`
	for thisFile in target/b/* ; do
		newFileName=`echo ${thisFile} | sed "s/\([0-9]\{8\}\)/${effectiveDate}/"`
		mv -v ${thisFile} ${newFileName}
	done
fi

createLists ${leftName} "target/a"
createLists ${rightName} "target/b"

echo -e "_File list differences ${leftName} vs ${rightName}_\n"
diff target/${leftName}_file_list.txt target/${rightName}_file_list.txt && echo "None"
echo

mkdir -p target/c
echo "_File content differences_"
echo "Line count diff is ${leftName} minus ${rightName}"
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
		echo "${leftName} line count: $leftFileCount"

		rightFileCount=`wc -l ${rightFile} | awk '{print $1}'`
		echo "${rightName} line count: $rightFileCount"

		echo "Line count diff: $[$leftFileCount-$rightFileCount]"

		echo -n "Content differences count (x2): "
		sort ${leftFile} > tmp.txt
		mv tmp.txt ${leftFile}
		sort ${rightFile} > tmp.txt 
		mv tmp.txt ${rightFile}
		diff ${leftFile} ${rightFile} | tee target/c/diff_${file} | wc -l

		if [[ ${leftFile} == *Refset_* ]] || [[ ${leftFile} == *sct2_Relationship* ]]
		then
			echo -n "Content without id column differences count (x2): "
			leftFileTrim="${leftFile}_no_first_col.txt"
			rightFileTrim="${rightFile}_no_first_col.txt"
			cut -f2- ${leftFile} | sort > ${leftFileTrim}
			cut -f2- ${rightFile} | sort > ${rightFileTrim}
			diff ${leftFileTrim} ${rightFileTrim} | tee target/c/diff_${file}_no_first_col.txt | wc -l
		fi
		echo
	else
		echo "Skipping ${file} - no counterpart in ${rightName}"
		echo
	fi
done
