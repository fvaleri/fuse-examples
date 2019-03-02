package it.fvaleri.integ;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
public class Consumer {
    private static final Logger LOG = LoggerFactory.getLogger(Consumer.class);

    @JmsListener(destination = "example.foo")
    public void processMsg(String message) {
        LOG.info("--- Got {}", message);
    }
}

