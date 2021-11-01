package org.monarchinitiative.phenopktig.cmd;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import ca.uhn.fhir.context.ConfigurationException;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.apache.ApacheHttpRequest;
import ca.uhn.fhir.rest.client.apache.ApacheHttpResponse;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.CapturingInterceptor;
import ca.uhn.fhir.rest.server.exceptions.PreconditionFailedException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.hl7.fhir.instance.model.api.IBaseCoding;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r5.model.*;
import org.hl7.fhir.r5.utils.client.network.Client;
import org.monarchinitiative.phenopktig.exception.PhenopacktIgUtilRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

@CommandLine.Command(name = "phenopckt-ig-util", scope = CommandLine.ScopeType.INHERIT,
        mixinStandardHelpOptions = true, version = "app version 1.0",
        header = "Phenopacket FHIR IG Util",
        description = "Demo for using the GA4GH Phenopacket FHIR IG",
        footerHeading = "Copyright%n", footer = "(c) Copyright by the authors",
        showAtFileInUsageHelp = true)
public abstract class IgCommand implements Runnable {
    private static Logger LOGGER = LoggerFactory.getLogger(IgCommand.class);
    /** The directory where we download Jannovar files and later load them. */
    @CommandLine.Option(names = {"-s", "--server"},
            scope = CommandLine.ScopeType.INHERIT,
            required = true,
            description = "URL of HAPI FHIR Server")
    protected String hapiFhirUrl;

    @CommandLine.Option(names = { "-o", "--ig-out" },
            scope = CommandLine.ScopeType.INHERIT,
            description = "The location of the IG output directory ",
            required = true)
    protected Path igOutPath;

    protected FhirContext ctx;

    protected IParser xmlParser;

    protected IParser jsonParser;

    protected IGenericClient client = null;

    protected OutputStreamWriter reporter = null;

    private final ObjectMapper objectMapper =  new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);;

    public static final String ID_TAG_IRI = "http://github.com/phenopackets/core-ig/ig-tools/id-tag";
    public static final String TAG_URI = "http://github.com/phenopackets/core-ig/ig-tools/tag";
    public static final String TAG_GENERATED = "generated";
    public static final String VALIDATION_EXT_URI = "http://github.com/phenopackets/core-ig/ig-tools/validation-error-count";


    protected CapturingInterceptor capturing;

    protected void setupFhirClient() {
        ctx = FhirContext.forR5();
        xmlParser = ctx.newXmlParser();
        jsonParser = ctx.newJsonParser().setPrettyPrint(true);
        if (hapiFhirUrl != null) {
            client = ctx.newRestfulGenericClient(hapiFhirUrl);
            this.capturing = new CapturingInterceptor();
            client.registerInterceptor(capturing);
        }
    }

    protected String getHapiFhirUrl() {
        return hapiFhirUrl;
    }

    protected ImplementationGuide getIg() {
        LOGGER.info("Searching for implementation guide at {}", igOutPath);
        if (this.xmlParser == null) {
            throw new PhenopacktIgUtilRuntimeException("Xml Parser is null. Did you call setupFhirClient?");
        }
        List<Path> paths;
        try {
            paths = getPaths(igOutPath, "ImplementationGuide-*.xml");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (paths.size() != 1) {
            throw new IllegalArgumentException("Could not find one ImplementationGuide-*.xml file at: " + igOutPath);
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

    private List<Path> getPaths(Path dir, String match) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, match)) {
            return StreamSupport.stream(stream.spliterator(), false).collect(Collectors.toList());
        }
    }
    /**
     * @param fileName is relative to the IG output directory.
     * @return
     */
    public Resource loadResource(String fileName) {
        try {
            return (Resource) xmlParser.parseResource(new FileInputStream(igOutPath.resolve(fileName).toFile()));
        } catch (ConfigurationException | DataFormatException | FileNotFoundException e) {
            throw new PhenopacktIgUtilRuntimeException(e.getLocalizedMessage());
        }
    }

    protected IGenericClient getClient() {
        return client;
    }

    public static int checkOutcome(OperationOutcome outcome) {

        Map<OperationOutcome.IssueSeverity, List<String>> issues = new HashMap<>();
        issues.put(OperationOutcome.IssueSeverity.FATAL, new ArrayList<>());
        issues.put(OperationOutcome.IssueSeverity.ERROR, new ArrayList<>());
        issues.put(OperationOutcome.IssueSeverity.WARNING, new ArrayList<>());
        issues.put(OperationOutcome.IssueSeverity.INFORMATION, new ArrayList<>());
        issues.put(null, new ArrayList<>());
        if (outcome != null) {
            for (OperationOutcome.OperationOutcomeIssueComponent issue : outcome.getIssue()) {
                StringBuilder sb = new StringBuilder();
                OperationOutcome.IssueSeverity severity = issue.getSeverity();

                sb.append("Severity:" + severity);
                sb.append(", Code:" + issue.getCode());
                sb.append(", Details:" + issue.getDetails().getText());
                sb.append(", Location:" + issue.getLocation());
                sb.append(", Diagnositc:" + issue.getDiagnostics());

                issues.get(severity).add(sb.toString());

            }
        }

        // a one line output summary
        StringBuilder sb = new StringBuilder("Outcome summary: ");
        sb.append(issues.get(OperationOutcome.IssueSeverity.FATAL).size() + " fatals,");
        sb.append(issues.get(OperationOutcome.IssueSeverity.ERROR).size() + " errors,");
        sb.append(issues.get(OperationOutcome.IssueSeverity.WARNING).size() + " warnings,");
        sb.append(issues.get(OperationOutcome.IssueSeverity.INFORMATION).size() + " infos");
        LOGGER.info(sb.toString());

        // for each severity
        for (String severity : issues.get(OperationOutcome.IssueSeverity.FATAL)) {
            LOGGER.error("Outcome: " + severity);
        }
        for (String severity : issues.get(OperationOutcome.IssueSeverity.ERROR)) {
            LOGGER.error("Outcome: " + severity);
        }
        for (String severity : issues.get(OperationOutcome.IssueSeverity.WARNING)) {
            LOGGER.warn("Outcome: " + severity);
        }
        for (String severity : issues.get(OperationOutcome.IssueSeverity.INFORMATION)) {
            LOGGER.info("Outcome: " + severity);
        }

        return issues.get(OperationOutcome.IssueSeverity.FATAL).size() + issues.get(OperationOutcome.IssueSeverity.ERROR).size();
    }

    public void validate(DomainResource resource, String profile) {
        int maxErrors = 0;
        Extension validationExt = resource.getExtensionByUrl(VALIDATION_EXT_URI);
        if (validationExt != null) {
            maxErrors = Integer.valueOf(validationExt.getValue().primitiveValue());
        }
        LOGGER.info("Validating with profile: " + profile + ", and max errors: " + maxErrors);
        Parameters params = new Parameters();
        if (profile != null) {
            params.addParameter("profile", profile);
        }
        MethodOutcome methodOutcome = null;
        OperationOutcome operationOutcome = null;
        try {
            methodOutcome = getClient().operation().onInstance(resource.getId()).named("$validate")
                    .withParameters(params).returnMethodOutcome().execute();
            if (methodOutcome.getResource() instanceof OperationOutcome) {
                operationOutcome = (OperationOutcome) methodOutcome.getResource();
            } else {
                operationOutcome = (OperationOutcome) methodOutcome.getOperationOutcome();
            }
        } catch (PreconditionFailedException e) {
            operationOutcome = (OperationOutcome) e.getOperationOutcome();
        }

        int actualErrors = checkOutcome(operationOutcome);

        if (actualErrors > maxErrors) {
            LOGGER.error("VALIDATION ERROR COUNT OF: " + actualErrors + " exceeded max allowed validation error count of:"
                    + maxErrors);
        }
        if (actualErrors < maxErrors) {
            LOGGER.warn("VALIDATION ERROR COUNT OF: " + actualErrors + " less than max allowed validation error count of:"
                    + maxErrors);
        }
    }

    public void tagResourceId(IBaseResource target, IBaseResource source) {
        String id = source.getIdElement().getValue();

        for (IBaseCoding coding : target.getMeta().getTag()) {
            if (coding.getSystem().equals(ID_TAG_IRI)) {
                coding.setCode(id);
                return;
            }
        }
        IBaseCoding tag = target.getMeta().addTag();
        tag.setSystem(ID_TAG_IRI);
        tag.setCode(id);
    }

    public IParser getJsonParser() {
        return jsonParser;
    }

    public String logRequest() {
        String request = getRequest();
        LOGGER.debug(request);
        return request;
    }

    public  String getRequest() {
        StringBuilder sb = new StringBuilder();
        sb.append(getRequestMethod() + "\n");
        sb.append(getRequestHeaders() + "\n");
        sb.append(getRequestBody() + "\n");
        return sb.toString();
    }

    public String getRequestMethod() {
        ApacheHttpRequest request = (ApacheHttpRequest) getCapturing().getLastRequest();
        StringBuilder sb = new StringBuilder();
        sb.append(request.getHttpVerbName() + " ");
        try {
            sb.append(URLDecoder.decode(request.getUri(), StandardCharsets.UTF_8.toString()));
        } catch (UnsupportedEncodingException e) {
             throw new PhenopacktIgUtilRuntimeException(e.getMessage());
        }
        sb.append(" " + request.getApacheRequest().getProtocolVersion());
        return sb.toString();
    }

    public CapturingInterceptor getCapturing() {
        return capturing;
    }

    public String getRequestHeaders() {
        ApacheHttpRequest request = (ApacheHttpRequest) getCapturing().getLastRequest();
        StringBuilder sb = new StringBuilder();
        for (String header : request.getAllHeaders().keySet()) {
            sb.append(header + ": " + request.getAllHeaders().get(header) + "\n");
        }
        return sb.toString();
    }

    public String getRequestBody() {
        ApacheHttpRequest request = (ApacheHttpRequest) getCapturing().getLastRequest();
        StringBuilder sb = new StringBuilder();
        String bodyFromStream = null;
        try {
            bodyFromStream = request.getRequestBodyFromStream();
            if (bodyFromStream == null) {
                sb.append("NO BODY AVAILABLE");
            } else {
                Object json = objectMapper.readValue(bodyFromStream, Object.class);
                bodyFromStream = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
                sb.append(bodyFromStream);
            }
        } catch (IOException e) {
            throw new PhenopacktIgUtilRuntimeException(e.getMessage());
        }
        sb.append("\n");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    public <T extends MetadataResource> T loadMetadata(String fileName) {
        try {
            return (T) xmlParser.parseResource(new FileInputStream(igOutPath.resolve(fileName).toFile()));
        } catch (ConfigurationException | DataFormatException | FileNotFoundException e) {
            throw new PhenopacktIgUtilRuntimeException(e.getMessage());
        }
    }

    public String logResponse() {
        String request = getResponse();
        LOGGER.debug(request);
        return request;
    }

    public String getResponse() {
        StringBuilder sb = new StringBuilder();
        sb.append(getResponseStatus() + "\n");
        sb.append(getResponseHeaders() + "\n");
        sb.append(getResponseBody() + "\n");
        return sb.toString();
    }

    public String getResponseStatus( ) {
        ApacheHttpResponse response = (ApacheHttpResponse) getCapturing().getLastResponse();
        return response.getResponse().getStatusLine().toString();
    }

    public String getResponseHeaders() {
        ApacheHttpResponse response = (ApacheHttpResponse) getCapturing().getLastResponse();
        StringBuilder sb = new StringBuilder();
        for (String header : response.getAllHeaders().keySet()) {
            sb.append(header + ": " + response.getAllHeaders().get(header) + "\n");
        }
        return sb.toString();
    }

    public String getResponseBody() {
        ApacheHttpResponse response = (ApacheHttpResponse) getCapturing().getLastResponse();
        StringBuilder sb = new StringBuilder();
        try {
            sb.append(readReader(response.createReader()));
        } catch (IOException e) {
            throw new PhenopacktIgUtilRuntimeException(e.getMessage());
        }

        return sb.toString();
    }

    private  String readReader(Reader reader) {

        char[] buffer = new char[4096];
        StringBuilder builder = new StringBuilder();
        int numChars;
        try {
            while ((numChars = reader.read(buffer)) >= 0) {
                builder.append(buffer, 0, numChars);
            }
        } catch (IOException e) {
            throw new PhenopacktIgUtilRuntimeException(e.getMessage());
        }
        return builder.toString();
    }


}
