package org.folio.rest.domain;

import static java.util.Arrays.stream;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.folio.rest.utils.ActionResultAdapter;

public enum Action {
  PAY("Paid partially", "Paid fully", ActionResultAdapter.PAY),
  WAIVE("Waived partially", "Waived fully", ActionResultAdapter.WAIVE),
  TRANSFER("Transferred partially", "Transferred fully", ActionResultAdapter.TRANSFER),
  REFUND("Refunded partially", "Refunded fully", ActionResultAdapter.REFUND),
  CREDIT("Credited partially", "Credited fully", null),
  CANCELLED(null, "Cancelled as error", ActionResultAdapter.CANCEL);

  private final String partialResult;
  private final String fullResult;
  private final ActionResultAdapter actionResultAdapter;

  private static final List<String> fullResults = stream(values())
    .map(Action::getFullResult)
    .filter(Objects::nonNull)
    .collect(Collectors.toList());

  private static final List<String> partialResults = stream(values())
    .map(Action::getPartialResult)
    .filter(Objects::nonNull)
    .collect(Collectors.toList());

  Action(String partialResult, String fullResult, ActionResultAdapter actionResultAdapter) {
    this.partialResult = partialResult;
    this.fullResult = fullResult;
    this.actionResultAdapter = actionResultAdapter;
  }

  public String getPartialResult() {
    return partialResult;
  }

  public String getFullResult() {
    return fullResult;
  }

  public String getResult(boolean isFull) {
    return isFull ? getFullResult() : getPartialResult();
  }

  public boolean isActionForResult(String actionResult) {
    if (actionResult.isBlank()) {
      return false;
    }
    return actionResult.equals(partialResult) || actionResult.equals(fullResult);
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

  public ActionResultAdapter getActionResultAdapter() {
    return actionResultAdapter;
  }
}
