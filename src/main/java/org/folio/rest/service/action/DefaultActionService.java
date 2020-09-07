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

public abstract class DefaultActionService extends ActionService {

  public DefaultActionService(Action action, Map<String, String> headers, Context context) {
    super(action, new DefaultActionValidationService(headers, context), headers, context);
  }

  @Override
  protected Future<ActionContext> createFeeFineActions(ActionContext context) {
    final ActionRequest request = context.getRequest();
    final Account account = context.getAccount();
    final MonetaryValue requestedAmount = context.getRequestedAmount();

    MonetaryValue remainingAmountAfterAction = new MonetaryValue(account.getRemaining())
         .subtract(requestedAmount);

    boolean isFullAction = remainingAmountAfterAction.isZero();
    String actionType = isFullAction ? action.getFullResult() : action.getPartialResult();

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
      .map(context
        .withFeeFineAction(feeFineAction)
        .withIsFullAction(isFullAction)
        .withShouldCloseAccount(isFullAction)
      );
  }

}