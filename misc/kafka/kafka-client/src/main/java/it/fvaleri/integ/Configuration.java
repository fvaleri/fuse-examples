package it.fvaleri.integ;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Configuration {
    private static final Logger LOG = LoggerFactory.getLogger(Configuration.class);
    private static final JsonNode FILE_DATA = loadConfigurationFile();
    private static final Map<String, String> CONFIG = new TreeMap<>();

    // parsed variables into constants
    public static final String BOOTSTRAP_SERVERS = getOrDefault("BOOTSTRAP_SERVERS", "localhost:9092");
    public static final String SECURITY_PROTOCOL = getOrDefault("SECURITY_PROTOCOL", "PLAINTEXT");
    public static final String SSL_TRUSTSTORE_LOCATION = getOrDefault("SSL_TRUSTSTORE_LOCATION", null);
    public static final String SSL_TRUSTSTORE_PASSWORD = getOrDefault("SSL_TRUSTSTORE_PASSWORD", null);
    public static final String SSL_KEYSTORE_LOCATION = getOrDefault("SSL_KEYSTORE_LOCATION", null);
    public static final String SSL_KEYSTORE_PASSWORD = getOrDefault("SSL_KEYSTORE_PASSWORD", null);
    public static final String SASL_MECHANISM = getOrDefault("SASL_MECHANISM", null);
    public static final String SASL_USERNAME = getOrDefault("SASL_USERNAME", null);
    public static final String SASL_PASSWORD = getOrDefault("SASL_PASSWORD", null);
    public static final String SASL_OAUTH_CALLBACK_HANDLER = getOrDefault("SASL_OAUTH_CALLBACK_HANDLER", null);
    public static final String SASL_OAUTH_TOKEN_ENDPOINT_URI = getOrDefault("SASL_OAUTH_TOKEN_ENDPOINT_URI", null);
    public static final String SASL_OAUTH_CLIENT_ID = getOrDefault("SASL_OAUTH_CLIENT_ID", null);
    public static final String SASL_OAUTH_CLIENT_SECRET = getOrDefault("SASL_OAUTH_CLIENT_SECRET", null);
    public static final String SASL_OAUTH_SCOPE = getOrDefault("SASL_OAUTH_SCOPE", null);
    public static final String TOPICS = getOrDefault("TOPICS", "my-topic");
    public static final long MESSAGE_SIZE_BYTES = getOrDefault("MESSAGE_SIZE_BYTES", Long::parseLong, 100L);
    public static final long PROCESSING_DELAY_MS = getOrDefault("PROCESSING_DELAY_MS", Long::parseLong, 1000L);
    public static final String REGISTRY_URL = getOrDefault("REGISTRY_URL", null);
    public static final String SCHEMA_FORMAT = getOrDefault("SCHEMA_FORMAT", null);
    public static final String PRODUCER_ACKS = getOrDefault("PRODUCER_ACKS", "1");
    public static final String PRODUCER_COMPRESSION = getOrDefault("PRODUCER_COMPRESSION", "none");
    public static final String CONSUMER_GROUP_ID = getOrDefault("CONSUMER_GROUP_ID", "my-consumer-group");
    public static final String CONSUMER_OFFSET = getOrDefault("CONSUMER_OFFSET", "earliest");

    // help methods
    private Configuration() {
    }

    static {
        LOG.debug("=======================================================");
        CONFIG.forEach((key, value) -> LOG.debug("{}: {}", key, value));
        LOG.debug("=======================================================");
    }

    private static JsonNode loadConfigurationFile() {
        String configFilePath = System.getenv().getOrDefault("CONFIG_FILE_PATH",
                Paths.get(System.getProperty("user.dir"), "config.json").toAbsolutePath().toString());
        ObjectMapper mapper = new ObjectMapper();
        try {
            File jsonFile = new File(configFilePath).getAbsoluteFile();
            LOG.info("File: {}", configFilePath);
            return mapper.readTree(jsonFile);
        } catch (IOException ex) {
            return mapper.createObjectNode();
        }
    }

    private static String getOrDefault(String varName, String defaultValue) {
        return getOrDefault(varName, String::toString, defaultValue);
    }

    private static <T> T getOrDefault(String key, Function<String, T> converter, T defaultValue) {
        String value = System.getenv(key) != null ?
                System.getenv(key) :
                (Objects.requireNonNull(FILE_DATA).get(key) != null ?
                        FILE_DATA.get(key).asText() :
                        null);
        T returnValue = defaultValue;
        if (value != null) {
            returnValue = converter.apply(value);
        }
        CONFIG.put(key, String.valueOf(returnValue));
        return returnValue;
    }
}
