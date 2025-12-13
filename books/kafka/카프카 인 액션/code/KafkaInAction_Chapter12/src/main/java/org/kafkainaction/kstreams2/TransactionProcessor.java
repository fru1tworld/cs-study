package org.kafkainaction.kstreams2;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;
import org.kafkainaction.Funds;
import org.kafkainaction.Transaction;
import org.kafkainaction.TransactionResult;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;

import static io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG;
import static org.apache.kafka.common.metrics.Sensor.RecordingLevel.TRACE;
import static org.apache.kafka.streams.StreamsConfig.APPLICATION_ID_CONFIG;
import static org.apache.kafka.streams.StreamsConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.streams.StreamsConfig.METRICS_RECORDING_LEVEL_CONFIG;

public class TransactionProcessor {

  private final String transactionsInputTopicName;
  private final String transactionSuccessTopicName;
  private final String transactionFailedTopicName;
  private final String fundsStoreName;

  public TransactionProcessor(final String transactionsInputTopicName,
                              final String transactionSuccessTopicName,
                              final String transactionFailedTopicName,
                              final String fundsStoreName) {

    this.transactionsInputTopicName = transactionsInputTopicName;
    this.transactionSuccessTopicName = transactionSuccessTopicName;
    this.transactionFailedTopicName = transactionFailedTopicName;
    this.fundsStoreName = fundsStoreName;
  }

  private static boolean success(String account, TransactionResult result) {
    return result.getSuccess();
  }

  public static void main(String[] args) {
    String transactionsInputTopicName = "transaction-request";
    String transactionSuccessTopicName = "transaction-success";
    String transactionFailedTopicName = "transaction-failed";
    String fundsStoreName = "funds-store";

    final TransactionProcessor transactionProcessor = new TransactionProcessor(transactionsInputTopicName,
                                                                               transactionSuccessTopicName,
                                                                               transactionFailedTopicName,
                                                                               fundsStoreName);
    Properties props = new Properties();
    props.put(APPLICATION_ID_CONFIG, "transaction-processor");
    props.put(BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
    props.put(SCHEMA_REGISTRY_URL_CONFIG, "http://localhost:8081");
    props.put(METRICS_RECORDING_LEVEL_CONFIG, TRACE.name);

    transactionProcessor.createTopics(props,
                                      transactionsInputTopicName,
                                      transactionSuccessTopicName,
                                      transactionFailedTopicName);

    StreamsBuilder builder = new StreamsBuilder();

    // could use default serde config instead
    final SpecificAvroSerde<Transaction> transactionRequestAvroSerde = SchemaSerdes.getSpecificAvroSerde(props);
    final SpecificAvroSerde<TransactionResult> transactionResultAvroSerde = SchemaSerdes.getSpecificAvroSerde(props);
    final SpecificAvroSerde<Funds> fundsSerde = SchemaSerdes.getSpecificAvroSerde(props);

    final Topology topology = transactionProcessor.topology(builder,
                                                            transactionRequestAvroSerde,
                                                            transactionResultAvroSerde,
                                                            fundsSerde);

    System.out.println("topology = " + topology.describe().toString());
    final KafkaStreams streams = new KafkaStreams(topology, props);
    final CountDownLatch latch = new CountDownLatch(1);

    // Attach shutdown handler to catch Control-C.
    Runtime.getRuntime().addShutdownHook(new Thread("streams-shutdown-hook") {
      @Override
      public void run() {
        streams.close(Duration.ofSeconds(5));
        latch.countDown();
      }
    });

    try {
      streams.start();
      latch.await();
    } catch (Throwable e) {
      System.exit(1);
    }
    System.exit(0);
  }

  private void createTopics(Properties p, String... names) {
    try (AdminClient client = AdminClient.create(p)) {

      final List<NewTopic> topicList =
          Arrays.stream(names)
              .map(name -> new NewTopic(name, 6, (short) 1))
              .collect(Collectors.toList());
      client.createTopics(topicList);
    }
  }

  public Topology topology(final StreamsBuilder builder,
                           final SpecificAvroSerde<Transaction> transactionRequestAvroSerde,
                           final SpecificAvroSerde<TransactionResult> transactionResultAvroSerde,
                           final SpecificAvroSerde<Funds> fundsSerde) {

    final Serde<String> stringSerde = Serdes.String();
    // Add the state store to the topology
    builder.addStateStore(storesBuilder(this.fundsStoreName, stringSerde, fundsSerde));

    KStream<String, Transaction> transactionStream =
        builder.stream(this.transactionsInputTopicName,
                       Consumed.with(stringSerde, transactionRequestAvroSerde));

    // Use peek instead of print for better readability
    transactionStream = transactionStream.peek((key, value) ->
                                                   System.out.println("transactions logger: key=" + key + ", value=" + value));

    // Use toTable with Named.as() for the name parameter
    transactionStream.toTable(
        Materialized.<String, Transaction, KeyValueStore<Bytes, byte[]>>as("latest-transactions")
            .withKeySerde(stringSerde)
            .withValueSerde(transactionRequestAvroSerde));

    // Use the non-deprecated process method
    KStream<String, TransactionResult> resultStream =
        transactionStream.process(
            () -> new TransactionTransformer(fundsStoreName),
            fundsStoreName);

/*    final KStream<String, TransactionResult> resultStream =
        transactionStream.transformValues(() -> new TransactionTransformer());*/

    resultStream
        .filter(TransactionProcessor::success)
        .to(this.transactionSuccessTopicName, Produced.with(Serdes.String(), transactionResultAvroSerde));

    resultStream
        .filterNot(TransactionProcessor::success)
        .to(this.transactionFailedTopicName, Produced.with(Serdes.String(), transactionResultAvroSerde));

    return builder.build();
  }

  protected static StoreBuilder<KeyValueStore<String, Funds>> storesBuilder(final String storeName,
                                                                            final Serde<String> keySerde,
                                                                            final SpecificAvroSerde<Funds> valueSerde) {
    return Stores.keyValueStoreBuilder(Stores.persistentKeyValueStore(storeName),
                                       keySerde,
                                       valueSerde);
  }


}
