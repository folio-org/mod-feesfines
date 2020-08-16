package org.folio.rest.domain;

import java.util.Arrays;
import java.util.List;

public enum FeeFineActionResult {
  PAID_FULLY("Paid fully"),
  PAID_PARTIALLY("Paid partially"),
  WAIVED_FULLY("Waived fully"),
  WAIVED_PARTIALLY("Waived partially"),
  TRANSFERRED_FULLY("Transferred fully"),
  TRANSFERRED_PARTIALLY("Transferred partially"),
  REFUNDED_FULLY("Refunded fully"),
  REFUNDED_PARTIALLY("Refunded partially"),
  CANCELLED("Cancelled as error");

  private final String name;

  private static final List<String> closingStatuses = Arrays.asList(
    PAID_FULLY.getName(),
    WAIVED_FULLY.getName(),
    TRANSFERRED_FULLY.getName(),
    REFUNDED_FULLY.getName(),
    CANCELLED.getName());

  FeeFineActionResult(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public static boolean shouldCloseAccount(String statusName) {
    return closingStatuses.contains(statusName);
  }
}
