package it.fvaleri.integ;

import io.reactivex.Flowable;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.reactive.streams.api.CamelReactiveStreams;
import org.apache.camel.component.reactive.streams.api.CamelReactiveStreamsService;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;

public class ConsumerBackPressureTest extends CamelTestSupport {
    @Test
    public void test() throws Exception {
        CamelReactiveStreamsService rsCamel = CamelReactiveStreams.get(context);

        // create an array with the messages
        String[] inboxData = new String[100];
        for (int i = 0; i < 100; i++) {
            inboxData[i] = "Hello " + i;
        }

        // use stream engine to create a publisher
        Flowable.fromArray(inboxData)
            .doOnRequest(n -> {
                log.info("Requesting {} messages", n);
            })
            .subscribe(rsCamel.streamSubscriber("inbox", String.class));

        // let it run for 10 seconds
        Thread.sleep(10_000);
    }

    @Override
    protected RoutesBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                // allow at most 5 inflight messages and use 5 concurrent consumers
                from("reactive-streams:inbox?maxInflightExchanges=5&concurrentConsumers=5")
                    // use a little delay so humans can follow what happens
                    .delay(constant(10))
                    .log("Processing message ${body}");
            }
        };
    }
}

