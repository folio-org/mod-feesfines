package org.folio.rest.domain;

import java.util.Arrays;

public enum AccountStatus {
  CLOSED("Closed");

  private final String value;
  AccountStatus(String statusValue) {
    this.value = statusValue;
  }

  public static AccountStatus forValue(String value) {
    return Arrays.stream(values())
      .filter(currentEnum -> currentEnum.value.equalsIgnoreCase(value))
      .findFirst()
      .orElse(null);
  }
}
