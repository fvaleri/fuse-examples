package it.fvaleri.integ;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.apicurio.registry.utils.serde.AbstractKafkaSerDe;
import io.apicurio.registry.utils.serde.AbstractKafkaSerializer;
import io.apicurio.registry.utils.serde.AvroKafkaDeserializer;
import io.apicurio.registry.utils.serde.AvroKafkaSerializer;
import io.apicurio.registry.utils.serde.JsonSchemaKafkaDeserializer;
import io.apicurio.registry.utils.serde.JsonSchemaKafkaSerializer;
import io.apicurio.registry.utils.serde.ProtobufKafkaDeserializer;
import io.apicurio.registry.utils.serde.ProtobufKafkaSerializer;
import io.apicurio.registry.utils.serde.strategy.FindBySchemaIdStrategy;
import io.apicurio.registry.utils.serde.strategy.SimpleTopicIdStrategy;
import io.strimzi.kafka.oauth.client.ClientConfig;

public final class ApplicationUtil {
    private static final Logger LOG = LoggerFactory.getLogger(ApplicationUtil.class);
    private static Random RND = new Random();

    private ApplicationUtil() {
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
        long length = ConfigurationUtil.getMessageSizeBytes();
        for (long i = 0; i < length; i++) {
            sb.append(alphabet.charAt(RND.nextInt(alphabet.length())));
        }
        return sb.toString();
    }

    public static File getResourceAsFile(String name) {
        LOG.debug("Getting resource {}", name);
        ClassLoader classLoader = ApplicationUtil.class.getClassLoader();
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
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, ConfigurationUtil.getBootstrapServers());
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // durability/availability tradeoff
        // by default values are optimized for availability and latency, so if durability
        // is more important you have to tune it (i.e. acks=all, min.insync.replicas=3).
        config.put(ProducerConfig.ACKS_CONFIG, ConfigurationUtil.getProducerAcks());

        // throughput/latency tradeoff (batching)
        // buffering: must accomodate batching, compression and in-flight requests
        config.put(ProducerConfig.BATCH_SIZE_CONFIG, 16_384);
        config.put(ProducerConfig.LINGER_MS_CONFIG, 0);
        config.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33_554_432);
        config.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, ConfigurationUtil.getProducerCompression());
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
        if (ConfigurationUtil.getRegistryUrl() != null) {
            config.put(AbstractKafkaSerDe.REGISTRY_URL_CONFIG_PARAM, ConfigurationUtil.getRegistryUrl());
            // simple schema lookup by topic name strategy
            config.put(AbstractKafkaSerializer.REGISTRY_ARTIFACT_ID_STRATEGY_CONFIG_PARAM,
                    SimpleTopicIdStrategy.class.getName());
            config.put(AbstractKafkaSerializer.REGISTRY_GLOBAL_ID_STRATEGY_CONFIG_PARAM,
                    FindBySchemaIdStrategy.class.getName());
            config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            switch (ConfigurationUtil.getSchemaFormat()) {
            case JSON:
                config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSchemaKafkaSerializer.class.getName());
            case AVRO:
                config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, AvroKafkaSerializer.class.getName());
                break;
            case PROTOBUF:
                config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ProtobufKafkaSerializer.class.getName());
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
        config.put(ConsumerConfig.GROUP_ID_CONFIG, ConfigurationUtil.getConsumerGroupId());
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, ConfigurationUtil.getBootstrapServers());
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, ConfigurationUtil.getConsumerOffset());

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
        if (ConfigurationUtil.getRegistryUrl() != null) {
            config.put(AbstractKafkaSerDe.REGISTRY_URL_CONFIG_PARAM, ConfigurationUtil.getRegistryUrl());
            config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            switch (ConfigurationUtil.getSchemaFormat()) {
            case JSON:
                config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonSchemaKafkaDeserializer.class.getName());
            case AVRO:
                config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, AvroKafkaDeserializer.class.getName());
                break;
            case PROTOBUF:
                config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ProtobufKafkaDeserializer.class.getName());
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
        config.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, ConfigurationUtil.getSecurityProtocol());

        if (ConfigurationUtil.getSslTruststoreLocation() != null) {
            config.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, ConfigurationUtil.getSslTruststoreLocation());
            config.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, ConfigurationUtil.getSslTruststorePassword());
            if (ConfigurationUtil.getSslKeystoreLocation() != null) {
                config.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, ConfigurationUtil.getSslKeystoreLocation());
                config.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, ConfigurationUtil.getSslKeystorePassword());
            }
        }

        if (ConfigurationUtil.getSaslMechanism() != null) {
            config.put(SaslConfigs.SASL_MECHANISM, ConfigurationUtil.getSaslMechanism().toString());
            switch (ConfigurationUtil.getSaslMechanism()) {
                case PLAIN:
                    config.put(SaslConfigs.SASL_JAAS_CONFIG, ConfigurationUtil.getSaslPlainJaasConfig());
                    break;
                case SCRAMSHA512:
                    config.put(SaslConfigs.SASL_JAAS_CONFIG, ConfigurationUtil.getSaslScramJaasConfig());
                    break;
                case OAUTHBEARER:
                    config.put(SaslConfigs.SASL_JAAS_CONFIG, ConfigurationUtil.getSaslOauthJaasConfig());
                    config.put(SaslConfigs.SASL_LOGIN_CALLBACK_HANDLER_CLASS, ConfigurationUtil.getSaslOauthCallbackHandler());
                    System.setProperty(ClientConfig.OAUTH_TOKEN_ENDPOINT_URI, ConfigurationUtil.getSaslOauthTokenEndpointUri());
                    System.setProperty(ClientConfig.OAUTH_CLIENT_ID, ConfigurationUtil.getSaslOauthClientId());
                    System.setProperty(ClientConfig.OAUTH_CLIENT_SECRET, ConfigurationUtil.getSaslOauthClientSecret());
                    System.setProperty(ClientConfig.OAUTH_SCOPE, ConfigurationUtil.getSaslOauthScope());
                    break;
                default:
                    throw new IllegalArgumentException("Unknown SASL mechanism");
            }

        }
        return config;
    }

    public static enum SaslMechanism {
        PLAIN("PLAIN"), SCRAMSHA512("SCRAMSHA512"), OAUTHBEARER("OAUTHBEARER");

        private final String id;

        private SaslMechanism(String id) {
            this.id = id;
        }

        @Override
        public String toString() {
            return id;
        }
    }

    public static enum SchemaFormat {
        JSON("json"), AVRO("avro"), PROTOBUF("protobuf");

        private final String id;

        private SchemaFormat(String id) {
            this.id = id;
        }

        @Override
        public String toString() {
            return id;
        }
    }
}

