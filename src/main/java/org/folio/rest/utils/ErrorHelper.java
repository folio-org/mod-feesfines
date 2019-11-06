package org.folio.rest.utils;

import java.util.ArrayList;
import java.util.List;

import org.folio.rest.jaxrs.model.Error;
import org.folio.rest.jaxrs.model.Errors;
import org.folio.rest.jaxrs.model.Parameter;
import org.folio.rest.tools.RTFConsts;

import javax.ws.rs.core.Response;

public class ErrorHelper {

  private ErrorHelper() {
    throw new IllegalStateException("Utility class");
  }

  public static Errors createValidationErrorMessage(String field, String value, String message) {
    Errors e = new Errors();
    Error error = new Error();
    Parameter p = new Parameter();
    p.setKey(field);
    p.setValue(value);
    error.getParameters().add(p);
    error.setMessage(message);
    error.setCode("-1");
    error.setType(RTFConsts.VALIDATION_FIELD_ERROR);
    List<Error> l = new ArrayList<>();
    l.add(error);
    e.setErrors(l);
    return e;
  }

  public static boolean didUniqueConstraintViolationOccur(Response response, String constraintName) {
    return response != null &&
      response.hasEntity() &&
      response.getEntity() instanceof String &&
      ((String) response.getEntity())
        .startsWith(("duplicate key value violates unique constraint \"" + constraintName + "\""));
  }
}
