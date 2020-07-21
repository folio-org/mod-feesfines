package org.folio.rest.repository;


import java.util.Optional;

import org.folio.rest.domain.FeeFineNoticeContext;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.Feefine;
import org.folio.rest.persist.PostgresClient;

import io.vertx.core.Future;
import io.vertx.core.Promise;

public class FeeFineRepository {

  private static final String FEEFINES_TABLE = "feefines";

  private final PostgresClient pgClient;

  public FeeFineRepository(PostgresClient pgClient) {
    this.pgClient = pgClient;
  }

  public Future<FeeFineNoticeContext> loadFeefine(FeeFineNoticeContext context) {
    Optional<String> optionalFeeFineId = Optional.ofNullable(context)
      .map(FeeFineNoticeContext::getAccount)
      .map(Account::getFeeFineId);

    if (!optionalFeeFineId.isPresent()) {
      return Future.failedFuture(new IllegalArgumentException("Fee fine id is not present"));
    }

    Promise<Feefine> promise = Promise.promise();
    pgClient.getById(FEEFINES_TABLE, optionalFeeFineId.get(), Feefine.class, promise);
    return promise.future().map(context::withFeefine);
  }
}
