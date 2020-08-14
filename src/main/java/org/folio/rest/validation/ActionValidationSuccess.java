package org.folio.rest.validation;

public class ActionValidationSuccess extends ValidationResult {

  private final Double remainingAmount;

  public ActionValidationSuccess(Double remainingAmount) {
    super(true);
    this.remainingAmount = remainingAmount;
  }

  public Double getRemainingAmount() {
    return remainingAmount;
  }
}
