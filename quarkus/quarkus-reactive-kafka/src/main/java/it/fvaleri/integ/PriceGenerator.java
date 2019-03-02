package it.fvaleri.integ;

import java.time.Duration;
import java.util.Random;

import javax.enterprise.context.ApplicationScoped;

import io.smallrye.mutiny.Multi;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

/**
 * A bean producing random prices every 5 seconds.
 * The prices are written to the Kafka "prices" topic.
 */
@ApplicationScoped
public class PriceGenerator {
    private Random random = new Random();

    @Outgoing("generated-price")
    public Multi<Integer> generate() {
        // returning a Mutiny reactive stream (Multi) of prices called generated-price
        // this stream is mapped to Kafka using the application.properties
        return Multi.createFrom().ticks().every(Duration.ofSeconds(5))
                .onOverflow().drop()
                .map(tick -> random.nextInt(100));
    }
}

