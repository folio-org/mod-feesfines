package org.folio.rest.repository;

import static io.vertx.core.Future.failedFuture;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.folio.cql2pgjson.CQL2PgJSON;
import org.folio.cql2pgjson.exception.FieldException;
import org.folio.rest.domain.Action;
import org.folio.rest.jaxrs.model.Feefineaction;
import org.folio.rest.persist.Criteria.Criteria;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.Criteria.GroupedCriterias;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.persist.cql.CQLWrapper;
import org.folio.rest.persist.interfaces.Results;
import org.folio.rest.tools.utils.TenantTool;
import org.folio.rest.utils.FeeFineActionHelper;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowIterator;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

public class FeeFineActionRepository {
  private static final String ACTIONS_TABLE = "feefineactions";
  private static final String ACCOUNTS_TABLE = "accounts";
  private static final String DATE_FIELD = "dateAction";
  private static final String TYPE_FIELD = "typeAction";
  private static final int ACTIONS_LIMIT = 1000;
  private static final String FIND_REFUNDABLE_ACTIONS_QUERY_TEMPLATE =
    "typeAction any \"Paid Transferred\" AND accountId any \"%s\"";

  private final PostgresClient pgClient;
  private final String tenantId;

  public FeeFineActionRepository(Map<String, String> headers, Context context) {
    pgClient = PostgresClient.getInstance(context.owner(), TenantTool.tenantId(headers));
    tenantId = TenantTool.tenantId(headers);
  }

  public Future<List<Feefineaction>> get(Criterion criterion) {
    Promise<Results<Feefineaction>> promise = Promise.promise();
    pgClient.get(ACTIONS_TABLE, Feefineaction.class, criterion, true, promise);
    return promise.future().map(Results::getResults);
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

  public Future<List<Feefineaction>> findActionsOfTypesForAccount(String accountId,
    List<Action> types) {

    if (accountId == null) {
      return failedFuture(new IllegalArgumentException("Account ID is null"));
    }

    if (types == null || types.isEmpty()) {
      return failedFuture(new IllegalArgumentException("Types list is empty"));
    }

    GroupedCriterias typeCriterias = groupCriterias(getTypeCriterias(types), "OR");

    Criterion criterion = new Criterion(new Criteria()
      .addField("'accountId'")
      .setOperation("=")
      .setVal(accountId))
      .addGroupOfCriterias(typeCriterias);

    Promise<Results<Feefineaction>> promise = Promise.promise();
    pgClient.get(ACTIONS_TABLE, Feefineaction.class, criterion, false, promise);

    return promise.future()
      .map(Results::getResults);
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

    String query = format(FIND_REFUNDABLE_ACTIONS_QUERY_TEMPLATE,
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

  public Future<List<Feefineaction>> findActionsByTypeForPeriodAndOwners(Action typeAction,
    String startDate, String endDate, List<String> ownerIds, int limit) {

    String ownerIdsFilter = ownerIds != null && !ownerIds.isEmpty()
      ? String.format("AND accounts.jsonb->>'ownerId' IN (%s)",
      ownerIds.stream()
        .map(id -> format("'%s'", id))
        .collect(Collectors.joining(",")))
      : EMPTY;
    String typeActions = List.of(
      typeAction.getFullResult(), typeAction.getPartialResult()).stream()
      .map(result -> format("'%s'", result))
      .collect(Collectors.joining(","));

    String query = format(
      "SELECT actions.jsonb FROM %1$s.%2$s actions " +
      "LEFT OUTER JOIN %1$s.%3$s accounts ON actions.jsonb->> 'accountId' = accounts.jsonb->>'id' " +
      "WHERE actions.jsonb->>'dateAction' >= $1 " +
      "AND actions.jsonb->>'dateAction' < $2 " +
      "AND actions.jsonb->>'typeAction' IN (%4$s) " +
        ownerIdsFilter +
      "ORDER BY actions.jsonb->>'dateAction' ASC " +
      "LIMIT $3",
      PostgresClient.convertToPsqlStandard(tenantId),
      ACTIONS_TABLE,
      ACCOUNTS_TABLE,
      typeActions);

    Tuple params = Tuple.of(startDate, endDate, limit);
    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient.select(query, params, promise);

    return promise.future().map(this::mapToFeeFineActions);
  }

  public Future<Feefineaction> save(Feefineaction feefineaction) {
    Promise<String> promise = Promise.promise();
    pgClient.save(ACTIONS_TABLE, feefineaction.getId(), feefineaction, promise);

    return promise.future().map(feefineaction);
  }

  private List<Criteria> getTypeCriterias(List<Action> actions) {
    return actions.stream()
      .map(action -> List.of(
        new Criteria()
          .addField(format("'%s'", TYPE_FIELD))
          .setOperation("=")
          .setVal(action.getFullResult())
          .setJSONB(true),
        new Criteria()
          .addField(format("'%s'", TYPE_FIELD))
          .setOperation("=")
          .setVal(action.getPartialResult())
          .setJSONB(true)))
      .flatMap(Collection::stream)
      .collect(toList());
  }

  private List<Feefineaction> mapToFeeFineActions(RowSet<Row> rowSet) {
    RowIterator<Row> iterator = rowSet.iterator();
    List<Feefineaction> feeFineActions = new ArrayList<>();
    iterator.forEachRemaining(row -> feeFineActions.add(
      row.get(JsonObject.class, 0).mapTo(Feefineaction.class)));

    return feeFineActions;
  }

  private GroupedCriterias groupCriterias(List<Criteria> criterias, String op) {
    GroupedCriterias groupedCriterias = new GroupedCriterias();
    criterias.forEach(criteria -> groupedCriterias.addCriteria(criteria, op));
    return groupedCriterias;
  }
}
