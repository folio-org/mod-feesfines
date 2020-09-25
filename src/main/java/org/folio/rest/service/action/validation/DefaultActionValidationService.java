package org.folio.rest.service.action.validation;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.utils.AccountHelper.isClosedAndHasZeroRemainingAmount;

import java.util.List;
import java.util.Map;

import org.folio.rest.domain.MonetaryValue;
import org.folio.rest.exception.FailedValidationException;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.repository.AccountRepository;

import io.vertx.core.Context;
import io.vertx.core.Future;

public class DefaultActionValidationService extends ActionValidationService {

  public DefaultActionValidationService(AccountRepository accountRepository) {
    super(accountRepository);
  }

  public DefaultActionValidationService(Map<String, String> headers, Context context) {
    super(headers, context);
  }

  @Override
  protected void validateAccountStatuses(List<Account> accounts) {
    accounts.forEach(this::validateAccountStatus);
  }

  private void validateAccountStatus(Account account) {
    if (isClosedAndHasZeroRemainingAmount(account)) {
      throw new FailedValidationException("Fee/fine is already closed");
    }
  }

  @Override
  protected Future<Void> validateAmountMaximum(List<Account> accounts,
    MonetaryValue requestedAmount) {

    if (requestedAmount.isGreaterThan(calculateTotalRemaining(accounts))) {
      throw new FailedValidationException("Requested amount exceeds remaining amount");
    }

    return succeededFuture();
  }

  @Override
  protected Future<MonetaryValue> calculateRemainingBalance(List<Account> accounts,
    MonetaryValue requestedAmount) {

    return succeededFuture(calculateTotalRemaining(accounts).subtract(requestedAmount));
  }
}
