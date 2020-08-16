package org.folio.rest.repository;

import java.util.List;

import org.folio.rest.jaxrs.model.Feefineaction;
import org.folio.rest.persist.Criteria.Criteria;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.persist.interfaces.Results;
import org.folio.rest.utils.FeeFineActionHelper;

import io.vertx.core.Future;
import io.vertx.core.Promise;

public class ActionRepository {
  private static final String ACTIONS_TABLE = "feefineactions";
  private final PostgresClient pgClient;

  public ActionRepository(PostgresClient pgClient) {
    this.pgClient = pgClient;
  }

  public Future<List<Feefineaction>> findActionsForAccount(String accountId) {
    if (accountId == null) {
      return Future.failedFuture(new IllegalArgumentException("Account ID is null"));
    }

    Criterion criterion = new Criterion(new Criteria()
      .addField("'accountId'")
      .setOperation("=")
      .setVal(accountId));

    Promise<Results<Feefineaction>> promise = Promise.promise();
    pgClient.get(ACTIONS_TABLE, Feefineaction.class, criterion, false, promise);

    return promise.future()
      .map(Results::getResults);
  }

  public Future<Feefineaction> findChargeForAccount(String accountId) {
    return findActionsForAccount(accountId)
      .map(actions -> actions.stream()
        .filter(FeeFineActionHelper::isCharge)
        .findAny()
        .orElse(null)
      );
  }

  public Future<String> save(Feefineaction feefineaction) {
    Promise<String> promise = Promise.promise();
    pgClient.save(ACTIONS_TABLE, feefineaction.getId(), feefineaction, promise);
    return promise.future();
  }
}
