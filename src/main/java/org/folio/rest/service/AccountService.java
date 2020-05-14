package org.folio.rest.service;

import static io.vertx.core.Future.succeededFuture;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.folio.HttpStatus.HTTP_NO_CONTENT;
import static org.folio.rest.domain.AccountStatus.CLOSED;
import static org.folio.rest.domain.AccountStatus.forValue;
import static org.folio.rest.domain.AccountWithLoanClosedEvent.forAccount;
import static org.folio.rest.jaxrs.resource.Accounts.PutAccountsByAccountIdResponse;
import static org.folio.rest.jaxrs.resource.Accounts.PutAccountsByAccountIdResponse.respond500WithTextPlain;
import static org.folio.rest.persist.PgUtil.put;
import static org.folio.rest.tools.utils.TenantTool.tenantId;
import static org.folio.util.pubsub.PubSubClientUtils.constructModuleName;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.folio.rest.client.FeeFinePubSubClient;
import org.folio.rest.domain.AccountStatus;
import org.folio.rest.domain.AccountWithLoanClosedEvent;
import org.folio.rest.domain.FeeFineAmount;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.Event;
import org.folio.rest.jaxrs.model.EventMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;

public class AccountService {
  private static final Logger log = LoggerFactory.getLogger(AccountService.class);
  private static final String ACCOUNTS_TABLE = "accounts";
  private static final String ACCOUNT_CLOSED_EVENT_TYPE = "FEESFINES_ACCOUNT_WITH_LOAN_CLOSED";
  private static final int EVENT_TIME_TO_LIVE_MINUTES = 30;

  public CompletableFuture<AsyncResult<Response>> updateAccount(String accountId,
    Account account, Map<String, String> headers, Context context) {

    final FeeFinePubSubClient pubSubClient = new FeeFinePubSubClient(context, headers);
    final CompletableFuture<AsyncResult<Response>> putCompleted = new CompletableFuture<>();

    put(ACCOUNTS_TABLE, account, accountId, headers, context,
      PutAccountsByAccountIdResponse.class, putCompleted::complete);

    return putCompleted.thenCompose(responseResult -> {
      if (!isAccountWithLoanClosed(account)
        || !isAccountUpdateSucceeded(responseResult)) {
        return completedFuture(responseResult);
      }

      final Event event = createAccountWithLoanClosedEvent(account, headers);
      return pubSubClient.publishEvent(event).thenApply(notUsed -> responseResult);
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

  private Event createAccountWithLoanClosedEvent(Account forAccount,
    Map<String, String> okapiHeaders) {

    final AccountWithLoanClosedEvent payload = forAccount(forAccount);

    final EventMetadata eventMetadata = new EventMetadata()
      .withPublishedBy(constructModuleName())
      .withTenantId(tenantId(okapiHeaders))
      .withEventTTL(EVENT_TIME_TO_LIVE_MINUTES);

    return new Event()
      .withId(UUID.randomUUID().toString())
      .withEventType(ACCOUNT_CLOSED_EVENT_TYPE)
      .withEventPayload(payload.toJsonString())
      .withEventMetadata(eventMetadata);
  }
}
