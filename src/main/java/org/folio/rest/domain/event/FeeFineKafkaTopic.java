package org.folio.rest.domain.event;

import org.folio.kafka.services.KafkaTopic;

public enum FeeFineKafkaTopic implements KafkaTopic {
  FEE_FINE_BALANCE_CHANGED,
  LOAN_RELATED_FEE_FINE_CLOSED;

  private static final String MODULE_NAME = "feesfines";

  @Override
  public String moduleName() {
    return MODULE_NAME;
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
