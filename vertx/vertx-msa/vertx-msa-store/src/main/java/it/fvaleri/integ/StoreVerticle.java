package it.fvaleri.integ;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;

/**
 * Each verticle runs in its own event-loop thread, which shouldn't be blocked.
 */
public class StoreVerticle extends AbstractVerticle {
    private static final Logger LOG = LoggerFactory.getLogger(Application.class);
    private static final int httpPort = Integer.parseInt(System.getenv().getOrDefault("HTTP_PORT", "7_000"));

    private PgPool pgPool;

    @Override
    public void start(Promise<Void> startPromise) {
        // using Postgres search_path property to set default schema
        pgPool = PgPool.pool(vertx, new PgConnectOptions()
            .setHost("127.0.0.1")
            .setDatabase("sensorsdb")
            .setUser("sensorsus")
            .setPassword("sensorsus")
            .setProperties(Map.of("search_path", "sensorssc")), new PoolOptions());

        vertx.eventBus().<JsonObject>consumer("temperature.updates", this::recordTemperature);

        Router router = Router.router(vertx);
        router.get("/all").handler(this::getAllData);
        router.get("/for/:uuid").handler(this::getData);
        router.get("/last-5-minutes").handler(this::getLastFiveMinutes);

        vertx.createHttpServer().requestHandler(router).listen(httpPort).onSuccess(ok -> {
            LOG.info("HTTP server running: http://127.0.0.1:{}", httpPort);
            startPromise.complete();
        }).onFailure(startPromise::fail);
    }

    private void recordTemperature(Message<JsonObject> message) {
        JsonObject body = message.body();
        String query = "insert into temperature_records(uuid, tstamp, value) values ($1, $2, $3);";
        String uuid = body.getString("uuid");
        OffsetDateTime timestamp = OffsetDateTime.ofInstant(Instant.ofEpochMilli(body.getLong("timestamp")),
                ZoneId.systemDefault());
        Double temperature = body.getDouble("temperature");
        Tuple tuple = Tuple.of(uuid, timestamp, temperature);
        pgPool.preparedQuery(query).execute(tuple)
            .onSuccess(row -> LOG.info("Recorded {}", tuple.deepToString()))
            .onFailure(failure -> LOG.error("Recording failed", failure));
    }

    private void getAllData(RoutingContext context) {
        LOG.info("Requesting all data from {}", context.request().remoteAddress());
        String query = "select * from temperature_records";
        pgPool.preparedQuery(query).execute().onSuccess(rows -> {
            JsonArray data = new JsonArray();
            for (Row row : rows) {
                data.add(new JsonObject()
                    .put("uuid", row.getValue("uuid"))
                    .put("timestamp", row.getValue("tstamp").toString())
                    .put("value", row.getValue("value")));
            }
            context.response()
                .setStatusCode(200)
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject()
                    .put("data", data).encode());
        }).onFailure(failure -> {
            LOG.error("Query failed", failure);
            context.fail(500);
        });
    }

    private void getData(RoutingContext context) {
        String query = "select tstamp, value from temperature_records where uuid = $1";
        String uuid = context.request().getParam("uuid");
        LOG.info("Requesting the data for {} from {}", uuid, context.request().remoteAddress());
        pgPool.preparedQuery(query).execute(Tuple.of(uuid)).onSuccess(rows -> {
            JsonArray data = new JsonArray();
            for (Row row : rows) {
                data.add(new JsonObject()
                    .put("timestamp", row.getValue("tstamp").toString())
                    .put("value", row.getValue("value")));
            }
            context.response()
                .setStatusCode(200)
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject()
                    .put("uuid", uuid)
                    .put("data", data).encode());
        }).onFailure(failure -> {
            LOG.error("Query failed", failure);
            context.fail(500);
        });
    }

    private void getLastFiveMinutes(RoutingContext context) {
        LOG.info("Requesting the data from the last 5 minutes from {}", context.request().remoteAddress());
        String query = "select * from temperature_records where tstamp >= now() - INTERVAL '5 minutes'";
        pgPool.preparedQuery(query).execute().onSuccess(rows -> {
            JsonArray data = new JsonArray();
            for (Row row : rows) {
                data.add(new JsonObject()
                    .put("uuid", row.getValue("uuid"))
                    .put("timestamp", row.getValue("tstamp").toString())
                    .put("value", row.getValue("value")));
            }
            context.response()
                .setStatusCode(200)
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject()
                    .put("data", data).encode());
        }).onFailure(failure -> {
            LOG.error("Query failed", failure);
            context.fail(500);
        });
    }
}
