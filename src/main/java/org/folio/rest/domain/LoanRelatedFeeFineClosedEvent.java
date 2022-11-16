package org.folio.rest.domain;

import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.ActualCostRecord;

import io.vertx.core.json.JsonObject;

public final class LoanRelatedFeeFineClosedEvent {
  private final String loanId;
  private final String feeFineId;

  public LoanRelatedFeeFineClosedEvent(String loanId, String feeFineId) {
    this.loanId = loanId;
    this.feeFineId = feeFineId;
  }

  public String getLoanId() {
    return loanId;
  }

  public String getFeeFineId() {
    return feeFineId;
  }

  public String toJsonString() {
    return JsonObject.mapFrom(this).toString();
  }

  public static LoanRelatedFeeFineClosedEvent forFeeFine(Account feeFine) {
    return new LoanRelatedFeeFineClosedEvent(feeFine.getLoanId(), feeFine.getId());
  }

  public static LoanRelatedFeeFineClosedEvent forActualCostRecord(ActualCostRecord actualCostRecord) {
    return new LoanRelatedFeeFineClosedEvent(actualCostRecord.getLoan().getId(), null);
  }
}
