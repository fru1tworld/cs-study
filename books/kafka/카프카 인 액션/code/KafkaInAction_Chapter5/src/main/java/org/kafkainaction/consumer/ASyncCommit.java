package org.kafkainaction.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
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

public class ASyncCommit {

  final static Logger log = LoggerFactory.getLogger(ASyncCommit.class);
  private volatile boolean keepConsuming = true;
  public static final String TOPIC_NAME = "kinaction_views";

  public static void main(String[] args) {
    Properties kaProperties = new Properties();
    kaProperties.put("bootstrap.servers", "localhost:9092,localhost:9093");
    kaProperties.put("group.id", "kinaction_group_views");
    kaProperties.put("key.deserializer",
              "org.apache.kafka.common.serialization.StringDeserializer");
    kaProperties.put("value.deserializer",
              "org.apache.kafka.common.serialization.StringDeserializer");

    final ASyncCommit aSyncCommit = new ASyncCommit();
    aSyncCommit.consume(kaProperties);
    Runtime.getRuntime().addShutdownHook(new Thread(aSyncCommit::shutdown));
  }

  private void consume(final Properties kaProperties) {
    try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(kaProperties)) {

      consumer.assign(List.of(new TopicPartition(TOPIC_NAME, 1),
                              new TopicPartition(TOPIC_NAME, 2)));

      while (keepConsuming) {
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(250));
        for (ConsumerRecord<String, String> record : records) {
          log.info("kinaction_info offset = {}, value = {}", record.offset(),  record.value());
          commitOffset(record.offset(), record.partition(), TOPIC_NAME, consumer);
        }
      }
    }
  }

  public static void commitOffset(long offset, 
                                  int partition, 
                                  String topic, 
                                  KafkaConsumer<String, String> consumer) {
    OffsetAndMetadata offsetMeta = new OffsetAndMetadata(++offset, "");

    Map<TopicPartition, OffsetAndMetadata> kaOffsetMap = new HashMap<>();
    kaOffsetMap.put(new TopicPartition(topic, partition), offsetMeta);

    consumer.commitAsync(kaOffsetMap, (map, e) -> {
      if (e != null) {
        for (TopicPartition key : map.keySet()) {
          log.info("kinaction_error topic {}, offset {}", key.topic(),  map.get(key).offset());
        }
      } else {
        for (TopicPartition key : map.keySet()) {
          log.info("kinaction_info topic {}, offset {}", key.topic(), map.get(key).offset());
        }
      }
    });
  }

  private void shutdown() {
    keepConsuming = false;
  }
}
