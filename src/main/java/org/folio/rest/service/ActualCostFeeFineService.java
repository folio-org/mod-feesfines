package org.folio.rest.service;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;
import static java.lang.String.format;
import static org.folio.rest.jaxrs.model.ActualCostRecord.Status.OPEN;

import java.util.Map;

import org.folio.rest.client.CirculationStorageClient;
import org.folio.rest.exception.FailedValidationException;
import org.folio.rest.jaxrs.model.ActualCostRecord;

import io.vertx.core.Context;
import io.vertx.core.Future;

public abstract class ActualCostFeeFineService {
  final CirculationStorageClient circulationStorageClient;

  ActualCostFeeFineService(Context context, Map<String, String> okapiHeaders) {
    this.circulationStorageClient = new CirculationStorageClient(context.owner(), okapiHeaders);
  }

  Future<ActualCostRecord> fetchActualCostRecord(String actualCostRecordId) {
    return circulationStorageClient.fetchActualCostRecordById(actualCostRecordId);
  }

  Future<ActualCostRecord> updateActualCostRecord(ActualCostRecord actualCostRecord) {
    return circulationStorageClient.updateActualCostRecord(actualCostRecord);
  }

  Future<ActualCostRecord> failIfActualCostRecordIsNotOpen(ActualCostRecord actualCostRecord) {
    return failIfActualCostRecordIsNotInStatus(actualCostRecord, OPEN);
  }

  Future<ActualCostRecord> failIfActualCostRecordIsNotInStatus(ActualCostRecord actualCostRecord,
    ActualCostRecord.Status status) {

    if (actualCostRecord.getStatus() == status) {
      return succeededFuture(actualCostRecord);
    }

    return failedFuture(new FailedValidationException(format(
      "Actual cost record %s is already %s", actualCostRecord.getId(),
      actualCostRecord.getStatus())));
  }
}
