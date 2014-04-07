#!/bin/bash
cd "$(dirname "$0")"
cd ../
echo "Starting Builder process"
java -jar target/builder.jar > target/builder.log &
