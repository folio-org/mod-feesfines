package org.folio.rest.service.action;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.domain.Action.CREDIT;
import static org.folio.rest.domain.Action.PAY;
import static org.folio.rest.domain.Action.REFUND;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.folio.rest.domain.Action;
import org.folio.rest.domain.MonetaryValue;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.ActionRequest;
import org.folio.rest.jaxrs.model.Feefineaction;
import org.folio.rest.service.action.validation.RefundActionValidationService;

import io.vertx.core.Context;
import io.vertx.core.Future;

public class RefundActionService extends ActionService {
  private static final String REFUND_TO_PATRON = "Refund to patron";
  private static final String REFUND_TO_BURSAR = "Refund to Bursar";
  private static final String REFUNDED_TO_PATRON = "Refunded to patron";
  private static final String REFUNDED_TO_BURSAR = "Refunded to Bursar";

  public RefundActionService(Map<String, String> headers, Context context) {
    super(Action.REFUND, new RefundActionValidationService(headers, context), headers, context);
  }

  @Override
  protected Future<ActionContext> createFeeFineActions(ActionContext context) {
    return feeFineActionRepository.findRefundableActionsForAccount(context.getAccountId())
      .compose(feeFineActions -> createFeeFineActions(context, feeFineActions));
  }

  private Future<ActionContext> createFeeFineActions(ActionContext context,
    List<Feefineaction> refundableFeeFineActions) {

    double refundableAmountDouble = refundableFeeFineActions.stream()
      .mapToDouble(Feefineaction::getAmountAction)
      .sum();

    double paidAmountDouble = refundableFeeFineActions.stream()
      .filter(ffa -> PAY.isActionForResult(ffa.getTypeAction()))
      .mapToDouble(Feefineaction::getAmountAction)
      .sum();

    MonetaryValue refundableAmount = new MonetaryValue(refundableAmountDouble);
    MonetaryValue paidAmount = new MonetaryValue(paidAmountDouble);
    MonetaryValue transferredAmount = refundableAmount.subtract(paidAmount);
    MonetaryValue requestedAmount = context.getRequestedAmount();
    MonetaryValue refundAmountPayment = paidAmount.min(requestedAmount);
    MonetaryValue refundAmountTransfer = requestedAmount.subtract(refundAmountPayment);

    boolean isFullRefundPayment = paidAmount.subtract(refundAmountPayment).isZero();
    boolean isFullRefundTransfer = transferredAmount.subtract(refundAmountTransfer).isZero();

    return succeededFuture(context)
      .compose(ctx -> createFeeFineAction(ctx, CREDIT, refundAmountPayment, isFullRefundPayment,
        REFUND_TO_PATRON))
      .compose(ctx -> createFeeFineAction(ctx, CREDIT, refundAmountTransfer, isFullRefundTransfer,
        REFUND_TO_BURSAR))
      .compose(ctx -> createFeeFineAction(ctx, REFUND, refundAmountPayment, isFullRefundPayment,
        REFUNDED_TO_PATRON))
      .compose(ctx -> createFeeFineAction(ctx, REFUND, refundAmountTransfer, isFullRefundTransfer,
        REFUNDED_TO_BURSAR));
  }

  private Future<ActionContext> createFeeFineAction(ActionContext context, Action action,
     MonetaryValue amount, boolean isFullAction, String transactionInfo) {

    if (!amount.isPositive()) {
      return succeededFuture(context);
    }

    Account account = context.getAccount();
    ActionRequest request = context.getRequest();

    MonetaryValue remainingAmountBefore = new MonetaryValue(account.getRemaining());
    MonetaryValue remainingAmountAfter = action == CREDIT
      ? remainingAmountBefore.subtract(amount)
      : remainingAmountBefore.add(amount);

    account.setRemaining(remainingAmountAfter.toDouble());

    Feefineaction feeFineAction = new Feefineaction()
      .withTypeAction(action.getResult(isFullAction))
      .withAmountAction(amount.toDouble())
      .withBalance(account.getRemaining())
      .withComments(request.getComments())
      .withNotify(request.getNotifyPatron())
      .withTransactionInformation(transactionInfo)
      .withCreatedAt(request.getServicePointId())
      .withSource(request.getUserName())
      .withPaymentMethod(request.getPaymentMethod())
      .withAccountId(context.getAccountId())
      .withUserId(context.getAccount().getUserId())
      .withAccountId(context.getAccountId())
      .withDateAction(new Date())
      .withId(UUID.randomUUID().toString());

    return feeFineActionRepository.save(feeFineAction)
      .map(ffa -> action == CREDIT ? context : context.withFeeFineAction(ffa));
  }

}