package org.folio.rest.service.action;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

import org.folio.rest.domain.Action;
import org.folio.rest.domain.ActionRequest;
import org.folio.rest.domain.MonetaryValue;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.CancelActionRequest;
import org.folio.rest.jaxrs.model.Feefineaction;

import io.vertx.core.Context;
import io.vertx.core.Future;

public class CancelActionService extends ActionService {

  public CancelActionService(Map<String, String> okapiHeaders, Context vertxContext) {
    super(okapiHeaders, vertxContext);
  }

  @Override
  public Future<ActionContext> executeAction(String accountId,
    ActionRequest request) {

    return performAction(Action.CANCELLED, accountId, request);
  }

  @Override
  protected Future<ActionContext> createFeeFineAction(ActionContext context) {
    final CancelActionRequest request = (CancelActionRequest) context.getRequest();
    final Account account = context.getAccount();
    final Action action = context.getAction();

    MonetaryValue remainingAmountAfterAction = new MonetaryValue(account.getRemaining());

    Feefineaction feeFineAction = new Feefineaction()
      .withComments(request.getComments())
      .withNotify(request.getNotifyPatron())
      .withTransactionInformation(request.getTransactionInfo())
      .withCreatedAt(request.getServicePointId())
      .withSource(request.getUserName())
      .withPaymentMethod(request.getPaymentMethod())
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

    return cancelValidationService.validate(context.getAccount(), null)
      .map(result -> context.withRequestedAmount(null));
  }
}
