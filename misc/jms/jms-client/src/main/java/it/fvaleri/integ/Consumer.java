package it.fvaleri.integ;

import java.util.concurrent.CountDownLatch;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicSubscriber;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Consumer implements Runnable, MessageListener, ExceptionListener {
    private static final Logger LOG = LoggerFactory.getLogger(Consumer.class);
    private CountDownLatch latch = new CountDownLatch(1);
    private Connection connection = null;
    private Session session = null;

    @Override
    public void run() {
        try {
            LOG.debug("Starting consumer");
            connection = ApplicationUtil.openConnection();
            connection.setExceptionListener(this);
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    ApplicationUtil.closeConnection(connection);
                }
            });

            // the ack mode is not relevant when using a transacted session
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination dest = ApplicationUtil.createDestination(session);

            // creating a consumer involves a network round trip to the broker
            if (ConfigurationUtil.getSubscriptionName() != null) {
                TopicSubscriber subscriber = session.createDurableSubscriber((Topic) dest,
                        ConfigurationUtil.getSubscriptionName(), ConfigurationUtil.getMessageSelector(), false);
                subscriber.setMessageListener(this);
            } else {
                MessageConsumer consumer = session.createConsumer(dest, ConfigurationUtil.getMessageSelector());
                consumer.setMessageListener(this);
            }

            // start consumption and wait indefinitely for new messages
            connection.start();
            latch.await();

        } catch (Exception e) {
            LOG.error("Client error", e);
        }
    }

    @Override
    public void onMessage(Message message) {
        try {
            // messages passed serially and acked only when this completes successfully
            LOG.info("Received message {}{}", message.getJMSMessageID(),
                    message.getJMSRedelivered() ? " (redelivered)" : "");
            ApplicationUtil.sleep(ConfigurationUtil.getProcessingDelayMs());

        } catch (Exception e) {
            LOG.error("MessageListener error:", e);
            try {
                if (session != null) {
                    // if transacted session there is no need to recover
                    session.recover();
                    LOG.debug("Session recovered");
                }
            } catch (JMSException e1) {
                LOG.error("Session recovery failed", e1);
            }
        }
    }

    @Override
    public void onException(JMSException e) {
        LOG.error("ExceptionListener error", e);
        try {
            LOG.info("Trying to restart");
            ApplicationUtil.sleep(2_000);
            new Consumer().run();
            latch.countDown();
        } catch (Exception e1) {
            LOG.error("Reconnection failed", e1);
        }
    }
}

