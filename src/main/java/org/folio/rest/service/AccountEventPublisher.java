package org.folio.rest.service;

import static org.folio.rest.domain.EventType.FEE_FINE_BALANCE_CHANGED;
import static org.folio.rest.domain.EventType.LOAN_RELATED_FEE_FINE_CLOSED;
import static org.folio.rest.domain.LoanRelatedFeeFineClosedEvent.forActualCostRecord;
import static org.folio.rest.utils.JsonHelper.write;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.domain.LoanRelatedFeeFineClosedEvent;
import org.folio.rest.domain.MonetaryValue;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.ActualCostRecord;
import org.folio.util.UuidUtil;

import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class AccountEventPublisher {
  private static final Logger log = LogManager.getLogger(AccountEventPublisher.class);

  private final KafkaEventProducer kafkaEventProducer;
  private final Map<String, String> headers;

  public AccountEventPublisher(Context context, Map<String, String> headers) {
    this(context.owner(), headers);
  }

  public AccountEventPublisher(Vertx vertx, Map<String, String> headers) {
    this.kafkaEventProducer = new KafkaEventProducer(vertx);
    this.headers = headers;
  }

  public void publishAccountBalanceChangeEvent(Account account) {
    final String payload = createBalanceChangedPayload(account);

    kafkaEventProducer.publish(FEE_FINE_BALANCE_CHANGED, payload, headers)
      .onFailure(e -> log.error("Failed to publish {} event for account [id={}]: {}",
        FEE_FINE_BALANCE_CHANGED, account.getId(), e.getMessage()));
  }

  public void publishDeletedAccountBalanceChangeEvent(String accountId) {
    final Account account = new Account()
      .withId(accountId)
      .withRemaining(new MonetaryValue(BigDecimal.ZERO));

    publishAccountBalanceChangeEvent(account);
  }

  public CompletableFuture<Void> publishLoanRelatedFeeFineClosedEvent(String loanId) {
    return kafkaEventProducer.publish(LOAN_RELATED_FEE_FINE_CLOSED,
        new LoanRelatedFeeFineClosedEvent(loanId).toJsonString(), headers)
      .toCompletionStage()
      .toCompletableFuture();
  }

  public CompletableFuture<Void> publishLoanRelatedFeeFineClosedEvent(
    ActualCostRecord actualCostRecord) {

    return kafkaEventProducer.publish(LOAN_RELATED_FEE_FINE_CLOSED,
        forActualCostRecord(actualCostRecord).toJsonString(), headers)
      .toCompletionStage()
      .toCompletableFuture();
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
