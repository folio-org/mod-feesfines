package org.folio.rest.repository;

import static io.vertx.core.Future.succeededFuture;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.rest.persist.Conn;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.TenantTool;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;

public abstract class AbstractRepository {
  private static final Logger log = LogManager.getLogger(AbstractRepository.class);

  protected final PostgresClient pgClient;

  AbstractRepository(Map<String, String> headers, Context context) {
    pgClient = PostgresClient.getInstance(context.owner(), TenantTool.tenantId(headers));
  }

  AbstractRepository(PostgresClient pgClient) {
    this.pgClient = pgClient;
  }

  <T> Future<T> getById(String tableName, String id, Class<T> objectType) {
    return pgClient.getById(tableName, id, objectType);
  }

  <T> Future<Collection<T>> getByQuery(String query, Class<T> objectType) {
    return pgClient.select(query)
      .map(rowSet -> mapRowSet(rowSet, objectType));
  }

  <T> Future<Collection<T>> getByIds(String tableName, Collection<String> ids, Class<T> objectType) {
    log.info("Fetching {} {} by ID", ids.size(), objectType.getSimpleName());
    return getByKeyValues(tableName, "id", ids, objectType);
  }

  <T> Future<Collection<T>> getByKeyValues(String tableName, String key, Collection<String> values,
    Class<T> objectType) {

    Set<String> filteredValues = values.stream()
      .filter(StringUtils::isNotBlank)
      .collect(toSet());

    if (filteredValues.isEmpty()) {
      return succeededFuture(new ArrayList<>());
    }

    String joinedValues = filteredValues.stream()
      .map(value -> StringUtils.wrap(value, "'"))
      .collect(Collectors.joining(","));

    String query = format(
      "SELECT t.jsonb FROM %s.%s t WHERE left(lower(f_unaccent(jsonb->>'%s')), 600) IN (%s)",
      getSchemaName(), tableName, key, joinedValues);

    return getByQuery(query, objectType);
  }

  private static <T> Collection<T> mapRowSet(RowSet<Row> rowSet, Class<T> recordType) {
    return StreamSupport.stream(rowSet.spliterator(), false)
      .map(row -> row.get(JsonObject.class, row.getColumnIndex("jsonb")))
      .map(json -> json.mapTo(recordType))
      .collect(toList());
  }

  String getSchemaName() {
    return pgClient.getSchemaName();
  }

  String getTenantId() {
    return pgClient.getTenantId();
  }

  public <T> Future<T> save(String tableName, String id, T object) {
    return pgClient.save(tableName, id, object)
      .map(object);
  }

  public <T> Future<T> save(String tableName, String id, T object, Conn conn) {
    return conn.save(tableName, id, object)
      .map(object);
  }
}
