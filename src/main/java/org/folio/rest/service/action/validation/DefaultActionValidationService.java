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
  protected void validateAccountStatus(Account account) {
    if (isClosedAndHasZeroRemainingAmount(account)) {
      throw new FailedValidationException("Fee/fine is already closed");
    }
  }

  @Override
  protected Future<Void> validateAmountMaximum(Account account, MonetaryValue requestedAmount) {
    if (requestedAmount.isGreaterThan(new MonetaryValue(account.getRemaining()))) {
      throw new FailedValidationException("Requested amount exceeds remaining amount");
    }

    return succeededFuture();
  }

  @Override
  protected MonetaryValue calculateRemainingBalance(Account account,
    MonetaryValue requestedAmount) {

    return new MonetaryValue(account.getRemaining()).subtract(requestedAmount);
  }

  @Override
  protected Future<Void> validateAmountMaximum(List<Account> accounts,
    MonetaryValue requestedAmount) {

    if (requestedAmount.isGreaterThan(calculateTotalRemaining(accounts))) {
      throw new FailedValidationException("Requested amount exceeds the selected remaining amount");
    }

    return succeededFuture();
  }

  @Override
  protected MonetaryValue calculateRemainingBalance(List<Account> accounts,
    MonetaryValue requestedAmount) {

    return calculateTotalRemaining(accounts).subtract(requestedAmount);
  }

  private MonetaryValue calculateTotalRemaining(List<Account> accounts) {
    return new MonetaryValue(accounts.stream()
      .map(Account::getRemaining)
      .reduce(Double::sum)
      .orElse(0.0));
  }
}
