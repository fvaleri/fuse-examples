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
            LOG.debug("Starting producer");
            client = ApplicationUtil.openConnection();
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    ApplicationUtil.closeConnection(client);
                }
            });

            MqttMessage message = ApplicationUtil.createMessage();
            while (true) {
                client.publish(ConfigurationUtil.getTopic(), message);
                String text = ApplicationUtil.shorten(new String(message.getPayload()));
                LOG.info("Sent message {}", text);
                ApplicationUtil.sleep(ConfigurationUtil.getProcessingDelayMs());
            }

        } catch (Exception e) {
            LOG.error("Producer error", e);
        }
    }
}

