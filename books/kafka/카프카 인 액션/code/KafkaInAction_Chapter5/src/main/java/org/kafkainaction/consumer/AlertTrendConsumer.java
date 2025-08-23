package org.kafkainaction.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.kafkainaction.serde.AlertKeySerde;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Properties;

public class AlertTrendConsumer {

  final static Logger log = LoggerFactory.getLogger(AlertTrendConsumer.class);
  private volatile boolean keepConsuming = true;

  public static void main(String[] args) {
    Properties kaProperties = new Properties();
    kaProperties.put("bootstrap.servers", "localhost:9092,localhost:9093");
    kaProperties.put("enable.auto.commit", "true");
    kaProperties.put("group.id", "kinaction_team0groupalerttrend");
    kaProperties.put("key.deserializer", AlertKeySerde.class.getName());
    kaProperties.put("value.deserializer",
              "org.apache.kafka.common.serialization.StringDeserializer");

    AlertTrendConsumer consumer = new AlertTrendConsumer();
    consumer.consume(kaProperties);

    Runtime.getRuntime().addShutdownHook(new Thread(consumer::shutdown));
  }

  private void shutdown() {
    keepConsuming = false;
  }


  private void consume(final Properties kaProperties) {
    try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(kaProperties)) {

      consumer.subscribe(List.of("kinaction_alerttrend"));

      while (keepConsuming) {
        var records = consumer.poll(Duration.ofMillis(250));
        for (ConsumerRecord<String, String> record : records) {
          log.info("kinaction_info offset = {}, value = {}",
                   record.offset(), record.value());
        }
      }
    }

  }

}
