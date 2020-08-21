package org.folio.rest.service;

import static io.vertx.core.Future.succeededFuture;

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

  public Future<ValidationResult> validate(Account account, String rawAmount) {

    if (account == null) {
      throw new AccountNotFoundValidationException("Account was not found");
    }
    final double amount;
    try {
      amount = Double.parseDouble(rawAmount);
    } catch (NumberFormatException e) {
      throw new FailedValidationException("Invalid amount entered");
    }
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
    private final Double remainingAmount;

    public ValidationResult(Double remainingAmount) {
      this.remainingAmount = remainingAmount;
    }

    public Double getRemainingAmount() {
      return remainingAmount;
    }
  }
}
