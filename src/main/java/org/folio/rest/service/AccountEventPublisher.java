package org.folio.rest.service;

import static org.folio.rest.domain.LoanRelatedFeeFineClosedEvent.forActualCostRecord;
import static org.folio.rest.domain.event.FeeFineKafkaTopic.LOAN_RELATED_FEE_FINE_CLOSED;
import static org.folio.rest.utils.JsonHelper.write;

import java.math.BigDecimal;
import java.util.Map;

import org.folio.rest.domain.LoanRelatedFeeFineClosedEvent;
import org.folio.rest.domain.MonetaryValue;
import org.folio.rest.domain.event.FeeFineKafkaTopic;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.ActualCostRecord;
import org.folio.util.UuidUtil;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

public class AccountEventPublisher extends AbstractEventPublisher {

  public AccountEventPublisher(Context context, Map<String, String> headers) {
    super(context, headers);
  }

  public void publishAccountBalanceChangeEvent(Account account) {
    publish(account.getUserId(), FeeFineKafkaTopic.FEE_FINE_BALANCE_CHANGED, createBalanceChangedPayload(account))
      .onFailure(e -> log.error("Failed to publish log record event for account [id={}]", account.getId(), e));
  }

  public void publishDeletedAccountBalanceChangeEvent(String accountId) {
    final Account account = new Account()
      .withId(accountId)
      .withRemaining(new MonetaryValue(BigDecimal.ZERO));

    publishAccountBalanceChangeEvent(account);
  }

  public Future<Void> publishLoanRelatedFeeFineClosedEvent(Account account) {
    return publish(account.getUserId(), LOAN_RELATED_FEE_FINE_CLOSED,
      new LoanRelatedFeeFineClosedEvent(account.getLoanId()).toJson());
  }

  public Future<Void> publishLoanRelatedFeeFineClosedEvent(ActualCostRecord actualCostRecord) {
    return publish(actualCostRecord.getUser().getId(), LOAN_RELATED_FEE_FINE_CLOSED,
      forActualCostRecord(actualCostRecord).toJson());
  }

  private static JsonObject createBalanceChangedPayload(Account account) {
    JsonObject payload = new JsonObject();
    write(payload, "userId", account.getUserId());
    write(payload, "feeFineId", account.getId());
    write(payload, "feeFineTypeId", account.getFeeFineId());
    write(payload, "balance", account.getRemaining());
    if (UuidUtil.isUuid(account.getLoanId())) {
      write(payload, "loanId", account.getLoanId());
    }

    return payload;
  }
}
