package it.fvaleri.integ;

import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import io.vertx.ext.web.codec.BodyCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

/**
 * Each verticle runs in its own event-loop thread, which shouldn't be blocked.
 */
public class EdgeVerticle extends AbstractVerticle {

    private static final Logger LOG = LoggerFactory.getLogger(Application.class);

    private static final int httpPort = Integer.parseInt(System.getenv().getOrDefault("HTTP_PORT", "6000"));
    private static final String storeHost = System.getenv().getOrDefault("STORE_HOST", "127.0.0.1");
    private static final int storePort = Integer.parseInt(System.getenv().getOrDefault("STORE_PORT", "7000"));

    private final HashMap<String, Object> lastData = new HashMap<>();
    private WebClient webClient;
    private CircuitBreaker breaker;
    private JsonObject cachedLastFiveMinutes;

    @Override
    public void start(Promise<Void> startPromise) {
        webClient = WebClient.create(vertx);
        breaker = CircuitBreaker.create("store", vertx);

        breaker.openHandler(v -> LOG.info("Circuit breaker open"));
        breaker.halfOpenHandler(v -> LOG.info("Circuit breaker half-open"));
        breaker.closeHandler(v -> LOG.info("Circuit breaker close"));

        vertx.eventBus().consumer("temperature.updates", this::storeUpdate);

        Router router = Router.router(vertx);
        router.get("/latest").handler(this::latestData);
        router.get("/five-minutes").handler(this::fiveMinutes);

        vertx.createHttpServer().requestHandler(router).listen(httpPort).onSuccess(ok -> {
            LOG.info("HTTP server running: http://127.0.0.1:{}", httpPort);
            startPromise.complete();
        }).onFailure(startPromise::fail);
    }

    private void storeUpdate(Message<JsonObject> message) {
        lastData.put(message.body().getString("uuid"), message.body());
    }

    private void latestData(RoutingContext context) {
        LOG.info("Latest data requested from {}", context.request().remoteAddress());
        context.response()
            .setStatusCode(200)
            .putHeader("Content-Type", "application/json")
            .end(new JsonObject(lastData).encode());
    }

    private void fiveMinutes(RoutingContext context) {
        Future<JsonObject> future = breaker.execute(promise -> {
            webClient.get(storePort, storeHost, "/last-5-minutes")
                .expect(ResponsePredicate.SC_OK)
                .as(BodyCodec.jsonObject())
                .timeout(5_000)
                .send()
                .map(HttpResponse::body)
                .onSuccess(promise::complete)
                .onFailure(promise::fail);
        });

        future.onSuccess(json -> {
            LOG.info("Last 5 minutes data requested from {}", context.request().remoteAddress());
            cachedLastFiveMinutes = json;
            context.response()
                .setStatusCode(200)
                .putHeader("Content-Type", "application/json")
                .end(json.encode());
        }).onFailure(failure -> {
            LOG.info("Last 5 minutes data requested from {} and served from cache",
                    context.request().remoteAddress());
            if (cachedLastFiveMinutes != null) {
                context.response()
                    .setStatusCode(200)
                    .putHeader("Content-Type", "application/json")
                    .end(cachedLastFiveMinutes.encode());
            } else {
                LOG.error("Request failed", failure);
                context.fail(500);
            }
        });
    }
}
