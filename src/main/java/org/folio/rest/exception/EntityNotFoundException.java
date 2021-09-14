package org.folio.rest.exception;

public class EntityNotFoundException extends RuntimeException {
  public EntityNotFoundException(String message) {
    super(message);
  }

  public EntityNotFoundException(Class<?> entityType, String entityId) {
    super(String.format("Failed to find %s %s", entityType.getSimpleName(), entityId));
  }

}
