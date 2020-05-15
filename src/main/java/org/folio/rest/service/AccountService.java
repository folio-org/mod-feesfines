package org.folio.rest.service;

import static io.vertx.core.Future.succeededFuture;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.folio.HttpStatus.HTTP_NO_CONTENT;
import static org.folio.rest.domain.AccountStatus.CLOSED;
import static org.folio.rest.domain.AccountStatus.forValue;
import static org.folio.rest.jaxrs.resource.Accounts.PutAccountsByAccountIdResponse;
import static org.folio.rest.jaxrs.resource.Accounts.PutAccountsByAccountIdResponse.respond500WithTextPlain;
import static org.folio.rest.persist.PgUtil.put;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.folio.rest.domain.AccountStatus;
import org.folio.rest.domain.FeeFineAmount;
import org.folio.rest.jaxrs.model.Account;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;

public class AccountService {
  private static final Logger log = LoggerFactory.getLogger(AccountService.class);
  private static final String ACCOUNTS_TABLE = "accounts";

  public CompletableFuture<AsyncResult<Response>> updateAccount(String accountId,
    Account account, Map<String, String> headers, Context context) {

    final PubSubService pubSubService = new PubSubService(headers, context);
    final CompletableFuture<AsyncResult<Response>> putCompleted = new CompletableFuture<>();

    put(ACCOUNTS_TABLE, account, accountId, headers, context,
      PutAccountsByAccountIdResponse.class, putCompleted::complete);

    return putCompleted.thenCompose(responseResult -> {
      if (!isAccountUpdateSucceeded(responseResult)) {
        return completedFuture(responseResult);
      }

      pubSubService.publishAccountBalanceChangeEvent(account);

      if (isAccountWithLoanClosed(account)) {
        return pubSubService.publishAccountWithLoanClosedEvent(account)
          .thenApply(notUsed -> responseResult);
      }

      return completedFuture(responseResult);
    }).exceptionally(error -> {
      log.error("Cannot publish account closed event [loanId - {}, accountId - {}]," +
        " error occurred {}", account.getLoanId(), account.getId(), error);

      return succeededFuture(respond500WithTextPlain(error.getMessage()));
    });
  }

  private boolean isAccountWithLoanClosed(Account account) {
    final FeeFineAmount feeFineAmount = new FeeFineAmount(account.getRemaining());
    final AccountStatus accountStatus = account.getStatus() != null
      ? forValue(account.getStatus().getName()) : null;

    return accountStatus == CLOSED && StringUtils.isNotBlank(account.getLoanId())
      && feeFineAmount.hasZeroAmount();
  }

  private boolean isAccountUpdateSucceeded(AsyncResult<Response> responseAsyncResult) {
    return responseAsyncResult.succeeded()
      || responseAsyncResult.result().getStatus() == HTTP_NO_CONTENT.toInt();
  }
}
