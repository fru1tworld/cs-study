package org.kafkainaction.serde;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.kafkainaction.model.Alert;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class AlertKeySerde implements Serializer<Alert>,
                                      Deserializer<Alert> {   //<1>

  public byte[] serialize(String topic, Alert key) {    //<2>
    if (key == null) {
      return null;
    }
    return key.getStageId().getBytes(StandardCharsets.UTF_8);   //<3>
  }

  public Alert deserialize(String topic, byte[] value) {    //<4>
    return null;
  }

  @Override
  public void configure(final Map<String, ?> configs, final boolean isKey) {
    Serializer.super.configure(configs, isKey);
  }

  @Override
  public void close() {
    Serializer.super.close();
  }

}
