package org.folio.rest.service;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.utils.AccountHelper.isClosedAndHasZeroRemainingAmount;

import org.folio.rest.domain.MonetaryValue;
import org.folio.rest.exception.AccountNotFoundValidationException;
import org.folio.rest.exception.FailedValidationException;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.repository.AccountRepository;

import io.vertx.core.Future;

public class DefaultActionValidationService {

  private final AccountRepository accountRepository;

  public DefaultActionValidationService(AccountRepository accountRepository) {
    this.accountRepository = accountRepository;
  }

  Future<ValidationResult> validate(Account account, String rawAmount) {
    if (account == null) {
      throw new AccountNotFoundValidationException("Account was not found");
    }

    MonetaryValue requestedAmount;
    try {
      requestedAmount = new MonetaryValue(rawAmount);
    } catch (NumberFormatException e) {
      throw new FailedValidationException("Invalid amount entered");
    }

    if (isClosedAndHasZeroRemainingAmount(account)) {
      throw new FailedValidationException("Account is already closed");
    }

    final MonetaryValue remainingAmount = new MonetaryValue(account.getRemaining())
      .subtract(requestedAmount);

//    if (remainingAmount.isNegative()) {
//      throw new FailedValidationException("Requested amount exceeds remaining amount");
//    } else if (!requestedAmount.isPositive()) {
//      throw new FailedValidationException("Amount must be positive");
//    } else {
//      return succeededFuture(new ValidationResult(
//        remainingAmount.toString(), requestedAmount.toString()));
//    }

    validateAmountMaximum(account, amount);
    validateIfAmountIsPositive(amount);
    return succeededFuture(new ValidationResult(calculateRemainingBalance(account, amount)));
  }

  protected void validateAmountMaximum(Account account, double requestedAmount) {
    if (requestedAmount > account.getRemaining()) {
      throw new FailedValidationException("Requested amount exceeds remaining amount");
    }
  }

  private void validateIfAmountIsPositive(double requestedAmount) {
    if (requestedAmount <= 0) {
      throw new FailedValidationException("Amount must be positive");
    }
  }

  protected double calculateRemainingBalance(Account account, double requestedAmount) {
    return account.getRemaining() - requestedAmount;
  }

  public Future<ValidationResult> validate(String accountId, String rawAmount) {
    return accountRepository.getAccountById(accountId)
      .compose(account -> validate(account, rawAmount));
  }

  public static class ValidationResult {
    private final String remainingAmount;
    private final String formattedAmount;

    public ValidationResult(String remainingAmount, String formattedAmount) {
      this.remainingAmount = remainingAmount;
      this.formattedAmount = formattedAmount;
    }

    public String getRemainingAmount() {
      return remainingAmount;
    }

    public String getFormattedAmount() {
      return formattedAmount;
    }
  }
}
