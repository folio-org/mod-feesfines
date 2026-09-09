package org.folio.rest.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.folio.kafka.KafkaTopicNameHelper;
import org.folio.kafka.services.KafkaEnvironmentProperties;
import org.folio.rest.domain.event.FeeFineKafkaTopic;
import org.junit.jupiter.api.Test;

class FeeFineKafkaTopicTest {
  private static final String TENANT_ID = "test_tenant";

  @Test
  void shouldFormatFullTopicNameUsingFeesFinesModuleName() {
    String expectedTopic = KafkaTopicNameHelper.formatTopicName(
      KafkaEnvironmentProperties.environment(), TENANT_ID, "feesfines.FEE_FINE_BALANCE_CHANGED");

    assertEquals(expectedTopic, FeeFineKafkaTopic.FEE_FINE_BALANCE_CHANGED.fullTopicName(TENANT_ID));
  }

}
