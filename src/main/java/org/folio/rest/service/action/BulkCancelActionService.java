package org.folio.rest.service.action;

import java.util.Map;

import org.folio.rest.domain.Action;
import org.folio.rest.service.action.context.BulkActionContext;
import org.folio.rest.service.action.validation.CancelActionValidationService;
import org.folio.rest.utils.amountsplitter.DummyBulkSplitterStrategy;

import io.vertx.core.Context;
import io.vertx.core.Future;

public class BulkCancelActionService extends BulkActionService {

  public BulkCancelActionService(Map<String, String> headers, Context context) {
    super(Action.CANCEL, new CancelActionValidationService(headers, context),
      new DummyBulkSplitterStrategy(), headers, context);
  }

  @Override
  protected Future<BulkActionContext> validateAction(BulkActionContext context) {
    return validationService.validate(context.getAccounts(), null)
      .map(result -> context.withRequestedAmount(null));
  }

}
