package org.folio.rest.repository;

import static io.vertx.core.Future.failedFuture;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.rest.domain.Action.PAY;
import static org.folio.rest.domain.Action.TRANSFER;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.folio.rest.domain.Action;
import org.folio.rest.jaxrs.model.Feefineaction;
import org.folio.rest.persist.Criteria.Criteria;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.Criteria.GroupedCriterias;
import org.folio.rest.persist.Criteria.Limit;
import org.folio.rest.persist.PostgresClient;
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
  private static final String ACCOUNT_ID_FIELD = "accountId";
  private static final int ACTIONS_LIMIT = 1000;

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

    GroupedCriterias typeCriterias = buildGroupedCriterias(getTypeCriterias(types), "OR");

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

    GroupedCriterias accountIdsCriterias = buildGroupedCriterias(
      buildEqualsCriteriaList(ACCOUNT_ID_FIELD, accountIds), "OR");

    GroupedCriterias typeCriterias = buildGroupedCriterias(
      buildEqualsCriteriaList(TYPE_FIELD, List.of(PAY.getPartialResult(), PAY.getFullResult(),
        TRANSFER.getPartialResult(), TRANSFER.getFullResult())), "OR");

    Criterion criterion = new Criterion()
      .addGroupOfCriterias(typeCriterias)
      .addGroupOfCriterias(accountIdsCriterias)
      .setLimit(new Limit(ACTIONS_LIMIT));

    Promise<Results<Feefineaction>> promise = Promise.promise();
    pgClient.get(ACTIONS_TABLE, Feefineaction.class, criterion, false, promise);

    return promise.future()
      .map(Results::getResults);
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

    final String typeCondition = "actions.jsonb->>'typeAction' IN ($2, $3)";
    final String startDateCondition = "actions.jsonb->>'dateAction' >= $4";
    final String endDateCondition = "actions.jsonb->>'dateAction' < $5";

    String ownerIdsFilter = buildOwnerIdsFilter(ownerIds);
    Tuple params = Tuple.of(limit, typeAction.getFullResult(), typeAction.getPartialResult());
    List<String> whereConditions = new ArrayList<>();
    whereConditions.add(typeCondition);

    if (startDate != null && endDate != null) {
      whereConditions.add(startDateCondition);
      whereConditions.add(endDateCondition);
      params.addString(startDate);
      params.addString(endDate);
    } else if (startDate != null && endDate == null) {
      whereConditions.add(startDateCondition);
      params.addString(startDate);
    }

    String query = format(
      "SELECT actions.jsonb FROM %1$s.%2$s actions " +
        "LEFT OUTER JOIN %1$s.%3$s accounts ON actions.jsonb->>'accountId' = accounts.jsonb->>'id' " +
        "WHERE " +
        String.join(" AND ", whereConditions) +
        "%4$s" +
        "ORDER BY actions.jsonb->>'dateAction' ASC " +
        "LIMIT $1",
      PostgresClient.convertToPsqlStandard(tenantId),
      ACTIONS_TABLE,
      ACCOUNTS_TABLE,
      ownerIdsFilter);

    Promise<RowSet<Row>> promise = Promise.promise();
    pgClient.select(query, params, promise);

    return promise.future().map(this::mapToFeeFineActions);
  }

  private String buildOwnerIdsFilter(List<String> ownerIds) {
    if (ownerIds == null || ownerIds.isEmpty()) {
      return EMPTY;
    }

    return format("AND accounts.jsonb->>'ownerId' IN (%s) ",
      ownerIds.stream()
        .map(id -> format("'%s'", id))
        .collect(Collectors.joining(", ")));
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

  private List<Criteria> buildEqualsCriteriaList(String fieldName, Collection<String> values) {
    return values.stream()
      .map(value ->
        new Criteria()
          .addField(format("'%s'", fieldName))
          .setOperation("=")
          .setVal(value)
          .setJSONB(true))
      .collect(toList());
  }

  private List<Feefineaction> mapToFeeFineActions(RowSet<Row> rowSet) {
    RowIterator<Row> iterator = rowSet.iterator();
    List<Feefineaction> feeFineActions = new ArrayList<>();
    iterator.forEachRemaining(row -> feeFineActions.add(
      row.get(JsonObject.class, 0).mapTo(Feefineaction.class)));

    return feeFineActions;
  }

  private GroupedCriterias buildGroupedCriterias(List<Criteria> criterias, String op) {
    GroupedCriterias groupedCriterias = new GroupedCriterias();
    criterias.forEach(criteria -> groupedCriterias.addCriteria(criteria, op));
    return groupedCriterias;
  }
}
