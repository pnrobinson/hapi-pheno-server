package org.monarchinitiative.phenopktig;

import org.monarchinitiative.phenopktig.cmd.LoadConformanceCommand;
import org.monarchinitiative.phenopktig.cmd.PingCommand;
import picocli.CommandLine;


@CommandLine.Command(name = "phenopktigutil",
        mixinStandardHelpOptions = true,
        version = "0.0.1",
        description = "Send/receive/decode Phenopackets with local HAPI FHIR Server")
public class Main implements Runnable {
    public static void main(String[] args) {
        if (args.length == 0) {
            args = new String[]{"-h"}; // if the user does not pass any arguments, show help
        }
        CommandLine cline = new CommandLine(new Main())
                .addSubcommand("load", new LoadConformanceCommand())
                .addSubcommand("ping", new PingCommand());
        cline.setToggleBooleanFlags(false);
        int exitCode = cline.execute(args);
        System.exit(exitCode);
    }

    public Main() {
    }


    @Override
    public void run() {
        // work done in subcommands
    }
}
