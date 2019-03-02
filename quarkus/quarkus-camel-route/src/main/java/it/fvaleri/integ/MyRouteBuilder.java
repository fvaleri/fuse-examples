package it.fvaleri.integ;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.camel.builder.RouteBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class MyRouteBuilder extends RouteBuilder {
    @ConfigProperty(name = "timer.period", defaultValue = "1s")
    String period;

    @Inject
    MyProcessor processor;

    public void configure() {
        fromF("timer://foo?period=%s", period)
            .process(processor)
            .log("${body}");
    }
}

