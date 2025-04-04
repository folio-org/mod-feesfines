package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;

import java.util.Map;
import java.util.Optional;

import javax.ws.rs.core.Response;

import org.folio.rest.annotations.Validate;
import org.folio.rest.domain.MonetaryValue;
import org.folio.rest.jaxrs.model.LostItemFeePolicy;
import org.folio.rest.jaxrs.model.LostItemFeesPoliciesGetOrder;
import org.folio.rest.jaxrs.model.LostItemFeePolicies;
import org.folio.rest.jaxrs.resource.LostItemFeesPolicies;
import org.folio.rest.persist.PgUtil;

import static io.vertx.core.Future.succeededFuture;
import static org.folio.rest.jaxrs.resource.LostItemFeesPolicies.PostLostItemFeesPoliciesResponse.respond422WithApplicationJson;
import static org.folio.rest.utils.ErrorHelper.createError;
import static org.folio.rest.utils.ErrorHelper.uniqueNameConstraintViolated;

public class LostItemFeePoliciesAPI implements LostItemFeesPolicies {

  static final String TABLE_NAME = "lost_item_fee_policy";
  static final String DUPLICATE_ERROR_CODE = "feesfines.policy.lost.duplicate";
  private static final String DUPLICATE_NAME_MSG =
    "The Lost item fee policy name entered already exists. Please enter a different name.";
  private static final String NEGATIVE_VALUE_MESSAGE = "The value must greater than or equal to 0";

  @Validate
  @Override
  public void getLostItemFeesPolicies(String query, String orderBy,
    LostItemFeesPoliciesGetOrder order, String totalRecords, int offset, int limit,
    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) {

    PgUtil.get(TABLE_NAME, LostItemFeePolicy.class, LostItemFeePolicies.class, query, offset, limit, okapiHeaders, vertxContext,
      GetLostItemFeesPoliciesResponse.class, asyncResultHandler);
  }

  @Validate
  @Override
  public void postLostItemFeesPolicies(LostItemFeePolicy entity, Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

    if (refuseWhenLostItemProcessingFeeIsNegative(entity, asyncResultHandler)) {
      return;
    }

    PgUtil.post(TABLE_NAME, entity, okapiHeaders, vertxContext,
      PostLostItemFeesPoliciesResponse.class, r -> {
        if (r.succeeded() && uniqueNameConstraintViolated(r.result(), TABLE_NAME)) {
          asyncResultHandler.handle(
            r.map(respond422WithApplicationJson(
              createError(DUPLICATE_NAME_MSG, DUPLICATE_ERROR_CODE))));
          return;
        }
        asyncResultHandler.handle(r);
      });
  }

  @Validate
  @Override
  public void getLostItemFeesPoliciesByLostItemFeePolicyId(String lostItemFeePolicyId,
    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) {

    PgUtil.getById(TABLE_NAME, LostItemFeePolicy.class, lostItemFeePolicyId, okapiHeaders, vertxContext,
      GetLostItemFeesPoliciesByLostItemFeePolicyIdResponse.class, asyncResultHandler);
  }

  @Validate
  @Override
  public void deleteLostItemFeesPoliciesByLostItemFeePolicyId(String lostItemFeePolicyId,
    Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
    Context vertxContext) {

    PgUtil.deleteById(TABLE_NAME, lostItemFeePolicyId, okapiHeaders, vertxContext,
      DeleteLostItemFeesPoliciesByLostItemFeePolicyIdResponse.class, asyncResultHandler);
  }

  @Validate
  @Override
  public void putLostItemFeesPoliciesByLostItemFeePolicyId(String lostItemFeePolicyId,
    LostItemFeePolicy entity, Map<String, String> okapiHeaders,
    Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {

    if (refuseWhenLostItemProcessingFeeIsNegative(entity, asyncResultHandler)) {
      return;
    }

    PgUtil.put(TABLE_NAME, entity, lostItemFeePolicyId, okapiHeaders, vertxContext,
      PutLostItemFeesPoliciesByLostItemFeePolicyIdResponse.class, asyncResultHandler);
  }

  private boolean refuseWhenLostItemProcessingFeeIsNegative(LostItemFeePolicy entity,
    Handler<AsyncResult<Response>> asyncResultHandler) {

    boolean lostItemProcessingFeeIsNegative = Optional.of(Optional.ofNullable(entity)
      .map(LostItemFeePolicy::getLostItemProcessingFee)
      .map(MonetaryValue::isNegative)
      .orElse(false))
      .get();

    if (lostItemProcessingFeeIsNegative) {
      asyncResultHandler.handle(
        succeededFuture(respond422WithApplicationJson(createError(
          NEGATIVE_VALUE_MESSAGE, "lostItemProcessingFee",
          entity.getLostItemProcessingFee().getAmount().toString()))));
      return true;
    }

    return false;
  }
}
