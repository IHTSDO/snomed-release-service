#!/usr/bin/env bash

# Restore package cache
mv /var/cache/apt/archives /var/cache/apt/archives.old
ln -s /vagrant/vagrant/cache/ubuntu-packages /var/cache/apt/archives
mkdir -p /root/.m2
ln -s /vagrant/vagrant/cache/maven /root/.m2/repository

# Install packages
apt-get update
apt-get install -y nginx openjdk-7-jdk maven tomcat7 curl vim

# Tomcat config
service tomcat7 stop
cp /etc/tomcat7/server.xml /etc/tomcat7/server.xml.org
sed -i 's/<Connector port="8080" /<Connector port="8081" /' /etc/tomcat7/server.xml
rm -rf /etc/default/tomcat7
ln -s /vagrant/vagrant/tomcat7.conf /etc/default/tomcat7

# Remove JDK6
apt-get remove -y openjdk-6-* # Makes JDK7 default
apt-get autoremove # Remove some unneeded icedtea packages

# Nginx config
rm -rf /etc/nginx/sites-available/default
ln -s /vagrant/vagrant/nginx-site.conf /etc/nginx/sites-available/default

# Disable firewall on Ubuntu guest
ufw disable

# Build API
cd /vagrant
mvn clean install -DskipTests=true

# Deploy API
cp api/target/api.war /var/lib/tomcat7/webapps/

# Start Tomcat and Nginx
service tomcat7 start
service nginx start
