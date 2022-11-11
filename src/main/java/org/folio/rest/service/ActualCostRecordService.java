package org.folio.rest.service;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;
import static java.lang.String.format;
import static org.folio.rest.jaxrs.model.ActualCostRecord.Status.CANCELLED;

import java.util.Map;

import org.folio.rest.client.CirculationStorageClient;
import org.folio.rest.exception.FailedValidationException;
import org.folio.rest.jaxrs.model.ActualCostFeeFineCancel;
import org.folio.rest.jaxrs.model.ActualCostRecord;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpResponse;

public class ActualCostRecordService {
  private final CirculationStorageClient circulationStorageClient;

  public ActualCostRecordService(Vertx vertx, Map<String, String> okapiHeaders) {
    this.circulationStorageClient = new CirculationStorageClient(vertx, okapiHeaders);
  }

  public Future<HttpResponse<Buffer>> cancelActualCostRecord(ActualCostFeeFineCancel entity) {
    return circulationStorageClient.fetchActualCostRecordById(entity.getActualCostRecordId())
      .compose(this::checkIfRecordAlreadyCancelled)
      .map(actualCostRecord -> updateActualCostRecord(actualCostRecord, entity, CANCELLED))
      .compose(circulationStorageClient::updateActualCostRecord);
  }

  private Future<ActualCostRecord> checkIfRecordAlreadyCancelled(
    ActualCostRecord actualCostRecord) {

    if (actualCostRecord.getStatus() == CANCELLED) {
      return failedFuture(new FailedValidationException(format(
        "Actual cost record %s is already cancelled", actualCostRecord.getId())));
    }
    return succeededFuture(actualCostRecord);
  }

  private ActualCostRecord updateActualCostRecord(ActualCostRecord actualCostRecord,
    ActualCostFeeFineCancel actualCostFeeFineCancel, ActualCostRecord.Status status) {

    return actualCostRecord
      .withStatus(status)
      .withAdditionalInfoForPatron(actualCostFeeFineCancel.getAdditionalInfoForPatron())
      .withAdditionalInfoForStaff(actualCostFeeFineCancel.getAdditionalInfoForStaff());
  }
}
