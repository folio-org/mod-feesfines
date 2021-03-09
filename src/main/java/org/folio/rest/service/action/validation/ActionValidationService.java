package org.folio.rest.service.action.validation;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
  private static final Logger logger = LogManager.getLogger(ActionValidationService.class);

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
    return accountRepository.getAccountsByIdWithNulls(accountIds)
      .compose(accountsMap -> validate(accountsMap, rawAmount));
  }

  protected Future<ActionValidationResult> validate(String accountId, Account account,
    String rawAmount) {

    return validate(singletonMap(accountId, account), rawAmount);
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

  protected void validateIfAccountsExist(Map<String, Account> accounts) {
    accounts.keySet().forEach(accountId -> {
      if (accounts.get(accountId) == null) {
        String errorMessage = format("Fee/fine ID %s not found", accountId);
        logger.error(errorMessage);
        throw new AccountNotFoundValidationException(errorMessage);
      }
    });
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
