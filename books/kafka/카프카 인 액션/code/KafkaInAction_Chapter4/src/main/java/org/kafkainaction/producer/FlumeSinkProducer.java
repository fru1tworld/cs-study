package org.kafkainaction.producer;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.kafkainaction.callback.AlertCallback;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.stream.Stream;


@SuppressWarnings("unused")
public class FlumeSinkProducer {

  public static void main(String[] args) {

    Properties kaProperties = readConfig();

    String topic = kaProperties.getProperty("topic");
    kaProperties.remove("topic");

    try (Producer<String, String> producer = new KafkaProducer<>(kaProperties)) {

      ProducerRecord<String, String> producerRecord = new ProducerRecord<>(topic, null, "event");
      producer.send(producerRecord, new AlertCallback());
    }

  }

  private static Properties readConfig() {
    Path path = Paths.get("src/main/resources/kafkasink.conf");

    Properties kaProperties = new Properties();

    try (Stream<String> lines = Files.lines(path)) {
      lines.forEachOrdered(line -> determineProperty(line, kaProperties));
    } catch (IOException e) {
      System.out.println("kinaction_error " + e);
    }
    return kaProperties;
  }

  private static void determineProperty(String line, Properties kaProperties) {
    if (line.contains("bootstrap")) {
      kaProperties.put("bootstrap.servers", line.split("=")[1]);
    } else if (line.contains("acks")) {
      kaProperties.put("acks", line.split("=")[1]);
    } else if (line.contains("compression.type")) {
      kaProperties.put("compression.type", line.split("=")[1]);
    } else if (line.contains("topic")) {
      kaProperties.put("topic", line.split("=")[1]);
    }

    kaProperties.putIfAbsent("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    kaProperties.putIfAbsent("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

  }

}
