package it.fvaleri.integ;

import io.reactivex.Flowable;
import org.apache.camel.LoggingLevel;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.reactive.streams.api.CamelReactiveStreams;
import org.apache.camel.component.reactive.streams.api.CamelReactiveStreamsService;
import org.apache.camel.impl.ThrottlingInflightRoutePolicy;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;
import org.reactivestreams.Publisher;

public class InflightBackPressureTest extends CamelTestSupport {
    @Test
    public void test() throws Exception {
        CamelReactiveStreamsService rsCamel = CamelReactiveStreams.get(context);

        // create a publisher that receives from the inbox stream
        Publisher<String> inbox = rsCamel.fromStream("inbox", String.class);

        // use stream engine to subscribe from the publisher
        Flowable.fromPublisher(inbox)
            .doOnNext(c -> {
                log.info("Processing message {}", c);
                Thread.sleep(1000);
            })
            .subscribe();

        // send in 200 messages
        log.info("Sending 200 messages ...");
        for (int i = 0; i < 200; i++) {
            fluentTemplate.withBody("Hello " + i).to("seda:inbox?waitForTaskToComplete=Never").send();
        }
        log.info("Sent 200 messages done");

        // let it run for 250 seconds
        Thread.sleep(250_000);
    }

    @Override
    protected RoutesBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                // use in flight route policy to throttle how many messages to take in by Camel
                // it suspends the route and works best with messaging systems (JMS, AMQP, Kafka)
                ThrottlingInflightRoutePolicy inflight = new ThrottlingInflightRoutePolicy();
                inflight.setMaxInflightExchanges(20);
                // start consumer again when we are down or below 25% of max
                inflight.setResumePercentOfMax(25);

                from("seda:inbox")
                    .routePolicy(inflight)
                    // use a little delay as otherwise Camel is too fast and the inflight throttler cannot
                    // react so precisely and it also spread the incoming messages more evenly than a big burst
                    .delay(100)
                    .log("Camel routing to Reactive Streams: ${body}")
                    .to("reactive-streams:inbox");
            }
        };
    }
}

