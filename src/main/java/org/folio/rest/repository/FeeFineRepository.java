package org.folio.rest.repository;


import org.folio.rest.domain.FeeFineNoticeContext;
import org.folio.rest.jaxrs.model.Feefine;
import org.folio.rest.persist.PostgresClient;

import io.vertx.core.Future;

public class FeeFineRepository {

  private static final String FEEFINES_TABLE = "feefines";

  private PostgresClient pgClient;

  public FeeFineRepository(PostgresClient pgClient) {
    this.pgClient = pgClient;
  }

  public Future<FeeFineNoticeContext> loadFeefine(FeeFineNoticeContext context) {
    Future<Feefine> future = Future.future();
    pgClient.getById(FEEFINES_TABLE, context.getAccount().getFeeFineId(), Feefine.class, future);
    return future.map(context::withFeefine);
  }
}
