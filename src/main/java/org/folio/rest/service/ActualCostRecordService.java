package org.folio.rest.service;

import static io.vertx.core.Future.succeededFuture;

import java.util.Map;

import org.folio.rest.client.ActualCostRecordClient;
import org.folio.rest.jaxrs.model.ActualCostRecord;
import org.folio.rest.jaxrs.model.DoNotBillActualCost;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpResponse;

public class ActualCostRecordService {
  private final ActualCostRecordClient actualCostRecordClient;

  public ActualCostRecordService(Vertx vertx, Map<String, String> okapiHeaders) {
    this.actualCostRecordClient = new ActualCostRecordClient(vertx, okapiHeaders);
  }

  public Future<HttpResponse<Buffer>> cancelActualCostRecord(DoNotBillActualCost doNotBillActualCostEntity) {
    return actualCostRecordClient.fetchActualCostRecordById(doNotBillActualCostEntity.getActualCostRecordId())
      .compose(actualCostRecord -> updateActualCostRecordWithCancelledStatus(
        actualCostRecord, doNotBillActualCostEntity))
      .compose(actualCostRecordClient::updateActualCostRecord)
      .recover(Future::failedFuture);
  }

  private Future<ActualCostRecord> updateActualCostRecordWithCancelledStatus(
    ActualCostRecord actualCostRecord, DoNotBillActualCost doNotBillActualCostEntity) {

    return succeededFuture(actualCostRecord
      .withStatus(ActualCostRecord.Status.CANCELLED)
      .withAdditionalInfoForPatron(doNotBillActualCostEntity.getAdditionalInfoForPatron())
      .withAdditionalInfoForStaff(doNotBillActualCostEntity.getAdditionalInfoForStaff()));
  }
}
