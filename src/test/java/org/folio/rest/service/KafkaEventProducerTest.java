package org.folio.rest.service;

import static org.folio.rest.domain.EventType.FEE_FINE_BALANCE_CHANGED;
import static org.folio.rest.domain.FeeFineKafkaTopic.FEE_FINE_BALANCE_CHANGED_TOPIC;
import static org.folio.test.support.ApiTests.TENANT_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
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
    KafkaEventProducer producer = new KafkaEventProducer(producerRecord -> {
      sentRecord.set(producerRecord);
      return Future.succeededFuture();
    });

    Future<Void> result = producer.publish(FEE_FINE_BALANCE_CHANGED, payload, okapiHeaders());

    assertTrue(result.succeeded());
    assertEquals(FEE_FINE_BALANCE_CHANGED_TOPIC.fullTopicName(TENANT_NAME), sentRecord.get().topic());
    assertEquals(FEE_FINE_BALANCE_CHANGED.name(), sentRecord.get().key());
    assertEquals(payload, sentRecord.get().value());
    assertEquals(TENANT_NAME, tenantHeader(sentRecord.get()));
  }

  @Test
  void shouldPropagateOkapiHeaders() {
    String okapiToken = "test-token";
    String requestId = "request-id";
    String userId = "user-id";
    Map<String, String> okapiHeaders = Map.of(
      "X-Okapi-Tenant", TENANT_NAME,
      "X-Okapi-Token", okapiToken,
      "X-Okapi-Request-Id", requestId,
      "X-Okapi-User-Id", userId);
    AtomicReference<KafkaProducerRecord<String, String>> sentRecord = new AtomicReference<>();
    KafkaEventProducer producer = new KafkaEventProducer(producerRecord -> {
      sentRecord.set(producerRecord);
      return Future.succeededFuture();
    });

    Future<Void> result = producer.publish(FEE_FINE_BALANCE_CHANGED, "{\"balance\":0}", okapiHeaders);

    assertTrue(result.succeeded());
    assertEquals(okapiToken, headerValue(sentRecord.get(), "X-Okapi-Token"));
    assertEquals(requestId, headerValue(sentRecord.get(), "X-Okapi-Request-Id"));
    assertEquals(userId, headerValue(sentRecord.get(), "X-Okapi-User-Id"));
  }

  @Test
  void shouldForwardSendFailure() {
    RuntimeException expectedFailure = new RuntimeException("Kafka send failed");
    KafkaEventProducer producer = new KafkaEventProducer(
      producerRecord -> Future.failedFuture(expectedFailure));

    Future<Void> result = producer.publish(FEE_FINE_BALANCE_CHANGED, "{}", okapiHeaders());

    assertTrue(result.failed());
    assertSame(expectedFailure, result.cause());
  }

  private static Map<String, String> okapiHeaders() {
    return Map.of("X-Okapi-Tenant", TENANT_NAME);
  }

  private static String tenantHeader(KafkaProducerRecord<String, String> producerRecord) {
    return headerValue(producerRecord, FolioKafkaHeaders.TENANT_ID);
  }

  private static String headerValue(KafkaProducerRecord<String, String> producerRecord, String headerName) {
    return producerRecord.headers().stream()
      .filter(header -> headerName.equals(header.key()))
      .map(KafkaHeader::value)
      .map(Object::toString)
      .findFirst()
      .orElseThrow(() -> new AssertionError("Kafka header was not found: " + headerName));
  }
}
