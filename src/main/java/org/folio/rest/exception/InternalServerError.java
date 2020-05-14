package org.folio.rest.exception;

public class InternalServerError extends RuntimeException {

  public InternalServerError(String reason) {
    super(reason);
  }
}
