package org.folio.rest.repository;

import org.folio.rest.jaxrs.model.Owner;
import org.folio.rest.persist.PostgresClient;

import io.vertx.core.Future;

public class OwnerRepository {

  private static final String OWNERS_TABLE = "owners";

  private final PostgresClient pgClient;

  public OwnerRepository(PostgresClient pgClient) {
    this.pgClient = pgClient;
  }

  public Future<Owner> getById(String id) {
    return pgClient.getById(OWNERS_TABLE, id, Owner.class);
  }

}
