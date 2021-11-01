package org.monarchinitiative.phenopktig.cmd;


import java.nio.file.Path;


import picocli.CommandLine;

/**
 * Common superclass with two shared command-line options
 */
@CommandLine.Command(name = "phenopckt-ig-util", scope = CommandLine.ScopeType.INHERIT,
        mixinStandardHelpOptions = true, version = "app version 1.0",
        header = "Phenopacket FHIR IG Util",
        description = "Demo for using the GA4GH Phenopacket FHIR IG",
        footerHeading = "Copyright%n", footer = "(c) Copyright by the authors",
        showAtFileInUsageHelp = true)
public abstract class IgCommand implements Runnable {
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


}
