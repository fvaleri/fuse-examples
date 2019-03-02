package it.fvaleri.integ;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;

/**
 * Enable clustering so that the non-persistent event-bus works across all processes.
 */
public class Application {
    private static final Logger LOG = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        Vertx.clusteredVertx(new VertxOptions())
            .onSuccess(vertx -> vertx.deployVerticle(new SensorVerticle())
                .onSuccess(id -> LOG.info("Application started"))
                .onFailure(failure -> LOG.error("Deployment failed", failure)))
            .onFailure(failure -> LOG.error("Application failed", failure));
    }
}
