package it.fvaleri.integ;

import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.fvaleri.integ.ApplicationUtil.SaslMechanism;
import it.fvaleri.integ.ApplicationUtil.SchemaFormat;

public final class ConfigurationUtil {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationUtil.class);
    private static final String FILE = "/application.properties";
    private static Properties prop;

    static {
        try (InputStream stream = ConfigurationUtil.class.getResourceAsStream(FILE)) {
            prop = new Properties();
            prop.load(stream);
            LOG.debug("Configuration: {}", prop);
        } catch (Exception e) {
            LOG.error("Configuration error", e);
        }
    }

    private ConfigurationUtil() {
    }

    public static String getBootstrapServers() {
        return prop.getProperty("bootstrap.servers");
    }

    public static String getSecurityProtocol() {
        return prop.getProperty("security.protocol", "PLAINTEXT");
    }

    public static String getSslTruststoreLocation() {
        return prop.getProperty("ssl.truststore.location");
    }

    public static String getSslTruststorePassword() {
        return prop.getProperty("ssl.truststore.password");
    }

    public static String getSslKeystoreLocation() {
        return prop.getProperty("ssl.keystore.location");
    }

    public static String getSslKeystorePassword() {
        return prop.getProperty("ssl.keystore.password");
    }

    public static SaslMechanism getSaslMechanism() {
        return SaslMechanism.valueOf(prop.getProperty("sasl.mechanism").toUpperCase());
    }

    public static String getSaslPlainJaasConfig() {
        final String username = prop.getProperty("sasl.scram.username");
        final String password = prop.getProperty("sasl.scram.password");
        final String jaasConfig = "org.apache.kafka.common.security.plain.PlainLoginModule required\nusername=\""
                + username + "\" password=\"" + password + "\";";
        return jaasConfig;
    }

    public static String getSaslScramJaasConfig() {
        final String username = prop.getProperty("sasl.scram.username");
        final String password = prop.getProperty("sasl.scram.password");
        final String jaasConfig = "org.apache.kafka.common.security.scram.ScramLoginModule required\nusername=\""
                + username + "\" password=\"" + password + "\";";
        return jaasConfig;
    }

    public static String getSaslOauthJaasConfig() {
        final String jaasConfig = "org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required;";
        return jaasConfig;
    }

    public static String getSaslOauthCallbackHandler() {
        return prop.getProperty("sasl.oauth.callback.handler");
    }

    public static String getSaslOauthTokenEndpointUri() {
        return prop.getProperty("sasl.oauth.token.endpoint.uri");
    }

    public static String getSaslOauthClientId() {
        return prop.getProperty("sasl.oauth.client.id");
    }

    public static String getSaslOauthClientSecret() {
        return prop.getProperty("sasl.oauth.client.secret");
    }

    public static String getSaslOauthScope() {
        return prop.getProperty("sasl.oauth.scope");
    }

    public static String getTopics() {
        return prop.getProperty("topics");
    }

    public static long getMessageSizeBytes() {
        return Integer.parseInt(prop.getProperty("message.size.bytes", "100"));
    }

    public static long getProcessingDelayMs() {
        return Long.parseLong(prop.getProperty("precessing.delay.ms", "0"));
    }

    public static String getRegistryUrl() {
        return prop.getProperty("registry.url");
    }

    public static SchemaFormat getSchemaFormat() {
        return SchemaFormat.valueOf(prop.getProperty("schema.format").toUpperCase());
    }

    public static String getProducerId() {
        return prop.getProperty("producer.id");
    }

    public static String getProducerAcks() {
        return prop.getProperty("producer.acks", "1");
    }

    public static String getProducerCompression() {
        return prop.getProperty("producer.compression", "none");
    }

    public static String getConsumerId() {
        return prop.getProperty("consumer.id");
    }

    public static String getConsumerGroupId() {
        return prop.getProperty("consumer.group.id");
    }

    public static String getConsumerOffset() {
        return prop.getProperty("consumer.offset", "earliest");
    }
}

