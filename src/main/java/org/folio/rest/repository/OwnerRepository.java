package org.folio.rest.repository;

import java.util.Optional;

import org.folio.rest.domain.FeeFineNoticeContext;
import org.folio.rest.jaxrs.model.Feefine;
import org.folio.rest.jaxrs.model.Owner;
import org.folio.rest.persist.PostgresClient;

import io.vertx.core.Future;
import io.vertx.core.Promise;

public class OwnerRepository {

  private static final String OWNERS_TABLE = "owners";

  private final PostgresClient pgClient;

  public OwnerRepository(PostgresClient pgClient) {
    this.pgClient = pgClient;
  }

  public Future<FeeFineNoticeContext> loadOwner(FeeFineNoticeContext context) {
    Optional<String> optionalOwnerId = Optional.ofNullable(context)
        .map(FeeFineNoticeContext::getFeefine)
        .map(Feefine::getOwnerId);

    if (!optionalOwnerId.isPresent()) {
      return Future.failedFuture(new IllegalArgumentException("Owner id is not present"));
    }

    Promise<Owner> promise = Promise.promise();
    pgClient.getById(OWNERS_TABLE, optionalOwnerId.get(), Owner.class, promise);
    return promise.future().map(context::withOwner);
  }
}
