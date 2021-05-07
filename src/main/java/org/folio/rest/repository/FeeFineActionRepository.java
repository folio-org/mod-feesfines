package org.folio.rest.repository;

import static io.vertx.core.Future.failedFuture;
import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.SPACE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.folio.cql2pgjson.CQL2PgJSON;
import org.folio.cql2pgjson.exception.FieldException;
import org.folio.rest.domain.Action;
import org.folio.rest.jaxrs.model.Account;
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
  public static final String ACTIONS_TABLE_ALIAS = "actions";
  private static final String ACCOUNTS_TABLE_ALIAS = "accounts";
  private static final String DATE_FIELD = "dateAction";
  private static final String TYPE_FIELD = "typeAction";
  private static final String CREATED_AT_FIELD = "createdAt";
  private static final String SOURCE_FIELD = "source";
  private static final String OWNER_ID_FIELD = "ownerId";
  private static final int ACTIONS_LIMIT = 1000;
  private static final String FIND_REFUNDABLE_ACTIONS_QUERY_TEMPLATE =
    "typeAction any \"Paid Transferred\" AND accountId any \"%s\"";
  public static final String ORDER_BY_ACTION_DATE_ASC = "actions.jsonb->>'dateAction' ASC";
  public static final String ORDER_BY_OWNER_SOURCE_DATE_ASC = "accounts.jsonb->>'feeFineOwner', " +
    "actions.jsonb->>'source' ASC, actions.jsonb->>'dateAction' ASC";

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
      join(SPACE, accountIds));

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

  public Future<List<Feefineaction>> find(
    Action typeAction, String startDate, String endDate, List<String> ownerIds, int limit) {

    return findFeeFineActionsAndAccounts(typeAction, startDate, endDate, ownerIds,
      null, null, ORDER_BY_ACTION_DATE_ASC, limit)
      .map(Map::keySet)
      .map(ArrayList::new);
  }

  public Future<Map<Feefineaction, Account>> findFeeFineActionsAndAccounts(
    Action typeAction, String startDate, String endDate, List<String> ownerIds, String createdAt,
    List<String> sources, String orderBy, int limit) {

    Tuple params = Tuple.of(limit);
    List<String> conditions = new ArrayList<>();

    params.addString(typeAction.getFullResult());
    params.addString(typeAction.getPartialResult());
    conditions.add(format("%s.jsonb->>'%s' IN ($2, $3)", ACTIONS_TABLE_ALIAS, TYPE_FIELD));

    if (startDate != null) {
      params.addString(startDate);
      conditions.add(format("%s.jsonb->>'%s' >= $%d", ACTIONS_TABLE_ALIAS, DATE_FIELD,
        params.size()));
    }
    if (endDate != null) {
      params.addString(endDate);
      conditions.add(format("%s.jsonb->>'%s' < $%d", ACTIONS_TABLE_ALIAS, DATE_FIELD,
        params.size()));
    }
    if (createdAt != null) {
      params.addString(createdAt);
      conditions.add(format("%s.jsonb->>'%s' = $%d", ACTIONS_TABLE_ALIAS, CREATED_AT_FIELD,
        params.size()));
    }

    addFilterByListToConditions(conditions, ACCOUNTS_TABLE_ALIAS, OWNER_ID_FIELD, ownerIds);
    addFilterByListToConditions(conditions, ACTIONS_TABLE_ALIAS, SOURCE_FIELD, sources);

    String query = format(
      "SELECT actions.jsonb, accounts.jsonb FROM %1$s.%2$s %3$s " +
        "LEFT OUTER JOIN %1$s.%4$s %5$s ON %3$s.jsonb->>'accountId' = %5$s.jsonb->>'id' " +
        "WHERE " + join(" AND ", conditions) + " " +
        "ORDER BY %6$s " +
        "LIMIT $1",
      PostgresClient.convertToPsqlStandard(tenantId),
      ACTIONS_TABLE, ACTIONS_TABLE_ALIAS,
      ACCOUNTS_TABLE, ACCOUNTS_TABLE_ALIAS,
      orderBy);

    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient.select(query, params, promise);

    return promise.future().map(this::mapToFeeFineActionsAndAccounts);
  }

  private void addFilterByListToConditions(List<String> conditions, String tableName,
    String fieldName, List<String> valueList) {

    if (valueList == null || valueList.isEmpty()) {
      return;
    }

    conditions.add(format("%s.jsonb->>'%s' IN (%s)", tableName, fieldName,
      valueList.stream()
        .map(value -> format("'%s'", value))
        .collect(Collectors.joining(", "))));
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

  private Map<Feefineaction, Account> mapToFeeFineActionsAndAccounts(RowSet<Row> rowSet) {
    RowIterator<Row> iterator = rowSet.iterator();
    Map<Feefineaction, Account> feeFineActionsToAccountsMap = new LinkedHashMap<>();
    iterator.forEachRemaining(row -> {
      JsonObject actionJsonObject = row.get(JsonObject.class, 0);
      JsonObject accountJsonObject = row.get(JsonObject.class, 1);
      feeFineActionsToAccountsMap.put(
        actionJsonObject != null ? actionJsonObject.mapTo(Feefineaction.class) : null,
        accountJsonObject != null ? accountJsonObject.mapTo(Account.class) : null);
    });

    return feeFineActionsToAccountsMap;
  }

  private GroupedCriterias groupCriterias(List<Criteria> criterias, String op) {
    GroupedCriterias groupedCriterias = new GroupedCriterias();
    criterias.forEach(criteria -> groupedCriterias.addCriteria(criteria, op));
    return groupedCriterias;
  }
}
