package org.folio.rest.service.action.validation;

import org.folio.rest.domain.MonetaryValue;

public class ActionValidationResult {
  private final String remainingAmount;
  private final String formattedAmount;

  public ActionValidationResult(String remainingAmount, String formattedAmount) {
    this.remainingAmount = remainingAmount;
    this.formattedAmount = formattedAmount;
  }

  public ActionValidationResult(MonetaryValue remainingAmount, MonetaryValue formattedAmount) {
    this.remainingAmount = remainingAmount.toString();
    this.formattedAmount = formattedAmount.toString();
  }

  public String getRemainingAmount() {
    return remainingAmount;
  }

  public String getFormattedAmount() {
    return formattedAmount;
  }
}
