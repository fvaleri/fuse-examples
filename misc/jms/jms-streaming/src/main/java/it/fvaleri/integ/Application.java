package it.fvaleri.integ;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Application {
    private static final Logger LOG = LoggerFactory.getLogger(Application.class);
    private static final long FILE_SIZE = 2L * 1024 * 1024 * 1024; // 2 GiB message

    private static final String CONNECTION_URL = "tcp://localhost:61616?minLargeMessageSize=10240";
    private static final String USERNAME = "admin";
    private static final String PASSWORD = "admin";
    private static final String QUEUE_NAME = "TestQueue";

    public static void main(String[] args) {
        try {
            long maxMemory = Runtime.getRuntime().maxMemory();
            LOG.info("Configured JVM max memory (Xmx): {} bytes", maxMemory);

            File inputFile = new File("target/huge-message.dat");
            createFile(inputFile, FILE_SIZE);
            LOG.info("Test file with {} bytes created", FILE_SIZE);

            // large message streaming is only supported by Core (this example) and AMQP protocols
            ConnectionFactory factory = new ActiveMQConnectionFactory(CONNECTION_URL);
            Connection connection = factory.createConnection(USERNAME, PASSWORD);
            connection.setExceptionListener(new ExceptionListener() {
                @Override
                public void onException(JMSException e) {
                    LOG.warn("Failure: {}", e.getMessage());
                }
            });
            LOG.info("CONNECTED");

            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination destination = session.createQueue(QUEUE_NAME);
            MessageProducer producer = session.createProducer(destination);

            // when sending the message will read the InputStream until it gets EOF
            BytesMessage message = session.createBytesMessage();
            FileInputStream fileInputStream = new FileInputStream(inputFile);
            BufferedInputStream bufferedInput = new BufferedInputStream(fileInputStream);
            message.setObjectProperty("JMS_AMQ_InputStream", bufferedInput);
            LOG.info("Sending the huge message...");
            producer.send(message);
            LOG.info("Message sent with id {}", message.getJMSMessageID());

            // when we receive the large message we initially just receive the message with an empty body
            MessageConsumer messageConsumer = session.createConsumer(destination);
            connection.start();
            LOG.info("Receiving the huge message...");
            BytesMessage messageReceived = (BytesMessage) messageConsumer.receive(120_000);
            LOG.info("Message received with {} bytes", messageReceived.getLongProperty("_AMQ_LARGE_SIZE"));

            File outputFile = new File("target/huge-message-received.dat");
            LOG.info("Streaming file to disk...");
            try (FileOutputStream fileOutputStream = new FileOutputStream(outputFile)) {
                BufferedOutputStream bufferedOutput = new BufferedOutputStream(fileOutputStream);
                // this will save the stream and wait until the entire message is written before continuing
                messageReceived.setObjectProperty("JMS_AMQ_SaveStream", bufferedOutput);
            }
            LOG.info("File of size {} bytes streamed", outputFile.length());

            connection.close();

        } catch (Exception e) {
            LOG.error("Unexpected error", e);
        }
    }

    private static void createFile(File file, long fileSize) throws IOException {
        FileOutputStream fileOut = new FileOutputStream(file);
        try (BufferedOutputStream buffOut = new BufferedOutputStream(fileOut)) {
           byte[] outBuffer = new byte[1024 * 1024];
           for (long i = 0; i < fileSize; i += outBuffer.length) {
              buffOut.write(outBuffer);
           }
        }
     }
}

