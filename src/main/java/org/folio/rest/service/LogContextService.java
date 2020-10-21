package org.folio.rest.service;

import static org.folio.rest.domain.logs.LogContextHelper.buildFeeFineLogContext;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.folio.rest.client.UsersClient;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.Feefineaction;
import org.folio.rest.persist.PgUtil;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.repository.AccountRepository;
import org.folio.rest.repository.FeeFineRepository;

import java.util.Map;

public class LogContextService {
  private final FeeFineRepository feeFineRepository;
  private final AccountRepository accountRepository;
  private final UsersClient usersClient;

  public LogContextService(Vertx vertx, Map<String, String> okapiHeaders) {
    PostgresClient pgClient = PgUtil.postgresClient(vertx.getOrCreateContext(), okapiHeaders);
    feeFineRepository = new FeeFineRepository(pgClient);
    accountRepository = new AccountRepository(pgClient);
    usersClient = new UsersClient(vertx, okapiHeaders);
  }

  public Future<JsonObject> createFeeFineLogContext(Feefineaction action) {
    return accountRepository.getAccountById(action.getAccountId())
      .compose(account -> createFeeFineLogContext(action, account));
  }

  public Future<JsonObject> createFeeFineLogContext(Feefineaction action, Account account) {
    return feeFineRepository.getById(account.getFeeFineId())
      .compose(feeFine -> usersClient.fetchUserById(account.getUserId())
        .compose(user -> buildFeeFineLogContext(action, account, feeFine, user)));
  }
}
