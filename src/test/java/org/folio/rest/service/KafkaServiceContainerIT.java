package org.folio.rest.service;

import static org.folio.test.support.KafkaTestHelper.waitFor;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.Arrays;
import java.util.List;

import org.folio.rest.domain.FeeFineKafkaTopic;
import org.folio.test.support.KafkaTestHelper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.vertx.core.Vertx;

class KafkaServiceContainerIT {
  private static final String KAFKA_HOST_ENV = "KAFKA_HOST";
  private static final String KAFKA_PORT_ENV = "KAFKA_PORT";
  private static final String TENANT_ID = "kafka_test_tenant";

  private static KafkaTestHelper kafkaHelper;

  private Vertx vertx;

  @BeforeAll
  static void beforeAll() {
    assumeTrue(isBlankEnvironmentVariable(KAFKA_HOST_ENV), KAFKA_HOST_ENV + " must be unset or blank");
    assumeTrue(isBlankEnvironmentVariable(KAFKA_PORT_ENV), KAFKA_PORT_ENV + " must be unset or blank");

    kafkaHelper = KafkaTestHelper.getInstance();
  }

  @AfterAll
  static void afterAll() {
    if (kafkaHelper != null) {
      kafkaHelper.close();
    }
  }

  @BeforeEach
  void beforeEach() {
    vertx = Vertx.vertx();
    kafkaHelper.deleteTopics(topicNames());
  }

  @AfterEach
  void afterEach() {
    try {
      kafkaHelper.deleteTopics(topicNames());
    } finally {
      if (vertx != null) {
        waitFor(vertx.close());
        vertx = null;
      }
    }
  }

  @Test
  void shouldCreateAndDeleteFeeFineTopicsInKafka() {
    KafkaService kafkaService = new KafkaService(vertx);

    waitFor(kafkaService.createTopics(TENANT_ID));
    kafkaHelper.verifyTopicsExist(topicNames());

    waitFor(kafkaService.deleteTopics(TENANT_ID));
    kafkaHelper.verifyTopicsDoNotExist(topicNames());
  }

  private static List<String> topicNames() {
    return Arrays.stream(FeeFineKafkaTopic.values())
      .map(topic -> topic.fullTopicName(TENANT_ID))
      .toList();
  }

  private static boolean isBlankEnvironmentVariable(String name) {
    String value = System.getenv(name);
    return value == null || value.isBlank();
  }
}
