# HAPI-Pheno Server

A HAPI FHIR implementation for working with the GA4GH Phenopacket [FHIR Implementation Guide](http://phenopackets.org/core-ig/).

The code is based on the [hapi-fhir-jpaserver-starter](https://github.com/hapifhir/hapi-fhir-jpaserver-starter) project,
and many classes were copied. The server was extended with functionality to work withthe Phenopacket FHIR IG.

# Setup: IG

Install [SUSHI, SUSHI Unshortens ShortHand Inputs](http://hl7.org/fhir/uv/shorthand/2020May/sushi.html) as described on the SUSHI webpage.
Install [jekyll](https://jekyllrb.com/docs/installation/) as described on the webpage.


Clone the [core-ig](https://github.com/phenopackets/core-ig) repository. 

Run ``	_updatePublisher.sh`` to install the FHIR publisher tool from the core-ig repo.

Run ``_genonce.sh`` from the core-ig. This creates a folder ``output`` with the published IG. Note the
path to this directory, which we will set in the application.properties file.



# Running the server

Clone the [hapi-fhir-jpaserver-starter](https://github.com/hapifhir/hapi-fhir-jpaserver-starter) project.
There are many ways of starting the server, but we will use the following (shown as ``<output>`` below).

```bash
mvn -Djetty.port=8888 jetty:run
```

The server will then be accessible at http://localhost:8888/ and eg. http://localhost:8888/fhir/metadata. 

# Loading LOINC Code/Value sets

It is absolutely necessary to load LOINC data before running the Phenopackets example.
To do so, download the latest ``Loinc_2.72.zip`` archive from the [LOINC website](https://loinc.org/).
(requires an account)

Then download the hapi-fhir CLI tool from the
[HAPI Releases page](https://github.com/hapifhir/hapi-fhir/releases). 
If you downloaded source, build it: mvn package, but the major releases usually have a distribution. There's even one for just the cli here: https://github.com/hapifhir/hapi-fhir/releases/download/v6.0.1/hapi-fhir-6.0.1-cli.zip 
Then run the following command (note that the -t command points to the 
running server endpoint).

```aidl
java -jar hapi-fhir-cli.jar upload-terminology \
  -d Loinc_2.72.zip \
  -v r4 \
  -t http://localhost:8888/fhir \
  -u http://loinc.org
```
This will take roughly 30 minutes to complete. The command returns quickly, the server will be busy for a while.

With LOINC 2.73, I got the following error:
a.uhn.fhir.rest.server.exceptions.UnprocessableEntityException: HTTP 422 Unprocessable Entity: Could not find the following mandatory files in input: [AccessoryFiles/MultiAxialHierarchy/MultiAxialHierarchy.csv]
Fix this by navigating to the Archive of past LOINC releases, and get 2.72 until this is fixed in HAPI-FHIR.

Note that we added the following to the ``Application.java`` class

```aidl
/**
 * ERROR c.u.f.jpa.term.BaseTermReadSvcImpl [BaseTermReadSvcImpl.java:1796] Failed to pre-expand ValueSet: maxClauseCount is set to 1024
 * org.apache.lucene.search.BooleanQuery$TooManyClauses: maxClauseCount is set to 1024
 */
int MAX_CLAUSE_COUNT = 10_000; // to avoid problems as above
BooleanQuery.setMaxClauseCount(MAX_CLAUSE_COUNT);

```
It is also necessary to uncomment the ``#hibernate.search.enabled``
and following lines in the application.yaml file (already done here).

 

# Examining the structure definitions

http://localhost:8888/fhir/StructureDefinition

# Retrieving a Phenopacket and coding as GA4GH native phenopacket
See the accompanying project 
