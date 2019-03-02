package it.fvaleri.integ;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestParamType;
import org.springframework.stereotype.Component;

@Component
public class MyRouteBuilder extends RouteBuilder {
    public void configure() {
        restConfiguration()
            .contextPath("/api");

        rest().get("/messages")
            .produces("text/plain")
            .route()
                .to("sql:select message from audit_log order by audit_id")
                .convertBodyTo(String.class);

        rest().post("/message")
            .param().name("content").type(RestParamType.query).endParam()
            .produces("text/plain")
            .route()
                .transform().header("content")
                .to("direct:tx-route");

        from("direct:tx-route")
            .transacted()
            .transform().simple("${body}")
            .log("Processing message ${body}")
            .setHeader("message", body())
            .to("sql:insert into audit_log (message) values (:#message)")
            .to("jms:outbound?disableReplyTo=true")
            .choice()
                .when(body().startsWith("fail"))
                    .log("Forced exception")
                        .process(x -> {throw new RuntimeException("Boom");})
                .otherwise()
                    .log("Message ${body} added")
            .endChoice();

        from("jms:outbound")
            .log("Message sent to outbound: ${body}")
            .setHeader("message", simple("${body}-ok"))
            .to("sql:insert into audit_log (message) values (:#message)");
    }
}

