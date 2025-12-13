package org.kafkainaction.producer;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.kafkainaction.Alert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Properties;

import static org.kafkainaction.AlertStatus.Critical;

public class HelloWorldProducer {

  static final Logger log = LoggerFactory.getLogger(HelloWorldProducer.class);

  public static void main(String[] args) {
    Properties kaProperties = new Properties();
    kaProperties.put("bootstrap.servers",
                           "localhost:9092,localhost:9093,localhost:9094");
    kaProperties.put("key.serializer",
                           "org.apache.kafka.common.serialization.LongSerializer");
    kaProperties.put("value.serializer",
                           "io.confluent.kafka.serializers.KafkaAvroSerializer");   //<1>
    kaProperties.put("schema.registry.url", "http://localhost:8081");   //<2>

    try (Producer<Long, Alert> producer = new KafkaProducer<>(kaProperties)) {
      Alert alert = new Alert(12345L, Instant.now().toEpochMilli(), Critical);  //<3>

      log.info("kinaction_info Alert -> {}", alert);

      ProducerRecord<Long, Alert> producerRecord =
          new ProducerRecord<>("kinaction_schematest",
                               alert.getSensorId(),
                               alert); //<4>

      producer.send(producerRecord);
    }
  }
}
