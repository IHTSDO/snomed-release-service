SNOMED Release Service
======================

Cloud based 2.0 release service for SNOMED CT.

Development Environment under Vagrant
-------------------------------------
Install Vagrant: http://www.vagrantup.com/downloads

Install VirtualBox: https://www.virtualbox.org/wiki/Downloads

`cd snomed-release-service`

`vagrant up`

This will create a Ubuntu server and install the required packages.
The API will be built and deployed under Tomcat. The web module will be hosted under Nginx.

The release service will be available on your local machine here: http://localhost:8081/

Any HTML changes you make to the web module will be available instantly.
