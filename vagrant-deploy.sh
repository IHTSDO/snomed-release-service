#!/bin/bash

set -e # Stop on error

# Maven build
#mvn clean install -Dapple.awt.UIElement='true'

# Deploy API
echo "Deploying API"
cp api/target/*.war vagrant/tomcat-webapps

# Deploy (restart new version of) Builder
vagrant ssh -c "nohup sh /vagrant/builder/scripts/restart-builder.sh"

# Web module is mounted so is always instantly deployed

echo "All deployed."
