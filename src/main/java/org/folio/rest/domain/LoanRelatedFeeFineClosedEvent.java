package org.folio.rest.domain;

import org.folio.rest.jaxrs.model.ActualCostRecord;

import io.vertx.core.json.JsonObject;
import lombok.Getter;

@Getter
public final class LoanRelatedFeeFineClosedEvent {
  private final String loanId;

  public LoanRelatedFeeFineClosedEvent(String loanId) {
    this.loanId = loanId;
  }

  public JsonObject toJson() {
    return JsonObject.mapFrom(this);
  }

  public static LoanRelatedFeeFineClosedEvent forActualCostRecord(ActualCostRecord actualCostRecord) {
    return new LoanRelatedFeeFineClosedEvent(actualCostRecord.getLoan().getId());
  }
}
