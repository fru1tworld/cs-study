package org.kafkainaction.producer;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.ClassRule;
import org.junit.Test;
import org.kafkainaction.model.Alert;
import org.kafkainaction.partitioner.AlertLevelPartitioner;
import org.kafkainaction.serde.AlertKeySerde;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;

public class AlertLevelPartitionerTest {

    private static final String TOPIC = "kinaction_alert";
    private static final int PARTITION_NUMBER = 3;

    @ClassRule
    public static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("apache/kafka-native:3.8.0"));

    @Test
    public void testAlertPartitioner() throws ExecutionException, InterruptedException {
        // Create topic with 3 partitions
        createTopic(TOPIC, PARTITION_NUMBER);

        // Create producer properties
        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, AlertKeySerde.class.getName());
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.PARTITIONER_CLASS_CONFIG, AlertLevelPartitioner.class.getName());

        // Use AlertProducer to send a critical alert
        AlertProducer criticalProducer = new AlertProducer();
        criticalProducer.sendMessage(producerProps);

        // Create a custom producer for the non-critical alert
        // We need to do this because the default AlertProducer only sends a critical alert
        Alert nonCriticalAlert = new Alert(2, "Stage 2", "WARNING", "Stage 2 warning");
        CustomAlertProducer nonCriticalProducer = new CustomAlertProducer();
        nonCriticalProducer.sendCustomAlert(producerProps, nonCriticalAlert);

        // Create a consumer to verify the partitioning
        // We use StringDeserializer for both key and value since AlertKeySerde.deserialize returns null
        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        int criticalPartition = -1;
        int nonCriticalPartition = -1;

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps)) {
            consumer.subscribe(List.of(TOPIC));
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));

            for (ConsumerRecord<String, String> record : records) {
                if (record.value().equals("Stage 1 stopped")) {
                    criticalPartition = record.partition();
                } else if (record.value().equals("Stage 2 warning")) {
                    nonCriticalPartition = record.partition();
                }
            }
        }

        // Critical alerts should go to partition 0
        assertEquals("Critical alerts should go to partition 0", 0, criticalPartition);

        // Non-critical alerts should not go to partition 0
        assert nonCriticalPartition > 0 && nonCriticalPartition < PARTITION_NUMBER : 
            "Non-critical alert partition should be between 1 and " + (PARTITION_NUMBER - 1);
    }

    private void createTopic(String topicName, int partitions) throws ExecutionException, InterruptedException {
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());

        try (AdminClient adminClient = AdminClient.create(props)) {
            NewTopic newTopic = new NewTopic(topicName, partitions, (short) 1);
            adminClient.createTopics(Collections.singleton(newTopic)).all().get();
        }
    }

    /**
     * A custom producer that allows sending a specific alert
     * This is needed because the default AlertProducer only sends a critical alert
     */
    static class CustomAlertProducer extends AlertProducer {
        public void sendCustomAlert(Properties props, Alert alert) throws InterruptedException, ExecutionException {
            props.put("partitioner.class", "org.kafkainaction.partitioner.AlertLevelPartitioner");

            try (org.apache.kafka.clients.producer.Producer<Alert, String> producer = 
                    new org.apache.kafka.clients.producer.KafkaProducer<>(props)) {
                org.apache.kafka.clients.producer.ProducerRecord<Alert, String> producerRecord = 
                    new org.apache.kafka.clients.producer.ProducerRecord<>(TOPIC, alert, alert.getAlertMessage());
                producer.send(producerRecord).get();
                producer.flush();
            }
        }
    }
}
