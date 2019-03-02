package it.fvaleri.integ;

import java.util.Random;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

/**
 * Each verticle runs in its own event-loop thread, which shouldn't be blocked.
 */
public class SensorVerticle extends AbstractVerticle {
    private static final Logger LOG = LoggerFactory.getLogger(Application.class);
    private static final int httpPort = Integer.parseInt(System.getenv().getOrDefault("HTTP_PORT", "8080"));

    private final String uuid = UUID.randomUUID().toString();
    private double temperature = 21.0;
    private final Random random = new Random();

    @Override
    public void start(Promise<Void> startPromise) {
        vertx.setPeriodic(2_000, tick -> updateTemperature());

        // HTTP endpoints
        Router router = Router.router(vertx);
        router.get("/data").handler(this::getData);

        vertx.createHttpServer()
            .requestHandler(router)
            .listen(httpPort)
            .onSuccess(ok -> {
                LOG.info("HTTP server listening on http://127.0.0.1:{}", httpPort);
                startPromise.complete();
            }).onFailure(startPromise::fail);
    }

    private void updateTemperature() {
        temperature = temperature + (random.nextGaussian() / 2.0d);
        LOG.info("Temperature of sensor {} is {}", uuid, temperature);
        vertx.eventBus().publish("temperature.updates", updateData());
    }

    private void getData(RoutingContext context) {
        LOG.info("Data request from {}", context.request().remoteAddress());
        context.response()
            .putHeader("Content-Type", "application/json")
            .setStatusCode(200)
            .end(updateData().encode());
    }

    private JsonObject updateData() {
        return new JsonObject()
            .put("uuid", uuid)
            .put("temperature", temperature)
            .put("timestamp", System.currentTimeMillis());
    }
}
