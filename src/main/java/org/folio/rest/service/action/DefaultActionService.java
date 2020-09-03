package org.folio.rest.service.action;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

import org.folio.rest.domain.Action;
import org.folio.rest.domain.MonetaryValue;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.ActionRequest;
import org.folio.rest.jaxrs.model.Feefineaction;
import org.folio.rest.service.action.validation.DefaultActionValidationService;

import io.vertx.core.Context;
import io.vertx.core.Future;

public class DefaultActionService extends ActionService {

  public DefaultActionService(Map<String, String> headers, Context context) {
    super(new DefaultActionValidationService(headers, context), headers, context);
  }

  public Future<ActionContext> pay(String accountId, ActionRequest request) {
    return performAction(Action.PAY, accountId, request);
  }

  public Future<ActionContext> waive(String accountId, ActionRequest request) {
    return performAction(Action.WAIVE, accountId, request);
  }

  public Future<ActionContext> transfer(String accountId, ActionRequest request) {
    return performAction(Action.TRANSFER, accountId, request);
  }

  @Override
  Future<ActionContext> createFeeFineActions(ActionContext context) {
    final ActionRequest request = context.getRequest();
    final Account account = context.getAccount();
    final Action action = context.getAction();
    final MonetaryValue requestedAmount = context.getRequestedAmount();

    MonetaryValue remainingAmountAfterAction = new MonetaryValue(account.getRemaining())
         .subtract(requestedAmount);

    boolean shouldCloseAccount = remainingAmountAfterAction.isZero();
    String actionType = shouldCloseAccount ? action.getFullResult() : action.getPartialResult();

    Feefineaction feeFineAction = new Feefineaction()
      .withAmountAction(requestedAmount.toDouble())
      .withComments(request.getComments())
      .withNotify(request.getNotifyPatron())
      .withTransactionInformation(request.getTransactionInfo())
      .withCreatedAt(request.getServicePointId())
      .withSource(request.getUserName())
      .withPaymentMethod(request.getPaymentMethod())
      .withAccountId(context.getAccountId())
      .withUserId(account.getUserId())
      .withBalance(remainingAmountAfterAction.toDouble())
      .withTypeAction(actionType)
      .withId(UUID.randomUUID().toString())
      .withDateAction(new Date())
      .withAccountId(context.getAccountId());

    return feeFineActionRepository.save(feeFineAction)
      .map(context.withFeeFineAction(feeFineAction)
        .withShouldCloseAccount(shouldCloseAccount)
      );
  }

}