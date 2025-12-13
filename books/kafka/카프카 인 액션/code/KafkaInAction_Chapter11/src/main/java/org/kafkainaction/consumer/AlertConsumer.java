package org.kafkainaction.consumer;

import org.kafkainaction.avro.Alert;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Properties;

import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;

public class AlertConsumer {

  final static Logger log = LoggerFactory.getLogger(AlertConsumer.class);

  public static void main(String[] args) {
    Properties kaProperties = new Properties();
    kaProperties.put("bootstrap.servers", "localhost:9094");
    kaProperties.put("group.id", "alertinterceptor");
    kaProperties.put("enable.auto.commit", "true");
    kaProperties.put("auto.commit.interval.ms", "1000");
    kaProperties.put("key.deserializer", "org.apache.kafka.common.serialization.LongDeserializer");
    kaProperties.put("value.deserializer", "io.confluent.kafka.serializers.KafkaAvroDeserializer"); // <1>
    kaProperties.put("schema.registry.url", "http://localhost:8081"); // <2>
    kaProperties.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);

    KafkaConsumer<Long, Alert> consumer = new KafkaConsumer<Long, Alert>(kaProperties); //C <3>

    consumer.subscribe(List.of("kinaction_schematest")); //<4>

    while (true) {
      ConsumerRecords<Long, Alert> records = consumer.poll(Duration.ofMillis(250));
      for (ConsumerRecord<Long, Alert> record : records) {
        log.info("kinaction_info Alert Content = {}", record.value().toString()); //<5>
      }
    }
  }

}
