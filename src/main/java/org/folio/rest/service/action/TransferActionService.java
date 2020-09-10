package org.folio.rest.service.action;

import java.util.Map;

import org.folio.rest.domain.Action;
import org.folio.rest.service.action.validation.DefaultActionValidationService;

import io.vertx.core.Context;

public class TransferActionService extends ActionService {

  public TransferActionService(Map<String, String> headers, Context context) {
    super(Action.TRANSFER, new DefaultActionValidationService(headers, context), headers, context);
  }
}
