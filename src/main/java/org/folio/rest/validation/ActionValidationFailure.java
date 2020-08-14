package org.folio.rest.validation;

public class ActionValidationFailure extends ValidationResult {

  private final String errorMessage;

  public ActionValidationFailure(String errorMessage) {
    super(false);
    this.errorMessage = errorMessage;
  }

  public String getErrorMessage() {
    return errorMessage;
  }
}
