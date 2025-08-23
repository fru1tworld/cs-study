package org.kafkainaction.producer;

import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.Headers;
import org.kafkainaction.model.Alert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;

@SuppressWarnings("unused")
public class AlertProducerMetricsInterceptor implements ProducerInterceptor<Alert, String> {    //<1>
  final static Logger log = LoggerFactory.getLogger(AlertProducerMetricsInterceptor.class);

  public ProducerRecord<Alert, String> onSend(ProducerRecord<Alert, String> record) {   //<2>
    Headers headers = record.headers();
    String kinactionTraceId = UUID.randomUUID().toString();
    headers.add("kinactionTraceId", kinactionTraceId.getBytes());                                     //<3>
    log.info("kinaction_info Created kinactionTraceId: {}", kinactionTraceId);
    return record;                                                                  //<4>
  }
  
  public void onAcknowledgement(RecordMetadata metadata, Exception exception) {       //<5>
    if (exception != null) {
      log.info("kinaction_error " + exception.getMessage());
    } else {
      log.info("kinaction_info topic = {} offset = {}",
               metadata.topic(), metadata.offset());
    }
  }

  @Override
  public void close() {

  }

  @Override
  public void configure(final Map<String, ?> map) {

  }
}
