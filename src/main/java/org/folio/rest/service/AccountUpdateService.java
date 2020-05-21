package org.folio.rest.service;

import static io.vertx.core.Future.succeededFuture;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.folio.HttpStatus.HTTP_NO_CONTENT;
import static org.folio.rest.domain.FeeFineStatus.CLOSED;
import static org.folio.rest.domain.FeeFineStatus.forValue;
import static org.folio.rest.jaxrs.resource.Accounts.PutAccountsByAccountIdResponse;
import static org.folio.rest.jaxrs.resource.Accounts.PutAccountsByAccountIdResponse.respond500WithTextPlain;
import static org.folio.rest.persist.PgUtil.put;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.folio.rest.domain.FeeFineStatus;
import org.folio.rest.domain.FeeFineAmount;
import org.folio.rest.jaxrs.model.Account;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;

public class AccountUpdateService {
  private static final Logger log = LoggerFactory.getLogger(AccountUpdateService.class);
  private static final String ACCOUNTS_TABLE = "accounts";

  public CompletableFuture<AsyncResult<Response>> updateAccount(String accountId,
    Account account, Map<String, String> headers, Context context) {

    final AccountEventPublisher eventPublisher = new AccountEventPublisher(context, headers);
    final CompletableFuture<AsyncResult<Response>> putCompleted = new CompletableFuture<>();

    put(ACCOUNTS_TABLE, account, accountId, headers, context,
      PutAccountsByAccountIdResponse.class, putCompleted::complete);

    return putCompleted.thenCompose(responseResult -> {
      if (!isFeeFineUpdateSucceeded(responseResult)) {
        return completedFuture(responseResult);
      }

      eventPublisher.publishAccountBalanceChangeEvent(account);

      if (isFeeFineWithLoanClosed(account)) {
        return eventPublisher.publishLoanRelatedFeeFineClosedEvent(account)
          .thenApply(notUsed -> responseResult);
      }

      return completedFuture(responseResult);
    }).exceptionally(error -> {
      log.error("Cannot publish fee/fine closed event [feeFineId - {}, loanId - {}]" +
        " error occurred {}", account.getLoanId(), account.getId(), error);

      return succeededFuture(respond500WithTextPlain(error.getMessage()));
    });
  }

  private boolean isFeeFineWithLoanClosed(Account feeFine) {
    return isFeeFineAssociatedToLoan(feeFine) && isFeeFineClosed(feeFine);
  }

  private boolean isFeeFineClosed(Account feeFine) {
    final FeeFineAmount feeFineAmount = new FeeFineAmount(feeFine.getRemaining());
    final FeeFineStatus feeFineStatus = feeFine.getStatus() != null
      ? forValue(feeFine.getStatus().getName()) : null;

    return feeFineStatus == CLOSED && feeFineAmount.hasZeroAmount();
  }

  private boolean isFeeFineAssociatedToLoan(Account feeFine) {
    return StringUtils.isNotBlank(feeFine.getLoanId());
  }

  private boolean isFeeFineUpdateSucceeded(AsyncResult<Response> responseAsyncResult) {
    return responseAsyncResult.succeeded()
      || responseAsyncResult.result().getStatus() == HTTP_NO_CONTENT.toInt();
  }
}