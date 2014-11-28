#!/bin/bash

# Stop on error
set -e;

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
	find . -type f | sed -e 's/..\(.*\)\([0-9]\{8\}\).*/\1/'  > ../c/${listName}_file_list.txt
	cd - > /dev/null
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
#rm -rf ${workDir}/*
#
#Copy our two files in there and extract
#echo "Copying files to working directory: ${workDir} and extracting..."
#cp  ${currentRelease} ${workDir}
#cp  ${prospectiveRelease} ${workDir}
cd ${workDir}
#unzip ${currentRelease} -d a > /dev/null
#unzip ${prospectiveRelease} -d b > /dev/null
#mkdir {a_flat,b_flat,c}
#
#echo "Flattening structure..."
#cd a
#find . -type f | xargs -I {} cp {} ../a_flat
#cd ../b
#find . -type f | xargs -I {} cp {} ../b_flat
#cd ..
#
#createFileList "current" a_flat
#createFileList "prospective" b_flat

echo -e "*** File list differences ${currReleaseDate} vs ${prosReleaseDate} ***\n"
diff c/current_file_list.txt c/prospective_file_list.txt && echo "None"
echo

#Now check that all prospective full files are larger than all current full files
for file in `cat c/current_file_list.txt | grep Full`; do
	currFile=`ls a_flat/${file}*`
	prosFile=`ls b_flat/${file}*`
	#echo "Comparing ${currFile} to ${prosFile}"
	currLineCount=`wc -l ${currFile} | awk {'print $1'}`
	prosLineCount=`wc -l ${prosFile} | awk {'print $1'}`
	if (( ! prosLineCount > currLineCount ))
	then
		echo -e "Warning - ${file}: ${currLineCount} lines in ${currReleaseDate} compared to ${prosLineCount} in ${prosReleaseDate}"
	fi	
	
done

echo
echo "Comparison Complete."
