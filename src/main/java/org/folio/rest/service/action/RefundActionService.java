package org.folio.rest.service.action;

import static io.vertx.core.Future.succeededFuture;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.summingDouble;
import static java.util.stream.Collectors.toList;
import static org.folio.rest.domain.Action.CREDIT;
import static org.folio.rest.domain.Action.PAY;
import static org.folio.rest.domain.Action.REFUND;
import static org.folio.rest.domain.Action.TRANSFER;

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
  private static final String PATRON = "patron";

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
    MonetaryValue paymentRefundAmount = paidAmount.min(refundAmount);
    MonetaryValue transfersRefundAmount = refundAmount.subtract(paymentRefundAmount);

    boolean isFullPaymentRefund = paidAmount.subtract(paymentRefundAmount).isZero();
    boolean isFullTransferRefund = transferredAmount.subtract(transfersRefundAmount).isZero();

    Map<String, MonetaryValue> transferAccountToRefundAmount =
      getTransferRefundAmountsGroupedByTransferAccount(refundableFeeFineActions);

    return succeededFuture(context)
      .compose(ctx -> createFeeFineActionForPayment(ctx, account, CREDIT,
        isFullPaymentRefund, paymentRefundAmount))
      .compose(ctx -> createFeeFineActionsForTransfers(ctx, account, CREDIT,
        isFullTransferRefund, transferAccountToRefundAmount))
      .compose(ctx -> createFeeFineActionForPayment(ctx, account, REFUND,
        isFullPaymentRefund, paymentRefundAmount))
      .compose(ctx -> createFeeFineActionsForTransfers(ctx, account, REFUND,
        isFullTransferRefund, transferAccountToRefundAmount));
  }

  private Future<ActionContext> createFeeFineActionForPayment(ActionContext ctx,
    Account account, Action action, boolean isFullRefund, MonetaryValue refundAmount) {

    return createFeeFineAction(ctx, account, action, refundAmount, isFullRefund, PATRON);
  }

  private Future<ActionContext> createFeeFineActionsForTransfers(ActionContext ctx,
    Account account, Action action, boolean isFullRefund,
    Map<String, MonetaryValue> transferAccountToRefundAmount) {

    return CompositeFuture.all(
      transferAccountToRefundAmount.keySet().stream()
        .map(transferAccount -> createFeeFineActionForTransferRefund(ctx, account, action,
          transferAccountToRefundAmount.get(transferAccount), isFullRefund, transferAccount))
        .collect(toList()))
      .map(ctx);
  }

  private Future<ActionContext> createFeeFineActionForTransferRefund(ActionContext ctx,
    Account account, Action action, MonetaryValue refundAmount,
    boolean isFullRefund, String transferAccount) {

    return createFeeFineAction(ctx, account, action, refundAmount, isFullRefund, transferAccount);
  }

  private static Map<String, MonetaryValue> getTransferRefundAmountsGroupedByTransferAccount(
    List<Feefineaction> refundableFeeFineActions) {

    return refundableFeeFineActions.stream()
      .filter(ffa -> TRANSFER.isActionForResult(ffa.getTypeAction()))
      .collect(groupingBy(
        Feefineaction::getPaymentMethod,
        collectingAndThen(
          summingDouble(Feefineaction::getAmountAction),
          MonetaryValue::new
        )));
  }

  private Future<ActionContext> createFeeFineAction(ActionContext context, Account account,
    Action action, MonetaryValue amount, boolean isFullAction, String recipient) {

    if (!amount.isPositive()) {
      return succeededFuture(context);
    }

    ActionRequest request = context.getRequest();

    MonetaryValue remainingAmountBefore = new MonetaryValue(account.getRemaining());
    MonetaryValue remainingAmountAfter = action == CREDIT
      ? remainingAmountBefore.subtract(amount)
      : remainingAmountBefore.add(amount);

    String actionType = action.getResult(isFullAction);
    String transactionInfo = buildTransactionInfo(action, recipient);

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

  private static String buildTransactionInfo(Action action, String targetAccount) {
    switch (action) {
      case CREDIT:
        return "Refund to " + targetAccount;
      case REFUND:
        return "Refunded to " + targetAccount;
      default:
        throw new IllegalArgumentException("Cannot build transaction info for action: " + action);
    }
  }
}
