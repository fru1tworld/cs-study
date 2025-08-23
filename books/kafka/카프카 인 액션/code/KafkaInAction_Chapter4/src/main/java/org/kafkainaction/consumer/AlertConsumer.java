package org.kafkainaction.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.kafkainaction.model.Alert;
import org.kafkainaction.serde.AlertKeySerde;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class AlertConsumer {

  final static Logger log = LoggerFactory.getLogger(AlertConsumer.class);
  private volatile boolean keepConsuming = true;

  public static void main(String[] args) {
    Properties kaProperties = new Properties();
    kaProperties.put("bootstrap.servers", "localhost:9092,localhost:9093");
    kaProperties.put("enable.auto.commit", "false");
    kaProperties.put("group.id", "kinaction_team0groupalert");
    /** Deserialize key using {@link org.kafkainaction.serde.AlertKeySerde} */
    kaProperties.put("key.deserializer", AlertKeySerde.class.getName());
    kaProperties.put("value.deserializer",
              "org.apache.kafka.common.serialization.StringDeserializer");

    AlertConsumer consumer = new AlertConsumer();
    consumer.consume(kaProperties);

    Runtime.getRuntime().addShutdownHook(new Thread(consumer::shutdown));
  }

  private void shutdown() {
    keepConsuming = false;
  }

  private void consume(final Properties kaProperties) {
    KafkaConsumer<Alert, String> consumer = new KafkaConsumer<>(kaProperties);
    TopicPartition partitionZero = new TopicPartition("kinaction_alert", 0);
    consumer.assign(List.of(partitionZero));

    while (keepConsuming) {
      ConsumerRecords<Alert, String> records = consumer.poll(Duration.ofMillis(250));
      for (ConsumerRecord<Alert, String> record : records) {
        log.info("kinaction_info offset = {}, key = {}",
                 record.offset(),
                 record.key().getStageId());
        commitOffset(record.offset(), record.partition(), "kinaction_alert", consumer);
      }
    }
  }

  public static void commitOffset(long offset, int part, String topic, KafkaConsumer<Alert, String> consumer) {
    OffsetAndMetadata offsetMeta = new OffsetAndMetadata(++offset, "");

    Map<TopicPartition, OffsetAndMetadata> kaOffsetMap = new HashMap<>();
    kaOffsetMap.put(new TopicPartition(topic, part), offsetMeta);

    consumer.commitAsync(kaOffsetMap, AlertConsumer::onComplete);
  }

  private static void onComplete(Map<TopicPartition, OffsetAndMetadata> map,
                                 Exception e) {
    if (e != null) {
      for (TopicPartition key : map.keySet()) {
        log.info("kinaction_error topic {}, partition {}, offset {}",
                 key.topic(),
                 key.partition(),
                 map.get(key).offset());
      }
    } else {
      for (TopicPartition key : map.keySet()) {
        log.info("kinaction_info topic {}, partition {}, offset {}",
                 key.topic(),
                 key.partition(),
                 map.get(key).offset());
      }
    }
  }

}
