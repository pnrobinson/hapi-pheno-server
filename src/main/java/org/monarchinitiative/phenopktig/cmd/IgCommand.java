package org.monarchinitiative.phenopktig.cmd;

import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.util.concurrent.Callable;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.CapturingInterceptor;
import picocli.CommandLine;


public abstract class IgCommand implements Callable<Integer> {
    /** The directory where we download Jannovar files and later load them. */
    @CommandLine.Option(names = {"-s", "--server"},
            scope = CommandLine.ScopeType.INHERIT,
            required = true,
            description = "URL of HAPI FHIR Server")
    protected String hapiFhirUrl;

    @CommandLine.Option(names = { "-o", "--ig-out" }, description = "The location of the IG output directory "
            + "(Default: ${DEFAULT-VALUE}).")
    protected String igOutPath = "igout";

    protected FhirContext ctx;

    protected IParser xmlParser;

    protected IParser jsonParser;

    protected IGenericClient client = null;

    protected OutputStreamWriter reporter = null;


    protected CapturingInterceptor capturing;

    protected void setupFhirClient() {
        ctx = FhirContext.forR4();
        xmlParser = ctx.newXmlParser();
        jsonParser = ctx.newJsonParser().setPrettyPrint(true);
        if (hapiFhirUrl != null) {
            client = ctx.newRestfulGenericClient(hapiFhirUrl);
            this.capturing = new CapturingInterceptor();
            client.registerInterceptor(capturing);
        }

    }
}
