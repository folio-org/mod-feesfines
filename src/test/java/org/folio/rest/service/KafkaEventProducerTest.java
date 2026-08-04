package org.folio.rest.service;

import static org.folio.rest.domain.EventType.FEE_FINE_BALANCE_CHANGED;
import static org.folio.rest.domain.FeeFineKafkaTopic.FEE_FINE_BALANCE_CHANGED_TOPIC;
import static org.folio.test.support.ApiTests.TENANT_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicReference;

import org.folio.kafka.headers.FolioKafkaHeaders;
import org.junit.jupiter.api.Test;

import io.vertx.core.Future;
import io.vertx.kafka.client.producer.KafkaHeader;
import io.vertx.kafka.client.producer.KafkaProducerRecord;

class KafkaEventProducerTest {

  @Test
  void shouldSendPayloadToTopicForEventType() {
    String payload = "{\"balance\":0}";
    AtomicReference<KafkaProducerRecord<String, String>> sentRecord = new AtomicReference<>();
    KafkaEventProducer producer = new KafkaEventProducer(TENANT_NAME, record -> {
      sentRecord.set(record);
      return Future.succeededFuture();
    });

    Future<Void> result = producer.publish(FEE_FINE_BALANCE_CHANGED, payload);

    assertTrue(result.succeeded());
    assertEquals(FEE_FINE_BALANCE_CHANGED_TOPIC.fullTopicName(TENANT_NAME), sentRecord.get().topic());
    assertEquals(FEE_FINE_BALANCE_CHANGED.name(), sentRecord.get().key());
    assertEquals(payload, sentRecord.get().value());
    assertEquals(TENANT_NAME, tenantHeader(sentRecord.get()));
  }

  @Test
  void shouldForwardSendFailure() {
    RuntimeException expectedFailure = new RuntimeException("Kafka send failed");
    KafkaEventProducer producer = new KafkaEventProducer(TENANT_NAME,
      record -> Future.failedFuture(expectedFailure));

    Future<Void> result = producer.publish(FEE_FINE_BALANCE_CHANGED, "{}");

    assertTrue(result.failed());
    assertSame(expectedFailure, result.cause());
  }

  private static String tenantHeader(KafkaProducerRecord<String, String> record) {
    return record.headers().stream()
      .filter(header -> FolioKafkaHeaders.TENANT_ID.equals(header.key()))
      .map(KafkaHeader::value)
      .map(Object::toString)
      .findFirst()
      .orElseThrow(() -> new AssertionError("Tenant Kafka header was not found"));
  }
}
