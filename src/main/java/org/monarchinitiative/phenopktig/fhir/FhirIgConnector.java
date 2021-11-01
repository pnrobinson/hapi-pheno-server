package org.monarchinitiative.phenopktig.fhir;

import ca.uhn.fhir.context.ConfigurationException;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.CapturingInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.hl7.fhir.r5.model.ImplementationGuide;
import org.hl7.fhir.r5.model.Reference;
import org.hl7.fhir.r5.model.Resource;
import org.monarchinitiative.phenopktig.exception.PhenopacktIgUtilRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class FhirIgConnector {
    private static Logger LOGGER = LoggerFactory.getLogger(FhirIgConnector.class);
    public static final String ID_TAG_IRI = "http://github.com/phenopackets/core-ig/ig-tools/id-tag";
    public static final String TAG_URI = "http://github.com/phenopackets/core-ig/ig-tools/tag";
    public static final String TAG_GENERATED = "generated";
    public static final String VALIDATION_EXT_URI = "http://github.com/phenopackets/core-ig/ig-tools/validation-error-count";


    private FhirContext ctx;

    private IParser xmlParser;

    private IParser jsonParser;

    private IGenericClient client = null;

    private final ObjectMapper objectMapper =  new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private final CapturingInterceptor capturing;

    private final String hapiFhirUrl;

    private final ImplementationGuide implementationGuide;

    private final Path igPath;

    private final Map<String, Set<Reference>> refTypeMap = new HashMap<>();

    FhirIgConnector(String hapiUrl, Path igOutPath) {
        ctx = FhirContext.forR5();
        xmlParser = ctx.newXmlParser();
        jsonParser = ctx.newJsonParser().setPrettyPrint(true);
        this.hapiFhirUrl = hapiUrl;
        Objects.requireNonNull(this.hapiFhirUrl);
        client = ctx.newRestfulGenericClient(hapiFhirUrl);
        this.capturing = new CapturingInterceptor();
        client.registerInterceptor(capturing);
        this.igPath = igOutPath;
        implementationGuide = initializeIg();

    }

    private void initializeResources() {
        List<ImplementationGuide.ImplementationGuideDefinitionResourceComponent> examples = new ArrayList<>();
        List<ImplementationGuide.ImplementationGuideDefinitionResourceComponent> toRemove = new ArrayList<>();

        for (ImplementationGuide.ImplementationGuideDefinitionResourceComponent rc : implementationGuide.getDefinition().getResource()) {

            if (rc.hasExample()) {
                examples.add(rc);
                continue;
            }

            Reference ref = rc.getReference();
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

    }

    /**
     * This method expects the FHIR Phenopacket IG XML file
     * @return ImplementationGuide as initialized from the corresponding XML file
     */
    private ImplementationGuide initializeIg() {
        LOGGER.info("Searching for implementation guide at {}", igPath);
        if (this.xmlParser == null) {
            throw new PhenopacktIgUtilRuntimeException("Xml Parser is null. Did you call setupFhirClient?");
        }
        List<Path> paths;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(igPath, "ImplementationGuide-*.xml")) {
            paths =  StreamSupport.stream(stream.spliterator(), false).collect(Collectors.toList());
        } catch (IOException e) {
            throw new PhenopacktIgUtilRuntimeException(e.getMessage());
        }
        if (paths.size() != 1) {
            throw new IllegalArgumentException("Could not find one ImplementationGuide-*.xml file at: " + igPath);
        }

        Path path0 = paths.get(0);
        File igFile = path0.toFile();
        LOGGER.info("Found path at {}", igFile.getAbsoluteFile());
        LOGGER.info("Path exists?: " + igFile.isFile());

        try {
            return xmlParser.parseResource(ImplementationGuide.class, new FileInputStream(igFile));
        } catch (DataFormatException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param fileName is relative to the IG output directory.
     * @return
     */
    public Resource loadResource(String fileName) {
        try {
            return (Resource) xmlParser.parseResource(new FileInputStream(this.igPath.resolve(fileName).toFile()));
        } catch (ConfigurationException | DataFormatException | FileNotFoundException e) {
            throw new PhenopacktIgUtilRuntimeException(e.getLocalizedMessage());
        }
    }


}
