package org.folio.rest.impl;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.jaxrs.resource.OverdueFinesPolicies.PostOverdueFinesPoliciesResponse.respond422WithApplicationJson;
import static org.folio.rest.utils.ErrorHelper.createError;
import static org.folio.rest.utils.ErrorHelper.uniqueNameConstraintViolated;

import java.util.Map;
import java.util.Optional;

import javax.ws.rs.core.Response;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;

import org.folio.rest.annotations.Validate;
import org.folio.rest.domain.MonetaryValue;
import org.folio.rest.jaxrs.model.OverdueFine;
import org.folio.rest.jaxrs.model.OverdueFinePolicies;
import org.folio.rest.jaxrs.model.OverdueFinePolicy;
import org.folio.rest.jaxrs.model.OverdueFinesPoliciesGetOrder;
import org.folio.rest.jaxrs.resource.OverdueFinesPolicies;
import org.folio.rest.persist.PgUtil;

public class OverdueFinePoliciesAPI implements OverdueFinesPolicies {
  static final String TABLE_NAME = "overdue_fine_policy";
  static final String DUPLICATE_ERROR_CODE = "feesfines.policy.overdue.duplicate";
  private static final String DUPLICATE_NAME_MESSAGE =
    "The Overdue fine policy name entered already exists. Please enter a different name.";
  private static final String NEGATIVE_QUANTITY_MESSAGE =
    "The values overdueFine and overdueRecallFine must be greater than or equal to 0 appears.";

  @Validate
  @Override
  public void getOverdueFinesPolicies(String query,
    String orderBy,
    OverdueFinesPoliciesGetOrder order,
    int offset, int limit,
    String lang,
    Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) {

    PgUtil.get(TABLE_NAME, OverdueFinePolicy.class, OverdueFinePolicies.class, query, offset, limit, okapiHeaders, vertxContext,
      GetOverdueFinesPoliciesResponse.class, asyncResultHandler);
  }

  @Validate
  @Override
  public void postOverdueFinesPolicies(String lang,
    OverdueFinePolicy entity,
    Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) {

    if (containsNegativeQuantity(entity)) {
      asyncResultHandler.handle(
        succeededFuture(respond422WithApplicationJson(createError(NEGATIVE_QUANTITY_MESSAGE))));
      return;
    }

    PgUtil.post(TABLE_NAME, entity, okapiHeaders, vertxContext,
      PostOverdueFinesPoliciesResponse.class, r -> {
        if (r.succeeded() && uniqueNameConstraintViolated(r.result(), TABLE_NAME)) {
          asyncResultHandler.handle(
            r.map(respond422WithApplicationJson(
              createError(DUPLICATE_NAME_MESSAGE, DUPLICATE_ERROR_CODE))));
          return;
        }
        asyncResultHandler.handle(r);
      });
  }

  @Validate
  @Override
  public void getOverdueFinesPoliciesByOverdueFinePolicyId(String overdueFinePolicyId,
    String lang,
    Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) {

    PgUtil.getById(TABLE_NAME, OverdueFinePolicy.class, overdueFinePolicyId, okapiHeaders, vertxContext,
      GetOverdueFinesPoliciesByOverdueFinePolicyIdResponse.class, asyncResultHandler);
  }

  @Validate
  @Override
  public void deleteOverdueFinesPoliciesByOverdueFinePolicyId(String overdueFinePolicyId,
    String lang,
    Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) {

    PgUtil.deleteById(TABLE_NAME, overdueFinePolicyId, okapiHeaders, vertxContext,
      DeleteOverdueFinesPoliciesByOverdueFinePolicyIdResponse.class, asyncResultHandler);
  }

  @Validate
  @Override
  public void putOverdueFinesPoliciesByOverdueFinePolicyId(String overdueFinePolicyId,
    String lang,
    OverdueFinePolicy entity,
    Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) {

    if (containsNegativeQuantity(entity)) {
      asyncResultHandler.handle(
        succeededFuture(respond422WithApplicationJson(createError(NEGATIVE_QUANTITY_MESSAGE))));
      return;
    }

    PgUtil.put(TABLE_NAME, entity, overdueFinePolicyId, okapiHeaders, vertxContext,
      PutOverdueFinesPoliciesByOverdueFinePolicyIdResponse.class, asyncResultHandler);
  }

  private boolean containsNegativeQuantity(OverdueFinePolicy entity) {
    return hasNegativeQuantity(entity.getOverdueFine())
      || hasNegativeQuantity(entity.getOverdueRecallFine());
  }

  private boolean hasNegativeQuantity(OverdueFine overdueFine) {
    return Optional.ofNullable(overdueFine)
      .map(OverdueFine::getQuantity)
      .map(MonetaryValue::isNegative)
      .orElse(false);
  }
}
