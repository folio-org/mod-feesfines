package org.folio.rest.impl;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.folio.rest.jaxrs.model.TenantAttributes;
import org.folio.rest.tools.utils.TenantLoading;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class TenantRefAPI extends TenantAPI {
  private static final Logger log = LoggerFactory.getLogger(TenantRefAPI.class);

  @Override
  public void postTenant(TenantAttributes tenantAttributes,
                         Map<String, String> headers,
                         Handler<AsyncResult<Response>> handler, Context context) {

    log.info("postTenant");
    log.info("Tenant attributes: {}", JsonObject.mapFrom(tenantAttributes));

    Vertx vertx = context.owner();
    super.postTenant(tenantAttributes, headers, res -> {
      if (res.failed()) {
        handler.handle(res);
        return;
      }

      TenantLoading tenantLoading = new TenantLoading();
      tenantLoading.withKey("loadReference").withLead("reference-data")
        .withIdContent()
        .add("lost-item-fees-policies")
        .add("overdue-fines-policies")
        .perform(tenantAttributes, headers, vertx, performResponse -> {
          if (performResponse.failed()) {
            log.error("postTenant failure", performResponse.cause());

            handler.handle(io.vertx.core.Future.succeededFuture(PostTenantResponse
              .respond500WithTextPlain(performResponse.cause().getLocalizedMessage())));
            return;
          }

          log.info("postTenant executed successfully");
          handler.handle(io.vertx.core.Future.succeededFuture(PostTenantResponse
            .respond201WithApplicationJson("")));
        });
    }, context);
  }
}
