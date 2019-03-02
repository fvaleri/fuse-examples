package it.fvaleri.integ;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Application {
    private static final Logger LOG = LoggerFactory.getLogger(Application.class);

    private static final String CONNECTION_URL = "tcp://localhost:61616?jms.watchTopicAdvisories=false";
    private static final String USERNAME = "admin";
    private static final String PASSWORD = "admin";
    private static final String QUEUE_NAME = "TestQueue";

    private static final Queue<Throwable> errors = new ConcurrentLinkedQueue<>();
    private static final CountDownLatch serverReady = new CountDownLatch(1);

    public static void main(String[] args) {
        try {
            final ExecutorService executor = Executors.newFixedThreadPool(2);
            executor.execute(new ServiceA());
            executor.execute(new ServiceB());
            executor.shutdown();
            executor.awaitTermination(60, TimeUnit.SECONDS);
            LOG.info("Errors: {}", errors);

        } catch (Exception e) {
            LOG.error("Unexpected error", e);
        }
    }

    static class ServiceA implements Runnable {
        @Override
        public void run() {
            Connection connection = null;
            try {
                String className = this.getClass().getSimpleName();
                ConnectionFactory factory = new ActiveMQConnectionFactory(CONNECTION_URL);
                connection = factory.createConnection(USERNAME, PASSWORD);
                connection.setExceptionListener(new ExceptionListener() {
                    @Override
                    public void onException(JMSException e) {
                        LOG.warn("[{}] Failure: {}", className, e.getMessage());
                    }
                });
                LOG.debug("[{}] Connected", className);
                connection.start();

                // do not use transacted session to avoid hang
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                Destination requestQueue = session.createQueue(QUEUE_NAME);
                Destination tempQueue = session.createTemporaryQueue();

                MessageProducer producer = session.createProducer(requestQueue);
                MessageConsumer consumer = session.createConsumer(tempQueue);

                TextMessage request = session.createTextMessage("ping");
                request.setJMSReplyTo(tempQueue);
                request.setJMSCorrelationID("id" + System.nanoTime());

                serverReady.await(20, TimeUnit.SECONDS);
                LOG.debug("[{}] Sending request: {}", className, request.getText());
                producer.send(request);

                LOG.debug("[{}] Waiting for reply", className);
                TextMessage reply = (TextMessage) consumer.receive();
                LOG.debug("[{}] Reply received: {}", className, reply.getText());

            } catch (Exception e) {
                errors.add(e);
            } finally {
                try {
                    connection.close();
                } catch (JMSException e) {
                }
            }
        }
    }

    static class ServiceB implements Runnable {
        @Override
        public void run() {
            Connection connection = null;
            try {
                String className = this.getClass().getSimpleName();
                ConnectionFactory cf = new org.apache.activemq.ActiveMQConnectionFactory(CONNECTION_URL);
                connection = cf.createConnection(USERNAME, PASSWORD);
                connection.setExceptionListener(new ExceptionListener() {
                    @Override
                    public void onException(JMSException e) {
                        LOG.warn("[{}] Failure: {}", className, e.getMessage());
                    }
                });
                LOG.debug("[{}] Connected", className);
                connection.start();

                // do not use transacted session to avoid hang
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                Destination requestQueue = session.createQueue(QUEUE_NAME);

                MessageConsumer consumer = session.createConsumer(requestQueue);
                MessageProducer producer = session.createProducer(null);

                LOG.debug("[{}] Waiting for request", className);
                serverReady.countDown();
                TextMessage request = (TextMessage) consumer.receive();
                LOG.debug("[{}] Request received: {}", className, request.getText());

                TextMessage reply = session.createTextMessage("pong");
                reply.setJMSCorrelationID(request.getJMSMessageID());
                LOG.debug("[{}] Sending reply: {}", className, reply.getText());
                producer.send(request.getJMSReplyTo(), reply);

            } catch (Exception e) {
                errors.add(e);
            } finally {
                try {
                    connection.close();
                } catch (JMSException e) {
                }
            }
        }
    }
}

