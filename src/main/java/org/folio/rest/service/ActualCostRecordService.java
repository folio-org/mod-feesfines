package org.folio.rest.service;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;
import static java.lang.String.format;
import static org.folio.rest.jaxrs.model.ActualCostRecord.Status.CANCELLED;
import static org.folio.rest.jaxrs.model.ActualCostRecord.Status.OPEN;

import java.util.Map;

import org.folio.rest.client.CirculationStorageClient;
import org.folio.rest.exception.FailedValidationException;
import org.folio.rest.jaxrs.model.ActualCostFeeFineCancel;
import org.folio.rest.jaxrs.model.ActualCostRecord;

import io.vertx.core.Future;
import io.vertx.core.Vertx;

public class ActualCostRecordService {
  private final CirculationStorageClient circulationStorageClient;
  private final AccountEventPublisher accountEventPublisher;

  public ActualCostRecordService(Vertx vertx, Map<String, String> okapiHeaders) {
    this.circulationStorageClient = new CirculationStorageClient(vertx, okapiHeaders);
    this.accountEventPublisher = new AccountEventPublisher(vertx, okapiHeaders);
  }

  public Future<ActualCostRecord> cancelActualCostRecord(
    ActualCostFeeFineCancel cancellationRequest) {

    return circulationStorageClient.fetchActualCostRecordById(
      cancellationRequest.getActualCostRecordId())
        .compose(this::checkIfRecordIsOpen)
        .map(actualCostRecord -> updateActualCostRecord(actualCostRecord, cancellationRequest,
          CANCELLED))
        .compose(circulationStorageClient::updateActualCostRecord)
        .compose(this::sendLoanRelatedFeeFineClosedEvent);
  }

  private Future<ActualCostRecord> sendLoanRelatedFeeFineClosedEvent(ActualCostRecord actualCostRecord) {
    accountEventPublisher.publishLoanRelatedFeeFineClosedEvent(actualCostRecord);

    return succeededFuture(actualCostRecord);
  }

  private Future<ActualCostRecord> checkIfRecordIsOpen(ActualCostRecord actualCostRecord) {
    return failIfRecordAlreadyProcessed(actualCostRecord, OPEN);
  }

  private Future<ActualCostRecord> failIfRecordAlreadyProcessed(
    ActualCostRecord actualCostRecord, ActualCostRecord.Status status) {

    if (actualCostRecord.getStatus() != status) {
      return failedFuture(new FailedValidationException(format(
        "Actual cost record %s is already %s}", actualCostRecord.getId(),
        actualCostRecord.getStatus())));
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
