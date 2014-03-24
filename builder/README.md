Builder Process
===============

This module is an executible jar which runs Maven SNOMED builds.

Procedure:
- Listen on the build queue for the URL of an Execution
- Download Execution's build scripts zip via the API and extract
- Execute the Maven build process
- Upload the results via the API
- Repeat

Starting the Builder
--------------------
After building start the jar from the module directory with:
`java -jar target/builder.jar`
