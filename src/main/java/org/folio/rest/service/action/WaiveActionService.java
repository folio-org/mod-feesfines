package org.folio.rest.service.action;

import java.util.Map;

import org.folio.rest.domain.Action;
import org.folio.rest.domain.ActionRequest;

import io.vertx.core.Context;
import io.vertx.core.Future;

public class WaiveActionService extends ActionService {

  public WaiveActionService(Map<String, String> okapiHeaders, Context vertxContext) {
    super(okapiHeaders, vertxContext);
  }

  @Override
  public Future<ActionContext> executeAction(String accountId,
    ActionRequest request) {

    return performAction(Action.WAIVE, accountId, request);
  }
}
