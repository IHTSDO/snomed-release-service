Release Portal
======================

This project features an out-of-the-box Tomcat version, to aid local debugging.

The project can by tested using a disposable jetty installation using

```
mvn clean -D jetty.port=8081 jetty:run
```

And (assuming you've set up your host file to point local.snomedtools.org to 127.0.0.1), accessed in your local browser via

http://local.snomedtools.org:8081

Note: That has not - so far - proved possible to specify the jetty connector port as a configuration item in the POM as per the documentation: http://www.eclipse.org/jetty/documentation/current/jetty-maven-plugin.html