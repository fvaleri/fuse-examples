package it.fvaleri.integ;

import java.io.InputStream;
import java.util.Properties;

import javax.jms.DeliveryMode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.fvaleri.integ.ApplicationUtil.ConnectionFactoryType;

public final class ConfigurationUtil {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationUtil.class);
    private static final String FILE = "/application.properties";
    private static Properties prop;

    static {
        try (InputStream stream = ConfigurationUtil.class.getResourceAsStream(FILE)) {
            prop = new Properties();
            prop.load(stream);
            LOG.debug("Properties: {}", prop);
        } catch (Exception e) {
            LOG.error("Properties load error", e);
        }
    }

    private ConfigurationUtil() {
    }

    public static ConnectionFactoryType getConnectionFactoryType() {
        return ConnectionFactoryType.valueOf(
            prop.getProperty("connection.factory").toUpperCase());
    }

    public static String getConnectionUrl() {
        return prop.getProperty("connection.url");
    }

    public static String getConnectionUsername() {
        return prop.getProperty("connection.username");
    }

    public static String getConnectionPassword() {
        return prop.getProperty("connection.password");
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

    public static String getQueue() {
        return prop.getProperty("destination.queue");
    }

    public static String getTopic() {
        return prop.getProperty("destination.topic");
    }

    public static String getClientId() {
        return prop.getProperty("client.id");
    }

    public static String getSubscriptionName() {
        return prop.getProperty("subscription.name");
    }

    public static long getProcessingDelayMs() {
        return Long.parseLong(prop.getProperty("precessing.delay.ms", "0"));
    }

    public static String getMessageSelector() {
        return prop.getProperty("message.selector");
    }

    public static long getMessageSizeBytes() {
        return Integer.parseInt(prop.getProperty("message.size.bytes", "100"));
    }

    public static int getMessageDelivery() {
        switch (prop.getProperty("message.delivery", "2")) {
            case "1":
                return DeliveryMode.NON_PERSISTENT;
            case "2":
                return DeliveryMode.PERSISTENT;
            default:
                throw new IllegalArgumentException("Unknown delivery mode");
        }
    }

    public static int getMessagePriority() {
        return Integer.parseInt(prop.getProperty("message.priority", "4"));
    }

    public static long getMessageTtlMs() {
        return Long.parseLong(prop.getProperty("message.ttl.ms", "0"));
    }
}

