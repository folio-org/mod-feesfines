package org.folio.rest.service.action;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

import org.folio.rest.domain.Action;
import org.folio.rest.domain.MonetaryValue;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.CancelActionRequest;
import org.folio.rest.jaxrs.model.Feefineaction;
import org.folio.rest.service.action.context.ActionContext;
import org.folio.rest.service.action.validation.CancelActionValidationService;

import io.vertx.core.Context;
import io.vertx.core.Future;

public class CancelActionService extends ActionService {

  public CancelActionService(Map<String, String> headers, Context context) {
    super(Action.CANCEL, new CancelActionValidationService(headers, context), headers, context);
  }

  @Override
  protected Future<ActionContext> createFeeFineActions(ActionContext context) {
    final CancelActionRequest request = (CancelActionRequest) context.getRequest();
    final Account account = context.getAccount();

    MonetaryValue remainingAmountAfterAction = new MonetaryValue(account.getRemaining());

    Feefineaction feeFineAction = new Feefineaction()
      .withComments(request.getComments())
      .withNotify(request.getNotifyPatron())
      .withCreatedAt(request.getServicePointId())
      .withSource(request.getUserName())
      .withAccountId(context.getAccountId())
      .withUserId(account.getUserId())
      .withBalance(remainingAmountAfterAction.toDouble())
      .withTypeAction(action.getFullResult())
      .withId(UUID.randomUUID().toString())
      .withDateAction(new Date())
      .withAccountId(context.getAccountId());

    return feeFineActionRepository.save(feeFineAction)
      .map(context.withFeeFineAction(feeFineAction)
        .withShouldCloseAccount(true));
  }

  @Override
  protected Future<ActionContext> validateAction(ActionContext context) {

    return validationService.validate(context.getAccountId(), context.getAccount(), null)
      .map(result -> context.withRequestedAmount(null));
  }
}
