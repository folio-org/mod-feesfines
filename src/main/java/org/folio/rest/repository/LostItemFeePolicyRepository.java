package org.folio.rest.repository;

import java.util.Collection;
import java.util.Map;

import org.folio.rest.jaxrs.model.LostItemFeePolicy;

import io.vertx.core.Context;
import io.vertx.core.Future;

public class LostItemFeePolicyRepository extends AbstractRepository {
  private static final String TABLE_NAME = "lost_item_fee_policy";

  public LostItemFeePolicyRepository(Context context, Map<String, String> headers) {
    super(headers, context);
  }

  public Future<LostItemFeePolicy> getLostItemFeePolicyById(String id) {
    return getById(TABLE_NAME, id, LostItemFeePolicy.class);
  }

  public Future<Collection<LostItemFeePolicy>> getLostItemFeePoliciesByIds(Collection<String> ids) {
    return getByIds(TABLE_NAME, ids, LostItemFeePolicy.class);
  }
}
