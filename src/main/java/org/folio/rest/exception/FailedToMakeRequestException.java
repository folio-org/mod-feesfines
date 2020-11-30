package org.folio.rest.exception;

public class FailedToMakeRequestException extends RuntimeException {
  public FailedToMakeRequestException(String message) {
    super(message);
  }
}
