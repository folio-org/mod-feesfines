package org.folio.rest.service;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.jaxrs.model.Account;
import org.folio.rest.jaxrs.model.Feefineaction;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.repository.AccountRepository;
import org.folio.rest.repository.FeeFineActionRepository;
import org.folio.rest.tools.utils.TenantTool;

import io.vertx.core.Context;
import io.vertx.core.Future;

public class FeeFineChargeService {
  private static final Logger log = LogManager.getLogger(FeeFineChargeService.class);

  private final PostgresClient pgClient;
  private final AccountRepository accountRepository;
  private final FeeFineActionRepository feeFineActionRepository;
  private final AccountEventPublisher accountEventPublisher;

  public FeeFineChargeService(Context context, Map<String, String> headers) {
    this.pgClient = PostgresClient.getInstance(context.owner(), TenantTool.tenantId(headers));
    this.accountRepository = new AccountRepository(pgClient);
    this.feeFineActionRepository = new FeeFineActionRepository(pgClient);
    this.accountEventPublisher = new AccountEventPublisher(context, headers);
  }

  public Future<Void> chargeFeeFine(Account account, Feefineaction action) {
    final String userId = account.getUserId();
    final String feeFineType = account.getFeeFineType();

    log.info("Charging fee/fine \"{}\" for user {}", feeFineType, userId);

    return pgClient.withTrans(conn -> feeFineActionRepository.save(action, conn)
      .compose(ignored -> accountRepository.save(account, conn)))
      .onSuccess(accountEventPublisher::publishAccountBalanceChangeEvent)
      .onSuccess(v -> log.info(
        "Successfully charged fee/fine \"{}\" for user {}: accountId={}, feeFineActionId={}",
        feeFineType, userId, account.getId(), action.getId()))
      .onFailure(t -> log.error("Failed to charge fee/fine \"{}\" for user {}: {}",
        feeFineType, userId, t.getMessage()))
      .mapEmpty();
  }

}
