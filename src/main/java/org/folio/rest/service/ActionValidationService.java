package org.folio.rest.service;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.utils.AccountHelper.isClosedAndHasZeroRemainingAmount;
import static org.folio.rest.utils.MonetaryHelper.isNegative;
import static org.folio.rest.utils.MonetaryHelper.isNotPositive;
import static org.folio.rest.utils.MonetaryHelper.monetize;

import java.math.BigDecimal;

import org.folio.rest.exception.AccountNotFoundValidationException;
import org.folio.rest.exception.FailedValidationException;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.repository.AccountRepository;

import io.vertx.core.Future;

public class ActionValidationService {

  private final AccountRepository accountRepository;

  public ActionValidationService(AccountRepository accountRepository) {
    this.accountRepository = accountRepository;
  }

  Future<ValidationResult> validate(Account account, String rawAmount) {
    if (account == null) {
      throw new AccountNotFoundValidationException("Account was not found");
    }

    BigDecimal requestedAmount;
    try {
      requestedAmount = monetize(rawAmount);
    } catch (NumberFormatException e) {
      throw new FailedValidationException("Invalid amount entered");
    }

    if (isClosedAndHasZeroRemainingAmount(account)) {
      throw new FailedValidationException("Account is already closed");
    }

    final BigDecimal remainingAmount = monetize(account.getRemaining())
      .subtract(requestedAmount);

    if (isNegative(remainingAmount)) {
      throw new FailedValidationException("Requested amount exceeds remaining amount");
    } else if (isNotPositive(requestedAmount)) {
      throw new FailedValidationException("Amount must be positive");
    } else {
      return succeededFuture(new ValidationResult(
        remainingAmount.toString(), requestedAmount.toString()));
    }
  }

  public Future<ValidationResult> validate(String accountId, String rawAmount) {
    return accountRepository.getAccountById(accountId)
      .compose(account -> validate(account, rawAmount));
  }

  public static class ValidationResult {
    private final String remainingAmount;
    private final String scaledAmount;

    public ValidationResult(String remainingAmount, String scaledAmount) {
      this.remainingAmount = remainingAmount;
      this.scaledAmount = scaledAmount;
    }

    public String getRemainingAmount() {
      return remainingAmount;
    }

    public String getScaledAmount() {
      return scaledAmount;
    }
  }
}
