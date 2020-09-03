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
    super(new RefundActionValidationService(headers, context), headers, context);
  }

  public Future<ActionContext> refund(String accountId, ActionRequest request) {
    return performAction(Action.REFUND, accountId, request);
  }

  @Override
  Future<ActionContext> createFeeFineActions(ActionContext context) {
    return feeFineActionRepository.findRefundableActionsForAccount(context.getAccountId())
      .compose(feeFineActions -> createFeeFineActions(context, feeFineActions));
  }

  private Future<ActionContext> createFeeFineActions(ActionContext context,
    List<Feefineaction> refundableFeeFineActions) {

    final MonetaryValue refundableAmount = new MonetaryValue(
      refundableFeeFineActions.stream()
        .mapToDouble(Feefineaction::getAmountAction)
        .sum());

    final MonetaryValue paidAmount = new MonetaryValue(
      refundableFeeFineActions.stream()
        .filter(ffa -> PAY.isActionForResult(ffa.getTypeAction()))
        .mapToDouble(Feefineaction::getAmountAction)
        .sum());

    final MonetaryValue requestedAmount = context.getRequestedAmount();
    final MonetaryValue refundAmountForPayments = paidAmount.min(requestedAmount);
    final MonetaryValue refundAmountForTransfers = requestedAmount.subtract(refundAmountForPayments);

    boolean shouldRefundPayments = refundAmountForPayments.isPositive();
    boolean shouldRefundTransfers = refundAmountForTransfers.isPositive();
    boolean isFullRefund = refundableAmount.subtract(requestedAmount).isZero();

    Future<MonetaryValue> createActions = succeededFuture(
      new MonetaryValue(context.getAccount().getRemaining()));

    // 1. Credit for payments
    if (shouldRefundPayments) {
      createActions = createFeeFineAction(CREDIT, refundAmountForPayments, isFullRefund,
        REFUND_TO_PATRON, context, createActions);
    }
    // 2. Credit for transfers
    if (shouldRefundTransfers) {
      createActions = createFeeFineAction(CREDIT, refundAmountForTransfers, isFullRefund,
        REFUND_TO_BURSAR, context, createActions);
    }
    // 3. Refund for payments
    if (shouldRefundPayments) {
      createActions = createFeeFineAction(REFUND, refundAmountForPayments, isFullRefund,
        REFUNDED_TO_PATRON, context, createActions);
    }
    // 4. Refund for transfers
    if (shouldRefundTransfers) {
      createActions = createFeeFineAction(REFUND, refundAmountForTransfers, isFullRefund,
        REFUNDED_TO_BURSAR, context, createActions);
    }

    return createActions.map(context);
  }

  private Future<MonetaryValue> createFeeFineAction(Action action, MonetaryValue amount,
    boolean isFullRefund, String transInfo, ActionContext ctx, Future<MonetaryValue> prevStep) {

    return prevStep.map(balance -> action == CREDIT ? balance.subtract(amount) : balance.add(amount))
      .compose(balance -> createFeeFineAction(action, balance, amount, isFullRefund, transInfo, ctx));
  }

  private Future<MonetaryValue> createFeeFineAction(Action action, MonetaryValue balance,
    MonetaryValue amount, boolean isFullRefund, String transactionInfo, ActionContext context) {

    ActionRequest request = context.getRequest();

    Feefineaction feeFineAction = new Feefineaction()
      .withTypeAction(action.getResult(isFullRefund))
      .withAmountAction(amount.toDouble())
      .withBalance(balance.toDouble())
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
      .map(ffa -> action == CREDIT ? context : context.withFeeFineAction(ffa))
      .map(balance);
  }

}