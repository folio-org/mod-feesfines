package org.folio.rest.repository;

import java.util.Map;

import org.folio.rest.jaxrs.model.LostItemFeePolicy;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.TenantTool;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Promise;

public class LostItemFeePolicyRepository {
  private static final String TABLE_NAME = "lost_item_fee_policy";

  private final PostgresClient pgClient;

  public LostItemFeePolicyRepository(Context context, Map<String, String> headers) {
    this(PostgresClient.getInstance(context.owner(), TenantTool.tenantId(headers)));
  }

  public LostItemFeePolicyRepository(PostgresClient pgClient) {
    this.pgClient = pgClient;
  }

  public Future<LostItemFeePolicy> getLostItemFeePolicyById(String id) {
    Promise<LostItemFeePolicy> promise = Promise.promise();
    pgClient.getById(TABLE_NAME, id, LostItemFeePolicy.class, promise);
    return promise.future();
  }
}
