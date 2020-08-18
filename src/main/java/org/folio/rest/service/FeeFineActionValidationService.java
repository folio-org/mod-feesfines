package org.folio.rest.service;

import static io.vertx.core.Future.succeededFuture;

import org.folio.rest.exception.AccountNotFoundValidationException;
import org.folio.rest.exception.FailedValidationException;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.repository.AccountRepository;

import io.vertx.core.Future;

public class FeeFineActionValidationService {

  private final AccountRepository accountRepository;

  public FeeFineActionValidationService(AccountRepository accountRepository) {
    this.accountRepository = accountRepository;
  }

  public Future<ValidationResult> validate(Account account, String rawAmount) {
    if (account == null) {
      throw new AccountNotFoundValidationException("Account was not found");
    }
    final Double remainingAmount = account.getRemaining();
    final double amount;
    try {
      amount = Double.parseDouble(rawAmount);
    } catch (NumberFormatException e) {
      throw new FailedValidationException("Invalid amount entered");
    }
    if (amount > remainingAmount) {
      throw new FailedValidationException("Requested amount exceeds remaining amount");
    } else if (amount <= 0) {
      throw new FailedValidationException("Amount must be positive");
    } else {
      return succeededFuture(new ValidationResult(remainingAmount - amount));
    }
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
