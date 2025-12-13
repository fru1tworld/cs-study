package org.kafkainaction.producer;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.kafkainaction.model.Alert;
import org.kafkainaction.serde.AlertKeySerde;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.ExecutionException;

public class AlertTrendingProducer {

  private static final Logger log =
      LoggerFactory.getLogger(AlertTrendingProducer.class);

  public static void main(String[] args)
      throws InterruptedException, ExecutionException {

    Properties kaProperties = new Properties();
    kaProperties.put("bootstrap.servers",
                           "localhost:9092,localhost:9093,localhost:9094");
    kaProperties.put("key.serializer",
                          AlertKeySerde.class.getName());   //<1>
    kaProperties.put("value.serializer",
                           "org.apache.kafka.common.serialization.StringSerializer");

    try (Producer<Alert, String> producer = new KafkaProducer<>(kaProperties)) {
      Alert alert = new Alert(0, "Stage 0", "CRITICAL", "Stage 0 stopped");
      ProducerRecord<Alert, String> producerRecord =
          new ProducerRecord<>("kinaction_alerttrend", alert, alert.getAlertMessage());    //<2>

      RecordMetadata result = producer.send(producerRecord).get();
      log.info("kinaction_info offset = {}, topic = {}, timestamp = {}",
               result.offset(), result.topic(), result.timestamp());
    }
  }
}
