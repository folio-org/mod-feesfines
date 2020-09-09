package org.folio.rest.service.action;

import java.util.Map;

import org.folio.rest.domain.Action;

import io.vertx.core.Context;

public class PayActionService extends DefaultActionService {

  public PayActionService(Map<String, String> headers, Context context) {
    super(Action.PAY, headers, context);
  }
}
