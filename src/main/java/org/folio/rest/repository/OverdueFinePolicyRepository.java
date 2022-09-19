package org.folio.rest.repository;

import java.util.Collection;
import java.util.Map;

import org.folio.rest.jaxrs.model.OverdueFinePolicy;

import io.vertx.core.Context;
import io.vertx.core.Future;

public class OverdueFinePolicyRepository extends AbstractRepository {
  private static final String TABLE_NAME = "overdue_fine_policy";

  public OverdueFinePolicyRepository(Context context, Map<String, String> headers) {
    super(headers, context);
  }

  public Future<OverdueFinePolicy> getOverdueFinePolicyById(String id) {
    return getById(TABLE_NAME, id, OverdueFinePolicy.class);
  }

  public Future<Collection<OverdueFinePolicy>> getOverdueFinePoliciesByIds(Collection<String> ids) {
    return getByIds(TABLE_NAME, ids, OverdueFinePolicy.class);
  }
}
