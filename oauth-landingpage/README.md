OAuth 2.0 Landing Page
======================

This project features an out-of-the-box Tomcat version, to aid local debugging.   It can be run on a different port (to avoid conflict with an existing Tomcat installation) by changing the port number in the tomcat7-maven-plugin section of pom.xml

```xml
<configuration>
					<port>8080</port>
```	

The project should first be built using:

```
mvn clean install
```

and then can by tested using a disposable tomcat installation using

```
mvn tomcat7:run
```