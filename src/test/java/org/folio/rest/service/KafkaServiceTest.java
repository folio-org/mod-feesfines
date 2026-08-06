package org.folio.rest.service;

import static org.folio.test.support.ApiTests.TENANT_NAME;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

import org.folio.kafka.services.KafkaTopic;
import org.folio.rest.domain.FeeFineKafkaTopic;
import org.junit.jupiter.api.Test;

import io.vertx.core.Future;

class KafkaServiceTest {

  @Test
  void shouldCreateFeeFineTopicsForTenant() {
    AtomicReference<KafkaTopic[]> topicsReference = new AtomicReference<>();
    AtomicReference<String> tenantReference = new AtomicReference<>();

    KafkaService kafkaService = new KafkaService((topics, tenantId) -> {
      topicsReference.set(topics);
      tenantReference.set(tenantId);
      return Future.succeededFuture();
    });

    Future<Void> result = kafkaService.createTopics(TENANT_NAME);

    assertTrue(result.succeeded());
    assertArrayEquals(FeeFineKafkaTopic.values(), topicsReference.get());
    assertEquals(TENANT_NAME, tenantReference.get());
  }

  @Test
  void shouldDeleteFeeFineTopicsForTenant() {
    AtomicReference<KafkaTopic[]> topicsReference = new AtomicReference<>();
    AtomicReference<String> tenantReference = new AtomicReference<>();

    KafkaService kafkaService = new KafkaService(noOpTopicAdmin(), (topics, tenantId) -> {
      topicsReference.set(topics);
      tenantReference.set(tenantId);
      return Future.succeededFuture();
    });

    Future<Void> result = kafkaService.deleteTopics(TENANT_NAME);

    assertTrue(result.succeeded());
    assertArrayEquals(FeeFineKafkaTopic.values(), topicsReference.get());
    assertEquals(TENANT_NAME, tenantReference.get());
  }

  @Test
  void shouldSkipTopicCreationWhenKafkaIsNotConfigured() {
    AtomicReference<KafkaTopic[]> topicsReference = new AtomicReference<>();
    KafkaService kafkaService = new KafkaService((topics, tenantId) -> {
      topicsReference.set(topics);
      return Future.succeededFuture();
    }, () -> false);

    Future<Void> result = kafkaService.createTopics(TENANT_NAME);

    assertTrue(result.succeeded());
    assertNull(topicsReference.get());
  }

  @Test
  void shouldSkipTopicDeletionWhenKafkaIsNotConfigured() {
    AtomicReference<KafkaTopic[]> topicsReference = new AtomicReference<>();
    KafkaService kafkaService = new KafkaService(noOpTopicAdmin(), (topics, tenantId) -> {
      topicsReference.set(topics);
      return Future.succeededFuture();
    }, () -> false);

    Future<Void> result = kafkaService.deleteTopics(TENANT_NAME);

    assertTrue(result.succeeded());
    assertNull(topicsReference.get());
  }

  @Test
  void shouldRequireExplicitKafkaHostConfiguration() {
    assertFalse(KafkaService.isKafkaConfigured(null, null, null));
    assertTrue(KafkaService.isKafkaConfigured("kafka", null, null));
    assertTrue(KafkaService.isKafkaConfigured(null, "kafka", null));
    assertTrue(KafkaService.isKafkaConfigured(null, null, "kafka"));
  }

  private static BiFunction<KafkaTopic[], String, Future<Void>> noOpTopicAdmin() {
    return (topics, tenantId) -> Future.succeededFuture();
  }
}
