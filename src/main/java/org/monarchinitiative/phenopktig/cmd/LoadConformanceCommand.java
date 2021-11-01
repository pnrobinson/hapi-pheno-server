package org.monarchinitiative.phenopktig.cmd;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICriterion;
import ca.uhn.fhir.rest.gclient.StringClientParam;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import org.hl7.fhir.r5.model.*;
import org.monarchinitiative.phenopktig.fhir.FhirIgConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.*;

@CommandLine.Command(name = "load", aliases = {"L"},
        mixinStandardHelpOptions = true,
        description = "load the FHIR Phenopacket IG")
public class LoadConformanceCommand extends IgCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoadConformanceCommand.class);

    public LoadConformanceCommand() {

    }

    @Override
    public void run() {
        LOGGER.info("Starting load command with igOutPath: " +igOutPath);
        LOGGER.info("server: " +hapiFhirUrl);
        FhirIgConnector connector = new FhirIgConnector(this.hapiFhirUrl, this.igOutPath);
        try {
            BufferedWriter stdout = new BufferedWriter(new OutputStreamWriter(System.out));
            connector.printStatus(stdout);
        } catch (IOException e) {
            e.printStackTrace();
        }

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



}
