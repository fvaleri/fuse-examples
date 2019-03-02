package it.fvaleri.integ;

import org.apache.camel.Exchange;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageWorker {
    private static final Logger LOG = LoggerFactory.getLogger(MessageWorker.class);

    public void run(Exchange exchange) throws Exception {
        LOG.info("Processing message");
        Thread.sleep(600);
    }
}

