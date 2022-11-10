package org.folio.rest.client;

import static org.folio.HttpStatus.HTTP_NO_CONTENT;

import java.util.Map;

import org.folio.rest.jaxrs.model.ActualCostRecord;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpResponse;

public class ActualCostRecordClient extends OkapiClient {

  public static final String ACTUAL_COST_RECORD_STORAGE = "/actual-cost-record-storage/actual-cost-records";

  public ActualCostRecordClient(Vertx vertx, Map<String, String> okapiHeaders) {
    super(vertx, okapiHeaders);
  }

  public Future<ActualCostRecord> fetchActualCostRecordById(String actualCostRecordId) {
    return getById(ACTUAL_COST_RECORD_STORAGE, actualCostRecordId,
      ActualCostRecord.class);
  }

  public Future<HttpResponse<Buffer>> updateActualCostRecord(ActualCostRecord actualCostRecord) {
    final Promise<HttpResponse<Buffer>> promise = Promise.promise();

    okapiPutAbs(ACTUAL_COST_RECORD_STORAGE, actualCostRecord.getId())
      .sendJson(actualCostRecord, response -> {
        if (response.failed()) {
          promise.fail(response.cause());
        } else {
          promise.complete(response.result());
        }
      });

    return promise.future()
      .compose(response -> {
        if (response.statusCode() != HTTP_NO_CONTENT.toInt()) {
          log.error("The actual cost record was not updated, the error code is {}, " +
            "error message {}", response.statusCode(), response.statusMessage());
        }
        return Future.succeededFuture(response);
      })
      .recover(Future::failedFuture);
  }
}
