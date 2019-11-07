package org.folio.rest.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import javax.ws.rs.core.Response;

import org.folio.rest.jaxrs.model.Error;
import org.folio.rest.jaxrs.model.Errors;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

class ErrorHelperTest {

  private static final String CONSTRAINT_NAME = "test_constraint";

  @Test
  void didUniqueConstraintViolationOccurNullResponse() {
    assertFalse(ErrorHelper.didUniqueConstraintViolationOccur(null, CONSTRAINT_NAME));
  }

  @Test
  void didUniqueConstraintViolationOccurNoEntity() {
    assertFalse(ErrorHelper.didUniqueConstraintViolationOccur(Response.accepted().build(), CONSTRAINT_NAME));
  }

  @Test
  void createErrorsTest(){
    Error e1 = new Error();
    Error e2 = new Error();
    Errors errors = ErrorHelper.createErrors(e1, e2);

    assertNotNull(errors);
    assertNotNull(errors.getErrors());
    assertEquals(2, errors.getErrors().size());
    assertEquals(e1, errors.getErrors().get(0));
    assertEquals(e2, errors.getErrors().get(1));
  }
}
