package org.folio.rest.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import javax.ws.rs.core.Response;

import org.folio.rest.jaxrs.model.Error;
import org.folio.rest.jaxrs.model.Errors;
import org.junit.Test;

public class ErrorHelperTest {

  private static final String TABLE_NAME = "test_table";

  @Test
  public void didUniqueConstraintViolationOccurNullResponse() {
    assertFalse(ErrorHelper.uniqueNameConstraintViolated(null, TABLE_NAME));
  }

  @Test
  public void didUniqueConstraintViolationOccurNoEntity() {
    assertFalse(ErrorHelper.uniqueNameConstraintViolated(Response.accepted().build(), TABLE_NAME));
  }

  @Test
  public void createErrorsTest(){
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
