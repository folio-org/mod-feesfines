package org.folio.rest.utils;

import org.folio.rest.jaxrs.model.Error;
import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.jaxrs.model.Parameter;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;

import static org.junit.jupiter.api.Assertions.*;

class ErrorHelperTest {

  private static final String CONSTRAINT_NAME = "test_constraint";

  @Test
  void createValidationErrorMessage() {
    Errors message = ErrorHelper.createValidationErrorMessage("field", "value", "message");
    assertNotNull(message);
    assertEquals(1, message.getErrors().size());

    Error error = message.getErrors().get(0);
    assertEquals("message", error.getMessage());

    assertNotNull(error.getParameters());
    assertEquals(1, error.getParameters().size());

    Parameter parameter = error.getParameters().get(0);
    assertNotNull(parameter);
    assertNotNull(parameter.getKey());
    assertNotNull(parameter.getValue());
    assertEquals("field", parameter.getKey());
    assertEquals("value", parameter.getValue());
  }

  @Test
  void didUniqueConstraintViolationOccurNullResponse() {
    assertFalse(ErrorHelper.didUniqueConstraintViolationOccur(null, CONSTRAINT_NAME));
  }

  @Test
  void didUniqueConstraintViolationOccurNoEntity() {
    assertFalse(ErrorHelper.didUniqueConstraintViolationOccur(Response.accepted().build(), CONSTRAINT_NAME));
  }
}
