package org.kafkainaction.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class KinactionStopConsumer implements Runnable {

  final static Logger log = LoggerFactory.getLogger(KinactionStopConsumer.class);

  private final KafkaConsumer<String, String> consumer;
  private final AtomicBoolean stopping = new AtomicBoolean(false);

  public KinactionStopConsumer(KafkaConsumer<String, String> consumer) {
    this.consumer = consumer;
  }

  public void run() {
    try {
      consumer.subscribe(List.of("kinaction_promos"));
      while (!stopping.get()) {
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(250));
        for (ConsumerRecord<String, String> record : records) {
          log.info("kinaction_info offset = {},value = {}", record.offset(), record.value());
        }
      }
    } catch (WakeupException e) {
        if (!stopping.get()) {
            throw e;
        }
    } finally {
      consumer.close();
    }
  }

  public void shutdown() {
    stopping.set(true);
    consumer.wakeup();
  }
}
