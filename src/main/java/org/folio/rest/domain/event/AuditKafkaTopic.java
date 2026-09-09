package org.folio.rest.domain.event;

import org.folio.kafka.services.KafkaTopic;
import org.folio.rest.domain.EventType;

public enum AuditKafkaTopic implements KafkaTopic {
  LOG_RECORD_TOPIC(EventType.LOG_RECORD);

  private final EventType eventType;

  AuditKafkaTopic(EventType eventType) {
    this.eventType = eventType;
  }

  @Override
  public String moduleName() {
    return "audit";
  }

  @Override
  public String topicName() {
    return eventType.name();
  }

  @Override
  public int numPartitions() {
    return 10;
  }
}
