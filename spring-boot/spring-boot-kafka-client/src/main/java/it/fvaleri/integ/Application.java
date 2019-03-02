package it.fvaleri.integ;

import javax.annotation.PostConstruct;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;

@SpringBootApplication
public class Application {
    private static final Logger LOG = LoggerFactory.getLogger(Application.class);
    private static final int NUM_MSGS = 1000;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Autowired
    private KafkaTemplate<String, String> template;

    @PostConstruct
    public void init() throws Exception {
        Thread.sleep(2000);
        for (int i = 0; i < NUM_MSGS; i++) {
            String message = "Hello World " + i;
            this.template.send("my-topic", message);
            LOG.info("Sent: {}", message);
        }
    }

    @KafkaListener(id = "my-listener", topics = "my-topic", concurrency = "1")
    public void listen(ConsumerRecord<?, ?> record) throws Exception {
        LOG.info("Received: {}", record.value());
    }
}

