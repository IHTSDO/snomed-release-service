SNOMED Release Service
======================

Cloud based 2.0 release service for SNOMED CT.

Development Environment
-----------------------
Build the project using maven: 

`mvn clean install`

Start the application using the standalone executable jar which includes an embedded tomcat:

`java -Xms512m -Xmx4g -jar api/target/dependency/webapp-runner.jar api/target/api.war --path /api --port 8080`

# Development process

The repository makes use of the [jgitflow Maven plugin](https://bitbucket.org/atlassian/jgit-flow/wiki/Home)
to implement the [gitflow](http://nvie.com/posts/a-successful-git-branching-model/) git branching model.

See the [pom.xml](pom.xml) for configuration options used.

The plugin is configured to use your SSH Agent for ease of use.

## Performing a release

To perform a release run:

```sh
$ mvn jgitflow:release-start jgitflow:release-finish
```

This will prompt for the version to release (which it derives from the
current develop version, e.g 0.0.2-SNAPSHOT produces a 0.0.2 release version) and the new development version (0.0.3-SNAPSHOT, from our example).

During the release process it will create the release branch and merge it.

When finished push your branches and tags up and the Jenkins jobs will run to produce packages.

## Fresh installations

On a fresh installation of the snomed-release-service-web package, nginx will need to be started as well as
any default configuration removed. This is handled by the ansible role, but if you're not using that you'll
need to do it manually
