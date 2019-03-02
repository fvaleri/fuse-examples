package it.fvaleri.integ;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public class MyProcessor implements Processor {
    private static final Logger LOG = LoggerFactory.getLogger(MyProcessor.class);

    public void process(Exchange exchange) throws Exception {
        InputStream is = exchange.getIn().getBody(InputStream.class);

        if (is == null) {
            exchange.getIn().setBody("InputStream is null");
            return;
        } else {
            String content = new BufferedReader(new InputStreamReader(is))
                .lines().collect(Collectors.joining("\n"));
            exchange.getIn().setBody(content);
        }

        // this should be zero because the stream can be read once
        LOG.info("File size is {} bytes", is.available());
    }
}

