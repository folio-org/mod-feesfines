package org.folio.rest.repository;

import java.util.Optional;

import org.folio.rest.domain.FeeFineNoticeContext;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.Feefineaction;
import org.folio.rest.persist.PostgresClient;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;

public class AccountRepository {
  private static final String ACCOUNTS_TABLE = "accounts";

  private final PostgresClient pgClient;

  public AccountRepository(PostgresClient pgClient) {
    this.pgClient = pgClient;
  }

  public Future<FeeFineNoticeContext> loadAccount(FeeFineNoticeContext context) {
    Optional<String> optionalAccountId = Optional.ofNullable(context)
      .map(FeeFineNoticeContext::getPrimaryAction)
      .map(Feefineaction::getAccountId);

    if (!optionalAccountId.isPresent()) {
      return Future.failedFuture(new IllegalArgumentException("Account id is not present"));
    }

    return getAccountById(optionalAccountId.get()).map(context::withAccount);
  }

  public Future<Account> getAccountById(String accountId) {
    Promise<Account> promise = Promise.promise();
    pgClient.getById(ACCOUNTS_TABLE, accountId, Account.class, promise);
    return promise.future();
  }

  public Future<Account> update(Account account) {
    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient.update(ACCOUNTS_TABLE, account, account.getId(), promise);
    return promise.future().map(account);
  }

}
