SNOMED Release Service
======================

Cloud based release service for SNOMED CT

Development Environment
-----------------------
Build the project using maven: 

`mvn clean install`

Start the application using the standalone executable jar which includes an embedded tomcat:

`java -Xms512m -Xmx4g -jar target/snomed-release-service-*.jar --server.port=8081 --server.servlet.contextPath=/api`
