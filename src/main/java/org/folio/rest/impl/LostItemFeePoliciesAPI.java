package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import java.util.Map;
import javax.ws.rs.core.Response;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.Error;
import org.folio.rest.jaxrs.model.LostItemFeePolicy;
import org.folio.rest.jaxrs.model.LostItemFeesPoliciesGetOrder;
import org.folio.rest.jaxrs.model.LostItemFeePolicies;
import org.folio.rest.jaxrs.resource.LostItemFeesPolicies;
import org.folio.rest.persist.PgUtil;
import org.folio.rest.utils.ErrorHelper;

public class LostItemFeePoliciesAPI implements LostItemFeesPolicies {

    public static final String LOST_ITEM_FEE_TABLE = "lost_item_fee_policy";
    private static final Class<LostItemFeePolicy> LOST_ITEM_FEE_POLICY = LostItemFeePolicy.class;
    private static final String PRIMARY_KEY = "lost_item_fee_policy_pkey";
    static final String DUPLICATE_ERROR_CODE = "feesfines.policy.lost.duplicate";
    private static final String DUPLICATE_ENTITY_MESSAGE =
            "A lost item fee policy with this name already exists. Please choose a different name.";

    @Validate
    @Override
    public void getLostItemFeesPolicies(String query,
            String orderBy,
            LostItemFeesPoliciesGetOrder order,
            int offset, int limit,
            String lang,
            Map<String, String> okapiHeaders,
            Handler<AsyncResult<Response>> asyncResultHandler,
            Context vertxContext) {

        PgUtil.get(LOST_ITEM_FEE_TABLE, LOST_ITEM_FEE_POLICY, LostItemFeePolicies.class, query, offset, limit, okapiHeaders, vertxContext,
                GetLostItemFeesPoliciesResponse.class, asyncResultHandler);
    }

    @Validate
    @Override
    public void postLostItemFeesPolicies(String lang,
            LostItemFeePolicy entity,
            Map<String, String> okapiHeaders,
            Handler<AsyncResult<Response>> asyncResultHandler,
            Context vertxContext) {

        PgUtil.post(LOST_ITEM_FEE_TABLE, entity, okapiHeaders, vertxContext,
                PostLostItemFeesPoliciesResponse.class, r -> {
                    if (ErrorHelper.didUniqueConstraintViolationOccur(r.result(), PRIMARY_KEY)) {
                        Error error = new Error()
                                .withMessage(DUPLICATE_ENTITY_MESSAGE)
                                .withCode(DUPLICATE_ERROR_CODE);
                        asyncResultHandler.handle(
                                r.map(PostLostItemFeesPoliciesResponse.respond422WithApplicationJson(
                                  org.folio.rest.utils.ErrorHelper.createErrors(error))));
                        return;
                    }
                    asyncResultHandler.handle(r);
                });
    }

    @Validate
    @Override
    public void getLostItemFeesPoliciesByLostItemFeePolicyId(String lostItemFeePolicyId,
            String lang,
            Map<String, String> okapiHeaders,
            Handler<AsyncResult<Response>> asyncResultHandler,
            Context vertxContext) {

        PgUtil.getById(LOST_ITEM_FEE_TABLE, LOST_ITEM_FEE_POLICY, lostItemFeePolicyId, okapiHeaders, vertxContext,
                GetLostItemFeesPoliciesByLostItemFeePolicyIdResponse.class, asyncResultHandler);
    }

    @Validate
    @Override
    public void deleteLostItemFeesPoliciesByLostItemFeePolicyId(String lostItemFeePolicyId,
            String lang,
            Map<String, String> okapiHeaders,
            Handler<AsyncResult<Response>> asyncResultHandler,
            Context vertxContext) {

        PgUtil.deleteById(LOST_ITEM_FEE_TABLE, lostItemFeePolicyId, okapiHeaders, vertxContext,
                DeleteLostItemFeesPoliciesByLostItemFeePolicyIdResponse.class, asyncResultHandler);
    }

    @Validate
    @Override
    public void putLostItemFeesPoliciesByLostItemFeePolicyId(String lostItemFeePolicyId,
            String lang,
            LostItemFeePolicy entity,
            Map<String, String> okapiHeaders,
            Handler<AsyncResult<Response>> asyncResultHandler,
            Context vertxContext) {

        PgUtil.put(LOST_ITEM_FEE_TABLE, entity, lostItemFeePolicyId, okapiHeaders, vertxContext,
                PutLostItemFeesPoliciesByLostItemFeePolicyIdResponse.class, asyncResultHandler);
    }
}
