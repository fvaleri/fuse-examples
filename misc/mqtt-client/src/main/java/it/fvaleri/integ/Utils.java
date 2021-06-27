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

public final class Utils {
    private static final Logger LOG = LoggerFactory.getLogger(Utils.class);
    private static final Random RND = new Random();

    private Utils() {
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
        if (Configuration.SSL_TRUSTSTORE_LOCATION != null) {
            System.setProperty("javax.net.ssl.trustStore", Configuration.SSL_TRUSTSTORE_LOCATION);
            System.setProperty("javax.net.ssl.trustStorePassword", Configuration.SSL_TRUSTSTORE_PASSWORD);
            if (Configuration.SSL_KEYSTORE_LOCATION != null) {
                System.setProperty("javax.net.ssl.keyStore", Configuration.SSL_KEYSTORE_LOCATION);
                System.setProperty("javax.net.ssl.keyStorePassword", Configuration.SSL_KEYSTORE_PASSWORD);
            }
        }
        String clientId = UUID.randomUUID().toString();
        // using the synchronous implementation
        String[] urls = Configuration.CONNECTION_URL.split(",");
        IMqttClient client = new MqttClient(urls[0], clientId, new MqttDefaultFilePersistence("/tmp"));
        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(urls);
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        options.setConnectionTimeout(10);
        if (Configuration.CONNECTION_USERNAME != null) {
            options.setUserName(Configuration.CONNECTION_USERNAME);
        }
        if (Configuration.CONNECTION_PASSWORD != null) {
            options.setPassword(Configuration.CONNECTION_PASSWORD.toCharArray());
        }
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
        for (long i = 0; i < Configuration.MESSAGE_SIZE_BYTES; i++) {
            sb.append(alphabet.charAt(RND.nextInt(alphabet.length())));
        }
        byte[] payload = sb.toString().getBytes();
        MqttMessage message = new MqttMessage(payload);
        message.setQos(Configuration.MESSAGE_QOS);
        message.setRetained(Configuration.MESSAGE_RETAINED);
        return message;
    }
}

