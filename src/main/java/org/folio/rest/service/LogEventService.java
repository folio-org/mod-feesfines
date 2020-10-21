package org.folio.rest.service;

import static org.folio.rest.domain.logs.LogEventPayloadHelper.buildFeeFineLogEventPayload;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.Feefineaction;
import org.folio.rest.persist.PgUtil;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.repository.AccountRepository;
import org.folio.rest.repository.FeeFineRepository;

import java.util.Map;

public class LogEventService {
  private final FeeFineRepository feeFineRepository;
  private final AccountRepository accountRepository;

  public LogEventService(Vertx vertx, Map<String, String> okapiHeaders) {
    PostgresClient pgClient = PgUtil.postgresClient(vertx.getOrCreateContext(), okapiHeaders);
    feeFineRepository = new FeeFineRepository(pgClient);
    accountRepository = new AccountRepository(pgClient);
  }

  public Future<JsonObject> createFeeFineLogEventPayload(Feefineaction action) {
    return accountRepository.getAccountById(action.getAccountId())
      .compose(account -> createFeeFineLogEventPayload(action, account));
  }

  public Future<JsonObject> createFeeFineLogEventPayload(Feefineaction action, Account account) {
    return feeFineRepository.getById(account.getFeeFineId())
      .compose(feeFine -> buildFeeFineLogEventPayload(action, account, feeFine));
  }
}
