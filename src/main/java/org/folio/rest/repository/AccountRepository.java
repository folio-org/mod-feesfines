package org.folio.rest.repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.folio.rest.domain.FeeFineNoticeContext;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.Feefineaction;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.TenantTool;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;

public class AccountRepository {
  private static final String ACCOUNTS_TABLE = "accounts";
  private final PostgresClient pgClient;

  public AccountRepository(PostgresClient pgClient) {
    this.pgClient = pgClient;
  }

  public AccountRepository(Context context, Map<String, String> headers) {
    this(PostgresClient.getInstance(context.owner(), TenantTool.tenantId(headers)));
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

  public Future<Map<String, Account>> getAccountsById(List<String> accountIds) {
    Promise<Map<String, Account>> promise = Promise.promise();
    pgClient.getById(ACCOUNTS_TABLE, new JsonArray(accountIds), Account.class, promise);
    return promise.future()
      .map(accountsMap -> accountIds.stream()
          .collect(HashMap::new, (m, v) -> m.put(v, accountsMap.get(v)), HashMap::putAll));
  }

  public Future<Account> update(Account account) {
    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient.update(ACCOUNTS_TABLE, account, account.getId(), promise);
    return promise.future().map(account);
  }

}
