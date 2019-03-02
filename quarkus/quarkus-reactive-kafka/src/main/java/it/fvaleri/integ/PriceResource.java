package it.fvaleri.integ;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.annotations.SseElementType;
import org.reactivestreams.Publisher;

import org.eclipse.microprofile.reactive.messaging.Channel;

/**
 * A simple JAX-RS resource retrieving the in-memory "my-data-stream" and sending the
 * items to a server-sent event. Unlike WebSockets, server-sent events are unidirectional.
 * Data messages are delivered in one direction, from the server to the client (i.e. web browser).
 */
@Path("/prices")
public class PriceResource {
    @Inject
    @Channel("my-data-stream")
    Publisher<Double> prices;

    @GET
    @Path("/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS) // server side events (SSE) will be produced
    @SseElementType("text/plain") // contained data is just regular text/plain data
    public Publisher<Double> stream() {
        return prices;
    }
}

