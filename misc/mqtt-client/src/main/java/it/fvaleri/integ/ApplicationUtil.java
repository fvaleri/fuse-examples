package it.fvaleri.integ;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public static String shorten(String str) {
        return (str != null && str.length() > 30) ? str.substring(0, 31) + ".." : str;
    }

    public static IMqttClient openConnection() throws Exception {
        LOG.debug("Connecting to server");
        if (ConfigurationUtil.getSslTruststoreLocation() != null) {
            System.setProperty("javax.net.ssl.trustStore", ConfigurationUtil.getSslTruststoreLocation());
            System.setProperty("javax.net.ssl.trustStorePassword", ConfigurationUtil.getSslTruststorePassword());
            if (ConfigurationUtil.getSslKeystoreLocation() != null) {
                System.setProperty("javax.net.ssl.keyStore", ConfigurationUtil.getSslKeystoreLocation());
                System.setProperty("javax.net.ssl.keyStorePassword", ConfigurationUtil.getSslKeystorePassword());
            }
        }
        String clientId = UUID.randomUUID().toString();
        // using the synchronous implementation
        String[] urls = ConfigurationUtil.getConnectionUrl();
        IMqttClient client = new MqttClient(urls[0], clientId, new MqttDefaultFilePersistence("/tmp"));
        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(urls);
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        options.setConnectionTimeout(10);
        options.setUserName(ConfigurationUtil.getConnectionUsername());
        options.setPassword(ConfigurationUtil.getConnectionPassword());
        client.connect(options);
        LOG.info("CONNECTED");
        return client;
    }

    public static void closeConnection(IMqttClient client) {
        LOG.debug("Closing connection");
        if (client != null) {
            try {
                client.disconnect();
                client.close();
            } catch (Exception e) {
                LOG.error("Client disconnect error: {}", e.getMessage());
            }
        }
    }

    public static MqttMessage createMessage() {
        StringBuilder sb = new StringBuilder();
        String alphabet = "ACGT";
        long length = ConfigurationUtil.getMessageSizeBytes();
        for (long i = 0; i < length; i++) {
            sb.append(alphabet.charAt(RND.nextInt(alphabet.length())));
        }
        byte[] payload = sb.toString().getBytes();
        MqttMessage message = new MqttMessage(payload);
        message.setQos(ConfigurationUtil.getMessageQos());
        message.setRetained(ConfigurationUtil.getMessageRetained());
        return message;
    }
}

