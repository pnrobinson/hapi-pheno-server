package org.monarchinitiative.phenopktig.cmd;

public class PingCommand extends IgCommand {




    @Override
    public Integer call() throws Exception {
        System.out.println("ping: " + hapiFhirUrl);
        setupFhirClient();
        return 0;
    }
}
