package it.fvaleri.integ;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jboss.ejb3.annotation.ResourceAdapter;

@ResourceAdapter(value = "activemq-ra")
@MessageDriven(name = "TestQueueMDB", activationConfig = {
        @ActivationConfigProperty(propertyName = "useJNDI", propertyValue = "true"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "java:/jms/queue/TestQueue"),
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge"),
        // number of parallel consumers (default 15, must be <= mdb-strict-max-pool.max-pool-size/derive-size)
        @ActivationConfigProperty(propertyName = "maxSession", propertyValue = "15"),
        // hA and rebalanceConnections to enable reconnection in case of clustering
        @ActivationConfigProperty(propertyName = "hA", propertyValue = "true"),
        @ActivationConfigProperty(propertyName = "rebalanceConnections", propertyValue = "true"),
        // if user authentication is enabled
        @ActivationConfigProperty(propertyName = "user", propertyValue = "admin"),
        @ActivationConfigProperty(propertyName = "password", propertyValue = "admin")
})
public class TestQueueMDB implements MessageListener {
    private static final Logger LOG = LoggerFactory.getLogger(TestQueueMDB.class);

    @Override
    public void onMessage(Message msg) {
        try {
            if (msg instanceof TextMessage) {
                TextMessage message = (TextMessage) msg;
                LOG.info("Received message {}", message.getJMSMessageID());
            } else {
                LOG.warn("Wrong type: {}", msg.getClass().getName());
            }
        } catch (JMSException e) {
            LOG.error("MDB error", e);
            throw new RuntimeException(e);
        }
    }
}

