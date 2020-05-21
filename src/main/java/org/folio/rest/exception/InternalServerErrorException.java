package org.folio.rest.exception;

public class InternalServerErrorException extends RuntimeException {

  public InternalServerErrorException(String reason) {
    super(reason);
  }
}
