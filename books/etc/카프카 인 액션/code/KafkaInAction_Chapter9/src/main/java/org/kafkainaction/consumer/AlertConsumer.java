package org.kafkainaction.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Properties;

public class AlertConsumer {

  final static Logger log = LoggerFactory.getLogger(AlertConsumer.class);

  public static void main(String[] args) {
    Properties kaProperties = new Properties();
    kaProperties.put("bootstrap.servers", "localhost:9092,localhost:9093");
    kaProperties.put("group.id", "kinaction_alertinterceptor");
    kaProperties.put("enable.auto.commit", "true");
    kaProperties.put("auto.commit.interval.ms", "1000");
    kaProperties.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
    kaProperties.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
    kaProperties.put("interceptor.classes", AlertConsumerMetricsInterceptor.class.getName());

    try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(kaProperties)) {
      consumer.subscribe(List.of("kinaction_alert"));

      while (true) {
        var records = consumer.poll(Duration.ofMillis(250));
        for (ConsumerRecord<String, String> record : records) {
          log.info("kinaction_info offset = {}, value = {}", record.offset(), record.value());
        }
      }
    }
  }

}
