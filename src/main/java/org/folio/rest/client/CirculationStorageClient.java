package org.folio.rest.client;

import java.util.Map;

import org.folio.rest.jaxrs.model.Loan;
import org.folio.rest.jaxrs.model.LoanPolicy;

import io.vertx.core.Future;
import io.vertx.core.Vertx;

public class CirculationStorageClient extends OkapiClient {
  public CirculationStorageClient(Vertx vertx, Map<String, String> okapiHeaders) {
    super(vertx, okapiHeaders);
  }

  public Future<Loan> getLoanById(String id) {
    return getById("/loan-storage/loans", id, Loan.class);
  }

  public Future<LoanPolicy> getLoanPolicyById(String id) {
    return getById("/loan-policy-storage/loan-policies", id, LoanPolicy.class);
  }
}
