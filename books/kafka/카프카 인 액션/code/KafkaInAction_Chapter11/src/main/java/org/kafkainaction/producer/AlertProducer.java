package org.kafkainaction.producer;

import org.kafkainaction.avro.Alert;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Properties;

import static org.kafkainaction.avro.AlertStatus.Critical;

public class AlertProducer {

  final static Logger log = LoggerFactory.getLogger(AlertProducer.class);

  public static void main(String[] args) {

    Properties kaProperties = new Properties();
    kaProperties.put("bootstrap.servers", "localhost:9092");
    kaProperties.put("key.serializer", "org.apache.kafka.common.serialization.LongSerializer");
    kaProperties.put("value.serializer", "io.confluent.kafka.serializers.KafkaAvroSerializer"); // <1>
    kaProperties.put("schema.registry.url", "http://localhost:8081"); // <2>

    try (Producer<Long, Alert> producer = new KafkaProducer<>(kaProperties)) {
      Alert alert = new Alert(); //<3>
      alert.setSensorId(12345L);
      alert.setTime(Calendar.getInstance().getTimeInMillis());
      alert.setStatus(Critical);
      /* Uncomment the following line if alert_v2.avsc is the latest Alert model */
     // alert.setRecoveryDetails("RecoveryDetails");
      log.info(alert.toString());

      ProducerRecord<Long, Alert> producerRecord = new ProducerRecord<>("kinaction_schematest", alert.getSensorId(), alert); // <4>

      producer.send(producerRecord);
    }

  }

}
