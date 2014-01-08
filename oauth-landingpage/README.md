OAuth 2.0 Landing Page
======================

This project features an out-of-the-box Tomcat version, to aid local debugging.

The project should first be built using:

```
mvn clean install
```

and then can by tested using a disposable tomcat installation using

```
mvn tomcat7:run
```

And (assuming you've set up your host file to point local.snomedtools.org to 127.0.0.1), accessed in your local browser via

http://local.snomedtools.org:8081/oauth-landingpage/hello.html

Note: There is also a version of the page served at /hello (Servlet based), but I couldn't get the Google javascript API to render my button properly, although clicking on the Div did work.  I'm parking that for the moment on my TODO list.

The built in Tomcat can be run on a different port by changing the port number in the tomcat7-maven-plugin section of pom.xml

```xml
<configuration>
					<port>8081</port>
```
