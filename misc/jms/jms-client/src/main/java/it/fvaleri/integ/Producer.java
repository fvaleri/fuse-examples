package it.fvaleri.integ;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.Message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Producer implements Runnable, ExceptionListener {
    private static final Logger LOG = LoggerFactory.getLogger(Producer.class);
    private Connection connection = null;

    @Override
    public void run() {
        try {
            LOG.debug("Starting producer");
            connection = Utils.openConnection();
            connection.setExceptionListener(this);
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    Utils.closeConnection(connection);
                }
            });

            // transacted=false as we only have only one destination
            // AUTO_ACKNOWLEDGE as we are dealing with one message at a time
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination dest = Utils.createDestination(session);

            MessageProducer producer = session.createProducer(dest);
            producer.setDeliveryMode(Configuration.MESSAGE_DELIVERY);
            producer.setPriority(Configuration.MESSAGE_PRIORITY);
            producer.setTimeToLive(Configuration.MESSAGE_TTL_MS);

            Message message = Utils.createMessage(session);
            while (true) {
                // sync send by default, async send with TXs and non-pers messages
                producer.send(message);
                LOG.info("Sent message {}", message.getJMSMessageID());
                Utils.sleep(Configuration.PROCESSING_DELAY_MS);
            }
        } catch (Exception e) {
            LOG.error("Client error", e);
        }
    }

    @Override
    public void onException(JMSException e) {
        LOG.error("ExceptionListener error", e);
        try {
            LOG.info("Trying to restart");
            Utils.sleep(2_000);
            new Producer().run();
        } catch (Exception e1) {
            LOG.error("Reconnection failed", e1);
        }
    }
}
