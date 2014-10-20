#!/bin/bash
set -e; # Stop on error

# TODO find export files downloaded and convert them to rel2 files
echo "Converting export files to rel2 in directory $1"

cd $1
# ls der* 2>/dev/null | cut -c4- | xargs -I§ mv der§ rel§ || true 
# ls sct* 2>/dev/null | cut -c4- | xargs -I§ mv sct§ rel§ || true 

for file in `ls`; do
  mv $file `echo $file | sed "s/^.../rel/" | sed 's/\(_[^_]*\)$/_INT\1/'`
done

# We would expect the Language file to specify en also
for file in `ls *Language*`; do
  mv $file `echo $file | sed 's/\(Delta\)/Delta-en/'`
done

echo "Removing inferred relationship files"
rm "rel2_Relationship_*"

echo -n "Process Complete. Returning to "
cd -
