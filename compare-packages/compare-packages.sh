#!/bin/bash

set -e

if [ $# -ne 2 ]; then
	echo "Usage: Give two zip file paths."
	exit 1
fi

packageA=$1
packageB=$2

ex="extract"

function extractZip {
	zipName=$1
	dirName=$2
	echo "Extracting $zipName to $dirName"
	rm -rf $dirName
	extractDir="$dirName/$ex"
	mkdir -p $extractDir
	unzip -q $zipName -d $extractDir
}

function createLists {
	dir=$1
	cd $dir/$ex
	find . -type d > ../dir_list.txt
	find . -type f > ../file_list.txt
	cd - > /dev/null
}

extractZip $packageA "a"
extractZip $packageB "b"
echo

createLists "a"
createLists "b"

echo -e "_Directory list differences_\n"
diff a/dir_list.txt b/dir_list.txt || echo
echo

echo -e "_File list differences_\n"
diff a/file_list.txt b/file_list.txt
echo

echo -e "_File content differences_\n"
for file in `find a/extract -type f | sed 's/a\///'`; do
	echo "File" $file
	sort a/$file > tmp.txt && mv tmp.txt a/$file
	sort b/$file > tmp.txt && mv tmp.txt b/$file
	diff a/$file b/$file && echo "None"
	echo
done
