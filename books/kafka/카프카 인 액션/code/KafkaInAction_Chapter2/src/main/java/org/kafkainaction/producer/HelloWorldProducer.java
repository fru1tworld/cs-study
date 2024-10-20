package org.kafkainaction.producer;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Properties;

public class HelloWorldProducer {

  public static void main(String[] args) {

    Properties kaProperties = new Properties();   //<1>
    kaProperties.put("bootstrap.servers",
                           "localhost:9092,localhost:9093,localhost:9094");   //<2>

    kaProperties.put(
        "key.serializer", "org.apache.kafka.common.serialization.StringSerializer");    //<3>
    kaProperties.put("value.serializer",
                           "org.apache.kafka.common.serialization.StringSerializer");

    try (Producer<String, String> producer = new KafkaProducer<>(kaProperties)) { //<4>

      ProducerRecord<String, String> producerRecord =
          new ProducerRecord<>("kinaction_helloworld", null, "hello world again!");   //<5>

      producer.send(producerRecord);    //<6>
      producer.close();   //<7>
    }
  }
}
