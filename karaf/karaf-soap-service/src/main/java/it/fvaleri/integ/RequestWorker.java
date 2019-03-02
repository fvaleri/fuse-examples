package it.fvaleri.integ;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import it.fvaleri.integ.InputReportIncident;
import it.fvaleri.integ.OutputReportIncident;

public class RequestWorker {
    private static final Logger LOG = LoggerFactory.getLogger(RequestWorker.class);

    public OutputReportIncident run(InputReportIncident input) {
        LOG.info("Processing request");
        OutputReportIncident ok = new OutputReportIncident();
        ok.setCode("OK " + input.getIncidentId());
        return ok;
    }
}
