package it.fvaleri.integ;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Produced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

public final class WCStream {
    private static final Logger LOG = LoggerFactory.getLogger(WCStream.class);

    public static void main(String[] args) {
        final StreamsBuilder builder = new StreamsBuilder();
        final KStream<String, String> source = builder.stream("wc-input");
        LOG.info("Word count stream created");

        // A processor topology defines the stream processing computational logic for your application
        // partitions are assigned to a fixed number of tasks and processed by configured threads across all instances
        // stateful DSL operation such as count trigger the creation and management of a state store called KTable
        // KTable is the stateful view of a KStream (stream-table duality; table = stream snapshot; RocksDB)
        // more advanced use cases can be addressed by using reusable Processors and Transformers
        final KTable<String, Long> counts = source
            .flatMapValues(value -> Arrays.asList(value.toLowerCase(Locale.getDefault()).split(" ")))
            .groupBy((key, value) -> value)
            .count();

        // we need to override value serde to Long type
        counts.toStream().to("wc-output", Produced.with(Serdes.String(), Serdes.Long()));

        final KafkaStreams streams = new KafkaStreams(builder.build(), createStreamsConfig());
        final CountDownLatch latch = new CountDownLatch(1);

        Runtime.getRuntime().addShutdownHook(new Thread("wc-shutdown") {
            @Override
            public void run() {
                streams.close();
                latch.countDown();
            }
        });

        try {
            streams.start();
            latch.await();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static Properties createStreamsConfig() {
        Properties config = new Properties();
        config.put(StreamsConfig.APPLICATION_ID_CONFIG, "wc-stream");
        config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        config.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        config.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0);

        // durability/availability tradeoff
        config.put(StreamsConfig.REPLICATION_FACTOR_CONFIG, 2);
        config.put(ProducerConfig.ACKS_CONFIG, "all");

        // tls
        /*config.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");
        config.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, "/tmp/truststore.jks");
        config.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, "changeit");*/

        // threads
        // one thread per partition across all instances; replicas is used to minimize task failover latency
        // if you configure n standby replicas, you need to provision n+1 KafkaStreams instances
        config.put(StreamsConfig.NUM_STREAM_THREADS_CONFIG, 1);
        config.put(StreamsConfig.NUM_STANDBY_REPLICAS_CONFIG, 0);

        // transactions
        // the commit interval determines the transaction size (throughput/latency tradeoff)
        /*config.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, "exactly_once");
        config.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, "100");*/

        // reconnections
        config.put(StreamsConfig.RECONNECT_BACKOFF_MS_CONFIG, 1_000);
        config.put(StreamsConfig.RECONNECT_BACKOFF_MAX_MS_CONFIG, 5_000);

        // retries
        config.put(StreamsConfig.REQUEST_TIMEOUT_MS_CONFIG, 60_000);
        config.put(StreamsConfig.TASK_TIMEOUT_MS_CONFIG, 300_000);
        config.put(StreamsConfig.RETRY_BACKOFF_MS_CONFIG, 2_000);

        return config;
    }
}

