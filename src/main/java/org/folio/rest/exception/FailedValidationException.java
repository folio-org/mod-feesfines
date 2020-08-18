package org.folio.rest.exception;

public class FailedValidationException extends RuntimeException{
  public FailedValidationException(String message) {
    super(message);
  }
}
