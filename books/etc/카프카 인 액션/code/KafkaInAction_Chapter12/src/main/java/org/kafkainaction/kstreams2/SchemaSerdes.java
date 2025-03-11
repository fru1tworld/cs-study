package org.kafkainaction.kstreams2;

import org.apache.avro.specific.SpecificRecord;

import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;

import static io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG;

public class SchemaSerdes {

  static <T extends SpecificRecord> SpecificAvroSerde<T> getSpecificAvroSerde(final Properties envProps) {
    final Map<String, String>
        serdeConfig =
        Map.of(SCHEMA_REGISTRY_URL_CONFIG, 
               Optional.ofNullable(envProps.getProperty("schema.registry.url")).orElse("")
        );
    final SpecificAvroSerde<T> specificAvroSerde = new SpecificAvroSerde<>();

    specificAvroSerde.configure(serdeConfig, false);
    return specificAvroSerde;
  }
}
