package org.folio.rest.service;

import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.repository.AccountRepository;
import org.folio.rest.validation.ActionValidationFailure;
import org.folio.rest.validation.ActionValidationSuccess;
import org.folio.rest.validation.ValidationResult;

import io.vertx.core.Future;

public class AccountValidationService {

  private final AccountRepository accountRepository;

  public AccountValidationService(AccountRepository accountRepository) {
    this.accountRepository = accountRepository;
  }

  public ValidationResult validate(Account account, double amount) {
    if (amount > account.getRemaining()) {
      return new ActionValidationFailure("Requested amount exceeds remaining amount");
    } else if (amount <= 0) {
      return new ActionValidationFailure("Amount must be positive");
    } else {
      return new ActionValidationSuccess(account.getRemaining());
    }
  }

  public Future<ValidationResult> validate(String accountId, double amount) {

    return accountRepository.getAccountById(accountId)
      .map(account -> validate(account, amount));
  }
}
