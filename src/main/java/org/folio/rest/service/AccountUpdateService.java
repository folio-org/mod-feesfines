package org.folio.rest.service;

import static io.vertx.core.Future.succeededFuture;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.folio.HttpStatus.HTTP_NO_CONTENT;
import static org.folio.rest.jaxrs.resource.Accounts.PutAccountsByAccountIdResponse;
import static org.folio.rest.jaxrs.resource.Accounts.PutAccountsByAccountIdResponse.respond500WithTextPlain;
import static org.folio.rest.persist.PgUtil.put;
import static org.folio.rest.utils.AccountHelper.isClosedAndHasZeroRemainingAmount;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.repository.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccountUpdateService {
  private static final Logger log = LoggerFactory.getLogger(AccountUpdateService.class);
  private static final String ACCOUNTS_TABLE = "accounts";

  private final AccountRepository accountRepository;
  private final AccountEventPublisher eventPublisher;
  private final Map<String, String> okapiHeaders;
  private final Context context;

  public AccountUpdateService(Map<String, String> okapiHeaders, Context context) {
    this.okapiHeaders = okapiHeaders;
    this.context = context;
    this.accountRepository = new AccountRepository(context, okapiHeaders);
    this.eventPublisher = new AccountEventPublisher(context, okapiHeaders);
  }

  public CompletableFuture<AsyncResult<Response>> updateAccount(String accountId, Account account) {
    final CompletableFuture<AsyncResult<Response>> putCompleted = new CompletableFuture<>();

    put(ACCOUNTS_TABLE, account, accountId, okapiHeaders, context,
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

  public Future<Account> updateAccount(Account account) {
    return accountRepository.update(account)
      .onSuccess(a -> {
        eventPublisher.publishAccountBalanceChangeEvent(account);
        if (isFeeFineWithLoanClosed(account)) {
          eventPublisher.publishLoanRelatedFeeFineClosedEvent(account);
        }
      });
  }

  private boolean isFeeFineWithLoanClosed(Account feeFine) {
    return isFeeFineAssociatedToLoan(feeFine) && isClosedAndHasZeroRemainingAmount(feeFine);
  }

  private boolean isFeeFineAssociatedToLoan(Account feeFine) {
    return StringUtils.isNotBlank(feeFine.getLoanId());
  }

  private boolean isFeeFineUpdateSucceeded(AsyncResult<Response> responseAsyncResult) {
    return responseAsyncResult.succeeded()
      && responseAsyncResult.result().getStatus() == HTTP_NO_CONTENT.toInt();
  }
}
