package org.folio.rest.service.action.validation;

import org.folio.rest.domain.MonetaryValue;

public class ActionValidationResult {
  private final String remainingAmount;
  private final String requestedAmount;

  public ActionValidationResult(String remainingAmount, String requestedAmount) {
    this.remainingAmount = remainingAmount;
    this.requestedAmount = requestedAmount;
  }

  public ActionValidationResult(MonetaryValue remainingAmount, MonetaryValue requestedAmount) {
    this(remainingAmount.toString(), requestedAmount.toString());
  }

  public String getRemainingAmount() {
    return remainingAmount;
  }

  public String getRequestedAmount() {
    return requestedAmount;
  }
}
