package it.fvaleri.integ;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.fvaleri.integ.ApplicationUtil.SchemaFormat;

import java.util.Date;

import java.util.concurrent.TimeUnit;

public class Producer implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(Producer.class);

    @Override
    public void run() {
        LOG.debug("Starting producer");
        try (KafkaProducer<Object, Object> producer = new KafkaProducer<>(ApplicationUtil.createProducerConfig())) {
            String message = ApplicationUtil.createMessage();
            Schema schema = null;
            if (ConfigurationUtil.getRegistryUrl() != null && ConfigurationUtil.getSchemaFormat() == SchemaFormat.AVRO) {
                schema = new Schema.Parser().parse(ApplicationUtil.getResourceAsFile("schemas/greeting.avsc"));
            }

            while (true) {
                Object value = message;
                if (schema != null) {
                    // create a generic avro record using the schema
                    GenericRecord gr = new GenericData.Record(schema);
                    gr.put("message", message);
                    gr.put("time", new Date().getTime());
                    value = gr;
                }

                ProducerRecord<Object, Object> record = new ProducerRecord<>(ConfigurationUtil.getTopics(), value);
                // sync send (at-least-once in-order per-partition delivery)
                RecordMetadata metadata = producer.send(record).get();

                LOG.info("Sent message [topic: {}, partition: {}, offset: {}, key: {}]", record.topic(),
                        record.partition(), metadata.offset(), record.key());

                TimeUnit.MILLISECONDS.sleep(ConfigurationUtil.getProcessingDelayMs());
            }

        } catch (Exception e) {
            LOG.error("Producer error", e);
            throw new RuntimeException(e);
        }
    }
}

