package org.kafkainaction.kstreams2;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.kafkainaction.Transaction;

import java.math.BigDecimal;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Stream;

import io.confluent.kafka.serializers.KafkaAvroSerializer;

import static io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG;
import static java.lang.String.valueOf;
import static java.util.Collections.singletonList;
import static org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.CLIENT_ID_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG;
import static org.kafkainaction.TransactionType.DEPOSIT;
import static org.kafkainaction.TransactionType.WITHDRAW;

public class TransactionProducerTest {

  public static void main(String[] args) {

    var p = new Properties();
    p.put(BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
    p.put(SCHEMA_REGISTRY_URL_CONFIG, "http://localhost:8081");
    p.put(CLIENT_ID_CONFIG, "transactions-producer");
    p.put(KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    p.put(VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);

    try (var client = AdminClient.create(p)) {
      var txRequest = new NewTopic("transaction-request", 6, (short) 1);
      client.createTopics(singletonList(txRequest));
    }

    try (var producer = new KafkaProducer<String, Transaction>(p)) {

      final var tx1 =
          new Transaction(UUID.randomUUID().toString(), valueOf(1), new BigDecimal(100), DEPOSIT, "CAD",
                          "CA");
      final var tx2 =
          new Transaction(UUID.randomUUID().toString(), valueOf(1), new BigDecimal(200), DEPOSIT, "CAD",
                          "CA");
      final var tx3 =
          new Transaction(UUID.randomUUID().toString(), valueOf(1), new BigDecimal(300), DEPOSIT, "CAD",
                          "CA");
      final var tx4 =
          new Transaction(UUID.randomUUID().toString(), valueOf(1), new BigDecimal(300), WITHDRAW,
                          "CAD", "CA");
      final var tx5 =
          new Transaction(UUID.randomUUID().toString(), valueOf(1), new BigDecimal(1000), WITHDRAW,
                          "CAD", "CA");

      final var tx6 =
          new Transaction(UUID.randomUUID().toString(), valueOf(2), new BigDecimal(100), DEPOSIT,
                          "USD", "USA");
      final var tx7 =
          new Transaction(UUID.randomUUID().toString(), valueOf(2), new BigDecimal(50), DEPOSIT,
                          "USD", "USA");
      final var tx8 =
          new Transaction(UUID.randomUUID().toString(), valueOf(2), new BigDecimal(300), DEPOSIT,
                          "USD", "USA");
      final var tx9 =
          new Transaction(UUID.randomUUID().toString(), valueOf(2), new BigDecimal(300), WITHDRAW,
                          "USD", "USA");

      Stream.of(tx1, tx2, tx3, tx4, tx5, tx6, tx7, tx8, tx9)
          .forEach(tx -> producer.send(new ProducerRecord<>("transaction-request", tx.getAccount(), tx)));
    }
  }
}
