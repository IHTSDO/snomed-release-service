Web Front End
======================

This project features an out-of-the-box Tomcat version, to aid local debugging.

The project can by tested using a disposable jetty installation using

```
mvn clean -D jetty.port=8081 jetty:run
```

And (assuming you've set up your host file to point local.snomedtools.org to 127.0.0.1), accessed in your local browser via

http://local.snomedtools.org:8081

Note: That has not - so far - proved possible to specify the jetty connector port as a configuration item in the POM as per the documentation: http://www.eclipse.org/jetty/documentation/current/jetty-maven-plugin.html

TopTip: Ensure the Jetty Plugin is stopped before running a git pull because it locks the files it's serving something nasty.

Server Installation
-------------------

We will be serving the webapp, using the following configuration:

* Git working directory in /home/www-data
* Nginx configuration /etc/nginx/sites-available/<servername.domain-name>  root /var/www/release
* Symlinked ln -s /home/www-data/snomed-release-service/web /var/www/release
* Start ssh agent to deliver ssh key:   eval "$(ssh-agent)"
* ssh-add /home/www-data/.ssh/id_pub
* Testing using  ssh -T git@github.com (note that username is always "git" for github)
* git clone git@github.com:IHTSDO/snomed-release-service.git
* Callable script to update the code (performs a git pull over ssh): http://release.snomedtools.org/cgi-bin/deploy.sh
* GitHub hook configured to automatically call deploy script when repository receives an update: https://github.com/IHTSDO/snomed-release-service/settings/hooks
