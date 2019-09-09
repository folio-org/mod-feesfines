package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import java.util.Map;
import javax.ws.rs.core.Response;
import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.LostItemFeePolicy;
import org.folio.rest.jaxrs.model.LostItemFeesPoliciesGetOrder;
import org.folio.rest.jaxrs.model.LostItemFeePolicies;
import org.folio.rest.jaxrs.resource.LostItemFeesPolicies;
import org.folio.rest.persist.PgUtil;

public class LostItemFeePoliciesAPI implements LostItemFeesPolicies {

    public static final String LOST_ITEM_FEE_TABLE = "lost_item_fee_policy";
    private static final Class<LostItemFeePolicy> LOST_ITEM_FEE_POLICY = LostItemFeePolicy.class;

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
                PostLostItemFeesPoliciesResponse.class, asyncResultHandler);
    }

    @Validate
    @Override
    public void getLostItemFeesPoliciesByLostItemFeePolicyId(String lostItemFeePolicyId,
            String lang,
            Map<String, String> okapiHeaders,
            Handler<AsyncResult<Response>> asyncResultHandler,
            Context vertxContext) {

        PgUtil.getById(LOST_ITEM_FEE_TABLE, LOST_ITEM_FEE_POLICY, LostItemFeePolicyId, okapiHeaders, vertxContext,
                GetLostItemFeesPoliciesByLostItemFeePolicyIdResponse.class, asyncResultHandler);
    }

    @Validate
    @Override
    public void deleteLostItemFeesPoliciesByLostItemFeePolicyId(String lostItemFeePolicyId,
            String lang,
            Map<String, String> okapiHeaders,
            Handler<AsyncResult<Response>> asyncResultHandler,
            Context vertxContext) {

        PgUtil.deleteById(LOST_ITEM_FEE_TABLE, LostItemFeePolicyId, okapiHeaders, vertxContext,
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

        PgUtil.put(LOST_ITEM_FEE_TABLE, entity, LostItemFeePolicyId, okapiHeaders, vertxContext,
                PutLostItemFeesPoliciesByLostItemFeePolicyIdResponse.class, asyncResultHandler);
    }
}
