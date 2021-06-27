package it.fvaleri.integ;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Consumer implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(Consumer.class);
    private IMqttClient client;

    @Override
    public void run() {
        try {
            LOG.info("Starting consumer");
            client = Utils.openConnection();
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    Utils.closeConnection(client);
                }
            });

            client.subscribe(Configuration.TOPIC, (topic, message) -> {
                String text = Utils.shorten(new String(message.getPayload()));
                LOG.info("Received message {}", text);
                Utils.sleep(Configuration.PROCESSING_DELAY_MS);
            });
            Utils.sleep(Long.MAX_VALUE);
        } catch (Exception e) {
            LOG.error("Consumer error", e);
        }
    }
}
