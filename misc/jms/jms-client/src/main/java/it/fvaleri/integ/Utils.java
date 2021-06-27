package it.fvaleri.integ;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.jms.*;

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

    public static Connection openConnection() throws Exception {
        LOG.debug("Connecting to server");
        if (Configuration.SSL_TRUSTSTORE_LOCATION != null) {
            System.setProperty("javax.net.ssl.trustStore", Configuration.SSL_TRUSTSTORE_LOCATION);
            System.setProperty("javax.net.ssl.trustStorePassword", Configuration.SSL_TRUSTSTORE_PASSWORD);
            if (Configuration.SSL_KEYSTORE_LOCATION != null) {
                System.setProperty("javax.net.ssl.keyStore", Configuration.SSL_KEYSTORE_LOCATION);
                System.setProperty("javax.net.ssl.keyStorePassword", Configuration.SSL_KEYSTORE_PASSWORD);
            }
        }
        ConnectionFactory factory = null;
        switch (Configuration.CONNECTION_FACTORY) {
            case "openwire":
                factory = new org.apache.activemq.ActiveMQConnectionFactory(Configuration.CONNECTION_URL);
                break;
            case "core":
                factory = new org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory(Configuration.CONNECTION_URL);
                break;
            case "amqp":
                factory = new org.apache.qpid.jms.JmsConnectionFactory(Configuration.CONNECTION_URL);
                break;
            default:
                throw new IllegalArgumentException("Unknown ConnectionFactory type");
        }
        Connection connection = factory.createConnection(
                Configuration.CONNECTION_USERNAME, Configuration.CONNECTION_PASSWORD); // thread-safe
        if (Configuration.CLIENT_ID != null) {
            connection.setClientID(Configuration.CLIENT_ID);
        }
        LOG.info("CONNECTED");
        return connection;
    }

    public static void closeConnection(Connection connection) {
        LOG.debug("Closing connection");
        if (connection != null) {
            try {
                connection.close();
            } catch (Exception e) {
                LOG.error("Connection close error: {}", e.getMessage());
            }
        }
    }

    public static Destination createDestination(Session session) throws JMSException {
        LOG.debug("Creating destination");
        if (Configuration.DESTINATION_QUEUE != null) {
            return session.createQueue(Configuration.DESTINATION_QUEUE);
        } else if (Configuration.DESTINATION_TOPIC != null) {
            return session.createTopic(Configuration.DESTINATION_TOPIC);
        } else {
            throw new IllegalArgumentException("Destination not found");
        }
    }

    public static Message createMessage(Session session) throws JMSException {
        LOG.debug("Creating message");
        StringBuilder sb = new StringBuilder();
        String alphabet = "acgt";
        for (long i = 0; i < Configuration.MESSAGE_SIZE_BYTES; i++) {
            sb.append(alphabet.charAt(RND.nextInt(alphabet.length())));
        }
        return session.createTextMessage(sb.toString());
    }
}
