package org.folio.rest.service.action;

import java.util.Map;

import org.folio.rest.domain.Action;

import io.vertx.core.Context;

public class WaiveActionService extends DefaultActionService {

  public WaiveActionService(Map<String, String> headers, Context context) {
    super(Action.WAIVE, headers, context);
  }
}
