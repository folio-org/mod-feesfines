package org.folio.rest.service.action.validation;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;

import java.math.BigDecimal;
import java.util.ArrayList;
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
    return validateByIds(singletonList(accountId), rawAmount);
  }

  public Future<ActionValidationResult> validateByIds(List<String> accountIds, String rawAmount) {
    return accountRepository.getAccountsById(accountIds)
      .compose(accountsMap -> validate(accountsMap, rawAmount));
  }

  public Future<ActionValidationResult> validate(Account account, String rawAmount) {
    validateIfAccountExists(account);
    return validate(singletonMap(account.getId(), account), rawAmount);
  }

  public Future<ActionValidationResult> validate(Map<String, Account> accountsMap,
    String rawAmount) {

    validateIfAccountsExist(accountsMap);
    MonetaryValue requestedAmount = validateRawAmount(rawAmount);

    List<Account> accounts = new ArrayList<>(accountsMap.values());
    validateAccountStatuses(accounts);

    return validateAmountMaximum(accounts, requestedAmount)
      .compose(v -> calculateRemainingBalance(accounts, requestedAmount))
      .map(remainingBalance -> new ActionValidationResult(remainingBalance, requestedAmount));
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

  protected void validateIfAccountsExist(Map<String, Account> accounts) {
    accounts.values().forEach(this::validateIfAccountExists);
  }

  protected MonetaryValue calculateTotalRemaining(List<Account> accounts) {
    return accounts.stream()
      .map(Account::getRemaining)
      .map(MonetaryValue::new)
      .reduce(MonetaryValue::add)
      .orElse(new MonetaryValue(BigDecimal.ZERO));
  }

  protected abstract void validateAccountStatuses(List<Account> account);

  protected abstract Future<Void> validateAmountMaximum(List<Account> accounts,
    MonetaryValue requestedAmount);

  protected abstract Future<MonetaryValue> calculateRemainingBalance(List<Account> accounts,
    MonetaryValue requestedAmount);
}
