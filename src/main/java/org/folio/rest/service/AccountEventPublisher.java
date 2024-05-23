package org.folio.rest.service;

import static org.folio.rest.domain.EventType.FEE_FINE_BALANCE_CHANGED;
import static org.folio.rest.domain.EventType.LOAN_RELATED_FEE_FINE_CLOSED;
import static org.folio.rest.domain.LoanRelatedFeeFineClosedEvent.forActualCostRecord;
import static org.folio.rest.utils.JsonHelper.write;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.folio.rest.domain.LoanRelatedFeeFineClosedEvent;
import org.folio.rest.domain.MonetaryValue;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.ActualCostRecord;
import org.folio.util.UuidUtil;

import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class AccountEventPublisher {
  private final EventPublisher eventPublisher;

  public AccountEventPublisher(Context context, Map<String, String> headers) {
    this(context.owner(), headers);
  }

  public AccountEventPublisher(Vertx vertx, Map<String, String> headers) {
    eventPublisher = new EventPublisher(vertx, headers);
  }

  public void publishAccountBalanceChangeEvent(Account account) {
    final String payload = createBalanceChangedPayload(account);

    eventPublisher.publishEventAsynchronously(FEE_FINE_BALANCE_CHANGED, payload);
  }

  public void publishDeletedAccountBalanceChangeEvent(String accountId) {
    final Account account = new Account()
      .withId(accountId)
      .withRemaining(new MonetaryValue(BigDecimal.ZERO));

    publishAccountBalanceChangeEvent(account);
  }

  public CompletableFuture<Void> publishLoanRelatedFeeFineClosedEvent(String loanId) {
    return eventPublisher.publishEvent(LOAN_RELATED_FEE_FINE_CLOSED,
      new LoanRelatedFeeFineClosedEvent(loanId).toJsonString());
  }

  public CompletableFuture<Void> publishLoanRelatedFeeFineClosedEvent(
    ActualCostRecord actualCostRecord) {

    return eventPublisher.publishEvent(LOAN_RELATED_FEE_FINE_CLOSED,
      forActualCostRecord(actualCostRecord).toJsonString());
  }

  private String createBalanceChangedPayload(Account account) {
    JsonObject payload = new JsonObject();
    write(payload, "userId", account.getUserId());
    write(payload, "feeFineId", account.getId());
    write(payload, "feeFineTypeId", account.getFeeFineId());
    write(payload, "balance", account.getRemaining());
    if (UuidUtil.isUuid(account.getLoanId())) {
      write(payload, "loanId", account.getLoanId());
    }

    return payload.toString();
  }
}
