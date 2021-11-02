package org.monarchinitiative.phenopktig.cmd;

import org.monarchinitiative.phenopktig.fhir.FhirIgConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;

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
            stdout.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
