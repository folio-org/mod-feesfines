package org.folio.rest.repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.persist.Conn;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.TenantTool;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;

public class AccountRepository extends AbstractRepository {
  private static final String ACCOUNTS_TABLE = "accounts";

  public AccountRepository(PostgresClient pgClient) {
    super(pgClient);
  }

  public AccountRepository(Context context, Map<String, String> headers) {
    this(PostgresClient.getInstance(context.owner(), TenantTool.tenantId(headers)));
  }

  public Future<Account> save(Account account) {
    return save(ACCOUNTS_TABLE, account.getId(), account);
  }

  public Future<Account> save(Account account, Conn conn) {
    return save(ACCOUNTS_TABLE, account.getId(), account, conn);
  }

  public Future<Account> getAccountById(String accountId) {
    return pgClient.getById(ACCOUNTS_TABLE, accountId, Account.class);
  }

  public Future<Map<String, Account>> getAccountsById(List<String> accountIds) {
    Promise<Map<String, Account>> promise = Promise.promise();
    pgClient.getById(ACCOUNTS_TABLE, new JsonArray(accountIds), Account.class, promise::handle);
    return promise.future();
  }

  public Future<Map<String, Account>> getAccountsByIdWithNulls(List<String> accountIds) {
    return getAccountsById(accountIds)
      .map(accountsMap -> accountIds.stream()
        // keys are ALL requested IDs, value is null if not found
        .collect(HashMap<String, Account>::new, (m, v) -> m.put(v, accountsMap.get(v)), HashMap::putAll)
      );
  }

  public Future<Account> update(Account account) {
    return pgClient.update(ACCOUNTS_TABLE, account, account.getId())
      .map(account);
  }

}
