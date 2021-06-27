package it.fvaleri.integ;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

import java.util.concurrent.TimeUnit;

public class Producer implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(Producer.class);

    @Override
    public void run() {
        LOG.info("Starting producer");
        try (KafkaProducer<Object, Object> producer = new KafkaProducer<>(Utils.createProducerConfig())) { // thread-safe
            final String message = Utils.createMessage();

            while (true) {
                Object value = message;

                if (Configuration.REGISTRY_URL != null) {
                    switch (Configuration.SCHEMA_FORMAT) {
                        case "avro":
                            // using the generic record for testing
                            Schema schema = new Schema.Parser().parse(Utils.getResourceAsFile("schemas/test.avsc"));
                            GenericRecord record = new GenericData.Record(schema);
                            record.put("time", new Date().getTime());
                            record.put("message", message);
                            value = record;
                        default:
                            throw new IllegalArgumentException("Unsupported schema type");
                    }
                }

                ProducerRecord<Object, Object> record = new ProducerRecord<>(Configuration.TOPICS, value);
                // sync send (at-least-once in-order per-partition delivery)
                RecordMetadata metadata = producer.send(record).get();

                LOG.info("Sent message [topic: {}, partition: {}, offset: {}, key: {}]", record.topic(),
                        record.partition(), metadata.offset(), record.key());

                TimeUnit.MILLISECONDS.sleep(Configuration.PROCESSING_DELAY_MS);
            }
        } catch (Exception e) {
            LOG.error("Producer error", e);
            throw new RuntimeException(e);
        }
    }
}

