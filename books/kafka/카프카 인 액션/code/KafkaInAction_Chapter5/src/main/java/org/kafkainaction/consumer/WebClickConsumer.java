package org.kafkainaction.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Properties;


public class WebClickConsumer {

  final static Logger log = LoggerFactory.getLogger(WebClickConsumer.class);
  private volatile boolean keepConsuming = true;

  public static void main(String[] args) {
    Properties kaProperties = new Properties();
    kaProperties.put("bootstrap.servers",
              "localhost:9092,localhost:9093,,localhost:9094");
    kaProperties.put("group.id", "kinaction_webconsumer");   //<1>
    kaProperties.put("enable.auto.commit", "true");
    kaProperties.put("auto.commit.interval.ms", "1000");
    kaProperties.put("key.deserializer",
              "org.apache.kafka.common.serialization.StringDeserializer");    //<2>
    kaProperties.put("value.deserializer",
              "org.apache.kafka.common.serialization.StringDeserializer");

    WebClickConsumer webClickConsumer = new WebClickConsumer();
    webClickConsumer.consume(kaProperties);

    Runtime.getRuntime().addShutdownHook(new Thread(webClickConsumer::shutdown));
  }

  private void consume(Properties kaProperties) {
    try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(kaProperties)) {   //<3>
      consumer.subscribe(List.of("kinaction_promos"));   //<4>

      while (keepConsuming) {   //<5>
        var records = consumer.poll(Duration.ofMillis(250));
        for (ConsumerRecord<String, String> record : records) {
          log.info("kinaction_info offset = {}, key = {}",
                   record.offset(), record.key());
          log.info("kinaction_info value = {}", Double.parseDouble(record.value()) * 1.543);
        }
      }
    }
  }

  private void shutdown() {
    keepConsuming = false;
  }
}
