package org.monarchinitiative.phenopktig.cmd;

public class PingCommand extends IgCommand {



    @Override
    public void run() {
        System.out.println("ping: " + hapiFhirUrl);


    }
}
