package org.folio.rest.exception;

public class AccountNotFoundValidationException extends RuntimeException {
  public AccountNotFoundValidationException(String message) {
    super(message);
  }
}
