package org.folio.rest.domain;

import java.util.Arrays;

public enum FeeFineStatus {
  OPEN("Open"),
  CLOSED("Closed");

  private final String value;
  FeeFineStatus(String statusValue) {
    this.value = statusValue;
  }

  public String getValue() {
    return value;
  }

  public static FeeFineStatus forValue(String value) {
    return Arrays.stream(values())
      .filter(currentEnum -> currentEnum.value.equalsIgnoreCase(value))
      .findFirst()
      .orElse(null);
  }
}
