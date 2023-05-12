#!/bin/bash
set -e

#Switch this off to do comparisons both with and without ids.
optimiseFlag=true

OS=$(uname -s)

leftFile=$1
rightFile=$2
fileName=$3
leftName=$4
rightName=$5

tmpOutput="target/${fileName}_comparison_details_${RANDOM}.txt"

function prepRF1RelFile() {
	thisFile=$1
	targetColumn=5 # Remove any rows with a 'Qualifier' characteristic
	stripValue=1
	#We also need to cut out column 6 because calculating the refinability is out of scope
	tmpFile="tmp_${RANDOM}.txt"
	cat ${thisFile} | awk -F $"\t" -v col="${targetColumn}" -v val="${stripValue}" '$col!=val' | cut -f1-5,7 > ${tmpFile}
	lateralityAttribute="272741003"
	#Copy the laterality qualifying relationships back in
	#AWK is losing our tab separators but only in the Termmed file? Be explicit about Output Field Separator
	cat ${thisFile} | awk  -v col="${targetColumn}" -v val="${stripValue}" -v type="${lateralityAttribute}" \
	-F "\t" 'BEGIN {OFS=FS} $col=val && $3==type' | cut -f1-5,7 >> ${tmpFile}
	mv ${tmpFile} ${thisFile}
}

function prepRF1HistoryFile() {
	thisFile=$1
	#Make it all upper case
	#Also standardise comma space for multiple event reasons
	tmpFile="tmp_${RANDOM}.txt"
	cat ${thisFile} | tr "[a-z]" "[A-Z]" | \
	sed 's/;/,/' | sed 's/,I/, I/' | sed 's/,L/, L/' | \
	sed 's/LANGUAGECODE CHANGE, DESCRIPTIONTYPE CHANGE/DESCRIPTIONTYPE CHANGE, LANGUAGECODE CHANGE/' | \
	sed 's/INITIALCAPITALSTATUS CHANGE, DESCRIPTIONTYPE CHANGE/DESCRIPTIONTYPE CHANGE, INITIALCAPITALSTATUS CHANGE/' > ${tmpFile}
	mv ${tmpFile} ${thisFile}
}

if [ -f "${rightFile}" ] && [ -f "${leftFile}" ]
then 

	echo "Completed Comparison of  ${rightFile}" > ${tmpOutput}
	
	if [[ ${rightFile} == *.txt ]]
	then
		
		#RF1 Relationshipfiles should be compared without Qualifier values, so strip rows where col5 == "1"
		if [[ ${leftFile} == *1_*Relationships* ]]
		then
			prepRF1RelFile "${leftFile}"
			prepRF1RelFile "${rightFile}"
		fi
		
		#RF1 ComponentHistory needs some further normalisation
		if [[ ${leftFile} == *sct1_ComponentHistory* ]]
		then
			prepRF1HistoryFile "${leftFile}"
			prepRF1HistoryFile "${rightFile}"
		fi	
		
		leftFileCount=`wc -l ${leftFile} | awk '{print $1}'`
		echo "${leftName} line count: $leftFileCount" >> ${tmpOutput}

		rightFileCount=`wc -l ${rightFile} | awk '{print $1}'`
		echo "${rightName} line count: $rightFileCount" >> ${tmpOutput}

		echo "Line count diff: $[$leftFileCount-$rightFileCount]" >> ${tmpOutput}
		
		#If RF1 Relationship file, check if right file has sctids.  Do comparison including ids if so
		stripRelIds="true"
		if [[ ${rightFile} == *1*Relationship* ]]
		then
			col1=`head ${rightFile} | tail -1 | cut -f 1`
			if [[ -n "$col1" ]]
			then
				echo "*** Right Relationship file has SCTIDs, including in comparison: ${rightFile}"
				stripRelIds=""
			else
				echo "*** Right Relationship file has no SCTIDs, excluding from comparison: ${rightFile}"
			fi
		fi

		comparisonComplete=false
		if [[ ${leftFile} == *Refset_* || ( ${leftFile} == *Relationship* && -n "$stripRelIds" ) || ${leftFile} == *der1_SubsetMembers* ]]
		then
			echo -n "Content without id column differences count (x2): " >> ${tmpOutput}
			leftFileTrim="${leftFile}_no_first_col.txt"
			rightFileTrim="${rightFile}_no_first_col.txt"
			cut -f2- ${leftFile} | sort > ${leftFileTrim}
			cut -f2- ${rightFile} | sort > ${rightFileTrim}
			diff ${leftFileTrim} ${rightFileTrim} | tee target/c/diff_${fileName}_no_first_col.txt | wc -l >> ${tmpOutput}
			comparisonComplete=true;
		fi
		
		if [ ${comparisonComplete} = false ] || [ ${optimiseFlag} = false ]
		then
			echo -n "Content differences count (x2): " >> ${tmpOutput}
			tmpFile="tmp_${RANDOM}.txt"
			sort ${leftFile} > ${tmpFile}
			mv ${tmpFile} ${leftFile}
			sort ${rightFile} > ${tmpFile} 
			mv ${tmpFile} ${rightFile}
			diff ${leftFile} ${rightFile} | tee target/c/diff_${fileName} | wc -l >> ${tmpOutput}
		fi
		
		if [[ ${rightFile} == *1_Relationship* ]]
		then
			echo -n "Content without group column differences count (x2): " >> ${tmpOutput}
			leftFileTrim2="${leftFile}_no_6_col.txt"
			rightFileTrim2="${rightFile}_no_6_col.txt"
			cut -f1-5 ${leftFile} | sort > ${leftFileTrim2}
			cut -f1-5 ${rightFile} | sort > ${rightFileTrim2}
			diff ${leftFileTrim2} ${rightFileTrim2} | tee target/c/diff_${fileName}_no_6_col.txt | wc -l >> ${tmpOutput}
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
	if [[ $OS == Linux ]]; then
		leftSize=`stat -c%s ${leftFile}`
		rightSize=`stat -c%s ${rightFile}`
	else
		leftSize=`stat -f%z ${leftFile}`
		rightSize=`stat -f%z ${rightFile}`
	fi
	echo "${leftSize} - ${rightSize}" | bc >> ${tmpOutput}
	echo >> ${tmpOutput}
else
	echo "Skipping ${fileName} does not exist or no counterpart found." >> ${tmpOutput}
	echo >> ${tmpOutput}
fi

cat ${tmpOutput}
rm  ${tmpOutput}