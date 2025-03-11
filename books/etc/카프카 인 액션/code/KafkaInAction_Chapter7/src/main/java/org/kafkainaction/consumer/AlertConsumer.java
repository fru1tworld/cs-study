package org.kafkainaction.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.kafkainaction.model.Alert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Properties;

public class AlertConsumer {

  final static Logger log = LoggerFactory.getLogger(AlertConsumer.class);

  @SuppressWarnings("resource")
  public ConsumerRecords<Alert, String> getAlertMessages(Properties kaConsumerProperties) {
  	KafkaConsumer<Alert, String> consumer = new KafkaConsumer<>(kaConsumerProperties);
  	consumer.subscribe(List.of("kinaction_alert"));
  	return consumer.poll(Duration.ofMillis(2500));
  }

}
