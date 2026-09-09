package org.folio.rest.domain;

import static org.folio.rest.domain.EventType.FEE_FINE_BALANCE_CHANGED;
import static org.folio.rest.domain.EventType.LOAN_RELATED_FEE_FINE_CLOSED;
import static org.folio.rest.domain.event.FeeFineKafkaTopic.LOAN_RELATED_FEE_FINE_CLOSED_TOPIC;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.folio.kafka.KafkaTopicNameHelper;
import org.folio.kafka.services.KafkaEnvironmentProperties;
import org.folio.rest.domain.event.FeeFineKafkaTopic;
import org.junit.jupiter.api.Test;

class FeeFineKafkaTopicTest {
  private static final String TENANT_ID = "test_tenant";

  @Test
  void shouldUseExistingEventNamesAsTopicNames() {
    assertEquals("feesfines", FeeFineKafkaTopic.FEE_FINE_BALANCE_CHANGED_TOPIC.moduleName());
    assertEquals(FEE_FINE_BALANCE_CHANGED.name(), FeeFineKafkaTopic.FEE_FINE_BALANCE_CHANGED_TOPIC.topicName());

    assertEquals("feesfines", LOAN_RELATED_FEE_FINE_CLOSED_TOPIC.moduleName());
    assertEquals(LOAN_RELATED_FEE_FINE_CLOSED.name(), LOAN_RELATED_FEE_FINE_CLOSED_TOPIC.topicName());
  }

  @Test
  void shouldFormatFullTopicNameUsingFeesFinesModuleName() {
    String expectedTopic = KafkaTopicNameHelper.formatTopicName(
      KafkaEnvironmentProperties.environment(), TENANT_ID, "feesfines.FEE_FINE_BALANCE_CHANGED");

    assertEquals(expectedTopic, FeeFineKafkaTopic.FEE_FINE_BALANCE_CHANGED_TOPIC.fullTopicName(TENANT_ID));
  }

  @Test
  void shouldMapEventTypeToKafkaTopic() {
    assertEquals(FeeFineKafkaTopic.FEE_FINE_BALANCE_CHANGED_TOPIC, FeeFineKafkaTopic.from(FEE_FINE_BALANCE_CHANGED));
    assertEquals(LOAN_RELATED_FEE_FINE_CLOSED_TOPIC, FeeFineKafkaTopic.from(LOAN_RELATED_FEE_FINE_CLOSED));
  }
}
