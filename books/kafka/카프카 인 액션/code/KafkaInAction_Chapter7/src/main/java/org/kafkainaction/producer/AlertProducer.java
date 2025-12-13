package org.kafkainaction.producer;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.kafkainaction.model.Alert;

import java.util.Properties;
import java.util.concurrent.ExecutionException;

public class AlertProducer {

	public void sendMessage(Properties kaProperties) throws InterruptedException, ExecutionException {
		kaProperties.put("partitioner.class", "org.kafkainaction.partitioner.AlertLevelPartitioner"); // <2>

		try (Producer<Alert, String> producer = new KafkaProducer<>(kaProperties)) {
			Alert alert = new Alert(1, "Stage 1", "CRITICAL", "Stage 1 stopped");
			ProducerRecord<Alert, String> producerRecord = new ProducerRecord<>("kinaction_alert", alert,
					alert.getAlertMessage()); // <3>

			producer.send(producerRecord).get();
		}

	}

}
