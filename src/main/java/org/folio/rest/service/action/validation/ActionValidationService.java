package org.folio.rest.service.action.validation;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.utils.AccountHelper.isClosed;

import org.folio.rest.domain.MonetaryValue;
import org.folio.rest.exception.AccountNotFoundValidationException;
import org.folio.rest.exception.FailedValidationException;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.repository.AccountRepository;

import io.vertx.core.Future;

public abstract class ActionValidationService {
  private final AccountRepository accountRepository;

  public ActionValidationService(AccountRepository accountRepository) {
    this.accountRepository = accountRepository;
  }

  public Future<ActionValidationResult> validate(String accountId, String rawAmount) {
    return accountRepository.getAccountById(accountId)
      .compose(account -> validate(account, rawAmount));
  }

  public Future<ActionValidationResult> validate(Account account, String rawAmount) {
    if (account == null) {
      throw new AccountNotFoundValidationException("Fee/fine was not found");
    }

    MonetaryValue requestedAmount;
    try {
      requestedAmount = new MonetaryValue(rawAmount);
    } catch (NumberFormatException e) {
      throw new FailedValidationException("Invalid amount entered");
    }

    if (!requestedAmount.isPositive()) {
      throw new FailedValidationException("Amount must be positive");
    }

    final MonetaryValue remainingAmount = new MonetaryValue(account.getRemaining());

    if (isClosed(account) && remainingAmount.isZero()) {
      throw new FailedValidationException("Fee/fine is already closed");
    }

    validateAmountMaximum(account, requestedAmount);

    return succeededFuture(new ActionValidationResult(
      calculateRemainingBalance(requestedAmount, remainingAmount).toString(),
      requestedAmount.toString())
    );
  }

  protected abstract void validateAmountMaximum(Account account, MonetaryValue requestedAmount);

  protected abstract MonetaryValue calculateRemainingBalance(MonetaryValue requestedAmount,
    MonetaryValue remainingAmount);

}
