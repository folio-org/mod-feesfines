package org.folio.rest.service.action;

import static io.vertx.core.Future.succeededFuture;
import static java.util.stream.Collectors.toList;
import static org.folio.rest.domain.Action.CREDIT;
import static org.folio.rest.domain.Action.PAY;
import static org.folio.rest.domain.Action.REFUND;
import static org.folio.rest.domain.FeeFineStatus.OPEN;
import static org.folio.rest.jaxrs.model.PaymentStatus.Name.fromValue;
import static org.folio.rest.utils.FeeFineActionHelper.getTotalAmount;
import static org.folio.rest.utils.FeeFineActionHelper.getTotalAmounts;
import static org.folio.rest.utils.FeeFineActionHelper.groupFeeFineActionsByAccountId;
import static org.folio.rest.utils.FeeFineActionHelper.groupTransferredAmountsByTransferAccount;

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

import io.vertx.core.Context;
import io.vertx.core.Future;

public class RefundActionService extends ActionService {
  private static final String PAYMENT_REFUND_RECIPIENT = "patron";

  public RefundActionService(Map<String, String> headers, Context context) {
    super(Action.REFUND, new RefundActionValidationService(headers, context), headers, context);
  }

  @Override
  protected Future<ActionContext> createFeeFineActions(ActionContext context) {
    return feeFineActionRepository.findRefundableActionsForAccounts(context.getAccounts().keySet())
      .compose(feeFineActions -> processRefund(context, feeFineActions));
  }

  private Future<ActionContext> processRefund(ActionContext context,
    List<Feefineaction> refundableFeeFineActions) {

    Map<String, List<Feefineaction>> refundableActionsByAccountId =
      groupFeeFineActionsByAccountId(refundableFeeFineActions);

    Map<String, MonetaryValue> refundableAmountsByAccountId =
      getTotalAmounts(refundableActionsByAccountId);

    Map<String, MonetaryValue> refundAmountsByAccountId =
      distributeRefundAmount(context.getRequestedAmount(), refundableAmountsByAccountId);

    return Future.all(
        context.getAccounts()
          .values()
          .stream()
          .map(account -> createFeeFineActionsForAccount(context, account,
            refundableAmountsByAccountId.get(account.getId()),
            refundAmountsByAccountId.get(account.getId()),
            refundableActionsByAccountId.get(account.getId())))
          .collect(toList()))
      .map(context);
  }

  private Future<ActionContext> createFeeFineActionsForAccount(ActionContext context,
    Account account, MonetaryValue refundableAmount, MonetaryValue refundAmount,
    List<Feefineaction> refundableFeeFineActions) {

    MonetaryValue paidAmount = getTotalAmount(refundableFeeFineActions, PAY);
    MonetaryValue transferredAmount = refundableAmount.subtract(paidAmount);
    MonetaryValue paymentsRefundAmount = paidAmount.min(refundAmount);
    MonetaryValue transfersRefundAmount = refundAmount.subtract(paymentsRefundAmount);

    boolean isFullPaymentsRefund = paidAmount.subtract(paymentsRefundAmount).isZero();
    boolean isFullTransfersRefund = transferredAmount.subtract(transfersRefundAmount).isZero();

    Map<String, MonetaryValue> transferRefundAmounts = distributeRefundAmount(
      transfersRefundAmount, groupTransferredAmountsByTransferAccount(refundableFeeFineActions));

    return succeededFuture(context)
      .compose(ctx -> refundPayments(ctx, account, CREDIT, isFullPaymentsRefund, paymentsRefundAmount))
      .compose(ctx -> refundTransfers(ctx, account, CREDIT, isFullTransfersRefund, transferRefundAmounts))
      .compose(ctx -> refundPayments(ctx, account, REFUND, isFullPaymentsRefund, paymentsRefundAmount))
      .compose(ctx -> refundTransfers(ctx, account, REFUND, isFullTransfersRefund, transferRefundAmounts));
  }

  private Future<ActionContext> refundPayments(ActionContext ctx, Account account, Action action,
    boolean isFullRefund, MonetaryValue refundAmount) {

    return createFeeFineActionAndUpdateAccount(ctx, account, action, isFullRefund, refundAmount,
      PAYMENT_REFUND_RECIPIENT);
  }

  private Future<ActionContext> refundTransfers(ActionContext ctx, Account account, Action action,
    boolean isFullRefund, Map<String, MonetaryValue> refundAmountByTransferAccount) {

    return Future.all(
        refundAmountByTransferAccount.keySet().stream()
          .map(transferAccount -> refundTransfer(ctx, account, action,
            refundAmountByTransferAccount.get(transferAccount), isFullRefund, transferAccount))
          .collect(toList()))
      .map(ctx);
  }

  private Future<ActionContext> refundTransfer(ActionContext ctx, Account account, Action action,
    MonetaryValue refundAmount, boolean isFullRefund, String transferAccount) {

    return createFeeFineActionAndUpdateAccount(ctx, account, action, isFullRefund, refundAmount,
      transferAccount);
  }

  private Future<ActionContext> createFeeFineActionAndUpdateAccount(ActionContext context,
    Account account, Action action, boolean isFullAction, MonetaryValue amount,
    String refundRecipient) {

    if (!amount.isPositive()) {
      return succeededFuture(context);
    }

    Feefineaction feeFineAction = buildFeeFineAction(account, action, amount, isFullAction,
      refundRecipient, context);

    updateAccountInMemory(account, feeFineAction);

    return feeFineActionRepository.save(feeFineAction)
      .map(context::withFeeFineAction);
  }

  private Feefineaction buildFeeFineAction(Account account, Action action, MonetaryValue amount,
    boolean isFullAction, String refundRecipient, ActionContext context) {

    String actionType = action.getResult(isFullAction);
    MonetaryValue balance = calculateFeeFineActionBalance(account, action, amount);
    String transactionInfo = buildRefundTransactionInfo(action, refundRecipient);
    ActionRequest request = context.getRequest();

    return new Feefineaction()
      .withTypeAction(actionType)
      .withAmountAction(amount)
      .withBalance(balance)
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
  }

  private Map<String, MonetaryValue> distributeRefundAmount(MonetaryValue refundAmount,
    Map<String, MonetaryValue> refundableAmountsByAccountId) {

    return amountSplitterStrategy.split(refundAmount, refundableAmountsByAccountId);
  }

  private static void updateAccountInMemory(Account account, Feefineaction feeFineAction) {
    account.setRemaining(feeFineAction.getBalance());
    account.getPaymentStatus().setName(fromValue(feeFineAction.getTypeAction()));
    account.getStatus().setName(OPEN.getValue());
  }

  private static String buildRefundTransactionInfo(Action action, String targetAccount) {
    switch (action) {
    case CREDIT:
      return "Refund to " + targetAccount;
    case REFUND:
      return "Refunded to " + targetAccount;
    default:
      throw new IllegalArgumentException("Cannot build transaction info for action: " + action);
    }
  }

  private static MonetaryValue calculateFeeFineActionBalance(Account account,
    Action action, MonetaryValue refundAmount) {

    MonetaryValue remainingAmount = account.getRemaining();

    return action == REFUND
      ? remainingAmount.add(refundAmount)
      : remainingAmount;
  }
}
