package org.folio.rest.service.action.validation;

import java.util.List;
import java.util.Map;

import org.folio.rest.domain.MonetaryValue;
import org.folio.rest.exception.AccountNotFoundValidationException;
import org.folio.rest.exception.FailedValidationException;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.repository.AccountRepository;
import org.folio.rest.tools.utils.TenantTool;

import io.vertx.core.Context;
import io.vertx.core.Future;

public abstract class ActionValidationService {
  private final AccountRepository accountRepository;

  public ActionValidationService(AccountRepository accountRepository) {
    this.accountRepository = accountRepository;
  }

  public ActionValidationService(Map<String, String> headers, Context context) {
    PostgresClient postgresClient = PostgresClient.getInstance(context.owner(),
      TenantTool.tenantId(headers));

    this.accountRepository = new AccountRepository(postgresClient);
  }

  public Future<ActionValidationResult> validateById(String accountId, String rawAmount) {
    return accountRepository.getAccountById(accountId)
      .compose(account -> validate(account, rawAmount));
  }

  public Future<ActionValidationResult> validateByIds(List<String> accountIds, String rawAmount) {
    return accountRepository.getAccountsById(accountIds)
      .compose(account -> validate(account, rawAmount));
  }

  public Future<ActionValidationResult> validate(Account account, String rawAmount) {
    validateIfAccountExists(account);

    MonetaryValue requestedAmount = validateRawAmount(rawAmount);
    validateAccountStatus(account);

    return validateAmountMaximum(account, requestedAmount)
      .map(new ActionValidationResult(
        calculateRemainingBalance(account, requestedAmount).toString(), requestedAmount.toString()));
  }

  public Future<ActionValidationResult> validate(List<Account> accounts, String rawAmount) {
    validateIfAccountsExist(accounts);
    MonetaryValue requestedAmount = validateRawAmount(rawAmount);
    accounts.forEach(this::validateAccountStatus);

    return validateAmountMaximum(accounts, requestedAmount)
      .map(new ActionValidationResult(
        calculateRemainingBalance(accounts, requestedAmount).toString(),
        requestedAmount.toString()));
  }

  private MonetaryValue validateRawAmount(String rawAmount) {
    MonetaryValue requestedAmount;

    try {
      requestedAmount = new MonetaryValue(rawAmount);
    } catch (NumberFormatException e) {
      throw new FailedValidationException("Invalid amount entered");
    }

    if (!requestedAmount.isPositive()) {
      throw new FailedValidationException("Amount must be positive");
    }

    return requestedAmount;
  }

  protected void validateIfAccountExists(Account account) {
    if (account == null) {
      throw new AccountNotFoundValidationException("Fee/fine was not found");
    }
  }

  protected void validateIfAccountsExist(List<Account> accounts) {
    accounts.forEach(this::validateIfAccountExists);
  }

  protected abstract void validateAccountStatus(Account account);

  protected abstract Future<Void> validateAmountMaximum(Account account,
    MonetaryValue requestedAmount);

  protected abstract MonetaryValue calculateRemainingBalance(Account account,
    MonetaryValue requestedAmount);

  protected abstract Future<Void> validateAmountMaximum(List<Account> accounts,
    MonetaryValue requestedAmount);

  protected abstract MonetaryValue calculateRemainingBalance(List<Account> accounts,
    MonetaryValue requestedAmount);
}
