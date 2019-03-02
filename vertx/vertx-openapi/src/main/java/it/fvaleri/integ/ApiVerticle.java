package it.fvaleri.integ;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.SelfSignedCertificate;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.ext.web.validation.RequestParameters;

public class ApiVerticle extends AbstractVerticle {
    private static final Logger LOG = LoggerFactory.getLogger(ApiVerticle.class);
    private static final int PORT = 8443;

    private List<JsonObject> pets = new ArrayList<>(Arrays.asList(
        new JsonObject().put("id", 1).put("name", "Fufi").put("tag", "ABC"),
        new JsonObject().put("id", 2).put("name", "Garfield").put("tag", "XYZ"),
        new JsonObject().put("id", 3).put("name", "Puffa")
    ));

    private HttpServer server;

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        LOG.info("Starting the server");

        // contract-first APIs development (OpenAPI 3)
        RouterBuilder.create(this.vertx, "petstore.yaml")
            .onSuccess(routerBuilder -> {
                // maps the OpenAPI endpoint definitions to the service implementation we create
                // HTTP request validation is handled automatically
                addGetPetsHandler(routerBuilder);
                addPostPetHandler(routerBuilder);
                addGetPetHandler(routerBuilder);

                Router router = routerBuilder.createRouter();
                addErrorHandler(router, 404, "Not Found");
                addErrorHandler(router, 400, "Invalid Request");

                // generate the certificate for HTTPS
                SelfSignedCertificate cert = SelfSignedCertificate.create();

                // start the HTTP/2 server with the OpenAPI router
                this.server = vertx.createHttpServer(new HttpServerOptions()
                    .setSsl(true)
                    .setUseAlpn(true)
                    .setKeyCertOptions(cert.keyCertOptions())
                    .setPort(PORT));

                LOG.info("Server listening on port {}", PORT);
                this.server.requestHandler(router).listen();

                startPromise.complete();
            })
            .onFailure(startPromise::fail);
    }

    @Override
    public void stop(){
        LOG.info("Stopping the server");
        this.server.close();
    }

    private void addGetPetsHandler(RouterBuilder routerBuilder) {
        final String operationId = "listPets";
        routerBuilder.operation(operationId).handler(routingContext -> {
            LOG.debug("Processing {} request from {}", operationId, routingContext.request().remoteAddress());
            routingContext
                .response()
                .setStatusCode(200)
                .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .end(new JsonArray(getAllPets()).encode());
            });
    }

    private List<JsonObject> getAllPets() {
        return this.pets;
    }

    private void addPostPetHandler(RouterBuilder routerBuilder) {
        final String operationId = "createPets";
        routerBuilder.operation(operationId).handler(routingContext -> {
            LOG.debug("Processing {} request from {}", operationId, routingContext.request().remoteAddress());
            JsonObject pet = routingContext.getBodyAsJson();
            addPet(pet);
            vertx.eventBus().publish("pet.updates", pet);
            routingContext
                .response()
                .setStatusCode(200)
                .end();
        });
    }

    private void addPet(JsonObject pet) {
        this.pets.add(pet);
    }

    private void addGetPetHandler(RouterBuilder routerBuilder) {
        final String operationId = "showPetById";
        routerBuilder.operation(operationId).handler(routingContext -> {
            LOG.debug("Processing {} request from {}", operationId, routingContext.request().remoteAddress());
            RequestParameters params = routingContext.get("parsedParameters");
            Integer id = params.pathParameter("petId").getInteger();
            Optional<JsonObject> pet = getAllPets()
                .stream()
                .filter(p -> p.getInteger("id").equals(id))
                .findFirst();
            if (pet.isPresent()) {
                routingContext
                    .response()
                    .setStatusCode(200)
                    .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                    .end(pet.get().encode());
            } else {
                routingContext.fail(404, new Exception("Pet not found"));
            }
        });
    }

    private void addErrorHandler(Router router, int httpCode, String defaultMessage) {
        router.errorHandler(httpCode, routingContext -> {
            JsonObject errorObject = new JsonObject()
                .put("code", httpCode)
                .put("message",
                    (routingContext.failure() != null) ?
                        routingContext.failure().getMessage() :
                        defaultMessage
                );
            routingContext
                .response()
                .setStatusCode(httpCode)
                .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .end(errorObject.encode());
        });
    }
}
