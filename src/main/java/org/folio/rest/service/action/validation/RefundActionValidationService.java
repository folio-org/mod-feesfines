package org.folio.rest.service.action.validation;

import static org.folio.rest.domain.Action.PAY;
import static org.folio.rest.domain.Action.REFUND;
import static org.folio.rest.domain.Action.TRANSFER;
import static org.folio.rest.utils.FeeFineActionHelper.getTotalAmount;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.folio.rest.domain.MonetaryValue;
import org.folio.rest.exception.FailedValidationException;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.Feefineaction;
import org.folio.rest.repository.FeeFineActionRepository;

import io.vertx.core.Context;
import io.vertx.core.Future;

public class RefundActionValidationService extends ActionValidationService {
  private final FeeFineActionRepository feeFineActionRepository;

  public RefundActionValidationService(Map<String, String> headers, Context context) {
    super(headers, context);
    this.feeFineActionRepository = new FeeFineActionRepository(headers, context);
  }

  @Override
  protected void validateAccountStatuses(List<Account> account) {
    // doing nothing as closed fee/fine can also be refunded
  }

  private Future<MonetaryValue> getRefundableAmountForAccounts(List<Account> accounts) {
    List<String> accountIds = accounts.stream()
      .map(Account::getId)
      .collect(Collectors.toList());

    return feeFineActionRepository.findActionsForAccounts(accountIds, List.of(PAY, TRANSFER, REFUND))
      .map(this::getRefundableAmountForFeeFineActions);
  }

  private MonetaryValue getRefundableAmountForFeeFineActions(List<Feefineaction> feeFineActions) {
    return getTotalAmount(feeFineActions, List.of(PAY, TRANSFER))
      .subtract(getTotalAmount(feeFineActions, REFUND));
  }

  @Override
  protected Future<Void> validateAmountMaximum(List<Account> accounts,
    MonetaryValue requestedAmount) {

    return getRefundableAmountForAccounts(accounts)
      .map(refundableAmount -> {
        if (requestedAmount.isGreaterThan(refundableAmount)) {
          throw new FailedValidationException(
            "Refund amount must be greater than zero and less than or equal to Selected amount");
        }
        return null;
      });
  }

  @Override
  protected Future<MonetaryValue> calculateRemainingBalance(List<Account> accounts,
    MonetaryValue requestedAmount) {

    return getRefundableAmountForAccounts(accounts)
      .map(refundableAmount -> refundableAmount.subtract(requestedAmount));
  }

}
