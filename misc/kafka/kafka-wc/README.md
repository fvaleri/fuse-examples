```sh
bin/kafka-topics.sh --bootstrap-server :9092 --create --topic wc-input --partitions 1 --replication-factor 1
bin/kafka-topics.sh --bootstrap-server :9092 --create --topic wc-output --partitions 1 --replication-factor 1

bin/kafka-console-producer.sh --bootstrap-server :9092 --topic wc-input
bin/kafka-console-consumer.sh --bootstrap-server :9092 \
    --formatter kafka.tools.DefaultMessageFormatter \
    --property key.deserializer=org.apache.kafka.common.serialization.StringDeserializer \
    --property value.deserializer=org.apache.kafka.common.serialization.LongDeserializer \
    --property print.key=true \
    --property print.value=true \
    --topic wc-output \
    --from-beginning

rm -rf /tmp/kafka-wc
mvn clean compile exec:java
```
