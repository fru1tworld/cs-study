package org.kafkainaction.util;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.errors.TopicExistsException;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

public class CreateTopic {

  public static void main(String[] args) {

    Properties kaProperties = new Properties();
    kaProperties.put("bootstrap.servers", "localhost:9092,localhost:9093");
    kaProperties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    kaProperties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    NewTopic requestedTopic = new NewTopic("kinaction_selfserviceTopic", 2, (short) 2);
    AdminClient client = AdminClient.create(kaProperties);
    CreateTopicsResult topicResult = client.createTopics(List.of(requestedTopic));
    try {
      topicResult.values().get("kinaction_selfserviceTopic").get();
    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (ExecutionException e) {
      if (!(e.getCause() instanceof TopicExistsException)) {
        throw new RuntimeException(e.getMessage(), e);
      }
      e.printStackTrace();
    }
  }
}
