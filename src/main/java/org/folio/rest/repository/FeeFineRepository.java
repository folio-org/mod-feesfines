package org.folio.rest.repository;

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

  public Future<Feefine> getById(String id) {
    Promise<Feefine> promise = Promise.promise();
    pgClient.getById(FEEFINES_TABLE, id, Feefine.class, promise);
    return promise.future();
  }
}
