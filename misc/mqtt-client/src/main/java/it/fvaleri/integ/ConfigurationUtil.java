package it.fvaleri.integ;

import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public static String[] getConnectionUrl() {
        String url = prop.getProperty("connection.url");
        return url.split(",");
    }

    public static String getConnectionUsername() {
        return prop.getProperty("connection.username");
    }

    public static char[] getConnectionPassword() {
        return prop.getProperty("connection.password").toCharArray();
    }

    public static String getTopic() {
        return prop.getProperty("topic", "my-topic");
    }

    public static long getProcessingDelayMs() {
        return Long.parseLong(prop.getProperty("precessing.delay.ms", "0"));
    }

    public static long getMessageSizeBytes() {
        return Long.parseLong(prop.getProperty("message.size.bytes", "100"));
    }

    public static int getMessageQos() {
        return Integer.parseInt(prop.getProperty("message.qos", "1"));
    }

    public static boolean getMessageRetained() {
        return Boolean.parseBoolean(prop.getProperty("message.retained", "true"));
    }
}

