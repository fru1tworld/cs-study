package org.kafkainaction.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class AuditConsumer {

  final static Logger log = LoggerFactory.getLogger(AuditConsumer.class);
  private volatile boolean keepConsuming = true;

  public static void main(String[] args) {
    Properties kaProperties = new Properties();
    kaProperties.put("bootstrap.servers", "localhost:9092,localhost:9093");
    kaProperties.put("enable.auto.commit", "false");
    kaProperties.put("group.id", "kinaction_group_audit");
    kaProperties.put("key.deserializer",
              "org.apache.kafka.common.serialization.StringDeserializer");
    kaProperties.put("value.deserializer",
              "org.apache.kafka.common.serialization.StringDeserializer");

    final AuditConsumer auditConsumer = new AuditConsumer();
    auditConsumer.consume(kaProperties);
    Runtime.getRuntime().addShutdownHook(new Thread(auditConsumer::shutdown));
  }

  private void consume(final Properties kaProperties) {
    try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(kaProperties)) {

      consumer.subscribe(List.of("kinaction_audit"));

      while (keepConsuming) {
        var records = consumer.poll(Duration.ofMillis(250));
        for (ConsumerRecord<String, String> record : records) {
          log.info("kinaction_info offset = {}, value = {}",
                   record.offset(), record.value());

          OffsetAndMetadata offsetMeta = new OffsetAndMetadata(record.offset() + 1, "");

          Map<TopicPartition, OffsetAndMetadata> kaOffsetMap = new HashMap<>();
          kaOffsetMap.put(new TopicPartition("kinaction_audit", record.partition()), offsetMeta);

          consumer.commitSync(kaOffsetMap);
        }
      }
    }

  }

  private void shutdown() {
    keepConsuming = false;
  }
}
