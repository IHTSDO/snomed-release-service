#!/bin/bash

set -e

./compare-packages.sh $1 $2  | tee /tmp/daily_comparison.txt
uuencode /tmp/daily_comparison.txt daily_comparison.txt | mail -s "Output of Daily Comparison" pwi@ihtsdo.org