package org.folio.rest.service.action;

import java.util.Map;

import org.folio.rest.domain.Action;
import org.folio.rest.service.action.validation.DefaultActionValidationService;

import io.vertx.core.Context;

public class WaiveActionService extends ActionService {

  public WaiveActionService(Map<String, String> headers, Context context) {
    super(Action.WAIVE, new DefaultActionValidationService(headers, context), headers, context);
  }
}
