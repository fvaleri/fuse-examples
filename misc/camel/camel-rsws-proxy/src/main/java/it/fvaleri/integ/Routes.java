package it.fvaleri.integ;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.apache.camel.model.dataformat.SoapJaxbDataFormat;

public final class Routes extends RouteBuilder {
    @Override
    public void configure() {
        final JacksonDataFormat json = new JacksonDataFormat(Instruction.class);

        final SoapJaxbDataFormat soap = new SoapJaxbDataFormat("org.tempuri");
        soap.setVersion("1.2");

        from("netty-http:proxy://0.0.0.0:8080")
            .unmarshal(json)
            .setHeader("SOAPAction", simple("${body.soapAction()}"))
            .setBody(simple("${body.operation()}"))
            .marshal(soap)
            // set content-type after marshal to avoid reset to application/xml
            .setHeader("Content-Type", constant("application/soap+xml; charset=utf-8"))
            .log("${body}")
            .toD("netty-http:"
                + "${header." + Exchange.HTTP_SCHEME + "}://"
                + "${bean:svc?method=hostname}:"
                + "${header." + Exchange.HTTP_PORT + "}"
                + "${header." + Exchange.HTTP_PATH + "}")
            .unmarshal(soap)
            .log("${body}")
            .marshal(json);
    }
}

