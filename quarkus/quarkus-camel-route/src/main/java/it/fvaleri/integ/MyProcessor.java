package it.fvaleri.integ;

import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.enterprise.context.ApplicationScoped;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

@ApplicationScoped
public class MyProcessor implements Processor {
    @Override
    public void process(Exchange exchange) throws Exception {
        String name = "unknown-host";
        try {
            name = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
        }
        exchange.getIn().setBody("Hello " + name);
    }
}

