package org.folio.rest.service.action;

import java.util.Map;

import org.folio.rest.domain.Action;

import io.vertx.core.Context;

public class TransferActionService extends DefaultActionService {

  public TransferActionService(Map<String, String> headers, Context context) {
    super(Action.TRANSFER, headers, context);
  }
}
