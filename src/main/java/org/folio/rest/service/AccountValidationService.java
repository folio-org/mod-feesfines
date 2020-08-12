package org.folio.rest.service;

import javax.ws.rs.core.Response;

import org.folio.rest.jaxrs.model.AccountsCheckRequest;
import org.folio.rest.jaxrs.model.AccountsCheckResponse;
import org.folio.rest.jaxrs.resource.Accounts;
import org.folio.rest.repository.AccountRepository;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;

public class AccountValidationService {

  private final AccountRepository accountRepository;

  public AccountValidationService(AccountRepository accountRepository) {
    this.accountRepository = accountRepository;
  }

  public void validatePayment(String accountId, AccountsCheckRequest entity,
    Handler<AsyncResult<Response>> asyncResultHandler) {

    accountRepository.getAccountById(accountId)
      .onComplete(account -> {
        double entityAmount = entity.getAmount();
        if (entityAmount > account.result().getRemaining()) {
          AccountsCheckResponse response = create422AccountCheckResponse(
            accountId, entityAmount, "Payment amount exceeds the selected amount");
          asyncResultHandler.handle(Future.succeededFuture(
            Accounts.PostAccountsCheckPayByAccountIdResponse
              .respond422WithApplicationJson(response)));
        }
        if (entityAmount < 0) {
          AccountsCheckResponse response = create422AccountCheckResponse(
            accountId, entityAmount, "Invalid amount entered");
          asyncResultHandler.handle(Future.succeededFuture(
            Accounts.PostAccountsCheckPayByAccountIdResponse
              .respond422WithApplicationJson(response)));
        } else {
          AccountsCheckResponse response = create200AccountCheckResponse(
            accountId, entityAmount, account.result().getRemaining());
          asyncResultHandler.handle(Future.succeededFuture(
            Accounts.PostAccountsCheckPayByAccountIdResponse
              .respond200WithApplicationJson(response)));
        }
      });
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
}
