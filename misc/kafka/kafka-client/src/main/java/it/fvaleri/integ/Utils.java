package it.fvaleri.integ;

import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;

import io.apicurio.registry.utils.serde.*;
import io.apicurio.registry.utils.serde.strategy.FindBySchemaIdStrategy;
import io.apicurio.registry.utils.serde.strategy.SimpleTopicIdStrategy;
import io.strimzi.kafka.oauth.client.ClientConfig;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Utils {
    private static final Logger LOG = LoggerFactory.getLogger(Utils.class);
    private static final Random RANDOM = new Random();

    private Utils() {
    }

    public static String getSaslPlainJaasConfig() {
        String jaasConfig = "org.apache.kafka.common.security.plain.PlainLoginModule required\nusername=\""
                + Configuration.SASL_USERNAME + "\" password=\"" + Configuration.SASL_PASSWORD + "\";";
        return jaasConfig;
    }

    public static String getSaslScramJaasConfig() {
        String jaasConfig = "org.apache.kafka.common.security.scram.ScramLoginModule required\nusername=\""
                + Configuration.SASL_USERNAME + "\" password=\"" + Configuration.SASL_PASSWORD + "\";";
        return jaasConfig;
    }

    public static String getSaslOauthJaasConfig() {
        final String jaasConfig = "org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required;";
        return jaasConfig;
    }

    public static void sleep(long millis) {
        try {
            TimeUnit.MILLISECONDS.sleep(millis);
        } catch (InterruptedException e) {
        }
    }

    public static String createMessage() {
        LOG.debug("Creating message");
        StringBuilder sb = new StringBuilder();
        String alphabet = "ACGT";
        for (long i = 0; i < Configuration.MESSAGE_SIZE_BYTES; i++) {
            sb.append(alphabet.charAt(RANDOM.nextInt(alphabet.length())));
        }
        return sb.toString();
    }

    public static File getResourceAsFile(String name) {
        LOG.debug("Getting resource {}", name);
        ClassLoader classLoader = Utils.class.getClassLoader();
        URL resource = classLoader.getResource(name);
        if (resource == null) {
            throw new IllegalArgumentException(String.format("Resource %s not found", name));
        } else {
            return new File(resource.getFile());
        }
    }

    public static Properties createProducerConfig() throws IllegalArgumentException {
        Properties config = new Properties();

        // JMX metrics (kafka.producer:type=producer-metrics,client-id="{client-id}")
        String clientId = "producer-" + System.currentTimeMillis();
        config.put(ProducerConfig.CLIENT_ID_CONFIG, clientId);
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, Configuration.BOOTSTRAP_SERVERS);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // durability/availability tradeoff
        // by default values are optimized for availability and latency, so if durability
        // is more important you have to tune it (i.e. acks=all, min.insync.replicas=3).
        config.put(ProducerConfig.ACKS_CONFIG, Configuration.PRODUCER_ACKS);

        // throughput/latency tradeoff (batching)
        // buffering: must accomodate batching, compression and in-flight requests
        config.put(ProducerConfig.BATCH_SIZE_CONFIG, 16_384);
        config.put(ProducerConfig.LINGER_MS_CONFIG, 0);
        config.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33_554_432);
        config.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, Configuration.PRODUCER_COMPRESSION);
        config.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, Integer.MAX_VALUE);

        // ordering
        // set (idemp,inflight) = (false,1) or (true,>1) to preserve ordering in case of retries
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, false);
        config.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);

        // transactions
        // exactly once semantics (EOS) with indempotence and two-phase commmit protocol
        /*config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        config.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, clientId + "-tid");
        config.put(ProducerConfig.TRANSACTION_TIMEOUT_CONFIG, 900_000);*/

        // reconnections
        config.put(ProducerConfig.RECONNECT_BACKOFF_MAX_MS_CONFIG, 5_000);
        config.put(ProducerConfig.RECONNECT_BACKOFF_MS_CONFIG, 1_000);

        // retries
        config.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 60_000);
        config.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 2_000);
        config.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);

        // registry
        if (Configuration.REGISTRY_URL != null) {
            config.put(AbstractKafkaSerDe.REGISTRY_URL_CONFIG_PARAM, Configuration.REGISTRY_URL);
            // simple schema lookup by topic name strategy
            config.put(AbstractKafkaSerializer.REGISTRY_ARTIFACT_ID_STRATEGY_CONFIG_PARAM,
                    SimpleTopicIdStrategy.class.getName());
            config.put(AbstractKafkaSerializer.REGISTRY_GLOBAL_ID_STRATEGY_CONFIG_PARAM,
                    FindBySchemaIdStrategy.class.getName());
            config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            switch (Configuration.SCHEMA_FORMAT) {
                case "avro":
                    config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, AvroKafkaSerializer.class.getName());
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported schema type");
            }
        }

        config.putAll(createSharedConfig());
        return config;
    }

    public static Properties createConsumerConfig() throws IllegalArgumentException {
        Properties config = new Properties();

        // JMX metrics (kafka.consumer:type=consumer-fetch-manager-metrics,client-id="{client-id}")
        String clientId = "consumer-" + System.currentTimeMillis();
        config.put(ConsumerConfig.CLIENT_ID_CONFIG, clientId);
        // every client with the same group.id will be part of the same consumer group
        config.put(ConsumerConfig.GROUP_ID_CONFIG, Configuration.CONSUMER_GROUP_ID);
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, Configuration.BOOTSTRAP_SERVERS);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, Configuration.CONSUMER_OFFSET);

        // throughput/latency tradeoff (batching)
        // increase the min amount of data fetched in a request to improve throughput
        // increase max batch size to reduce latency (max.partition.fetch.bytes > max.message.bytes)
        config.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 16_384);
        config.put(ConsumerConfig.FETCH_MAX_BYTES_CONFIG, 52_428_800);
        config.put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, 1_048_576);
        config.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);

        // auto commit
        // if true, the offset is committed periodically with loss and duplicates possible
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        config.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 1_000);

        // transactions
        // set read_committed isolation to read only committed records
        /*config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        config.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, IsolationLevel.READ_COMMITTED);*/

        // rebalances
        // increasing the number of heartbeats reduces the likelihood of unnecessary rebalances
        config.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 10_000);
        config.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 3_000);
        // also insufficient poll interval may cause unnecessary rebalancing
        config.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300_000);
        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500);

        // reconnections
        config.put(ConsumerConfig.RECONNECT_BACKOFF_MAX_MS_CONFIG, 5_000);
        config.put(ConsumerConfig.RECONNECT_BACKOFF_MS_CONFIG, 1_000);

        // retries
        config.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, 60_000);
        config.put(ConsumerConfig.RETRY_BACKOFF_MS_CONFIG, 2_000);

        // registry
        if (Configuration.REGISTRY_URL != null) {
            config.put(AbstractKafkaSerDe.REGISTRY_URL_CONFIG_PARAM, Configuration.REGISTRY_URL);
            config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            switch (Configuration.SCHEMA_FORMAT) {
                case "avro":
                    config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, AvroKafkaDeserializer.class.getName());
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported schema type");
            }
        }

        config.putAll(createSharedConfig());
        return config;
    }

    private static Map<String, String> createSharedConfig() {
        Map<String, String> config = new HashMap<>();
        config.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, Configuration.SECURITY_PROTOCOL);

        if (Configuration.SSL_TRUSTSTORE_LOCATION != null) {
            config.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, Configuration.SSL_TRUSTSTORE_LOCATION);
            config.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG,Configuration.SSL_TRUSTSTORE_PASSWORD);
            if (Configuration.SSL_KEYSTORE_LOCATION != null) {
                config.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, Configuration.SSL_KEYSTORE_LOCATION);
                config.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, Configuration.SSL_KEYSTORE_LOCATION);
            }
        }

        if (Configuration.SASL_MECHANISM != null) {
            config.put(SaslConfigs.SASL_MECHANISM, Configuration.SASL_MECHANISM);
            switch (Configuration.SASL_MECHANISM) {
                case "PLAIN":
                    config.put(SaslConfigs.SASL_JAAS_CONFIG, Utils.getSaslPlainJaasConfig());
                    break;
                case "SCRAMSHA512":
                    config.put(SaslConfigs.SASL_JAAS_CONFIG, Utils.getSaslScramJaasConfig());
                    break;
                case "OAUTHBEARER":
                    config.put(SaslConfigs.SASL_JAAS_CONFIG, Utils.getSaslOauthJaasConfig());
                    config.put(SaslConfigs.SASL_LOGIN_CALLBACK_HANDLER_CLASS, Configuration.SASL_OAUTH_CALLBACK_HANDLER);
                    System.setProperty(ClientConfig.OAUTH_TOKEN_ENDPOINT_URI, Configuration.SASL_OAUTH_TOKEN_ENDPOINT_URI);
                    System.setProperty(ClientConfig.OAUTH_CLIENT_ID, Configuration.SASL_OAUTH_CLIENT_ID);
                    System.setProperty(ClientConfig.OAUTH_CLIENT_SECRET, Configuration.SASL_OAUTH_CLIENT_SECRET);
                    System.setProperty(ClientConfig.OAUTH_SCOPE, Configuration.SASL_OAUTH_SCOPE);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown SASL mechanism");
            }
        }
        return config;
    }
}
