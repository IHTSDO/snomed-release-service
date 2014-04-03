SNOMED Release Service: API
============================

The API is built as a self serving executable jar and as a traditional war.

To launch the executable jar:
```sh
cd snomed-release-service
mvn clean install
cd api
java -jar target/exec-api.jar -httpPort=8085`
```

The API will then be available on your machine here: http://localhost:8085/api/v1/
