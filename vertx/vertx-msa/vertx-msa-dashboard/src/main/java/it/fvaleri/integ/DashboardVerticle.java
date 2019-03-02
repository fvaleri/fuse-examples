package it.fvaleri.integ;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.SockJSBridgeOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;

/**
 * Each verticle runs in its own event-loop thread, which shouldn't be blocked.
 */
public class DashboardVerticle extends AbstractVerticle {
    private static final Logger LOG = LoggerFactory.getLogger(Application.class);
    private static final int httpPort = Integer.parseInt(System.getenv().getOrDefault("HTTP_PORT", "5_000"));

    @Override
    public void start(Promise<Void> startPromise) {
        Router router = Router.router(vertx);

        SockJSHandler sockJSHandler = SockJSHandler.create(vertx);
        SockJSBridgeOptions bridgeOptions = new SockJSBridgeOptions()
                .addOutboundPermitted(new PermittedOptions().setAddress("temperature.updates"));
        sockJSHandler.bridge(bridgeOptions);

        router.route("/eventbus/*").handler(sockJSHandler);
        router.route().handler(StaticHandler.create("webroot"));
        router.get("/*").handler(ctx -> ctx.reroute("/index.html"));

        vertx.createHttpServer()
            .requestHandler(router)
            .listen(httpPort)
            .onSuccess(ok -> {
                LOG.info("HTTP server listening on http://127.0.0.1:{}", httpPort);
                startPromise.complete();
            }).onFailure(startPromise::fail);
    }
}
