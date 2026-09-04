package org.folio.rest.service;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.HttpStatus.HTTP_NO_CONTENT;
import static org.folio.rest.jaxrs.resource.Accounts.PutAccountsByAccountIdResponse;
import static org.folio.rest.jaxrs.resource.Accounts.PutAccountsByAccountIdResponse.respond500WithTextPlain;
import static org.folio.rest.persist.PgUtil.put;
import static org.folio.rest.utils.AccountHelper.isClosedAndHasZeroRemainingAmount;
import static org.folio.rest.utils.MetadataHelper.populateMetadata;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.repository.AccountRepository;
import org.folio.rest.service.action.context.ActionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Promise;

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

  public Future<AsyncResult<Response>> updateAccount(String accountId, Account account) {
    final Promise<AsyncResult<Response>> putCompleted = Promise.promise();

    put(ACCOUNTS_TABLE, account, accountId, okapiHeaders, context,
      PutAccountsByAccountIdResponse.class, putCompleted::complete);

    return putCompleted.future().compose(responseResult -> {
      if (!isFeeFineUpdateSucceeded(responseResult)) {
        return succeededFuture(responseResult);
      }

      eventPublisher.publishAccountBalanceChangeEvent(account);

      if (isFeeFineWithLoanClosed(account)) {
        return eventPublisher.publishLoanRelatedFeeFineClosedEvent(account.getLoanId())
          .map(responseResult);
      }

      return succeededFuture(responseResult);
    }).recover(error -> {
      log.error("Cannot publish fee/fine closed event [feeFineId - {}, loanId - {}]" +
        " error occurred {}", account.getLoanId(), account.getId(), error);

      return succeededFuture(succeededFuture(respond500WithTextPlain(error.getMessage())));
    });
  }

  public Future<Account> updateAccount(Account account, Map<String, String> headers) {
    populateMetadata(account, headers);

    return accountRepository.update(account)
      .onSuccess(a -> eventPublisher.publishAccountBalanceChangeEvent(account));
  }

  public void publishLoanRelatedFeeFineClosedEvent(ActionContext actionContext) {
    actionContext.getAccounts().values().stream()
      .filter(this::isFeeFineWithLoanClosed)
      .map(Account::getLoanId)
      .distinct()
      .forEach(eventPublisher::publishLoanRelatedFeeFineClosedEvent);
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
