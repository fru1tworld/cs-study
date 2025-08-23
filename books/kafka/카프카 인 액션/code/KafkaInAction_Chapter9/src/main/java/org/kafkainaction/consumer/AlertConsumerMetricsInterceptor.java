package org.kafkainaction.consumer;

import org.apache.kafka.clients.consumer.ConsumerInterceptor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.kafkainaction.model.Alert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@SuppressWarnings("unused")
public class AlertConsumerMetricsInterceptor implements ConsumerInterceptor<Alert, String> {

  final static Logger log = LoggerFactory.getLogger(AlertConsumerMetricsInterceptor.class);

  public ConsumerRecords<Alert, String>
  onConsume(ConsumerRecords<Alert, String> records) {
    if (records.isEmpty()) {
      return records;
    } else {
      for (ConsumerRecord<Alert, String> record : records) {
        Headers headers = record.headers();       // <2>
        for (Header header : headers) {
          if ("kinactionTraceId".equals(header.key())) { // <3>
            log.info("kinactionTraceId is: " + new String(header.value()));
          }
        }
      }
    }
    return records; // <4>
  }

  @Override
  public void onCommit(final Map<TopicPartition, OffsetAndMetadata> map) {

  }

  @Override
  public void close() {

  }

  @Override
  public void configure(final Map<String, ?> map) {

  }
}
