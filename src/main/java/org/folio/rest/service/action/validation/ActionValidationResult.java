package org.folio.rest.service.action.validation;

public class ActionValidationResult {
    private final String remainingAmount;
    private final String formattedAmount;

    public ActionValidationResult(String remainingAmount, String formattedAmount) {
      this.remainingAmount = remainingAmount;
      this.formattedAmount = formattedAmount;
    }

    public String getRemainingAmount() {
      return remainingAmount;
    }

    public String getFormattedAmount() {
      return formattedAmount;
    }
}
