package org.folio.rest.client;

import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;

import java.util.Collection;
import java.util.Map;

import org.folio.rest.exception.http.HttpNotFoundException;
import org.folio.rest.exception.http.HttpPutException;
import org.folio.rest.jaxrs.model.ActualCostRecord;
import org.folio.rest.jaxrs.model.Loan;
import org.folio.rest.jaxrs.model.LoanPolicy;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.client.HttpResponse;

public class CirculationStorageClient extends OkapiClient {
  private static final String ACTUAL_COST_RECORD_STORAGE_URL =
    "/actual-cost-record-storage/actual-cost-records";

  public CirculationStorageClient(Vertx vertx, Map<String, String> okapiHeaders) {
    super(vertx, okapiHeaders);
  }

  public Future<Loan> getLoanById(String id) {
    return getById("/loan-storage/loans", id, Loan.class);
  }

  public Future<Collection<Loan>> getLoansByIds(Collection<String> ids) {
    return getByIds("/loan-storage/loans", ids, Loan.class, "loans");
  }

  public Future<LoanPolicy> getLoanPolicyById(String id) {
    return getById("/loan-policy-storage/loan-policies", id, LoanPolicy.class);
  }

  public Future<Collection<LoanPolicy>> getLoanPoliciesByIds(Collection<String> ids) {
    return getByIds("/loan-policy-storage/loan-policies", ids, LoanPolicy.class, "loanPolicies");
  }

  public Future<ActualCostRecord> fetchActualCostRecordById(String actualCostRecordId) {
    return getById(ACTUAL_COST_RECORD_STORAGE_URL, actualCostRecordId, ActualCostRecord.class);
  }

  public Future<ActualCostRecord> updateActualCostRecord(ActualCostRecord actualCostRecord) {
    Promise<HttpResponse<Buffer>> promise = Promise.promise();
    okapiPutAbs(ACTUAL_COST_RECORD_STORAGE_URL, actualCostRecord.getId())
      .sendJson(actualCostRecord, promise);

    return promise.future().compose(response -> {
      int responseStatus = response.statusCode();
      if (responseStatus != 204) {
        log.error("Failed to update record with ID {}. Response status code: {}",
          actualCostRecord.getId(), responseStatus);
        if (responseStatus == 404) {
          return failedFuture(new HttpNotFoundException(HttpMethod.PUT, ACTUAL_COST_RECORD_STORAGE_URL,
            response));
        }
        return failedFuture(new HttpPutException(ACTUAL_COST_RECORD_STORAGE_URL, response));
      }
      return succeededFuture(actualCostRecord);
    });
  }
}
