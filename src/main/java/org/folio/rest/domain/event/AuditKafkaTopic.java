package org.folio.rest.domain.event;

import org.folio.kafka.services.KafkaTopic;

public enum AuditKafkaTopic implements KafkaTopic {
  LOG_RECORD;

  @Override
  public String moduleName() {
    return "audit";
  }

  @Override
  public String topicName() {
    return name();
  }

  @Override
  public int numPartitions() {
    return 10;
  }
}
