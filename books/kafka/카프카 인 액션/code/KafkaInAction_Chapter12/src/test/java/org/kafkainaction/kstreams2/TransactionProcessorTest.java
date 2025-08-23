package org.kafkainaction.kstreams2;


import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.TopologyTestDriver;
import org.apache.kafka.streams.state.KeyValueStore;
import org.junit.Before;
import org.junit.Test;
import org.kafkainaction.ErrorType;
import org.kafkainaction.Funds;
import org.kafkainaction.Transaction;
import org.kafkainaction.TransactionResult;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;

import static io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG;
import static java.nio.file.Files.createTempDirectory;
import static org.apache.kafka.streams.StreamsConfig.STATE_DIR_CONFIG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.kafkainaction.TransactionType.DEPOSIT;
import static org.kafkainaction.TransactionType.WITHDRAW;

public class TransactionProcessorTest {

  private Serde<String> stringSerde;
  private Transaction deposit100;
  private Transaction withdraw100;
  private Transaction withdraw200;

  private Properties properties;
  private Topology topology;
  private SpecificAvroSerde<TransactionResult> transactionResultSerde;
  private SpecificAvroSerde<Transaction> transactionSerde;
  private SpecificAvroSerde<Funds> fundsSerde;

  private static String transactionsInputTopicName = "transaction-request";
  private static String transactionSuccessTopicName = "transaction-success";
  private static String transactionFailedTopicName = "transaction-failed";
  private static String fundsStoreName = "funds-store";

  public TransactionProcessorTest() throws IOException {
  }

  @Before
  public void setUp() throws IOException {

    final StreamsBuilder streamsBuilder = new StreamsBuilder();
    properties = new Properties();
    properties.putAll(
        Map.of(
            SCHEMA_REGISTRY_URL_CONFIG, "mock://schema-registry.kafkainaction.org:8080",
            // workaround https://stackoverflow.com/a/50933452/27563
            STATE_DIR_CONFIG, createTempDirectory("kafka-streams").toAbsolutePath().toString()
        ));

    // serdes
    stringSerde = Serdes.String();
    transactionResultSerde = SchemaSerdes.getSpecificAvroSerde(properties);
    transactionSerde = SchemaSerdes.getSpecificAvroSerde(properties);
    fundsSerde = SchemaSerdes.getSpecificAvroSerde(properties);

    topology = new TransactionProcessor(transactionsInputTopicName,
                                        transactionSuccessTopicName,
                                        transactionFailedTopicName,
                                        fundsStoreName)
        .topology(streamsBuilder,
                  transactionSerde,
                  transactionResultSerde,
                  fundsSerde);

    deposit100 = new Transaction(UUID.randomUUID().toString(),
                                 "1", new BigDecimal(100), DEPOSIT, "USD", "USA");

    withdraw100 = new Transaction(UUID.randomUUID().toString(),
                                  "1", new BigDecimal(100), WITHDRAW, "USD", "USA");

    withdraw200 = new Transaction(UUID.randomUUID().toString(),
                                  "1", new BigDecimal(200), WITHDRAW, "USD", "USA");
  }

  @Test
  public void testDriverShouldNotBeNull() {
    try (TopologyTestDriver testDriver = new TopologyTestDriver(topology, properties)) {
      assertThat(testDriver).isNotNull();
    }
  }

  @Test
  public void shouldCreateSuccessfulTransaction() {

    try (TopologyTestDriver testDriver = new TopologyTestDriver(topology, properties)) {

      final TestInputTopic<String, Transaction> inputTopic = testDriver
          .createInputTopic(transactionsInputTopicName,
                            stringSerde.serializer(),
                            transactionSerde.serializer());

      inputTopic.pipeInput(deposit100.getAccount(), deposit100);
      inputTopic.pipeInput(withdraw100.getAccount(), withdraw100);

      final TestOutputTopic<String, TransactionResult>
          outputTopic =
          testDriver.createOutputTopic(transactionSuccessTopicName,
                                       stringSerde.deserializer(),
                                       transactionResultSerde.deserializer());

      final List<TransactionResult> successfulTransactions = outputTopic.readValuesToList();
      // balance should be 0
      final TransactionResult transactionResult = successfulTransactions.get(1);

      assertThat(transactionResult.getFunds().getBalance()).isEqualByComparingTo(new BigDecimal(0));
    }
  }

  @Test
  public void shouldBeInsufficientFunds() {

    try (TopologyTestDriver testDriver = new TopologyTestDriver(topology, properties)) {
      final TestInputTopic<String, Transaction> inputTopic = testDriver
          .createInputTopic(transactionsInputTopicName,
                            stringSerde.serializer(),
                            transactionSerde.serializer());

      inputTopic.pipeInput(deposit100.getAccount(), deposit100);
      inputTopic.pipeInput(withdraw200.getAccount(), withdraw200);

      final TestOutputTopic<String, TransactionResult>
          failedResultOutputTopic =
          testDriver.createOutputTopic(transactionFailedTopicName,
                                       stringSerde.deserializer(),
                                       transactionResultSerde.deserializer());

      final TestOutputTopic<String, TransactionResult>
          successResultOutputTopic =
          testDriver.createOutputTopic(transactionSuccessTopicName,
                                       stringSerde.deserializer(),
                                       transactionResultSerde.deserializer());

      final TransactionResult successfulDeposit100Result = successResultOutputTopic.readValuesToList().get(0);

      assertThat(successfulDeposit100Result.getFunds().getBalance()).isEqualByComparingTo(new BigDecimal(100));

      final List<TransactionResult> failedTransactions = failedResultOutputTopic.readValuesToList();
      // balance should be 0
      final TransactionResult transactionResult = failedTransactions.get(0);
      assertThat(transactionResult.getErrorType()).isEqualTo(ErrorType.INSUFFICIENT_FUNDS);
    }
  }

  @Test
  public void balanceShouldBe300() {
    try (TopologyTestDriver testDriver = new TopologyTestDriver(topology, properties)) {
      final TestInputTopic<String, Transaction> inputTopic = testDriver
          .createInputTopic(transactionsInputTopicName,
                            stringSerde.serializer(),
                            transactionSerde.serializer());

      inputTopic.pipeInput(deposit100.getAccount(), deposit100);
      inputTopic.pipeInput(deposit100.getAccount(), deposit100);
      inputTopic.pipeInput(deposit100.getAccount(), deposit100);

      final KeyValueStore<String, Funds> store = testDriver.getKeyValueStore(fundsStoreName);

      assertThat(store.get("1").getBalance()).isEqualByComparingTo(new BigDecimal(300));
    }
  }
}