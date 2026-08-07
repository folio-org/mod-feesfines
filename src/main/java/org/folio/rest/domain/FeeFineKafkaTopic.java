package org.folio.rest.domain;

import org.folio.kafka.services.KafkaTopic;

public enum FeeFineKafkaTopic implements KafkaTopic {
  FEE_FINE_BALANCE_CHANGED_TOPIC(EventType.FEE_FINE_BALANCE_CHANGED),
  LOAN_RELATED_FEE_FINE_CLOSED_TOPIC(EventType.LOAN_RELATED_FEE_FINE_CLOSED),
  LOG_RECORD_TOPIC(EventType.LOG_RECORD);

  private static final String MODULE_NAME = "feesfines";

  private final EventType eventType;

  FeeFineKafkaTopic(EventType eventType) {
    this.eventType = eventType;
  }

  public static FeeFineKafkaTopic from(EventType eventType) {
    for (FeeFineKafkaTopic topic : values()) {
      if (topic.eventType == eventType) {
        return topic;
      }
    }

    throw new IllegalArgumentException("No Kafka topic for event type " + eventType);
  }

  @Override
  public String moduleName() {
    return MODULE_NAME;
  }

  @Override
  public String topicName() {
    return eventType.name();
  }
}
