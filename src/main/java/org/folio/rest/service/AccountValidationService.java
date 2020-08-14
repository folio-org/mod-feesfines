package org.folio.rest.service;

import javax.ws.rs.core.Response;

import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.AccountsCheckRequest;
import org.folio.rest.jaxrs.model.AccountsCheckResponse;
import org.folio.rest.jaxrs.resource.Accounts;
import org.folio.rest.repository.AccountRepository;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;

public class AccountValidationService {

  private final AccountRepository accountRepository;
  static final String PAYMENT_ACTION = "Payment";

  public AccountValidationService(AccountRepository accountRepository) {
    this.accountRepository = accountRepository;
  }

  public void validatePayment(String accountId, AccountsCheckRequest entity,
    Handler<AsyncResult<Response>> asyncResultHandler) {

    accountRepository.getAccountById(accountId)
      .onSuccess(account -> {
        ValidationResult validateResult = validate(account, entity, PAYMENT_ACTION);
        if (validateResult.isAllowed) {
          AccountsCheckResponse response = create200AccountCheckResponse(
            accountId, entity.getAmount(), account.getRemaining());
          asyncResultHandler.handle(Future.succeededFuture(
            Accounts.PostAccountsCheckPayByAccountIdResponse
              .respond200WithApplicationJson(response)));
        } else {
          AccountsCheckResponse response = create422AccountCheckResponse(
            accountId, entity.getAmount(), validateResult.getErrorMessage());
          asyncResultHandler.handle(Future.succeededFuture(
            Accounts.PostAccountsCheckPayByAccountIdResponse
              .respond422WithApplicationJson(response)));
        }
      }).onFailure(e -> Future.succeededFuture(
        Accounts.PostAccountsCheckPayByAccountIdResponse.respond500WithTextPlain(e.getMessage())));
  }

  ValidationResult validate(Account account, AccountsCheckRequest request, String actionName) {
    double entityAmount = request.getAmount();
    if (entityAmount > account.getRemaining()) {
      return new ValidationResult(false,
        String.format("%s amount exceeds the selected amount", actionName));
    } else if (entityAmount < 0) {
      return new ValidationResult(false, "Invalid amount entered");
    } else {
      return new ValidationResult(true, account.getRemaining());
    }
  }

  private AccountsCheckResponse create422AccountCheckResponse(String accountId,
    double entityAmount, String errorMessage) {

    AccountsCheckResponse response = createBaseAccountCheckResponse(accountId, entityAmount);
    response.setAllowed(false);
    response.setErrorMessage(errorMessage);
    return response;
  }

  private AccountsCheckResponse create200AccountCheckResponse(String accountId,
    double entityAmount, double remaining) {

    AccountsCheckResponse response = createBaseAccountCheckResponse(accountId, entityAmount);
    response.setAllowed(true);
    response.setRemainingAmount(remaining);
    return response;
  }

  private AccountsCheckResponse createBaseAccountCheckResponse(
    String accountId, double entityAmount) {

    AccountsCheckResponse response = new AccountsCheckResponse();
    response.setAccountId(accountId);
    response.setAmount(entityAmount);
    return response;
  }

  static class ValidationResult {

    private boolean isAllowed;
    private String errorMessage;
    private Double remaining;

    public ValidationResult(boolean isAllowed, String errorMessage) {
      this.isAllowed = isAllowed;
      this.errorMessage = errorMessage;
    }

    public ValidationResult(boolean isAllowed, Double remaining) {
      this.isAllowed = isAllowed;
      this.remaining = remaining;
    }

    public boolean isAllowed() {
      return isAllowed;
    }

    public String getErrorMessage() {
      return errorMessage;
    }

    public Double getRemaining() {
      return remaining;
    }
  }
}
