package org.folio.rest.repository;

import org.folio.rest.domain.FeeFineNoticeContext;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.persist.PostgresClient;

import io.vertx.core.Future;

public class AccountRepository {

  private static final String ACCOUNTS_TABLE = "accounts";

  private PostgresClient pgClient;

  public AccountRepository(PostgresClient pgClient) {
    this.pgClient = pgClient;
  }

  public Future<FeeFineNoticeContext> loadAccount(FeeFineNoticeContext context) {
    Future<Account> future = Future.future();
    pgClient.getById(ACCOUNTS_TABLE, context.getFeefineaction().getAccountId(), Account.class, future);
    return future.map(context::withAccount);
  }
}
