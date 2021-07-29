package org.folio.rest.repository;

import java.util.Map;

import org.folio.rest.jaxrs.model.OverdueFinePolicy;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.TenantTool;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Promise;

public class OverdueFinePolicyRepository {
  private static final String TABLE_NAME = "overdue_fine_policy";

  private final PostgresClient pgClient;

  public OverdueFinePolicyRepository(PostgresClient pgClient) {
    this.pgClient = pgClient;
  }

  public OverdueFinePolicyRepository(Context context, Map<String, String> headers) {
    this(PostgresClient.getInstance(context.owner(), TenantTool.tenantId(headers)));
  }

  public Future<OverdueFinePolicy> getOverdueFinePolicyById(String id) {
    Promise<OverdueFinePolicy> promise = Promise.promise();
    pgClient.getById(TABLE_NAME, id, OverdueFinePolicy.class, promise);
    return promise.future();
  }
}
