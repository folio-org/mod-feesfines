package org.folio.rest.utils;

import java.util.Arrays;
import java.util.Optional;

import javax.ws.rs.core.Response;

import org.folio.rest.jaxrs.model.Error;
import org.folio.rest.jaxrs.model.Errors;

public class ErrorHelper {
  private static final String DUPLICATE_NAME_MSG_TEMPLATE =
    "lower(f_unaccent(jsonb ->> 'name'::text)) value already exists in table %s";

  private ErrorHelper() {
  }

  public static boolean uniqueNameConstraintViolated(Response response, String tableName) {
    return Optional.ofNullable(response)
      .filter(Response::hasEntity)
      .map(Response::getEntity)
      .filter(entity -> entity instanceof Errors)
      .map(Errors.class::cast)
      .map(Errors::getErrors)
      .filter(errors -> !errors.isEmpty())
      .map(errors -> errors.get(0))
      .map(Error::getMessage)
      .filter(msg -> msg.contains(String.format(DUPLICATE_NAME_MSG_TEMPLATE, tableName)))
      .isPresent();
  }

  public static Errors createErrors(Error... errors){
    return new Errors()
      .withErrors(Arrays.asList(errors));
  }

  public static Errors createError(String message, String code) {
    Error error = new Error()
      .withMessage(message)
      .withCode(code);

    return createErrors(error);
  }
}
