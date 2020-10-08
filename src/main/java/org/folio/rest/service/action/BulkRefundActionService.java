package org.folio.rest.service.action;

import java.util.Map;

import org.folio.rest.domain.Action;
import org.folio.rest.service.action.validation.RefundActionValidationService;

import io.vertx.core.Context;

public class BulkRefundActionService extends BulkActionService {

  public BulkRefundActionService(Map<String, String> headers, Context context) {
    super(Action.REFUND, new RefundActionValidationService(headers, context), headers, context);
  }
}
