package org.folio.rest.service.action;

import java.util.Map;

import org.folio.rest.domain.Action;
import org.folio.rest.service.action.validation.DefaultActionValidationService;

import io.vertx.core.Context;

public class PayActionService extends ActionService {

  public PayActionService(Map<String, String> headers, Context context) {
    super(Action.PAY, new DefaultActionValidationService(headers, context), headers, context);
  }
}
