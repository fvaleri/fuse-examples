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
    public static final String CONNECTION_FACTORY = getOrDefault("CONNECTION_FACTORY", "core");
    public static final String CONNECTION_URL = getOrDefault("CONNECTION_URL", "tcp://localhost:61616");
    public static final String CONNECTION_USERNAME = getOrDefault("CONNECTION_USERNAME", "admin");
    public static final String CONNECTION_PASSWORD = getOrDefault("CONNECTION_PASSWORD", "admin");
    public static final String SSL_TRUSTSTORE_LOCATION = getOrDefault("SSL_TRUSTSTORE_LOCATION", null);
    public static final String SSL_TRUSTSTORE_PASSWORD = getOrDefault("SSL_TRUSTSTORE_PASSWORD", null);
    public static final String SSL_KEYSTORE_LOCATION = getOrDefault("SSL_KEYSTORE_LOCATION", null);
    public static final String SSL_KEYSTORE_PASSWORD = getOrDefault("SSL_KEYSTORE_PASSWORD", null);
    public static final String DESTINATION_QUEUE = getOrDefault("DESTINATION_QUEUE", "TestQueue");
    public static final String DESTINATION_TOPIC = getOrDefault("DESTINATION_TOPIC", "TestTopic");
    public static final String CLIENT_ID = getOrDefault("CLIENT_ID", null);
    public static final String SUBSCRIPTION_NAME = getOrDefault("SUBSCRIPTION_NAME", null);
    public static final long MESSAGE_SIZE_BYTES = getOrDefault("MESSAGE_SIZE_BYTES", Long::parseLong, 100L);
    public static final long PROCESSING_DELAY_MS = getOrDefault("PROCESSING_DELAY_MS", Long::parseLong, 1000L);
    public static final String MESSAGE_SELECTOR = getOrDefault("MESSAGE_SELECTOR", null);
    public static final int MESSAGE_DELIVERY = getOrDefault("MESSAGE_DELIVERY", Integer::parseInt, 2); // persistent
    public static final int MESSAGE_PRIORITY = getOrDefault("MESSAGE_PRIORITY", Integer::parseInt, 4);
    public static final long MESSAGE_TTL_MS = getOrDefault("MESSAGE_TTL_MS", Long::parseLong, 0L);

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
