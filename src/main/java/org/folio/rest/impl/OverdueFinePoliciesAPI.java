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

import org.apache.commons.lang3.tuple.ImmutablePair;
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
  private static final String NEGATIVE_QUANTITY_MESSAGE = "Quantity must not be negative.";

  @Validate
  @Override
  public void getOverdueFinesPolicies(String query, String orderBy, OverdueFinesPoliciesGetOrder order,
    String totalRecords, int offset, int limit, Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

    PgUtil.get(TABLE_NAME, OverdueFinePolicy.class, OverdueFinePolicies.class, query, offset, limit, okapiHeaders, vertxContext,
      GetOverdueFinesPoliciesResponse.class, asyncResultHandler);
  }

  @Validate
  @Override
  public void postOverdueFinesPolicies(OverdueFinePolicy entity, Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

    if (containsNegativeQuantity(entity)) {
      ImmutablePair<String, String> errorInformation = getErrorInformation(entity);
      asyncResultHandler.handle(
        succeededFuture(respond422WithApplicationJson(createError(
          NEGATIVE_QUANTITY_MESSAGE, errorInformation.getLeft(), errorInformation.getRight()))));
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
    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) {

    PgUtil.getById(TABLE_NAME, OverdueFinePolicy.class, overdueFinePolicyId, okapiHeaders, vertxContext,
      GetOverdueFinesPoliciesByOverdueFinePolicyIdResponse.class, asyncResultHandler);
  }

  @Validate
  @Override
  public void deleteOverdueFinesPoliciesByOverdueFinePolicyId(String overdueFinePolicyId,
    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) {

    PgUtil.deleteById(TABLE_NAME, overdueFinePolicyId, okapiHeaders, vertxContext,
      DeleteOverdueFinesPoliciesByOverdueFinePolicyIdResponse.class, asyncResultHandler);
  }

  @Validate
  @Override
  public void putOverdueFinesPoliciesByOverdueFinePolicyId(String overdueFinePolicyId,
    OverdueFinePolicy entity, Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

    if (containsNegativeQuantity(entity)) {
      ImmutablePair<String, String> errorInformation = getErrorInformation(entity);
      asyncResultHandler.handle(
        succeededFuture(respond422WithApplicationJson(createError(
          NEGATIVE_QUANTITY_MESSAGE, errorInformation.getLeft(), errorInformation.getRight()))));
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

  private ImmutablePair<String, String> getErrorInformation(OverdueFinePolicy entity) {
    return hasNegativeQuantity(entity.getOverdueFine())
      ? new ImmutablePair<>("overdueFine.quantity",
          getQuantityValueAsString(entity.getOverdueFine()))
      : new ImmutablePair<>("overdueRecallFine.quantity",
          getQuantityValueAsString(entity.getOverdueRecallFine()));
  }

  private String getQuantityValueAsString(OverdueFine overdueFine) {
    return overdueFine.getQuantity().toString();
  }
}
