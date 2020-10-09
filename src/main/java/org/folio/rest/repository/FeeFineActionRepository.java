package org.folio.rest.repository;

import static io.vertx.core.Future.failedFuture;
import static java.util.Collections.singleton;
import static org.apache.commons.lang3.StringUtils.SPACE;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.folio.cql2pgjson.CQL2PgJSON;
import org.folio.cql2pgjson.exception.FieldException;
import org.folio.rest.jaxrs.model.Feefineaction;
import org.folio.rest.persist.Criteria.Criteria;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.persist.cql.CQLWrapper;
import org.folio.rest.persist.interfaces.Results;
import org.folio.rest.tools.utils.TenantTool;
import org.folio.rest.utils.FeeFineActionHelper;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Promise;

public class FeeFineActionRepository {
  private static final String ACTIONS_TABLE = "feefineactions";
  private static final int ACTIONS_LIMIT = 1000;
  private static final String FIND_REFUNDABLE_ACTIONS_QUERY_TEMPLATE =
    "typeAction any \"Paid Transferred\" AND accountId any \"%s\"";

  private final PostgresClient pgClient;

  public FeeFineActionRepository(PostgresClient pgClient) {
    this.pgClient = pgClient;
  }

  public FeeFineActionRepository(Map<String, String> headers, Context context) {
    pgClient = PostgresClient.getInstance(context.owner(), TenantTool.tenantId(headers));
  }

  public Future<List<Feefineaction>> findActionsForAccount(String accountId) {
    if (accountId == null) {
      return failedFuture(new IllegalArgumentException("Account ID is null"));
    }

    Criterion criterion = new Criterion(new Criteria()
      .addField("'accountId'")
      .setOperation("=")
      .setVal(accountId));

    Promise<Results<Feefineaction>> promise = Promise.promise();
    pgClient.get(ACTIONS_TABLE, Feefineaction.class, criterion, false, promise);

    return promise.future()
      .map(Results::getResults);
  }

  public Future<List<Feefineaction>> findRefundableActionsForAccount(String accountId) {
    return findRefundableActionsForAccounts(singleton(accountId));
  }

  public Future<List<Feefineaction>> findRefundableActionsForAccounts(Collection<String> accountIds) {
    if (accountIds == null || accountIds.isEmpty()) {
      return failedFuture(new IllegalArgumentException("List of account IDs is empty or null"));
    }

    CQL2PgJSON cql2pgJson;
    try {
      cql2pgJson = new CQL2PgJSON(ACTIONS_TABLE + ".jsonb");
    } catch (FieldException e) {
      return failedFuture(e);
    }

    String query = String.format(FIND_REFUNDABLE_ACTIONS_QUERY_TEMPLATE,
      String.join(SPACE, accountIds));

    CQLWrapper cqlWrapper = new CQLWrapper(cql2pgJson, query, ACTIONS_LIMIT, 0);
    Promise<Results<Feefineaction>> promise = Promise.promise();
    pgClient.get(ACTIONS_TABLE, Feefineaction.class, cqlWrapper, false, promise);

    return promise.future().map(Results::getResults);
  }

  public Future<Feefineaction> findChargeForAccount(String accountId) {
    return findActionsForAccount(accountId)
      .map(actions -> actions.stream()
        .filter(FeeFineActionHelper::isCharge)
        .findAny()
        .orElse(null)
      );
  }

  public Future<Feefineaction> save(Feefineaction feefineaction) {
    Promise<String> promise = Promise.promise();
    pgClient.save(ACTIONS_TABLE, feefineaction.getId(), feefineaction, promise);

    return promise.future().map(feefineaction);
  }
}
