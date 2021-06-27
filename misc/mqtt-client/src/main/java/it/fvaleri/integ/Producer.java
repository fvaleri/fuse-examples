package it.fvaleri.integ;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Producer implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(Producer.class);
    private IMqttClient client;

    @Override
    public void run() {
        try {
            LOG.info("Starting producer");
            client = Utils.openConnection();
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    Utils.closeConnection(client);
                }
            });

            MqttMessage message = Utils.createMessage();
            while (true) {
                client.publish(Configuration.TOPIC, message);
                String text = Utils.shorten(new String(message.getPayload()));
                LOG.info("Sent message {}", text);
                Utils.sleep(Configuration.PROCESSING_DELAY_MS);
            }
        } catch (Exception e) {
            LOG.error("Producer error", e);
        }
    }
}
