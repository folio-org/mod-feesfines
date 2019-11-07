package org.folio.rest.utils;

import java.util.Arrays;
import java.util.Optional;

import javax.ws.rs.core.Response;

import org.folio.rest.jaxrs.model.Error;
import org.folio.rest.jaxrs.model.Errors;

public class ErrorHelper {

  private static final String UNIQUE_CONSTRAINT_MSG_TEMPLATE = "duplicate key value violates unique constraint \"%s\"";

  private ErrorHelper() {
  }

  public static boolean didUniqueConstraintViolationOccur(Response response, String constraintName) {
    return Optional.ofNullable(response)
      .filter(Response::hasEntity)
      .map(Response::getEntity)
      .filter(r -> r instanceof String)
      .filter(r -> ((String) r).contains(String.format(UNIQUE_CONSTRAINT_MSG_TEMPLATE, constraintName)))
      .isPresent();
  }

  public static Errors createErrors(Error... errors){
    return new Errors()
      .withErrors(Arrays.asList(errors));
  }
}
