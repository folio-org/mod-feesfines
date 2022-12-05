package org.folio.rest.service;

import static org.folio.rest.jaxrs.model.ActualCostRecord.Status.CANCELLED;

import java.util.Map;

import org.folio.rest.jaxrs.model.ActualCostFeeFineCancel;
import org.folio.rest.jaxrs.model.ActualCostRecord;

import io.vertx.core.Context;
import io.vertx.core.Future;

public class ActualCostFeeFineCancellationService extends ActualCostFeeFineService {
  private final AccountEventPublisher accountEventPublisher;

  public ActualCostFeeFineCancellationService(Context context, Map<String, String> okapiHeaders) {
    super(context, okapiHeaders);
    this.accountEventPublisher = new AccountEventPublisher(context.owner(), okapiHeaders);
  }

  public Future<ActualCostRecord> cancel(ActualCostFeeFineCancel request) {
    return fetchActualCostRecord(request.getActualCostRecordId())
      .compose(this::failIfActualCostRecordIsNotOpen)
      .compose(actualCostRecord -> updateActualCostRecord(actualCostRecord, request))
      .onSuccess(accountEventPublisher::publishLoanRelatedFeeFineClosedEvent);
  }

  private Future<ActualCostRecord> updateActualCostRecord(ActualCostRecord actualCostRecord,
    ActualCostFeeFineCancel actualCostFeeFineCancel) {

    return updateActualCostRecord(actualCostRecord
      .withStatus(CANCELLED)
      .withAdditionalInfoForPatron(actualCostFeeFineCancel.getAdditionalInfoForPatron())
      .withAdditionalInfoForStaff(actualCostFeeFineCancel.getAdditionalInfoForStaff()));
  }
}
