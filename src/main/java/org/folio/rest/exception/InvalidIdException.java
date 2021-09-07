package org.folio.rest.exception;

import lombok.Getter;

@Getter
public class InvalidIdException extends RuntimeException {
  private final Class<?> entityType;
  private final String id;

  public InvalidIdException(Class<?> entityType, String id) {
    super(String.format("Invalid %s ID: %s", entityType.getSimpleName(), id));
    this.entityType = entityType;
    this.id = id;
  }
}
