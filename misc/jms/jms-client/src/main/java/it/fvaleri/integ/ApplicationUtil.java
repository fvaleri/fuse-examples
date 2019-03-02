package it.fvaleri.integ;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

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

    public static Connection openConnection() throws Exception {
        LOG.debug("Connecting to server");
        if (ConfigurationUtil.getSslTruststoreLocation() != null) {
            System.setProperty("javax.net.ssl.trustStore", ConfigurationUtil.getSslTruststoreLocation());
            System.setProperty("javax.net.ssl.trustStorePassword", ConfigurationUtil.getSslTruststorePassword());
            if (ConfigurationUtil.getSslKeystoreLocation() != null) {
                System.setProperty("javax.net.ssl.keyStore", ConfigurationUtil.getSslKeystoreLocation());
                System.setProperty("javax.net.ssl.keyStorePassword", ConfigurationUtil.getSslKeystorePassword());
            }
        }
        ConnectionFactory factory = null;
        String url = ConfigurationUtil.getConnectionUrl();
        switch (ConfigurationUtil.getConnectionFactoryType()) {
            case OPENWIRE:
                factory = new org.apache.activemq.ActiveMQConnectionFactory(url);
                break;
            case CORE:
                factory = new org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory(url);
                break;
            case AMQP:
                factory = new org.apache.qpid.jms.JmsConnectionFactory(url);
                break;
            default:
                throw new IllegalArgumentException("Unknown ConnectionFactory type");
        }
        Connection connection = factory.createConnection(ConfigurationUtil.getConnectionUsername(),
                ConfigurationUtil.getConnectionPassword()); // thread-safe
        if (ConfigurationUtil.getClientId() != null) {
            connection.setClientID(ConfigurationUtil.getClientId());
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
        if (ConfigurationUtil.getQueue() != null) {
            return session.createQueue(ConfigurationUtil.getQueue());
        } else if (ConfigurationUtil.getTopic() != null) {
            return session.createTopic(ConfigurationUtil.getTopic());
        } else {
            throw new IllegalArgumentException("Destination not found");
        }
    }

    public static Message createMessage(Session session) throws JMSException {
        LOG.debug("Creating message");
        StringBuilder sb = new StringBuilder();
        String alphabet = "acgt";
        long length = ConfigurationUtil.getMessageSizeBytes();
        for (long i = 0; i < length; i++) {
            sb.append(alphabet.charAt(RND.nextInt(alphabet.length())));
        }
        return session.createTextMessage(sb.toString());
    }

    public static enum ConnectionFactoryType {
        OPENWIRE("openwire"), CORE("core"), AMQP("amqp");

        private final String id;

        private ConnectionFactoryType(String id) {
            this.id = id;
        }

        @Override
        public String toString() {
            return id;
        }
    }
}

