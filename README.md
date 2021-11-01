# phenopckt-ig-util
GA4GH Phenopacket FHIR IG Utilities

# Setup: Start a FHIR server

Clone the [hapi-fhir-jpaserver-starter](https://github.com/hapifhir/hapi-fhir-jpaserver-starter) project.
There are many ways of starting the server, but we will use the following. 

```bash
mvn -Djetty.port=8888 jetty:run
```

Server will then be accessible at http://localhost:8888/ and eg. http://localhost:8888/fhir/metadata. Remember to adjust you overlay configuration in the application.yaml to eg.

```yaml
    tester:
      -
          id: home
          name: Local Tester
          server_address: 'http://localhost:8888/fhir'
          refuse_to_fetch_third_party_urls: false
          fhir_version: R4
```

# Loading the implementation guide

First clone the [Phenopacket IG](https://github.com/phenopackets/core-ig) repository and
build the IG locally (with the ``sushi`` command). This will create a new directory called ``output``.
We then run the ``load`` command of this app as follows (note that if you use maven to build the app, the executable 
jar file will be located in the ``target`` subdirectory).

```bazaar
java -jar load phenopktig-util.jar
--server http://localhost:8888/
--ig-out
/home/peter/GIT/core-ig/output
```
