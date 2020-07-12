package org.folio.rest.repository;

import java.util.Optional;

import org.folio.rest.domain.FeeFineNoticeContext;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.Feefineaction;
import org.folio.rest.persist.PostgresClient;

import io.vertx.core.Future;
import io.vertx.core.Promise;

public class AccountRepository {

  public static final String ACCOUNTS_TABLE = "accounts";

  private PostgresClient pgClient;

  public AccountRepository(PostgresClient pgClient) {
    this.pgClient = pgClient;
  }

  public Future<FeeFineNoticeContext> loadAccount(FeeFineNoticeContext context) {
    Optional<String> optionalAccountId = Optional.ofNullable(context)
      .map(FeeFineNoticeContext::getFeefineaction)
      .map(Feefineaction::getAccountId);

    if (!optionalAccountId.isPresent()) {
      return Future.failedFuture(new IllegalArgumentException("Account id is not present"));
    }

    Promise<Account> promise = Promise.promise();
    pgClient.getById(ACCOUNTS_TABLE, optionalAccountId.get(), Account.class, promise);
    return promise.future().map(context::withAccount);
  }
}
