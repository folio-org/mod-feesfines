package org.folio.rest.impl;

import static org.folio.rest.utils.ErrorHelper.createErrors;

import java.util.Map;

import javax.ws.rs.core.Response;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;

import org.folio.rest.annotations.Validate;
import org.folio.rest.jaxrs.model.Error;
import org.folio.rest.jaxrs.model.OverdueFinePolicies;
import org.folio.rest.jaxrs.model.OverdueFinePolicy;
import org.folio.rest.jaxrs.model.OverdueFinesPoliciesGetOrder;
import org.folio.rest.jaxrs.resource.OverdueFinesPolicies;
import org.folio.rest.persist.PgUtil;
import org.folio.rest.utils.ErrorHelper;

public class OverdueFinePoliciesAPI implements OverdueFinesPolicies {

    static final String OVERDUE_FINE_POLICY_TABLE = "overdue_fine_policy";
    static final String DUPLICATE_ERROR_CODE = "feesfines.policy.overdue.duplicate";
    private static final String PRIMARY_KEY = "overdue_fine_policy_pkey";
    private static final Class<OverdueFinePolicy> OVERDUE_FINE_POLICY = OverdueFinePolicy.class;
    private static final String DUPLICATE_ENTITY_MESSAGE =
            "An overdue fine policy with this name already exists. Please choose a different name.";

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

        PgUtil.get(OVERDUE_FINE_POLICY_TABLE, OVERDUE_FINE_POLICY, OverdueFinePolicies.class, query, offset, limit, okapiHeaders, vertxContext,
                GetOverdueFinesPoliciesResponse.class, asyncResultHandler);
    }

    @Validate
    @Override
    public void postOverdueFinesPolicies(String lang,
            OverdueFinePolicy entity,
            Map<String, String> okapiHeaders,
            Handler<AsyncResult<Response>> asyncResultHandler,
            Context vertxContext) {

        PgUtil.post(OVERDUE_FINE_POLICY_TABLE, entity, okapiHeaders, vertxContext,
          PostOverdueFinesPoliciesResponse.class, r -> {
            if (ErrorHelper.didUniqueConstraintViolationOccur(r.result(), PRIMARY_KEY)) {
              Error error = new Error()
                .withMessage(DUPLICATE_ENTITY_MESSAGE)
                .withCode(DUPLICATE_ERROR_CODE);
              asyncResultHandler.handle(
                r.map(PostOverdueFinesPoliciesResponse.respond422WithApplicationJson(createErrors(error))));
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

        PgUtil.getById(OVERDUE_FINE_POLICY_TABLE, OVERDUE_FINE_POLICY, overdueFinePolicyId, okapiHeaders, vertxContext,
                GetOverdueFinesPoliciesByOverdueFinePolicyIdResponse.class, asyncResultHandler);
    }

    @Validate
    @Override
    public void deleteOverdueFinesPoliciesByOverdueFinePolicyId(String overdueFinePolicyId,
            String lang,
            Map<String, String> okapiHeaders,
            Handler<AsyncResult<Response>> asyncResultHandler,
            Context vertxContext) {

        PgUtil.deleteById(OVERDUE_FINE_POLICY_TABLE, overdueFinePolicyId, okapiHeaders, vertxContext,
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

        PgUtil.put(OVERDUE_FINE_POLICY_TABLE, entity, overdueFinePolicyId, okapiHeaders, vertxContext,
                PutOverdueFinesPoliciesByOverdueFinePolicyIdResponse.class, asyncResultHandler);
    }
}
