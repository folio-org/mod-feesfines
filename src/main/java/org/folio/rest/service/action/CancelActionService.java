package org.folio.rest.service.action;

import static org.folio.rest.domain.FeeFineStatus.CLOSED;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import org.folio.rest.domain.Action;
import org.folio.rest.domain.ActionRequest;
import org.folio.rest.domain.MonetaryValue;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.Feefineaction;
import org.folio.rest.service.action.context.ActionContext;
import org.folio.rest.service.action.validation.CancelActionValidationService;
import org.folio.rest.utils.amountsplitter.EchoActionableAmounts;

import io.vertx.core.Context;
import io.vertx.core.Future;

public class CancelActionService extends ActionService {

  public CancelActionService(Map<String, String> headers, Context context) {
    super(Action.CANCEL, new CancelActionValidationService(headers, context),
      new EchoActionableAmounts(), headers, context);
  }

  @Override
  protected Future<ActionContext> validateAction(ActionContext context) {
    return validationService.validate(context.getAccounts(), null)
      .map(result -> context.withRequestedAmount(null));
  }

  @Override
  protected Feefineaction createFeeFineActionAndUpdateAccount(Account account, MonetaryValue amount,
    ActionRequest request) {

    final MonetaryValue remainingAmountAfterAction = new MonetaryValue(0.0);
    final String reasonForAction = request.getReasonForAction() != null
      ? request.getReasonForAction() : action.getFullResult();

    final Feefineaction feeFineAction = new Feefineaction()
      .withAmountAction(account.getAmount())
      .withComments(request.getComments())
      .withCreatedAt(request.getServicePointId())
      .withSource(request.getUserName())
      .withAccountId(account.getId())
      .withUserId(account.getUserId())
      .withBalance(remainingAmountAfterAction)
      .withTypeAction(reasonForAction)
      .withId(UUID.randomUUID().toString())
      .withDateAction(new Date());

    account.getPaymentStatus().setName(reasonForAction);
    account.getStatus().setName(CLOSED.getValue());
    account.setRemaining(MonetaryValue.MONETARY_VALUE_ZERO);

    return feeFineAction;
  }

}
