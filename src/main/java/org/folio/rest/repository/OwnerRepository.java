package org.folio.rest.repository;

import org.folio.rest.jaxrs.model.Owner;
import org.folio.rest.persist.PostgresClient;

import io.vertx.core.Future;

public class OwnerRepository {

  private static final String OWNERS_TABLE = "owners";

  private PostgresClient pgClient;

  public OwnerRepository(PostgresClient pgClient) {
    this.pgClient = pgClient;
  }

  public Future<Owner> getById(String id) {
    Future<Owner> future = Future.future();
    pgClient.getById(OWNERS_TABLE, id, Owner.class, future);
    return future;
  }
}
