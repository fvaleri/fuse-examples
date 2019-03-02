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
            LOG.debug("Starting consumer");
            client = ApplicationUtil.openConnection();
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    ApplicationUtil.closeConnection(client);
                }
            });

            client.subscribe(ConfigurationUtil.getTopic(), (topic, message) -> {
                String text = ApplicationUtil.shorten(new String(message.getPayload()));
                LOG.info("Received message {}", text);
                ApplicationUtil.sleep(ConfigurationUtil.getProcessingDelayMs());
            });
            ApplicationUtil.sleep(Long.MAX_VALUE);

        } catch (Exception e) {
            LOG.error("Consumer error", e);
        }
    }
}

