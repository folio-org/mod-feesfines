package org.folio.rest.service.action;

import static io.vertx.core.Future.succeededFuture;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.summingDouble;
import static java.util.stream.Collectors.toList;
import static org.folio.rest.domain.Action.CREDIT;
import static org.folio.rest.domain.Action.PAY;
import static org.folio.rest.domain.Action.REFUND;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.folio.rest.domain.Action;
import org.folio.rest.domain.ActionRequest;
import org.folio.rest.domain.MonetaryValue;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.Feefineaction;
import org.folio.rest.service.action.context.ActionContext;
import org.folio.rest.service.action.validation.RefundActionValidationService;

import io.vertx.core.CompositeFuture;
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
    return feeFineActionRepository.findRefundableActionsForAccounts(context.getAccounts().keySet())
      .compose(feeFineActions -> createFeeFineActions(context, feeFineActions));
  }

  private Future<ActionContext> createFeeFineActions(ActionContext context,
    List<Feefineaction> refundableFeeFineActions) {

    Collection<Account> accounts = context.getAccounts().values();

    Map<String, MonetaryValue> refundableAmounts = refundableFeeFineActions.stream()
      .collect(groupingBy(
        Feefineaction::getAccountId,
        collectingAndThen(
          summingDouble(Feefineaction::getAmountAction),
          MonetaryValue::new
        )));

    Map<String, MonetaryValue> distributedAmounts =
      amountSplitterStrategy.split(context.getRequestedAmount(), accounts, refundableAmounts);

    Map<String, List<Feefineaction>> refundableActionsByAccount = refundableFeeFineActions.stream()
      .collect(groupingBy(Feefineaction::getAccountId));

    return CompositeFuture.all(
      accounts.stream()
        .map(account -> createFeeFineActionsForAccount(context, account,
          distributedAmounts.get(account.getId()), refundableActionsByAccount.get(account.getId())))
        .collect(toList()))
      .map(context);
  }

  private Future<ActionContext> createFeeFineActionsForAccount(ActionContext context,
    Account account, MonetaryValue refundAmount, List<Feefineaction> refundableFeeFineActions) {

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
    MonetaryValue refundAmountPayment = paidAmount.min(refundAmount);
    MonetaryValue refundAmountTransfer = refundAmount.subtract(refundAmountPayment);

    boolean isFullRefundPayment = paidAmount.subtract(refundAmountPayment).isZero();
    boolean isFullRefundTransfer = transferredAmount.subtract(refundAmountTransfer).isZero();

    return succeededFuture(context)
      .compose(ctx -> createFeeFineAction(ctx, account, CREDIT, refundAmountPayment,
        isFullRefundPayment, REFUND_TO_PATRON))
      .compose(ctx -> createFeeFineAction(ctx, account, CREDIT, refundAmountTransfer,
        isFullRefundTransfer, REFUND_TO_BURSAR))
      .compose(ctx -> createFeeFineAction(ctx, account, REFUND, refundAmountPayment,
        isFullRefundPayment, REFUNDED_TO_PATRON))
      .compose(ctx -> createFeeFineAction(ctx, account, REFUND, refundAmountTransfer,
        isFullRefundTransfer, REFUNDED_TO_BURSAR));
  }

  private Future<ActionContext> createFeeFineAction(ActionContext context, Account account,
    Action action, MonetaryValue amount, boolean isFullAction, String transactionInfo) {

    if (!amount.isPositive()) {
      return succeededFuture(context);
    }

    ActionRequest request = context.getRequest();

    MonetaryValue remainingAmountBefore = new MonetaryValue(account.getRemaining());
    MonetaryValue remainingAmountAfter = action == CREDIT
      ? remainingAmountBefore.subtract(amount)
      : remainingAmountBefore.add(amount);

    String actionType = action.getResult(isFullAction);

    account.setRemaining(remainingAmountAfter.toDouble());
    account.getPaymentStatus().setName(actionType);

    Feefineaction feeFineAction = new Feefineaction()
      .withTypeAction(actionType)
      .withAmountAction(amount.toDouble())
      .withBalance(account.getRemaining())
      .withComments(request.getComments())
      .withNotify(request.getNotifyPatron())
      .withTransactionInformation(transactionInfo)
      .withCreatedAt(request.getServicePointId())
      .withSource(request.getUserName())
      .withPaymentMethod(request.getPaymentMethod())
      .withUserId(account.getUserId())
      .withAccountId(account.getId())
      .withDateAction(new Date())
      .withId(UUID.randomUUID().toString());

    return feeFineActionRepository.save(feeFineAction)
      .map(context::withFeeFineAction);
  }
}
