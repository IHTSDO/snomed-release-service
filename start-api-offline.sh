#!/bin/bash
set -e
echo 'Building API webapp (skipping tests)..'
sleep 1
mvn clean install -Dapple.awt.UIElement='true' -DskipTests=true
echo
echo "Starting API webapp in offline mode.."
echo
sleep 1
java -jar api/target/exec-api.jar
