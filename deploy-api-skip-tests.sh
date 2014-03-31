#!/bin/sh
mvn clean package -Dapple.awt.UIElement='true' -DskipTests=true && cp api/target/*.war vagrant/tomcat-webapps
