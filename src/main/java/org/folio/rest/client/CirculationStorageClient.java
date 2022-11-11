package org.folio.rest.client;

import static org.folio.HttpStatus.HTTP_NO_CONTENT;

import java.util.Collection;
import java.util.Map;

import org.folio.rest.jaxrs.model.ActualCostRecord;
import org.folio.rest.jaxrs.model.Loan;
import org.folio.rest.jaxrs.model.LoanPolicy;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpResponse;

public class CirculationStorageClient extends OkapiClient {
  private static final String ACTUAL_COST_RECORD_STORAGE =
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
    return getById(ACTUAL_COST_RECORD_STORAGE, actualCostRecordId,
      org.folio.rest.jaxrs.model.ActualCostRecord.class);
  }

  public Future<HttpResponse<Buffer>> updateActualCostRecord(ActualCostRecord actualCostRecord) {
    return okapiPutAbs(ACTUAL_COST_RECORD_STORAGE, actualCostRecord.getId())
      .sendJson(actualCostRecord);
  }
}
