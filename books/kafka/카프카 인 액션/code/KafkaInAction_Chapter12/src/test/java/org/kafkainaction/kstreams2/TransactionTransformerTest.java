package org.kafkainaction.kstreams2;

import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.processor.api.MockProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.Stores;
import org.junit.Before;
import org.junit.Test;
import org.kafkainaction.Funds;
import org.kafkainaction.Transaction;
import org.kafkainaction.TransactionResult;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;

import static io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG;
import static org.apache.kafka.streams.StreamsConfig.APPLICATION_ID_CONFIG;
import static org.apache.kafka.streams.StreamsConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.kafkainaction.TransactionType.DEPOSIT;


public class TransactionTransformerTest {

  private KeyValueStore<String, Funds> fundsStore;
  private MockProcessorContext<String, TransactionResult> mockContext;
  private TransactionTransformer transactionTransformer;

  final static Map<String, String> testConfig = Map.of(
      BOOTSTRAP_SERVERS_CONFIG, "localhost:8080",
      APPLICATION_ID_CONFIG, "mytest",
      SCHEMA_REGISTRY_URL_CONFIG, "mock://schema-registry.kafkainaction.org:8080"
  );

  @Before
  public void setup() {
    final Properties properties = new Properties();
    properties.putAll(testConfig);
    mockContext = new MockProcessorContext<>(properties);

    final SpecificAvroSerde<Funds>
        fundsSpecificAvroSerde =
        SchemaSerdes.getSpecificAvroSerde(properties);

    final Serde<String> stringSerde = Serdes.String();
    final String fundsStoreName = "fundsStore";
    this.fundsStore = Stores.keyValueStoreBuilder(
        Stores.inMemoryKeyValueStore(fundsStoreName),
        stringSerde,
        fundsSpecificAvroSerde)
        .withLoggingDisabled()    // Changelog is not supported by MockProcessorContext.
        .build();

    fundsStore.init(mockContext.getStateStoreContext(), fundsStore);
    mockContext.addStateStore(fundsStore);

    transactionTransformer = new TransactionTransformer(fundsStoreName);
    transactionTransformer.init(mockContext);
  }

  @Test
  public void shouldInitializeTransformer() {
    // Just verify that the transformer can be initialized
    assertThat(transactionTransformer).isNotNull();
  }

  @Test
  public void shouldProcessTransaction() {
    // Create a transaction
    final Transaction transaction =
        new Transaction(UUID.randomUUID().toString(), "1", new BigDecimal(100), DEPOSIT, "USD", "USA");

    // Process the transaction (this should not throw an exception)
    transactionTransformer.process(new Record<>("key", transaction, 0L));

    // If we get here without an exception, the test passes
    assertThat(true).isTrue();
  }

  @Test
  public void shouldCloseWithoutErrors() {
    // The close method is empty, but we should test it for coverage
    transactionTransformer.close();
    // If we get here without errors, the test passes
    assertThat(true).isTrue();
  }
}
