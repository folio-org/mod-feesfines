package org.folio.rest.domain;

import static java.util.Arrays.stream;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public enum Action {
  PAY("Paid partially", "Paid fully"),
  WAIVE("Waived partially", "Waived fully"),
  TRANSFER("Transferred partially", "Transferred fully"),
  REFUND("Refunded partially", "Refunded fully"),
  CANCELLED(null, "Cancelled as error");

  private final String partialResult;
  private final String fullResult;

  private static final List<String> fullResults = stream(values())
    .map(Action::getFullResult)
    .filter(Objects::nonNull)
    .collect(Collectors.toList());

  private static final List<String> partialResults = stream(values())
    .map(Action::getPartialResult)
    .filter(Objects::nonNull)
    .collect(Collectors.toList());

  Action(String partialResult, String fullResult) {
    this.partialResult = partialResult;
    this.fullResult = fullResult;
  }

  public String getPartialResult() {
    return partialResult;
  }

  public String getFullResult() {
    return fullResult;
  }

  public static boolean isFullActionResult(String result) {
    return fullResults.contains(result);
  }

  public static boolean isPartialActionResult(String result) {
    return partialResults.contains(result);
  }

  public static boolean isActionResult(String result) {
    return isPartialActionResult(result) || isFullActionResult(result);
  }
}