package it.fvaleri.integ;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Consumer implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(Consumer.class);

    @Override
    public void run() {
        LOG.debug("Starting consumer");
        try (KafkaConsumer<Object, Object> consumer = new KafkaConsumer<>(ApplicationUtil.createConsumerConfig())) {
            consumer.subscribe(Collections.singletonList(ConfigurationUtil.getTopics()));

            // if consumers fail within a consumer group, a rebalance is triggered
            while (true) {
                ConsumerRecords<Object, Object> records = consumer.poll(Duration.ofSeconds(1)); // blocking
                if (records.isEmpty()) {
                    continue;
                }

                // each client in a consumer group receives data **exclusively** from zero or more partitions
                // consumer lag indicates the difference in the rate of production and consumption of messages
                for (ConsumerRecord<Object, Object> record : records) {
                    LOG.info("Received message [topic: {}, partition: {}, offset: {}, key: {}]", record.topic(),
                            record.partition(), record.offset(), record.key());
                }

                // at-least-once: commit after processing (duplicates are possible)
                // to avoid duplicates the consumer must be idempotent
                consumer.commitSync();

                TimeUnit.MILLISECONDS.sleep(ConfigurationUtil.getProcessingDelayMs());
            }

        } catch (Exception e) {
            LOG.error("Consumer error", e);
            throw new RuntimeException(e);
        }
    }
}

