package org.folio.rest.domain;

import static org.folio.rest.domain.EventType.FEE_FINE_BALANCE_CHANGED;
import static org.folio.rest.domain.EventType.LOAN_RELATED_FEE_FINE_CLOSED;
import static org.folio.rest.domain.EventType.LOG_RECORD;
import static org.folio.rest.domain.FeeFineKafkaTopic.FEE_FINE_BALANCE_CHANGED_TOPIC;
import static org.folio.rest.domain.FeeFineKafkaTopic.LOAN_RELATED_FEE_FINE_CLOSED_TOPIC;
import static org.folio.rest.domain.FeeFineKafkaTopic.LOG_RECORD_TOPIC;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.folio.kafka.KafkaTopicNameHelper;
import org.folio.kafka.services.KafkaEnvironmentProperties;
import org.junit.jupiter.api.Test;

class FeeFineKafkaTopicTest {
  private static final String TENANT_ID = "test_tenant";

  @Test
  void shouldUseExistingEventNamesAsTopicNames() {
    assertEquals("feesfines", FEE_FINE_BALANCE_CHANGED_TOPIC.moduleName());
    assertEquals(FEE_FINE_BALANCE_CHANGED.name(), FEE_FINE_BALANCE_CHANGED_TOPIC.topicName());

    assertEquals("feesfines", LOAN_RELATED_FEE_FINE_CLOSED_TOPIC.moduleName());
    assertEquals(LOAN_RELATED_FEE_FINE_CLOSED.name(), LOAN_RELATED_FEE_FINE_CLOSED_TOPIC.topicName());

    assertEquals("feesfines", LOG_RECORD_TOPIC.moduleName());
    assertEquals(LOG_RECORD.name(), LOG_RECORD_TOPIC.topicName());
  }

  @Test
  void shouldFormatFullTopicNameUsingFeesFinesModuleName() {
    String expectedTopic = KafkaTopicNameHelper.formatTopicName(
      KafkaEnvironmentProperties.environment(), TENANT_ID, "feesfines.FEE_FINE_BALANCE_CHANGED");

    assertEquals(expectedTopic, FEE_FINE_BALANCE_CHANGED_TOPIC.fullTopicName(TENANT_ID));
  }

  @Test
  void shouldMapEventTypeToKafkaTopic() {
    assertEquals(FEE_FINE_BALANCE_CHANGED_TOPIC, FeeFineKafkaTopic.from(FEE_FINE_BALANCE_CHANGED));
    assertEquals(LOAN_RELATED_FEE_FINE_CLOSED_TOPIC, FeeFineKafkaTopic.from(LOAN_RELATED_FEE_FINE_CLOSED));
    assertEquals(LOG_RECORD_TOPIC, FeeFineKafkaTopic.from(LOG_RECORD));
  }
}
