# HAPI-Pheno Server

A HAPI FHIR implementation for working with the GA4GH Phenopacket [FHIR Implementation Guide](http://phenopackets.org/core-ig/).

The code is based on the [hapi-fhir-jpaserver-starter](https://github.com/hapifhir/hapi-fhir-jpaserver-starter) project,
and many classes were copied. The server was extended with functionality to work withthe Phenopacket FHIR IG.

# Setup: IG

Install [SUSHI, SUSHI Unshortens ShortHand Inputs](http://hl7.org/fhir/uv/shorthand/2020May/sushi.html) as described on the SUSHI webpage.
Install [jekyll](https://jekyllrb.com/docs/installation/) as described on the webpage.

Run ``	_updatePublisher.sh`` to install the FHIR publisher tool.

Clone the [core-ig](https://github.com/phenopackets/core-ig) repository. 

Run ``_genonce.sh`` from the core-ig. This creates a folder ``output`` with the published IG. Note the
path to this directory, which we will set in the application.properties file.



# Running the server

Clone the [hapi-fhir-jpaserver-starter](https://github.com/hapifhir/hapi-fhir-jpaserver-starter) project.
There are many ways of starting the server, but we will use the following (shown as ``<output>`` below).

```bash
mvn -Djetty.port=8888 jetty:run
```

The server will then be accessible at http://localhost:8888/ and eg. http://localhost:8888/fhir/metadata. 

# Loading the implementation guide

First clone the [Phenopacket IG](https://github.com/phenopackets/core-ig) repository and
build the IG locally (with the ``sushi`` command). This will create a new directory called ``output``.
We then run the ``load`` command of this app as follows (note that if you use maven to build the app, the executable 
jar file will be located in the ``target`` subdirectory).

```bazaar
java -jar load phenopktig-util.jar
--server http://localhost:8888/
--ig-out <output>
```

# Examining the structure definitions

http://localhost:8888/fhir/StructureDefinition

# Retrieving a Phenopacket and coding as GA4GH native phenopacket
This is done in the accompanying project 
