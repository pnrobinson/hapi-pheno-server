package org.monarchinitiative.phenopktig.cmd;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICriterion;
import ca.uhn.fhir.rest.gclient.StringClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import org.hl7.fhir.r5.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.util.*;

@CommandLine.Command(name = "load", aliases = {"L"},
        mixinStandardHelpOptions = true,
        description = "load the FHIR Phenopacket IG")
public class LoadConformanceCommand extends IgCommand {

    private static Logger LOGGER = LoggerFactory.getLogger(LoadConformanceCommand.class);


    private final Map<String, Set<Reference>> refTypeMap = new HashMap<>();
    private final List<ImplementationGuide.ImplementationGuideDefinitionResourceComponent> examples = new ArrayList<>();
    private ImplementationGuide ig;


    public LoadConformanceCommand() {

    }

    @Override
    public void run() {
        LOGGER.info("Starting load command with igOutPath: " +igOutPath);
        LOGGER.info("server: " +hapiFhirUrl);
        setupFhirClient();
        ig = getIg();
        LOGGER.info("Loading IG: " + ig.getUrl() + " to server: " + getHapiFhirUrl());
        List<ImplementationGuide.ImplementationGuideDefinitionResourceComponent> toRemove = new ArrayList<>();

        for (ImplementationGuide.ImplementationGuideDefinitionResourceComponent rc : ig.getDefinition().getResource()) {

            if (rc.hasExample()) {
                examples.add(rc);
                continue;
            }

            Reference ref = rc.getReference();
            LOGGER.info("Reference: {}", ref.getReference());
            String[] refParts = ref.getReference().split("/");
            if (refParts.length == 2) {
                Set<Reference> list = refTypeMap.get(refParts[0]);
                if (list == null) {
                    list = new HashSet<>();
                    refTypeMap.put(refParts[0], list);
                }
                switch (refParts[0]) {
                    case "CodeSystem":
                    case "ValueSet":
                    case "StructureDefinition":
                    case "SearchParameter":
                        list.add(ref);
                        break;
                    default:
                        LOGGER.info("Ignoring reference: " + rc.getReference().getReference());
                        toRemove.add(rc);
                }
            }

        }
        // remove any references we haven't dealt with so far in terms of loading them
        // to the server
        if (toRemove.size() > 0) {
            LOGGER.info("Not loading the following resources...");
            for (ImplementationGuide.ImplementationGuideDefinitionResourceComponent resource : toRemove) {
                LOGGER.info("Failed to load {}", resource.getId());
            }
        }
        ig.getDefinition().getResource().removeAll(toRemove);

        loadResources(refTypeMap.get("CodeSystem"));
        try {
            Thread.sleep(5000);

        loadResources(refTypeMap.get("ValueSet"));
        Thread.sleep(5000);
        loadResources(refTypeMap.get("StructureDefinition"));
        Thread.sleep(5000);
        loadResources(refTypeMap.get("SearchParameter"));
        Thread.sleep(5000);
        loadExamples();
        //loadUpdate(ig, true);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void loadResources(Set<Reference> references) {
        if (references == null) {
            LOGGER.error("Warning: Attempt to load Resources with null pointer");
            return;
        }
        for (Reference reference : references) {
            String[] parts = reference.getReference().split("/");
            MetadataResource updatedMetadata = loadUpdate(loadMetadata(parts[0] + "-" + parts[1] + ".xml"), false);
            if (ValueSet.class.isInstance(updatedMetadata)) {
                // we need to expand it on the server
//                ValueSet vs = (ValueSet) updatedMetadata;
//                updatedMetadata = expandValueSet(vs);
            }
            if (updatedMetadata != null) {
                String id = updatedMetadata.getIdElement().asStringValue();
                LOGGER.debug("Updating IG reference from:" + reference.getReference() + " to:" + id);
                reference.setReferenceElement(new StringType(id));
            }
        }
    }

    private ValueSet expandValueSet(ValueSet vs) {
        Parameters parameters = getClient().operation().onInstance("ValueSet/" + vs.getIdElement().getIdPart())
                .named("expand").withNoParameters(Parameters.class).execute();
        ValueSet expanded = (ValueSet) parameters.getParameterFirstRep().getResource();
        ValueSet.ValueSetExpansionComponent expansion = expanded.getExpansion();
        vs.setExpansion(expansion);
        MethodOutcome mo = getClient().update().resource(vs).execute();
        vs = (ValueSet) mo.getResource();
        return vs;
    }

    /**
     * the overall goal here is to load the examples both with a profile under
     * .meta.profile, and without a profile.
     *
     * if the ig specifies the profile for the example, we make sure we add that
     * profile to one instance of the example if the example already has the profile
     * indicated by the ig, we load a copy of it without that profile
     *
     * we'll add a reference to the ig instance for the additional copies loaded,
     * and tag that reference with a tooling extension.
     */
    private void loadExamples() {
        List<ImplementationGuide.ImplementationGuideDefinitionResourceComponent> addedComponents = new ArrayList<>();
        for (ImplementationGuide.ImplementationGuideDefinitionResourceComponent rc : examples) {
            String ref = rc.getReference().getReference();
            String refProfile = rc.getExampleCanonicalType().asStringValue();

            String[] refParts = ref.split("/");
            DomainResource example = (DomainResource) loadResource(refParts[0] + "-" + refParts[1] + ".xml");
            LOGGER.info("Loading example: {}", example.getId());

            // search if already loaded
            DomainResource existing = null;
            DomainResource existingGenerated = null;

            TokenClientParam tokenParam = new TokenClientParam("_tag");
            ICriterion<TokenClientParam> tokenCriterion = tokenParam.exactly().systemAndCode(ID_TAG_IRI, ref);
            Bundle bundle = (Bundle) getClient().search().forResource(example.getClass()).and(tokenCriterion)
                    .execute();

            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                DomainResource resource = (DomainResource) entry.getResource();
                if (resource.getMeta().getTag(TAG_URI, TAG_GENERATED) != null) {
                    existingGenerated = resource;
                    LOGGER.info("Found existing generated: {}", existingGenerated.getId());
                } else {
                    existing = resource;
                    LOGGER.info("Found existing: {}", existing.getId());
                }
            }

            // create or update original
            DomainResource updated = null;

            // without profile first
            DomainResource exampleCopy = example.copy();
            tagResourceId(exampleCopy, example);
            MethodOutcome methodOutcome = null;
            boolean created = false;
            if (existing != null) {
                exampleCopy.setId(existing.getId());
                methodOutcome = getClient().update().resource(exampleCopy).execute();
            } else {
                methodOutcome = getClient().create().resource(exampleCopy).execute();
                created = true;
            }
            updated = (DomainResource) methodOutcome.getResource();
            if (created) {
                LOGGER.info("Created: " + updated.getId());
            } else {
                LOGGER.info("Updated: " + updated.getId());
            }
            checkOutcome((OperationOutcome) methodOutcome.getOperationOutcome());
            rc.getReference().setReference(updated.getIdElement().asStringValue());

            validate(updated, refProfile);

            // if the example doesn't already have the profile, and we have a profile, we'll
            // add another example instance with the profile
            DomainResource updatedGenerated = null;
            if (refProfile != null && !updated.getMeta().hasProfile(refProfile)) {
                // we'll add the profile to .meta.profile and create/update
                DomainResource generatedCopy = example.copy();
                generatedCopy.getMeta().addProfile(refProfile);
                generatedCopy.getMeta().addTag(TAG_URI, "generated", "generated");
                tagResourceId(generatedCopy, example);
                MethodOutcome mo = null;
                if (existingGenerated != null) {
                    generatedCopy.setId(existingGenerated.getId());
                    mo = getClient().update().resource(generatedCopy).execute();
                    LOGGER.info("Updated generated example: " + ((Resource) mo.getResource()).getId());
                } else {
                    mo = getClient().create().resource(generatedCopy).execute();
                    LOGGER.info("Created generated example: " + ((Resource) mo.getResource()).getId());
                }
                checkOutcome((OperationOutcome) mo.getOperationOutcome());
                generatedCopy = (DomainResource) mo.getResource();
                Reference r = new Reference(generatedCopy);
                ImplementationGuide.ImplementationGuideDefinitionResourceComponent rcGenerated =
                        new ImplementationGuide.ImplementationGuideDefinitionResourceComponent();
                rcGenerated.getReference().setReference(generatedCopy.getId());
                rcGenerated.addExtension(TAG_URI, new StringType("generated"));
                addedComponents.add(rcGenerated);

            } else {
                // skip but delete any existing generated instance from the past.
                if (existingGenerated != null) {
                    LOGGER.info("Deleting no longer needed existing generated example: " + existingGenerated.getId());
                    MethodOutcome execute = getClient().delete().resource(existingGenerated).execute();
                    checkOutcome((OperationOutcome) execute.getOperationOutcome());
                }
            }

            // //
            // // now if we have a profile for the example, update or create
            // if (refProfile != null) {
            // exampleCopy = example.copy();
            // Utils.tagResourceId(exampleCopy, example);
            // if (!exampleCopy.getMeta().hasProfile(refProfile)) {
            // exampleCopy.getMeta().addProfile(refProfile);
            // }
            // if (existingGenerated != null) {
            // exampleCopy.setId(existingGenerated.getId());
            // methodOutcome = main.getClient().update().resource(exampleCopy).execute();
            // } else {
            // methodOutcome = main.getClient().create().resource(exampleCopy).execute();
            // }
            // updatedGenerated = (DomainResource) methodOutcome.getResource();
            // }
            //
            // // TODO: validation and reporting
        }

        ig.getDefinition().getResource().addAll(addedComponents);

    }

    private MetadataResource loadUpdate(MetadataResource resource, boolean deleteExisting) {
        LOGGER.info("Loading: " + resource.getId() + ", url:" + resource.getUrl());
        MetadataResource resourceCopy = resource.copy();
        tagResourceId(resourceCopy, resource);
        IGenericClient client = getClient();
        String url = resource.getUrl();
        Bundle bundle = (Bundle) client.search().forResource(resource.getClass())
                .and(new StringClientParam("url").matchesExactly().value(url)).execute();

        List<Bundle.BundleEntryComponent> entries = bundle.getEntry();
        MetadataResource existingResource = null;

        if (entries.size() > 1) {
            String json = getJsonParser().encodeResourceToString(bundle);
            LOGGER.warn("Search returned: multiple bundle entries. Skippping. JSON:");
            LOGGER.debug(json);
            return null;

        } else if (entries.size() == 1) {
            existingResource = (MetadataResource) entries.get(0).getResource();
            LOGGER.info("Search returned: " + existingResource.getId());
        }

        if (deleteExisting && existingResource != null) {
            LOGGER.info("Deleting resource: " + existingResource.getId());
            getClient().delete().resource(existingResource).execute();
            existingResource = null;
        }

        logRequest();
        logResponse();

        MethodOutcome mo = null;

        if (existingResource == null) {
            LOGGER.info("Creating resource...");
            mo = getClient().create().resource(resourceCopy).execute();
        } else {
            resourceCopy.setId(existingResource.getId());
            LOGGER.info("Updating resource: " + resourceCopy.getId());
            mo = getClient().update().resource(resourceCopy).execute();
        }

        checkOutcome( (OperationOutcome) mo.getOperationOutcome());

        MetadataResource mr = (MetadataResource) mo.getResource();

        validate( mr, null);

        return (MetadataResource) mo.getResource();
    }
}
