#!/bin/bash
set -e

leftFile=$1
rightFile=$2
fileName=$3
leftName=$4
rightName=$5

tmpOutput="target/${fileName}_comparison_details_${RANDOM}.txt"

echo "Started Comparison of ${rightFile}"

function prepRF1RelFile() {
	thisFile=$1
	targetColumn=5 # Remove any rows with a 'Qualifier' characteristic
	stripValue=1
	#We also need to cut out column 6 because calculating the refinability is out of scope
	tmpFile="tmp_${RANDOM}.txt"
	cat ${thisFile} | awk -v col="${targetColumn}" -v val="${stripValue}" '$col!=val' | cut -f1-5,7 > ${tmpFile}
	mv ${tmpFile} ${thisFile}
}

if [ -f "${rightFile}" ] && [ -f "${leftFile}" ]
then 

	echo "Completed Comparison of  ${rightFile}" > ${tmpOutput}
	
	if [[ ${rightFile} == *.txt ]]
	then
		
		#RF1 Relationshipfiles should be compared without Qualifier values, so strip rows where col5 == "1"
		if [[ ${leftFile} == *sct1_Relationships* ]]
		then
			prepRF1RelFile "${leftFile}" 5 1
			prepRF1RelFile "${rightFile}" 5 1
		fi		
		
		leftFileCount=`wc -l ${leftFile} | awk '{print $1}'`
		echo "${leftName} line count: $leftFileCount" >> ${tmpOutput}

		rightFileCount=`wc -l ${rightFile} | awk '{print $1}'`
		echo "${rightName} line count: $rightFileCount" >> ${tmpOutput}

		echo "Line count diff: $[$leftFileCount-$rightFileCount]" >> ${tmpOutput}

		echo -n "Content differences count (x2): " >> ${tmpOutput}
		tmpFile="tmp_${RANDOM}.txt"
		sort ${leftFile} > ${tmpFile}
		mv ${tmpFile} ${leftFile}
		sort ${rightFile} > ${tmpFile} 
		mv ${tmpFile} ${rightFile}
		diff ${leftFile} ${rightFile} | tee target/c/diff_${fileName} | wc -l >> ${tmpOutput}

		if [[ ${leftFile} == *Refset_* ]] || [[ ${leftFile} == *sct2_Relationship* ]] || [[ ${leftFile} == *sct1_Relationships* ]] || [[ ${leftFile} == *der1_SubsetMembers* ]]
		then
			
			echo -n "Content without id column differences count (x2): " >> ${tmpOutput}
			leftFileTrim="${leftFile}_no_first_col.txt"
			rightFileTrim="${rightFile}_no_first_col.txt"
			cut -f2- ${leftFile} | sort > ${leftFileTrim}
			cut -f2- ${rightFile} | sort > ${rightFileTrim}
			diff ${leftFileTrim} ${rightFileTrim} | tee target/c/diff_${fileName}_no_first_col.txt | wc -l >> ${tmpOutput}
		fi
		
		if [[ ${leftFile} == *sct2_Relationship* ]]
		then
			echo -n "Content without id or group column differences count (x2): " >> ${tmpOutput}
			leftFileTrim2="${leftFile}_no_1_7_col.txt"
			rightFileTrim2="${rightFile}_no_1_7_col.txt"
			#Ideally I'd use cut's --complement here but it doesn't exist for mac
			cut -f2,3,4,5,6,8,9,10 ${leftFile} | sort > ${leftFileTrim2}
			cut -f2,3,4,5,6,8,9,10 ${rightFile} | sort > ${rightFileTrim2}
			diff ${leftFileTrim2} ${rightFileTrim2} | tee target/c/diff_${fileName}_no_1_7_col.txt | wc -l >> ${tmpOutput}
		fi
	fi
	
	echo -n "File size difference (bytes): " >> ${tmpOutput}
	leftSize=`stat -f%z ${leftFile}`
	rightSize=`stat -f%z ${rightFile}`
	echo "${leftSize} - ${rightSize}" | bc >> ${tmpOutput}
	echo >> ${tmpOutput}
else
	echo "Skipping ${fileName} does not exist or no counterpart found." >> ${tmpOutput}
	echo >> ${tmpOutput}
fi

cat ${tmpOutput}
rm  ${tmpOutput}