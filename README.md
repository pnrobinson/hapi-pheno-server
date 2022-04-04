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

This is done simply by these lines in the ``application.yaml`` file, no additional work is required.

See [core-ig](https://github.com/phenopackets/core-ig) for the original IG code.

```bazaar
    implementationguides:
      phenopackets:
        url: http://phenopackets.org/core-ig/package.tgz
        name: hl7.fhir.us.ga4gh.phenopacket
        version: 0.1.0
```

# Examining the structure definitions

http://localhost:8888/fhir/StructureDefinition

# Retrieving a Phenopacket and coding as GA4GH native phenopacket
This is done in the accompanying [hapi-pheno-client](https://github.com/pnrobinson/hapi-pheno-client/) project.
