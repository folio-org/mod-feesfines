package org.folio.rest.service.action.validation;

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

  private Future<MonetaryValue> getRefundableAmount(List<Account> accounts) {
    List<String> accountIds = accounts.stream()
      .map(Account::getId)
      .collect(Collectors.toList());

    // Sum of the refundable amounts of all accounts
    return feeFineActionRepository.findRefundableActionsForAccounts(accountIds)
      .map(actions -> new MonetaryValue(
        actions.stream()
          .mapToDouble(Feefineaction::getAmountAction)
          .sum()
      ));
  }

  @Override
  protected Future<Void> validateAmountMaximum(List<Account> accounts,
    MonetaryValue requestedAmount) {

    return getRefundableAmount(accounts)
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

    return getRefundableAmount(accounts)
      .map(refundableAmount -> refundableAmount.subtract(requestedAmount));
  }

}
